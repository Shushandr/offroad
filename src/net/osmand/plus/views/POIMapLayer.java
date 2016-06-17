package net.osmand.plus.views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.ValueHolder;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.util.Algorithms;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OsmBitmapPanel;

public class POIMapLayer extends OsmandMapLayer implements IContextMenuProvider,
		MapTextProvider<Amenity>,  IRouteInformationListener {
	private static final int startZoom = 9;

	public static final org.apache.commons.logging.Log log = PlatformUtil.getLog(POIMapLayer.class);

	private BufferedImage poiBackground;
	private BufferedImage poiBackgroundSmall;

	private OsmBitmapPanel view;
	private final static int MAXIMUM_SHOW_AMENITIES = 5;

	private ResourceManager resourceManager;
	private RoutingHelper routingHelper;
	private PoiUIFilter filter;
	private MapTextLayer mapTextLayer;

	/// cache for displayed POI
	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private MapLayerData<List<Amenity>> data;

	private OsmandSettings settings;

	private OsmWindow app;


	public POIMapLayer(final OsmWindow activity) {
		routingHelper = activity.getRoutingHelper();
		routingHelper.addListener(this);
		settings = activity.getSettings();
		app = activity;
		data = new OsmandMapLayer.MapLayerData<List<Amenity>>() {
			{
				ZOOM_THRESHOLD = 0;
			}

			@Override
			public boolean isInterrupted() {
				return super.isInterrupted();
			}

			@Override
			public void layerOnPostExecute() {
				activity.getDrawPanel().refreshMap();
			}

			@Override
			protected List<Amenity> calculateResult(RotatedTileBox tileBox) {
				QuadRect latLonBounds = tileBox.getLatLonBounds();
				if (filter == null || latLonBounds == null) {
					return new ArrayList<Amenity>();
				}
				int z = (int) Math.floor(tileBox.getZoom() + Math.log(app.getSettings().MAP_DENSITY.get()) / Math.log(2));

				List<Amenity> res = filter.searchAmenities(latLonBounds.top, latLonBounds.left,
						latLonBounds.bottom, latLonBounds.right, z, new ResultMatcher<Amenity>() {

							@Override
							public boolean publish(Amenity object) {
								return true;
							}

							@Override
							public boolean isCancelled() {
								return isInterrupted();
							}
						});

				Collections.sort(res, new Comparator<Amenity>() {
					@Override
					public int compare(Amenity lhs, Amenity rhs) {
						return lhs.getId() < rhs.getId() ? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1);
					}
				});

				return res;
			}
		};
	}


	public void getAmenityFromPoint(RotatedTileBox tb, Point2D pPoint, List<? super Amenity> am) {
		List<Amenity> objects = data.getResults();
		if (objects != null) {
			int ex = (int) pPoint.getX();
			int ey = (int) pPoint.getY();
			final int rp = getRadiusPoi(tb);
			int compare = rp;
			int radius = rp * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
						compare = radius;
						am.add(n);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}

	private StringBuilder buildPoiInformation(StringBuilder res, Amenity n) {
		String format = OsmAndFormatter.getPoiStringWithoutType(n,
				view.getSettings().MAP_PREFERRED_LOCALE.get());
		res.append(" " + format + "\n" + OsmAndFormatter.getAmenityDescriptionContent(view.getApplication(), n, true));
		return res;
	}

	@Override
	public void initLayer(OsmBitmapPanel view) {
		this.view = view;

//		paintIcon = new Paint();
//		//paintIcon.setStrokeWidth(1);
//		//paintIcon.setStyle(Style.STROKE);
//		//paintIcon.setColor(Color.BLUE);
//		paintIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
//		paintIconBackground = new Paint();
		poiBackground = readImage("map_white_orange_poi_shield", view);
		poiBackgroundSmall = readImage("map_white_orange_poi_shield_small", view);

		resourceManager = view.getApplication().getResourceManager();
		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
	}


	public int getRadiusPoi(RotatedTileBox tb) {
		int r = 0;
		final double zoom = tb.getZoom();
		if (zoom < startZoom) {
			r = 0;
		} else if (zoom <= 15) {
			r = 10;
		} else if (zoom <= 16) {
			r = 14;
		} else if (zoom <= 17) {
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * view.getScaleCoefficient());
	}

	@Override
	public void onPrepareBufferImage(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
		Graphics2D g2 = view.createGraphics(canvas);
		g2.setColor(Color.blue);
		g2.setStroke(new BasicStroke(1));
		// FIXME: Here, we need multiply? What role plays the color white??
//		g2.setComposite(AlphaComposite.SrcIn);
		if (!Algorithms.objectEquals(this.settings.SELECTED_POI_FILTER_FOR_MAP.get(),
				filter == null ? null : filter.getFilterId())) {
			if (this.settings.SELECTED_POI_FILTER_FOR_MAP.get() == null) {
				this.filter = null;
			} else {
				PoiFiltersHelper pfh = app.getPoiFilters();
				this.filter = pfh.getFilterById(this.settings.SELECTED_POI_FILTER_FOR_MAP.get());
			}
			data.clearCache();
		}

		List<Amenity> objects = Collections.emptyList();
		List<Amenity> fullObjects = new ArrayList<>();
		List<LatLon> fullObjectsLatLon = new ArrayList<>();
		List<LatLon> smallObjectsLatLon = new ArrayList<>();
		if (filter != null) {
			if (tileBox.getZoom() >= startZoom) {
				data.queryNewData(tileBox);
				objects = data.getResults();
				if (objects != null) {
					float iconSize = poiBackground.getWidth() * 3 / 2;
					QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

					for (Amenity o : objects) {
						float x = tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						float y = tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());

						if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
							g2.drawImage(poiBackgroundSmall, (int)(x - poiBackgroundSmall.getWidth() / 2), (int)(y - poiBackgroundSmall.getHeight() / 2), null);
							smallObjectsLatLon.add(new LatLon(o.getLocation().getLatitude(),
									o.getLocation().getLongitude()));
						} else {
							fullObjects.add(o);
							fullObjectsLatLon.add(new LatLon(o.getLocation().getLatitude(),
									o.getLocation().getLongitude()));
						}
					}
					for (Amenity o : fullObjects) {
						int x = (int) tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						int y = (int) tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						g2.drawImage(poiBackground, (int)(x - poiBackground.getWidth() / 2), (int)(y - poiBackground.getHeight() / 2), null);
						BufferedImage bmp = app.getBitmap(o);
						if (bmp != null) {
							g2.drawImage(bmp, (int)(x - bmp.getWidth() / 2), (int)(y - bmp.getHeight() / 2), null);
						}
					}
					this.fullObjectsLatLon = fullObjectsLatLon;
					this.smallObjectsLatLon = smallObjectsLatLon;
				}
			}
		}
		mapTextLayer.putData(this, objects);
		g2.dispose();
	}


	@Override
	public void onDraw(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
		routingHelper.removeListener(this);
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public static void showWikipediaDialog(OsmWindow ctx, OsmWindow app, Amenity a) {
		String lang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (a.getType().isWiki()) {
			String preferredLang = lang;
			if (Algorithms.isEmpty(preferredLang)) {
				preferredLang = app.getLanguage();
			}
			showWiki(ctx, app, a, preferredLang);
		}
	}

	public static void showDescriptionDialog(OsmWindow ctx, OsmWindow app, String text, String title) {
		showText(ctx, app, text, title);
	}

//	static int getResIdFromAttribute(final OsmWindow ctx, final int attr) {
//		if (attr == 0)
//			return 0;
//		final TypedValue typedvalueattr = new TypedValue();
//		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
//		return typedvalueattr.resourceId;
//	}

	private static void showWiki(final OsmWindow ctx, final OsmWindow app, final Amenity a, final String lang) {
		final String title = a.getName(lang);
		String lng = a.getContentSelected("content", lang, "en");
		if (Algorithms.isEmpty(lng)) {
			lng = "en";
		}

		final String langSelected = lng;
		String content = a.getDescription(langSelected);
		String article = "https://" + langSelected.toLowerCase() + ".wikipedia.org/wiki/" + title.replace(' ', '_');
		app.showWikipedia(content, title, article);
	}

	private static void showText(final OsmWindow ctx, final OsmWindow app, final String text, String title) {
		app.showWikipedia(text, title, "");
//		JOptionPane.showMessageDialog(null, text, title, JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof Amenity) {
			return buildPoiInformation(new StringBuilder(), (Amenity) o).toString();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Amenity) {
			return new PointDescription(PointDescription.POINT_TYPE_POI, ((Amenity) o).getName(
					view.getSettings().MAP_PREFERRED_LOCALE.get()));
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(Point2D point, RotatedTileBox tileBox, List<Object> objects) {
		getAmenityFromPoint(tileBox, point, objects);
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Amenity) {
			return ((Amenity) o).getLocation();
		}
		return null;
	}

	public boolean isObjectClickable(Object o) {
		return o instanceof Amenity;
	}


	@Override
	public LatLon getTextLocation(Amenity o) {
		return o.getLocation();
	}


	public int getTextShift(Amenity o, RotatedTileBox rb) {
		return getRadiusPoi(rb);
	}


	public String getText(Amenity o) {
		return o.getName(view.getSettings().MAP_PREFERRED_LOCALE.get());
	}


	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}


	public void routeWasCancelled() {
	}


	public void routeWasFinished() {
	}

	public static int dpToPx(OsmWindow ctx, float dp) {
		return -1;
	}


}
