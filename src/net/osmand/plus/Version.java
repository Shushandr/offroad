package net.osmand.plus;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	private final static String FREE_VERSION_NAME = "net.osmand";
	private final static String SHERPAFY_VERSION_NAME = "net.osmand.sherpafy";
	
	
	public static boolean isGpsStatusEnabled(OsmWindow ctx) {
		return isGooglePlayEnabled(ctx) && !isBlackberry(ctx);
	}
	
	public static boolean isBlackberry(OsmWindow ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+blackberry");
	}
	
	public static boolean isMarketEnabled(OsmWindow ctx) {
		return isGooglePlayEnabled(ctx) || isAmazonEnabled(ctx);
	}
	
	public static String marketPrefix(OsmWindow ctx) {
		if (isAmazonEnabled(ctx)) {
			return "amzn://apps/android?p=";
		} else if (isGooglePlayEnabled(ctx)) {
			return "market://search?q=pname:";
		} 
		return "http://osmand.net/apps?id="; 
	}
	
	private static boolean isAmazonEnabled(OsmWindow ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+amazon");
	}
	
	public static boolean isGooglePlayEnabled(OsmWindow ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+play_market");
	}
	
	public static boolean isSherpafy(OsmWindow ctx) {
		return false;
	}
	
	private Version(OsmWindow ctx) {
		appVersion = ctx.getString(R.string.app_version);
		appName = ctx.getString(R.string.app_name);
	}

	private static Version ver = null;
	private static Version getVersion(OsmWindow ctx){
		if(ver == null){
			ver = new Version(ctx);
		}
		return ver;
	}
	
	public static String getFullVersion(OsmWindow ctx){
		Version v = getVersion(ctx);
		return v.appName + " " + v.appVersion;
	}
	
	public static String getAppVersion(OsmWindow ctx){
		Version v = getVersion(ctx);
		return v.appVersion;
	}

	public static String getBuildAppEdition(OsmWindow ctx){
		return ctx.getString(R.string.app_edition);
	}
	
	public static String getAppName(OsmWindow ctx){
		Version v = getVersion(ctx);
		return v.appName;
	}
	
	public static boolean isProductionVersion(OsmWindow ctx){
		Version v = getVersion(ctx);
		return !v.appVersion.contains("#");
	}

	public static String getVersionAsURLParam(OsmWindow ctx) {
		try {
			return "osmandver=" + URLEncoder.encode(getVersionForTracker(ctx), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static boolean isFreeVersion(OsmWindow ctx){
		return true;
		
	}
	
	public static boolean isDeveloperVersion(OsmWindow ctx){
		return getAppName(ctx).contains("~");
	}
	
	public static String getVersionForTracker(OsmWindow ctx) {
		String v = Version.getAppName(ctx);
		if(Version.isProductionVersion(ctx)){
			v = Version.getFullVersion(ctx);
		} else {
			v +=" test";
		}
		return v;
	}

}
