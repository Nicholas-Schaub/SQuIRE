package nist.quantitativeabsorbance;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class QuantitativeAbsorption implements MMPlugin {
	   
	   // Variables for naming the plugin
	   public static String menuName = "Quantitative Absorption";
	   public static String tooltipDescription =
	      "Measures absorption in an RPE sample";
	
	   // Provides access to the Micro-Manager Java API (for GUI control and high-
	   // level functions).
	   private ScriptInterface app_;
	   
	   public void setApp(ScriptInterface app) {
	      app_ = app;
	   }
	
	   public void dispose() {
	      // We do nothing here as the only object we create, our dialog, should
	      // be dismissed by the user.
	   }

	   public void show() {
		   
		   // Make sure live mode is turned off.
		   if (app_.isLiveModeOn()) {app_.enableLiveMode(false);}

		   // Display a dialog to configure the acquisition.
		   AppParams.getInstance().setApp(app_);
		   AppParams.initializeMicroscopeHardware();
		   testHardware();
		   try {
			   new QuantitativeAbsorptionGUI();
		   } catch (Exception e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }
	   }
	   
	   public void testHardware() {

	   }


	   public String getInfo () {
	      return "Quantitative Absorption Plug-in.";
	   }

	   public String getDescription() {
	      return "The Quantitative Absorption Plugin runs a protocol to assess the " +
	      		 "amount of absorption in a biological sample. This Plugin was designed " +
	    		  "to work with a monolayer of cells, specifically RPE cells.";
	   }
	   
	   public String getVersion() {
	      return "1.0";
	   }
	   
	   public String getCopyright() {
	      return "None...";
	   }
	   
}
