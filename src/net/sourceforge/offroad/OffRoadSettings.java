package net.sourceforge.offroad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import net.osmand.plus.api.SettingsAPI;

final class OffRoadSettings implements SettingsAPI {
	
	private static final String OTHER_PROPERTIES_POSTFIX = ".priv.properties";
	private static final String OFFROAD_PROPERTIES = "offroad.properties";

	private static final class OffRoadSettingsEditor implements SettingsEditor {
		private Properties properties;
		private Properties mPref;

		public OffRoadSettingsEditor(Properties pCopy, Properties pPref) {
			properties = pCopy;
			mPref = pPref;
		}

		@Override
		public SettingsEditor putString(String pKey, String pValue) {
			if (pValue != null) {
				properties.setProperty(pKey, pValue);
			} else {
				properties.remove(pKey);
			}
			return this;
		}

		@Override
		public SettingsEditor putBoolean(String pKey, boolean pValue) {
			properties.setProperty(pKey, ""+pValue);
			return this;
		}

		@Override
		public SettingsEditor putFloat(String pKey, float pValue) {
			properties.setProperty(pKey, ""+pValue);
			return this;
		}

		@Override
		public SettingsEditor putInt(String pKey, int pValue) {
			properties.setProperty(pKey, ""+pValue);
			return this;
		}

		@Override
		public SettingsEditor putLong(String pKey, long pValue) {
			properties.setProperty(pKey, ""+pValue);
			return this;
		}

		@Override
		public SettingsEditor remove(String pKey) {
			properties.remove(pKey);
			return this;
		}

		@Override
		public boolean commit() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				properties.store(out , "");
				mPref.clear();
				mPref.load(new ByteArrayInputStream(out.toByteArray()));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	private OsmWindow mOsmWindow;
	private HashMap<String, Properties> mPreferencesHash = new HashMap<>();
	private static final String SHARED_PREFERENCES_NAME = "net.osmand.settings"; //$NON-NLS-1$

	public OffRoadSettings(OsmWindow ctx) {
		mOsmWindow = ctx;
		mPreferencesHash.put(SHARED_PREFERENCES_NAME, readDefaultPreferences());
		load();
	}

	public void load() {
		Path dir = Paths.get(mOsmWindow.getAppPath("").getAbsolutePath());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*"+OTHER_PROPERTIES_POSTFIX)) {
			for (Path entry : stream) {
				Properties props = new Properties();
				try {
					InputStream in = new FileInputStream(entry.toFile());
					props.load(in);
					in.close();
				} catch (Exception ex) {
					ex.printStackTrace();
					System.err.println("Panic! Error while loading offroad properties.");
				}
				String key = entry.toFile().getName().replaceFirst(OTHER_PROPERTIES_POSTFIX, "");
				System.out.println("Read file " + entry + " and put as " + key);
				mPreferencesHash.put(key, props);
			}
		} catch (IOException x) {
			// IOException can never be thrown by the iteration.
			// In this snippet, it can // only be thrown by newDirectoryStream.
			System.err.println(x);
		}
	}
	
	public Properties readDefaultPreferences() {
		Properties props = new Properties();
		try {
			InputStream in = new FileInputStream(mOsmWindow.getAppPath(OFFROAD_PROPERTIES));
			props.load(in);
			in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.println("Panic! Error while loading offroad properties.");
		}
		return props;
	}

	@Override
	public String getString(Object pPref, String pKey, String pDefValue) {
		String res = getPreferences(pPref).getProperty(pKey, pDefValue);
//		System.out.println("PREFS: " + pKey + "="+res);
		return res;
	}

	private Properties getPreferences(Object pPref) {
		return (Properties) pPref;
	}

	@Override
	public Object getPreferenceObject(String pKey) {
		if(!mPreferencesHash.containsKey(pKey)){
			System.out.println("Create prefs for " +pKey);
			mPreferencesHash.put(pKey, new Properties());
		}
		return mPreferencesHash.get(pKey);
	}

	@Override
	public long getLong(Object pPref, String pKey, long pDefValue) {
		try {
			return Long.parseLong(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return pDefValue;
		}
	}

	@Override
	public int getInt(Object pPref, String pKey, int pDefValue) {
		try {
			return Integer.parseInt(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return pDefValue;
		}
	}

	@Override
	public float getFloat(Object pPref, String pKey, float pDefValue) {
		try {
			return Float.parseFloat(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return pDefValue;
		}
	}

	@Override
	public boolean getBoolean(Object pPref, String pKey, boolean pDefValue) {
		try {
			return Boolean.parseBoolean(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			return pDefValue;
		}
	}

	@Override
	public SettingsEditor edit(Object pPref) {
		Properties copy  = new Properties();
		if (pPref instanceof Properties) {
			Properties props = (Properties) pPref;
			copyProperties(props, copy);
		}
		return new OffRoadSettingsEditor(copy, getPreferences(pPref));
	}
	
	public static void copyProperties(Properties src_prop, Properties dest_prop)
	  {
	      for (Enumeration<?> propertyNames = src_prop.propertyNames();
	           propertyNames.hasMoreElements(); )
	      {
	          Object key = propertyNames.nextElement();
	          dest_prop.put(key, src_prop.get(key));
	      }
	  }
	@Override
	public boolean contains(Object pPref, String pKey) {
		return getPreferences(pPref).containsKey(pKey);
	}

	public void save() {
		try {
			FileOutputStream stream = new FileOutputStream(mOsmWindow.getAppPath(OFFROAD_PROPERTIES));
			mPreferencesHash.get(SHARED_PREFERENCES_NAME).store(stream, "");
			stream.close();
			for (String key : mPreferencesHash.keySet()) {
				if(key.equals(SHARED_PREFERENCES_NAME)){
					continue;
				}
				FileOutputStream streamL = new FileOutputStream(mOsmWindow.getAppPath(key+OTHER_PROPERTIES_POSTFIX));
				mPreferencesHash.get(key).store(streamL, "");
				streamL.close();
			}
			System.out.println("Settings saved.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}