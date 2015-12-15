package nist.textfield.validator;

public class ValidatorDbl
implements Validator<Double>
{
	private double min;

	private double max;

	private String errorText;

	public ValidatorDbl()
	{
		min = Double.NEGATIVE_INFINITY;
		max = Double.POSITIVE_INFINITY;
		errorText = "<html>Please only enter numbers in the text field.<br>Must be any double.</html>";
	}

	public ValidatorDbl(double min, double max)
	{
		this.min = min;
		this.max = max;
		errorText = ("<html>Please only enter numbers in the text field.<br>Must be greater than or equal to " + min + " and less than or equal to " + max + "</html>");
	}

	public boolean validate(String val)
	{
		try
		{
			double test = Double.parseDouble(val);

			if ((test < min) || (test > max)) {
				return false;
			}
			return true;
		} catch (NumberFormatException e) {}
		return false;
	}

	public String getErrorText()
	{
		return errorText;
	}

	public Double getValue(String val)
	{
		if (val.equals(""))
			return Double.valueOf(0.0D);
		try {
			return Double.valueOf(Double.parseDouble(val));
		} catch (NumberFormatException ex) {}
		return Double.valueOf(min);
	}
}

