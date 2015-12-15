package nist.quantitativeabsorbance;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import mmcorej.CMMCore;

public class SimpleCapture {
	
	private CMMCore core_ = AppParams.getCore_();
	private String saveDirectory;
	private ImagePlus singleImage;
	private int bitDepth = 16;
	private ImagePlus imageSeries;
	private int width = (int) core_.getImageWidth();
	private int height = (int) core_.getImageHeight();
	
	public SimpleCapture(String saveDirectory) {
		if (!saveDirectory.endsWith(File.separator)) {
			saveDirectory = saveDirectory+File.separator;
		}
		this.saveDirectory = saveDirectory;
	}
	
	public void setSaveDir(String saveDirectory) {
		if (!saveDirectory.endsWith(File.separator)) {
			saveDirectory = saveDirectory+File.separator;
		}
		this.saveDirectory = saveDirectory;
	}
	
	public void setBitDepth(int bitDepth) {this.bitDepth = bitDepth;}
	
	public ImagePlus powerCaptureSeries(String imgName, int start, int end, int replicates){
		imageSeries = IJ.createHyperStack("", width, height, 1, end+1, replicates, bitDepth);
		for (int i = start; i<=end; i++) {
			int exposure = (int) Math.pow(2,i);
			for (int j = 0; j<replicates; j++) {
				singleImage = singleCapture(imgName + " - Exp " + Integer.toString(exposure) + "ms - Rep "+Integer.toString(j+1),exposure);
				imageSeries.setPosition(1,i,j);
				imageSeries.getProcessor().setPixels(singleImage.getProcessor().getPixels());
			}
		}
		
		return imageSeries;
	}
	
	// Captures a single image and returns an ImagePlus image.
	public ImagePlus singleCapture(String str, int exposure) {

		ImagePlus implus = new ImagePlus();
		double dExposure = (int) exposure;

		try {
			core_.setExposure(dExposure);
			core_.snapImage();
			Object pix = core_.getImage();
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
}
