package net.osmand;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class AndroidUtils {

	public static String formatDate(long time) {
		return java.text.DateFormat.getInstance().format(new Date(time));
	}
	
	public static String formatDateTime(long time) {
		Date d = new Date(time);
		return java.text.DateFormat.getInstance().format(d) +
				" " + java.text.DateFormat.getTimeInstance().format(d);
	}
	
	public static String formatTime(long time) {
		return java.text.DateFormat.getTimeInstance().format(new Date(time));
	}

	public static String getFileAsString(File file) {
		try {
			FileInputStream fin = new FileInputStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(line);
			}
			reader.close();
			fin.close();
			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * @param string1
	 *            input (or null)
	 * @param string2
	 *            input (or null)
	 * @return true, if equal (that means: same text or both null)
	 */
	public static boolean safeEquals(String string1, String string2) {
		return (string1 != null && string2 != null && string1.equals(string2))
				|| (string1 == null && string2 == null);
	}
}
