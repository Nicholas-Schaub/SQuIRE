package nist.quantitativeabsorbance;

import java.awt.Color;

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

	// Variables for statistics
	public float averageIntercept;
	public float averageSlope;
	public float averageR;

	// Variables for plotting global statistics
	public float[] exposureSet;
	public float[] intensitySet;
	public float[] deviationSet;
	public float[] maxPixelIntensity;
	public float[] minPixelIntensity;

	// Basic image and capture settings.
	public String name;
	public String channelLabel;
	public int width;
	public int height;
	public int bitdepth;
	public int imagebitdepth;
	private double minExposure = AppParams.getMinExposure();
	private int numReplicates = AppParams.getNumReplicates();
	private double maxExposure = AppParams.getMaxExposure();
	public final int nFrames;
	public final int nSlices;
	public final int nChannels;

	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	// Call this function to perform statistics on an ImagePlus object.
	public ImageStats(ImagePlus imp) {
		name = imp.getTitle();
		channelLabel = "";
		
		// Core and image attributes
		core_ = AppParams.getCore_();
		width = (int) core_.getImageWidth();
		height = (int) core_.getImageHeight();
		bitdepth = (int) core_.getImageBitDepth();

		// Make sure the bit depth is something ImageJ can handle
		if ((bitdepth == 12) || (bitdepth==14)) {imagebitdepth=16;} else {imagebitdepth=bitdepth;}

		rawImage = imp;
		nFrames = rawImage.getNFrames();
		nSlices = rawImage.getNSlices();
		nChannels = rawImage.getNChannels();
	}
	
	// Direct calls to this instantiation is reserved for benchmarking cameras.
	protected ImageStats(String sample, String channel) {
		this(new ImagePlus());
		// Sample/channel label
		name = sample;
		channelLabel = channel;
		
		if (rawImage==null) {
			System.out.println("Running pixel exposure statistics...");
			getPixelExposureStats(AppParams.getForceMax());
		}
	}

	// Performs linear regression on all pixels in an image - Last edit -> NJS 2015-08-28
	public ImagePlus pixelLinReg(ImageStats foreground, ImageStats background) {
		/*****************************************************************************************
		 * This method performs a linear regression on the intensity of each pixel in an image   *
		 *	as a function of exposure time.														*
		 *																						*
		 * It is assumed that pixel intensities in an image near the pixel intensity values of	*
		 * 	the background are non-linear, and the same assumption about non-linearity is made	*
		 * 	about pixel intensity values in an image near the saturation point. To make sure	*
		 * 	that linear regression is performed on the linear section of the intensity/exposure	*
		 * 	function, intensity values that are within 3 standard deviations of the maximum or	*
		 * 	minimum values of pixel intensity are excluded from the regression					*
		 * 																						*
		 * Last Edit - Nick Schaub 2015-08-28													*
		 *****************************************************************************************/
		if (nFrames<=1) {
			IJ.error("nFrames<=1, so I can't perform a linear regression.");
			return new ImagePlus();
		}
		
		if (background.meanImage==null) {
			background.getFrameMean();
		}
		
		if (background.meanImage.getNFrames()<nFrames) {
			background.getFrameDeviation();
			if (background.meanImage.getNSlices()<nFrames) {
				IJ.error("Background frames smaller than image frames. \n Need more background frames for linear regression.");
				return new ImagePlus();
			}
		}
		
		if (meanImage==null || meanImage.getStackSize()<nFrames) {
			getFrameDeviationAndMean(rawImage);
		}
		
		ImageStack slopeStats = new ImageStack(width,height,3);
		int zlen = meanImage.getStackSize();
		int forelen = foreground.meanImage.getNSlices();
		int backlen = background.meanImage.getNSlices();
		float[] dIntensity = new float[zlen];
		double[] intensityFit;
		float[] exposureRange = this.getExposureRange();
		double[] exposureFit;
		int flen = width*height;
		float[] cPixels = new float[flen]; //Holds pixel values of the image to perform regression on
		float[] bPixels = new float[flen]; //Holds pixel values of the background image
		float[] sPixels = new float[flen]; //Holds slope values
		float[] iPixels = new float[flen]; //Holds y-intercept values
		float[] rPixels = new float[flen]; //Holds r^2 values
		float aIntercept = 0F; //Holds the mean y-intercept value
		float aSlope = 0F; //Holds the mean slope value
		float aR = 0F; //Holds the mean R^2 value
		CurveFitter cf;
		double[] curveFitParams = new double[2];
		int j = 0;
		int k = 0;
		
		int maxIntensity = forelen;
/*		for (int i = forelen; i>1; i--) {
			System.out.println(i);
			System.out.println(deviationSet[i-1]);
			if (foreground.deviationSet[i-1]<foreground.deviationSet[i-2]) {
				maxIntensity = i;
			}
		}*/

		System.out.println("Exposure positions: " + Integer.toString(forelen));
		System.out.println("Max exposure position: " + Integer.toString(maxIntensity));
		
		// Determine the range of values accepted for linear regression
		double maxValue = foreground.maxPixelIntensity[maxIntensity-1] - (3*foreground.deviationSet[maxIntensity-2]);
		double minValue = background.maxPixelIntensity[backlen-1] + (3*background.deviationSet[backlen-1]);

		System.out.println("Maximum pixel intensity: " + Double.toString(foreground.maxPixelIntensity[maxIntensity-1]));
		System.out.println("Maximum pixel deviation: " + Double.toString(foreground.deviationSet[maxIntensity-2]));
		System.out.println("Maximum allowed intensity: " + Double.toString(maxValue));
		System.out.println("Minimum allowed intensity: " + Double.toString(minValue));

		// This should loop should be optimized.
		for (int i=0; i<flen; i++) {
			dIntensity = new float[zlen];
			j = 0;
			k = 0;
			for (int m = 0; m<zlen; m++) {
				background.meanImage.setPosition(m+1);
				meanImage.setPosition(m+1);
				bPixels = (float[]) background.meanImage.getProcessor().getPixels();
				cPixels = (float[]) meanImage.getProcessor().getPixels();

				dIntensity[m] = (cPixels[i] - bPixels[i]);
				if (cPixels[i]>maxValue) {
					break;
				} else if (cPixels[i]<minValue) {
					k++;
				}
				j = m;
			}
			
			if (j<k) {
				IJ.log("Error in linear regression bounds estimation.\n" +
						"This probably means the frames aren't in order of increasing exposure, \n" +
						"or the background signal distribution overlaps the foreground signal distribution.");
				IJ.log("Maximum foreground intensity: " + Float.toString(maxPixelIntensity[forelen-1]));
				IJ.log("Maximum background intensity: " + Float.toString(background.maxPixelIntensity[forelen-1]));
				IJ.log("Upper Intensity Bound: " + Double.toString(maxValue));
				IJ.log("Lower Intesity Bound: " + Double.toString(minValue));
				return new ImagePlus();
			}
			
			exposureFit = new double[j-k];
			intensityFit = new double[j-k];

			for (int l=0; l<(j-k); l++) {
				exposureFit[l] = exposureRange[l+k];
				intensityFit[l] = dIntensity[l+k];
			}

			cf = new CurveFitter(exposureFit,intensityFit);
			cf.doFit(0);
			curveFitParams = cf.getParams();
			iPixels[i] = (float) curveFitParams[0];
			sPixels[i] = (float) curveFitParams[1];
			rPixels[i] = (float) cf.getRSquared();

			aIntercept += iPixels[i]/flen;
			aSlope += sPixels[i]/flen;
			aR += rPixels[i]/flen;
		}

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

	public Float getAverageSlope(int channel) {return averageSlope;}

	public Float getAverageR(int channel) {return averageR;}

	public Float getAverageIntercept(int channel) {return averageIntercept;}

	// Gets Absorption values from linear regression - Last edit -> NJS 2015-08-28
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
	private void getPixelExposureStats(boolean forceMax) {

		// Create object to handle the camera. This object is optimized to capture images at
		//	the fastest possible rate by the camera.
		SimpleCapture cap = new SimpleCapture(false);
		float oldDeviation = 0;
		float newDeviation = 0;

		// Images to temporarily hold captured or processed images.
		ImagePlus imstackTemp = IJ.createHyperStack("", width, height, 1, numReplicates, 40, imagebitdepth);
		ImagePlus imcaptureTemp = IJ.createImage("", width, height, 1, imagebitdepth);
		ImageStack meanStack = new ImageStack(width,height);
		ImageStack stdStack = new ImageStack(width,height);

		if (AppParams.hasAutoShutter() && AppParams.useAutoShutter() && !forceMax) {
			try {
				core_.setShutterOpen(true);
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (int i = 1; i<=40; i++) {

			//Capture images.
			int exp = (int) (minExposure*Math.pow(2, i-1));
			imcaptureTemp = cap.seriesCapture("Exposure " + Double.toString(minExposure * Math.pow(2, i)), exp, numReplicates);
			
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

			if (forceMax) {
				if (minExposure*Math.pow(2, i)>maxExposure) {
					break;
				}
			} else if ((minExposure*Math.pow(2, i))>maxExposure) {
				break;
			} else if (oldDeviation>newDeviation && i>1) {
				break;
			}

		}

		if (AppParams.hasAutoShutter() && AppParams.useAutoShutter() && !forceMax) {
			try {
				core_.setShutterOpen(false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ImagePlus imMeanStats = new ImagePlus(channelLabel + " Mean",meanStack);
		ImagePlus imDeviationStats = new ImagePlus(channelLabel + " Standard Deviation",stdStack);
		int frames = imMeanStats.getNSlices();

		intensitySet = new float[frames];
		deviationSet = new float[frames];
		exposureSet = new float[frames]; //get range of exposure values and image mean pixel intensities
		maxPixelIntensity = new float[frames];
		minPixelIntensity = new float[frames];
		rawImage = IJ.createHyperStack(name+channelLabel, width, height, 1, numReplicates, frames, imagebitdepth);

		for (int i=0; i<frames; i++) {
			exposureSet[i] = (float) (minExposure*Math.pow(2, i));
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
		float[] exposureRange = getExposureRange();
		float[] intensityRange = getGlobalIntensity();
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
		float fMean = 0;
		float[] fMeanPixels = (float[]) imp.getPixels();
		
		for (int i = 0; i<fMeanPixels.length; i++) {
			fMean += fMeanPixels[i];
		}
		fMean /= (float) fMeanPixels.length;
		
		return fMean;
	}
	
	private float getDeviationImageMean(ImageProcessor imp) {
		// TODO this needs to be corrected.
		double fDeviation = 0;
		float[] fDeviationPixels = (float[]) imp.getPixels();
		
		for (int i = 0; i<fDeviationPixels.length; i++) {
			fDeviation += (double) fDeviationPixels[i];//*fDeviationPixels[i];
			System.out.println("Current fDeviation sum: " + Double.toString(fDeviation));
		}
		System.out.println("fDeviation sum = " + Double.toString(fDeviation));
		fDeviation /= (double) fDeviationPixels.length;
		//fDeviation = Math.sqrt(Math.abs(fDeviation));
		
		System.out.println("getDeviationImageMean = " + Double.toString(fDeviation));
		
		return (float) fDeviation;
	}
	
	private ImagePlus getFrameDeviationAndMean(ImagePlus imp) {

		stdImage = new ImagePlus();
		int frames = imp.getNFrames();
		meanImage = IJ.createImage("", width, height, frames, 32);
		ImageStack meanStack = new ImageStack(width,height);
		ImageStack stdStack = new ImageStack(width,height);
		
		int flen = width*height;

		for (int i=1; i<=frames; i++) {
			float[] tpixel = new float[flen];
			float[] fpixelmean = new float[flen];
			float[] fpixeldeviation = new float[flen];
			for (int j=1; j<=(numReplicates); j++) { //loop to calculate the mean
				imp.setPosition(1,j,i);
				tpixel = (float[]) imp.getProcessor().convertToFloat().getPixels();
				for (int k=0; k<flen; k++) {
					fpixeldeviation[k] += tpixel[k]*tpixel[k];
					fpixelmean[k] += tpixel[k];
				}
			}

			for (int j = 0; j<flen; j++) {
				fpixelmean[j] /= (float) numReplicates;
				fpixeldeviation[j] /= (float) numReplicates;
				fpixeldeviation[j] = (float) Math.sqrt(fpixeldeviation[j] - fpixelmean[j]*fpixelmean[j]);
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
		
		maxPixelIntensity = new float[frames];
		minPixelIntensity = new float[frames];
		deviationSet = new float[frames];
		for (int i=0; i<frames; i++){
			meanImage.setPosition(i+1);
			stdImage.setPosition(i+1);
			maxPixelIntensity[i] = (float) meanImage.getStatistics().max;
			minPixelIntensity[i] = (float) meanImage.getStatistics().min;
			deviationSet[i] = (float) stdImage.getStatistics().mean;
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

	private float[] getExposureRange() {
		if (meanImage==null) {
			getFrameMean();
		}
		exposureSet = new float[nFrames];
		for (int i = 0; i<nFrames; i++) {
			exposureSet[i] = Float.parseFloat(meanImage.getImageStack().getSliceLabel(i+1));
		}
		return exposureSet;
	}

	public float[] getGlobalIntensity() {return intensitySet;}

	// This method returns the global deviation values at each exposure for the indicated channel.
	public float[] getDeviationSet() {return deviationSet;}

	// This method returns the ImageStack containing the results of the linear regression for the indicated channel.
	public ImageStack getSlopeImage() {
		return slopeStats;
	}
	
	public float[] pseudoPoisson() {
		if (meanImage==null || meanImage.getNSlices()!=nFrames) {
			getFrameDeviationAndMean(rawImage);
		}
		int len = width*height;
		int vol = len*(nFrames-1);
		float[] iPixels = new float[len];
		float[] dPixels = new float[len];
		double[] iRegPixels = new double[vol];
		double[] dRegPixels = new double[vol];
		for (int i=1;i<=nFrames-1;i++) {
			meanImage.setPosition(i);
			stdImage.setPosition(i);
			iPixels = (float[]) meanImage.getProcessor().getPixels();
			dPixels = (float[]) stdImage.getProcessor().getPixels();
			for (int j=0;j<len;j++) {
				iRegPixels[(i-1)*len+j] = iPixels[j];
				dRegPixels[(i-1)*len+j] = dPixels[j];
			}
		}
		CurveFitter cf = new CurveFitter(iRegPixels,dRegPixels);
		cf.doFit(0);
		double[] curveFitParams = cf.getParams();
		float[] params = new float[3];
		params[0] = (float) curveFitParams[0];
		params[1] = (float) curveFitParams[1];
		params[2] = (float) cf.getRSquared();
		return params;
	}

}