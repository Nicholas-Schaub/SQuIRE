package nist.ij.log;

import ij.IJ;
import java.util.HashMap;

public class Log
{
	public static enum LogType
	{
		NONE("None"), 

		MANDATORY("Mandatory"), 

		DEBUG("Debug"), 

		VERBOSE("Verbose");

		private LogType(String text) {
			this.text = text;
		}

		private final String text;
		private static HashMap<String, LogType> logMap;
		public String toString()
		{
			return text;
		}


		static
		{
			logMap = new HashMap();
			for (LogType t : values()) {
				logMap.put(t.toString(), t);
			}
		}

		public static String[] enumValsToStringArray() {
			LogType[] values = values();

			String[] ret = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				ret[(i++)] = values[i].toString();
			}
			return ret;
		}

		public static LogType getLogType(String name) {
			return (LogType)logMap.get(name);
		}
	}

	private static long startTime = 0L;
	private static LogType logLevel = LogType.MANDATORY;
	private static boolean timeEnabled = false;

	public static void enableTiming()
	{
		timeEnabled = true;
	}

	public static void disableTiming()
	{
		timeEnabled = false;
	}

	public static void setLogLevel(String level)
	{
		LogType type = LogType.getLogType(level);
		if (type == null) {
			type = LogType.NONE;
		}
		setLogLevel(type);
	}

	public static void setLogLevel(LogType level)
	{
		logLevel = level;
		if (level != LogType.NONE) {
			msg("Log Level set to: " + logLevel);
		}
	}

	public static void error(Throwable e) {
		StackTraceElement[] st = e.getStackTrace();

		error("Exception in " + st[0].getClassName() + "." + st[0].getMethodName() + ": " + e.getMessage());
		error("********************** Stack Trace: **********************");
		for (int i = 0; i < st.length; i++) {
			error(st[i].toString());
		}
		error("**********************************************************");
	}

	public static void error(String message) {
		msg(message);
	}

	public static void mandatory(String message)
	{
		if (logLevel.ordinal() == LogType.VERBOSE.ordinal()) {
			verbose(message);
		} else if (logLevel.ordinal() >= LogType.MANDATORY.ordinal()) {
			msg(message);
		}
	}

	public static void debug(String message)
	{
		if (logLevel.ordinal() == LogType.VERBOSE.ordinal()) {
			verbose(message);
		} else if (logLevel.ordinal() >= LogType.DEBUG.ordinal()) {
			msg(message);
		}
	}

	public static void verbose(String message)
	{
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		String fullClassName = stackTrace[3].getClassName();
		String methodName = stackTrace[3].getMethodName();
		int lineNumber = stackTrace[3].getLineNumber();
		message = fullClassName + ":" + methodName + ":" + lineNumber + " - " + message;

		msg(message);
	}

	private static void msg(String message)
	{
		if (timeEnabled) {
			if (startTime == 0L) {
				startTime = System.currentTimeMillis();
			}

			long elapsed = System.currentTimeMillis() - startTime;


			IJ.log(elapsed + "ms: " + message);
		} else {
			IJ.log(message);
		}
	}
}