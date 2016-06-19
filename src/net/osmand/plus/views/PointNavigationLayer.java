package net.osmand.plus.views;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.DirectOffroadLayer;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OsmBitmapPanel;

public class PointNavigationLayer extends OsmandMapLayer implements DirectOffroadLayer, IContextMenuProvider {
	protected final static int DIST_TO_SHOW = 80;

//	private Stroke point;
//	private Stroke bitmapPaint;
	
	private OsmBitmapPanel view;
	private float[] calculations = new float[2];

	private BufferedImage startPoint;
	private BufferedImage targetPoint;
	private BufferedImage intermediatePoint;
	private BufferedImage arrowToDestination;

//	private Stroke textPaint;

	private final OsmWindow map;
	
	public PointNavigationLayer(OsmWindow map) {
		this.map = map;
	}
	

	private void initUI() {
		
//		point = new Stroke();
//		point.setColor(view.getResources().getColor(R.color.nav_point));
//		point.setAntiAlias(true);
//		point.setStyle(Style.FILL);

//		bitmapPaint = new Stroke();
//		bitmapPaint.setDither(true);
//		bitmapPaint.setAntiAlias(true);
//		bitmapPaint.setFilterBitmap(true);
//		textPaint = new Stroke();
//		float sp = Resources.getSystem().getDisplayMetrics().scaledDensity;
//		textPaint.setTextSize(sp * 18);
//		textPaint.setTextAlign(Align.CENTER);
//		textPaint.setAntiAlias(true);
		startPoint = readImage("map_start_point" , view);
		targetPoint = readImage("map_target_point", view);
		intermediatePoint = readImage("map_intermediate_point", view);
		arrowToDestination = readImage("map_arrow_to_destination", view);

		
	}
	
	@Override
	public void initLayer(OsmBitmapPanel view) {
		this.view = view;
		initUI();
	}


	
	@Override
	public void onDraw(Graphics2D canvasO, RotatedTileBox tb, DrawSettings nightMode) {
		if (tb.getZoom() < 3) {
			return;
		}
		Graphics2D canvas = view.createGraphics(canvasO);
		TargetPointsHelper targetPoints = map.getTargetPointsHelper();
		TargetPoint pointToStart = targetPoints.getPointToStart();
		if (pointToStart != null) {
			if (isLocationVisible(tb, pointToStart)) {
				int marginX = startPoint.getWidth() / 6;
				int marginY = startPoint.getHeight();
				int locationX = (int) tb.getPixXFromLatLon(pointToStart.getLatitude(), pointToStart.getLongitude());
				int locationY = (int) tb.getPixYFromLatLon(pointToStart.getLatitude(), pointToStart.getLongitude());
				//canvas.rotate(-tb.getRotate(), locationX, locationY);
				/* bitmapPaint */
				canvas.drawImage(startPoint, locationX - marginX, locationY - marginY, null);
			}
		}

		int index = 0;
		for (TargetPoint ip : targetPoints.getIntermediatePoints()) {
			index ++;
			if (isLocationVisible(tb, ip)) {
				int marginX = intermediatePoint.getWidth() / 6;
				int marginY = intermediatePoint.getHeight();
				int locationX = (int) tb.getPixXFromLatLon(ip.getLatitude(), ip.getLongitude());
				int locationY = (int) tb.getPixYFromLatLon(ip.getLatitude(), ip.getLongitude());
				//canvas.rotate(-tb.getRotate(), locationX, locationY);
				/* bitmapPaint */
				canvas.drawImage(intermediatePoint, locationX - marginX, locationY - marginY, null);
				marginX = intermediatePoint.getWidth() / 3;
				// textPaint
				// TODO: Scaling factor
				Font textFont = canvas.getFont().deriveFont(18);
				canvas.setFont(textFont);
				canvas.drawString(index + "", locationX + marginX, locationY - 3 * marginY / 5);
				//canvas.rotate(tb.getRotate(), locationX, locationY);
			}
		}

		TargetPoint pointToNavigate = targetPoints.getPointToNavigate();
		if (isLocationVisible(tb, pointToNavigate)) {
			int marginX = targetPoint.getWidth() / 6;
			int marginY = targetPoint.getHeight();
			int locationX = (int) tb.getPixXFromLatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
			int locationY = (int) tb.getPixYFromLatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
			//canvas.rotate(-tb.getRotate(), locationX, locationY);
			/* bitmapPaint */
			canvas.drawImage(targetPoint, locationX - marginX, locationY - marginY, null);
		} 

		Iterator<TargetPoint> it = targetPoints.getIntermediatePoints().iterator();
		if(it.hasNext()) {
			pointToNavigate = it.next();
		}
		if (pointToNavigate != null && !isLocationVisible(tb, pointToNavigate)) {
			boolean show = !view.getApplication().getRoutingHelper().isRouteCalculated();
			if(view.getSettings().SHOW_DESTINATION_ARROW.isSet()) {
				show = view.getSettings().SHOW_DESTINATION_ARROW.get();
			}
			if (show) {
				LatLon latLon = map.getMouseLocation();
				net.osmand.Location.distanceBetween(latLon.getLatitude(), latLon.getLongitude(),
						pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), calculations);
				float bearing = calculations[1] - 90;
				float radiusBearing = DIST_TO_SHOW * tb.getDensity();
				final QuadPoint cp = tb.getCenterPixelPoint();
				//canvas.rotate(bearing, cp.x, cp.y);
				canvas.translate(-24 * tb.getDensity() + radiusBearing, -22 * tb.getDensity());
				/* bitmapPaint */
				canvas.drawImage(arrowToDestination, (int)cp.x, (int)cp.y, null);
			}
		}
		
	}

	public boolean isLocationVisible(RotatedTileBox tb, TargetPoint p){
		if(p == null || tb == null){
			return false;
		}
		return tb.containsLatLon(p.getLatitude(), p.getLongitude());
	}
	
	
	@Override
	public void destroyLayer() {
		
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public boolean disableSingleTap() {
		return false;
	}

	public boolean disableLongPressOnMap() {
		return false;
	}

	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(Point2D point, RotatedTileBox tileBox, List<Object> o) {
		TargetPointsHelper tg = map.getTargetPointsHelper();
		List<TargetPoint> intermediatePoints = tg.getAllPoints();
		int r = getRadiusPoi(tileBox);
		for (int i = 0; i < intermediatePoints.size(); i++) {
			TargetPoint tp = intermediatePoints.get(i);
			LatLon latLon = tp.point;
			if (latLon != null) {
				int ex = (int) point.getX();
				int ey = (int) point.getY();
				int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
				if (calculateBelongs(ex, ey, x, y, r)) {
					o.add(tp);
				}
			}
		}
		
		
	}
	
	private boolean calculateBelongs(int ex, int ey, int objx, int objy, int radius) {
		return Math.abs(objx - ex) <= radius && (ey - objy) <= radius && (objy - ey) <= 2.5 * radius ;
	}
	
	public int getRadiusPoi(RotatedTileBox tb){
		int r = 0;
		final double zoom = tb.getZoom();
		if(zoom <= 15){
			r = 10;
		} else if(zoom <= 16){
			r = 14;
		} else if(zoom <= 17){
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	public LatLon getObjectLocation(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).point;
		}
		return null;
	}

	public String getObjectDescription(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getPointDescription(view.getContext()).getFullPlainName(view.getContext());
		}
		return null;
	}

	public PointDescription getObjectName(Object o) {
		if (o instanceof TargetPoint) {
			return ((TargetPoint) o).getPointDescription(view.getContext());
		}
		return null;
	}
}
