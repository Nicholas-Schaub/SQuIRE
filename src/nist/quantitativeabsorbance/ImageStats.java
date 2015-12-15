package nist.quantitativeabsorbance;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import ij.process.FloatProcessor;
import mmcorej.CMMCore;

//This class holds images representing the statistical information for each pixel collected
//	in multiple replicates. The mean intensity stack contains images that are the mean
//	pixel intensity from multiple images collected at the same exposure. The deviation
//	stack holds the corresponding deviation values at each pixel. Finally, the raw
//	intensity stack holds all of the raw data used to collect the other values.

// Updated NJS 2015-12-02
// Will now save all raw images if specified in the Control Panel.
public class ImageStats {

	   // ArrayLists that hold individual channel information
	   public ArrayList<ImageStack> mean;
	   public ArrayList<ImageStack> deviation;
	   public ArrayList<ImageStack> slopeStats;
	   public ArrayList<ImagePlus> absorbance;
	   public ArrayList<Float> averageIntercept;
	   public ArrayList<Float> averageSlope;
	   public ArrayList<Float> averageR;
	   public ArrayList<double[]> exposureSet;
	   public ArrayList<double[]> intensitySet;
	   public ArrayList<double[]> deviationSet;
	   public ArrayList<String> channelLabels;
	   public ArrayList<ImageStack> imageStatsStack;
	   public ArrayList<int[]> maxPixelIntensity;
	   public ArrayList<int[]> minPixelIntensity;
	   public ArrayList<int[]> maxPixelDeviation;
	   public ArrayList<int[]> minPixelDeviation;
	   
	   public ImageStack channelAbsorption = null;
	   
	   // Basic image and capture settings.
	   public String name = AppParams.getPlateID();
	   public int width;
	   public int height;
	   public int bitdepth;
	   public int imagebitdepth;
	   public double minExposure = AppParams.getMinExposure();
	   public int numReplicates = AppParams.getNumReplicates();
	   public double maxExposure = AppParams.getMaxExposure();

	   // Provides access to the Micro-Manager Core API (for direct hardware
	   // control)
	   private CMMCore core_;

	   public ImageStats() {
		   this.name += "-"+AppParams.getCurrentSampleName();
		   this.core_ = AppParams.getCore_();
		   this.width = (int) core_.getImageWidth();
		   this.height = (int) core_.getImageHeight();
		   this.bitdepth = (int) core_.getImageBitDepth();
		   
		   if ((bitdepth == 12) || (bitdepth==14)) {imagebitdepth=16;} else {imagebitdepth=bitdepth;}
		   
		   this.mean = new ArrayList<ImageStack>();
		   this.deviation = new ArrayList<ImageStack>();
		   this.exposureSet = new ArrayList<double[]>();
		   this.intensitySet = new ArrayList<double[]>();
		   this.deviationSet = new ArrayList<double[]>();
		   this.channelLabels = new ArrayList<String>();
		   this.slopeStats = new ArrayList<ImageStack>();
		   this.averageIntercept = new ArrayList<Float>();
		   this.averageSlope = new ArrayList<Float>();
		   this.averageR = new ArrayList<Float>();
		   absorbance = new ArrayList<ImagePlus>();
		   imageStatsStack= new ArrayList<ImageStack>();
		   maxPixelIntensity = new ArrayList<int[]>();
		   minPixelIntensity = new ArrayList<int[]>();
		   maxPixelDeviation = new ArrayList<int[]>();
		   minPixelDeviation = new ArrayList<int[]>();
		   
		   this.channelLabels.add(this.name);
		   getPixelExposureStats(AppParams.getForceMax());
	   }
	   
	   // Performs linear regression on all pixels in an image - Last edit -> NJS 2015-08-28
	   public ImagePlus pixelLinReg(int imageChannel, ImageStats foreground, ImageStats background) {
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
		   
		   ImagePlus imageStack = new ImagePlus(name + " Slope Image", this.mean.get(imageChannel));
		   ImageStack slopeStats = new ImageStack(imageStack.getWidth(),imageStack.getHeight(),3);
		   int zlen = imageStack.getStackSize();
		   int forelen = foreground.maxPixelIntensity.get(imageChannel).length;
		   double[] dIntensity = new double[zlen];
		   double[] intensityFit;
		   double[] exposureRange = this.getExposureRange(imageChannel);
		   double[] exposureFit;
		   int flen = imageStack.getWidth()*imageStack.getHeight();
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
		   
		   double maxValue = foreground.maxPixelIntensity.get(imageChannel)[forelen-1]-3*foreground.maxPixelDeviation.get(imageChannel)[forelen-1];
		   double minValue = 3*background.maxPixelDeviation.get(imageChannel)[zlen-1];
		   
		   // This should loop should be optimized.
		   for (int i=0; i<flen; i++) {
			   dIntensity = new double[zlen];
			   j = 0;
			   k = 0;
			   for (int m = 0; m<zlen; m++) {
				   bPixels = (float[]) background.mean.get(imageChannel).getPixels(m+1);
				   imageStack.setPosition(m+1);
				   cPixels = (float[]) imageStack.getProcessor().getPixels();
				   
				   dIntensity[m] = (cPixels[i] - bPixels[i]);
				   if (cPixels[i]>maxValue) {
					   break;
				   } else if (cPixels[i]<minValue) {
					   k++;
				   }
				   j = m;
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
		   
		   this.slopeStats.add(imageChannel, slopeStats);
		   this.averageIntercept.add(imageChannel, aIntercept);
		   this.averageSlope.add(imageChannel, aSlope);
		   this.averageR.add(imageChannel, aR);
		   
		   return new ImagePlus(this.name + this.channelLabels + " Slope Stats", slopeStats);

	   }

	   public Float getAverageSlope(int channel) {return this.averageSlope.get(channel);}
	   
	   public Float getAverageR(int channel) {return this.averageR.get(channel);}
	   
	   public Float getAverageIntercept(int channel) {return this.averageIntercept.get(channel);}
	   
	   // Gets Absorption values from linear regression - Last edit -> NJS 2015-08-28
	   public ImageStack getAbsorbance(ImageStats foreground) {
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
		   this.channelAbsorption = new ImageStack(foreground.width, foreground.height);
		   
		   slopeForeground = foreground.getSlopeImage(0);
		   slopeSample = this.getSlopeImage(0);

		   fpixels = (float[]) slopeForeground.getPixels(2);
		   spixels = (float[]) slopeSample.getPixels(2);
		   apixels = new float[flen];

		   for (int j=0; j<flen; j++) {
			   apixels[j] = (float) -Math.log10(spixels[j]/fpixels[j]);
		   }

		   imageHolder.setPixels(apixels);

		   this.channelAbsorption.addSlice(this.channelLabels.get(0),imageHolder,0);
		   
		   return this.channelAbsorption;
	   }
	   
	   // Captures multiple images at various exposures and gets stats for each pixel (mean, std).
	   public void getPixelExposureStats(boolean forceMax) {
		   
		  //This code was written to be functional, and can probably be optimized.
		   
	      float oldDeviation = 1;
	      float newDeviation = 2;
		   
	      ImagePlus imstackTemp = IJ.createHyperStack("", width, height, 1, 40, numReplicates, imagebitdepth);
	      ImagePlus imcaptureTemp = IJ.createImage("", width, height, 1, imagebitdepth);
	      ImageStack imMean = ImageStack.create(width, height, 1, imagebitdepth);
	      ImageStack imDeviation = ImageStack.create(width, height, 1, imagebitdepth);
	      
	      FloatProcessor imageHolder = new FloatProcessor(width,height);
	      float[] fpixelmean = (float[]) imageHolder.getPixels();
	      float[] fpixeldeviation = fpixelmean;
	      float[] tpixel = fpixelmean;
	      int flen = fpixelmean.length;
		  
	      if (AppParams.hasAutoShutter() && AppParams.useAutoShutter() && !forceMax) {
			  try {
				core_.setShutterOpen(true);
				Thread.sleep(100);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
	      
    	  for (int i = 1; i<=40; i++) {
    		  
    		  //Capture numaverage+1 images. This appears to be required since there may be a bug
    		  //	that prevents the appropriate number of images being saved to an ImagePlus
    		  //	hyperstack.
    		  
    		  for (int j=1; j!=(numReplicates+1); j++) {
    			  int exp = (int) (minExposure*Math.pow(2, i-1));
    			  imcaptureTemp = capture("Exposure " + Double.toString(minExposure * Math.pow(2, i)) + "ms - Capture " + Integer.toString(j),exp);
    			  
    			  // Saves every image capture if the control panel indicates "Save Raw Images"
    			  /*if (AppParams.isSaveRawImages()) {
    				  IJ.saveAsTiff(imcaptureTemp,
    					  AppParams.getOutDir()+"Raw Images"+File.separator+AppParams.getCurrentSampleName()+"-Exp"+Integer.toString(exp)+"-Rep"+Integer.toString(j));
    			  }*/
    			  
    			  imstackTemp.setPosition(1,i,j);
				  imstackTemp.getProcessor().setPixels(imcaptureTemp.getProcessor().getPixels());
    		  }
    		  
    		  //This section calculates the average of the replicates and the corresponding deviation
    		  //	at each pixel.
			  fpixelmean = new float[flen];
			  fpixeldeviation = new float[flen];
    		  for (int j=1; j!=(numReplicates+1); j++) { //loop to calculate the mean
    			  imstackTemp.setPosition(1,i,j);
    			  tpixel = (float[]) imstackTemp.getProcessor().convertToFloat().getPixels();
    			  for (int k=0; k<flen; k++) {
    				  fpixelmean[k] += (tpixel[k] / ((float) numReplicates));
    			  }
    		  }
    		  
    		  for (int j=1; j!=(numReplicates+1); j++) { //loop to calculate the deviation
    			  imstackTemp.setPosition(1,i,j);
    			  tpixel = (float[]) imstackTemp.getProcessor().convertToFloat().getPixels();
    			  for (int k=0; k<flen; k++) {
    				  fpixeldeviation[k] += Math.abs(tpixel[k] - fpixelmean[k]) / ((float) numReplicates);
    			  }
    		  }
    		  
    		  imageHolder.setPixels(fpixelmean);
    		  imMean.addSlice(Integer.toString((int) (minExposure*Math.pow(2, i-1))),imageHolder,i-1);
    		  
    		  imageHolder.setPixels(fpixeldeviation);
    		  imDeviation.addSlice(Integer.toString((int) (minExposure*Math.pow(2, i-1))),imageHolder,i-1);
    	      
    	      newDeviation = 0;
    	      for (int j = 0; j<flen; j++) newDeviation += fpixeldeviation[j] / ((float) flen);
    	      
    	      if (forceMax) {
    	    	  if (minExposure*Math.pow(2, i)>maxExposure) {
    	    		  break;
    	    	  }
	    	  } else if ((minExposure*Math.pow(2, i))>maxExposure) {
    	    	  break;
    	      } else if (oldDeviation<newDeviation) {
    	    	  oldDeviation = newDeviation;
    	      } else {
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
    	  
    	  imMean.deleteLastSlice();
    	  imDeviation.deleteLastSlice();
    	  
    	  ImagePlus imMeanStats = new ImagePlus(this.channelLabels.get(this.channelLabels.size()-1) + " Mean",imMean);
    	  ImagePlus imDeviationStats = new ImagePlus(this.channelLabels.get(this.channelLabels.size()-1) + " Standard Deviation",imDeviation);
    	  
    	  double[] meanSet = new double[imMean.getSize()];
    	  double[] deviationSet = new double[imMean.getSize()];
    	  double[] exposureSet = new double[imMean.getSize()]; //get range of exposure values and image mean pixel intensities
    	  int[] max = new int[imMean.getSize()];
    	  int[] min = new int[imMean.getSize()];
    	  int[] maxDeviation = new int[imMean.getSize()];
    	  int[] minDeviation = new int[imMean.getSize()];
    	  
    	  for (int i=0; i<imMean.getSize(); i++) {
    		  exposureSet[i] = (minExposure*Math.pow(2, i));
    		  imMeanStats.setPosition(i+1);
    		  meanSet[i] = imMeanStats.getStatistics().mean;
    		  deviationSet[i] = imMeanStats.getStatistics().stdDev;
    		  max[i] = (int) imMeanStats.getStatistics().max;
    		  min[i] = (int) imMeanStats.getStatistics().min;
    		  maxDeviation[i] = (int) imDeviationStats.getStatistics().max;
    		  minDeviation[i] = (int) imDeviationStats.getStatistics().min;
    	  }
		  
    	  this.maxPixelIntensity.add(max);
    	  this.minPixelIntensity.add(min);
    	  this.maxPixelDeviation.add(maxDeviation);
    	  this.minPixelDeviation.add(minDeviation);
    	  this.mean.add(imMean);
    	  this.deviation.add(imDeviation);
    	  this.exposureSet.add(exposureSet);
    	  this.intensitySet.add(meanSet);
    	  this.deviationSet.add(deviationSet);
    	  
    	  
	   }
	   
	   // Captures a single image and returns an ImagePlus image.
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
   
	   // Create and return a plot of global pixel intensity versus exposure
	   public Plot plotGlobalIntensity(int channel) {
		   Plot intensityPlot = new Plot(this.getChannelLabel(channel),"Exposure time (ms)","Intensity");
		   double[] exposureRange = this.getExposureRange(channel);
		   double[] intensityRange = this.getGlobalIntensity(channel);
		   double[] deviationRange = this.getDeviationSet(channel);
		   intensityPlot.setLimits(0, exposureRange[exposureRange.length-1]*1.25, 0, intensityRange[intensityRange.length-1]*1.25);
		   intensityPlot.setColor(Color.RED);
		   intensityPlot.addPoints(exposureRange,intensityRange, deviationRange, Plot.CROSS);
		   return intensityPlot;
	   }
	   
	   // Create and return a plot of global pixel deviation versus exposure
	   public Plot plotGlobalDeviation(int channel) {
		   Plot deviationPlot = new Plot(this.getChannelLabel(channel),"Exposure time (ms)","Deviation",this.getExposureRange(channel),this.getDeviationSet(channel));
		   return deviationPlot;
	   }
	   
	   
	   public ImageStack getMeanImageStack(int channel) {
		   return mean.get(channel);
	   }
	   
	   public ImageStack getDeviationImageStack(int channel) {
		   return deviation.get(channel);
	   }
	   
	   public String getName() {
		   return name;
	   }

	   public String getChannelLabel(int channel) {
		   return channelLabels.get(channel);
	   }
	   
	   public double[] getExposureRange(int channel) {
		   return exposureSet.get(channel);
	   }

	   public double[] getGlobalIntensity(int channel) {
		   return intensitySet.get(channel);
	   }
	   
	   // This method returns the global deviation values at each exposure for the indicated channel.
	   public double[] getDeviationSet(int channel) {
		   return deviationSet.get(channel);
	   }
	   
	   // This method returns the ImageStack containing the results of the linear regression for the indicated channel.
	   public ImageStack getSlopeImage(int channel) {
		   return this.slopeStats.get(channel);
	   }

}