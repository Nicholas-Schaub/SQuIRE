package nist.squire;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMScriptException;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

import ij.IJ;
import ij.ImagePlus;
import mmcorej.BooleanVector;
import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.DoubleVector;
import mmcorej.StrVector;
import nist.ij.log.Log;
import nist.squire.guipanels.BenchmarkingPanel;
import nist.squire.guipanels.ControlPanel;

public class AppParams {
	// Strings used in the plug-in
	@SuppressWarnings("unused")
	private static final String APP_TITLE = "Quantitative Absorption GUI";
	
	// Micromanager classes
	private static CMMCore core_;
	private static ScriptInterface app_;
	
	// Microscope hardware configuration
	private static boolean hasAutoShutter;
	private static StrVector stateDevices;
	private static StrVector shutterDevices;
	
	// Save Settings
	private static String coreSaveDir; //root save folder
	private static String outDir; //save folder for individual capture runs
	private static StrVector rawImageDir;
	private static StrVector calibrationImageDir;
	private static StrVector channelImageDir;
	private static boolean saveBenchmarkingExcel = false; //benchmarking
	private static boolean saveBenchmarkingTxt = false; //benchmarking
	
	// Quantitative absorption thread settings. Many of these will also control the benchmark thread.
	private static int channels = 1;
	private static int numReplicates = 3;
	private static int numSamples = 5;
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
	private static BooleanVector useAutofocus;
	private static boolean isAbsorbance = true;
	private static String twilio_sid = "";
	private static String twilio_token = "";
	private static String twilio_phone = "";
	private static String receiver_phone = "";
	
	// Benchmarking Thread settings
	private static double fluctuation = 0.0001;
	private static double equilibrium = 0.00005;
	private static boolean showBenchmarkGraph = false;
	private static boolean isStable = false;
	
	// Methods to get device hardware.
	public static boolean hasAutoShutter() {return hasAutoShutter;}
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
	public static BooleanVector getAutofocus() {return useAutofocus;}
	public static DoubleVector getChannelExposures() {return channelExposure;}
	public static DoubleVector getChannelOffset() {return channelOffset;}
	public static String getFluorescentShutter() {return fluorescentShutter;}
	public static String getTransmittedShutter() {return transmittedShutter;}
	
	// Methods to set device hardware.
	public static void setCurrentSampleName(String sampleName) {currentSampleName = sampleName;}
	public static void setChannelExposure(int index, double exp) {AppParams.channelExposure.set(index, exp);}
	public static void setFluorescentDevice(StrVector fluorescentDevice) {AppParams.fluorescentDevice = fluorescentDevice;}
	public static void setFluorescentDeviceSetting(StrVector fluorescentDeviceSetting) {AppParams.fluorescentDeviceSetting = fluorescentDeviceSetting;}
	public static void setTransmittedDevice(StrVector transmittedDevice) {AppParams.transmittedDevice = transmittedDevice;}
	public static void setTransmittedDeviceSetting(StrVector transmittedDeviceSetting) {AppParams.transmittedDeviceSetting = transmittedDeviceSetting;}
	
	// Methods to set quantitative absorption thread settings. These are also used for benchmarking.
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
	public static boolean getIsAbsorbance() {return isAbsorbance;};
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
	public static String getCoreSaveDir() {return coreSaveDir;}
	public static String getOutDir() {return outDir;}
	public static String getRawImageDir(int index) {return rawImageDir.get(index);}
	public static String getCalibrationImageDir(int index) {return calibrationImageDir.get(index);}
	public static String getChannelImageDir(int index) {return channelImageDir.get(index);}
	
	// Methods to set save settings
	public static void saveBenchmarkExcel (boolean saveBenchmarkingExcel) {AppParams.saveBenchmarkingExcel = saveBenchmarkingExcel;}
	public static void saveBenchmarkTxt (boolean saveBenchmarkingTxt) {AppParams.saveBenchmarkingTxt = saveBenchmarkingTxt;}
	
	// Methods to control the imaging thread.
	public boolean getStop() {return stopThread;}

	public static AppParams getInstance() { return INSTANCE;}
	
	public void start(Object callSource) throws Exception {
		// TODO Auto-generated method stub

		pullParamsFromGui(callSource);
		
		AppParams.lightBlank = new ArrayList<ImageStats>();
		AppParams.foreground = new ArrayList<ImagePlus>();
		
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
			if (AppParams.isAutomated) {
				thread = new Thread(new AutomatedCaptureThread());
			} else {
				thread = new Thread(new ManualCaptureThread());
			}
			
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
		saveBenchmarkingExcel = QuantitativeAbsorptionGUI.getBenchmarkingPanel().isSaveBenchmarkingExcel();
		saveBenchmarkingTxt = QuantitativeAbsorptionGUI.getBenchmarkingPanel().isSaveBenchmarkingTxt();
		isAutomated = QuantitativeAbsorptionGUI.getControlPanel().isAutomated();
		isAbsorbance = QuantitativeAbsorptionGUI.getControlPanel().isAbsorbance();
		numReplicates = QuantitativeAbsorptionGUI.getControlPanel().getNumReplicates();
		numSamples = QuantitativeAbsorptionGUI.getControlPanel().getNumSample();
		fluorescentShutter = QuantitativeAbsorptionGUI.getControlPanel().getFluorescentShutter();
		transmittedShutter = QuantitativeAbsorptionGUI.getControlPanel().getTransmittedShutter();
		Object[][] automatedSettings = QuantitativeAbsorptionGUI.getControlPanel().getAutomatedSettings();
		twilio_sid = QuantitativeAbsorptionGUI.getNotificationsPanel().getSID();
		twilio_token = QuantitativeAbsorptionGUI.getNotificationsPanel().getToken();
		twilio_phone = QuantitativeAbsorptionGUI.getNotificationsPanel().getTwilPhone();
		receiver_phone = QuantitativeAbsorptionGUI.getNotificationsPanel().getRecPhone();
		channels = automatedSettings.length;
		System.out.println(channels);
		fluorescentDevice = new StrVector();
		fluorescentDeviceSetting = new StrVector();
		transmittedDevice = new StrVector();
		transmittedDeviceSetting = new StrVector();
		absorptionSetting = new StrVector();
		channelName = new StrVector();
		channelExposure = new DoubleVector();
		channelOffset = new DoubleVector();
		useAutofocus = new BooleanVector();
		for (int i=0; i<channels; i++) {
			channelName.add((String) automatedSettings[i][0]);
			absorptionSetting.add((String) automatedSettings[i][1]);
			transmittedDevice.add(QuantitativeAbsorptionGUI.getControlPanel().getTransmittedDevice());
			transmittedDeviceSetting.add((String) automatedSettings[i][2]);
			fluorescentDevice.add(QuantitativeAbsorptionGUI.getControlPanel().getFluorescentDevice());
			fluorescentDeviceSetting.add((String) automatedSettings[i][3]);
			useAutofocus.add((Boolean) automatedSettings[i][6]);
			if (automatedSettings[i][4].equals("")) {
				channelExposure.add(1);
			} else {
				channelExposure.add(Double.parseDouble(automatedSettings[i][4].toString()));
			}
			if (automatedSettings[i][5].equals("")) {
				channelOffset.add(0);
			} else {
				channelOffset.add(Double.parseDouble(automatedSettings[i][5].toString()));
			}
		}
		
		if (isAutomated) {
			numSamples = app_.getPositionList().getNumberOfPositions();

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
					calibrationImageDir = new StrVector();
					for (int i = 0; i<channels; i++) {
						channelImageDir.add(outDir + File.separator + channelName.get(i) + File.separator);
						rawImageDir.add(channelImageDir.get(i) + "Raw Images" + File.separator);
						calibrationImageDir.add(channelImageDir.get(i) + "Calibration Images" + File.separator);
						new File(channelImageDir.get(i)).mkdir();
						if (absorptionSetting.get(i).equals("Absorbance")) {
							new File(calibrationImageDir.get(i)).mkdir();
							new File(rawImageDir.get(i)).mkdir();
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

		pref.putInt("numSamples", numSamples);
		pref.putInt("channels", channels);
		pref.putInt("numReplicates", numReplicates);
		pref.put("coreSaveDir", coreSaveDir);
		pref.putBoolean("isAutomated", isAutomated);
		pref.put("twilio_sid", twilio_sid);
		pref.put("twilio_token", twilio_token);
		pref.put("twilio_phone", twilio_phone);

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
		numSamples = pref.getInt("numSamples", numSamples);
		channels = pref.getInt("channels", channels);
		numReplicates = pref.getInt("numReplicates", numReplicates);
		isAutomated = pref.getBoolean("isAutomated", isAutomated);
		twilio_sid = pref.get("twilio_sid", twilio_sid);
		twilio_token = pref.get("twilio_token", twilio_token);
		twilio_phone = pref.get("twilio_phone", twilio_phone);
	}
		
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
	
	public static void sendText(String msg) {
		Twilio.init(twilio_sid, twilio_token);
		
		Message.creator(new PhoneNumber(receiver_phone),
										 new PhoneNumber(twilio_phone),
										 msg).create();
	}
	
	public static String getSID() {return twilio_sid;}
	public static String getToken() {return twilio_token;}
	public static String getTwilioPhone() {return twilio_phone;}
	public static String getReceiverPhone() {return twilio_phone;}
	public static void setSID(String sid) {twilio_sid = sid;}
	public static void setToken(String token) {twilio_token = token;}
	public static void setTwilioPhone(String phone) {twilio_phone = phone;}
	public static void setReceiverPhone(String phone) {receiver_phone = phone;}
}