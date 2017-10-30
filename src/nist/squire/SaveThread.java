package nist.squire;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import ij.IJ;
import ij.ImagePlus;

public class SaveThread implements Runnable {

	private String rawImageDir;
	private boolean isCalib;
	private ImagePlus rawImage;
	private int channelIndex;
	
	public SaveThread(ImagePlus image, int channelIndex, boolean isCalib) {
		rawImage = image;
		if (isCalib) {
			rawImageDir = AppParams.getCalibrationImageDir(channelIndex);
		} else {
			rawImageDir = AppParams.getRawImageDir(channelIndex);
		}
		this.isCalib = isCalib;
		this.channelIndex = channelIndex;
	}
	
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		checkAndSave(rawImage,rawImageDir);
		long stopTime = System.currentTimeMillis();
		
		System.out.println("Thread for " + rawImage.getTitle() + " was completed in "
							+ Long.toString(stopTime - startTime) + "ms");
	}
	
	public void checkAndSave(ImagePlus imp, String saveDir) {
		System.out.println("Saving : " + saveDir + imp.getTitle());
		if (isCalib) {
			try {
				if (imp.getNFrames()==1) {
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(AppParams.getCalibrationImageDir(channelIndex) + imp.getTitle() + ".txt"))));
					pw.println("Channel Name, " + AppParams.getChannelName().get(channelIndex));
					pw.println("Channel Exposure, " + AppParams.getChannelExposures().get(channelIndex));
					pw.println("# Blank Images, " + imp.getNSlices());
					pw.close();
					ImageStats imstats = new ImageStats(imp);
					IJ.saveAsTiff(imstats.getFrameMean(), saveDir+imp.getTitle()+"-Mean");
					IJ.saveAsTiff(imstats.getFrameDeviation(), saveDir+imp.getTitle()+"-STD");
					imstats = null;
				} else {
					IJ.saveAsTiff(imp, saveDir+imp.getTitle());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			IJ.saveAsTiff(imp, saveDir+imp.getTitle());
		}
		
	}
	
}
