package net.osmand.plus;


import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.access.RelativeDirectionStyle;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.api.SettingsAPI.SettingsEditor;

public class OsmandSettings {

	public interface OsmandPreference<T> {
		T get();

		boolean set(T obj);

		boolean setModeValue(ApplicationMode m, T obj);

		T getModeValue(ApplicationMode m);

		String getId();

		void resetToDefault();

		void overrideDefaultValue(T newDefaultValue);

		void addListener(StateChangedListener<T> listener);

		void removeListener(StateChangedListener<T> listener);

		boolean isSet();
	}

	private abstract class PreferenceWithListener<T> implements OsmandPreference<T> {
		private List<WeakReference<StateChangedListener<T>>> l = null;

		@Override
		public void addListener(StateChangedListener<T> listener) {
			if (l == null) {
				l = new LinkedList<WeakReference<StateChangedListener<T>>>();
			}
			if (!l.contains(new WeakReference<StateChangedListener<T>>(listener))) {
				l.add(new WeakReference<StateChangedListener<T>>(listener));
			}
		}

		public void fireEvent(T value) {
			if (l != null) {
				Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
				while (it.hasNext()) {
					StateChangedListener<T> t = it.next().get();
					if (t == null) {
						it.remove();
					} else {
						t.stateChanged(value);
					}
				}
			}
		}

		@Override
		public void removeListener(StateChangedListener<T> listener) {
			if (l != null) {
				Iterator<WeakReference<StateChangedListener<T>>> it = l.iterator();
				while (it.hasNext()) {
					StateChangedListener<T> t = it.next().get();
					if (t == listener) {
						it.remove();
					}
				}
			}
		}
	}

	// These settings are stored in SharedPreferences
	private static final String SHARED_PREFERENCES_NAME = "net.osmand.settings"; //$NON-NLS-1$


	/// Settings variables
	private SettingsAPI settingsAPI;
	private Object globalPreferences;
	private Object defaultProfilePreferences;
	private Object profilePreferences;
	private ApplicationMode currentMode;
	private Map<String, OsmandPreference<?>> registeredPreferences =
			new LinkedHashMap<String, OsmandSettings.OsmandPreference<?>>();

	// cache variables
	private long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;


	public OsmandSettings(SettingsAPI settinsAPI) {
		this.settingsAPI = settinsAPI;
		initPrefs();
	}

	private void initPrefs() {
		globalPreferences = settingsAPI.getPreferenceObject(SHARED_PREFERENCES_NAME);
		defaultProfilePreferences = getProfilePreferences(ApplicationMode.DEFAULT);
		currentMode = readApplicationMode();
		profilePreferences = getProfilePreferences(currentMode);
	}

	public void setSettingsAPI(SettingsAPI settingsAPI) {
		this.settingsAPI = settingsAPI;
		initPrefs();
	}

	public SettingsAPI getSettingsAPI() {
		return settingsAPI;
	}

	public static String getSharedPreferencesName(ApplicationMode mode) {
		if (mode == null) {
			return SHARED_PREFERENCES_NAME;
		} else {
			return SHARED_PREFERENCES_NAME + "." + mode.getStringKey().toLowerCase();
		}
	}

	public Object getProfilePreferences(ApplicationMode mode) {
		return settingsAPI.getPreferenceObject(getSharedPreferencesName(mode));
	}

	public ApplicationMode LAST_ROUTING_APPLICATION_MODE = null;

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<ApplicationMode> APPLICATION_MODE = new PreferenceWithListener<ApplicationMode>() {
		@Override
		public String getId() {
			return "application_mode";
		}

		;

		@Override
		public ApplicationMode get() {
			return currentMode;
		}

		@Override
		public void overrideDefaultValue(ApplicationMode newDefaultValue) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void resetToDefault() {
			set(ApplicationMode.DEFAULT);
		}

		;

		@Override
		public boolean isSet() {
			return true;
		}

		;

		@Override
		public boolean set(ApplicationMode val) {
			ApplicationMode oldMode = currentMode;
			boolean changed = settingsAPI.edit(globalPreferences).putString(getId(), val.getStringKey()).commit();
			if (changed) {
				currentMode = val;
				profilePreferences = getProfilePreferences(currentMode);
				fireEvent(oldMode);
			}
			return changed;
		}

		@Override
		public ApplicationMode getModeValue(ApplicationMode m) {
			return m;
		}

		@Override
		public boolean setModeValue(ApplicationMode m, ApplicationMode obj) {
			throw new UnsupportedOperationException();
		}
	};

	public ApplicationMode getApplicationMode() {
		return APPLICATION_MODE.get();
	}

	protected ApplicationMode readApplicationMode() {
		String s = settingsAPI.getString(globalPreferences, APPLICATION_MODE.getId(), ApplicationMode.DEFAULT.getStringKey());
		return ApplicationMode.valueOfStringKey(s, ApplicationMode.DEFAULT);
	}


	// Check internet connection available every 15 seconds
	public boolean isInternetConnectionAvailable() {
		return isInternetConnectionAvailable(false);
	}

	public boolean isInternetConnectionAvailable(boolean update) {
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if (delta < 0 || delta > 15000 || update) {
			internetConnectionAvailable = isInternetConnected();
		}
		return internetConnectionAvailable;
	}

	public boolean isWifiConnected() {
		return true;
	}

	private boolean isInternetConnected() {
		return true;
	}


	/////////////// PREFERENCES classes ////////////////

	public abstract class CommonPreference<T> extends PreferenceWithListener<T> {
		private final String id;
		private boolean global;
		private T cachedValue;
		private Object cachedPreference;
		private boolean cache;
		private Map<ApplicationMode, T> defaultValues;
		private T defaultValue;


		public CommonPreference(String id, T defaultValue) {
			this.id = id;
			this.defaultValue = defaultValue;
		}

		public CommonPreference<T> makeGlobal() {
			global = true;
			return this;
		}

		public CommonPreference<T> cache() {
			cache = true;
			return this;
		}

		public CommonPreference<T> makeProfile() {
			global = false;
			return this;
		}

		protected Object getPreferences() {
			return global ? globalPreferences : profilePreferences;
		}

		public void setModeDefaultValue(ApplicationMode mode, T defValue) {
			if (defaultValues == null) {
				defaultValues = new LinkedHashMap<ApplicationMode, T>();
			}
			defaultValues.put(mode, defValue);
		}

		@Override
		public boolean setModeValue(ApplicationMode mode, T obj) {
			if (global) {
				return set(obj);
			}
			boolean ch = setValue(getProfilePreferences(mode), obj);
			fireEvent(obj);
			return ch;
		}

		public T getProfileDefaultValue(ApplicationMode mode) {
			if (global) {
				return defaultValue;
			}
			if (defaultValues != null && defaultValues.containsKey(mode)) {
				return defaultValues.get(mode);
			}
			ApplicationMode pt = mode.getParent();
			if (pt != null) {
				return getProfileDefaultValue(pt);
			}
			if (settingsAPI.contains(defaultProfilePreferences, getId())) {
				return getValue(defaultProfilePreferences, defaultValue);
			} else {
				return defaultValue;
			}
		}

		protected T getDefaultValue() {
			return getProfileDefaultValue(currentMode);
		}

		@Override
		public void overrideDefaultValue(T newDefaultValue) {
			this.defaultValue = newDefaultValue;
		}

		protected abstract T getValue(Object prefs, T defaultValue);


		protected abstract boolean setValue(Object prefs, T val);

		@Override
		public T getModeValue(ApplicationMode mode) {
			if (global) {
				return get();
			}
			T defaultV = getProfileDefaultValue(mode);
			return getValue(getProfilePreferences(mode), defaultV);
		}

		@Override
		public T get() {
			if (cache && cachedValue != null && cachedPreference == getPreferences()) {
				return cachedValue;
			}
			cachedPreference = getPreferences();
			cachedValue = getValue(cachedPreference, getProfileDefaultValue(currentMode));
			return cachedValue;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public void resetToDefault() {
			T o = getProfileDefaultValue(currentMode);
			set(o);
		}

		@Override
		public boolean set(T obj) {
			Object prefs = getPreferences();
			if (setValue(prefs, obj)) {
				cachedValue = obj;
				cachedPreference = prefs;
				fireEvent(obj);
				return true;
			}
			return false;
		}

		public boolean isSet() {
			return settingsAPI.contains(getPreferences(), getId());
		}

	}

	private class BooleanPreference extends CommonPreference<Boolean> {


		private BooleanPreference(String id, boolean defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Boolean getValue(Object prefs, Boolean defaultValue) {
			return settingsAPI.getBoolean(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Boolean val) {
			return settingsAPI.edit(prefs).putBoolean(getId(), val).commit();
		}
	}

	private class BooleanAccessibilityPreference extends BooleanPreference {

		private BooleanAccessibilityPreference(String id, boolean defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Boolean getValue(Object prefs, Boolean defaultValue) {
			return true ?
					super.getValue(prefs, defaultValue) :
					defaultValue;
		}

		@Override
		protected boolean setValue(Object prefs, Boolean val) {
			return true ?
					super.setValue(prefs, val) :
					false;
		}
	}

	private class IntPreference extends CommonPreference<Integer> {


		private IntPreference(String id, int defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Integer getValue(Object prefs, Integer defaultValue) {
			return settingsAPI.getInt(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Integer val) {
			return settingsAPI.edit(prefs).putInt(getId(), val).commit();
		}

	}

	private class LongPreference extends CommonPreference<Long> {


		private LongPreference(String id, long defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Long getValue(Object prefs, Long defaultValue) {
			return settingsAPI.getLong(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Long val) {
			return settingsAPI.edit(prefs).putLong(getId(), val).commit();
		}

	}

	private class FloatPreference extends CommonPreference<Float> {


		private FloatPreference(String id, float defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected Float getValue(Object prefs, Float defaultValue) {
			return settingsAPI.getFloat(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, Float val) {
			return settingsAPI.edit(prefs).putFloat(getId(), val).commit();
		}

	}

	private class StringPreference extends CommonPreference<String> {

		private StringPreference(String id, String defaultValue) {
			super(id, defaultValue);
		}

		@Override
		protected String getValue(Object prefs, String defaultValue) {
			return settingsAPI.getString(prefs, getId(), defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, String val) {
			return settingsAPI.edit(prefs).putString(getId(), (val != null) ? val.trim() : val).commit();
		}

	}

	private class EnumIntPreference<E extends Enum<E>> extends CommonPreference<E> {

		private final E[] values;

		private EnumIntPreference(String id, E defaultValue, E[] values) {
			super(id, defaultValue);
			this.values = values;
		}


		@Override
		protected E getValue(Object prefs, E defaultValue) {
			try {
				int i = settingsAPI.getInt(prefs, getId(), -1);
				if (i >= 0 && i < values.length) {
					return values[i];
				}
			} catch (ClassCastException ex) {
				setValue(prefs, defaultValue);
			}
			return defaultValue;
		}

		@Override
		protected boolean setValue(Object prefs, E val) {
			return settingsAPI.edit(prefs).putInt(getId(), val.ordinal()).commit();
		}

	}
	///////////// PREFERENCES classes ////////////////

	// this value string is synchronized with settings_pref.xml preference name
	private final OsmandPreference<String> PLUGINS = new StringPreference("enabled_plugins", "").makeGlobal();

	public Set<String> getEnabledPlugins() {
		String plugs = PLUGINS.get();
		StringTokenizer toks = new StringTokenizer(plugs, ",");
		Set<String> res = new LinkedHashSet<String>();
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken();
			if (!tok.startsWith("-")) {
				res.add(tok);
			}
		}
		return res;
	}

	public Set<String> getPlugins() {
		String plugs = PLUGINS.get();
		StringTokenizer toks = new StringTokenizer(plugs, ",");
		Set<String> res = new LinkedHashSet<String>();
		while (toks.hasMoreTokens()) {
			res.add(toks.nextToken());
		}
		return res;
	}

	public void enablePlugin(String pluginId, boolean enable) {
		Set<String> set = getPlugins();
		if (enable) {
			set.remove("-" + pluginId);
			set.add(pluginId);
		} else {
			set.remove(pluginId);
			set.add("-" + pluginId);
		}
		StringBuilder serialization = new StringBuilder();
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			serialization.append(it.next());
			if (it.hasNext()) {
				serialization.append(",");
			}
		}
		if (!serialization.toString().equals(PLUGINS.get())) {
			PLUGINS.set(serialization.toString());
		}
	}


	@SuppressWarnings("unchecked")
	public CommonPreference<Boolean> registerBooleanPreference(String id, boolean defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Boolean>) registeredPreferences.get(id);
		}
		BooleanPreference p = new BooleanPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<String> registerStringPreference(String id, String defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<String>) registeredPreferences.get(id);
		}
		StringPreference p = new StringPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Integer> registerIntPreference(String id, int defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Integer>) registeredPreferences.get(id);
		}
		IntPreference p = new IntPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Long> registerLongPreference(String id, long defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Long>) registeredPreferences.get(id);
		}
		LongPreference p = new LongPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	@SuppressWarnings("unchecked")
	public CommonPreference<Float> registerFloatPreference(String id, float defValue) {
		if (registeredPreferences.containsKey(id)) {
			return (CommonPreference<Float>) registeredPreferences.get(id);
		}
		FloatPreference p = new FloatPreference(id, defValue);
		registeredPreferences.put(id, p);
		return p;
	}

	public final CommonPreference<Boolean> USE_FAST_RECALCULATION = new BooleanPreference("use_fast_recalculation", true).makeGlobal().cache();

	public final CommonPreference<Boolean> SHOW_CARD_TO_CHOOSE_DRAWER = new BooleanPreference("show_card_to_choose_drawer", false).makeGlobal();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_START = new BooleanPreference("should_show_dashboard_on_start", false).makeGlobal();
	public final CommonPreference<Boolean> SHOW_DASHBOARD_ON_MAP_SCREEN = new BooleanPreference("show_dashboard_on_map_screen", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> USE_INTERNET_TO_DOWNLOAD_TILES = new BooleanPreference("use_internet_to_download_tiles", true).makeGlobal().cache();

	public final OsmandPreference<String> AVAILABLE_APP_MODES = new StringPreference("available_application_modes", "car,bicycle,pedestrian,").makeGlobal().cache();

	public final OsmandPreference<String> LAST_FAV_CATEGORY_ENTERED = new StringPreference("last_fav_category", "").makeGlobal();


	public final OsmandPreference<ApplicationMode> DEFAULT_APPLICATION_MODE = new CommonPreference<ApplicationMode>("default_application_mode_string", ApplicationMode.DEFAULT) {
		{
			makeGlobal();
		}

		@Override
		protected ApplicationMode getValue(Object prefs, ApplicationMode defaultValue) {
			String key = settingsAPI.getString(prefs, getId(), defaultValue.getStringKey());
			return ApplicationMode.valueOfStringKey(key, defaultValue);
		}

		@Override
		protected boolean setValue(Object prefs, ApplicationMode val) {
			return settingsAPI.edit(prefs).putString(getId(), val.getStringKey()).commit();
		}
	};

	public final OsmandPreference<Boolean> FIRST_MAP_IS_DOWNLOADED = new BooleanPreference(
			"first_map_is_downloaded", false);

	public final OsmandPreference<DrivingRegion> DRIVING_REGION = new EnumIntPreference<DrivingRegion>(
			"default_driving_region", DrivingRegion.EUROPE_ASIA, DrivingRegion.values()) {
		protected boolean setValue(Object prefs, DrivingRegion val) {
			if (val != null) {
				METRIC_SYSTEM.set(val.defMetrics);
			}
			return super.setValue(prefs, val);
		}

		;

		protected DrivingRegion getDefaultValue() {
			Locale df = Locale.getDefault();
			if (df == null) {
				return DrivingRegion.EUROPE_ASIA;
			}
			if (df.getCountry().equalsIgnoreCase(Locale.US.getCountry())) {
				return DrivingRegion.US;
			} else if (df.getCountry().equalsIgnoreCase(Locale.CANADA.getCountry())) {
				return DrivingRegion.CANADA;
			} else if (df.getCountry().equalsIgnoreCase(Locale.JAPAN.getCountry())) {
				return DrivingRegion.JAPAN;
				// potentially wrong in europe
//			} else if(df.getCountry().equalsIgnoreCase(Locale.UK.getCountry())) {
//				return DrivingRegion.UK_AND_OTHERS;
			}
			return DrivingRegion.EUROPE_ASIA;
		}

		;
	}.makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<MetricsConstants> METRIC_SYSTEM = new EnumIntPreference<MetricsConstants>(
			"default_metric_system", MetricsConstants.KILOMETERS_AND_METERS, MetricsConstants.values()) {
		protected MetricsConstants getDefaultValue() {
			return DRIVING_REGION.get().defMetrics;
		}

		;
	}.makeGlobal().cache();


	public final OsmandPreference<SpeedConstants> SPEED_SYSTEM = new EnumIntPreference<SpeedConstants>(
			"default_speed_system", SpeedConstants.KILOMETERS_PER_HOUR, SpeedConstants.values()) {

		@Override
		public SpeedConstants getProfileDefaultValue(ApplicationMode mode) {
			MetricsConstants mc = METRIC_SYSTEM.get();
			if (mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
				if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
					return SpeedConstants.MINUTES_PER_KILOMETER;
				} else {
					return SpeedConstants.MILES_PER_HOUR;
				}
			}
			if (mode.isDerivedRoutingFrom(ApplicationMode.BOAT)) {
				return SpeedConstants.NAUTICALMILES_PER_HOUR;
			}
			if (mc == MetricsConstants.NAUTICAL_MILES) {
				return SpeedConstants.NAUTICALMILES_PER_HOUR;
			} else if (mc == MetricsConstants.KILOMETERS_AND_METERS) {
				return SpeedConstants.KILOMETERS_PER_HOUR;
			} else {
				return SpeedConstants.MILES_PER_HOUR;
			}
		}

		;

	}.makeProfile().cache();


	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<RelativeDirectionStyle> DIRECTION_STYLE = new EnumIntPreference<RelativeDirectionStyle>(
			"direction_style", RelativeDirectionStyle.SIDEWISE, RelativeDirectionStyle.values()).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<AccessibilityMode> ACCESSIBILITY_MODE = new EnumIntPreference<AccessibilityMode>(
			"accessibility_mode", AccessibilityMode.DEFAULT, AccessibilityMode.values()).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Float> SPEECH_RATE =
			new FloatPreference("speech_rate", 1f).makeGlobal();

	public final OsmandPreference<Float> ARRIVAL_DISTANCE_FACTOR =
			new FloatPreference("arrival_distance_factor", 1f).makeProfile();

	public final OsmandPreference<Float> SPEED_LIMIT_EXCEED =
			new FloatPreference("speed_limit_exceed", 5f).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_TRACKBALL_FOR_MOVEMENTS =
			new BooleanPreference("use_trackball_for_movements", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> ZOOM_BY_TRACKBALL =
			new BooleanAccessibilityPreference("zoom_by_trackball", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SCROLL_MAP_BY_GESTURES =
			new BooleanAccessibilityPreference("scroll_map_by_gestures", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_SHORT_OBJECT_NAMES =
			new BooleanAccessibilityPreference("use_short_object_names", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> ACCESSIBILITY_EXTENSIONS =
			new BooleanAccessibilityPreference("accessibility_extensions", false).makeGlobal();


	// magnetic field doesn'torkmost of the time on some phones
	public final OsmandPreference<Boolean> USE_MAGNETIC_FIELD_SENSOR_COMPASS = new BooleanPreference("use_magnetic_field_sensor_compass", false).makeGlobal().cache();
	public final OsmandPreference<Boolean> USE_KALMAN_FILTER_FOR_COMPASS = new BooleanPreference("use_kalman_filter_compass", true).makeGlobal().cache();


	public final CommonPreference<Float> TEXT_SCALE = new FloatPreference("text_scale", 1f).makeProfile().cache();

	{
		TEXT_SCALE.setModeDefaultValue(ApplicationMode.CAR, 1.25f);
	}

	public final CommonPreference<Float> MAP_DENSITY = new FloatPreference("map_density_n", 1f).makeProfile().cache();

	{
		MAP_DENSITY.setModeDefaultValue(ApplicationMode.CAR, 1.5f);
	}


	public final OsmandPreference<Boolean> SHOW_POI_LABEL = new BooleanPreference("show_poi_label", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> PREFERRED_LOCALE = new StringPreference("preferred_locale", "").makeGlobal();

	public static final String TRANSPORT_STOPS_OVER_MAP = "transportStops";

	public final OsmandPreference<String> MAP_PREFERRED_LOCALE = new StringPreference("map_preferred_locale", "").makeGlobal().cache();

	public boolean usingEnglishNames() {
		return MAP_PREFERRED_LOCALE.get().equals("en");
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_NAME = new StringPreference("user_name", "").makeGlobal();

	public final OsmandPreference<String> BILLING_USER_ID = new StringPreference("billing_user_id", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_NAME = new StringPreference("billing_user_name", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_EMAIL = new StringPreference("billing_user_email", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY = new StringPreference("billing_user_country", "").makeGlobal();
	public final OsmandPreference<String> BILLING_USER_COUNTRY_DOWNLOAD_NAME = new StringPreference("billing_user_country_download_name", "").makeGlobal();
	public final OsmandPreference<Boolean> BILLING_HIDE_USER_NAME = new BooleanPreference("billing_hide_user_name", false).makeGlobal();
	public final OsmandPreference<Boolean> BILLING_PURCHASE_TOKEN_SENT = new BooleanPreference("billing_purchase_token_sent", false).makeGlobal();
	public final OsmandPreference<Boolean> LIVE_UPDATES_PURCHASED = new BooleanPreference("billing_live_updates_purchased", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_OSM_BUG_NAME =
			new StringPreference("user_osm_bug_name", "NoName/OsmAnd").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_PASSWORD =
			new StringPreference("user_password", "").makeGlobal();

	// this value boolean is synchronized with settings_pref.xml preference offline POI/Bugs edition
	public final OsmandPreference<Boolean> OFFLINE_EDITION = new BooleanPreference("offline_osm_editing", true).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<DayNightMode> DAYNIGHT_MODE =
			new EnumIntPreference<DayNightMode>("daynight_mode", DayNightMode.DAY, DayNightMode.values());

	{
		DAYNIGHT_MODE.makeProfile().cache();
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.CAR, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, DayNightMode.AUTO);
		DAYNIGHT_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, DayNightMode.DAY);
	}

//	// this value string is synchronized with settings_pref.xml preference name
//	public final OsmandPreference<RouteService> ROUTER_SERVICE =
//			new EnumIntPreference<RouteService>("router_service", RouteService.OSMAND,
//					RouteService.values()).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<AutoZoomMap> AUTO_ZOOM_MAP =
			new EnumIntPreference<AutoZoomMap>("auto_zoom_map_new", AutoZoomMap.NONE,
					AutoZoomMap.values()).makeProfile().cache();

	{
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.CAR, AutoZoomMap.FAR);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, AutoZoomMap.NONE);
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, AutoZoomMap.NONE);
	}

	public final CommonPreference<Integer> DELAY_TO_START_NAVIGATION = new IntPreference("delay_to_start_navigation", -1) {

		protected Integer getDefaultValue() {
			if (DEFAULT_APPLICATION_MODE.get().isDerivedRoutingFrom(ApplicationMode.CAR)) {
				return 10;
			}
			return -1;
		}

		;
	}.makeGlobal().cache();

	public final CommonPreference<Boolean> SNAP_TO_ROAD = new BooleanPreference("snap_to_road", false).makeProfile().cache();

	{
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.CAR, true);
		SNAP_TO_ROAD.setModeDefaultValue(ApplicationMode.BICYCLE, true);
	}

	public final CommonPreference<Boolean> INTERRUPT_MUSIC = new BooleanPreference("interrupt_music", false).makeGlobal();

	public final CommonPreference<String> PROXY_HOST = new StringPreference("proxy_host", "127.0.0.1").makeGlobal();
	public final CommonPreference<Integer> PROXY_PORT = new IntPreference("proxy_port", 8118).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$

	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_TO_GPX = new BooleanPreference("save_global_track_to_gpx", false).makeGlobal().cache();
	public final CommonPreference<Integer> SAVE_GLOBAL_TRACK_INTERVAL = new IntPreference("save_global_track_interval", 5000).makeGlobal().cache();
	public final CommonPreference<Boolean> SAVE_GLOBAL_TRACK_REMEMBER = new BooleanPreference("save_global_track_remember", false).makeGlobal().cache();
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SAVE_TRACK_TO_GPX = new BooleanPreference("save_track_to_gpx", false).makeProfile().cache();

	{
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, false);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public final CommonPreference<Boolean> DISABLE_RECORDING_ONCE_APP_KILLED = new BooleanPreference("disable_recording_once_app_killed", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> FAST_ROUTE_MODE = new BooleanPreference("fast_route_mode", true).makeProfile();
	// temporarily for new version
	public final CommonPreference<Boolean> DISABLE_COMPLEX_ROUTING = new BooleanPreference("disable_complex_routing", false).makeGlobal();

	public final CommonPreference<Boolean> SHOW_TRAFFIC_WARNINGS = new BooleanPreference("show_traffic_warnings", false).makeProfile().cache();

	{
		SHOW_TRAFFIC_WARNINGS.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final CommonPreference<Boolean> SHOW_PEDESTRIAN = new BooleanPreference("show_pedestrian", false).makeProfile().cache();

	{
		SHOW_PEDESTRIAN.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	public final OsmandPreference<Boolean> SHOW_CAMERAS = new BooleanPreference("show_cameras", false).makeProfile().cache();
	public final CommonPreference<Boolean> SHOW_LANES = new BooleanPreference("show_lanes", false).makeProfile().cache();

	{
		SHOW_LANES.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_LANES.setModeDefaultValue(ApplicationMode.BICYCLE, true);
	}

	public final OsmandPreference<Boolean> SHOW_WPT = new BooleanPreference("show_gpx_wpt", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_FAVORITES = new BooleanPreference("show_nearby_favorites", false).makeGlobal().cache();
	public final OsmandPreference<Boolean> SHOW_NEARBY_POI = new BooleanPreference("show_nearby_poi", false).makeGlobal().cache();

	public final OsmandPreference<Boolean> SPEAK_STREET_NAMES = new BooleanPreference("speak_street_names", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_TRAFFIC_WARNINGS = new BooleanPreference("speak_traffic_warnings", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_PEDESTRIAN = new BooleanPreference("speak_pedestrian", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_SPEED_LIMIT = new BooleanPreference("speak_speed_limit", true).makeProfile().cache();
	public final OsmandPreference<Boolean> SPEAK_SPEED_CAMERA = new BooleanPreference("speak_cameras", false).makeProfile().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_WPT = new BooleanPreference("announce_wpt", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_FAVORITES = new BooleanPreference("announce_nearby_favorites", false).makeProfile().cache();
	public final OsmandPreference<Boolean> ANNOUNCE_NEARBY_POI = new BooleanPreference("announce_nearby_poi", false).makeProfile().cache();

	public final OsmandPreference<Boolean> GPX_ROUTE_CALC_OSMAND_PARTS = new BooleanPreference("gpx_routing_calculate_osmand_route", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> GPX_CALCULATE_RTEPT = new BooleanPreference("gpx_routing_calculate_rtept", true).makeGlobal().cache();
	public final OsmandPreference<Boolean> GPX_ROUTE_CALC = new BooleanPreference("calc_gpx_route", false).makeGlobal().cache();

	public final OsmandPreference<Boolean> AVOID_TOLL_ROADS = new BooleanPreference("avoid_toll_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_MOTORWAY = new BooleanPreference("avoid_motorway", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_UNPAVED_ROADS = new BooleanPreference("avoid_unpaved_roads", false).makeProfile().cache();
	public final OsmandPreference<Boolean> AVOID_FERRIES = new BooleanPreference("avoid_ferries", false).makeProfile().cache();

	public final OsmandPreference<Boolean> PREFER_MOTORWAYS = new BooleanPreference("prefer_motorways", false).makeProfile().cache();

	public final OsmandPreference<Long> LAST_UPDATES_CARD_REFRESH = new LongPreference("last_updates_card_refresh", 0).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> SAVE_TRACK_INTERVAL = new IntPreference("save_track_interval", 5000).makeProfile();

	{
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.CAR, 3000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.BICYCLE, 7000);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 10000);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> LIVE_MONITORING = new BooleanPreference("live_monitoring", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> LIVE_MONITORING_INTERVAL = new IntPreference("live_monitoring_interval", 5000).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> LIVE_MONITORING_URL = new StringPreference("live_monitoring_url",
			"http://example.com?lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}").makeGlobal();

	public final CommonPreference<String> GPS_STATUS_APP = new StringPreference("gps_status_app", "").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_OSM_BUGS = new BooleanPreference("show_osm_bugs", false).makeGlobal();

	public final OsmandPreference<String> MAP_INFO_CONTROLS = new StringPreference("map_info_controls", "").makeProfile();

	public final OsmandPreference<String> OSMO_DEVICE_KEY = new StringPreference("osmo_device_token", "").makeGlobal();

	public final OsmandPreference<String> OSMO_USER_NAME = new StringPreference("osmo_user_name", "").makeGlobal();

	public final OsmandPreference<String> OSMO_USER_PWD = new StringPreference("osmo_user_pwd", null).makeGlobal();

	public final OsmandPreference<Boolean> OSMO_AUTO_CONNECT = new BooleanPreference("osmo_automatically_connect", false).makeGlobal();

	public final OsmandPreference<Long> OSMO_LAST_PING = new LongPreference("osmo_last_ping", 0).makeGlobal().cache();

	public final OsmandPreference<Boolean> OSMO_SEND_LOCATIONS_STATE = new BooleanPreference("osmo_send_locations", false).cache().makeGlobal();

	public final OsmandPreference<Boolean> OSMO_SHOW_GROUP_NOTIFICATIONS = new BooleanPreference("osmo_show_toast_notifications", true).makeGlobal();

	public final CommonPreference<Integer> OSMO_SAVE_TRACK_INTERVAL = new IntPreference("osmo_save_track_interval", 10000).makeGlobal().cache();

	public final OsmandPreference<String> OSMO_GROUPS = new StringPreference("osmo_groups", "{}").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DEBUG_RENDERING_INFO = new BooleanPreference("debug_rendering", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_FAVORITES = new BooleanPreference("show_favorites", true).makeGlobal().cache();

	public final CommonPreference<Boolean> SHOW_ZOOM_BUTTONS_NAVIGATION = new BooleanPreference("show_zoom_buttons_navigation", false).makeProfile().cache();

	{
		SHOW_ZOOM_BUTTONS_NAVIGATION.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	// Json
	public final OsmandPreference<String> SELECTED_GPX = new StringPreference("selected_gpx", "").makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAP_SCREEN_ORIENTATION =
			new IntPreference("map_screen_orientation", -1/*ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED*/).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
//	public final CommonPreference<Boolean> SHOW_VIEW_ANGLE = new BooleanPreference("show_view_angle", false).makeProfile().cache();
//	{
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.CAR, false);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.BICYCLE, true);
//		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
//	}

	// this value string is synchronized with settings_pref.xml preference name
	// seconds to auto_follow
	public final CommonPreference<Integer> AUTO_FOLLOW_ROUTE = new IntPreference("auto_follow_route", 0).makeProfile();

	{
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.CAR, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.BICYCLE, 15);
		AUTO_FOLLOW_ROUTE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// this value string is synchronized with settings_pref.xml preference name
	// seconds to auto_follow
	public final CommonPreference<Integer> KEEP_INFORMING = new IntPreference("keep_informing", 0).makeProfile();

	{
		// 0 means never
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.CAR, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		KEEP_INFORMING.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// screen power save
	public final CommonPreference<Integer> WAKE_ON_VOICE_INT = new IntPreference("wake_on_voice_int", 0).makeProfile();

	{
		// 0 means never
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.CAR, 0);
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.BICYCLE, 0);
		WAKE_ON_VOICE_INT.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 0);
	}

	// this value string is synchronized with settings_pref.xml preference name
	// try without AUTO_FOLLOW_ROUTE_NAV (see forum discussion 'Simplify our navigation preference menu')
	//public final CommonPreference<Boolean> AUTO_FOLLOW_ROUTE_NAV = new BooleanPreference("auto_follow_route_navigation", true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	public final CommonPreference<Integer> ROTATE_MAP =
			new IntPreference("rotate_map", ROTATE_MAP_NONE).makeProfile().cache();

	{
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.CAR, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, ROTATE_MAP_COMPASS);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;
	public final CommonPreference<Boolean> CENTER_POSITION_ON_MAP = new BooleanPreference("center_position_on_map", false).makeProfile();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAX_LEVEL_TO_DOWNLOAD_TILE = new IntPreference("max_level_download_tile", 20).makeProfile().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> LEVEL_TO_SWITCH_VECTOR_RASTER = new IntPreference("level_to_switch_vector_raster", 1).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> AUDIO_STREAM_GUIDANCE = new IntPreference("audio_stream",
			3/*AudioManager.STREAM_MUSIC*/).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> MAP_ONLINE_DATA = new BooleanPreference("map_online_data", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SHOW_DESTINATION_ARROW = new BooleanPreference("show_destination_arrow", false).makeProfile();

	{
		SHOW_DESTINATION_ARROW.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_OVERLAY = new StringPreference("map_overlay", null).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_UNDERLAY = new StringPreference("map_underlay", null).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_OVERLAY_TRANSPARENCY = new IntPreference("overlay_transparency",
			100).makeGlobal().cache();

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_TRANSPARENCY = new IntPreference("map_transparency",
			255).makeGlobal().cache();

//	// this value string is synchronized with settings_pref.xml preference name
//	public final CommonPreference<String> MAP_TILE_SOURCES = new StringPreference("map_tile_sources",
//			TileSourceManager.getMapnikSource().getName()).makeGlobal();

	public final CommonPreference<Boolean> SHOW_LAYER_TRANSPARENCY_SEEKBAR =
			new BooleanPreference("show_layer_transparency_seekbar", false).makeGlobal();

	public final CommonPreference<String> MAP_OVERLAY_PREVIOUS = new StringPreference("map_overlay_previous", null).makeGlobal().cache();

	public final CommonPreference<String> MAP_UNDERLAY_PREVIOUS = new StringPreference("map_underlay_previous", null).makeGlobal().cache();

	public CommonPreference<String> PREVIOUS_INSTALLED_VERSION = new StringPreference("previous_installed_version", "").makeGlobal();

	public final OsmandPreference<Boolean> SHOULD_SHOW_FREE_VERSION_BANNER = new BooleanPreference("should_show_free_version_banner", false).makeGlobal().cache();

	public final OsmandPreference<Boolean> USE_MAP_MARKERS = new BooleanPreference("use_map_markers", true).makeGlobal().cache();

	public final CommonPreference<MapMarkersMode> MAP_MARKERS_MODE =
			new EnumIntPreference<>("map_markers_mode", MapMarkersMode.TOOLBAR, MapMarkersMode.values());

	{
		MAP_MARKERS_MODE.makeProfile().cache();
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.DEFAULT, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.CAR, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.BICYCLE, MapMarkersMode.TOOLBAR);
		MAP_MARKERS_MODE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, MapMarkersMode.TOOLBAR);
	}

	public final OsmandPreference<Boolean> ROUTE_MAP_MARKERS_START_MY_LOC = new BooleanPreference("route_map_markers_start_my_loc", false).makeGlobal().cache();


	public static final String EXTERNAL_STORAGE_DIR = "external_storage_dir"; //$NON-NLS-1$

	public static final String EXTERNAL_STORAGE_DIR_V19 = "external_storage_dir_V19"; //$NON-NLS-1$
	public static final String EXTERNAL_STORAGE_DIR_TYPE_V19 = "external_storage_dir_type_V19"; //$NON-NLS-1$
	public static final int EXTERNAL_STORAGE_TYPE_DEFAULT = 0; // Environment.getExternalStorageDirectory()
	public static final int EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE = 1; // ctx.getExternalFilesDirs(null)
	public static final int EXTERNAL_STORAGE_TYPE_INTERNAL_FILE = 2; // ctx.getFilesDir()
	public static final int EXTERNAL_STORAGE_TYPE_OBB = 3; // ctx.getObbDirs
	public static final int EXTERNAL_STORAGE_TYPE_SPECIFIED = 4;



	public static boolean isWritable(File dirToTest) {
		boolean isWriteable = false;
		try {
			dirToTest.mkdirs();
			File writeTestFile = File.createTempFile("osmand_", ".tmp", dirToTest);
			isWriteable = writeTestFile.exists();
			writeTestFile.delete();
		} catch (IOException e) {
			isWriteable = false;
		}
		return isWriteable;
	}


	public Object getGlobalPreferences() {
		return globalPreferences;
	}


	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom"; //$NON-NLS-1$

	public static final String MAP_LABEL_TO_SHOW = "map_label_to_show"; //$NON-NLS-1$
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show"; //$NON-NLS-1$
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show"; //$NON-NLS-1$
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show"; //$NON-NLS-1$

	public LatLon getLastKnownMapLocation() {
		float lat = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}

	public boolean isLastKnownMapLocation() {
		return settingsAPI.contains(globalPreferences, LAST_KNOWN_MAP_LAT);
	}


	public LatLon getAndClearMapLocationToShow() {
		if (!settingsAPI.contains(globalPreferences, MAP_LAT_TO_SHOW)) {
			return null;
		}
		float lat = settingsAPI.getFloat(globalPreferences, MAP_LAT_TO_SHOW, 0);
		float lon = settingsAPI.getFloat(globalPreferences, MAP_LON_TO_SHOW, 0);
		settingsAPI.edit(globalPreferences).remove(MAP_LAT_TO_SHOW).commit();
		return new LatLon(lat, lon);
	}


	private Object objectToShow;

	public Object getAndClearObjectToShow() {
		Object objectToShow = this.objectToShow;
		this.objectToShow = null;
		return objectToShow;
	}

	public int getMapZoomToShow() {
		return settingsAPI.getInt(globalPreferences, MAP_ZOOM_TO_SHOW, 5);
	}


	// Do not use that method if you want to show point on map. Use setMapLocationToShow
	public void setLastKnownMapLocation(double latitude, double longitude) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences);
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}

	public int getLastKnownMapZoom() {
		return settingsAPI.getInt(globalPreferences, LAST_KNOWN_MAP_ZOOM, 5);
	}

	public void setLastKnownMapZoom(int zoom) {
		settingsAPI.edit(globalPreferences).putInt(LAST_KNOWN_MAP_ZOOM, zoom).commit();
	}

	public final static String POINT_NAVIGATE_LAT = "point_navigate_lat"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON = "point_navigate_lon"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_ROUTE = "point_navigate_route_integer"; //$NON-NLS-1$
	public final static int NAVIGATE = 1;
	public final static String POINT_NAVIGATE_DESCRIPTION = "point_navigate_description"; //$NON-NLS-1$
	public final static String START_POINT_LAT = "start_point_lat"; //$NON-NLS-1$
	public final static String START_POINT_LON = "start_point_lon"; //$NON-NLS-1$
	public final static String START_POINT_DESCRIPTION = "start_point_description"; //$NON-NLS-1$

	public final static String INTERMEDIATE_POINTS = "intermediate_points"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_DESCRIPTION = "intermediate_points_description"; //$NON-NLS-1$

	public final static String POINT_NAVIGATE_LAT_BACKUP = "point_navigate_lat_backup"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON_BACKUP = "point_navigate_lon_backup"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_DESCRIPTION_BACKUP = "point_navigate_description_backup"; //$NON-NLS-1$
	public final static String START_POINT_LAT_BACKUP = "start_point_lat_backup"; //$NON-NLS-1$
	public final static String START_POINT_LON_BACKUP = "start_point_lon_backup"; //$NON-NLS-1$
	public final static String START_POINT_DESCRIPTION_BACKUP = "start_point_description_backup"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_BACKUP = "intermediate_points_backup"; //$NON-NLS-1$
	public final static String INTERMEDIATE_POINTS_DESCRIPTION_BACKUP = "intermediate_points_description_backup"; //$NON-NLS-1$

	public final static String MAP_MARKERS_POINT = "map_markers_point"; //$NON-NLS-1$
	public final static String MAP_MARKERS_COLOR = "map_markers_color"; //$NON-NLS-1$
	public final static String MAP_MARKERS_DESCRIPTION = "map_markers_description"; //$NON-NLS-1$
	public final static String MAP_MARKERS_POSITION = "map_markers_position"; //$NON-NLS-1$
	public final static String MAP_MARKERS_SELECTION = "map_markers_selection"; //$NON-NLS-1$
	public final static String MAP_MARKERS_HISTORY_POINT = "map_markers_history_point"; //$NON-NLS-1$
	public final static String MAP_MARKERS_HISTORY_DESCRIPTION = "map_markers_history_description"; //$NON-NLS-1$
	public final static int MAP_MARKERS_HISTORY_LIMIT = 30;

	public void backupPointToStart() {
		settingsAPI.edit(globalPreferences)
				.putFloat(START_POINT_LAT_BACKUP, settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0))
				.putFloat(START_POINT_LON_BACKUP, settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0))
				.putString(START_POINT_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION, ""))
				.commit();
	}

	private void backupPointToNavigate() {
		settingsAPI.edit(globalPreferences)
				.putFloat(POINT_NAVIGATE_LAT_BACKUP, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0))
				.putFloat(POINT_NAVIGATE_LON_BACKUP, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0))
				.putString(POINT_NAVIGATE_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION, ""))
				.commit();
	}

	private void backupIntermediatePoints() {
		settingsAPI.edit(globalPreferences)
				.putString(INTERMEDIATE_POINTS_BACKUP, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS, ""))
				.putString(INTERMEDIATE_POINTS_DESCRIPTION_BACKUP, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_DESCRIPTION, ""))
				.commit();
	}

	public void backupTargetPoints() {
		backupPointToStart();
		backupPointToNavigate();
		backupIntermediatePoints();
	}

	public void restoreTargetPoints() {
		settingsAPI.edit(globalPreferences)
				.putFloat(START_POINT_LAT, settingsAPI.getFloat(globalPreferences, START_POINT_LAT_BACKUP, 0))
				.putFloat(START_POINT_LON, settingsAPI.getFloat(globalPreferences, START_POINT_LON_BACKUP, 0))
				.putString(START_POINT_DESCRIPTION, settingsAPI.getString(globalPreferences, START_POINT_DESCRIPTION_BACKUP, ""))
				.putFloat(POINT_NAVIGATE_LAT, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT_BACKUP, 0))
				.putFloat(POINT_NAVIGATE_LON, settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON_BACKUP, 0))
				.putString(POINT_NAVIGATE_DESCRIPTION, settingsAPI.getString(globalPreferences, POINT_NAVIGATE_DESCRIPTION_BACKUP, ""))
				.putString(INTERMEDIATE_POINTS, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_BACKUP, ""))
				.putString(INTERMEDIATE_POINTS_DESCRIPTION, settingsAPI.getString(globalPreferences, INTERMEDIATE_POINTS_DESCRIPTION_BACKUP, ""))
				.commit();
	}

	public LatLon getPointToNavigate() {
		float lat = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, POINT_NAVIGATE_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}


	public LatLon getPointToStart() {
		float lat = settingsAPI.getFloat(globalPreferences, START_POINT_LAT, 0);
		float lon = settingsAPI.getFloat(globalPreferences, START_POINT_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}


	public int isRouteToPointNavigateAndClear() {
		int vl = settingsAPI.getInt(globalPreferences, POINT_NAVIGATE_ROUTE, 0);
		if (vl != 0) {
			settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_ROUTE).commit();
		}
		return vl;
	}


	public boolean clearIntermediatePoints() {
		return settingsAPI.edit(globalPreferences).remove(INTERMEDIATE_POINTS).remove(INTERMEDIATE_POINTS_DESCRIPTION).commit();
	}

	public boolean clearActiveMapMarkers() {
		return settingsAPI.edit(globalPreferences)
				.remove(MAP_MARKERS_POINT)
				.remove(MAP_MARKERS_DESCRIPTION)
				.remove(MAP_MARKERS_COLOR)
				.remove(MAP_MARKERS_POSITION)
				.remove(MAP_MARKERS_SELECTION)
				.commit();
	}

	public boolean clearMapMarkersHistory() {
		return settingsAPI.edit(globalPreferences)
				.remove(MAP_MARKERS_HISTORY_POINT)
				.remove(MAP_MARKERS_HISTORY_DESCRIPTION)
				.commit();
	}

	public final CommonPreference<Boolean> USE_INTERMEDIATE_POINTS_NAVIGATION =
			new BooleanPreference("use_intermediate_points_navigation", false).makeGlobal().cache();





	public boolean clearPointToNavigate() {
		return settingsAPI.edit(globalPreferences).remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).
				remove(POINT_NAVIGATE_DESCRIPTION).commit();
	}

	public boolean clearPointToStart() {
		return settingsAPI.edit(globalPreferences).remove(START_POINT_LAT).remove(START_POINT_LON).
				remove(START_POINT_DESCRIPTION).commit();
	}

	public boolean navigateDialog() {
		return settingsAPI.edit(globalPreferences).putInt(POINT_NAVIGATE_ROUTE, NAVIGATE).commit();
	}


	/**
	 * the location of a parked car
	 */

	public static final String LAST_SEARCHED_REGION = "last_searched_region"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY = "last_searched_city"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY_NAME = "last_searched_city_name"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_POSTCODE = "last_searched_postcode"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_STREET = "last_searched_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_LAT = "last_searched_lat"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_LON = "last_searched_lon"; //$NON-NLS-1$

	public LatLon getLastSearchedPoint() {
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_LAT) && settingsAPI.contains(globalPreferences, LAST_SEARCHED_LON)) {
			return new LatLon(settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LAT, 0),
					settingsAPI.getFloat(globalPreferences, LAST_SEARCHED_LON, 0));
		}
		return null;
	}

	public boolean setLastSearchedPoint(LatLon l) {
		if (l == null) {
			return settingsAPI.edit(globalPreferences).remove(LAST_SEARCHED_LAT).remove(LAST_SEARCHED_LON).commit();
		} else {
			return setLastSearchedPoint(l.getLatitude(), l.getLongitude());
		}
	}

	public boolean setLastSearchedPoint(double lat, double lon) {
		return settingsAPI.edit(globalPreferences).putFloat(LAST_SEARCHED_LAT, (float) lat).
				putFloat(LAST_SEARCHED_LON, (float) lon).commit();
	}

	public String getLastSearchedRegion() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_REGION, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedRegion(String region, LatLon l) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).
				putString(LAST_SEARCHED_CITY_NAME, "").putString(LAST_SEARCHED_POSTCODE, "").
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(l);
		return res;
	}

	public String getLastSearchedPostcode() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_POSTCODE, null);
	}

	public boolean setLastSearchedPostcode(String postcode, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "") //$NON-NLS-1$
				.putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, postcode); //$NON-NLS-1$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public Long getLastSearchedCity() {
		return settingsAPI.getLong(globalPreferences, LAST_SEARCHED_CITY, -1);
	}

	public String getLastSearchedCityName() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_CITY_NAME, "");
	}

	public boolean setLastSearchedCity(Long cityId, String name, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_CITY_NAME, name).
				putString(LAST_SEARCHED_STREET, "").putString(LAST_SEARCHED_BUILDING, "").putString(LAST_SEARCHED_POSTCODE, ""); //$NON-NLS-1$
		//edit.remove(LAST_SEARCHED_POSTCODE);
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedStreet() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedStreet(String street, LatLon point) {
		SettingsEditor edit = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		if (settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		boolean res = edit.commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedBuilding() {
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedBuilding(String building, LatLon point) {
		boolean res = settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
		setLastSearchedPoint(point);
		return res;
	}

	public String getLastSearchedIntersectedStreet() {
		if (!settingsAPI.contains(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET)) {
			return null;
		}
		return settingsAPI.getString(globalPreferences, LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedIntersectedStreet(String street, LatLon l) {
		setLastSearchedPoint(l);
		return settingsAPI.edit(globalPreferences).putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}


	public final OsmandPreference<String> SELECTED_POI_FILTER_FOR_MAP = new StringPreference("selected_poi_filter_for_map", null).makeGlobal().cache();

	public static final String VOICE_PROVIDER_NOT_USE = "VOICE_PROVIDER_NOT_USE";

	public static final String[] TTS_AVAILABLE_VOICES = new String[]{
			"de", "en", "es", "fr", "it", "ja", "nl", "pl", "pt", "ru", "zh"
	};
	// this value string is synchronized with settings_pref.xml preference name
	// this value could localized
	public final OsmandPreference<String> VOICE_PROVIDER = new StringPreference("voice_provider", null) {
		protected String getDefaultValue() {
			return "en-tts";
		}

		;
	}.makeGlobal();


//	// this value string is synchronized with settings_pref.xml preference name
//	public final CommonPreference<String> RENDERER = new StringPreference("renderer", RendererRegistry.DEFAULT_RENDER) {
//		{
//			makeProfile();
//		}
//
//		@Override
//		protected boolean setValue(Object prefs, String val) {
//			val = RendererRegistry.DEFAULT_RENDER;
//			super.setValue(prefs, val);
//			return true;
//		}
//
//		;
//	};
//

	Map<String, CommonPreference<String>> customRendersProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<String>>();

	public CommonPreference<String> getCustomRenderProperty(String attrName) {
		if (!customRendersProps.containsKey(attrName)) {
			customRendersProps.put(attrName, new StringPreference("nrenderer_" + attrName, "").makeProfile());
		}
		return customRendersProps.get(attrName);
	}

	{
		CommonPreference<String> pref = getCustomRenderProperty("appMode");
		pref.setModeDefaultValue(ApplicationMode.CAR, "car");
		pref.setModeDefaultValue(ApplicationMode.PEDESTRIAN, "pedestrian");
		pref.setModeDefaultValue(ApplicationMode.BICYCLE, "bicycle");
	}

	Map<String, CommonPreference<Boolean>> customBooleanRendersProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<Boolean>>();

	public CommonPreference<Boolean> getCustomRenderBooleanProperty(String attrName) {
		if (!customBooleanRendersProps.containsKey(attrName)) {
			customBooleanRendersProps.put(attrName, new BooleanPreference("nrenderer_" + attrName, false).makeProfile());
		}
		return customBooleanRendersProps.get(attrName);
	}

	Map<String, CommonPreference<String>> customRoutingProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<String>>();

	public CommonPreference<String> getCustomRoutingProperty(String attrName, String defValue) {
		if (!customRoutingProps.containsKey(attrName)) {
			customRoutingProps.put(attrName, new StringPreference("prouting_" + attrName, defValue).makeProfile());
		}
		return customRoutingProps.get(attrName);
	}

	{
//		CommonPreference<String> pref = getCustomRoutingProperty("appMode");
//		pref.setModeDefaultValue(ApplicationMode.CAR, "car");
	}

	Map<String, CommonPreference<Boolean>> customBooleanRoutingProps = new LinkedHashMap<String, OsmandSettings.CommonPreference<Boolean>>();

	public CommonPreference<Boolean> getCustomRoutingBooleanProperty(String attrName) {
		if (!customBooleanRoutingProps.containsKey(attrName)) {
			customBooleanRoutingProps.put(attrName, new BooleanPreference("prouting_" + attrName, false).makeProfile());
		}
		return customBooleanRoutingProps.get(attrName);
	}

	public final OsmandPreference<Boolean> USE_OSM_LIVE_FOR_ROUTING = new BooleanPreference("enable_osmc_routing", false).makeGlobal();
	
	public final OsmandPreference<Boolean> VOICE_MUTE = new BooleanPreference("voice_mute", false).makeGlobal();

	// for background service
	public final OsmandPreference<Boolean> MAP_ACTIVITY_ENABLED = new BooleanPreference("map_activity_enabled", false).makeGlobal();

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SAFE_MODE = new BooleanPreference("safe_mode", false).makeGlobal();

	public final OsmandPreference<Boolean> NATIVE_RENDERING_FAILED = new BooleanPreference("native_rendering_failed_init", false).makeGlobal();

	public final OsmandPreference<Boolean> USE_OPENGL_RENDER = new BooleanPreference("use_opengl_render",
			false /*Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH*/
	).makeGlobal().cache();

	public final OsmandPreference<Boolean> OPENGL_RENDER_FAILED = new BooleanPreference("opengl_render_failed", false).makeGlobal().cache();


	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_ENABLED = "service_off_enabled"; //$NON-NLS-1$

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> SERVICE_OFF_INTERVAL = new IntPreference("service_off_interval",
			0).makeGlobal();


	public final OsmandPreference<String> CONTRIBUTION_INSTALL_APP_DATE = new StringPreference("CONTRIBUTION_INSTALL_APP_DATE", null).makeGlobal();


	public final OsmandPreference<Boolean> FOLLOW_THE_ROUTE = new BooleanPreference("follow_to_route", false).makeGlobal();
	public final OsmandPreference<String> FOLLOW_THE_GPX_ROUTE = new StringPreference("follow_gpx", null).makeGlobal();

	public final OsmandPreference<Boolean> SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME =
			new BooleanPreference("show_arrival_time", true).makeGlobal();

	public final OsmandPreference<Long> AGPS_DATA_LAST_TIME_DOWNLOADED =
			new LongPreference("agps_data_downloaded", 0).makeGlobal();

	// Live Updates
	public final OsmandPreference<Boolean> IS_LIVE_UPDATES_ON =
			new BooleanPreference("is_live_updates_on", false).makeGlobal();
	public final OsmandPreference<Integer> LIVE_UPDATES_RETRIES =
			new IntPreference("live_updates_retryes", 2).makeGlobal();

	// UI boxes
	public final CommonPreference<Boolean> TRANSPARENT_MAP_THEME =
			new BooleanPreference("transparent_map_theme", true).makeProfile();

	{
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.CAR, false);
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		TRANSPARENT_MAP_THEME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}

	public final CommonPreference<Boolean> SHOW_STREET_NAME =
			new BooleanPreference("show_street_name", false).makeProfile();

	{
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.CAR, true);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		SHOW_STREET_NAME.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}

	public static final int OSMAND_DARK_THEME = 0;
	public static final int OSMAND_LIGHT_THEME = 1;


	public final CommonPreference<Integer> SEARCH_TAB =
			new IntPreference("SEARCH_TAB", 0).makeGlobal().cache();

	public final CommonPreference<Integer> FAVORITES_TAB =
			new IntPreference("FAVORITES_TAB", 0).makeGlobal().cache();

	public final CommonPreference<Integer> OSMAND_THEME =
			new IntPreference("osmand_theme", OSMAND_LIGHT_THEME).makeGlobal().cache();

	public boolean isLightActionBar() {
		return true;
	}


	public boolean isLightContent() {
		return OSMAND_THEME.get() != OSMAND_DARK_THEME;
	}


	public final CommonPreference<Boolean> FLUORESCENT_OVERLAYS =
			new BooleanPreference("fluorescent_overlays", false).makeGlobal().cache();


	public final CommonPreference<Boolean> SHOW_RULER =
			new BooleanPreference("show_ruler", true).makeProfile().cache();

//	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS_V2 = new IntPreference("free_downloads_v2", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_FREE_DOWNLOADS = new IntPreference("free_downloads_v3", 0).makeGlobal();

	// For DashRateUsFragment
	public final OsmandPreference<Long> LAST_DISPLAY_TIME =
			new LongPreference("last_display_time", 0).makeGlobal().cache();

	public final OsmandPreference<Long> LAST_CHECKED_UPDATES =
			new LongPreference("last_checked_updates", 0).makeGlobal();

	public final OsmandPreference<Integer> NUMBER_OF_APPLICATION_STARTS =
			new IntPreference("number_of_app_starts", 0).makeGlobal().cache();


	public enum DayNightMode {
		AUTO(0),
		DAY(1),
		NIGHT(2),
		SENSOR(3);

		private final int key;

		DayNightMode(int key) {
			this.key = key;
		}

		public String toHumanString() {
			return ""+key;
		}

		public boolean isSensor() {
			return this == SENSOR;
		}

		public boolean isAuto() {
			return this == AUTO;
		}

		public boolean isDay() {
			return this == DAY;
		}

		public boolean isNight() {
			return this == NIGHT;
		}

		public static DayNightMode[] possibleValues() {
			return DayNightMode.values();
		}
	}

	public enum MapMarkersMode {
		TOOLBAR(0),
		WIDGETS(1),
		NONE(2);

		private final int key;

		MapMarkersMode(int key) {
			this.key = key;
		}

		public String toHumanString() {
			return ""+key;
		}

		public boolean isToolbar() {
			return this == TOOLBAR;
		}

		public boolean isWidgets() {
			return this == WIDGETS;
		}

		public boolean isNone() {
			return this == NONE;
		}

		public static MapMarkersMode[] possibleValues() {
			return new MapMarkersMode[]{TOOLBAR, WIDGETS, NONE};
		}
	}

	public enum SpeedConstants {
		KILOMETERS_PER_HOUR(0, 0),
		MILES_PER_HOUR(1, 1),
		METERS_PER_SECOND(2, 2),
		MINUTES_PER_MILE(3, 3),
		MINUTES_PER_KILOMETER(4, 4),
		NAUTICALMILES_PER_HOUR(5, 5);

		private final int key;
		private int descr;

		SpeedConstants(int key, int descr) {
			this.key = key;
			this.descr = descr;
		}

		public String toHumanString() {
			return ""+(descr);
		}

		public String toShortString() {
			return ""+(key);
		}


	}

	public enum MetricsConstants {
		KILOMETERS_AND_METERS(0, "km-m"),
		MILES_AND_FOOTS(1, "mi-f"),
		NAUTICAL_MILES(2, "nm"),
		MILES_AND_YARDS(3, "mi-y");

		private final int key;
		private final String ttsString;

		MetricsConstants(int key, String ttsString) {
			this.key = key;
			this.ttsString = ttsString;
		}

		public String toHumanString() {
			return ""+(key);
		}

		public String toTTSString() {
			return ttsString;
		}

	}

	public enum AutoZoomMap {
		NONE(0, 0f, 18),
		FARTHEST(1, 1f, 15.5f),
		FAR(2, 1.4f, 17f),
		CLOSE(3, 2f, 19f);
		public final float coefficient;
		public final int name;
		public final float maxZoom;

		AutoZoomMap(int name, float coefficient, float maxZoom) {
			this.name = name;
			this.coefficient = coefficient;
			this.maxZoom = maxZoom;

		}
	}

	/**
	 * Class represents specific for driving region
	 * Signs, leftHandDriving
	 */
	public enum DrivingRegion {

		EUROPE_ASIA(0, MetricsConstants.KILOMETERS_AND_METERS, false, false),
		US(1, MetricsConstants.MILES_AND_FOOTS, false, true),
		CANADA(2, MetricsConstants.KILOMETERS_AND_METERS, false, true),
		UK_AND_OTHERS(3, MetricsConstants.MILES_AND_FOOTS, true, false),
		JAPAN(4, MetricsConstants.KILOMETERS_AND_METERS, true, false);

		public final boolean leftHandDriving;
		public final boolean americanSigns;
		public final MetricsConstants defMetrics;
		public final int name;

		DrivingRegion(int name, MetricsConstants def, boolean leftHandDriving, boolean americanSigns) {
			this.name = name;
			defMetrics = def;
			this.leftHandDriving = leftHandDriving;
			this.americanSigns = americanSigns;
		}

	}

}
