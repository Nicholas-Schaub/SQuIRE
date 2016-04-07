package nist.quantitativeabsorbance;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.DoubleVector;
import mmcorej.StrVector;
import nist.ij.log.Log;
import nist.quantitativeabsorbance.guipanels.BenchmarkingPanel;
import nist.quantitativeabsorbance.guipanels.ControlPanel;

import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

public class AppParams {
	// Strings used in the plug-in
	@SuppressWarnings("unused")
	private static final String APP_TITLE = "Quantitative Absorption GUI";
	
	// Micromanager classes
	private static CMMCore core_;
	private static ScriptInterface app_;
	
	// Microscope hardware configuration
	private static boolean hasAutoShutter;
	private static boolean useAutoShutter = false;
	private static StrVector stateDevices;
	private static StrVector shutterDevices;
	
	// Save Settings
	private static String coreSaveDir; //root save folder
	private static String outDir; //save folder for individual capture runs
	private static StrVector rawImageDir;
	private static StrVector meanImageDir;
	private static StrVector stdImageDir;
	private static StrVector linearRegressionDir;
	private static StrVector channelImageDir;
	private static boolean saveRawImages = false;
	private static boolean saveMeanImages = false;
	private static boolean saveStdImages = false;
	private static boolean saveLinearRegression = false;
	private static boolean saveSlopeImage = false;
	private static boolean saveBenchmarkingExcel = false; //benchmarking
	private static boolean saveBenchmarkingTxt = false; //benchmarking
	
	// Quantitative absorption thread settings. Many of these will also control the benchmark thread.
	private static double minExposure = 1;
	private static double maxExposure = 1000;
	private static int channels = 1;
	private static int numReplicates = 3;
	private static int numSamples = 5;
	private static PositionList automatedPositionList;
	private static final AppParams INSTANCE = new AppParams();
	private Thread thread;
	private volatile boolean stopThread = false;
	private static boolean forceMax = false;
	private static String plateID = "Plate ID";
	private static ImageStats darkBlank = null;
	private static ArrayList<ImageStats> lightBlank = new ArrayList<ImageStats>();
	private static ArrayList<ImagePlus> foreground = new ArrayList<ImagePlus>();
	private static boolean isAutomated = false;
	private static String currentSampleName = "Undefined";
	private static StrVector fluorescentDevice;
	private static StrVector fluorescentDeviceSetting;
	private static StrVector transmittedDevice;
	private static StrVector transmittedDeviceSetting;
	private static StrVector absorptionSetting;
	private static StrVector channelName;
	private static DoubleVector channelExposure;
	private static DoubleVector channelOffset;
	private static String fluorescentShutter;
	private static String transmittedShutter;
	
	// Benchmarking Thread settings
	private static double fluctuation = 0.0001;
	private static double equilibrium = 0.00005;
	private static boolean showBenchmarkGraph = false;
	private static boolean isStable = false;
	
	// Methods to get device hardware.
	public static boolean hasAutoShutter() {return hasAutoShutter;}
	public static boolean useAutoShutter() {return useAutoShutter;}
	public static StrVector getStateDevices() {return stateDevices;}
	public static StrVector getDeviceStates(String stateDevice) throws Exception {
		return core_.getAllowedPropertyValues(stateDevice,"Label");
	};
	public static StrVector getShutterDevices() {return shutterDevices;}
	public static StrVector getFluorescentDevice() {return fluorescentDevice;}
	public static StrVector getFluorescentDeviceSetting() {return fluorescentDeviceSetting;}
	public static StrVector getTransmittedDevice() {return transmittedDevice;}
	public static StrVector getTransmittedDeviceSetting() {return transmittedDeviceSetting;}
	public static StrVector getAbsorptionSetting() {return absorptionSetting;}
	public static StrVector getChannelName() {return channelName;}
	public static DoubleVector getChannelExposures() {return channelExposure;}
	public static DoubleVector getChannelOffset() {return channelOffset;}
	public static String getFluorescentShutter() {return fluorescentShutter;}
	public static String getTransmittedShutter() {return transmittedShutter;}
	
	// Methods to set device hardware.
	public static void setCurrentSampleName(String sampleName) {currentSampleName = sampleName;}
	public static void useAutoShutter(boolean useShutter) {AppParams.useAutoShutter = useShutter;}
	public static void setChannelExposure(int index, double exp) {AppParams.channelExposure.set(index, exp);}
	public static void setFluorescentDevice(StrVector fluorescentDevice) {AppParams.fluorescentDevice = fluorescentDevice;}
	public static void setFluorescentDeviceSetting(StrVector fluorescentDeviceSetting) {AppParams.fluorescentDeviceSetting = fluorescentDeviceSetting;}
	public static void setTransmittedDevice(StrVector transmittedDevice) {AppParams.transmittedDevice = transmittedDevice;}
	public static void setTransmittedDeviceSetting(StrVector transmittedDeviceSetting) {AppParams.transmittedDeviceSetting = transmittedDeviceSetting;}
	
	// Methods to set quantitative absorption thread settings. These are also used for benchmarking.
	public static void setMinExposure(float minExposure) {AppParams.minExposure = minExposure;}
	public static void setMaxExposure(float maxExposure) {AppParams.maxExposure = maxExposure;}
	public static void setChannels(int channels) {AppParams.channels = channels;}
	public static void setNumReplicates(int numReplicates) {AppParams.numReplicates = numReplicates;}
	public static void setNumSamples(int numSamples) {AppParams.numSamples = numSamples;}
	public static void setForceMax(boolean forceMax) {AppParams.forceMax = forceMax;}
	public static void setPlateID(String plateID) {AppParams.plateID = plateID;}
	public static void setDarkBlank(ImageStats darkBlank) {AppParams.darkBlank = darkBlank;}
	public static void addLightBlank(ImageStats lightBlank) {AppParams.lightBlank.add(lightBlank);}
	public static void addForeground(ImagePlus foreground) {AppParams.foreground.add(foreground);}
	
	// Methods to get quantitative absorption thread settings. These are also used for benchmarking.
	public static String getAPP_TITLE() {return "Quantitative Absorption GUI";}
	public static double getMinExposure() {return minExposure;}
	public static double getMaxExposure() {return maxExposure;}
	public static int getChannels() {return channels;}
	public static int getNumReplicates() {return numReplicates;}
	public static int getNumSamples() {return numSamples;}
	public static boolean getForceMax() {return forceMax;}
	public static CMMCore getCore_() {return core_;}
	public static String getPlateID() {return plateID;}
	public static ImageStats getDarkBlank() {return darkBlank;}
	public static ImageStats getLightBlank(int index) {return AppParams.lightBlank.get(index);}
	public static ImagePlus getForeground(int index) {return AppParams.foreground.get(index);}
	public static boolean getIsAutomated() {return isAutomated;}
	public static String getCurrentSampleName() {return currentSampleName;}
	
	// Methods to set benchmarking thread settings
	public static void setFluctuation(double fluctuation) {AppParams.fluctuation = fluctuation;}
	public static void setEquilibrium(double equilibrium) {AppParams.equilibrium = equilibrium;}
	public static void setBenchmarkVisible(boolean showBenchmarkGraph) {AppParams.showBenchmarkGraph = showBenchmarkGraph;}
	public static void setStable(boolean isStable) {AppParams.isStable = isStable;}
	
	// Methods to get benchmarking thread settings
	public static double getFluctuation() {return fluctuation;}
	public static double getEquilibrium() {return equilibrium;}
	public static boolean getBenchmarkVisible() {return showBenchmarkGraph;}
	public static boolean getStable() {return isStable;}
	
	// Methods to get save settings
	public static boolean saveBenchmarkExcel() {return saveBenchmarkingExcel;}
	public static boolean saveBenchmarkTxt() {return saveBenchmarkingTxt;}
	public static boolean isSaveRawImages() {return saveRawImages;}
	public static boolean isSaveMeanImages() {return saveMeanImages;}
	public static boolean isSaveStdImages() {return saveStdImages;}
	public static boolean isLinearRegression() {return saveLinearRegression;}
	public static boolean isSaveSlopeImage() {return saveSlopeImage;}
	public static String getCoreSaveDir() {return coreSaveDir;}
	public static String getOutDir() {return outDir;}
	public static String getRawImageDir(int index) {return rawImageDir.get(index);}
	public static String getMeanImageDir(int index) {return meanImageDir.get(index);}
	public static String getStdImageDir(int index) {return stdImageDir.get(index);}
	public static String getLinearRegressionDir(int index) {return linearRegressionDir.get(index);}
	public static String getChannelImageDir(int index) {return channelImageDir.get(index);}
	
	// Methods to set save settings
	public static void saveBenchmarkExcel (boolean saveBenchmarkingExcel) {AppParams.saveBenchmarkingExcel = saveBenchmarkingExcel;}
	public static void saveBenchmarkTxt (boolean saveBenchmarkingTxt) {AppParams.saveBenchmarkingTxt = saveBenchmarkingTxt;}
	
	// Methods to control the imaging thread.
	public boolean getStop() {return stopThread;}

	public static AppParams getInstance() { return INSTANCE;}
	
	public void start(Object callSource) throws Exception {
		// TODO Auto-generated method stub
		AppParams.lightBlank = new ArrayList<ImageStats>();
		pullParamsFromGui(callSource);
		
		recordPreferences();
		
		if (isAutomated) {
			if (app_.getPositionList().getNumberOfPositions()<=1 && callSource instanceof ControlPanel) {
				IJ.error("Positions were not set up for imaging. Set up imaging sites in Plate Scan Tab.");
			}
		}
		
		stopThread = false;
		if (callSource instanceof BenchmarkingPanel) {
			thread = new Thread(new BenchmarkingThread());
		} else if (callSource instanceof ControlPanel) {
			thread = new Thread(new SampleCaptureThread());
		}
		
		thread.start();
	}
	
	public void cancel() throws Exception {
		// TODO Auto-generated method stub
		if (thread != null) {
			thread.interrupt();
		}
		stopThread = true;
	}

	private void pullParamsFromGui(Object callSource) throws MMScriptException {

		plateID = QuantitativeAbsorptionGUI.getControlPanel().getPlateId();
		coreSaveDir = QuantitativeAbsorptionGUI.getControlPanel().getCoreSaveDirectory();
		saveBenchmarkingExcel = QuantitativeAbsorptionGUI.getSaveSettingsPanel().isSaveBenchmarkingExcel();
		saveBenchmarkingTxt = QuantitativeAbsorptionGUI.getSaveSettingsPanel().isSaveBenchmarkingTxt();
		isAutomated = QuantitativeAbsorptionGUI.getControlPanel().isAutomated();
		minExposure = QuantitativeAbsorptionGUI.getControlPanel().getMinExposure();
		maxExposure = QuantitativeAbsorptionGUI.getControlPanel().getMaxExposure();
		numReplicates = QuantitativeAbsorptionGUI.getControlPanel().getNumReplicates();
		numSamples = QuantitativeAbsorptionGUI.getControlPanel().getNumSample();
		channels = QuantitativeAbsorptionGUI.getControlPanel().getNumChannels();
		saveRawImages = QuantitativeAbsorptionGUI.getControlPanel().getSaveRawImages();
		saveMeanImages = QuantitativeAbsorptionGUI.getControlPanel().getSaveMeanImages();
		saveStdImages = QuantitativeAbsorptionGUI.getControlPanel().getSaveStdImages();
		saveLinearRegression = QuantitativeAbsorptionGUI.getControlPanel().getSaveLinearRegression();
		saveSlopeImage = QuantitativeAbsorptionGUI.getControlPanel().getSaveSlopeImage();
		useAutoShutter = QuantitativeAbsorptionGUI.getControlPanel().useAutoShutter();
		fluorescentShutter = QuantitativeAbsorptionGUI.getControlPanel().getFluorescentShutter();
		transmittedShutter = QuantitativeAbsorptionGUI.getControlPanel().getTransmittedShutter();
		
		if (isAutomated) {
			numSamples = app_.getPositionList().getNumberOfPositions();
			Object[][] automatedSettings = QuantitativeAbsorptionGUI.getControlPanel().getAutomatedSettings();
			channels = automatedSettings.length;
			fluorescentDevice = new StrVector();
			fluorescentDeviceSetting = new StrVector();
			transmittedDevice = new StrVector();
			transmittedDeviceSetting = new StrVector();
			absorptionSetting = new StrVector();
			channelName = new StrVector();
			channelExposure = new DoubleVector();
			channelOffset = new DoubleVector();
			for (int i=0; i<channels; i++) {
				channelName.add((String) automatedSettings[i][0]);
				absorptionSetting.add((String) automatedSettings[i][1]);
				fluorescentDevice.add((String) automatedSettings[i][2]);
				fluorescentDeviceSetting.add((String) automatedSettings[i][3]);
				transmittedDevice.add((String) automatedSettings[i][4]);
				transmittedDeviceSetting.add((String) automatedSettings[i][5]);
				if (automatedSettings[i][6].equals("")) {
					channelExposure.add(1);
				} else {
					channelExposure.add(Double.parseDouble(automatedSettings[i][6].toString()));
				}
				if (automatedSettings[i][7].equals("")) {
					channelOffset.add(0);
				} else {
					channelOffset.add(Double.parseDouble(automatedSettings[i][7].toString()));
				}
			}
		}
		
		fluctuation = QuantitativeAbsorptionGUI.getBenchmarkingPanel().getFluctuation(); 
		equilibrium = QuantitativeAbsorptionGUI.getBenchmarkingPanel().getEquilibrium();
		
		if (!coreSaveDir.endsWith(File.separator)) {
			coreSaveDir = coreSaveDir + File.separator;
		}
		
		if (callSource instanceof ControlPanel) {
			if (coreSaveDir.toLowerCase().endsWith(plateID.toLowerCase() + File.separator)) {
				outDir = coreSaveDir + File.separator + "Date " + getISOTimeString();
				coreSaveDir = coreSaveDir.toLowerCase().replace(plateID.toLowerCase() + File.separator, "");
			} else {
				outDir = coreSaveDir + plateID + File.separator + "Date " + getISOTimeString() + File.separator;
			}
		} else if (callSource instanceof BenchmarkingPanel) {
			if (coreSaveDir.toLowerCase().endsWith(plateID.toLowerCase() + File.separator)) {
				coreSaveDir = coreSaveDir.toLowerCase().replace(plateID.toLowerCase() + File.separator, "");
			}
			outDir = coreSaveDir + "Benchmarking Data" + File.separator + getISOTimeString() + File.separator;
		}
		
		if (saveBenchmarkExcel() || saveBenchmarkTxt() || callSource instanceof ControlPanel) {
			File file = new File(outDir);
			if (!file.exists()) {
				file.mkdirs();
				if (callSource instanceof ControlPanel) {
					channelImageDir = new StrVector();
					rawImageDir = new StrVector();
					meanImageDir = new StrVector();
					stdImageDir = new StrVector();
					linearRegressionDir = new StrVector();
					for (int i = 0; i<channels; i++) {
						channelImageDir.add(outDir + File.separator + channelName.get(i) + File.separator);
						rawImageDir.add(channelImageDir.get(i) + "Raw Images" + File.separator);
						meanImageDir.add(channelImageDir.get(i) + "Mean Images" + File.separator);
						stdImageDir.add(channelImageDir.get(i) + "Std Images" + File.separator);
						linearRegressionDir.add(channelImageDir.get(i) + "Linear Regression Images" + File.separator);
						new File(channelImageDir.get(i)).mkdir();
						if (absorptionSetting.get(i).equals("Absorbance")) {
							if (saveRawImages) {
								new File(rawImageDir.get(i)).mkdir();
							}
							if (saveMeanImages) {
								new File(meanImageDir.get(i)).mkdir();
							}
							if (saveStdImages) {
								new File(stdImageDir.get(i)).mkdir();
							}
							if (saveLinearRegression) {
								new File(linearRegressionDir.get(i)).mkdir();
							}
						}
					}
				}
			}
		}
		
	}
	
	public static void initializeMicroscopeHardware() {
		if (core_.getShutterDevice().equals("")) {
			hasAutoShutter = false;
			Log.debug("No Shutter Device");
		} else {
			hasAutoShutter = true;
			core_.setAutoShutter(false);
			Log.debug(core_.getShutterDevice()+" - Auto Shutter Off");
		}
		
		stateDevices = core_.getLoadedDevicesOfType(DeviceType.StateDevice);
		shutterDevices = core_.getLoadedDevicesOfType(DeviceType.ShutterDevice);
		
		Log.error(Double.toString(core_.getPixelSizeUm()));
	}
	
	public void setApp(ScriptInterface app) {
		// TODO Auto-generated method stub
		core_ = app.getMMCore();
		AppParams.app_ = app;
		
		resetToDefaultParams();
		loadPreferences();
	}
	
	public static ScriptInterface getApp_() {
		return app_;
	}
	
	private void resetToDefaultParams()
	{
		Log.debug("Resetting params to defaults");

		coreSaveDir = System.getProperty("user.home");
		if (!coreSaveDir.endsWith(File.separator)) {
			coreSaveDir += File.separator;
		}
		thread = null;
	}
	
	private void recordPreferences() {
		Log.mandatory("Recording user preferences");

		Preferences pref = Preferences.userRoot().node("QuantitativeAbsorption");
		try
		{
			pref.clear();
		} catch (BackingStoreException e) {
			Log.mandatory("Error unable clear preferences: " + e.getMessage());
			return;
		}

		pref.putDouble("minExposure", minExposure);
		pref.putDouble("maxExposure", maxExposure);
		pref.putInt("numSamples", numSamples);
		pref.putInt("channels", channels);
		pref.putInt("numReplicates", numReplicates);
		pref.put("coreSaveDir", coreSaveDir);
		pref.putBoolean("isAutomated", isAutomated);
		pref.putBoolean("saveRawImages", saveRawImages);
		pref.putBoolean("saveMeanImages", saveMeanImages);
		pref.putBoolean("saveStdImages", saveStdImages);
		pref.putBoolean("saveLinearRegression", saveLinearRegression);
		pref.putBoolean("saveSlopeImage", saveSlopeImage);
		pref.putBoolean("useAutoShutter", useAutoShutter);

		try
		{
			pref.flush();
		} catch (BackingStoreException e) {
			Log.mandatory("Error unable to record preferences: " + e.getMessage());
		}
	}

	private void loadPreferences()
	{
		Log.mandatory("Loading user preferences");
		Preferences pref = Preferences.userRoot().node("QuantitativeAbsorption");
		try
		{
			pref.sync();
		} catch (BackingStoreException e) {
			Log.error("Error synchronizing preferences: " + e.getMessage());
		}

		coreSaveDir = pref.get("coreSaveDir", coreSaveDir);
		if (!coreSaveDir.endsWith(File.separator)) {
			coreSaveDir += File.separator;
		}
		minExposure = pref.getDouble("minExposure", minExposure);
		maxExposure = pref.getDouble("maxExposure", maxExposure);
		numSamples = pref.getInt("numSamples", numSamples);
		channels = pref.getInt("channels", channels);
		numReplicates = pref.getInt("numReplicates", numReplicates);
		isAutomated = pref.getBoolean("isAutomated", isAutomated);
		saveRawImages = pref.getBoolean("saveRawImages", saveRawImages);
		saveMeanImages = pref.getBoolean("saveMeanImages", saveMeanImages);
		saveStdImages = pref.getBoolean("saveStdImages", saveStdImages);
		saveLinearRegression = pref.getBoolean("saveLinearRegression", saveLinearRegression);
		saveSlopeImage = pref.getBoolean("saveSlopeImage", saveSlopeImage);
		useAutoShutter = pref.getBoolean("useAutoShutter", useAutoShutter);
	}
	
	public static void updateStatus(double percentComplete) {QuantitativeAbsorptionGUI.getControlPanel().updateStatus(percentComplete);}
	
	public static String getISOTimeString() {
		Timestamp curTime = new Timestamp(Calendar.getInstance().getTimeInMillis());
		String timeStr = curTime.toString();

		timeStr = timeStr.replace(" ", "T");
		timeStr = timeStr.replaceFirst(":", "h");
		timeStr = timeStr.replaceFirst(":", "m");
		timeStr = timeStr.replaceFirst(":", "m");
		timeStr = timeStr + "s";
		int idx = timeStr.indexOf('.');
		timeStr = timeStr.substring(0, idx);
		return timeStr;
	}
}
