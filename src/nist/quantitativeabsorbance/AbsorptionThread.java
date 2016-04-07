package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;

public class AbsorptionThread implements Runnable {

	private String meanSaveDir;
	private String stdSaveDir;
	private String slopeSaveDir;
	private String rawImageDir;
	private String absImageDir;
	private boolean saveMean;
	private boolean saveSTD;
	private boolean saveSlope;
	private boolean saveRaw;
	private ImagePlus meanImage;
	private ImagePlus stdImage;
	private ImagePlus slopeImage;
	private ImagePlus rawImage;
	private ImagePlus absorbanceImage;
	private ImageStats foreground;
	private ImagePlus impForeground;
	private ImageStats background;
	
	public AbsorptionThread(ImagePlus image, ImageStats foreground, int channelIndex) {
		rawImage = image;
		meanSaveDir = AppParams.getMeanImageDir(channelIndex);
		saveMean = AppParams.isSaveMeanImages();
		stdSaveDir = AppParams.getStdImageDir(channelIndex);
		saveSTD = AppParams.isSaveStdImages();
		slopeSaveDir = AppParams.getLinearRegressionDir(channelIndex);
		saveSlope = AppParams.isLinearRegression();
		rawImageDir = AppParams.getRawImageDir(channelIndex);
		saveRaw = AppParams.isSaveRawImages();
		this.foreground = foreground;
		background = AppParams.getDarkBlank();
		absImageDir = AppParams.getChannelImageDir(channelIndex);
	}
	
	public AbsorptionThread(ImagePlus image, ImageStats slopeImage, ImagePlus foreground, int channelIndex) {
		rawImage = image;
		rawImageDir = AppParams.getRawImageDir(channelIndex);
		saveRaw = AppParams.isSaveRawImages();
		meanSaveDir = AppParams.getMeanImageDir(channelIndex);
		saveMean = AppParams.isSaveMeanImages();
		stdSaveDir = AppParams.getStdImageDir(channelIndex);
		saveSTD = AppParams.isSaveStdImages();
		this.foreground = slopeImage;
		this.impForeground = foreground;
		background = AppParams.getDarkBlank();
		absImageDir = AppParams.getChannelImageDir(channelIndex);
	}
	
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		checkAndSave(rawImage,saveRaw,rawImageDir);
		ImageStats imStat = new ImageStats(rawImage);
		meanImage = imStat.getFrameMean();
		checkAndSave(meanImage,saveMean,meanSaveDir);
		stdImage = imStat.getFrameDeviation();
		checkAndSave(stdImage,saveSTD,stdSaveDir);
		if (this.foreground!= null) {
			slopeImage = imStat.pixelLinReg(foreground, background);
			checkAndSave(slopeImage,saveSlope,slopeSaveDir);
			absorbanceImage = imStat.getAbsorbance(foreground, background);
		} else {
			absorbanceImage = imStat.getAbsorbance(foreground, impForeground);
		}
		checkAndSave(absorbanceImage,true,absImageDir);
		long stopTime = System.currentTimeMillis();
		
		System.out.println("Thread for " + rawImage.getTitle() + " was completed in "
							+ Long.toString(stopTime - startTime) + "ms");
	}
	
	public void checkAndSave(ImagePlus imp, boolean save, String saveDir) {
		if (save) {
			System.out.println("Saving : " + saveDir + imp.getTitle());
			IJ.saveAsTiff(imp, saveDir+imp.getTitle());
		}
		
	}
	
}
