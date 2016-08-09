package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;

public class AbsorptionThread implements Runnable {

	private String meanSaveDir;
	private String stdSaveDir;
	private String rawImageDir;
	private boolean saveMean;
	private boolean saveSTD;
	private boolean saveRaw;
	private ImagePlus meanImage;
	private ImagePlus stdImage;
	private ImagePlus rawImage;
	
	public AbsorptionThread(ImagePlus image, int channelIndex) {
		rawImage = image;
		meanSaveDir = AppParams.getMeanImageDir(channelIndex);
		saveMean = AppParams.isSaveMeanImages();
		stdSaveDir = AppParams.getStdImageDir(channelIndex);
		saveSTD = AppParams.isSaveStdImages();
		rawImageDir = AppParams.getRawImageDir(channelIndex);
		saveRaw = AppParams.isSaveRawImages();
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
