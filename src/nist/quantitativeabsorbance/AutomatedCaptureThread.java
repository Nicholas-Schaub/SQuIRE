package nist.quantitativeabsorbance;

import javax.swing.JOptionPane;

import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.AutofocusManager;
import org.micromanager.utils.MMScriptException;

import ij.IJ;
import ij.ImagePlus;
import mmcorej.BooleanVector;
import mmcorej.CMMCore;
import mmcorej.DoubleVector;
import mmcorej.StrVector;

public class AutomatedCaptureThread implements Runnable {
	private ImagePlus currentSample;
	private String sampleLabel;
	private ScriptInterface app_ = AppParams.getApp_();
	private CMMCore core_ = app_.getMMCore();
	private PositionList platePl;
	
	private StrVector fluorescentDevice;
	private StrVector fluorescentDeviceSetting;
	private StrVector transmittedDevice;
	private StrVector transmittedDeviceSetting;
	private StrVector absorptionSetting;
	private StrVector channelName;
	private DoubleVector channelExposure;
	private DoubleVector channelOffset;
	private BooleanVector useAutofocus;
	private int numChannels;
	private int numReplicates = AppParams.getNumReplicates();
	
    public void run() {
		AppParams params = AppParams.getInstance();
		SimpleCapture cap = new SimpleCapture(false);
		
		channelName = AppParams.getChannelName();
		absorptionSetting = AppParams.getAbsorptionSetting();
		fluorescentDevice = AppParams.getFluorescentDevice();
		fluorescentDeviceSetting = AppParams.getFluorescentDeviceSetting();
		transmittedDevice = AppParams.getTransmittedDevice();
		transmittedDeviceSetting = AppParams.getTransmittedDeviceSetting();
		channelExposure = AppParams.getChannelExposures();
		channelOffset = AppParams.getChannelOffset();
		numChannels = AppParams.getChannels();
		useAutofocus = AppParams.getAutofocus();
		
		int start = 0;
		
		try {
			AutofocusManager afm_ = app_.getAutofocusManager();
			afm_.refresh();
			
			platePl = app_.getPositionList();
						
			if (AppParams.hasAutoShutter()) {
				core_.setShutterDevice(AppParams.getTransmittedShutter());
			}
			
			// Collect stats for each pixel in each channel at multiple exposures.
			for (int i=start; i<AppParams.getNumSamples()+2; i++) {
				if (i==0) {
					if (AppParams.hasAutoShutter()) {
						core_.setShutterDevice(AppParams.getTransmittedShutter());
						core_.setShutterOpen(false);
					}
					if (!platePl.getPosition(0).getLabel().endsWith("BLANKWELL")) {
						core_.setShutterOpen(true);
						app_.enableLiveMode(true);
						JOptionPane.showMessageDialog(null,
							"Please move your sample to a clean, empty space.",
							"Quantitative Absorption Plugin",
							JOptionPane.PLAIN_MESSAGE);
						app_.enableLiveMode(false);
						core_.setShutterOpen(false);
					} else {
						MultiStagePosition.goToPosition(platePl.getPosition(i), core_);
					}
					sampleLabel = "Dark Background";
					AppParams.setCurrentSampleName(sampleLabel);
					currentSample = cap.powerCaptureSeries(sampleLabel, 0,(int) Math.pow(2, 8), numReplicates);
					AppParams.setDarkBlank(new ImageStats(currentSample));
				} else if (i==1) {
					
					if (AppParams.hasAutoShutter()) {
						core_.setShutterDevice(AppParams.getTransmittedShutter());
						core_.setShutterOpen(true);
					}
					
					for (int j = 0; j<numChannels; j++) {
						if (absorptionSetting.get(j).equals("Absorbance")){
							System.out.println(j);
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForDevice(transmittedDevice.get(j));
							core_.waitForDevice(fluorescentDevice.get(j));
							while (!core_.getProperty(fluorescentDevice.get(j), "Label").equalsIgnoreCase(fluorescentDeviceSetting.get(j)) ||
									!core_.getProperty(transmittedDevice.get(j), "Label").equalsIgnoreCase(transmittedDeviceSetting.get(j))){
								System.out.println("Current fluorescent setting: " + core_.getProperty(fluorescentDevice.get(j), "Label"));
								System.out.println("Current fluorescent setting: " + core_.getProperty(transmittedDevice.get(j), "Label"));
								core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
								core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
								core_.waitForDevice(transmittedDevice.get(j));
								core_.waitForDevice(fluorescentDevice.get(j));
							}
							sampleLabel = channelName.get(j) + " - Linear Regression";
							AppParams.setCurrentSampleName(sampleLabel);
							ImageStats lightStats = new ImageStats(sampleLabel,"");
							lightStats.pixelLinReg();
							AppParams.addLightBlank(lightStats);
							System.out.println("Added Light Blank!");
							Thread linThread = new Thread(new SaveThread(lightStats.rawImage,j,true));
							linThread.start();
							//IJ.saveAsTiff(lightStats.rawImage, AppParams.getCalibrationImageDir(j)+lightStats.rawImage.getTitle());
							//IJ.saveAsTiff(AppParams.getDarkBlank().rawImage, AppParams.getCalibrationImageDir(j)+AppParams.getDarkBlank().rawImage.getTitle());
							Thread backThread = new Thread(new SaveThread(AppParams.getDarkBlank().rawImage,j,true));
							backThread.start();
							ImagePlus foregroundRaw = cap.seriesCapture(channelName.get(j)+" - Light Background", lightStats.bestExposure(), lightStats.numBlankSamples(lightStats.bestExposure()));
							ImageStats foreground = new ImageStats(foregroundRaw);
							AppParams.addForeground(foreground.getFrameMean());
							AppParams.setChannelExposure(j, lightStats.bestExposure());
							Thread forThread = new Thread(new SaveThread(foreground.rawImage,j,true));
							forThread.start();
							//IJ.saveAsTiff(foreground.rawImage, AppParams.getCalibrationImageDir(j)+foreground.rawImage.getTitle());
						}
					}

				} else if (i>1) {
					core_.setProperty(fluorescentDevice.get(0), "Label", fluorescentDeviceSetting.get(0));
					core_.setProperty(transmittedDevice.get(0), "Label", transmittedDeviceSetting.get(0));
					
					sampleLabel = platePl.getPosition(i-2).getLabel();
					long startTime = System.currentTimeMillis();
					MultiStagePosition.goToPosition(platePl.getPosition(i-2), core_);
					System.out.print("Stage move time: " + Long.toString(System.currentTimeMillis()-startTime) + "\n");

					startTime = System.currentTimeMillis();
					if (!core_.getShutterDevice().equals(AppParams.getTransmittedShutter())) {
						core_.setShutterOpen(false);
						core_.setShutterDevice(AppParams.getTransmittedShutter());
						core_.setShutterOpen(true);
					}
					System.out.print("Shutter open time: " + Long.toString(System.currentTimeMillis()-startTime) + "\n");
					System.out.println("Position: " + platePl.getPosition(i-2).getLabel());
					
					int currentAbsorb = 0;
					
					for (int j = 0; j<numChannels; j++) {
						AppParams.setCurrentSampleName(sampleLabel);
						
						if (absorptionSetting.get(j).startsWith("Fluorescence")) {
							if (!core_.getShutterDevice().equals(AppParams.getFluorescentShutter())) {
								core_.setShutterOpen(false);
								core_.waitForSystem();
								core_.setShutterDevice(AppParams.getFluorescentShutter());
								core_.setShutterOpen(true);
								core_.waitForSystem();
							}
						} else if (absorptionSetting.get(j).startsWith("Absorbance")) {
							if (!core_.getShutterDevice().equals(AppParams.getTransmittedShutter())) {
								core_.setShutterOpen(false);
								core_.waitForSystem();
								core_.setShutterDevice(AppParams.getTransmittedShutter());
								core_.setShutterOpen(true);
								core_.waitForSystem();
							}
						}
						
						core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
						core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
						core_.waitForSystem();
						while (!core_.getProperty(fluorescentDevice.get(j), "Label").equalsIgnoreCase(fluorescentDeviceSetting.get(j)) ||
								!core_.getProperty(transmittedDevice.get(j), "Label").equalsIgnoreCase(transmittedDeviceSetting.get(j))){
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForSystem();
						}
						
						if (useAutofocus.get(j)){
							System.out.print("Focusing...");
							core_.waitForSystem();
							afm_.getDevice().setPropertyValue("Exposure", Double.toString(channelExposure.get(j)));
							afm_.getDevice().applySettings();
							afm_.getDevice().fullFocus();
						}
						if (channelOffset.get(j)!=0) {
							core_.setRelativePosition(channelOffset.get(j));
						}
						core_.waitForSystem();
						if (absorptionSetting.get(j).equals("Absorbance")){
							startTime = System.currentTimeMillis();
							currentSample = cap.threshCaptureSeries(sampleLabel, channelExposure.get(j), numReplicates, AppParams.getLightBlank(currentAbsorb).minConfPix(numReplicates));
							//currentSample = cap.powerCaptureSeries(sampleLabel, (int) channelExposure.get(j), (int) (channelExposure.get(j)*Math.pow(2,5)), numReplicates);
							long captureTime = System.currentTimeMillis(); 
							System.out.print("Capture time: " + Long.toString(captureTime-startTime) + "\n");
							IJ.saveAsTiff(currentSample, AppParams.getRawImageDir(j) + sampleLabel);
							long saveTime = System.currentTimeMillis();
							System.out.print("Save time: " + Long.toString(saveTime - captureTime) + "\n");
							Thread absorptionThread = new Thread(new SaveThread(currentSample,j,false));
							absorptionThread.start();
							long threadTime = System.currentTimeMillis();
							System.out.print("Thread initiation time: " + Long.toString(threadTime - saveTime) + "\n");
						} else if (absorptionSetting.get(j).startsWith("Phase")) {
							currentSample = cap.singleCapture(sampleLabel,channelExposure.get(j));
							IJ.saveAsTiff(currentSample, AppParams.getChannelImageDir(j) + sampleLabel);
						} else {
							cap.setExposure(channelExposure.get(j));
							currentSample = cap.singleCapture(sampleLabel);
							IJ.saveAsTiff(currentSample, AppParams.getChannelImageDir(j) + sampleLabel);
						}
					}

				}
				
				if (params.getStop() || Thread.interrupted()) {
					core_.setShutterOpen(false);
					throw new InterruptedException("canceled");
				}
			}
			
			core_.setShutterOpen(false);
			
		} catch (InterruptedException ex) {
		} catch (MMScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				core_.waitForSystem();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

}
