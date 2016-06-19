package net.sourceforge.offroad.ui;



import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.views.OsmandMapLayer;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.Paint.Style;

/** Creates the perimeter circle around the current position, if enabled in the view.
 * @author foltin
 * @date 16.06.2016
 */
public class CursorDistanceLayer extends OsmandMapLayer implements DirectOffroadLayer, IContextMenuProvider {
	private static final Log LOG = PlatformUtil.getLog(CursorDistanceLayer.class);

	protected final static int RADIUS = 7;
	protected final static float HEADING_ANGLE = 60;
	
	private Paint area;
	private Paint aroundArea;
	
	private OsmBitmapPanel view;
	
	private ApplicationMode appMode;
	private boolean nm;
	
	public CursorDistanceLayer() {
	}

	private void initUI() {
		
		area = new Paint();
		area.setColor(view.getResources().getColor(R.color.pos_area));
		
		aroundArea = new Paint();
		aroundArea.setColor(view.getResources().getColor(R.color.pos_around));
		aroundArea.setStyle(Style.STROKE);
		aroundArea.setStrokeWidth(7);
		aroundArea.setAntiAlias(true);
		
		
		updateIcons(view.getSettings().getApplicationMode(), false);
	}
	
	@Override
	public void initLayer(OsmBitmapPanel view) {
		this.view = view;
		initUI();
	}


	
	@Override
	public void onDraw(Graphics2D canvas, RotatedTileBox box, DrawSettings nightMode) {
		if(!view.isCursorRadiusEnabled()){
			return;
		}
		if(box.getZoom() < 3) {
			return;
		}
		// draw
		boolean nm = nightMode != null && nightMode.isNightMode();
		updateIcons(view.getSettings().getApplicationMode(), nm);
		LatLon cursorLocation = view.getCursorPosition();
		if(cursorLocation == null || view == null){
			return;
		}
		int locationX = (int) box.getPixXFromLatLon(cursorLocation);
		int locationY = (int) box.getPixYFromLatLon(cursorLocation);

		final double dist = box.getDistance(0, box.getPixHeight() / 2, box.getPixWidth(), box.getPixHeight() / 2);
		int radius = (int) (((double) box.getPixWidth()) / dist * view.getCursorRadiusSizeInMeters());
		
		if (radius > RADIUS * box.getDensity()) {
			area.updateGraphics(canvas);
			canvas.fillOval(locationX-radius, locationY-radius, 2*radius, 2*radius);
			aroundArea.updateGraphics(canvas);
			canvas.drawOval(locationX-radius, locationY-radius, 2*radius, 2*radius);
		}
	}
	

	@Override
	public void destroyLayer() {
		
	}
	public void updateIcons(ApplicationMode appMode, boolean nighMode) {
		if (appMode != this.appMode || this.nm != nighMode) {
			this.appMode = appMode;
			this.nm = nighMode;
			area.setColor(view.getResources().getColor(!nm ? R.color.pos_area : R.color.pos_area_night));
		}
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}


	@Override
	public void collectObjectsFromPoint(Point2D point, RotatedTileBox tileBox, List<Object> o) {
		if (view.isCursorRadiusEnabled()) {
			getMyLocationFromPoint(tileBox, point, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		return getMyLocation();
	}

	@Override
	public String getObjectDescription(Object o) {
		return view.getResources().getString(R.string.shared_string_my_location);
	}

	@Override
	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
				view.getContext().getString(R.string.shared_string_my_location), "");
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
		return false;
	}

	private LatLon getMyLocation() {
		return view.getCursorPosition();
	}

	private void getMyLocationFromPoint(RotatedTileBox tb, Point2D point, List<? super LatLon> myLocation) {
		LatLon location = getMyLocation();
		if (location != null && view != null) {
			int ex = (int) point.getX();
			int ey = (int) point.getY();
			int x = (int) tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			int y = (int) tb.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
			int rad = (int) (18 * tb.getDensity());
			if (Math.abs(x - ex) <= rad && (ey - y) <= rad && (y - ey) <= 2.5 * rad) {
				myLocation.add(location);
			}
		}
	}
}
