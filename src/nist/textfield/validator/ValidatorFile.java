package nist.textfield.validator;

import java.io.File;
import java.io.IOException;

public class ValidatorFile
implements Validator<File>
{
	private String errorText;

	public ValidatorFile()
	{
		errorText = "<html>Please only a valid File Path.</html>";
	}

	@Override
	public boolean validate(String val)
	{
		File f = new File(val);
		try {
			f.getCanonicalPath();
		} catch (IOException ignored) {
			return false;
		}
		return true;
	}

	@Override
	public String getErrorText()
	{
		return errorText;
	}

	@Override
	public File getValue(String val)
	{
		return new File(val);
	}
}