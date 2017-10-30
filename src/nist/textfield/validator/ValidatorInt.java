package nist.textfield.validator;

public class ValidatorInt
implements Validator<Integer>
{
	private int min;

	private int max;

	private String errorText;

	public ValidatorInt()
	{
		min = Integer.MIN_VALUE;
		max = Integer.MAX_VALUE;
		errorText = "<html>Please only enter numbers in the text field.<br>Must be any integer.</html>";
	}

	public ValidatorInt(int min, int max)
	{
		this.min = min;
		this.max = max;
		errorText = ("<html>Please only enter integers in the text field.<br>Must be greater than or equal to " + min + " and less than or equal to " + max + "</html>");
	}

	@Override
	public boolean validate(String val)
	{
		try
		{
			int test = Integer.parseInt(val);

			if ((test < min) || (test > max)) {
				return false;
			}
			return true;
		} catch (NumberFormatException e) {}
		return false;
	}

	@Override
	public String getErrorText()
	{
		return errorText;
	}

	@Override
	public Integer getValue(String val)
	{
		if (val.equals(""))
			return Integer.valueOf(0);
		try {
			return Integer.valueOf(Integer.parseInt(val));
		} catch (NumberFormatException ex) {}
		return Integer.valueOf(min);
	}
}