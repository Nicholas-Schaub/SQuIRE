package nist.quantitativeabsorbance;

import java.awt.Color;
import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mmcorej.CMMCore;

//This class holds images representing the statistical information for each pixel collected
//	in multiple replicates. The mean intensity stack contains images that are the mean
//	pixel intensity from multiple images collected at the same exposure. The deviation
//	stack holds the corresponding deviation values at each pixel. Finally, the raw
//	intensity stack holds all of the raw data used to collect the other values.

// Updated NJS 2015-12-02
// Will now save all raw images if specified in the Control Panel.
public class ImageStats {

	// Images and stacks associated with pixel statistics
	public ImageStack slopeStats;
	public ImagePlus absorbance;
	public ImageStack imageStatsStack;
	public ImagePlus channelAbsorption = null;
	private ImagePlus meanImage;
	private ImagePlus stdImage;
	public ImagePlus rawImage;
	public ImageStats background;

	// Variables for statistics
	public float averageIntercept;
	public float averageSlope;
	public float averageR;

	// Variables for plotting global statistics
	public double[] exposureSet;
	public double[] intensitySet;
	public double[] deviationSet;
	public double[] maxPixelIntensity;
	public double[] minPixelIntensity;

	// Basic image and capture settings.
	public String name;
	public String channelLabel;
	public int width;
	public int height;
	public int bitdepth;
	public int imagebitdepth;
	private int numReplicates = AppParams.getNumReplicates();
	public int nFrames;
	public int nSlices;
	public int nChannels;
	
	// Properties related to images captured for absorption
	double[] standardDev;
	double rSqr;
	int minimumPixInt;
	double bestExp;
	double bestExpIntensity;
	int numExp;

	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	// Call this function to perform statistics on an ImagePlus object.
	public ImageStats(ImagePlus imp) {
		name = imp.getTitle();
		channelLabel = "";

		rawImage = imp;
		setup();
	}
	
	// Direct calls to this instantiation is reserved for benchmarking cameras.
	protected ImageStats(String sample, String channel) {
		this(new ImagePlus());
		// Sample/channel label
		name = sample;
		channelLabel = channel;
		
		System.out.println("Running pixel exposure statistics...");
		getPixelExposureStats();
		
		setup();
	}
	
	private void setup() {
		// Core and image attributes
		core_ = AppParams.getCore_();
		width = (int) core_.getImageWidth();
		height = (int) core_.getImageHeight();
		bitdepth = (int) core_.getImageBitDepth();

		// Make sure the bit depth is something ImageJ can handle
		if ((bitdepth == 12) || (bitdepth==14)) {imagebitdepth=16;} else {imagebitdepth=bitdepth;}

		nFrames = rawImage.getNFrames();
		nSlices = rawImage.getNSlices();
		nChannels = rawImage.getNChannels();
	}
	
	public int numBlankSamples(double exposure) {
		/* 
		 *  This function calculates the number of images required to make sure that the average pixel
		 *  instensity value will be within 2 pixels of the actual pixel intensity. This calculation is
		 *  based on the equation for estimating the number of samples requied for a 95% confidence
		 *  interval.
		 *  
		 *  n = (Z*sigma/E)^2 
		 *  Z - Z-statistic for a condifence interval (~2 for 95% confidence)
		 *  E - Error bounds of the confidence interval
		 *  sigma - Standard deviation of pixel intensity
		 *  
		 *  For the sake of simplicity, Z = 2 and E = 2 pixels, so that n = sigma^2.
		 */
		if (nFrames==1) {
			return 0;
		}
		
		getFrameDeviation();
		int pos = 2;
		while (Math.abs(deviationSet[pos] - this.stdEst(intensitySet[pos]))/deviationSet[pos] < 0.05) {
			pos++;
			if (pos>=deviationSet.length) break;
		}
		pos--;
		
		CurveFitter cf = new CurveFitter(Arrays.copyOfRange(exposureSet, 0, pos), Arrays.copyOfRange(intensitySet, 0, pos));
		cf.doFit(0);
		double[] curveFitParams = cf.getParams();
		
		double intensity = curveFitParams[0] + curveFitParams[1]*exposure;
		
		double deviation = standardDev[0] + standardDev[1]*Math.sqrt(intensity);
		
		return (int) (deviation*deviation);
	}
	
	public double bestExposure() {
		/* 
		 *  This method estimates the best possible exposure value. The best possible exposure value
		 *  is the time in milliseconds where the average pixel intensity value of all pixels in an
		 *  image are at least 2 standard deviations below the saturation point of the camera.
		 */
		if (bestExp!=0.0) {
			return bestExp;
		}
		
		getFrameDeviation();
		
		int pos = 2;
		while (Math.abs(deviationSet[pos] - this.stdEst(intensitySet[pos]))/deviationSet[pos] < 0.05) {
			pos++;
			if (pos>=deviationSet.length) break;
		}
		pos--;

		CurveFitter cf = new CurveFitter(Arrays.copyOfRange(exposureSet, 0, pos), Arrays.copyOfRange(intensitySet, 0, pos));
		cf.doFit(0);
		double[] curveFitParams = cf.getParams();
		
		double bitDepth = Math.pow(2, core_.getImageBitDepth());
		bestExp = (bitDepth - 3*this.stdEst(bitDepth) - curveFitParams[0])/curveFitParams[1];
		bestExpIntensity = curveFitParams[0] + curveFitParams[1]*bestExp;
		
		System.out.println("Intercept: " + curveFitParams[0]);
		System.out.println("Slope: " + curveFitParams[1]);
		System.out.println("Max value: " + bitDepth);
		System.out.println("Estimated STD: " + this.stdEst(bitDepth));
		
		return bestExp;
	}
	
	public int bestExposureIntensity() {
		/*
		 *  This function returns the expected intensity value at the ideal exposure.
		 */
		if (bestExpIntensity==0.0) {
			bestExposure();
		}
		return (int) bestExpIntensity;
	}
	
	private double absSigma(int intensity) {
		/*  
		 *  This function gives an approximate standard deviation for an absorbance value based
		 *  on the input intensity value provided.
		 */
		double ln = Math.log(10);
		double dIntensity = (double) intensity;
		double dBestInt;
		double dSTD;
		if (nFrames==1) {
			dBestInt = intensitySet[0];
			dSTD = deviationSet[0];
		} else {
			dBestInt = bestExposureIntensity();
			dSTD = this.stdEst(dBestInt);
		}
		double sigmaI = Math.pow(this.stdEst(dIntensity)/(dIntensity*ln), 2);
		double sigmaIo = Math.pow(dSTD/(dBestInt*ln), 2);
		
		double sigmaA = Math.sqrt(sigmaI + sigmaIo);
		
		return sigmaA;
	}
		
	public int minConfPix(int numExp) {
		/*  
		 *  This function returns an integer value that corresponds to a minimum pixel intensity
		 *  over which the 95% confidence interval for an absorbance value will be at least 0.01
		 *  absorbance units. This is an estimate based on what the intensity at the best
		 *  exposure value should be.
		 *  
		 *  The standard deviation of absorbance values are calculated using propagation of error
		 *  estimated by taylor expansion.
		 */
		double sqrtN = Math.sqrt(numExp);
		double z = 1.96;
		int intensity;
		if (nFrames==1) {
			intensity = (int) intensitySet[0];
		} else {
			intensity = bestExposureIntensity();
		}
		double test = z*this.absSigma(intensity)/sqrtN;
		while (test < 0.01 && intensity>0) {
			test = z*this.absSigma(--intensity)/sqrtN;
		}
		
		return intensity;
	}

	// Performs linear regression on all pixels in an image - Last edit -> NJS 2015-08-28
	public ImagePlus pixelLinReg() {
		/* 
		 *  This functions performs a linear regression on the mean pixel intensity across
		 *  different exposures to give pixel intensity as a function of exposure time.
		 */
		if (nFrames<3) {
			nFrames = rawImage.getNFrames();
			if (nFrames<=1){
				IJ.error("nFrames<3, so an accurate linear regression cannot be determined.");
				return new ImagePlus();
			}
		}
		
		if (meanImage==null || meanImage.getStackSize()<nFrames) {
			getFrameDeviationAndMean(rawImage);
		}
		
		ImageStack slopeStats = new ImageStack(width,height,3);
		double[] intensityFit;
		double[] exposureFit;
		int flen = width*height;
		float[] sPixels = new float[flen]; //Holds slope values
		float[] iPixels = new float[flen]; //Holds y-intercept values
		float[] rPixels = new float[flen]; //Holds r^2 values
		float aIntercept = 0F; //Holds the mean y-intercept value
		float aSlope = 0F; //Holds the mean slope value
		float aR = 0F; //Holds the mean R^2 value
		CurveFitter cf;
		double[] curveFitParams = new double[2];
		
		int maxIntensity = 2;
		while (Math.abs(deviationSet[maxIntensity] - this.stdEst(intensitySet[maxIntensity]))/deviationSet[maxIntensity] < 0.05) {
			maxIntensity++;
			if (maxIntensity>=deviationSet.length) break;
		}
		maxIntensity--;
		
		System.out.println("Exposure positions: " + Integer.toString(intensitySet.length));
		System.out.println("Max exposure position: " + Integer.toString(maxIntensity));
		System.out.println("Error at " + exposureSet[maxIntensity] + "ms exposure: " + Math.abs(deviationSet[maxIntensity] - this.stdEst(intensitySet[maxIntensity]))/deviationSet[maxIntensity]);
		if (maxIntensity!=deviationSet.length-1) {
			System.out.println("Error at " + exposureSet[maxIntensity+1] + "ms exposure: " + Math.abs(deviationSet[maxIntensity+1] - this.stdEst(intensitySet[maxIntensity+1]))/deviationSet[maxIntensity+1]);
		}

		exposureFit = Arrays.copyOfRange(exposureSet, 0, maxIntensity);
		// This should loop should be optimized.
		for (int i=0; i<flen; i++) {
			intensityFit = Arrays.copyOfRange(intensitySet, 0, maxIntensity);

			cf = new CurveFitter(exposureFit,intensityFit);
			cf.doFit(0);
			curveFitParams = cf.getParams();
			iPixels[i] = (float) curveFitParams[0];
			sPixels[i] = (float) curveFitParams[1];
			rPixels[i] = (float) cf.getRSquared();

			aIntercept += iPixels[i];
			aSlope += sPixels[i];
			aR += rPixels[i];
		}
		aIntercept /= flen;
		aSlope /= flen;
		aR /= flen;

		slopeStats.setSliceLabel("Y-Intercept", 1);
		slopeStats.setPixels(iPixels, 1);

		slopeStats.setSliceLabel("Slope", 2);
		slopeStats.setPixels(sPixels, 2);

		slopeStats.setSliceLabel("R^2", 3);
		slopeStats.setPixels(rPixels, 3);

		this.slopeStats = slopeStats;
		this.averageIntercept = aIntercept;
		this.averageSlope = aSlope;
		this.averageR = aR;

		return new ImagePlus(this.name + this.channelLabel + " Slope Stats", slopeStats);

	}

	public Float getAverageSlope() {return averageSlope;}

	public Float getAverageR() {return averageR;}

	public Float getAverageIntercept() {return averageIntercept;}

	// Gets Absorption values from linear regression - Last edit -> NJS 2015-08-28
	public ImagePlus getAbsorbance(ImageStats slopeImage, ImagePlus foreground, ImageStats background) {
		FloatProcessor imageHolder = new FloatProcessor(width,height);
		getFrameMean();
		float[] fpixels = (float[]) foreground.getProcessor().getPixels();
		float[] bpixels = (float[]) background.getFrameMean().getProcessor().getPixels();
		float[] spixels;
		float[] apixels = (float[]) imageHolder.getPixels();
		int minPix = slopeImage.minConfPix(this.nSlices);
		int maxPix = (int) foreground.getStatistics().max;
		
		for (int j = 0; j<rawImage.getNFrames(); j++) {
			meanImage.setPosition(j+1);
			spixels = (float[]) meanImage.getProcessor().getPixels();
			for (int i=0; i<fpixels.length; i++) {
				if (spixels[i]>=minPix && spixels[i]<=maxPix && apixels[i]==0) {
					apixels[i] = (float) -Math.log10((spixels[i]-bpixels[i])/((fpixels[i]-bpixels[i])*(Math.pow(2, j))));
				}
			}
		}
		imageHolder.setPixels(apixels);
		absorbance = new ImagePlus(name,imageHolder);
		return absorbance;
	}
	
	public ImagePlus getAbsorbance(ImageStats foreground, ImageStats background) {
		/*****************************************************************************************
		 * This method performs determines the amount of absorption at each pixel for every		*
		 * 	channel in an image. An absorption value is obtained using the a blank foreground	*
		 * 	image using the normal formula for absorption -log(I/I0).							*
		 * 																						*
		 * Last Edit - Nick Schaub 2015-08-28													*
		 *****************************************************************************************/

		ImageStack slopeForeground = null;
		ImageStack slopeSample;
		FloatProcessor imageHolder = new FloatProcessor(width,height);
		float[] fpixels;
		float[] spixels;
		float[] apixels;
		int flen = foreground.width*foreground.height;

		slopeForeground = foreground.getSlopeImage();
		slopeSample = this.getSlopeImage();
		
		fpixels = (float[]) slopeForeground.getPixels(2);
		spixels = (float[]) slopeSample.getPixels(2);
		apixels = new float[flen];

		for (int j=0; j<flen; j++) {
			apixels[j] = (float) -Math.log10(spixels[j]/fpixels[j]);
		}

		imageHolder.setPixels(apixels);

		channelAbsorption = new ImagePlus(name,imageHolder);

		return channelAbsorption;
	}

	// Captures multiple images at various exposures and gets stats for each pixel (mean, std).
	private void getPixelExposureStats() {

		// Create object to handle the camera. This object is optimized to capture images at
		//	the fastest possible rate by the camera.
		IJ.log("getPixelExposureStats");
		SimpleCapture cap = new SimpleCapture(false);
		float oldDeviation = 0;
		float newDeviation = 0;

		// Images to temporarily hold captured or processed images.
		ImagePlus imstackTemp = IJ.createHyperStack("", width, height, 1, numReplicates, 40, imagebitdepth);
		ImagePlus imcaptureTemp = IJ.createImage("", width, height, 1, imagebitdepth);
		ImageStack meanStack = new ImageStack(width,height);
		ImageStack stdStack = new ImageStack(width,height);

		for (int i = 1; i<=10; i++) {

			//Capture images.
			int exp = (int) (Math.pow(2, i-1));
			imcaptureTemp = cap.seriesCapture("Exposure " + Double.toString(Math.pow(2, i)), exp, numReplicates);
			
			for (int j=1; j<=(numReplicates); j++) {
				imstackTemp.setPosition(1,j,i);
				imcaptureTemp.setPosition(1,j,1);
				imstackTemp.getProcessor().setPixels(imcaptureTemp.getProcessor().getPixels());
				imstackTemp.getStack().setSliceLabel(imcaptureTemp.getStack().getSliceLabel(imcaptureTemp.getCurrentSlice()), imstackTemp.getCurrentSlice());
			}

			//This section calculates the average of the replicates and the corresponding deviation
			//	at each pixel.
			stdStack.addSlice(Integer.toString(exp),
					getFrameDeviationAndMean(imcaptureTemp).getProcessor(),
					i-1);
			meanStack.addSlice(Integer.toString(exp),
					meanImage.getProcessor(),
					i-1);

			oldDeviation = newDeviation;
			newDeviation = getDeviationImageMean(stdImage.getProcessor());
			
			try {
				if (oldDeviation>newDeviation && i>1 && core_.getShutterOpen()) {
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		ImagePlus imMeanStats = new ImagePlus(channelLabel + " Mean",meanStack);
		ImagePlus imDeviationStats = new ImagePlus(channelLabel + " Standard Deviation",stdStack);
		int frames = imMeanStats.getNSlices();

		intensitySet = new double[frames];
		deviationSet = new double[frames];
		exposureSet = new double[frames]; //get range of exposure values and image mean pixel intensities
		maxPixelIntensity = new double[frames];
		minPixelIntensity = new double[frames];
		rawImage = IJ.createHyperStack(name+channelLabel, width, height, 1, numReplicates, frames, imagebitdepth);

		for (int i=0; i<frames; i++) {
			exposureSet[i] = (float) (Math.pow(2, i));
			int exposure = (int) (exposureSet[i]);
			imMeanStats.setPosition(i+1);
			imDeviationStats.setPosition(i+1);
			intensitySet[i] = getMeanImageMean(imMeanStats.getProcessor());
			deviationSet[i] = getDeviationImageMean(imDeviationStats.getProcessor());
			maxPixelIntensity[i] = (float) imMeanStats.getStatistics().max;
			minPixelIntensity[i] = (float) imMeanStats.getStatistics().min;
			for (int j = 0; j<numReplicates; j++) {
				rawImage.setPosition(1,j+1,i+1);
				imstackTemp.setPosition(1,j+1,i+1);
				rawImage.setProcessor(imstackTemp.getProcessor());
				rawImage.getStack().setSliceLabel(Integer.toString(exposure), rawImage.getCurrentSlice());
			}
		}
	}

	// Create and return a plot of global pixel intensity versus exposure
	public Plot plotGlobalIntensity() {
		Plot intensityPlot = new Plot(getChannelLabel(),"Exposure time (ms)","Intensity");
		double[] exposureRange = getExposureRange();
		double[] intensityRange = getGlobalIntensity();
		intensityPlot.setLimits(0, exposureRange[exposureRange.length-1]*1.25, 0, intensityRange[intensityRange.length-1]*1.25);
		intensityPlot.setColor(Color.RED);
		intensityPlot.addPoints(exposureRange,intensityRange, Plot.CROSS);
		return intensityPlot;
	}

	// Create and return a plot of global pixel deviation versus exposure
	public Plot plotGlobalDeviation() {
		Plot deviationPlot = new Plot(getChannelLabel(),"Exposure time (ms)","Deviation",getExposureRange(),getDeviationSet());
		return deviationPlot;
	}

	private float getMeanImageMean(ImageProcessor imp) {
		double fMean = 0;
		float[] fMeanPixels = (float[]) imp.getPixels();
		
		for (int i = 0; i<fMeanPixels.length; i++) {
			fMean += (double) fMeanPixels[i];
		}
		fMean /= (double) fMeanPixels.length;
		
		return (float) fMean;
	}
	
	private float getDeviationImageMean(ImageProcessor imp) {
		double fDeviation = 0;
		float[] fDeviationPixels = (float[]) imp.getPixels();
		
		for (int i = 0; i<fDeviationPixels.length; i++) {
			fDeviation += (double) fDeviationPixels[i]*fDeviationPixels[i];
		}
		fDeviation /= (double) fDeviationPixels.length;
		fDeviation = Math.sqrt(Math.abs(fDeviation));
		
		return (float) fDeviation;
	}
	
	private ImagePlus getFrameDeviationAndMean(ImagePlus imp) {

		stdImage = new ImagePlus();
		int frames = imp.getNFrames();
		int replicates = imp.getNSlices();
		meanImage = IJ.createImage("", width, height, frames, 32);
		ImageStack meanStack = new ImageStack(width,height);
		ImageStack stdStack = new ImageStack(width,height);
		
		int flen = width*height;

		for (int i=1; i<=frames; i++) {
			float[] tpixel = new float[flen];
			float[] fpixelmean = new float[flen];
			double[] dpixelmean = new double[flen];
			float[] fpixeldeviation = new float[flen];
			double[] dpixeldeviation = new double[flen];
			for (int j=1; j<=(replicates); j++) { //loop to calculate the mean
				imp.setPosition(1,j,i);
				tpixel = (float[]) imp.getProcessor().convertToFloat().getPixels();
				for (int k=0; k<flen; k++) {
					dpixeldeviation[k] += tpixel[k]*tpixel[k];
					dpixelmean[k] += tpixel[k];
				}
			}

			for (int j = 0; j<flen; j++) {
				dpixelmean[j] /= (double) replicates;
				dpixeldeviation[j] /= (double) replicates;
				dpixeldeviation[j] = Math.sqrt(dpixeldeviation[j] - dpixelmean[j]*dpixelmean[j]);
				fpixelmean[j] = (float) dpixelmean[j];
				fpixeldeviation[j] = (float) dpixeldeviation[j];
			}
			stdStack.addSlice(imp.getImageStack().getSliceLabel(imp.getCurrentSlice()),
					new FloatProcessor(width,height,fpixeldeviation),
					i-1);
			
			meanStack.addSlice(imp.getImageStack().getSliceLabel(imp.getCurrentSlice()),
					new FloatProcessor(width,height,fpixelmean),
					i-1);
		}
		
		stdImage = new ImagePlus(name,stdStack);
		meanImage = new ImagePlus(name,meanStack);
		
		maxPixelIntensity = new double[frames];
		minPixelIntensity = new double[frames];
		intensitySet = new double[frames];
		deviationSet = new double[frames];
		for (int i=0; i<frames; i++){
			meanImage.setPosition(i+1);
			stdImage.setPosition(i+1);
			maxPixelIntensity[i] = (float) meanImage.getStatistics().max;
			minPixelIntensity[i] = (float) meanImage.getStatistics().min;
			intensitySet[i] = getMeanImageMean(meanImage.getProcessor());
			deviationSet[i] = getDeviationImageMean(stdImage.getProcessor());
		}
		
		return stdImage;
	}
	
	public ImagePlus getFrameDeviation() {
		if (stdImage!=null && stdImage.getNFrames()==nFrames) {
			return stdImage;
		}
		return getFrameDeviationAndMean(rawImage);
	}

	public ImagePlus getFrameMean() {
		if (meanImage!=null && meanImage.getNFrames()==nFrames) {
			return meanImage;
		}
		getFrameDeviationAndMean(rawImage);
		return meanImage;
	}

	public String getName() {return name;}

	private String getChannelLabel() {return channelLabel;}

	private double[] getExposureRange() {
		if (meanImage==null) {
			getFrameMean();
		}
		exposureSet = new double[nFrames];
		for (int i = 0; i<nFrames; i++) {
			exposureSet[i] = Float.parseFloat(meanImage.getImageStack().getSliceLabel(i+1));
		}
		return exposureSet;
	}

	public double[] getGlobalIntensity() {return intensitySet;}

	// This method returns the global deviation values at each exposure for the indicated channel.
	public double[] getDeviationSet() {return deviationSet;}

	// This method returns the ImageStack containing the results of the linear regression for the indicated channel.
	public ImageStack getSlopeImage() {
		return slopeStats;
	}
	
	public double stdEst(double intensity) {
		/* 
		 *  This function returns an estimate of what the standard deviation should be for an input
		 *  intensity.
		 * 
		 *  The estimation is based on a linear regression of pixel intensities where the regression is std = a*sqrt(I) + b. Pixel intensities from
		 *  images at three different exposures are used for this regression.
	 	 */
		if (standardDev==null) {
			if (meanImage==null || meanImage.getNSlices()!=nFrames) {
				getFrameDeviationAndMean(rawImage);
			}
			if (nFrames<3) {
				IJ.error("Need at least 3 exposure times to estimate standard deviation.");
				return 0.0;
			}

			double[] dSqrtPixels = new double[3];
			double[] dStdPixels = new double[3];
			for (int i = 0; i<3; i++) {
				dSqrtPixels[i] = Math.sqrt(intensitySet[i]);
				dStdPixels[i] = deviationSet[i];
			}
			CurveFitter cf = new CurveFitter(dSqrtPixels,dStdPixels);
			cf.doFit(0);
			standardDev = cf.getParams();
			rSqr = cf.getRSquared();
		}

		return Math.sqrt(intensity)*standardDev[1] + standardDev[0];
	}
}