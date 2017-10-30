package nist.textfield;
  
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import nist.textfield.validator.Validator;

public class TextFieldInputPanel<T> extends JPanel {
    private static final long serialVersionUID = 1L;
    private JLabel label;
    private ValidatedTextField<T> input;
    private Validator<T> validator;
        
    public TextFieldInputPanel(String label, String text, Validator<T> validator) {
    	this(label, text, 10, validator);
    }
        
    public TextFieldInputPanel(String label, String text, String units, Validator<T> validator) {
    	this(label, text, 10, validator);
    }

    public TextFieldInputPanel(String label, String text, int sz, Validator<T> validator) {
    	super(new FlowLayout(0));
          
    	this.validator = validator;
    	this.label = new JLabel(label);
    	input = new ValidatedTextField(sz, text, validator);
    	add(this.label);
    	add(input);
    }
    
    public void setValue(int value) {
    	input.setText(Integer.toString(value));
    }
    
    public void setValue(double value) {
    	input.setText(Double.toString(value));
    }
     
    public void setValue(String value) {
    	input.setText(value);
    }
    
    public boolean hasError() {
    	return input.hasError();
    }
    
    public void showError() {
    	input.showError();
    }
    
    public void hideError() {
    	input.hideError();
    }
        
    public String getText() { return input.getText(); }
    
    public void setLabelText(String text) {this.label.setText(text); }

    public T getValue() {
    	return validator.getValue(input.getText());
	}
      
    public void enableIgnoreErrors() {
    	input.enableIgnoreErrors();
	}
    
    public void disableIgnoreErrors() {
    	input.disableIgnoreErrors();
    }
        
    @Override
	public void setEnabled(boolean enabled) {
    	super.setEnabled(enabled);
          
    	label.setEnabled(enabled);
    	input.setEnabled(enabled);
    }
}
 