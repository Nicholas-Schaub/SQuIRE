package nist.textfield.validator;

import java.io.File;

public class ValidatorPrefix
implements Validator<String>
{
	private String prefix;
	private String errorText;

	public ValidatorPrefix()
	{
		prefix = "";
		errorText = "<html>Please only enter valid file path characters in the text field.</html>";
	}

	public boolean validate(String val)
	{
		if (prefix.contains(File.separator)) {
			return false;
		}
		return true;
	}

	public String getErrorText()
	{
		return errorText;
	}

	public String getValue(String val)
	{
		if (validate(val)) {
			return val;
		}
		return "";
	}
}