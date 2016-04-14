package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.util.ArrayList;

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
		int pStart;
		if (start<1) {
			pStart = -1;
		} else {
			pStart = 31-Integer.numberOfLeadingZeros((int) start);
		}
		int pEnd = 31-Integer.numberOfLeadingZeros((int) end);
		int numExposures = pEnd-pStart+1;
		int currentFrame = 0;
		ImagePlus imageSeries = IJ.createHyperStack(imgName, width, height, 1, replicates, (int) numExposures, bitDepth); 
		for (int i = pStart; i <= pEnd; i++) {
			currentFrame++;
			double exposure = Math.pow(2,i);
			ImagePlus tempImage = seriesCapture(imgName,exposure,replicates);
			for (int j = 1; j<=replicates; j++) {
				imageSeries.setPosition(1,j,currentFrame);
				tempImage.setPosition(1,j,1);
				imageSeries.getProcessor().setPixels(tempImage.getProcessor().getPixels());
				imageSeries.getStack().setSliceLabel(Double.toString(exposure), imageSeries.getCurrentSlice());
			}
		}
		
		imageSeries.setPosition(1,1,1);
		return imageSeries;
	}
	
	public ImagePlus threshCaptureSeries(String imgName, double exp, int replicates,int thresh){
		ArrayList<ImagePlus> captureSeries = new ArrayList<ImagePlus>();
		captureSeries.add(seriesCapture(imgName,exp,replicates));
		int index = 0;
		ImageStats temp = new ImageStats(captureSeries.get(0));
		double min = temp.getFrameMean().getStatistics().min;
		while (min<thresh) {
			index++;
			captureSeries.add(seriesCapture(imgName,exp*Math.pow(2, index),replicates));
			temp = new ImageStats(captureSeries.get(index));
			min = temp.getFrameMean().getStatistics().min;
		}
		ImagePlus imageSeries = IJ.createHyperStack(imgName, width, height, 1, replicates, captureSeries.size(), bitDepth);
		for (int i=1; i<=captureSeries.size(); i++) {
			for (int j=1; j<=replicates; j++) {
				imageSeries.setPosition(1,j,i);
				captureSeries.get(i-1).setPosition(j);
				imageSeries.getProcessor().setPixels(captureSeries.get(i-1).getProcessor().getPixels());
				imageSeries.getStack().setSliceLabel(captureSeries.get(i-1).getStack().getSliceLabel(j), imageSeries.getCurrentSlice());
			}
		}
		imageSeries.setPosition(1,1,1);
		return imageSeries;
	}
	
	public ImagePlus seriesCapture(String imgName, double exposure, int replicates) {
		
		ImagePlus imageSeries = IJ.createHyperStack(imgName, width, height, 1, replicates, 1, bitDepth);
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
			
			int currentSlice = 0;
			while ((currentSlice+core_.getRemainingImageCount())<=replicates) {
				int remainingImages = core_.getRemainingImageCount();
				if (core_.getRemainingImageCount()>replicates-currentSlice) {
					remainingImages = replicates-currentSlice;
				}
				for (int j = 0; j<remainingImages; j++) {
					imageSeries.setPosition(1, ++currentSlice, 1);
					imageSeries.getProcessor().setPixels(core_.popNextImage());
					imageSeries.getStack().setSliceLabel(Double.toString(core_.getExposure()), currentSlice);
				}
			}
			
			int remainingImages = replicates-currentSlice;
			for (int j = 0; j<remainingImages; j++) {
				imageSeries.setPosition(1, ++currentSlice, 1);
				imageSeries.getProcessor().setPixels(core_.popNextImage());
				imageSeries.getStack().setSliceLabel(Double.toString(exposure), currentSlice);
			}
			
			if (!isLive) {
				core_.stopSequenceAcquisition();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		imageSeries.setPosition(1,1,1);
		
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
			e.printStackTrace();
		}

		implus.setTitle(str);
		
		return implus;
	}
	
	public ImagePlus singleCapture(String str, double exposure) {

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
			e.printStackTrace();
		}
	}
	
	public void stopLive() {
		try {
			core_.stopSequenceAcquisition();
			isLive = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setExposure(double exposure) {
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
			e.printStackTrace();
		}
	}
}
