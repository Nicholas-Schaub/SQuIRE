package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;

public class AbsorptionThread implements Runnable {

	private String rawImageDir;
	private boolean saveRaw;
	private ImagePlus rawImage;
	
	public AbsorptionThread(ImagePlus image, int channelIndex) {
		rawImage = image;
		rawImageDir = AppParams.getRawImageDir(channelIndex);
		saveRaw = true;
	}
	
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		checkAndSave(rawImage,saveRaw,rawImageDir);
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
