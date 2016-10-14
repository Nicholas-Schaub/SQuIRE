package nist.quantitativeabsorbance;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JOptionPane;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.measure.ResultsTable;
import ij.process.ColorProcessor;


public class BenchmarkingThread implements Runnable {
	ResultsTable benchmarkingResults = new ResultsTable();

	public void run() {
		AppParams params = AppParams.getInstance();
		ImageStats currentSample;
		double benchmarkAbsorption = 0;
		double currentSlope;
		ArrayList<Double> timePoint = new ArrayList<Double>();
		ArrayList<Double> slope = new ArrayList<Double>();
		ArrayList<Double> absorption = new ArrayList<Double>();
		ArrayList<Double> previousSlope = new ArrayList<Double>();
		ArrayList<Double> previousAbsorption = new ArrayList<Double>();
		
		Plot slopePlot = new Plot("Benchmarking","Number of Samples","Average Slope (intensity/exposure time)");
		Plot absorptionPlot = new Plot("Absorption","Number of Samples","Absorption");
		ImagePlus plotAggregator = new ImagePlus("Benchmarking Plot");
		
		int width = slopePlot.getProcessor().getWidth();
		int height = slopePlot.getProcessor().getHeight();
		
		ColorProcessor aggregateProcessor = new ColorProcessor(2*width,height);
		aggregateProcessor.insert(slopePlot.getProcessor(), 0, 0);
		aggregateProcessor.insert(absorptionPlot.getProcessor(),width,0);
		plotAggregator.setProcessor(aggregateProcessor);
		
		(new File(AppParams.getOutDir())).mkdirs();
		
		if (AppParams.getBenchmarkVisible()) {
			plotAggregator.show();
		}
		
		try {
			AppParams.setStable(false);
			AppParams.getApp_().getMMCore().setShutterOpen(false);
			
			if (!AppParams.hasAutoShutter()) {
	  	      	JOptionPane.showMessageDialog(null,
		    		  "Turn off the bright field light or close the shutter.",
		    		  "Quantitative Absorption Plugin",
		    		  JOptionPane.PLAIN_MESSAGE);
			}
			
			IJ.log("Getting dark image...");
  	      	AppParams.setForceMax(true);
  	      	AppParams.setCurrentSampleName("Read Current");
			AppParams.setDarkBlank(new ImageStats("Read Current", ""));
			
			if (!AppParams.hasAutoShutter()) {
	  	      	JOptionPane.showMessageDialog(null,
	  	    		  "Turn on the bright field light or open the shutter.",
	  	    		  "Quantitative Absorption Plugin",
	  	    		  JOptionPane.PLAIN_MESSAGE);
			}
			
			IJ.log("Getting first image...");
  	      	AppParams.setForceMax(false);
  	      	AppParams.getApp_().getMMCore().setShutterOpen(true);
  	      	AppParams.setCurrentSampleName("Initial Background");
			currentSample = new ImageStats("Initial Background","");
			
			currentSample.pixelLinReg();

			timePoint.add((double) 0);
			slope.add((double) currentSample.getAverageSlope());
			absorption.add(benchmarkAbsorption);
			
			previousSlope.add((double) currentSample.getAverageSlope());
			previousAbsorption.add(benchmarkAbsorption);
			
			double elapsedTime = 0;
			
			if (AppParams.saveBenchmarkExcel()) {
				addResult(elapsedTime,currentSample.getAverageSlope(),benchmarkAbsorption);
			}
			
			int i = 1;
			
			while (true) {
				
				if (params.getStop() || Thread.interrupted()) {
					AppParams.getApp_().getMMCore().setShutterOpen(false);
					throw new InterruptedException("canceled");
				}

				AppParams.setCurrentSampleName("Stabilization");
				currentSample = new ImageStats("Stabilization", Integer.toString(i++));
				currentSample.pixelLinReg();
				
				currentSlope = currentSample.getAverageSlope();
				
				float sum = 0;
				for(Double j : previousSlope) {sum += j;}
				sum /= previousSlope.size();
				benchmarkAbsorption = (float) -Math.log10(currentSlope/sum);
				
				previousSlope.add(currentSlope);
				previousAbsorption.add(benchmarkAbsorption);
				
				if (previousSlope.size()>7) {
					previousSlope.remove(0);
					previousAbsorption.remove(0);
				}
				
				elapsedTime++;
				
				timePoint.add(elapsedTime); //Elapsed time in minutes
				
				slope.add((double) currentSample.getAverageSlope());
				absorption.add(benchmarkAbsorption);
				
				Color markerColor = Color.BLACK;
				
				sum = 0;
				for(Double j : previousAbsorption) {sum += j;}
				sum /= previousAbsorption.size();
				
				IJ.log("Ideal exposure: " + Double.toString(currentSample.bestExposure()));
				IJ.log("Samples at ideal exposure: " + Integer.toString(currentSample.numBlankSamples(currentSample.bestExposure())));
				for (int j=3; j<=15; j++) {
					IJ.log("Minimum confident intensity " + Integer.toString(j) + ": " + Integer.toString(currentSample.minConfPix(j)));
				}
				
				if(absorption.size()>=7) {
					if ((Math.abs(Collections.max(previousAbsorption))<AppParams.getFluctuation()) && (Math.abs(sum)<AppParams.getEquilibrium())) {
						markerColor = Color.LIGHT_GRAY;
						AppParams.setStable(true);
						IJ.log("Microscope is stable!");
					} else if(AppParams.getStable()) {
						AppParams.setStable(false);
						IJ.log("Microscope is unstable!");
					}
				}
				
				slopePlot = new Plot("Benchmarking","Number of Samples","Average Slope (intensity/exposure time)");
				absorptionPlot = new Plot("Absorption","Number of Samples","Absorption");
				
				slopePlot.setLimits(Collections.min(timePoint)-1, Collections.max(timePoint)+1, Collections.min(slope)-Math.abs(slope.get(slope.size()-1)-slope.get(slope.size()-2)), Collections.max(slope)+Math.abs(slope.get(0)-slope.get(1)));
				slopePlot.setColor(markerColor);
				slopePlot.addPoints(timePoint, slope, Plot.CROSS);
				
				absorptionPlot.setLimits(Collections.min(timePoint)-1, Collections.max(timePoint)+1, Collections.min(absorption)-Math.abs(absorption.get(absorption.size()-1)-absorption.get(absorption.size()-2)), Collections.max(absorption)+Math.abs(absorption.get(0)-absorption.get(1)));
				absorptionPlot.setColor(markerColor);
				absorptionPlot.addPoints(timePoint, absorption, Plot.CROSS);
				
				if (AppParams.getBenchmarkVisible()) {
					aggregateProcessor.insert(slopePlot.getProcessor(), 0, 0);
					aggregateProcessor.insert(absorptionPlot.getProcessor(),width,0);
					plotAggregator.setProcessor(aggregateProcessor);
					plotAggregator.updateAndDraw();
				}
				
				if (AppParams.saveBenchmarkTxt()) {
					writeResults(elapsedTime,slope,absorption);
				}
				if (AppParams.saveBenchmarkExcel()) {
					addResult(elapsedTime,currentSlope,benchmarkAbsorption);
					benchmarkingResults.saveAs(AppParams.getOutDir() + "BenchmarkingResults.csv");
				}
			}
			
		} catch (InterruptedException ex) {
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeResults(double sample, ArrayList<Double> slope, ArrayList<Double> absorption)
	{
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(AppParams.getOutDir() + "Benchmarking Data.txt"))));

			pw.println("Sample\tAverage Slope\tAbsorption\n");
			pw.println();
			for (int i = 0; i < sample; i++) {
				pw.println(Integer.toString(i)+"\t"+Double.toString(slope.get(i))+"\t"+Double.toString(absorption.get(i))+"\n");
			}
			pw.close();
		}
		catch (IOException e) {}
	}
	
	private void addResult(double sample, double slope, double absorption) {
		benchmarkingResults.incrementCounter();
		benchmarkingResults.addValue("Sample",sample);
		benchmarkingResults.addValue("Average Slope",slope);
		benchmarkingResults.addValue("Benchmarking Absorption",absorption);
	}
}
