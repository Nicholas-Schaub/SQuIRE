package nist.filechooser;
  
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.swtdesigner.SwingResourceManager;

import nist.textfield.ValidatedTextField;
import nist.textfield.validator.ValidatorFile;

      
public class DirectoryChooserPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JLabel label;
	private JButton button;
	private ValidatedTextField<File> input;

	public DirectoryChooserPanel(String label, String defLocation, int sz) {
		super(new FlowLayout(1));
  
		File f = new File(defLocation);
		f.mkdirs();
  
		this.label = new JLabel(label);
  
		input = new ValidatedTextField(sz, defLocation, new ValidatorFile());
  
		button = new JButton("Browse");
		button.setIcon(SwingResourceManager.getIcon(DirectoryChooserPanel.class, "/folder.png"));
      
		input.setToolTipText(label);
		button.addActionListener(new ActionListener() {
    
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser(input.getText());
				chooser.setFileSelectionMode(1);
      
				int val = chooser.showOpenDialog(button);
				
				if (val == 0) {
					input.setText(chooser.getSelectedFile().getAbsolutePath());
				}
      
			}
    
		});
   
		add(this.label);
		add(input);
		add(button);
		hasError();
	}
    
	public void setEnabled(boolean val) {
		label.setEnabled(val);
		input.setEnabled(val);
		button.setEnabled(val);
		super.setEnabled(val);
	}

	public DirectoryChooserPanel(String label, String defLocation) {
		this(label, defLocation, 20);
	}

	public DirectoryChooserPanel(String label) {
		this(label, System.getProperty("user.home"));
    	}
  
    public DirectoryChooserPanel(String label, int sz) {
    	this(label, System.getProperty("user.home"), sz);
    }
  
    public JTextField getInputField() {
    	return input;
    }
    
    public boolean hasError() {
    	return input.hasError();
    }
    
    public void showError() {
    	input.setBackground(Color.RED);
    }
    
    public void hideError() {
    	input.setBackground(Color.WHITE);
    }
    	
    public void setValue(String value) {
    	input.setText(value);
    }
  
    public String getValue() {
    	return input.getText();
    }
  }