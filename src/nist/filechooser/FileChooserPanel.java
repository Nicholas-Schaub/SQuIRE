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
 
public class FileChooserPanel extends JPanel {
   private static final long serialVersionUID = 1L;
   private JLabel label;
   private JTextField input;
   private JButton button;

   public FileChooserPanel(String label, String defLocation) {
     super(new FlowLayout(1));
     
     File f = new File(defLocation);
     f.mkdirs();
     
     this.label = new JLabel(label);
     input = new JTextField(defLocation, 20);
     button = new JButton("Browse");
     input.setToolTipText(label);
     
     button.addActionListener(new ActionListener() {
       @Override
	public void actionPerformed(ActionEvent arg0) {
         JFileChooser chooser = new JFileChooser(input.getText());
         chooser.setFileSelectionMode(2);
         
         int val = chooser.showOpenDialog(button);
         if (val == 0) {
           input.setText(chooser.getSelectedFile().getAbsolutePath());
         }
         
       }
       
     });
     add(this.label);
     add(input);
     add(button);
   }
   
   public FileChooserPanel(String label) {
     this(label, System.getProperty("user.home"));
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

   public String getValue()
   {
     return input.getText();
   }

   public File getFile() {
     return new File(input.getText());
   }
}