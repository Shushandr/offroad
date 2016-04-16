package net.osmand.plus.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.Version;
import net.sourceforge.offroad.OsmWindow;

public class DownloadOsmandIndexesHelper {
	private final static Log log = PlatformUtil.getLog(DownloadOsmandIndexesHelper.class);
	
	public static class IndexFileList implements Serializable {
		private static final long serialVersionUID = 1L;

		private boolean downloadedFromInternet = false;
		IndexItem basemap;
		ArrayList<IndexItem> indexFiles = new ArrayList<IndexItem>();
		private String mapversion;
		
		private Comparator<IndexItem> comparator = new Comparator<IndexItem>(){
			@Override
			public int compare(IndexItem o1, IndexItem o2) {
				String object1 = o1.getFileName();
				String object2 = o2.getFileName();
				if(object1.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
					if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
						return object1.compareTo(object2);
					} else {
						return -1;
					}
				} else if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
					return 1;
				}
				return object1.compareTo(object2);
			}
		};
		
		public void setDownloadedFromInternet(boolean downloadedFromInternet) {
			this.downloadedFromInternet = downloadedFromInternet;
		}
		
		public boolean isDownloadedFromInternet() {
			return downloadedFromInternet;
		}

		public void setMapVersion(String mapversion) {
			this.mapversion = mapversion;
		}

		public void add(IndexItem indexItem) {
			indexFiles.add(indexItem);
			if(indexItem.getFileName().toLowerCase().startsWith("world_basemap")) {
				basemap = indexItem;
			}
		}
		
		public void sort(){
			Collections.sort(indexFiles, comparator);
		}

		public boolean isAcceptable() {
			return (indexFiles != null && !indexFiles.isEmpty()) || (mapversion != null);
		}

		public List<IndexItem> getIndexFiles() {
			return indexFiles;
		}
		
		public IndexItem getBasemap() {
			return basemap;
		}

		public boolean isIncreasedMapVersion() {
			try {
				int mapVersionInList = Integer.parseInt(mapversion);
				return IndexConstants.BINARY_MAP_VERSION < mapVersionInList;
			} catch (NumberFormatException e) {
				//ignore this...
			}
			return false;
		}

	}	

	public static IndexFileList getIndexesList(OsmWindow app) {
		IndexFileList result = downloadIndexesListFromInternet(app);
		if (result == null) {
			result = new IndexFileList();
		} else {
			result.setDownloadedFromInternet(true);
		}
		// add all tts files from assets
		listVoiceAssets(result, app, app.getSettings());
		return result;
	}
	
	private static Map<String, String>  assetMapping(OsmWindow ctx) throws XmlPullParserException, IOException {
		XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser();
		InputStream isBundledAssetsXml = ctx.getResource("bundled_assets.xml");
		xmlParser.setInput(isBundledAssetsXml, "UTF-8");
		Map<String, String> assets = new HashMap<String, String>();
		int next = 0;
		while ((next = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
			if (next == XmlPullParser.START_TAG && xmlParser.getName().equals("asset")) {
				final String source = xmlParser.getAttributeValue(null, "source");
				final String destination = xmlParser.getAttributeValue(null, "destination");
				assets.put(source, destination);
			}
		}
		isBundledAssetsXml.close();
		return assets;
	}
	
	private static void listVoiceAssets(IndexFileList result,  
			OsmWindow pApp, OsmandSettings settings) {
		try {
			String ext = DownloadActivityType.addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION);
			File voicePath = pApp.getAppPath(IndexConstants.VOICE_INDEX_DIR); 
			// list = amanager.list("voice");
			String date = "";
			long dateModified = System.currentTimeMillis();
//			try {
//				ApplicationInfo appInfo = pm.getApplicationInfo(OsmWindow.class.getPackage().getName(), 0);
//				dateModified =  new File(appInfo.sourceDir).lastModified();
				date = AndroidUtils.formatDate(dateModified);
//			} catch (NameNotFoundException e) {
//				//do nothing...
//			}
			Map<String, String> mapping = assetMapping(pApp);
			for (String key : mapping.keySet()) {
				String target = mapping.get(key);
				if (target.endsWith("-tts/_ttsconfig.p") && target.startsWith("voice/")) {
					String voice = target.substring("voice/".length(), target.length() - "/_ttsconfig.p".length());
					File destFile = new File(voicePath, voice + File.separatorChar + "_ttsconfig.p");
					
					result.add(new AssetIndexItem(voice + ext, "voice", date, dateModified, "0.1", destFile.length(), key,
							destFile.getPath(), DownloadActivityType.VOICE_FILE));
				}
			}
			result.sort();
		} catch (IOException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		}
	}
	

    public static String getIndexesUrl(OsmWindow ctx) {
            return "http://"+IndexConstants.INDEX_DOWNLOAD_DOMAIN+"/get_indexes?gzip&" + Version.getVersionAsURLParam(ctx); //$NON-NLS-1$;
    }

	private static IndexFileList downloadIndexesListFromInternet(OsmWindow ctx){
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			try {
				String strUrl = getIndexesUrl(ctx);
				OsmandSettings settings = ctx.getSettings();
				
				long nd = System.currentTimeMillis(); // ctx.getAppInitializer().getFirstInstalled();
				if(nd > 0) {
					strUrl += "&nd=" + ((System.currentTimeMillis() - nd) / (1000l * 24l * 60l * 60l)); 
				}
				strUrl += "&ns=1"; // + ctx.getAppInitializer().getNumberOfStarts();
				try {
					strUrl += "&aid=cafebabe";// + Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
				} catch (Exception e) {
					e.printStackTrace();
				}
				log.info(strUrl);
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				URLConnection connection = NetworkUtils.getHttpURLConnection(strUrl);
				InputStream in = connection.getInputStream();
				GZIPInputStream gzin = new GZIPInputStream(in);
				parser.setInput(gzin, "UTF-8"); //$NON-NLS-1$
				int next;
				while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (next == XmlPullParser.START_TAG) {
						DownloadActivityType tp = DownloadActivityType.getIndexType(parser.getAttributeValue(null, "type"));
						if (tp != null) {
							IndexItem it = tp.parseIndexItem(ctx, parser);
							if(it != null && !it.getFileName().contains("_wiki")) {
								result.add(it);
							}
						} else if ("osmand_regions".equals(parser.getName())) {
							String mapversion = parser.getAttributeValue(null, "mapversion");
							result.setMapVersion(mapversion);
						}
					}
				}
				result.sort();
				gzin.close();
				in.close();
			} catch (IOException e) {
				log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
				return null;
			} catch (XmlPullParserException e) {
				log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
				return null;
			}
			
			if (result.isAcceptable()) {
				return result;
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
			return null;
		}
	}

	public static class AssetIndexItem extends IndexItem {
		
		private final String assetName;
		private final String destFile;
		private final long dateModified;

		public AssetIndexItem(String fileName, String description, String date,
				long dateModified, String size, long sizeL, String assetName, String destFile, DownloadActivityType type) {
			super(fileName, description, dateModified, size, sizeL, sizeL, type);
			this.dateModified = dateModified;
			this.assetName = assetName;
			this.destFile = destFile;
		}
		
		public long getDateModified() {
			return dateModified;
		}

		@Override
		public DownloadEntry createDownloadEntry(OsmWindow ctx) {
			return new DownloadEntry(assetName, destFile, dateModified);
		}

		public String getDestFile(){
			return destFile;
		}
	}
	
	
}
