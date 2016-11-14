package nist.squire.guipanels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.LineBorder;

import mmcorej.CMMCore;
import nist.ij.log.Log;
import nist.squire.AppParams;
import nist.textfield.TextFieldInputPanel;
import nist.textfield.validator.ValidatorDbl;
import nist.textfield.validator.ValidatorInt;

import org.micromanager.api.MultiStagePosition;
import org.micromanager.api.PositionList;
import org.micromanager.api.ScriptInterface;
import org.micromanager.api.StagePosition;
import org.micromanager.hcs.AFPlane;
import org.micromanager.hcs.HCSException;
import org.micromanager.hcs.ParentPlateGUI;
import org.micromanager.hcs.PlatePanel;
import org.micromanager.hcs.WellPositionList;
import org.micromanager.utils.MMScriptException;
import org.micromanager.utils.TextUtils;

import com.swtdesigner.SwingResourceManager;

import ij.IJ;

public class PlateConfigPanel 
extends JPanel
implements ActionListener, ParentPlateGUI
{
	// Get CMMCore associated with the plugin.
	private CMMCore core_ = AppParams.getCore_();
	
	// Panel components
	//Labels
	private JLabel spacingLabel;
	//Text Inputs
	private TextFieldInputPanel spacingFieldX_;
	private TextFieldInputPanel spacingFieldY_;
	private TextFieldInputPanel columnsField_;
	private TextFieldInputPanel rowsField_;
	//Push Buttons
	private JButton refreshButton;
	private JButton calibrateXyButton;
	private JButton setPositionListButton;
	//Radio Buttons
	private JComboBox plateIDCombo_;
	private static final long serialVersionUID = 1L;
	private NISTPlate plate_;
	private PlatePanel platePanel_;
	private ScriptInterface app_ = AppParams.getApp_();
	private Point2D.Double xyStagePos_;
	private double zStagePos_;
	private Point2D.Double cursorPos_;
	private String stageWell_;
	private String cursorWell_;
	PositionList threePtList_;
	AFPlane focusPlane_;
	private final String PLATE_FORMAT_ID = "plate_format_id";
	private final String SITE_SPACING_X  = "site_spacing"; //keep string for backward compatibility
	private final String SITE_SPACING_Y  = "site_spacing_y";
	private final String SITE_OVERLAP    = "site_overlap"; //in µm
	private final String SITE_ROWS       = "site_rows";
	private final String SITE_COLS       = "site_cols";

	private JLabel statusLabel_;
	static private final String VERSION_INFO = "1.4.2";
	static private final String COPYRIGHT_NOTICE = "Copyright by UCSF, 2013";
	static private final String DESCRIPTION = "Generate imaging site positions for micro-well plates and slides";
	static private final String INFO = "Not available";
	private final ButtonGroup toolButtonGroup = new ButtonGroup();
	private JRadioButton rdbtnSelectWells_;
	private JRadioButton rdbtnMoveStage_;

	private ButtonGroup spacingButtonGroup = new ButtonGroup();
	private JRadioButton rdbtnDifferentXYSpacing_;
	private JRadioButton rdbtnFieldOfViewSpacing_;

	private double Xspacing = Math.round(core_.getImageWidth()*0.1);
	private double Yspacing = Math.round(core_.getImageHeight()*0.1);
	private long width  = core_.getImageWidth();
	private long height = core_.getImageHeight();
	private double cameraXFieldOfView = core_.getPixelSizeUm() * width;
	private double cameraYFieldOfView = core_.getPixelSizeUm() * height;
	
	public PlateConfigPanel()
	{	
		initElements();
		initPanel();
		initListeners();
	}
	
	private void initListeners() {
		// TODO Auto-generated method stub
		
	}

	private void initPanel() {
		// TODO Auto-generated method stub
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.insets = new Insets(2,7,2,7);
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 449;
		c.ipady = 311;
		c.gridy = 0;
		content.add(platePanel_,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = -1;
		c.ipady = 0;
		c.gridx = 0;
		c.gridy++;
		c.weightx = 1;
		c.gridwidth = 3;
		content.add(statusLabel_,c);
		
		c.gridy++;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		content.add(refreshButton,c);
		
		c.gridx++;
		content.add(calibrateXyButton,c);
		
		c.gridx++;
		content.add(setPositionListButton,c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		c.gridheight = 1;
		content.add(plateIDCombo_,c);
		
		c.gridx++;
		content.add(rowsField_,c);
		
		c.gridx++;
		content.add(columnsField_,c);
		
		c.gridy++;
		c.gridx = 0;
		content.add(spacingLabel,c);
		
		c.gridy++;
		content.add(rdbtnFieldOfViewSpacing_,c);
		
		c.gridx++;
		content.add(spacingFieldX_,c);
		
		c.gridx++;
		content.add(rdbtnSelectWells_,c);
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		content.add(rdbtnDifferentXYSpacing_,c);
		
		c.gridx++;
		content.add(spacingFieldY_,c);
		
		c.gridx++;
		content.add(rdbtnMoveStage_,c);
		
		add(content);
	}

	private void initElements() {
		// TODO Auto-generated method stub
		
		// Stage and mouse cursor position
		xyStagePos_ = new Point2D.Double(0.0D, 0.0D);
		cursorPos_ = new Point2D.Double(0.0D, 0.0D);
		
		// Plate interface
		plate_ = new NISTPlate();
		
		stageWell_ = "undef";
		cursorWell_ = "undef";
		
		focusPlane_ = null;
		
		platePanel_ = new PlatePanel(plate_,null,this);
		try {
			platePanel_.setApp(app_);
		} catch (HCSException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	     
		JLabel plateFormatLabel = new JLabel();
		plateFormatLabel.setText("Plate format:");
		
		plateIDCombo_ = new JComboBox();
		plateIDCombo_.setAlignmentX(0.0F);
		plateIDCombo_.addItem(NISTPlate.NIST_6_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_12_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_12_WELL_TRANS);
		plateIDCombo_.addItem(NISTPlate.NIST_24_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_48_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_96_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_384_WELL);
		plateIDCombo_.addItem(NISTPlate.NIST_SLIDE_HOLDER);
		plateIDCombo_.addItem(NISTPlate.NIST_SINGLE_SLIDE);
		plateIDCombo_.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				
				plate_.initializePlate((String) plateIDCombo_.getSelectedItem());

				updateXYspacing();         
				PositionList sites = generateSites(Integer.parseInt(rowsField_.getText()),
						Integer.parseInt(columnsField_.getText()),
						Xspacing,Yspacing);
				try {
					platePanel_.refreshImagingSites(sites);
				} catch (HCSException e1) {
					app_.logError(e1);
				}
				platePanel_.repaint();
			}
		});
		
		JLabel imagingSitesLabel = new JLabel();
		imagingSitesLabel.setText("Imaging Sites");
		
		rowsField_ = new TextFieldInputPanel("# of Rows: ", "1", new ValidatorInt(1,10000));
		columnsField_ = new TextFieldInputPanel("# of Columns: ", "1", new ValidatorInt(1,10000));
		
		spacingLabel = new JLabel();
		spacingLabel.setText("Spacing [um]");
		
		spacingFieldX_ = new TextFieldInputPanel("X Overlap: ", Double.toString(Xspacing),6, new ValidatorDbl(0,10000));
		spacingFieldY_ = new TextFieldInputPanel("Y Overlap: ", Double.toString(Yspacing),6, new ValidatorDbl(0,10000));

		rdbtnDifferentXYSpacing_ = new JRadioButton("Set XY Spacing");
		rdbtnDifferentXYSpacing_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (rdbtnDifferentXYSpacing_.isSelected()) {
					spacingLabel.setText("Spacing X,Y (um)");
					spacingFieldX_.setLabelText("X Spacing: ");
					spacingFieldY_.setLabelText("Y Spacing: ");
				}
			}
		});

		rdbtnFieldOfViewSpacing_ = new JRadioButton("Image Overlap");
		rdbtnFieldOfViewSpacing_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (rdbtnFieldOfViewSpacing_.isSelected()) {
					spacingLabel.setText("Overlap (um)");
					spacingFieldX_.setLabelText("X Overlap: ");
					spacingFieldY_.setLabelText("Y Overlap: ");
				}
			}
		});

		spacingButtonGroup.add(rdbtnDifferentXYSpacing_);
		spacingButtonGroup.add(rdbtnFieldOfViewSpacing_);
		rdbtnFieldOfViewSpacing_.setSelected(true);
		
		refreshButton = new JButton();
		refreshButton.setIcon(SwingResourceManager.getIcon(PlateConfigPanel.class, "/arrow_refresh.png"));
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				regenerate();
			}
		});
		refreshButton.setText("Refresh");
		
		calibrateXyButton = new JButton();
		calibrateXyButton.setIcon(SwingResourceManager.getIcon(PlateConfigPanel.class, "/cog.png"));
		calibrateXyButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				calibrateXY();
			}
		});
		calibrateXyButton.setText("Calibrate XY...");

		setPositionListButton = new JButton();
		setPositionListButton.setIcon(SwingResourceManager.getIcon(PlateConfigPanel.class, "/table.png"));
		setPositionListButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				setPositionList();
			}
		});
		setPositionListButton.setText("Build MM List");
		
		rdbtnSelectWells_ = new JRadioButton("Select Wells");
		rdbtnSelectWells_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (rdbtnSelectWells_.isSelected()) {
					platePanel_.setTool(PlatePanel.Tool.SELECT);
				}
			}
		});
		toolButtonGroup.add(rdbtnSelectWells_);
		rdbtnSelectWells_.setSelected(true);
		// set default tool
		platePanel_.setTool(PlatePanel.Tool.SELECT);

		rdbtnMoveStage_ = new JRadioButton("Move Stage");
		rdbtnMoveStage_.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rdbtnMoveStage_.isSelected()) {
					platePanel_.setTool(PlatePanel.Tool.MOVE);
				}
			}
		});
		toolButtonGroup.add(rdbtnMoveStage_);
		rdbtnSelectWells_.setSelected(false);

		statusLabel_ = new JLabel();
		statusLabel_.setBorder(new LineBorder(new Color(0, 0, 0)));
		statusLabel_.setText("<html>Cursor: <br>Stage:");
	}
	
	protected void calibrateXY() {
		if (app_ == null) {
			return;
		}
		int ret = JOptionPane.showConfirmDialog(this, "Manually position the XY stage over the center of the well A01 and press OK",
				"XYStage origin setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (ret == JOptionPane.OK_OPTION) {
			try {
				app_.setXYOrigin(plate_.getFirstWellX(), plate_.getFirstWellY());
				regenerate();
				Point2D.Double pt = app_.getXYStagePosition();
				JOptionPane.showMessageDialog(this, "XY Stage set at position: " + pt.x + "," + pt.y);
			} catch (MMScriptException e) {
				displayError(e.getMessage());
			}
		}
	}
	
	private void regenerate() {
		WellPositionList[] selectedWells = platePanel_.getSelectedWellPositions();

		updateXYspacing();         
		PositionList sites = generateSites(Integer.parseInt(rowsField_.getText()), Integer.parseInt(columnsField_.getText()),
				Xspacing,Yspacing);
		plate_.initialize((String) plateIDCombo_.getSelectedItem());
		try {
			platePanel_.refreshImagingSites(sites);
		} catch (HCSException e) {
			displayError(e.getMessage());
		}

		platePanel_.setSelectedWells(selectedWells);
		platePanel_.repaint();
	}
	
	private void setPositionList() {
		WellPositionList[] wpl = platePanel_.getSelectedWellPositions();
		PositionList platePl = new PositionList();
		for (WellPositionList wpl1 : wpl) {
			PositionList pl = PositionList.newInstance(wpl1.getSitePositions());
			for (int j = 0; j < pl.getNumberOfPositions(); j++) {
				MultiStagePosition mpl = pl.getPosition(j);
				// make label unique
				String wellLbl = plate_.getWellLabel(wpl1.getRow()+1, wpl1.getColumn()+1);
				mpl.setLabel(wellLbl + "_" + mpl.getLabel());
				if (app_ != null) {
					mpl.setDefaultXYStage(app_.getXYStageName());
					mpl.setDefaultZStage(app_.getMMCore().getFocusDevice());
				}
				// set the proper XYstage name
				for (int k = 0; k < mpl.size(); k++) {
					StagePosition sp = mpl.get(k);
					if (sp.numAxes == 2) {
						sp.stageName = mpl.getDefaultXYStage();
					}
				}
				// add Z position if 3-point focus is enabled
				/*if (useThreePtAF()) {
					if (focusPlane_ == null) {
						displayError("3-point AF is seleced but 3 points are not defined.");
						return;
					}
					// add z position from the 3-point plane estimate
					StagePosition sp = new StagePosition();
					sp.numAxes = 1;
					sp.x = focusPlane_.getZPos(mpl.getX(), mpl.getY());
					sp.stageName = mpl.getDefaultZStage();
					mpl.add(sp);
				}*/
				platePl.addPosition(pl.getPosition(j));
			}
		}

		try {
			if (app_ != null) {
				app_.setPositionList(platePl);
				IJ.log("Position List Set!");
			}
		} catch (MMScriptException e) {
			displayError(e.getMessage());
		}
	}
	
	private PositionList generateSites(int rows, int cols, double spacingX,  double spacingY) {
		PositionList sites = new PositionList();
		System.out.println("# Rows : " + rows + ", # Cols : " + cols + " ,spacingX = " + spacingX + " ,spacingY = " + spacingY);
		for (int i = 0; i < rows; i++) {
			// create snake-like pattern inside the well:
			boolean isEven = i % 2 == 0;
			int start = isEven ? 0 : cols - 1;
			int end = isEven ? cols : - 1;
			int j = start;
			// instead of using a for loop, cycle backwards on odd rows
			while ( (isEven && j < end) || (!isEven && j > end)  ) {
				double x;
				double y;
				if (cols > 1) {
					x = -cols * spacingX / 2.0 + spacingX * j + spacingX / 2.0;
				} else {
					x = 0.0;
				}

				if (rows > 1) {
					y = -rows * spacingY / 2.0 + spacingY * i + spacingY / 2.0;
				} else {
					y = 0.0;
				}

				MultiStagePosition mps = new MultiStagePosition();
				StagePosition sp = new StagePosition();
				sp.numAxes = 2;
				sp.x = x;
				sp.y = y;
				System.out.println("(" + i + "," + j + ") = " + x + "," + y);

				mps.add(sp);
				mps.setGridCoordinates(i, j);
				sites.addPosition(mps);
				if (isEven) {
					j++;
				} else {
					j--;
				}

			}
		}

		return sites;
	}
	
	private void updateXYspacing() {
		Xspacing = Double.parseDouble(spacingFieldX_.getText().replace(',','.'));
		Yspacing = Double.parseDouble(spacingFieldY_.getText().replace(',','.'));
		if (rdbtnFieldOfViewSpacing_.isSelected()) {
			Xspacing = cameraXFieldOfView - Xspacing;
			Yspacing = cameraYFieldOfView - Yspacing;
			Log.mandatory("Field of View Spacing");
		} else {
			Log.mandatory("XY Spacing");
		}
	}

	public void actionPerformed(ActionEvent paramActionEvent) {
		// TODO Auto-generated method stub
		
	}

	public void updatePointerXYPosition(double x, double y, String wellLabel, 
			String siteLabel) {
		cursorPos_.x = x;
		cursorPos_.y = y;
		cursorWell_ = wellLabel;

		displayStatus();
	}

	private void displayStatus() {
		if (statusLabel_ == null) {
			return;
		}

		String statusTxt = "<html>Cursor: X=" + TextUtils.FMT2.format(cursorPos_.x) + "um, Y=" + TextUtils.FMT2.format(cursorPos_.y) + "um, " + cursorWell_
				+ ((useThreePtAF() && focusPlane_ != null) ? ", Z->" : "")//TextUtils.FMT2.format(focusPlane_.getZPos(cursorPos_.x, cursorPos_.y)) + "um" : "")
				+ "<br>Stage: X=" + TextUtils.FMT2.format(xyStagePos_.x) + "um, Y=" + TextUtils.FMT2.format(xyStagePos_.y) + "um, Z=" + TextUtils.FMT2.format(zStagePos_) + "um, "
				+ stageWell_ + "</html>";
		statusLabel_.setText(statusTxt);
	}

	public void updateStagePositions(double x, double y, double z, String wellLabel, String siteLabel) {
		xyStagePos_.x = x;
		xyStagePos_.y = y;
		zStagePos_ = z;
		stageWell_ = wellLabel;

		displayStatus();
	}

	public String getXYStageName() {
		if (app_ != null) {
			return app_.getXYStageName();
		} else {
			return NISTPlate.DEFAULT_XYSTAGE_NAME;
		}
	}

	public boolean useThreePtAF() {
		// TODO Auto-generated method stub
		return false;
	}

	public PositionList getThreePointList() {
		// TODO Auto-generated method stub
		return null;
	}

	public java.lang.Double getThreePointZPos(double paramDouble1, double paramDouble2) {
		// TODO Auto-generated method stub
		return null;
	}

	public void displayError(String paramString) {
		// TODO Auto-generated method stub
		
	}

}
