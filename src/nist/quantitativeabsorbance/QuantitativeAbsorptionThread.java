package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import javax.swing.JOptionPane;

import mmcorej.CMMCore;
import mmcorej.DoubleVector;
import mmcorej.StrVector;

import org.apache.commons.math3.filter.KalmanFilter;
import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

import clooj.indent__init;

public class QuantitativeAbsorptionThread implements Runnable {
	private ImagePlus AbsorbanceImage;
	private ImageStats currentSample;
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
	
    public void run() {
		AppParams params = AppParams.getInstance();
		
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
					AppParams.setForceMax(true);
					currentSample = new ImageStats();
					AppParams.setDarkBlank(currentSample);
					for (int j = 0; j<numChannels; j++) {
						if (absorptionSetting.get(j).equals("Absorbance")){
							checkAndSaveImages(j);
						}
					}
					AppParams.setForceMax(false);
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
					
					if (numChannels>1) {
						for (int j = 0; j<numChannels; j++) {
							if (absorptionSetting.get(j).equals("Absorbance")){
								//core_.setShutterOpen(false);
								System.out.println(j);
								core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
								core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
								core_.waitForSystem();
								//core_.setShutterOpen(true);
								sampleLabel = channelName.get(j) + " - Light On Blank";
								AppParams.setCurrentSampleName(sampleLabel);
								//AppParams.setForceMax(false);
								//Thread.sleep(100);
								currentSample = new ImageStats();
								currentSample.pixelLinReg(0, currentSample, AppParams.getDarkBlank());
								AppParams.addLightBlank(currentSample);
								checkAndSaveImages(j);
							}
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
						sampleLabel = platePl.getPosition(i-2).getLabel();
						core_.setProperty(fluorescentDevice.get(0), "Label", fluorescentDeviceSetting.get(0));
						core_.setProperty(transmittedDevice.get(0), "Label", transmittedDeviceSetting.get(0));
						MultiStagePosition.goToPosition(platePl.getPosition(i-2), core_);
						if (!core_.getShutterDevice().equals(AppParams.getTransmittedDevice())) {
							core_.setShutterOpen(false);
							core_.setShutterDevice(AppParams.getTransmittedShutter());
							core_.setShutterOpen(true);
						}
						core_.waitForSystem();
					}
					
					int k = 0;
					for (int j = 0; j<numChannels; j++) {
						AppParams.setCurrentSampleName(sampleLabel);
						if (absorptionSetting.get(j).equals("Absorbance")){
							//core_.setShutterOpen(false);
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForSystem();
							//core_.setShutterOpen(true);
							//Thread.sleep(100);
							currentSample = new ImageStats();
							System.out.println(k);
							currentSample.pixelLinReg(0, AppParams.getLightBlank(k), AppParams.getDarkBlank());
							checkAndSaveImages(j);
							AbsorbanceImage = new ImagePlus(channelName.get(j),currentSample.getAbsorbance(AppParams.getLightBlank(j)));
							IJ.saveAsTiff(AbsorbanceImage, AppParams.getChannelImageDir(j) + sampleLabel);
							k++;
						} else {
							core_.setProperty(fluorescentDevice.get(j), "Label", fluorescentDeviceSetting.get(j));
							core_.setProperty(transmittedDevice.get(j), "Label", transmittedDeviceSetting.get(j));
							core_.waitForSystem();
							if (!core_.getShutterDevice().equals(AppParams.getFluorescentShutter())) {
								core_.setShutterOpen(false);
								core_.setShutterDevice(AppParams.getFluorescentShutter());
								core_.setShutterOpen(true);
							}
							ImagePlus fluorescentImage = capture(channelName.get(j), (int) channelExposure.get(j));
							IJ.saveAsTiff(fluorescentImage, AppParams.getChannelImageDir(j) + sampleLabel);
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
    
    public ImagePlus capture(String str, int exposure) {

    	ImagePlus implus = new ImagePlus();
    	double dExposure = (int) exposure;

    	try {
    		this.core_.setExposure(dExposure);
    		this.core_.snapImage();
    		Object pix = this.core_.getImage();
    		implus = NewImage.createImage(str,
    				(int) this.core_.getImageWidth(),
    				(int) this.core_.getImageHeight(),
    				1,
    				16, 
    				1);
    		implus.getProcessor().setPixels(pix);
    	} catch (Exception e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

    	return implus;
    }

	private void checkAndSaveImages(int i) {
		if (AppParams.isSaveMeanImages()) {
			ImagePlus impMean = new ImagePlus(currentSample.getChannelLabel(0) + " Mean",currentSample.mean.get(0));
			IJ.saveAsTiff(impMean, AppParams.getMeanImageDir(i) + sampleLabel + " - Mean");
		}
		
		if (AppParams.isSaveVarianceImages()) {
			ImagePlus impVar = new ImagePlus(currentSample.getChannelLabel(0) + " Variance",currentSample.deviation.get(0));
			IJ.saveAsTiff(impVar, AppParams.getVarianceImageDir(i) + sampleLabel + " - Variance");
		}
		
		if (AppParams.isLinearRegression()) {
			ImagePlus absImage = currentSample.pixelLinReg(0, currentSample, AppParams.getDarkBlank());
			IJ.saveAsTiff(absImage, AppParams.getLinearRegressionDir(i) + currentSample.getChannelLabel(0) + " - Linear Regression");
		}
	}
}
