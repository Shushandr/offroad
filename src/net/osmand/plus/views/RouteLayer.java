package net.osmand.plus.views;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.ui.ColorUtils;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.PorterDuffMultiplyFilter;

public class RouteLayer extends OsmandMapLayer implements IContextMenuProvider {
	
	private OsmBitmapPanel view;
	
	private final RoutingHelper helper;
	private List<Location> points = new ArrayList<Location>();
	private List<Location> actionPoints = new ArrayList<Location>();
	private BasicStroke paint;
	private BasicStroke actionPaint;
	private BasicStroke paint2;
	private boolean isPaint2;
	private BasicStroke shadowPaint;
	private boolean isShadowPaint;
	private BasicStroke paint_1;
	private boolean isPaint_1;
	private BasicStroke paintIcon;
	private BasicStroke paintIconAction;

	private Graphics2D gpaint;
	private Graphics2D gactionPaint;
	private Graphics2D gpaint2;
	private Graphics2D gshadowPaint;
	private Graphics2D gpaint_1;
	private Graphics2D gpaintIcon;
	private Graphics2D gpaintIconAction;

	private int cachedHash;

	private Path2D path;

	// cache
	private BufferedImage coloredArrowUp;
	private BufferedImage actionArrow;


	private OsmandRenderer osmandRenderer;


	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		paint = new BasicStroke(0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0);
		actionArrow = readImage("map_action_arrow", view); 
		actionPaint = new BasicStroke(7 * view.getScaleCoefficient(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0);
		path = new Path2D.Double();
		paintIcon = new BasicStroke(3);
		paintIconAction = new BasicStroke();
	}


	
	@Override
	public void initLayer(OsmBitmapPanel view) {
		this.view = view;
		osmandRenderer = view.getContext().getResourceManager().getRenderer().getRenderer();
		initUI();
	}

	public void updateLayerStyle() {
		cachedHash = -1;
	}
	
	private void updatePaints(Graphics2D pGraphics2d, DrawSettings nightMode, RotatedTileBox tileBox){
		RenderingRulesStorage rrs = view.getContext().getRenderingRulesStorage();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, isNight, tileBox.getMapDensity());
		if (hsh != cachedHash) {
//			cachedHash = hsh;
			// cachedColor = view.getResources().getColor(R.color.nav_track);
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
				gpaint = view.createGraphics(pGraphics2d);
				gactionPaint = view.createGraphics(pGraphics2d);
				gpaintIcon = view.createGraphics(pGraphics2d);
				gpaintIcon.setColor(Color.black);
				gpaintIconAction = view.createGraphics(pGraphics2d);
				gpaint2 = view.createGraphics(pGraphics2d);
				gpaint_1 = view.createGraphics(pGraphics2d);
				gshadowPaint = view.createGraphics(pGraphics2d);
				if (req.searchRenderingAttribute("route")) {
					RenderingContext rc = new OsmandRenderer.RenderingContext();
					rc.setDensityValue((float) tileBox.getMapDensity());
//					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
					gpaint.setStroke(paint);
					osmandRenderer.updatePaint(req, gpaint, 0, false, rc);
					Stroke stroke = gpaint.getStroke();
					if (stroke instanceof BasicStroke) {
						BasicStroke bstroke = (BasicStroke) stroke;
						if(bstroke.getLineWidth() == 0) {
							gpaint.setStroke(deriveStroke(stroke, 12f*view.getDensity()));
						}
					}
					gactionPaint.setStroke(actionPaint);
					osmandRenderer.updatePaint(req, gactionPaint, 2, false, rc);
					// see http://www.curious-creature.com/2006/09/20/new-blendings-modes-for-java2d/
					final Color color = gactionPaint.getColor();
					gpaintIconAction.setComposite(new PorterDuffMultiplyFilter(color));
//					paintIconAction.setColorFilter(new PorterDuffColorFilter(gactionPaint.getColor(), Mode.MULTIPLY));
					
					isPaint2 = osmandRenderer.updatePaint(req, gpaint2, 1, false, rc);
					isPaint_1 = osmandRenderer.updatePaint(req, gpaint_1, -1, false, rc);
					isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
					if(isShadowPaint) {
						gshadowPaint.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN));
						gshadowPaint.setColor(ColorUtils.create(req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR)));
//						ColorFilter cf = new PorterDuffColorFilter(req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR), Mode.SRC_IN);
//						shadowPaint.setColorFilter(cf);
						gshadowPaint.setStroke(deriveStroke(gshadowPaint.getStroke(), 12f*view.getDensity()));
						if (stroke instanceof BasicStroke) {
							BasicStroke bstroke = (BasicStroke) stroke;
							gshadowPaint.setStroke(deriveStroke(gshadowPaint.getStroke(), bstroke.getLineWidth() + 2 * rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS)));
						}
					}
				} else {
					System.err.println("Rendering attribute route is not found !");
					gpaint.setStroke(deriveStroke(gpaint.getStroke(), 12f*view.getDensity()));
				}
				gactionPaint.setStroke(deriveStroke(gactionPaint.getStroke(), 7 * view.getScaleCoefficient()));
			}
		}
	}


	public Stroke deriveStroke(Stroke pStroke, float newWidth) {
		if (pStroke instanceof BasicStroke) {
			BasicStroke bstroke = (BasicStroke) pStroke;
			return new BasicStroke(newWidth, bstroke.getEndCap(), bstroke.getLineJoin(), bstroke.getMiterLimit(), bstroke.getDashArray(), bstroke.getDashPhase());
		}
		return pStroke;
	}
	
	
	private int calculateHash(Object... o) {
		return Arrays.hashCode(o);
	}
	
	@Override
	public void onPrepareBufferImage(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
		path.reset();
		if (isRouteReady()) {
			updatePaints(canvas, settings, tileBox);
			if(coloredArrowUp == null) {
				BufferedImage originalArrowUp;
				originalArrowUp = RenderingIcons.getIcon("arrow", true);
				coloredArrowUp = originalArrowUp;
				//				coloredArrowUp = BufferedImage.createScaledBitmap(originalArrowUp, originalArrowUp.getWidth() * 3 / 4,	
				//						originalArrowUp.getHeight() * 3 / 4, true);
			}
			int w = tileBox.getPixWidth();
			int h = tileBox.getPixHeight();
			Location lastProjection = helper.getLastProjection();
			final RotatedTileBox cp ;
			if(lastProjection != null &&
					tileBox.containsLatLon(lastProjection.getLatitude(), lastProjection.getLongitude())){
				cp = tileBox.copy();
				cp.increasePixelDimensions(w /2, h);
			} else {
				cp = tileBox;
			}

			final QuadRect latlonRect = cp.getLatLonBounds();
			double topLatitude = latlonRect.top;
			double leftLongitude = latlonRect.left;
			double bottomLatitude = latlonRect.bottom;
			double rightLongitude = latlonRect.right;
			double lat = topLatitude - bottomLatitude + 0.1;
			double lon = rightLongitude - leftLongitude + 0.1;
			drawLocations(tileBox, canvas, topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);
			gpaint.dispose(); gpaint=null;
			gactionPaint.dispose(); gactionPaint=null;
			gpaintIconAction.dispose(); gpaintIconAction=null;
			gpaintIcon.dispose(); gpaintIcon=null;
			gpaint2.dispose(); gpaint2=null;
			gpaint_1.dispose(); gpaint_1=null;
			gshadowPaint.dispose(); gshadowPaint=null;

		}
	
	}


	public boolean isRouteReady() {
		return helper.getFinalLocation() != null && helper.getRoute().isCalculated();
	}
	
	@Override
	public void onDraw(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {}

	private void drawAction(RotatedTileBox tb, Graphics2D canvas) {
		if (actionPoints.size() > 0) {
			rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			try {
				Path2D pth = new Path2D.Double();
				AffineTransform matrix = new AffineTransform();
				boolean first = true;
				int x = 0, px = 0, py = 0, y = 0;
				for (int i = 0; i < actionPoints.size(); i++) {
					Location o = actionPoints.get(i);
					if (o == null) {
						first = true;
						gactionPaint.draw(pth);
						double angleRad = Math.atan2(y - py, x - px);
						double angle = (angleRad + Math.PI/2);
						double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if (distSegment == 0) {
							continue;
						}
						// int len = (int) (distSegment / pxStep);
						float pdx = x - px;
						float pdy = y - py;
						matrix.setToTranslation(px + pdx - actionArrow.getWidth() / 2, py + pdy);
						matrix.rotate(angle, actionArrow.getWidth() / 2, 0);
						matrix.translate(0, -actionArrow.getHeight() / 2);
						gpaintIconAction.drawImage(actionArrow, matrix, null);
					} else {
						px = x;
						py = y;
						x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
						y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
						if (first) {
							pth.reset();
							pth.moveTo(x, y);
							first = false;
						} else {
							pth.lineTo(x, y);
						}
					}
				}

			} finally {
				rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}

	private void drawSegment(RotatedTileBox tb, Graphics2D canvas) {
		if (points.size() > 0) {
			rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			try {
				TIntArrayList tx = new TIntArrayList();
				TIntArrayList ty = new TIntArrayList();
				for (int i = 0; i < points.size(); i++) {
					Location o = points.get(i);
					int x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					int y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					tx.add(x);
					ty.add(y);
				}
				calculatePath(tb, tx, ty, path);

				if (isPaint_1) {
					gpaint_1.draw(path);
				}
				if (isShadowPaint) {
					gshadowPaint.draw(path);
				}
				gpaint.draw(path);
				if (isPaint2) {
					gpaint2.draw(path);
				}
				if (tb.getZoomAnimation() == 0) {
					TIntArrayList lst = new TIntArrayList(50);
					calculateSplitPaths(tb, tx, ty, lst);
					drawArrowsOverPath(canvas, lst, coloredArrowUp);
				}
			} finally {
				rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}


	private void rotate(float pRotate, int pCenterPixelX, int pCenterPixelY) {
		gpaint.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gactionPaint.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gpaintIconAction.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gpaintIcon.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gpaint2.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gpaint_1.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		gshadowPaint.rotate(pRotate, pCenterPixelX, pCenterPixelY);
		
	}


	private void drawArrowsOverPath(Graphics2D canvas, TIntArrayList lst, BufferedImage arrow) {
		float pxStep = arrow.getHeight() * 4f;
		AffineTransform matrix = new AffineTransform();
		float dist = 0;
		for (int i = 0; i < lst.size(); i += 4) {
			int px = lst.get(i);
			int py = lst.get(i + 1);
			int x = lst.get(i + 2);
			int y = lst.get(i + 3);
			float angleRad = (float) Math.atan2(y - py, x - px);
			float angle = (float) (angleRad + Math.PI/2);
			float distSegment = (float) Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			if(distSegment == 0) {
				continue;
			}
			int len = (int) (distSegment / pxStep);
			if (len > 0) {
				float pdx = ((x - px) / len);
				float pdy = ((y - py) / len);
				for (int k = 1; k <= len; k++) {
					matrix.setToTranslation(px + k * pdx- arrow.getWidth() / 2 , py + pdy * k);
					matrix.rotate(angle, arrow.getWidth() / 2, 0);
					matrix.translate(0, -arrow.getHeight() / 2);
					gpaintIcon.drawImage(arrow, matrix, null);
					dist = 0;
				}
			} else {
				if(dist > pxStep) {
					matrix.setToTranslation(px + (x - px) / 2 - arrow.getWidth() / 2, py + (y - py) / 2);
					matrix.rotate(angle, arrow.getWidth() / 2, 0);
					matrix.translate(0, -arrow.getHeight() / 2);
					gpaintIcon.drawImage(arrow, matrix, null);
					dist = 0;
				} else {
					dist += distSegment;
				}
			}
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Graphics2D canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		points.clear();
		actionPoints.clear();
		boolean previousVisible = false;
		Location lastProjection = helper.getLastProjection();
		if (lastProjection != null) {
			if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
					&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
				points.add(lastProjection);
				previousVisible = true;
			}
		}
		List<Location> routeNodes = helper.getRoute().getRouteLocations();
		int cd = helper.getRoute().getCurrentRoute();
		List<RouteDirectionInfo> rd = helper.getRouteDirections();
		Iterator<RouteDirectionInfo> it = rd.iterator();
		for (int i = 0; i < routeNodes.size(); i++) {
			Location ls = routeNodes.get(i);
			if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
					&& ls.getLatitude() <= topLatitude) {
				points.add(ls);
				
				if (!previousVisible) {
					if (i > 0) {
						points.add(0, routeNodes.get(i - 1));
					} else if (lastProjection != null) {
						points.add(0, lastProjection);
					}
				}
				previousVisible = true;
			} else if (previousVisible) {
				points.add(ls);
				drawSegment(tb, canvas);
				previousVisible = false;
				points.clear();
			}
		}
		drawSegment(tb, canvas);
		if (tb.getZoom() >= 14) {
			calculateActionPoints(topLatitude, leftLongitude, bottomLatitude, rightLongitude, lastProjection,
					routeNodes, cd, it, tb.getZoom());
			drawAction(tb, canvas);
		}
	}


	private void calculateActionPoints(double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude, Location lastProjection, List<Location> routeNodes, int cd,
			Iterator<RouteDirectionInfo> it, int zoom) {
		RouteDirectionInfo nf = null;
		
		double DISTANCE_ACTION = 35;
		if(zoom >= 17) {
			DISTANCE_ACTION = 15;
		} else if (zoom == 15) {
			DISTANCE_ACTION = 70;
		} else if (zoom < 15) {
			DISTANCE_ACTION = 110;
		}
		double actionDist = 0;
		Location previousAction = null; 
		actionPoints.clear();
		int prevFinishPoint = -1;
		for (int routePoint = 0; routePoint < routeNodes.size(); routePoint++) {
			Location loc = routeNodes.get(routePoint);
			if(nf != null) {
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if(pnt < routePoint + cd ) {
					nf = null;
				}
			}
			while (nf == null && it.hasNext()) {
				nf = it.next();
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if (pnt < routePoint + cd) {
					nf = null;
				}
			}
			boolean action = nf != null && (nf.routePointOffset == routePoint + cd ||
					(nf.routePointOffset <= routePoint + cd && routePoint + cd  <= nf.routeEndPointOffset));
			if(!action && previousAction == null) {
				// no need to check
				continue;
			}
			boolean visible = leftLongitude <= loc.getLongitude() && loc.getLongitude() <= rightLongitude && bottomLatitude <= loc.getLatitude()
					&& loc.getLatitude() <= topLatitude;
			if(action && !visible && previousAction == null) {
				continue;
			}
			if (!action) {
				// previousAction != null
				float dist = loc.distanceTo(previousAction);
				actionDist += dist;
				if (actionDist >= DISTANCE_ACTION) {
					actionPoints.add(calculateProjection(1 - (actionDist - DISTANCE_ACTION) / dist, previousAction, loc));
					actionPoints.add(null);
					prevFinishPoint = routePoint;
					previousAction = null;
					actionDist = 0;
				} else {
					actionPoints.add(loc);
					previousAction = loc;
				}
			} else {
				// action point
				if (previousAction == null) {
					addPreviousToActionPoints(lastProjection, routeNodes, DISTANCE_ACTION,
							prevFinishPoint, routePoint, loc);
				}
				actionPoints.add(loc);
				previousAction = loc;
				prevFinishPoint = -1;
				actionDist = 0;
			}
		}
		if(previousAction != null) {
			actionPoints.add(null);
		}
	}


	private void addPreviousToActionPoints(Location lastProjection, List<Location> routeNodes, double DISTANCE_ACTION,
			int prevFinishPoint, int routePoint, Location loc) {
		// put some points in front
		int ind = actionPoints.size();
		Location lprevious = loc;
		double dist = 0;
		for (int k = routePoint - 1; k >= -1; k--) {
			Location l = k == -1 ? lastProjection : routeNodes.get(k);
			float locDist = lprevious.distanceTo(l);
			dist += locDist;
			if (dist >= DISTANCE_ACTION) {
				if (locDist > 1) {
					actionPoints.add(ind,
							calculateProjection(1 - (dist - DISTANCE_ACTION) / locDist, lprevious, l));
				}
				break;
			} else {
				actionPoints.add(ind, l);
				lprevious = l;
			}
			if (prevFinishPoint == k) {
				if (ind >= 2) {
					actionPoints.remove(ind - 2);
					actionPoints.remove(ind - 2);
				}
				break;
			}
		}
	}
	
	private Location calculateProjection(double part, Location lp, Location l) {
		Location p = new Location(l);
		p.setLatitude(lp.getLatitude() + part * (l.getLatitude() - lp.getLatitude()));
		p.setLongitude(lp.getLongitude() + part * (l.getLongitude() - lp.getLongitude()));
		return p;
	}


	public RoutingHelper getHelper() {
		return helper;
	}

	
	// to show further direction
	public Path2D getPath() {
		return path;
	}

	
	@Override
	public void destroyLayer() {
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(Point2D point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(Point2D point, RotatedTileBox tileBox) {
		return false;
	}


	@Override
	public String getObjectDescription(Object pO) {
		if (pO instanceof RouteCalculationResult) {
			RouteCalculationResult res = (RouteCalculationResult) pO;
			return res.getErrorMessage();
		}
		return null;
	}


	@Override
	public PointDescription getObjectName(Object pO) {
		if (pO instanceof RouteCalculationResult) {
			RouteCalculationResult res = (RouteCalculationResult) pO;
			return new PointDescription(PointDescription.POINT_TYPE_WPT, res.getErrorMessage());
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
	public boolean isObjectClickable(Object pO) {
		return (pO instanceof RouteCalculationResult);
	}


	public void getRouteFromPoint(RotatedTileBox tb, Point2D point, List<? super RouteCalculationResult> res) {
		if(!isRouteReady()){
			return;
		}
		double r = 15 * tb.getPixelDistanceInMeters();
		LatLon latLon = tb.getLatLonFromPixel(point.getX(), point.getY());
		RouteCalculationResult routeCalculationResult = helper.getRoute();
		List<Location> routeNodes = routeCalculationResult.getRouteLocations();
		LatLon last = null;
		for (Location l : routeNodes) {
			if(last != null){
				double distance = MapUtils.getOrthogonalDistance(latLon, last, l.getLatLon());
				if(Math.abs(distance)<r){
					res.add(routeCalculationResult);
					return;
				}
			}
			last = l.getLatLon();
		}
	}
	

	
	@Override
	public void collectObjectsFromPoint(Point2D pPoint, RotatedTileBox pTileBox, List<Object> pRes) {
		getRouteFromPoint(pTileBox, pPoint, pRes);
	}


	@Override
	public LatLon getObjectLocation(Object pO) {
		return null;
	}

}
