package il.co.fibi.snifit.ant.extras.common;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceHandler {
	private static final String BUNDLE_NAME = "il.co.fibi.snifit.ant.extras.common.ApplicationBuild";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private ResourceHandler() {}
	
	public static String getString(String key) {
		try {
			if (key == null)
				throw new NullPointerException();
			String newKey = key;
			if (key.startsWith("%"))
				newKey = key.substring(1);
			return RESOURCE_BUNDLE.getString(newKey);
		} catch (MissingResourceException missingResourceException) {
			return key;
		}
	}

	public static String getString(String key1, String key2) {
		if (key1 == null || key2 == null)
			throw new NullPointerException();
		String newKey1 = key1;
		if (key1.startsWith("%"))
			newKey1 = key1.substring(1);
		String newKey2 = key2;
		if (key2.startsWith("%"))
			newKey2 = key2.substring(1);
		Object[] strArgs = { getString(newKey2) };
		try {
			return MessageFormat.format(getString(newKey1), strArgs);
		} catch (Exception exception) {
			return key1;
		}
	}

	public static String getString(String key1, Object[] strArgs) {
		if (key1 == null)
			throw new NullPointerException();
		String newKey = key1;
		if (key1.startsWith("%"))
			newKey = key1.substring(1);
		try {
			return MessageFormat.format(getString(newKey), strArgs);
		} catch (Exception exception) {
			return key1;
		}
	}
}
