package nist.squire.guipanels;

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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import org.micromanager.utils.MMScriptException;

import com.swtdesigner.SwingResourceManager;

import mmcorej.StrVector;
import nist.filechooser.DirectoryChooserPanel;
import nist.ij.log.Log;
import nist.squire.AppParams;
import nist.textfield.TextFieldInputPanel;
import nist.textfield.validator.ValidatorInt;
import nist.textfield.validator.ValidatorPrefix;

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
	private JToggleButton useAbsorbance;
	private JToggleButton useTransmittance;
	private ButtonGroup useAbsorbanceGroup;
	
	// General Capture Settings
	private JPanel generalSettingsPanel;
	private DirectoryChooserPanel outputDirectory;
	private TextFieldInputPanel<Integer> numReplicatesInput;
	private TextFieldInputPanel<String> plateIdInput;
	private TextFieldInputPanel<Integer> numSampleInput;
	
	// Automated Capture Settings
	private JPanel automatedSettingsPanel;
	private JPanel fluorescentPanel;
	private JLabel fluorescentShutterLabel;
	private JComboBox fluorescentShutter;
	private JLabel fluorescentTurretLabel;
	private JComboBox fluorescentTurret;
	private JPanel transmittedPanel;
	private JLabel transmittedShutterLabel;
	private JComboBox transmittedShutter;
	private JLabel transmittedTurretLabel;
	private JComboBox transmittedTurret;
	private JCheckBox useAutoFocus;
	private JTable channelSettings;
	private DefaultTableModel dtm;
	private JButton addChannelButton;
	private JButton removeChannelButton;
	ArrayList<DefaultCellEditor> channelTypeEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<DefaultCellEditor> fluoroSettingEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<JComboBox> fluorescentSetting = new ArrayList<JComboBox>();
	ArrayList<DefaultCellEditor> transSettingEditor = new ArrayList<DefaultCellEditor>();
	ArrayList<JComboBox> transmittedSetting = new ArrayList<JComboBox>();

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
			useAbsorbanceGroup = new ButtonGroup();
				useAbsorbance = new JToggleButton("Capture Absorbance");
				useAbsorbance.setFocusPainted(false);
				useTransmittance = new JToggleButton("Capture Transmittance");
				useTransmittance.setFocusPainted(false);
			
		// General Settings Panel
		generalSettingsPanel = new JPanel(new GridBagLayout());
		generalSettingsPanel.setBorder(BorderFactory.createTitledBorder("General Settings"));
			outputDirectory = new DirectoryChooserPanel("Save Directory:", AppParams.getCoreSaveDir(), 30);
			outputDirectory.setToolTipText("<html>Where all of the data, images, and <br>metadata will be saved.</html>");
			plateIdInput = new TextFieldInputPanel("Sample Name: ", AppParams.getPlateID(), new ValidatorPrefix());
			plateIdInput.setToolTipText("<html>Please enter the name of the sample (used for saving files).</html>");
			numReplicatesInput = new TextFieldInputPanel("Number of replicates at each exposure: ", Integer.toString(AppParams.getNumReplicates()), new ValidatorInt(0,500));
			numReplicatesInput.setToolTipText("<html>Please enter the number of replicates for each exposure.");
			numSampleInput = new TextFieldInputPanel("Number of samples (FOV): ", Integer.toString(AppParams.getNumSamples()), new ValidatorInt(0,1000));
			numSampleInput.setToolTipText("<html>Please enter the number of replicates for each exposure.");
			
			
		// Automated Settings Panel
		automatedSettingsPanel = new JPanel(new GridBagLayout());
		automatedSettingsPanel.setBorder(BorderFactory.createTitledBorder("Automated Capture Settings"));
			fluorescentPanel = new JPanel(new GridBagLayout());
			fluorescentPanel.setBorder(BorderFactory.createTitledBorder("Fluorescent Settings"));
				fluorescentShutterLabel = new JLabel("Shutter");
				fluorescentShutter = new JComboBox();
				fluorescentTurretLabel = new JLabel("Turret");
				fluorescentTurret = new JComboBox();
			transmittedPanel = new JPanel(new GridBagLayout());
			transmittedPanel.setBorder(BorderFactory.createTitledBorder("Transmitted Settings"));
				transmittedShutterLabel = new JLabel("Shutter");
				transmittedShutter = new JComboBox();
				transmittedTurretLabel = new JLabel("Turret");
				transmittedTurret = new JComboBox();

			StrVector shutterDevices = AppParams.getShutterDevices();
			for (int i=0; i<shutterDevices.size(); i++) {
				fluorescentShutter.addItem(shutterDevices.get(i));
				transmittedShutter.addItem(shutterDevices.get(i));
			}
			fluorescentShutter.setSelectedIndex(0);
			transmittedShutter.setSelectedIndex(0);
			StrVector stateDevices = AppParams.getStateDevices();
			for (int i=0; i<stateDevices.size(); i++) {
				fluorescentTurret.addItem(stateDevices.get(i));
				transmittedTurret.addItem(stateDevices.get(i));
			}
			fluorescentTurret.setSelectedIndex(0);
			transmittedTurret.setSelectedIndex(0);
			
			useAutoFocus = new JCheckBox();
			useAutoFocus.setText("Use Autofocus");

			addChannelButton = new JButton("Add Channel");
			addChannelButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/plus.png"));
			removeChannelButton = new JButton("Remove Channel");
			removeChannelButton.setIcon(SwingResourceManager.getIcon(ControlPanel.class, "/minus.png"));
			String[] columnNames = { "<html><center>Channel<br>Name</center></html>",
					"<html><center>Type</center></html>",
					"<html><center>Transmitted<br>Setting</center></html>",
					"<html><center>Fluorescent<br>Setting</center></html>",
					"<html><center>Exposure</center></html>",
					"<html><center>Z-Offset</center></html>",
					"<html><center>Use<br>Autofocus</center></html>"};
			dtm = new DefaultTableModel();
			dtm.setColumnIdentifiers(columnNames);
			channelSettings = new JTable(dtm) {
				@Override
				public TableCellEditor getCellEditor(int row, int column) {
					int modelColumn = convertColumnIndexToModel(column);
					
					if (modelColumn==1) {
						return channelTypeEditor.get(row);
					} else if (modelColumn==2) {
						return transSettingEditor.get(row);
					} else if (modelColumn==3) {
						return fluoroSettingEditor.get(row);
					} else {
						return super.getCellEditor(row, column);
					}
				}
	            
				@Override
	            public Class getColumnClass(int column) {
					if (column==6) { 
						return Boolean.class;
					} else {
						return super.getColumnClass(column);
					}
	            }
			};
			((DefaultTableCellRenderer)channelSettings.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
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
		manualAutoGroup.add(useManual);
		startStopPanel.add(useManual,c);
		useManual.setSelected(true);
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx++;
		manualAutoGroup.add(useAutomated);
		startStopPanel.add(useAutomated,c);
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_END;
		useAbsorbanceGroup.add(useAbsorbance);
		startStopPanel.add(useAbsorbance, c);
		useAbsorbance.setSelected(true);
		c.gridx++;
		c.anchor = GridBagConstraints.LINE_START;
		useAbsorbanceGroup.add(useTransmittance);
		startStopPanel.add(useTransmittance, c);
		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		content.add(startStopPanel, c);
		
		// Create General Settings panel
		c.ipadx = 0;
		c.ipady = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 2;
		c.gridx = 0;
		generalSettingsPanel.add(outputDirectory,c);
		c.gridwidth = 1;
		c.gridy++;
		generalSettingsPanel.add(plateIdInput,c);
		c.gridx++;
		generalSettingsPanel.add(numReplicatesInput,c);
		c.gridx--;
		c.gridy++;
		c.gridwidth = 2;
		generalSettingsPanel.add(numSampleInput, c);
		c.gridx = 0;
		c.gridwidth = 1;
		c.gridy--;
		content.add(generalSettingsPanel,c);

		// Create Automated Settings Panel
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridy = 0;
		transmittedPanel.add(transmittedShutterLabel,c);
		c.gridx++;
		transmittedPanel.add(transmittedShutter,c);
		c.gridy++;
		transmittedPanel.add(transmittedTurret,c);
		c.gridx--;
		transmittedPanel.add(transmittedTurretLabel,c);
		c.gridx=0;
		c.gridy=0;
		automatedSettingsPanel.add(transmittedPanel,c);
		
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		fluorescentPanel.add(fluorescentShutterLabel,c);
		c.gridx++;
		fluorescentPanel.add(fluorescentShutter,c);
		c.gridy++;
		fluorescentPanel.add(fluorescentTurret,c);
		c.gridx--;
		fluorescentPanel.add(fluorescentTurretLabel,c);
		c.gridx = 1;
		c.gridy = 0;
		automatedSettingsPanel.add(fluorescentPanel,c);
		
		c.gridx = 0;
		c.gridy++;
		automatedSettingsPanel.add(addChannelButton,c);
		c.gridx++;
		automatedSettingsPanel.add(removeChannelButton,c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		automatedSettingsPanel.add(new JScrollPane(channelSettings),c);
		c.gridy = 4;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 1;
		content.add(automatedSettingsPanel,c);
		
		if (AppParams.getIsAutomated()) {
		} else {
		}
		
		// Nist Logo and About
//		c.fill = GridBagConstraints.NONE;
//		c.gridy++;
//		c.gridwidth = 1;
//		c.gridx = 1;
//		c.anchor = GridBagConstraints.CENTER;
//		content.add(new AddNistLogo(90,30),c);
		
		add(content,BorderLayout.NORTH);
	}

	private void initListeners() {
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
		
		transmittedTurret.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange()==ItemEvent.SELECTED) {
					try {
						StrVector deviceStates = AppParams.getDeviceStates(transmittedTurret.getSelectedItem().toString());
						int row = 0;
						int col = 2;
						for (JComboBox box: transmittedSetting) {
							box.removeAllItems();
							for (String state: deviceStates) {
								box.addItem(state);
							}
							box.setSelectedIndex(0);
							dtm.setValueAt(box.getSelectedItem().toString(), row++, col);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		fluorescentTurret.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange()==ItemEvent.SELECTED) {
					try {
						StrVector deviceStates = AppParams.getDeviceStates(fluorescentTurret.getSelectedItem().toString());
						int row = 0;
						int col = 3;
						for (JComboBox box: fluorescentSetting) {
							box.removeAllItems();
							for (String state: deviceStates) {
								box.addItem(state);
							}
							box.setSelectedIndex(0);
							dtm.setValueAt(box.getSelectedItem().toString(), row++, col);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		useManual.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (useManual.isSelected()) {
				}
			}
		});
		
		useAutomated.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (useAutomated.isSelected()) {
				}
			}
		});
		
		addChannelButton.addActionListener(new ActionListener() {
			@Override
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
			@Override
			public void actionPerformed(ActionEvent e) {
				if (channelSettings.getRowCount()>1) {
					removeChannel();
				}
			}
		});
	}

	@Override
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
	
	private void addChannel() throws Exception {
		dtm.addRow(new Object[] {"","","","","","",false});
		JComboBox channelGroup = new JComboBox();
		channelGroup.addItem("Absorbance");
		channelGroup.addItem("Phase/DIC");
		channelGroup.addItem("Fluorescence");
		channelTypeEditor.add(new DefaultCellEditor(channelGroup));
		
		StrVector transmittedStates = AppParams.getDeviceStates(transmittedTurret.getSelectedItem().toString());
		JComboBox transBox = new JComboBox();
		for (String state: transmittedStates) {
			transBox.addItem(state);
		}
		transBox.setSelectedIndex(0);
		transmittedSetting.add(transBox);
		transSettingEditor.add(new DefaultCellEditor(transBox));
		dtm.setValueAt(transBox.getSelectedItem().toString(), transmittedSetting.size()-1, 2);
		
		StrVector fluorescentStates = AppParams.getDeviceStates(fluorescentTurret.getSelectedItem().toString());
		JComboBox fluoroBox = new JComboBox();
		for (String state: fluorescentStates) {
			fluoroBox.addItem(state);
		}
		fluoroBox.setSelectedIndex(0);
		fluorescentSetting.add(fluoroBox);
		fluoroSettingEditor.add(new DefaultCellEditor(fluoroBox));
		dtm.setValueAt(fluoroBox.getSelectedItem().toString(), fluorescentSetting.size()-1, 3);

		dtm.setValueAt(false, fluorescentSetting.size()-1, 6);
	}
	
	private void removeChannel() {
		int rows = dtm.getRowCount()-1;
		fluorescentSetting.remove(rows);
		transmittedSetting.remove(rows);
		channelTypeEditor.remove(rows);
		transSettingEditor.remove(rows);
		fluoroSettingEditor.remove(rows);
		
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
	
	public String getFluorescentDevice() {return fluorescentTurret.getSelectedItem().toString();}
	
	public String getTransmittedDevice() {return transmittedTurret.getSelectedItem().toString();}
	
	public String getFluorescentShutter() {return fluorescentShutter.getSelectedItem().toString();}
	
	public String getTransmittedShutter() {return transmittedShutter.getSelectedItem().toString();}

	public boolean isAutomated() {return useAutomated.isSelected();}
	
	public boolean isAbsorbance() {return useAbsorbance.isSelected();}
	
	public String getCoreSaveDirectory() {return outputDirectory.getValue();}
	
	public void setCoreSaveDirectory(String coreSaveDir) {outputDirectory.setValue(coreSaveDir);}
	
	public String getPlateId() {return plateIdInput.getValue();}

	public void setPlateId(String plateId) {this.plateIdInput.setValue(plateId);}
	
	public void setNumSample(int numSample){this.numSampleInput.setValue(numSample);}

	public Integer getNumSample(){return numSampleInput.getValue();}
	
	public void setNumReplicates(int numReplicates){this.numReplicatesInput.setValue(numReplicates);}

	public Integer getNumReplicates(){return numReplicatesInput.getValue();}

}