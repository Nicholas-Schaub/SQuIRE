package nist.quantitativeabsorbance.guipanels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import mmcorej.StrVector;
import nist.filechooser.DirectoryChooserPanel;
import nist.ij.log.Log;
import nist.logofield.AddNistLogo;
import nist.quantitativeabsorbance.AppParams;
import nist.textfield.TextFieldInputPanel;
import nist.textfield.validator.ValidatorDbl;
import nist.textfield.validator.ValidatorInt;
import nist.textfield.validator.ValidatorPrefix;

import org.micromanager.utils.MMScriptException;

import com.swtdesigner.SwingResourceManager;

@SuppressWarnings("serial")
public class ControlPanel
extends JPanel
implements ActionListener
{
	// Start and Stop Panel
	private JPanel startStopPanel;
	private JButton startButton;
	private JButton stopButton;
	private JToggleButton useManual;
	private JToggleButton useAutomated;
	private ButtonGroup manualAutoGroup;
	private TextFieldInputPanel<Double> minExposureInput;
	private TextFieldInputPanel<Double> maxExposureInput;
	private TextFieldInputPanel<Integer> numReplicatesInput;
	
	// General Capture Settings
	private JPanel generalSettingsPanel;
	private TextFieldInputPanel<String> plateIdInput;
	private DirectoryChooserPanel outputDirectory;
	
	// Save Settings
	
	// Manual Capture Settings
	private JPanel manualSettingsPanel;
	private JLabel manualDescription;
	private TextFieldInputPanel<Integer> numSampleInput;
	private TextFieldInputPanel<Integer> channelInput;
	private TextFieldInputPanel<Integer> setBenchmarkDelay;
	
	// Automated Capture Settings
	private JPanel automatedSettingsPanel;
	private JLabel automatedDescription;
	private JLabel fluorescentShutterLabel;
	private JLabel transmittedShutterLabel;
	private JComboBox fluorescentShutter;
	private JComboBox transmittedShutter;
	private JLabel fluorescentTurretLabel;
	private JLabel condenserTurretLabel;
	private JComboBox fluorescentTurret;
	private JComboBox condenserTurret;
	private JTable channelSettings;
	private DefaultTableModel dtm;
	private JButton addChannelButton;
	private JButton removeChannelButton;
	ArrayList<DefaultCellEditor> channelTypeEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<DefaultCellEditor> fluoroDeviceEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<DefaultCellEditor> fluoroSettingEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<DefaultCellEditor> transDeviceEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<DefaultCellEditor> transSettingEditor = new ArrayList<DefaultCellEditor>();

	public ControlPanel() throws Exception
	{
		initElements();
		initPanel();
		initListeners();
	}

	private void initElements() throws Exception
	{
		// Start and stop acquisition Panel
		startStopPanel = new JPanel(new GridBagLayout());
		startStopPanel.setBorder(BorderFactory.createTitledBorder("Acquisition Control"));
			startButton = new JButton("Start");
			startButton.setForeground(new Color(0, 191, 0));
			startButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/control_play_blue.png"));
			stopButton = new JButton("Stop");
			stopButton.setForeground(new Color(211, 0, 0));
			stopButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/control_stop_blue.png"));
			manualAutoGroup = new ButtonGroup();
				useManual = new JToggleButton("Use Manual Settings");
				useManual.setFocusPainted(false);
				useAutomated = new JToggleButton("Use Automated Settings");
				useAutomated.setFocusPainted(false);
			manualAutoGroup.add(useManual);
			manualAutoGroup.add(useAutomated);
			numReplicatesInput = new TextFieldInputPanel("Replicates: ", Integer.toString(AppParams.getNumReplicates()),5, new ValidatorInt(0,53));
			numReplicatesInput.setToolTipText("<html>Please enter the number of replicates for each exposure.");
			minExposureInput = new TextFieldInputPanel("Min Exposure (ms): ", Double.toString(AppParams.getMinExposure()),5, new ValidatorDbl(0.001,503));
			minExposureInput.setToolTipText("<html>Please enter the minimum exposure to use for imaging.");
			maxExposureInput = new TextFieldInputPanel("Max Exposure (ms): ", Double.toString(AppParams.getMaxExposure()),5, new ValidatorDbl(0,10007));
			maxExposureInput.setToolTipText("<html>Please enter the maximum exposure to use for imaging.");
			
		// General Settings Panel
		generalSettingsPanel = new JPanel(new GridBagLayout());
		generalSettingsPanel.setBorder(BorderFactory.createTitledBorder("General Settings"));
			plateIdInput = new TextFieldInputPanel("Sample Name: ", AppParams.getPlateID(), new ValidatorPrefix());
			plateIdInput.setToolTipText("<html>Please enter the name of the sample (used for saving files).</html>");
			outputDirectory = new DirectoryChooserPanel("Save Directory:", AppParams.getCoreSaveDir(), 30);
			outputDirectory.setToolTipText("<html>Where all of the data, images, and <br>metadata will be saved.</html>");

		// Manual Settings Panel
		manualSettingsPanel = new JPanel(new GridBagLayout());
		manualSettingsPanel.setBorder(BorderFactory.createTitledBorder("Manual Capture Settings"));
			manualDescription = new JLabel();
			manualDescription.setText("Use these settings for manually moving samples or switching filters.");
			numSampleInput = new TextFieldInputPanel("Number of samples: ", Integer.toString(AppParams.getNumSamples()), new ValidatorInt(0, 97));
			numSampleInput.setToolTipText("<html>Please enter the number of samples to imaged.</html>");
			channelInput = new TextFieldInputPanel("Number of Channels: ", Integer.toString(AppParams.getChannels()), new ValidatorInt(1,7));
			channelInput.setToolTipText("<html>Please enter the number of channels used for imaging.");
			
		// Automated Settings Panel
		automatedSettingsPanel = new JPanel(new GridBagLayout());
		automatedSettingsPanel.setBorder(BorderFactory.createTitledBorder("Automated Capture Settings"));
			automatedDescription = new JLabel();
			automatedDescription.setText("Use these settings in addition to the Plate Scan tab to automatically capture images.");
			fluorescentShutterLabel = new JLabel("Fluorescent Shutter");
			fluorescentShutter = new JComboBox();
			fluorescentTurretLabel = new JLabel("Fluorescent Turret");
			fluorescentTurret = new JComboBox();
			transmittedShutterLabel = new JLabel("Transmitted Shutter");
			transmittedShutter = new JComboBox();
			condenserTurretLabel =  new JLabel("Condesner Turret");
			condenserTurret = new JComboBox();
			StrVector shutterDevices = AppParams.getShutterDevices();
			for (int i=0; i<shutterDevices.size(); i++) {
				fluorescentShutter.addItem(shutterDevices.get(i));
				transmittedShutter.addItem(shutterDevices.get(i));
			}
			StrVector stateDevices = AppParams.getStateDevices();
			for (int i = 0; i<stateDevices.size();i++) {
				fluorescentTurret.addItem(stateDevices.get(i));
				condenserTurret.addItem(stateDevices.get(i));
			}
			addChannelButton = new JButton("Add Channel");
			addChannelButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/plus.png"));
			removeChannelButton = new JButton("Remove Channel");
			removeChannelButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/minus.png"));
			String[] columnNames = { "<html><center>Channel<br>Name</center></html>",
					"<html><center>Type</center></html>",
					"<html><center>Turret 1</center></html>",
					"<html><center>Turret 1<br>Setting</center></html>",
					"<html><center>Turret 2</center></html>",
					"<html><center>Turret 2<br>Setting</center></html>",
					"<html><center>Exposure</center></html>",
					"<html><center>Z-Offset</center></html>"};
			dtm = new DefaultTableModel();
			dtm.setColumnIdentifiers(columnNames);
			channelSettings = new JTable(dtm) {
				public TableCellEditor getCellEditor(int row, int column) {
					int modelColumn = convertColumnIndexToModel(column);
					
					if (modelColumn==1) {
						return channelTypeEditor.get(row);
					} else if (modelColumn==2) {
						return fluoroDeviceEditor.get(row);
					} else if (modelColumn==3) {
						return fluoroSettingEditor.get(row);
					} else if (modelColumn==4) {
						return transDeviceEditor.get(row);
					} else if (modelColumn==5) {
						return transSettingEditor.get(row);
					} else {
						return super.getCellEditor(row, column);
					}
				}
			};
			((DefaultTableCellRenderer)channelSettings.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
			addChannel();
			dtm.setValueAt("Absorbance", 0, 0);
			dtm.setValueAt("Absorbance", 0, 1);
			channelSettings.getTableHeader().setPreferredSize(new Dimension(channelSettings.getColumnModel().getTotalColumnWidth(),37));
			channelSettings.setPreferredScrollableViewportSize(new Dimension(51,101));
	}

	private void initPanel()
	{
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Basic constraints
		c.insets = new Insets(2, 8, 2, 8);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		
		// Create the startStopPanel
		c.ipady = 10;
		c.ipadx = 10;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		startStopPanel.add(startButton,c);
		c.gridx++;
		startStopPanel.add(stopButton,c);
		c.gridy++;
		c.ipady = 1;
		c.ipadx = 0;
		c.gridx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		startStopPanel.add(useManual,c);
		useManual.setSelected(true);
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx++;
		startStopPanel.add(useAutomated,c);
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		startStopPanel.add(outputDirectory,c);
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 0;
		startStopPanel.add(plateIdInput,c);
		c.gridx++;
		startStopPanel.add(numReplicatesInput,c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		startStopPanel.add(minExposureInput,c);
		c.gridx++;
		startStopPanel.add(maxExposureInput,c);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 0;
		c.gridwidth = 2;
		content.add(startStopPanel, c);
		
		// Create Manual Settings Panel
		c.fill = GridBagConstraints.NONE;
		c.gridy = 0;
		manualSettingsPanel.add(manualDescription,c);
		c.gridy++;
		c.gridwidth = 1;
		manualSettingsPanel.add(numSampleInput,c);
		c.gridx++;
		manualSettingsPanel.add(channelInput,c);
		c.gridy = 3;
		c.gridwidth = 2;
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		content.add(manualSettingsPanel,c);

		// Create Automated Settings Panel
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		//automatedSettingsPanel.add(automatedDescription,c);
		//c.gridy++;
		c.gridwidth = 1;
		automatedSettingsPanel.add(fluorescentShutterLabel,c);
		c.gridx++;
		c.gridwidth = 3;
		automatedSettingsPanel.add(fluorescentShutter,c);
		c.gridwidth = 1;
		c.gridx += 3;
		automatedSettingsPanel.add(fluorescentTurretLabel,c);
		c.gridx++;
		automatedSettingsPanel.add(fluorescentTurret,c);
		c.gridy++;
		c.gridx = 0;
		automatedSettingsPanel.add(transmittedShutterLabel,c);
		c.gridx++;
		c.gridwidth = 3;
		automatedSettingsPanel.add(transmittedShutter,c);
		c.gridwidth = 1;
		c.gridx += 3;
		automatedSettingsPanel.add(condenserTurretLabel,c);
		c.gridx++;
		automatedSettingsPanel.add(condenserTurret,c);
		c.gridx = 3;
		c.gridy++;
		automatedSettingsPanel.add(addChannelButton,c);
		c.gridx++;
		automatedSettingsPanel.add(removeChannelButton,c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 6;
		c.fill = GridBagConstraints.HORIZONTAL;
		automatedSettingsPanel.add(new JScrollPane(channelSettings),c);
		c.gridy = 4;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		content.add(automatedSettingsPanel,c);
		
		if (AppParams.getIsAutomated()) {
			useAutomated.setSelected(true);
			manualSettingsPanel.setVisible(false);
		} else {
			useManual.setSelected(true);
			automatedSettingsPanel.setVisible(false);
		}
		
		// Nist Logo and About
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		c.gridwidth = 1;
		c.gridx = 1;
		c.anchor = GridBagConstraints.CENTER;
		content.add(new AddNistLogo(90,30),c);
		
		add(content,BorderLayout.NORTH);
	}

	private void initListeners() {
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
		
		useManual.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (useManual.isSelected()) {
					manualSettingsPanel.setVisible(true);
					automatedSettingsPanel.setVisible(false);
				}
			}
		});
		
		useAutomated.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (useAutomated.isSelected()) {
					manualSettingsPanel.setVisible(false);
					automatedSettingsPanel.setVisible(true);
				}
			}
		});
		
		addChannelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					addChannel();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		removeChannelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (channelSettings.getRowCount()>1) {
					removeChannel();
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == startButton) {
			Log.debug("Start Button Pressed");
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
		}
		else if (e.getSource() == stopButton) {
			Log.debug("Stop Button Pressed");
			try {
				AppParams.getInstance().cancel();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			Log.error("Invalid Control Panel Event");
		}
	}
	
	private class StateDeviceBox extends JComboBox {
		JComboBox deviceStateBox;
		
		public StateDeviceBox() throws Exception {
			super();
			StrVector stateDevices = AppParams.getStateDevices();
			for (int i = 0; i<stateDevices.size();i++) {
				this.addItem(stateDevices.get(i));
			}
			this.setSelectedIndex(0);

			deviceStateBox = new JComboBox();
			StrVector deviceStates = AppParams.getDeviceStates(stateDevices.get(0));
			for (int i = 0; i<deviceStates.size();i++) {
				deviceStateBox.addItem(deviceStates.get(i));
			}
			deviceStateBox.setSelectedIndex(0);
			
			addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					// TODO Auto-generated method stub
					if (e.getStateChange()==ItemEvent.SELECTED) {
						StateDeviceBox parentBox = (StateDeviceBox) e.getSource();
						JComboBox childBox = parentBox.getDeviceStateBox();
						StrVector deviceStates = new StrVector();
						try {
							deviceStates = AppParams.getDeviceStates(parentBox.getSelectedItem().toString());
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						childBox.removeAllItems();
						for (int i = 0; i<deviceStates.size(); i++) {
							childBox.addItem(deviceStates.get(i).toString());
						}
						childBox.setSelectedIndex(0);
						int row = channelSettings.getSelectedRow();
						int column = channelSettings.getSelectedColumn();
						channelSettings.setValueAt(deviceStates.get(0).toString(), row, column+1);
					}
				}
			});
		}
		
		public JComboBox getDeviceStateBox() {
			return deviceStateBox;
		}

	}
	
	private void addChannel() throws Exception {
		dtm.addRow(new Object[] {"","","","","","","",""});
		JComboBox channelGroup = new JComboBox();
		channelGroup.addItem("Absorbance");
		channelGroup.addItem("Phase/DIC");
		channelGroup.addItem("Fluorescence");
		channelTypeEditor.add(new DefaultCellEditor(channelGroup));
		StateDeviceBox fluorescentGroup = new StateDeviceBox();
		fluoroDeviceEditor.add(new DefaultCellEditor(fluorescentGroup));
		StateDeviceBox transmittedGroup = new StateDeviceBox();
		transDeviceEditor.add(new DefaultCellEditor(transmittedGroup));
		JComboBox fluorescentSetting = fluorescentGroup.getDeviceStateBox();
		fluoroSettingEditor.add(new DefaultCellEditor(fluorescentSetting));
		JComboBox transmittedSetting = transmittedGroup.getDeviceStateBox();
		transSettingEditor.add(new DefaultCellEditor(transmittedSetting));
	}
	
	private void removeChannel() {
		int rows = dtm.getRowCount();
		dtm.removeRow(rows-1);;
	}
	
	public Object[][] getAutomatedSettings() {
		int rows = dtm.getRowCount();
		int cols = dtm.getColumnCount();
		Object[][] tableData = new Object[rows][cols];
		for (int i = 0; i<rows; i++) {
			for (int j = 0; j<cols; j++) {
				tableData[i][j] = dtm.getValueAt(i, j);
			}
		}
		return tableData;
	}
	
	public String getFluorescentShutter() {return fluorescentShutter.getSelectedItem().toString();}
	
	public String getTransmittedShutter() {return transmittedShutter.getSelectedItem().toString();}
	
	public boolean isAutomated() {return useAutomated.isSelected();}
	
	public String getCoreSaveDirectory() {return outputDirectory.getValue();}
	
	public void setCoreSaveDirectory(String coreSaveDir) {outputDirectory.setValue(coreSaveDir);}
	
	public String getPlateId() {return plateIdInput.getValue();}

	public void setPlateId(String plateId) {this.plateIdInput.setValue(plateId);}
	
	public void setBenchmarkDelay(Integer benchmarkDelay){this.setBenchmarkDelay.setValue(benchmarkDelay);} 
	
	public Integer getBenchmarkDelay(){return (Integer) setBenchmarkDelay.getValue();}
	
	public void setNumSample(int numSample){this.numSampleInput.setValue(numSample);}

	public Integer getNumSample(){return (Integer) numSampleInput.getValue();}
	
	public void setNumReplicates(int numReplicates){this.numReplicatesInput.setValue(numReplicates);}

	public Integer getNumReplicates(){return (Integer) numReplicatesInput.getValue();}
	
	public void setNumChannels(int numChannels){this.channelInput.setValue(numChannels);}

	public Integer getNumChannels(){return (Integer) channelInput.getValue();}
	
	public void setMinExposure(double minExposure){this.minExposureInput.setValue(minExposure);}

	public Double getMinExposure(){return (Double) minExposureInput.getValue();}

	public void setMaxExposure(double minExposure){this.maxExposureInput.setValue(minExposure);}

	public Double getMaxExposure(){return (Double) maxExposureInput.getValue();}

}