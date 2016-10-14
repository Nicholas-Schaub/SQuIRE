package nist.quantitativeabsorbance;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import nist.quantitativeabsorbance.guipanels.BenchmarkingPanel;
import nist.quantitativeabsorbance.guipanels.ControlPanel;
import nist.quantitativeabsorbance.guipanels.PlateConfigPanel;


@SuppressWarnings("serial")
public class QuantitativeAbsorptionGUI extends JFrame {
	
	private static ControlPanel controlPanel;
	private static BenchmarkingPanel benchmarkingPanel;
	private static PlateConfigPanel plateConfigPanel;
	
	public QuantitativeAbsorptionGUI() throws Exception {
		super(AppParams.getAPP_TITLE());
		init();
		dispApp();
	}
	
	private void dispApp()
	{
		setSize(new java.awt.Dimension(577,601));

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e) {
				performExit();
				super.windowClosing(e);
			}

		});
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void init() throws Exception
	{
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setTabLayoutPolicy(1);

		controlPanel = new ControlPanel();
		tabbedPane.add("Control", controlPanel);
		
		benchmarkingPanel = new BenchmarkingPanel();
		tabbedPane.add("Benchmarking", benchmarkingPanel);
		
		plateConfigPanel = new PlateConfigPanel();
		tabbedPane.add("Plate Scan",plateConfigPanel);
		
		//tabbedPane.setSize(new Dimension(577,577));

		setLayout(new BorderLayout());
		add(tabbedPane, "Center");
	}
	
	public static ControlPanel getControlPanel() {
		return controlPanel;
	}
	
	public static BenchmarkingPanel getBenchmarkingPanel() {
		return benchmarkingPanel;
	}
	
	public void performExit()
	{
		/*AppParams params = AppParams.getInstance();
		params.cancel();

		Window temp = WindowManager.getWindow(AppParams.getDataTableTitle());
		if (temp != null)
			temp.dispose();
		temp = WindowManager.getWindow(AppParams.getBenchmarkingParametersTableTitle());
		if (temp != null)
			temp.dispose();
		temp = WindowManager.getWindow(AppParams.getGraphsTitle());
		if (temp != null)
			temp.dispose();
		temp = WindowManager.getWindow(AppParams.getImagesStackTitle());
		if (temp != null) {
			temp.dispose();
		}

		dispose();*/
	}
	
}
