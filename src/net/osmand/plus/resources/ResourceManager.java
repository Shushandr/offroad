package net.osmand.plus.resources;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;

import net.osmand.AndroidUtils;
import net.osmand.GeoidAltitudeCorrection;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.CachedOsmandIndexes;
import net.osmand.data.Amenity;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;

/**
 * Resource manager is responsible to work with all resources 
 * that could consume memory (especially with file resources).
 * Such as indexes, tiles.
 * Also it is responsible to create cache for that resources if they
 *  can't be loaded fully into memory & clear them on request. 
 */
public class ResourceManager {

	public static final String VECTOR_MAP = "#vector_map"; //$NON-NLS-1$

	
	private static final Log log = PlatformUtil.getLog(ResourceManager.class);
	
	
	protected static ResourceManager manager = null;
	
	// it is not good investigated but no more than 64 (satellite images)
	// Only 8 MB (from 16 Mb whole mem) available for images : image 64K * 128 = 8 MB (8 bit), 64 - 16 bit, 32 - 32 bit
	// at least 3*9?
	protected int maxImgCacheSize = 28;
	
	protected Map<String, BufferedImage> cacheOfImages = new LinkedHashMap<String, BufferedImage>();
	protected Map<String, Boolean> imagesOnFS = new LinkedHashMap<String, Boolean>() ;
	
	protected File dirWithTiles ;
	
	public interface ResourceWatcher {
		
		
		public boolean indexResource(File f);
		
		public List<String> getWatchWorkspaceFolder();
	}
	
	
	// Indexes
	private final Map<String, RegionAddressRepository> addressMap = new ConcurrentHashMap<String, RegionAddressRepository>();
	protected final Map<String, AmenityIndexRepository> amenityRepositories =  new ConcurrentHashMap<String, AmenityIndexRepository>();
	protected final Map<String, String> indexFileNames = new ConcurrentHashMap<String, String>();
	protected final Map<String, String> basemapFileNames = new ConcurrentHashMap<String, String>();
	protected final Map<String, BinaryMapIndexReader> routingMapFiles = new ConcurrentHashMap<String, BinaryMapIndexReader>();
	protected final Map<String, TransportIndexRepository> transportRepositories = new ConcurrentHashMap<String, TransportIndexRepository>();
	
	protected final IncrementalChangesManager changesManager = new IncrementalChangesManager(this);
	
	protected final MapRenderRepositories renderer;

//	private HandlerThread renderingBufferImageThread;
	
	protected boolean internetIsNotAccessible = false;
	private java.text.DateFormat dateFormat;
	private OsmWindow context;
	
	public ResourceManager(OsmWindow pContext) {
		
		context = pContext;
		this.renderer = new MapRenderRepositories(pContext.getSettings());
//		renderingBufferImageThread = new HandlerThread("RenderingBaseImage");
//		renderingBufferImageThread.start();

		dateFormat = DateFormat.getDateInstance();
		resetStoreDirectory();
	}
	
//	public HandlerThread getRenderingBufferImageThread() {
//		return renderingBufferImageThread;
//	}

	
	public void resetStoreDirectory() {
//		context.getAppPath(IndexConstants.GPX_INDEX_DIR).mkdirs();
	}
	
	public java.text.DateFormat getDateFormat() {
		return dateFormat;
	}
	
	// introduce cache in order save memory
	
	protected StringBuilder builder = new StringBuilder(40);
	protected char[] tileId = new char[120];
	private GeoidAltitudeCorrection geoidAltitudeCorrection;
	private boolean searchAmenitiesInProgress;

    ////////////////////////////////////////////// Working with indexes ////////////////////////////////////////////////

	public List<String> reloadIndexesOnStart(IProgress progress, List<String> warnings){
		close();
		reloadIndexes(progress, warnings);
		return warnings;
	}

	public List<String> reloadIndexes(IProgress progress, List<String> warnings) {
		geoidAltitudeCorrection = new GeoidAltitudeCorrection(context.getAppPath("geoid"));
		// do it lazy
		// indexingImageTiles(progress);
		warnings.addAll(indexingMaps(progress));
		warnings.addAll(indexVoiceFiles(progress));
		warnings.addAll(indexAdditionalMaps(progress));
		return warnings;
	}

	public List<String> indexAdditionalMaps(IProgress progress) {
		return Collections.emptyList();
	}


	public List<String> indexVoiceFiles(IProgress progress){
		File file = context.getAppPath("voice");
		file.mkdirs();
		List<String> warnings = new ArrayList<String>();
		if (file.exists() && file.canRead()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f.isDirectory()) {
						File conf = new File(f, "_config.p");
						if (!conf.exists()) {
							conf = new File(f, "_ttsconfig.p");
						}
						if (conf.exists()) {
							indexFileNames.put(f.getName(), dateFormat.format(conf.lastModified())); //$NON-NLS-1$
						}
					}
				}
			}
		}
		return warnings;
	}
	
	private List<String> checkAssets(IProgress progress) {
		return Collections.emptyList();
	}
	
	private void copyRegionsBoundaries() {
		try {
			File file = context.getAppPath("regions.ocbf");
			if (file != null) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(context.getResource("regions.ocbf"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	private void copyPoiTypes() {
		try {
			File file = context.getAppPath("poi_types.xml");
			if (file != null) {
				FileOutputStream fout = new FileOutputStream(file);
				Algorithms.streamCopy(context.getResource("poi_types.xml"), fout);
				fout.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private List<File> collectFiles(File dir, String ext, List<File> files) {
		if(dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if(lf == null || lf.length == 0) {
				return files;
			}
			for (File f : lf) {
				if (f.getName().endsWith(ext)) {
					System.out.println("Found " + f.getName() + " on search for " + dir.getAbsolutePath());
					files.add(f);
				}
			}
		}
		return files;
	}
	
	
	
	private void renameRoadsFiles(ArrayList<File> files, File roadsPath) {
		Iterator<File> it = files.iterator();
		while(it.hasNext()) {
			File f = it.next();
			if (f.getName().endsWith("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName().replace("-roads" + IndexConstants.BINARY_MAP_INDEX_EXT,
						IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)));
			} else if (f.getName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				f.renameTo(new File(roadsPath, f.getName()));
			}
		}
	}

	public List<String> indexingMaps(final IProgress progress) {
		long val = System.currentTimeMillis();
		ArrayList<File> files = new ArrayList<File>();
		File appPath = context.getAppPath(null);
		File roadsPath = context.getAppPath(IndexConstants.ROADS_INDEX_DIR);
		roadsPath.mkdirs();
		File mapsPath = context.getAppPath(IndexConstants.MAPS_PATH);
		mapsPath.mkdirs();
		
		collectFiles(mapsPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		renameRoadsFiles(files, roadsPath);
		collectFiles(roadsPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		collectFiles(context.getAppPath(IndexConstants.WIKI_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
//		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			collectFiles(context.getAppPath(IndexConstants.SRTM_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);
//		}
		
		changesManager.collectChangesFiles(context.getAppPath(IndexConstants.LIVE_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);

		Collections.sort(files, Algorithms.getFileVersionComparator());
		List<String> warnings = new ArrayList<String>();
		renderer.clearAllResources();
		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = context.getAppPath(IndexConstants.MAPS_INDEXES_CACHE);
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache, CachedOsmandIndexes.VERSION);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		File liveDir = context.getAppPath(IndexConstants.LIVE_INDEX_DIR);
		for (File f : files) {
//			progress.startTask(context.getString(R.string.indexing_map) + " " + f.getName(), -1); //$NON-NLS-1$
			try {
				BinaryMapIndexReader mapReader = null;
				try {
					mapReader = cachedOsmandIndexes.getReader(f);
					if (mapReader.getVersion() != IndexConstants.BINARY_MAP_VERSION) {
						mapReader = null;
					}
					if (mapReader != null) {
						renderer.initializeNewResource(progress, f, mapReader);
					}
				} catch (IOException e) {
					log.error(String.format("File %s could not be read", f.getName()), e);
				}
				if (mapReader == null ) {
//					warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
				} else {
					if (mapReader.isBasemap()) {
						basemapFileNames.put(f.getName(), f.getName());
					}
					long dateCreated = mapReader.getDateCreated();
					if (dateCreated == 0) {
						dateCreated = f.lastModified();
					}
					if(f.getParentFile().getName().equals(liveDir.getName())) {
						boolean toUse = changesManager.index(f, dateCreated, mapReader);
						if(!toUse) {
							try {
								mapReader.close();
							} catch (IOException e) {
								log.error(e.getMessage(), e);
							}
							continue;
						}
					} else {
						changesManager.indexMainMap(f, dateCreated);
						indexFileNames.put(f.getName(), dateFormat.format(dateCreated)); //$NON-NLS-1$
					}
					if (!mapReader.getRegionNames().isEmpty()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							RegionAddressRepositoryBinary rarb = new RegionAddressRepositoryBinary(this,
									new BinaryMapIndexReader(raf, mapReader), f.getName());
							addressMap.put(f.getName(), rarb);
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
//							warnings.add(MessageFormat.format(
//									context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					if (mapReader.hasTransportData()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							transportRepositories.put(f.getName(), new TransportIndexRepositoryBinary(new BinaryMapIndexReader(raf, mapReader)));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
//							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					// disable osmc for routing temporarily due to some bugs
					if (mapReader.containsRouteData() && (!f.getParentFile().equals(liveDir) || 
							context.getSettings().USE_OSM_LIVE_FOR_ROUTING.get())) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							routingMapFiles.put(f.getName(), new BinaryMapIndexReader(raf, mapReader));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
					if (mapReader.containsPoiData()) {
						try {
							RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$
							amenityRepositories.put(f.getName(), new AmenityIndexRepositoryBinary(new BinaryMapIndexReader(raf, mapReader)));
						} catch (IOException e) {
							log.error("Exception reading " + f.getAbsolutePath(), e); //$NON-NLS-1$
//							warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_not_supported), f.getName())); //$NON-NLS-1$
						}
					}
				}
			} catch (OutOfMemoryError oome) {
				log.error("Exception reading " + f.getAbsolutePath(), oome); //$NON-NLS-1$
//				warnings.add(MessageFormat.format(context.getString(R.string.version_index_is_big_for_memory), f.getName()));
			}
		}
		log.debug("All map files initialized " + (System.currentTimeMillis() - val) + " ms");
		if (files.size() > 0 && (!indCache.exists() || indCache.canWrite())) {
			try {
				cachedOsmandIndexes.writeToFile(indCache);
			} catch (Exception e) {
				log.error("Index file could not be written", e);
			}
		}
		return warnings;
	}

	

	////////////////////////////////////////////// Working with amenities ////////////////////////////////////////////////
	public List<Amenity> searchAmenities(SearchPoiTypeFilter filter,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, final ResultMatcher<Amenity> matcher) {
		final List<Amenity> amenities = new ArrayList<Amenity>();
		searchAmenitiesInProgress = true;
		try {
			if (!filter.isEmpty()) {
				for (AmenityIndexRepository index : amenityRepositories.values()) {
					if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
						List<Amenity> r = index.searchAmenities(MapUtils.get31TileNumberY(topLatitude),
								MapUtils.get31TileNumberX(leftLongitude), MapUtils.get31TileNumberY(bottomLatitude),
								MapUtils.get31TileNumberX(rightLongitude), zoom, filter, matcher);
						if(r != null) {
							amenities.addAll(r);
						}
					}
				}
			}
		} finally {
			searchAmenitiesInProgress = false;
		}
		return amenities;
	}
	
	public List<Amenity> searchAmenitiesOnThePath(List<Location> locations, double radius, SearchPoiTypeFilter filter,
			ResultMatcher<Amenity> matcher) {
		searchAmenitiesInProgress = true;
		final List<Amenity> amenities = new ArrayList<Amenity>();
		try {
			if (locations != null && locations.size() > 0) {
				List<AmenityIndexRepository> repos = new ArrayList<AmenityIndexRepository>();
				double topLatitude = locations.get(0).getLatitude();
				double bottomLatitude = locations.get(0).getLatitude();
				double leftLongitude = locations.get(0).getLongitude();
				double rightLongitude = locations.get(0).getLongitude();
				for (Location l : locations) {
					topLatitude = Math.max(topLatitude, l.getLatitude());
					bottomLatitude = Math.min(bottomLatitude, l.getLatitude());
					leftLongitude = Math.min(leftLongitude, l.getLongitude());
					rightLongitude = Math.max(rightLongitude, l.getLongitude());
				}
				if (!filter.isEmpty()) {
					for (AmenityIndexRepository index : amenityRepositories.values()) {
						if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
							repos.add(index);
						}
					}
					if (!repos.isEmpty()) {
						for (AmenityIndexRepository r : repos) {
							List<Amenity> res = r.searchAmenitiesOnThePath(locations, radius, filter, matcher);
							if(res != null) {
								amenities.addAll(res);
							}
						}
					}
				}
			}
		} finally {
			searchAmenitiesInProgress = false;
		}
		return amenities;
	}
	
	
	public boolean containsAmenityRepositoryToSearch(boolean searchByName){
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if(searchByName){
				if(index instanceof AmenityIndexRepositoryBinary){
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}
	
	public List<Amenity> searchAmenitiesByName(String searchQuery,
			double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, 
			double lat, double lon, ResultMatcher<Amenity> matcher) {
		List<Amenity> amenities = new ArrayList<Amenity>();
		List<AmenityIndexRepositoryBinary> list = new ArrayList<AmenityIndexRepositoryBinary>();
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if (index instanceof AmenityIndexRepositoryBinary) {
				if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
					if(index.checkContains(lat, lon)){
						list.add(0, (AmenityIndexRepositoryBinary) index);
					} else {
						list.add((AmenityIndexRepositoryBinary) index);
					}
					
				}
			}
		}
//		int left = MapUtils.get31TileNumberX(leftLongitude);
//		int top = MapUtils.get31TileNumberY(topLatitude);
//		int right = MapUtils.get31TileNumberX(rightLongitude);
//		int bottom = MapUtils.get31TileNumberY(bottomLatitude);
		int left = 0;
		int top = 0;
		int right = Integer.MAX_VALUE;
		int bottom = Integer.MAX_VALUE;
		for (AmenityIndexRepositoryBinary index : list) {
			if (matcher != null && matcher.isCancelled()) {
				break;
			}
			List<Amenity> result = index.searchAmenitiesByName(MapUtils.get31TileNumberX(lon), MapUtils.get31TileNumberY(lat),
					left, top, right, bottom,
					searchQuery, matcher);
			amenities.addAll(result);
		}

		return amenities;
	}
	
	public Map<PoiCategory, List<String>> searchAmenityCategoriesByName(String searchQuery, double lat, double lon) {
		Map<PoiCategory, List<String>> map = new LinkedHashMap<PoiCategory, List<String>>();
		for (AmenityIndexRepository index : amenityRepositories.values()) {
			if (index instanceof AmenityIndexRepositoryBinary) {
				if (index.checkContains(lat, lon)) {
					((AmenityIndexRepositoryBinary) index).searchAmenityCategoriesByName(searchQuery, map);
				}
			}
		}
		return map;
	}
	
	
	////////////////////////////////////////////// Working with address ///////////////////////////////////////////
	
	public RegionAddressRepository getRegionRepository(String name){
		return addressMap.get(name);
	}
	
	public Collection<RegionAddressRepository> getAddressRepositories(){
		return addressMap.values();
	}
	
	
	////////////////////////////////////////////// Working with transport ////////////////////////////////////////////////
	public List<TransportIndexRepository> searchTransportRepositories(double latitude, double longitude) {
		List<TransportIndexRepository> repos = new ArrayList<TransportIndexRepository>();
		for (TransportIndexRepository index : transportRepositories.values()) {
			if (index.checkContains(latitude,longitude)) {
				repos.add(index);
			}
		}
		return repos;
	}
	
	
	public void searchTransportAsync(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, List<TransportStop> toFill){
		List<TransportIndexRepository> repos = new ArrayList<TransportIndexRepository>();
		for (TransportIndexRepository index : transportRepositories.values()) {
			if (index.checkContains(topLatitude, leftLongitude, bottomLatitude, rightLongitude)) {
				if (!index.checkCachedObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude, zoom, toFill, true)) {
					repos.add(index);
				}
			}
		}
		if(!repos.isEmpty()){
//			TransportLoadRequest req = asyncLoadingThread.new TransportLoadRequest(repos, zoom);
//			req.setBoundaries(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
//			asyncLoadingThread.requestToLoadTransport(req);
		}
	}
	
	////////////////////////////////////////////// Working with map ////////////////////////////////////////////////
	public boolean isSearchAmenitiesInProgress() {
		return searchAmenitiesInProgress;
	}
	
	public MapRenderRepositories getRenderer() {
		return renderer;
	}
	
	////////////////////////////////////////////// Closing methods ////////////////////////////////////////////////
	
	public void closeFile(String fileName) {
		AmenityIndexRepository rep = amenityRepositories.remove(fileName);
		if(rep != null) {
			rep.close();
		}
		RegionAddressRepository rar = addressMap.remove(fileName);
		if(rar != null) {
			rar.close();
		}
		TransportIndexRepository tir = transportRepositories.remove(fileName);
		if(tir != null) {
			tir.close();
		}
		BinaryMapIndexReader rmp = routingMapFiles.remove(fileName);
		if(rmp != null) {
			try {
				rmp.close();
			} catch (IOException e) {
				log.error(e, e);
			}
		}
		indexFileNames.remove(fileName);
		renderer.closeConnection(fileName);
	}
	
	public void closeAmenities(){
		for(AmenityIndexRepository r : amenityRepositories.values()){
			r.close();
		}
		amenityRepositories.clear();
	}
	
	public void closeAddresses(){
		for(RegionAddressRepository r : addressMap.values()){
			r.close();
		}
		addressMap.clear();
	}
	
	public void closeTransport(){
		for(TransportIndexRepository r : transportRepositories.values()){
			r.close();
		}
		transportRepositories.clear();
	}
	
	public synchronized void close(){
		imagesOnFS.clear();
		indexFileNames.clear();
		basemapFileNames.clear();
		renderer.clearAllResources();
		closeAmenities();
		closeRouteFiles();
		closeAddresses();
		closeTransport();
	}
	
	
	public BinaryMapIndexReader[] getRoutingMapFiles() {
		return routingMapFiles.values().toArray(new BinaryMapIndexReader[routingMapFiles.size()]);
	}
	
	public void closeRouteFiles() {
		List<String> map = new ArrayList<String>(routingMapFiles.keySet());
		for(String m : map){
			try {
				BinaryMapIndexReader ind = routingMapFiles.remove(m);
				if(ind != null){
					ind.getRaf().close();
				}
			} catch(IOException e){
				log.error("Error closing resource " + m, e);
			}
		}
		
	}

	public Map<String, String> getIndexFileNames() {
		return new LinkedHashMap<String, String>(indexFileNames);
	}
	
	
	public boolean containsBasemap(){
		return !basemapFileNames.isEmpty();
	}
	
	public Map<String, String> getBackupIndexes(Map<String, String> map) {
		File file = context.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		if (file != null && file.isDirectory()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f != null && f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
						map.put(f.getName(), AndroidUtils.formatDate(f.lastModified())); //$NON-NLS-1$		
					}
				}
			}
		}
		return map;
	}
	
	public synchronized void reloadTilesFromFS(){
		imagesOnFS.clear();
	}
	
	/// On low memory method ///
	public void onLowMemory() {
		log.info("On low memory : cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		clearTiles();
		for(RegionAddressRepository r : addressMap.values()){
			r.clearCache();
		}
		renderer.clearCache();
		
		System.gc();
	}
	
	public GeoidAltitudeCorrection getGeoidAltitudeCorrection() {
		return geoidAltitudeCorrection;
	}

//	public OsmandRegions getOsmandRegions() {
//		return context.getRegions();
//	}
//	
	
	protected synchronized void clearTiles() {
		log.info("Cleaning tiles - size = " + cacheOfImages.size()); //$NON-NLS-1$
		ArrayList<String> list = new ArrayList<String>(cacheOfImages.keySet());
		// remove first images (as we think they are older)
		for (int i = 0; i < list.size() / 2; i++) {
			cacheOfImages.remove(list.get(i));
		}
	}
	
	public IncrementalChangesManager getChangesManager() {
		return changesManager;
	}

	public OsmWindow getContext() {
		return context;
	}
}
