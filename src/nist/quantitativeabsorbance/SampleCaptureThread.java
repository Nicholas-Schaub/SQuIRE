package nist.quantitativeabsorbance;

import java.io.File;

import javax.swing.JOptionPane;

import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

import ij.IJ;
import ij.ImagePlus;
import mmcorej.CMMCore;
import mmcorej.DoubleVector;
import mmcorej.StrVector;

public class SampleCaptureThread implements Runnable {
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
	private int numChannels;
	private int minExposure = (int) AppParams.getMinExposure();
	private int maxExposure = (int) AppParams.getMaxExposure();
	private int numReplicates = AppParams.getNumReplicates();
	
    public void run() {
		AppParams params = AppParams.getInstance();
		SimpleCapture cap = new SimpleCapture(true);
		
		channelName = AppParams.getChannelName();
		absorptionSetting = AppParams.getAbsorptionSetting();
		fluorescentDevice = AppParams.getFluorescentDevice();
		fluorescentDeviceSetting = AppParams.getFluorescentDeviceSetting();
		transmittedDevice = AppParams.getTransmittedDevice();
		transmittedDeviceSetting = AppParams.getTransmittedDeviceSetting();
		channelExposure = AppParams.getChannelExposures();
		channelOffset = AppParams.getChannelOffset();
		numChannels = AppParams.getChannels();
		
		try {
			
			if (AppParams.getIsAutomated()) {
				platePl = app_.getPositionList();
			}
			
			if (AppParams.hasAutoShutter()) {
				core_.setShutterOpen(false);
			}
			
			// Collect stats for each pixel in each channel at multiple exposures.
			for (int i=0; i<AppParams.getNumSamples()+2; i++) {
				if (i==0) {
					
					if (!AppParams.getIsAutomated()) {
						JOptionPane.showMessageDialog(null,
							"Please move your sample to an empty well and turn the bright field light OFF.",
							"Quantitative Absorption Plugin",
							JOptionPane.PLAIN_MESSAGE);
					} else if (!platePl.getPosition(0).getLabel().endsWith("BLANKWELL")) {
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
					sampleLabel = "Light off intensity";
					AppParams.setCurrentSampleName(sampleLabel);
					currentSample = cap.powerCaptureSeries(sampleLabel, 0, 3, numReplicates);
					AppParams.setDarkBlank(new ImageStats(currentSample));
					IJ.saveAsTiff(currentSample, AppParams.getRawImageDir(0)+currentSample.getTitle());
				} else if (i==1) {
					
					if (AppParams.hasAutoShutter() && !AppParams.useAutoShutter()) {
						core_.setShutterDevice(AppParams.getTransmittedShutter());
						core_.setShutterOpen(true);
					}
					
					if (!AppParams.getIsAutomated()) {
						JOptionPane.showMessageDialog(null,
							"Please move your sample to an empty well and turn the bright field light ON.",
							"Quantitative Absorption Plugin",
							JOptionPane.PLAIN_MESSAGE);
					}
					
					for (int j = 0; j<numChannels; j++) {
						if (absorptionSetting.get(j).equals("Absorbance")){
							System.out.println(j);
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForDevice(transmittedDevice.get(j));
							core_.waitForDevice(fluorescentDevice.get(j));
							sampleLabel = channelName.get(j) + " - Light On Blank";
							AppParams.setCurrentSampleName(sampleLabel);
							currentSample = cap.powerCaptureSeries(sampleLabel, 0, 3, numReplicates);
							ImageStats lightStats = new ImageStats(currentSample);
							AppParams.addLightBlank(lightStats);
							System.out.println("Added Light Blank!");
							IJ.saveAsTiff(currentSample, AppParams.getRawImageDir(j));
						}
					}

				} else if (i>1) {
					if (!AppParams.getIsAutomated() && i==2) {
						JOptionPane.showMessageDialog(null,
							"Please move sample #" + Integer.toString(i-1) + " into view.",
							"Quantitative Absorption Plugin",
							JOptionPane.PLAIN_MESSAGE);
						sampleLabel = "Sample #"+Integer.toString(i-1);
					} else if (!AppParams.getIsAutomated()) {
						JOptionPane.showMessageDialog(null,
							"Please move sample #" + Integer.toString(i-1) + " into view.",
							"Quantitative Absorption Plugin",
							JOptionPane.PLAIN_MESSAGE);
						sampleLabel = "Sample #"+Integer.toString(i-1);
					} else {
						
						core_.setProperty(fluorescentDevice.get(0), "Label", fluorescentDeviceSetting.get(0));
						core_.setProperty(transmittedDevice.get(0), "Label", transmittedDeviceSetting.get(0));
						
						sampleLabel = platePl.getPosition(i-2).getLabel();
						long startTime = System.currentTimeMillis();
						MultiStagePosition.goToPosition(platePl.getPosition(i-2), core_);
						System.out.print("Stage move time: " + Long.toString(System.currentTimeMillis()-startTime) + "\n");

						startTime = System.currentTimeMillis();
						core_.setShutterOpen(true);
						System.out.print("Shutter open time: " + Long.toString(System.currentTimeMillis()-startTime) + "\n");
					}
					
					int currentAbsorb = 0;
					
					for (int j = 0; j<numChannels; j++) {
						AppParams.setCurrentSampleName(sampleLabel);
						if (absorptionSetting.get(j).equals("Absorbance")){
							if (!core_.getShutterDevice().equals(AppParams.getTransmittedShutter())) {
								core_.setShutterOpen(false);
								core_.setShutterDevice(AppParams.getTransmittedShutter());
								core_.setShutterOpen(true);
							}
							if (j!=0) {
								core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
								core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							}
							core_.waitForSystem();
							long startTime = System.currentTimeMillis();
							currentSample = cap.powerCaptureSeries(sampleLabel, 0, 3, numReplicates);
							long captureTime = System.currentTimeMillis(); 
							System.out.print("Capture time: " + Long.toString(captureTime-startTime) + "\n");
							IJ.saveAsTiff(currentSample, AppParams.getRawImageDir(j) + sampleLabel);
							long saveTime = System.currentTimeMillis();
							System.out.print("Save time: " + Long.toString(saveTime - captureTime) + "\n");
							Thread absorptionThread = new Thread(new AbsorptionThread(currentSample,AppParams.getLightBlank(currentAbsorb++),j));
							absorptionThread.start();
							long threadTime = System.currentTimeMillis();
							System.out.print("Thread initiation time: " + Long.toString(threadTime - saveTime) + "\n");
						} else if (absorptionSetting.get(j).startsWith("Phase")) {
							if (!core_.getShutterDevice().equals(AppParams.getTransmittedShutter())) {
								core_.setShutterOpen(false);
								core_.setShutterDevice(AppParams.getTransmittedShutter());
								core_.setShutterOpen(true);
							}
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForSystem();
							currentSample = cap.singleCapture(sampleLabel,channelExposure.get(j));
							IJ.saveAsTiff(currentSample, AppParams.getChannelImageDir(j) + sampleLabel);
						} else {
							if (!core_.getShutterDevice().equals(AppParams.getFluorescentShutter())) {
								core_.setShutterOpen(false);
								core_.setShutterDevice(AppParams.getFluorescentShutter());
								core_.setShutterOpen(true);
							}
							cap.setExposure(channelExposure.get(j));
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForSystem();
							currentSample = cap.singleCapture(sampleLabel);
							IJ.saveAsTiff(currentSample, AppParams.getChannelImageDir(j) + sampleLabel);
						}
					}
					AppParams.updateStatus(((double)(i-2))/((double) AppParams.getNumSamples()));

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
			e.printStackTrace();
		}
	}

}
