package net.osmand.plus.views;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.DirectOffroadLayer;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.PorterDuffMultiplyFilter;

public class FavoritesLayer  extends OsmandMapLayer implements IContextMenuProvider, MapTextProvider<LocationPoint> {

	public static final int DEFAULT_COLOR = 0x55aabbcc;
	public static final int DEFAULT_OUTER_COLOR = 0x88555555;

	protected int startZoom = 6;
	
	protected OsmBitmapPanel view;
//	private Paint paint;
	private FavouritesDbHelper favorites;
	protected List<LocationPoint> cache = new ArrayList<LocationPoint>();
	private MapTextLayer textLayer;
//	private Paint paintIcon;
	private BufferedImage pointSmall;
	private int defaultColor;

	private OsmandSettings settings;
	
	protected Class<? extends LocationPoint> getFavoriteClass() {
		return (Class<? extends LocationPoint>) FavouritePoint.class;
	}
	
	protected String getObjName() {
		return view.getContext().getString(R.string.favorite);
	}
	
	protected List<? extends LocationPoint> getPoints() {
		return favorites.getFavouritePoints();
	}
	
	@Override
	public void initLayer(OsmBitmapPanel view) {
		this.view = view;
//		paint = new Paint();
//		paint.setAntiAlias(true);
//		paint.setFilterBitmap(true);
//		paint.setDither(true);
		settings = view.getApplication().getSettings();
		favorites = view.getApplication().getFavorites();
		textLayer = view.getLayerByClass(MapTextLayer.class);
//		paintIcon = new Paint();
		pointSmall = readImage("map_white_shield_small", view);
		defaultColor = DEFAULT_COLOR; //view.getResources().getColor(R.color.color_favorite);
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return (Math.abs(objx - ex) <= radius * 2 && Math.abs(objy - ey) <= radius * 2) ;
//		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
		//return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
	}

	@Override
	public void destroyLayer() {
		
	}
	

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}
	
	
	@Override
	public void onDraw(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}
	
	@Override
	public void onPrepareBufferImage(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
		cache.clear();
		if (this.settings.SHOW_FAVORITES.get()) {
			if (tileBox.getZoom() >= startZoom) {
				float iconSize = FavoriteImageDrawable.getOrCreate(view.getContext(), 0,
						 true).getIntrinsicWidth() * 3 / 2.5f;
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

				// request to load
				final QuadRect latLonBounds = tileBox.getLatLonBounds();
				List<LocationPoint> fullObjects = new ArrayList<>();
				List<LatLon> fullObjectsLatLon = new ArrayList<>();
				List<LatLon> smallObjectsLatLon = new ArrayList<>();
				for (LocationPoint o : getPoints()) {
					if (!o.isVisible()) {
						continue;
					}
					int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());

					if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
						int col = o.getColor() == 0 ? defaultColor : o.getColor();
						Graphics2D ng = (Graphics2D) canvas.create();
						ng.setComposite(new PorterDuffMultiplyFilter(new Color(col, true)));
						ng.drawImage(pointSmall, x - pointSmall.getWidth() / 2, y - pointSmall.getHeight() / 2, null); //paintIcon
						ng.dispose();
						smallObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					} else {
						fullObjects.add(o);
						fullObjectsLatLon.add(new LatLon(o.getLatitude(), o.getLongitude()));
					}
				}
				for (LocationPoint o : fullObjects) {
					drawPoint(canvas, tileBox, latLonBounds, o);
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		if(textLayer.isVisible()) {
			textLayer.putData(this, cache);
		}

	}


	private void drawPoint(Graphics2D canvas, RotatedTileBox tileBox, final QuadRect latLonBounds, LocationPoint o) {
		if (o.isVisible() && o.getLatitude() >= latLonBounds.bottom && o.getLatitude() <= latLonBounds.top  && o.getLongitude() >= latLonBounds.left
				&& o.getLongitude() <= latLonBounds.right ) {
			cache.add(o);
			int x = (int) tileBox.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
			FavoriteImageDrawable fid = FavoriteImageDrawable.getOrCreate(view.getContext(), o.getColor(), true);
			fid.drawBitmapInCenter(canvas, x, y);
//					canvas.drawBitmap(favoriteIcon, x - favoriteIcon.getWidth() / 2, 
//							y - favoriteIcon.getHeight(), paint);
		}
	}
	
	
	@Override
	public boolean onLongPressEvent(Point2D point, RotatedTileBox tileBox) {
		return false;
	}

	public void getFavoriteFromPoint(RotatedTileBox tb, Point2D point, List<? super LocationPoint> res) {
		int r = (int) (15 * tb.getDensity());
		int ex = (int) point.getX();
		int ey = (int) point.getY();
		for (LocationPoint n : getPoints()) {
			getFavFromPoint(tb, res, r, ex, ey, n);
		}
	}

	private void getFavFromPoint(RotatedTileBox tb, List<? super LocationPoint> res, int r, int ex, int ey,
			LocationPoint n) {
		if (n.isVisible()) { 
			int x = (int) tb.getPixXFromLatLon(n.getLatitude(), n.getLongitude());
			int y = (int) tb.getPixYFromLatLon(n.getLatitude(), n.getLongitude());
			if (calculateBelongs(ex, ey, x, y, r)) {
				res.add(n);
			}
		}
	}

	@Override
	public String getObjectDescription(Object o) {
		Class<? extends LocationPoint> fcl = getFavoriteClass();
		if(o!= null && fcl.isInstance(o)) {
			return PointDescription.getSimpleName((LocationPoint) o, view.getContext()) ;
		}
		return null;
	}

	
	
	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof LocationPoint){
			return ((LocationPoint) o).getPointDescription(view.getContext()); //$NON-NLS-1$
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
	public boolean isObjectClickable(Object o) {
		return o instanceof LocationPoint;
	}

	@Override
	public void collectObjectsFromPoint(Point2D point, RotatedTileBox tileBox, List<Object> res) {
		if (this.settings.SHOW_FAVORITES.get() && tileBox.getZoom() >= startZoom) {
			getFavoriteFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof LocationPoint){
			return new LatLon(((LocationPoint)o).getLatitude(), ((LocationPoint)o).getLongitude());
		}
		return null;
	}

	@Override
	public LatLon getTextLocation(LocationPoint o) {
		return new LatLon(o.getLatitude(), o.getLongitude());
	}

	@Override
	public int getTextShift(LocationPoint o, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity());
	}

	@Override
	public String getText(LocationPoint o) {
		return PointDescription.getSimpleName(o, view.getContext());
	}
	

}


