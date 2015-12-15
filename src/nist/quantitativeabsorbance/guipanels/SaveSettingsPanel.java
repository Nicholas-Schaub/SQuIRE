package nist.quantitativeabsorbance.guipanels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import nist.filechooser.DirectoryChooserPanel;
import nist.logofield.AddNistLogo;
import nist.quantitativeabsorbance.AppParams;

public class SaveSettingsPanel
extends JPanel
implements ActionListener
{
	private DirectoryChooserPanel outputDirectory;
	private JCheckBox saveBenchmarkingExcel;
	private JCheckBox saveBenchmarkingTxt;

	public SaveSettingsPanel()
	{
		initElements();
		initPanel();
		initListeners();
	}

	private void initElements()
	{
		outputDirectory = new DirectoryChooserPanel("Save Directory:", "c:\\Benchmarking Images\\", 30);
		outputDirectory.setToolTipText("<html>Where all of the data, images, and <br>metadata will be saved.</html>");
		
		saveBenchmarkingExcel = new JCheckBox("Save Benchmarking Data to Spreadsheet", AppParams.saveBenchmarkExcel());
		saveBenchmarkingExcel.setToolTipText("<html>Save the data acquired by the benchmarking protocol to an excel spreadsheet.</html>");
		
		saveBenchmarkingTxt = new JCheckBox("Save Benchmarking Data to Text File", AppParams.saveBenchmarkTxt());
		saveBenchmarkingExcel.setToolTipText("<html>Save the data acquired by the benchmarking protocol to a tab delimited file.</html>");
	}

	private void initPanel()
	{
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(2, 8, 2, 8);
		c.anchor = GridBagConstraints.CENTER;

		c.fill = GridBagConstraints.REMAINDER;
		c.ipady = 1;
		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 0;
		content.add(outputDirectory, c);
		
		c.gridy++;
		content.add(saveBenchmarkingExcel,c);
		
		c.gridy++;
		content.add(saveBenchmarkingTxt,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(new AddNistLogo(90,30),c);

		add(content);
	}

	private void initListeners() {
	}

	public void actionPerformed(ActionEvent e)
	{
/*		if (e.getSource() == startButton) {
			
		} else {
			Log.error("Invalid Control Panel Event");
		}*/
	}
	
	public String getOutputDirectory() {return outputDirectory.getValue();}

	public void setOutputDirectory(String outputDirectory) {this.outputDirectory.setValue(outputDirectory);}
	
	public boolean isSaveBenchmarkingExcel() {return saveBenchmarkingExcel.isSelected();}
	
	public void setSaveBenchmarkingExcel(boolean saveBenchmarkingExcel) {this.saveBenchmarkingExcel.setSelected(saveBenchmarkingExcel);}
	
	public boolean isSaveBenchmarkingTxt() {return saveBenchmarkingTxt.isSelected();}
	
	public void setSaveBenchmarkingTxt(boolean saveBenchmarkingTxt) {this.saveBenchmarkingTxt.setSelected(saveBenchmarkingTxt);}

}