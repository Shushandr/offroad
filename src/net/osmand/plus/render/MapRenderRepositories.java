package net.osmand.plus.render;


import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.api.SettingsAPI.SettingsEditor;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRuleStorageProperties;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class MapRenderRepositories {

	// It is needed to not draw object twice if user have map index that intersects by boundaries
	public static boolean checkForDuplicateObjectIds = true;
	
	private final static Log log = PlatformUtil.getLog(MapRenderRepositories.class);
	private final static int zoomOnlyForBasemaps = 11;

	static int zoomForBaseRouteRendering  = 14;
	private Map<String, BinaryMapIndexReader> files = new LinkedHashMap<String, BinaryMapIndexReader>();
	private Set<String> nativeFiles = new HashSet<String>();
	private OsmandRenderer renderer;
	


	// lat/lon box of requested vector data
	private QuadRect cObjectsBox = new QuadRect();
	private int cObjectsZoom = 0;
	// cached objects in order to render rotation without reloading data from db
	private List<BinaryMapDataObject> cObjects = new LinkedList<BinaryMapDataObject>();

	// currently rendered box (not the same as already rendered)
	// this box is checked for interrupted process or
	private RotatedTileBox requestedBox = null;

	// location of rendered bitmap
	private RotatedTileBox prevBmpLocation = null;
	// to track necessity of map download (1 (if basemap) + 2 (if normal map) 
	private int checkedRenderedState;
	private RotatedTileBox checkedBox;

	// location of rendered bitmap
	private RotatedTileBox bmpLocation = null;
	// Field used in C++
	private boolean interrupted = false;
	private int renderedState = 0; 	// (1 (if basemap) + 2 (if normal map)
	private RenderingContext currentRenderingContext;
	private SearchRequest<BinaryMapDataObject> searchRequest;

	private OsmandSettings prefs = new OsmandSettings(new SettingsAPI() {
		
		@Override
		public String getString(Object pPref, String pKey, String pDefValue) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Object getPreferenceObject(String pKey) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public long getLong(Object pPref, String pKey, long pDefValue) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public int getInt(Object pPref, String pKey, int pDefValue) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public float getFloat(Object pPref, String pKey, float pDefValue) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public boolean getBoolean(Object pPref, String pKey, boolean pDefValue) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public SettingsEditor edit(Object pPref) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public boolean contains(Object pPref, String pKey) {
			// TODO Auto-generated method stub
			return false;
		}
	});

	public MapRenderRepositories() {
		this.renderer = new OsmandRenderer();
	}

	public OsmandRenderer getRenderer() {
		return renderer;
	}

	public void initializeNewResource(final IProgress progress, File file, BinaryMapIndexReader reader) {
		if (files.containsKey(file.getName())) {
			closeConnection(file.getName());
		
		}
		LinkedHashMap<String, BinaryMapIndexReader> cpfiles = new LinkedHashMap<String, BinaryMapIndexReader>(files);
		cpfiles.put(file.getName(), reader);
		files = cpfiles;
	}

	public RotatedTileBox getBitmapLocation() {
		return bmpLocation;
	}

	public RotatedTileBox getPrevBmpLocation() {
		return prevBmpLocation;
	}

	public synchronized void closeConnection(String file) {
		LinkedHashMap<String, BinaryMapIndexReader> cpfiles = new LinkedHashMap<String, BinaryMapIndexReader>(files);
		BinaryMapIndexReader bmir = cpfiles.remove(file);
		files = cpfiles;
		if (bmir != null) {
			try {
				bmir.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean containsLatLonMapData(double lat, double lon, int zoom) {
		int x = MapUtils.get31TileNumberX(lon);
		int y = MapUtils.get31TileNumberY(lat);
		for (BinaryMapIndexReader reader : files.values()) {
			if (reader.containsMapData(x, y, zoom)) {
				return true;
			}
		}
		return false;
	}

	public void clearAllResources() {
		clearCache();
		bmpLocation = null;
		for (String f : new ArrayList<String>(files.keySet())) {
			closeConnection(f);
		}
	}

	public boolean updateMapIsNeeded(RotatedTileBox box, DrawSettings drawSettings) {
		if (box == null) {
			return false;
		}
		if (requestedBox == null) {
			log.info("RENDER MAP: update due to start");
			return true;
		}
		if (drawSettings.isUpdateVectorRendering()) {
			log.info("RENDER MAP: update due to request");
			return true;
		}
		if (requestedBox.getZoom() != box.getZoom() ||
				requestedBox.getMapDensity() != box.getMapDensity()) {
			log.info("RENDER MAP: update due zoom/map density");
			return true;
		}

		float deltaRotate = requestedBox.getRotate() - box.getRotate();
		if (deltaRotate > 180) {
			deltaRotate -= 360;
		} else if (deltaRotate < -180) {
			deltaRotate += 360;
		}
		if (Math.abs(deltaRotate) > 25) {
			log.info("RENDER MAP: update due to rotation");
			return true;
		}
		boolean upd = !requestedBox.containsTileBox(box);
		if(upd) {
			log.info("RENDER MAP: update due to tile box");
		}
		return upd;
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public void interruptLoadingMap() {
		interrupted = true;
		if (currentRenderingContext != null) {
			currentRenderingContext.interrupted = true;
		}
		if (searchRequest != null) {
			searchRequest.setInterrupted(true);
		}
		log.info("RENDER MAP: Interrupt rendering map");
	}
	
	public boolean wasInterrupted() {
		return interrupted;
	}

	private boolean checkWhetherInterrupted() {
		if (interrupted || (currentRenderingContext != null && currentRenderingContext.interrupted)) {
			requestedBox = bmpLocation;
			return true;
		}
		return false;
	}

	public boolean basemapExists() {
		for (BinaryMapIndexReader f : files.values()) {
			if (f.isBasemap()) {
				return true;
			}
		}
		return false;
	}
	
	
	private boolean loadVectorDataNative(QuadRect dataBox, final int zoom, final RenderingRuleSearchRequest renderingReq) {
		int leftX = MapUtils.get31TileNumberX(dataBox.left);
		int rightX = MapUtils.get31TileNumberX(dataBox.right);
		int bottomY = MapUtils.get31TileNumberY(dataBox.bottom);
		int topY = MapUtils.get31TileNumberY(dataBox.top);
		long now = System.currentTimeMillis();

		// check that everything is initialized
		if (checkWhetherInterrupted()) {
			return false;
		}
		cObjectsBox = dataBox;
		cObjectsZoom = zoom;
		log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
				dataBox.bottom, dataBox.top, dataBox.left, dataBox.right, zoom));
		log.info(String.format("Native search: %s ms ", System.currentTimeMillis() - now)); //$NON-NLS-1$
		return true;
	}

	
	private void readRouteDataAsMapObjects(SearchRequest<BinaryMapDataObject> sr, BinaryMapIndexReader c, 
			final ArrayList<BinaryMapDataObject> tempResult, final TLongSet ids) {
		final boolean basemap = c.isBasemap();
		try {
			for (RouteRegion reg : c.getRoutingIndexes()) {
				List<RouteSubregion> parent = sr.getZoom() < 15 ? reg.getBaseSubregions() : reg.getSubregions();
				List<RouteSubregion> searchRouteIndexTree = c.searchRouteIndexTree(sr, parent);
				final MapIndex nmi = new MapIndex();
				c.loadRouteIndexData(searchRouteIndexTree, new ResultMatcher<RouteDataObject>() {

					@Override
					public boolean publish(RouteDataObject r) {
						if (basemap) {
							renderedState |= 1;
						} else {
							renderedState |= 2;
						}
						if (checkForDuplicateObjectIds && !basemap) {
							if (ids.contains(r.getId()) && r.getId() > 0) {
								// do not add object twice
								return false;
							}
							ids.add(r.getId());
						}
						int[] coordinantes = new int[r.getPointsLength() * 2];
						int[] roTypes = r.getTypes();
						for(int k = 0; k < roTypes.length; k++) {
							int type = roTypes[k];
							registerMissingType(nmi, r, type);
						}
						for(int k = 0; k < coordinantes.length/2; k++ ) {
							coordinantes[2 * k] = r.getPoint31XTile(k);
							coordinantes[2 * k + 1] = r.getPoint31YTile(k);
						}
						BinaryMapDataObject mo = new BinaryMapDataObject(coordinantes, roTypes, new int[0][], r.getId());
						TIntObjectHashMap<String> names = r.getNames();
						if(names != null) {
							TIntObjectIterator<String> it = names.iterator();
							while(it.hasNext()) {
								it.advance();
								registerMissingType(nmi, r, it.key());
								mo.putObjectName(it.key(), it.value());
							}
						}
						mo.setMapIndex(nmi);
						tempResult.add(mo);
						return false;
					}

					private void registerMissingType(final MapIndex nmi, RouteDataObject r, int type) {
						if (!nmi.isRegisteredRule(type)) {
							RouteTypeRule rr = r.region.quickGetEncodingRule(type);
							String tag = rr.getTag();
							int additional = ("highway".equals(tag) || "route".equals(tag) || "railway".equals(tag)
									|| "aeroway".equals(tag) || "aerialway".equals(tag)) ? 0 : 1;
							nmi.initMapEncodingRule(additional, type, rr.getTag(), rr.getValue());
						}
					}

					@Override
					public boolean isCancelled() {
						return !interrupted;
					}
				});
			}
		} catch (IOException e) {
			log.debug("Search failed " + c.getRegionNames(), e); //$NON-NLS-1$
		}
	}


	private boolean loadVectorData(QuadRect dataBox, final int zoom, final RenderingRuleSearchRequest renderingReq) {
		double cBottomLatitude = dataBox.bottom;
		double cTopLatitude = dataBox.top;
		double cLeftLongitude = dataBox.left;
		double cRightLongitude = dataBox.right;

		long now = System.currentTimeMillis();

		System.gc(); // to clear previous objects
		ArrayList<BinaryMapDataObject> tempResult = new ArrayList<BinaryMapDataObject>();
		ArrayList<BinaryMapDataObject> basemapResult = new ArrayList<BinaryMapDataObject>();
		
		int[] count = new int[]{0};
		boolean[] ocean = new boolean[]{false};
		boolean[] land = new boolean[]{false};
		List<BinaryMapDataObject> coastLines = new ArrayList<BinaryMapDataObject>();
		List<BinaryMapDataObject> basemapCoastLines = new ArrayList<BinaryMapDataObject>();
		int leftX = MapUtils.get31TileNumberX(cLeftLongitude);
		int rightX = MapUtils.get31TileNumberX(cRightLongitude);
		int bottomY = MapUtils.get31TileNumberY(cBottomLatitude);
		int topY = MapUtils.get31TileNumberY(cTopLatitude);
		TLongSet ids = new TLongHashSet();
		MapIndex mi = readMapObjectsForRendering(zoom, renderingReq, tempResult, basemapResult, ids, count, ocean,
				land, coastLines, basemapCoastLines, leftX, rightX, bottomY, topY);
		int renderRouteDataFile = 0;
		if (renderingReq.searchRenderingAttribute("showRoadMapsAttribute")) {
			renderRouteDataFile = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_INT_VALUE);
		}
		if (checkWhetherInterrupted()) {
			return false;
		}
		boolean objectsFromMapSectionRead = tempResult.size() > 0;
		if (renderRouteDataFile >= 0 && zoom >= zoomOnlyForBasemaps ) {
			searchRequest = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom, null);
			for (BinaryMapIndexReader c : files.values()) {
				// false positive case when we have 2 sep maps Country-roads & Country
				if(c.getMapIndexes().size() == 0 || renderRouteDataFile == 1) {
					readRouteDataAsMapObjects(searchRequest, c, tempResult, ids);
				}
			}
			log.info(String.format("Route objects %s", tempResult.size() +""));
		}

		String coastlineTime = "";
		boolean addBasemapCoastlines = true;
		boolean emptyData = zoom > zoomOnlyForBasemaps && tempResult.isEmpty() && coastLines.isEmpty();
		boolean basemapMissing = zoom <= zoomOnlyForBasemaps && basemapCoastLines.isEmpty() && mi == null;
		boolean detailedLandData = zoom >= zoomForBaseRouteRendering && tempResult.size() > 0  && objectsFromMapSectionRead;
		if (!coastLines.isEmpty()) {
			long ms = System.currentTimeMillis();
			boolean coastlinesWereAdded = processCoastlines(coastLines, leftX, rightX, bottomY, topY, zoom,
					basemapCoastLines.isEmpty(), true, tempResult);
			addBasemapCoastlines = (!coastlinesWereAdded && !detailedLandData) || zoom <= zoomOnlyForBasemaps;
			coastlineTime = "(coastline " + (System.currentTimeMillis() - ms) + " ms )";
		} else {
			addBasemapCoastlines = !detailedLandData;
		}
		if (addBasemapCoastlines) {
			long ms = System.currentTimeMillis();
			boolean coastlinesWereAdded = processCoastlines(basemapCoastLines, leftX, rightX, bottomY, topY, zoom,
					true, true, tempResult);
			addBasemapCoastlines = !coastlinesWereAdded;
			coastlineTime = "(coastline " + (System.currentTimeMillis() - ms) + " ms )";
		}
		if (addBasemapCoastlines && mi != null) {
			BinaryMapDataObject o = new BinaryMapDataObject(new int[]{leftX, topY, rightX, topY, rightX, bottomY, leftX, bottomY, leftX,
					topY}, new int[]{ocean[0] && !land[0] ? mi.coastlineEncodingType : (mi.landEncodingType)}, null, -1);
			o.setMapIndex(mi);
			tempResult.add(o);
		}
		if (emptyData || basemapMissing) {
			// message
			MapIndex mapIndex;
			if (!tempResult.isEmpty()) {
				mapIndex = tempResult.get(0).getMapIndex();
			} else {
				mapIndex = new MapIndex();
				mapIndex.initMapEncodingRule(0, 1, "natural", "coastline");
				mapIndex.initMapEncodingRule(0, 2, "name", "");
			}
		}
		if (zoom <= zoomOnlyForBasemaps || emptyData) {
			tempResult.addAll(basemapResult);
		}


		if (count[0] > 0) {
			log.info(String.format("BLat=%s, TLat=%s, LLong=%s, RLong=%s, zoom=%s", //$NON-NLS-1$
					cBottomLatitude, cTopLatitude, cLeftLongitude, cRightLongitude, zoom));
			log.info(String.format("Searching: %s ms  %s (%s results found)", System.currentTimeMillis() - now, coastlineTime, count[0])); //$NON-NLS-1$
		}


		cObjects = tempResult;
		cObjectsBox = dataBox;
		cObjectsZoom = zoom;

		return true;
	}

	

	private MapIndex readMapObjectsForRendering(final int zoom, final RenderingRuleSearchRequest renderingReq,
			ArrayList<BinaryMapDataObject> tempResult, ArrayList<BinaryMapDataObject> basemapResult, 
			TLongSet ids, int[] count, boolean[] ocean, boolean[] land, List<BinaryMapDataObject> coastLines,
			List<BinaryMapDataObject> basemapCoastLines, int leftX, int rightX, int bottomY, int topY) {
		BinaryMapIndexReader.SearchFilter searchFilter = new BinaryMapIndexReader.SearchFilter() {
			@Override
			public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex root) {
				for (int j = 0; j < types.size(); j++) {
					int type = types.get(j);
					TagValuePair pair = root.decodeType(type);
					if (pair != null) {
						// TODO is it fast enough ?
						for (int i = 1; i <= 3; i++) {
							renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, zoom);
							renderingReq.setStringFilter(renderingReq.ALL.R_TAG, pair.tag);
							renderingReq.setStringFilter(renderingReq.ALL.R_VALUE, pair.value);
							if (renderingReq.search(i, false)) {
								return true;
							}
						}
						renderingReq.setStringFilter(renderingReq.ALL.R_TAG, pair.tag);
						renderingReq.setStringFilter(renderingReq.ALL.R_VALUE, pair.value);
						if (renderingReq.search(RenderingRulesStorage.TEXT_RULES, false)) {
							return true;
						}
					}
				}
				return false;
			}

		};
		if (zoom > 16) {
			searchFilter = null;
		}
		MapIndex mi = null;
		searchRequest = BinaryMapIndexReader.buildSearchRequest(leftX, rightX, topY, bottomY, zoom, searchFilter);
		for (BinaryMapIndexReader c : files.values()) {
			boolean basemap = c.isBasemap();
			searchRequest.clearSearchResults();
			List<BinaryMapDataObject> res;
			try {
				res = c.searchMapIndex(searchRequest);
			} catch (IOException e) {
				res = new ArrayList<BinaryMapDataObject>();
				log.debug("Search failed " + c.getRegionNames(), e); //$NON-NLS-1$
			}
			if(res.size() > 0) {
				if(basemap) {
					renderedState |= 1;
				} else {
					renderedState |= 2;
				}
			}
			for (BinaryMapDataObject r : res) {
				if (checkForDuplicateObjectIds && !basemap) {
					if (ids.contains(r.getId()) && r.getId() > 0) {
						// do not add object twice
						continue;
					}
					ids.add(r.getId());
				}
				count[0]++;

				if (r.containsType(r.getMapIndex().coastlineEncodingType)) {
					if (basemap) {
						basemapCoastLines.add(r);
					} else {
						coastLines.add(r);
					}
				} else {
					// do not mess coastline and other types
					if (basemap) {
						basemapResult.add(r);
					} else {
						tempResult.add(r);
					}
				}
				if (checkWhetherInterrupted()) {
					return null;
				}
			}

			if (searchRequest.isOcean()) {
				mi = c.getMapIndexes().get(0);
				ocean[0] = true;
			}
			if (searchRequest.isLand()) {
				mi = c.getMapIndexes().get(0);
				land[0] = true;
			}
		}
		return mi;
	}

	private void validateLatLonBox(QuadRect box) {
		if (box.top > 90) {
			box.top = 85.5f;
		}
		if (box.bottom < -90) {
			box.bottom = -85.5f;
		}
		if (box.left <= -180) {
			box.left = -179.5f;
		}
		if (box.right > 180) {
			box.right = 180.0f;
		}
	}

	public RotatedTileBox getCheckedBox() {
		return checkedBox;
	}
	
	public int getCheckedRenderedState() {
		// to track necessity of map download (1 (if basemap) + 2 (if normal map)
		return checkedRenderedState;
	}

	

	public synchronized void loadMGap(Graphics2D pGraphics2d, RotatedTileBox tileRect, RenderingRulesStorage storage) {
		boolean prevInterrupted = interrupted;
		interrupted = false;
		// prevent editing
		requestedBox = new RotatedTileBox(tileRect);
		log.info("RENDER MAP: new request " + tileRect ); 
		if (currentRenderingContext != null) {
			currentRenderingContext = null;
		}
		try {
			// find selected rendering type
			boolean nightMode = false;
			// boolean moreDetail = prefs.SHOW_MORE_MAP_DETAIL.get();
			RenderingRuleSearchRequest renderingReq = new RenderingRuleSearchRequest(storage);
			renderingReq.setBooleanFilter(renderingReq.ALL.R_NIGHT_MODE, nightMode);
			for (RenderingRuleProperty customProp : storage.PROPS.getCustomRules()) {
				if (customProp.isBoolean()) {
					if(customProp.getAttrName().equals(RenderingRuleStorageProperties.A_ENGINE_V1)) {
						renderingReq.setBooleanFilter(customProp, true);
					} else if (RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN.equals(customProp.getCategory())) {
						renderingReq.setBooleanFilter(customProp, false);
					} else {
						CommonPreference<Boolean> pref = prefs.getCustomRenderBooleanProperty(customProp.getAttrName());
						renderingReq.setBooleanFilter(customProp, pref.get());
					}
				} else if (RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN.equals(customProp.getCategory())) {
					if (customProp.isString()) {
						renderingReq.setStringFilter(customProp, "");
					} else {
						renderingReq.setIntFilter(customProp, 0);
					}
				} else {
					CommonPreference<String> settings = prefs.getCustomRenderProperty(customProp.getAttrName());
					String res = settings.get();
					if (!Algorithms.isEmpty(res)) {
						if (customProp.isString()) {
							renderingReq.setStringFilter(customProp, res);
						} else {
							try {
								renderingReq.setIntFilter(customProp, Integer.parseInt(res));
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					} else {
						if (customProp.isString()) {
							renderingReq.setStringFilter(customProp, "");
						}
					}
				}
			}
			renderingReq.saveState();


			// calculate data box
			QuadRect dataBox = requestedBox.getLatLonBounds();
			int dataBoxZoom = requestedBox.getZoom();
			long now = System.currentTimeMillis();
			if (cObjectsBox.left > dataBox.left || cObjectsBox.top < dataBox.top || cObjectsBox.right < dataBox.right
					|| cObjectsBox.bottom > dataBox.bottom 
					|| dataBoxZoom != cObjectsZoom || prevInterrupted) {
				// increase data box in order for rotate
				if ((dataBox.right - dataBox.left) > (dataBox.top - dataBox.bottom)) {
					double wi = (dataBox.right - dataBox.left) * .05;
					dataBox.left -= wi;
					dataBox.right += wi;
				} else {
					double hi = (dataBox.top - dataBox.bottom) * .05;
					dataBox.top += hi;
					dataBox.bottom -= hi;
				}
				validateLatLonBox(dataBox);
				renderedState = 0;
				boolean loaded;
				loaded = loadVectorData(dataBox, requestedBox.getZoom(), renderingReq);
					
				if (!loaded || checkWhetherInterrupted()) {
					return;
				}
			}
			final long searchTime = System.currentTimeMillis() - now;

			currentRenderingContext = new OsmandRenderer.RenderingContext();
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_DEFAULT_COLOR)) {
				currentRenderingContext.defaultColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_COLOR_VALUE);
			}
			renderingReq.clearState();
			renderingReq.setIntFilter(renderingReq.ALL.R_MINZOOM, requestedBox.getZoom());
			if(renderingReq.searchRenderingAttribute(RenderingRuleStorageProperties.A_SHADOW_RENDERING)) {
				currentRenderingContext.shadowRenderingMode = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_INT_VALUE);
				currentRenderingContext.shadowRenderingColor = renderingReq.getIntPropertyValue(renderingReq.ALL.R_SHADOW_COLOR);
			}
			if(renderingReq.searchRenderingAttribute("polygonMinSizeToDisplay")) {
				currentRenderingContext.polygonMinSizeToDisplay = renderingReq.getIntPropertyValue(renderingReq.ALL.R_ATTR_INT_VALUE);
			}
			final QuadPointDouble lt = requestedBox.getLeftTopTile(requestedBox.getZoom());
			double cfd = MapUtils.getPowZoom(requestedBox.getZoomFloatPart())* requestedBox.getMapDensity();
			lt.x *= cfd;
			lt.y *= cfd;
//			LatLon ltn = requestedBox.getLeftTopLatLon();
			final double tileDivisor = MapUtils.getPowZoom(31 - requestedBox.getZoom()) / cfd;
			
			currentRenderingContext.leftX = lt.x;
			currentRenderingContext.topY = lt.y;
			currentRenderingContext.zoom = requestedBox.getZoom();
			currentRenderingContext.rotate = requestedBox.getRotate();
			currentRenderingContext.width = requestedBox.getPixWidth();
			currentRenderingContext.height = requestedBox.getPixHeight();
			currentRenderingContext.nightMode = nightMode;
			if(requestedBox.getZoom() <= zoomOnlyForBasemaps && 
					"".equals(prefs.MAP_PREFERRED_LOCALE.get())) {
				currentRenderingContext.preferredLocale = prefs.MAP_PREFERRED_LOCALE.get();
			}
			final float mapDensity = (float) requestedBox.getMapDensity();
			currentRenderingContext.setDensityValue(mapDensity);
			//Text/icon scales according to mapDensity (so text is size of road)
//			currentRenderingContext.textScale = (requestedBox.getDensity()*app.getSettings().TEXT_SCALE.get()); 
			//Text/icon stays same for all sizes 
			currentRenderingContext.textScale = (requestedBox.getDensity() * prefs.TEXT_SCALE.get())
					/ mapDensity;
			
			currentRenderingContext.screenDensityRatio = 1 / Math.max(1, requestedBox.getDensity()) ;
			// init rendering context
			currentRenderingContext.tileDivisor = tileDivisor;
			if (checkWhetherInterrupted()) {
				return;
			}

			now = System.currentTimeMillis();
			boolean transparent = false;
			RenderingRuleProperty rr = storage.PROPS.get("noPolygons");
			if (rr != null) {
				transparent = renderingReq.getIntPropertyValue(rr) > 0;
			}

			// 1. generate image step by step
			this.prevBmpLocation = this.bmpLocation;
			this.bmpLocation = tileRect;
			renderer.generateNewBitmap(currentRenderingContext, cObjects, pGraphics2d, renderingReq);
			// Force to use rendering request in order to prevent Garbage Collector when it is used in C++
			if(renderingReq != null){
				log.info("Debug :" + renderingReq != null);				
			}
			String renderingDebugInfo = currentRenderingContext.renderingDebugInfo;
			currentRenderingContext.ended = true;
			if (checkWhetherInterrupted()) {
				// revert if it was interrupted 
				// (be smart a bit do not revert if road already drawn) 
				if(currentRenderingContext.lastRenderedKey < 35) {
					this.bmpLocation = this.prevBmpLocation;
					this.prevBmpLocation = null;
				}
				currentRenderingContext = null;
				return;
			} else {
				this.checkedRenderedState = renderedState;
				this.checkedBox = this.bmpLocation;
			}
			currentRenderingContext = null;

			// 2. replace whole image
			// keep cache
			// this.prevBmp = null;
			this.prevBmpLocation = null;
		} catch (RuntimeException e) {
			log.error("Runtime memory exception", e); //$NON-NLS-1$
		} catch (OutOfMemoryError e) {
			log.error("Out of memory error", e); //$NON-NLS-1$
			cObjects = new ArrayList<BinaryMapDataObject>();
			cObjectsBox = new QuadRect();
		} finally {
			if(currentRenderingContext != null) {
				currentRenderingContext.ended = true;
			}
		}

	}

	public synchronized void clearCache() {
		cObjects = new ArrayList<BinaryMapDataObject>();
		cObjectsBox = new QuadRect();

		requestedBox = prevBmpLocation = null;
		// Do not clear main bitmap to not cause a screen refresh
//		prevBmp = null;
//		bmp = null;
//		bmpLocation = null;
	}
	
	public Map<String, BinaryMapIndexReader> getMetaInfoFiles() {
		return files;
	}

	/// MULTI POLYGONS (coastline)
	// returns true if coastlines were added!
	private boolean processCoastlines(List<BinaryMapDataObject> coastLines, int leftX, int rightX, 
			int bottomY, int topY, int zoom, boolean doNotAddIfIncompleted, boolean addDebugIncompleted, List<BinaryMapDataObject> result) {
		List<TLongList> completedRings = new ArrayList<TLongList>();
		List<TLongList> uncompletedRings = new ArrayList<TLongList>();
		MapIndex mapIndex = null;
		long dbId = 0;
		for (BinaryMapDataObject o : coastLines) {
			int len = o.getPointsLength();
			if (len < 2) {
				continue;
			}
			mapIndex = o.getMapIndex();
			dbId = o.getId() >> 1;
			TLongList coordinates = new TLongArrayList(o.getPointsLength() / 2);
			int px = o.getPoint31XTile(0);
			int py = o.getPoint31YTile(0);
			int x = px;
			int y = py;
			boolean pinside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
			if (pinside) {
				coordinates.add(combine2Points(x, y));
			}
			for (int i = 1; i < len; i++) {
				x = o.getPoint31XTile(i);
				y = o.getPoint31YTile(i);
				boolean inside = leftX <= x && x <= rightX && y >= topY && y <= bottomY;
				boolean lineEnded = calculateLineCoordinates(inside, x, y, pinside, px, py, leftX, rightX, bottomY, topY, coordinates);
				if (lineEnded) {
					combineMultipolygonLine(completedRings, uncompletedRings, coordinates);
					// create new line if it goes outside
					coordinates = new TLongArrayList();
				}
				px = x;
				py = y;
				pinside = inside;
			}
			combineMultipolygonLine(completedRings, uncompletedRings, coordinates);
		}
		if (completedRings.size() == 0 && uncompletedRings.size() == 0) {
			return false;
		}
		if (uncompletedRings.size() > 0) {
			unifyIncompletedRings(uncompletedRings, completedRings, leftX, rightX, bottomY, topY, dbId, zoom);
		}
		long mask = 0xffffffffL;
		// draw uncompleted for debug purpose
		for (int i = 0; i < uncompletedRings.size(); i++) {
			TLongList ring = uncompletedRings.get(i);
			int[] coordinates = new int[ring.size() * 2];
			for (int j = 0; j < ring.size(); j++) {
				coordinates[j * 2] = (int) (ring.get(j) >> 32);
				coordinates[j * 2 + 1] = (int) (ring.get(j) & mask);
			}
			BinaryMapDataObject o = new BinaryMapDataObject(coordinates, new int[] { mapIndex.coastlineBrokenEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			result.add(o);
		}
		if(!doNotAddIfIncompleted && uncompletedRings.size() > 0){
			return false;
		}
		boolean clockwiseFound = false;
		
		for (int i = 0; i < completedRings.size(); i++) {
			TLongList ring = completedRings.get(i);
			int[] coordinates = new int[ring.size() * 2];
			for (int j = 0; j < ring.size(); j++) {
				coordinates[j * 2] = (int) (ring.get(j) >> 32);
				coordinates[j * 2 + 1] = (int) (ring.get(j) & mask);
			}
			boolean clockwise = MapAlgorithms.isClockwiseWay(ring);
			clockwiseFound = clockwiseFound || clockwise;
			BinaryMapDataObject o = new BinaryMapDataObject(coordinates, new int[] { clockwise ? mapIndex.coastlineEncodingType
					: mapIndex.landEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			o.setArea(true);
			result.add(o);
		}
		
		if (!clockwiseFound && uncompletedRings.size() == 0) {
			// add complete water tile
			BinaryMapDataObject o = new BinaryMapDataObject(new int[] { leftX, topY, rightX, topY, rightX, bottomY, leftX, bottomY, leftX,
					topY }, new int[] { mapIndex.coastlineEncodingType }, null, dbId);
			o.setMapIndex(mapIndex);
			log.info("!!! Isolated islands !!!"); //$NON-NLS-1$
			result.add(o);

		}
		return true;
	}
	
	private boolean eq(long i1, long i2){
		return i1 == i2;
	}
	
	private void combineMultipolygonLine(List<TLongList> completedRings, List<TLongList> incompletedRings,	TLongList coordinates) {
		if (coordinates.size() > 0) {
			if (eq(coordinates.get(0), coordinates.get(coordinates.size() - 1))) {
				completedRings.add(coordinates);
			} else {
				boolean add = true;
				for (int k = 0; k < incompletedRings.size();) {
					boolean remove = false;
					TLongList i = incompletedRings.get(k);
					if (eq(coordinates.get(0), i.get(i.size() - 1))) {
						i.addAll(coordinates.subList(1, coordinates.size()));
						remove = true;
						coordinates = i;
					} else if (eq(coordinates.get(coordinates.size() - 1), i.get(0))) {
						coordinates.addAll(i.subList(1, i.size()));
						remove = true;
					}
					if (remove) {
						incompletedRings.remove(k);
					} else {
						k++;
					}
					if (eq(coordinates.get(0), coordinates.get(coordinates.size() - 1))) {
						completedRings.add(coordinates);
						add = false;
						break;
					}
				}
				if (add) {
					incompletedRings.add(coordinates);
				}
			}
		}
	}

	private void unifyIncompletedRings(List<TLongList> toProcces, List<TLongList> completedRings, int leftX, int rightX, int bottomY, int topY, long dbId, int zoom) {
		int mask = 0xffffffff;
		List<TLongList> uncompletedRings = new ArrayList<TLongList>(toProcces);
		toProcces.clear();
		Set<Integer> nonvisitedRings = new LinkedHashSet<Integer>();
		for (int j = 0; j < uncompletedRings.size(); j++) {
			TLongList i = uncompletedRings.get(j);
			int x = (int) (i.get(i.size() - 1) >> 32);
			int y = (int) (i.get(i.size() - 1) & mask);
			int sx = (int) (i.get(0) >> 32);
			int sy = (int) (i.get(0) & mask);
			boolean st = y == topY || x == rightX || y == bottomY || x == leftX;
			boolean end = sy == topY || sx == rightX || sy == bottomY || sx == leftX;
			// something goes wrong
			// These exceptions are used to check logic about processing multipolygons
			// However this situation could happen because of broken multipolygons (so it should data causes app error)
			// that's why these exceptions could be replaced with return; statement.
			if (!end || !st) {
				float dx = (float) MapUtils.get31LongitudeX(x);
				float dsx = (float) MapUtils.get31LongitudeX(sx);
				float dy = (float) MapUtils.get31LatitudeY(y);
				float dsy = (float) MapUtils.get31LatitudeY(sy);
				String str;
				if (!end) {
					str = " Starting point (to close) not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				if (!st) {
					str = " End not found : end_x = {0}, end_y = {1}, start_x = {2}, start_y = {3} : bounds {4} {5} - {6} {7}"; //$NON-NLS-1$
					System.err
							.println(MessageFormat.format(dbId + str, dx, dy, dsx, dsy, leftX + "", topY + "", rightX + "", bottomY + "")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				}
				toProcces.add(i);
			} else {
				nonvisitedRings.add(j);
			}
		}
		for (int j = 0; j < uncompletedRings.size(); j++) {
			TLongList i = uncompletedRings.get(j);
			if (!nonvisitedRings.contains(j)) {
				continue;
			}

			int x = (int) (i.get(i.size() - 1) >> 32);
			int y = (int) (i.get(i.size() - 1) & mask);
			// 31 - (zoom + 8)
			int EVAL_DELTA = 6 << (23 - zoom);
			int UNDEFINED_MIN_DIFF = -1 - EVAL_DELTA;
			while (true) {
				int st = 0; // st already checked to be one of the four
				if (y == topY) {
					st = 0;
				} else if (x == rightX) {
					st = 1;
				} else if (y == bottomY) {
					st = 2;
				} else if (x == leftX) {
					st = 3;
				}
				int nextRingIndex = -1;
				// BEGIN go clockwise around rectangle
				for (int h = st; h < st + 4; h++) {

					// BEGIN find closest nonvisited start (including current)
					int mindiff = UNDEFINED_MIN_DIFF;
					for (Integer ni : nonvisitedRings) {
						TLongList cni = uncompletedRings.get(ni);
						int csx = (int) (cni.get(0) >> 32);
						int csy = (int) (cni.get(0) & mask);
						if (h % 4 == 0) {
							// top
							if (csy == topY && csx >= safelyAddDelta(x, -EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csx - x) <= mindiff) {
									mindiff = (csx - x);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 1) {
							// right
							if (csx == rightX && csy >= safelyAddDelta(y, -EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (csy - y) <= mindiff) {
									mindiff = (csy - y);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 2) {
							// bottom
							if (csy == bottomY && csx <= safelyAddDelta(x, EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (x - csx) <= mindiff) {
									mindiff = (x - csx);
									nextRingIndex = ni;
								}
							}
						} else if (h % 4 == 3) {
							// left
							if (csx == leftX && csy <= safelyAddDelta(y, EVAL_DELTA)) {
								if (mindiff == UNDEFINED_MIN_DIFF || (y - csy) <= mindiff) {
									mindiff = (y - csy);
									nextRingIndex = ni;
								}
							}
						}
					} // END find closest start (including current)

					// we found start point
					if (mindiff != UNDEFINED_MIN_DIFF) {
						break;
					} else {
						if (h % 4 == 0) {
							// top
							y = topY;
							x = rightX;
						} else if (h % 4 == 1) {
							// right
							y = bottomY;
							x = rightX;
						} else if (h % 4 == 2) {
							// bottom
							y = bottomY;
							x = leftX;
						} else if (h % 4 == 3) {
							y = topY;
							x = leftX;
						}
						i.add((((long) x) << 32) | ((long) y));
					}

				} // END go clockwise around rectangle
				if (nextRingIndex == -1) {
					// it is impossible (current start should always be found)
				} else if (nextRingIndex == j) {
					i.add(i.get(0));
					nonvisitedRings.remove(j);
					break;
				} else {
					i.addAll(uncompletedRings.get(nextRingIndex));
					nonvisitedRings.remove(nextRingIndex);
					// get last point and start again going clockwise
					x = (int) (i.get(i.size() - 1) >> 32);
					y = (int) (i.get(i.size() - 1) & mask);
				}
			}

			completedRings.add(i);
		}
	}

	private int safelyAddDelta(int number, int delta) {
		int res = number + delta;
		if (delta > 0 && res < number) {
			return Integer.MAX_VALUE;
		} else if (delta < 0 && res > number) {
			return Integer.MIN_VALUE;
		}
		return res;
	}
	
	private static long combine2Points(int x, int y) {
		return (((long) x ) <<32) | ((long)y );
	}
	
	private boolean calculateLineCoordinates(boolean inside, int x, int y, boolean pinside, int px, int py, int leftX, int rightX,
			int bottomY, int topY, TLongList coordinates) {
		boolean lineEnded = false;
		if (pinside) {
			if (!inside) {
				long is = MapAlgorithms.calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
				if (is == -1) {
					// it is an error (!)
					is = combine2Points(px, py);
				}
				coordinates.add(is);
				lineEnded = true;
			} else {
				coordinates.add(combine2Points(x, y));
			}
		} else {
			long is = MapAlgorithms.calculateIntersection(x, y, px, py, leftX, rightX, bottomY, topY);
			if (inside) {
				// assert is != -1;
				coordinates.add(is);
				coordinates.add(combine2Points(x, y));
			} else if (is != -1) {
				int bx = (int) (is >> 32);
				int by = (int) (is & 0xffffffff);
				coordinates.add(is);
				is = MapAlgorithms.calculateIntersection(x, y, bx, by, leftX, rightX, bottomY, topY);
				coordinates.add(is);
				lineEnded = true;
			}
		}

		return lineEnded;
	}

	


}
