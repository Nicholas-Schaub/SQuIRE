package nist.textfield;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

import nist.textfield.validator.Validator;

public class ValidatedTextField<T> extends JTextField {
	private static final long serialVersionUID = 1L;
	private boolean ignoreErrors = false;

	private Validator<T> validator;

	public ValidatedTextField(int size, String text, Validator<T> validator) {
		super(size);
		setText(text);
		this.validator = validator;

		PlainDocument doc = (PlainDocument)super.getDocument();

		doc.setDocumentFilter(new TextFieldFilter(this, validator));
		setToolTipText(validator.getErrorText());
		hasError();
	}

	public void showError()
	{
		setBackground(Color.RED);
	}

	public void hideError()
	{
		setBackground(Color.WHITE);
	}

	public boolean hasError()
	{
		if (validator.validate(getText())) {
			hideError();
			return false;
		}
		showError();
		return true;
	}

	public void enableIgnoreErrors()
	{
		ignoreErrors = true;
	}

	public void disableIgnoreErrors()
	{
		ignoreErrors = false;
	}

	class TextFieldFilter<V> extends DocumentFilter {
		private JTextField txtArea;

		private Validator<V> validator;

		public TextFieldFilter(JTextField txtArea, Validator<V> validator)
		{
			this.txtArea = txtArea;
			this.validator = validator;
		}

		public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			if (!ignoreErrors) {
				Document doc = fb.getDocument();
				StringBuilder sb = new StringBuilder();
				sb.append(doc.getText(0, doc.getLength()));
				sb.insert(offset, string);
				super.insertString(fb, offset, string, attr);

				if (validator.validate(sb.toString())) {
					txtArea.setBackground(Color.WHITE);
				} else {
					txtArea.setBackground(Color.RED);
				}
			}
			else {
				super.insertString(fb, offset, string, attr);
			}
		}

		public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (!ignoreErrors) {
				Document doc = fb.getDocument();
				StringBuilder sb = new StringBuilder();
				sb.append(doc.getText(0, doc.getLength()));
				sb.replace(offset, offset + length, text);
				super.replace(fb, offset, length, text, attrs);

				if (validator.validate(sb.toString())) {
					txtArea.setBackground(Color.WHITE);
				} else {
					txtArea.setBackground(Color.RED);
				}
			} else {
				super.replace(fb, offset, length, text, attrs);
			}
		}

		public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
			if (!ignoreErrors) {
				Document doc = fb.getDocument();
				StringBuilder sb = new StringBuilder();
				sb.append(doc.getText(0, doc.getLength()));
				sb.delete(offset, offset + length);
				super.remove(fb, offset, length);

				if (validator.validate(sb.toString())) {
					txtArea.setBackground(Color.WHITE);
				} else {
					txtArea.setBackground(Color.RED);
				}
			}
			else {
				super.remove(fb, offset, length);
			}
		}
	}
}