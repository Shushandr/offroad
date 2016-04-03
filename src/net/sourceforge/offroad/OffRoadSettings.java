package net.sourceforge.offroad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.osmand.plus.api.SettingsAPI;

final class OffRoadSettings implements SettingsAPI {
	
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
			properties.setProperty(pKey, pValue);
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
				mPref.load(new ByteArrayInputStream(out.toByteArray()));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	private OsmWindow mOsmWindow;
	private Properties mPreferences;

	public OffRoadSettings(OsmWindow ctx) {
		mOsmWindow = ctx;
		mPreferences = readDefaultPreferences();
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
		return getPreferences(pPref).getProperty(pKey, pDefValue);
	}

	private Properties getPreferences(Object pPref) {
		return mPreferences;
	}

	@Override
	public Object getPreferenceObject(String pKey) {
		return getPreferences(pKey);
	}

	@Override
	public long getLong(Object pPref, String pKey, long pDefValue) {
		try {
			return Long.parseLong(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			return pDefValue;
		}
	}

	@Override
	public int getInt(Object pPref, String pKey, int pDefValue) {
		try {
			return Integer.parseInt(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			return pDefValue;
		}
	}

	@Override
	public float getFloat(Object pPref, String pKey, float pDefValue) {
		try {
			return Float.parseFloat(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			return pDefValue;
		}
	}

	@Override
	public boolean getBoolean(Object pPref, String pKey, boolean pDefValue) {
		try {
			return Boolean.parseBoolean(getString(pPref, pKey, ""+pDefValue));
		} catch (NumberFormatException nfe) {
			return pDefValue;
		}
	}

	@Override
	public SettingsEditor edit(Object pPref) {
		Properties copy  = new Properties();
		if (pPref instanceof Properties) {
			Properties props = (Properties) pPref;
			copy = new Properties(props);
		}
		return new OffRoadSettingsEditor(copy, getPreferences(pPref));
	}

	@Override
	public boolean contains(Object pPref, String pKey) {
		return getPreferences(pPref).containsKey(pKey);
	}

	public void save() {
		try {
			FileOutputStream stream = new FileOutputStream(mOsmWindow.getAppPath(OFFROAD_PROPERTIES));
			mPreferences.store(stream, "");
			stream.close();
			System.out.println("Settings saved.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}