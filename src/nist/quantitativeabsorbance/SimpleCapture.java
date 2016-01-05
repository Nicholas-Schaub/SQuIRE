package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import mmcorej.CMMCore;

public class SimpleCapture {
	
	private CMMCore core_ = AppParams.getCore_();
	private int bitDepth = 16;
	private int width = (int) core_.getImageWidth();
	private int height = (int) core_.getImageHeight();
	private boolean isLive = false;
	
	public SimpleCapture(boolean startLive) {
		if (startLive) {
			startLive();
		}
		if (bitDepth!=8 || bitDepth!=16) {
			if (bitDepth==8) {
				bitDepth = 8;
			} else {
				bitDepth = 16;
			}
		}
	}
	
	public ImagePlus powerCaptureSeries(String imgName, int start, int end, int replicates){
		ImagePlus imageSeries = IJ.createHyperStack(imgName, width, height, 1, end-start+1, replicates, bitDepth); 
		for (int i = start; i<=end; i++) {
			int exposure = (int) Math.pow(2,i);
			ImagePlus tempImage = seriesCapture(imgName,exposure,replicates);
			for (int j = 1; j<=replicates; j++) {
				imageSeries.setPosition(1,i+1,j);
				tempImage.setPosition(1,1,j);
				imageSeries.getProcessor().setPixels(tempImage.getProcessor().getPixels());
				imageSeries.getStack().setSliceLabel("Exposure: " + Integer.toString(exposure) + "ms Replicate: " + Integer.toString(j), imageSeries.getCurrentSlice());
			}
		}
		
		imageSeries.setPosition(1,1,1);
		
		return imageSeries;
	}
	
	public ImagePlus seriesCapture(String imgName, int exposure, int replicates) {
		
		ImagePlus imageSeries = IJ.createHyperStack(imgName, width, height, 1, 1, replicates, bitDepth);
		double dExposure = (double) exposure;
		
		try {
			if (isLive) {
				if (core_.getExposure()!=dExposure) {
					setExposure(exposure);
				}
			} else {
				core_.setExposure(dExposure);
				core_.initializeCircularBuffer();
				core_.startContinuousSequenceAcquisition(0);
			}
			
			core_.clearCircularBuffer();
			
			int i = 0;
			while ((i+core_.getRemainingImageCount())<=replicates) {
				int remainingImages = core_.getRemainingImageCount();
				for (int j = 0; j<remainingImages; j++) {
					imageSeries.setPosition(1, 1, ++i);
					imageSeries.getProcessor().setPixels(core_.popNextImage());
					imageSeries.setTitle(imgName);
				}
			}
			for (int j = i; j<replicates; j++) {
				imageSeries.setPosition(1, 1, ++i);
				imageSeries.getProcessor().setPixels(core_.popNextImage());
				imageSeries.setTitle(imgName);
			}
			
			imageSeries.setPosition(1, 1, 1);
			
			for (i = 1; i<=replicates; i++) {
				ImageStack tempStack = imageSeries.getStack();
				tempStack.setSliceLabel("Exposure: " + Integer.toString(exposure) + "ms Replicate: " + Integer.toString(i), i);
			}
			
			if (!isLive) {
				core_.stopSequenceAcquisition();
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return imageSeries;
		
	}
	
	// Captures a single image and returns an ImagePlus image.
	public ImagePlus singleCapture(String str) {

		ImagePlus implus = new ImagePlus();
		Object pix;
		
		try {
			if (isLive) {
				core_.clearCircularBuffer();
				while (core_.getRemainingImageCount()==0) {
				}
				pix = core_.popNextImage();
			} else {
				core_.snapImage();
				pix = core_.getImage();
			}
			implus = NewImage.createImage(str,
					width,
					height,
					1,
					bitDepth, 
					1);
			implus.getProcessor().setPixels(pix);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return implus;
	}
	
	public ImagePlus singleCapture(String str, int exposure) {

		ImagePlus implus = new ImagePlus();

		setExposure(exposure);
		
		implus = singleCapture(str);

		return implus;
	}
	
	public void startLive() {
		try {
			core_.initializeCircularBuffer();
			core_.startContinuousSequenceAcquisition(0);
			isLive = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void stopLive() {
		try {
			core_.stopSequenceAcquisition();
			isLive = false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setExposure(int exposure) {
		try {
			if (isLive) {
				core_.stopSequenceAcquisition();
			}
			core_.setExposure(exposure);
			if (isLive) {
				core_.initializeCircularBuffer();
				core_.startContinuousSequenceAcquisition(0);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
