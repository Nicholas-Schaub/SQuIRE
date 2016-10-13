package nist.quantitativeabsorbance.guipanels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.micromanager.utils.MMScriptException;

import nist.filechooser.DirectoryChooserPanel;
import nist.ij.log.Log;
import nist.quantitativeabsorbance.AppParams;
import nist.textfield.TextFieldInputPanel;
import nist.textfield.validator.ValidatorDbl;

public class BenchmarkingPanel
extends JPanel
implements ActionListener
{
	private JButton startBenchmarkButton;
	private JButton stopBenchmarkButton;
	private TextFieldInputPanel<Double> equilibriumInput;
	private TextFieldInputPanel<Double> fluctuationInput;
	private JCheckBox showGraph;
	private JCheckBox saveBenchmarkingExcel;
	private JCheckBox saveBenchmarkingTxt;

	public BenchmarkingPanel()
	{
		initElements();
		initPanel();
		initListeners();
	}

	private void initElements()
	{
		startBenchmarkButton = new JButton("Start Benchmark");
		startBenchmarkButton.setForeground(new Color(0, 190, 0));
		stopBenchmarkButton = new JButton("Stop Benchmark");
		stopBenchmarkButton.setForeground(new Color(202, 0, 0));
		
		fluctuationInput = new TextFieldInputPanel("Acceptable Fluctuations in Absorbance: ", Double.toString(AppParams.getFluctuation()), new ValidatorDbl(0.000001,1));
		fluctuationInput.setToolTipText("<html>Definition for acceptable fluctuations in absorbance.<br> Values should be between 0.000001 and 0.1</html>");

		equilibriumInput = new TextFieldInputPanel("Acceptable Equilibrium Absorbance: ", Double.toString(AppParams.getEquilibrium()), new ValidatorDbl(.000001, 1));
		equilibriumInput.setToolTipText("<html>Definition for acceptable equilibrium in absorbance.<br> Values should be between 0.000001 and 0.1, and should be smaller than the fluctuation value.</html>");
		
		showGraph = new JCheckBox("Show Graph", AppParams.getBenchmarkVisible());
		showGraph.setToolTipText("<html>Run a pixel by pixel benchmark instead of obtaining absorption values.</html>");
		
		saveBenchmarkingExcel = new JCheckBox("Save Benchmarking Data to Spreadsheet", AppParams.saveBenchmarkExcel());
		saveBenchmarkingExcel.setToolTipText("<html>Save the data acquired by the benchmarking protocol to an excel spreadsheet.</html>");
		
		saveBenchmarkingTxt = new JCheckBox("Save Benchmarking Data to Text File", AppParams.saveBenchmarkTxt());
		saveBenchmarkingExcel.setToolTipText("<html>Save the data acquired by the benchmarking protocol to a tab delimited file.</html>");

		//aboutButton = new JButton("About");
		//aboutButton.setToolTipText("<html>This takes you to the NIST Fluorescence <br>Microscope Benchmarking website.<br><i>Plugin Version 1.0</i></html>");
	}

	private void initPanel()
	{
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(2, 8, 2, 8);
		c.anchor = GridBagConstraints.CENTER;

		c.fill = GridBagConstraints.BOTH;
		c.ipady = 10;
		c.gridwidth = 1;
		c.weightx = 4.0D;

		c.gridx = 0;
		c.gridy = 0;
		content.add(startBenchmarkButton, c);

		c.weightx = 1.0D;
		c.gridx = 1;
		content.add(stopBenchmarkButton, c);

		c.gridwidth = 2;
		c.gridx = 0;
		c.ipady = 1;
		c.fill = GridBagConstraints.REMAINDER;
		
		c.gridy++;
		content.add(fluctuationInput,c);

		c.gridy++;
		content.add(equilibriumInput,c);
		
		c.gridy++;
		content.add(showGraph,c);
		
		c.gridy++;
		content.add(saveBenchmarkingExcel,c);
		
		c.gridy++;
		content.add(saveBenchmarkingTxt,c);

		add(content);
	}

	private void initListeners() {
		startBenchmarkButton.addActionListener(this);
		stopBenchmarkButton.addActionListener(this);
		showGraph.addActionListener(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == startBenchmarkButton) {
			Log.debug("Start Benchmark Button Pressed");
			try {
				AppParams.getInstance().start(this);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (MMScriptException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else if (e.getSource() == stopBenchmarkButton) {
			Log.debug("Stop Benchmark Button Pressed");
			try {
				AppParams.getInstance().cancel();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else if (e.getSource() == showGraph) {
			Log.debug("Show Benchmark Graph box clicked.");
			AppParams.setBenchmarkVisible(isBenchmarkVisible());
		} else {
			Log.error("Invalid Control Panel Event");
		}
	}
	
	public boolean isBenchmarkVisible() {return showGraph.isSelected();}
	
	public void setBenchmarkVisible(boolean isVisible) {showGraph.setSelected(isVisible);}
	
	public double getFluctuation() {return fluctuationInput.getValue();}
	
	public void setFluctuation(double fluctuation) {fluctuationInput.setValue(fluctuation);}

	public double getEquilibrium() {return equilibriumInput.getValue();}
	
	public void setEquilibrium(double equilibrium) {equilibriumInput.setValue(equilibrium);}
	
	public boolean isSaveBenchmarkingExcel() {return saveBenchmarkingExcel.isSelected();}
	
	public void setSaveBenchmarkingExcel(boolean saveBenchmarkingExcel) {this.saveBenchmarkingExcel.setSelected(saveBenchmarkingExcel);}
	
	public boolean isSaveBenchmarkingTxt() {return saveBenchmarkingTxt.isSelected();}
	
	public void setSaveBenchmarkingTxt(boolean saveBenchmarkingTxt) {this.saveBenchmarkingTxt.setSelected(saveBenchmarkingTxt);}
}
