package net.osmand.plus.render;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.plus.render.TextRenderer.TextDrawInfo;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.Cap;
import net.sourceforge.offroad.DashPathEffect;
import net.sourceforge.offroad.ui.ColorUtils;
import net.sourceforge.offroad.ui.OsmBitmapPanel.IntermediateImageListener;
import net.sourceforge.offroad.ui.Paint;
import net.sourceforge.offroad.ui.Paint.Style;

public class OsmandRenderer {
	private static final Log log = PlatformUtil.getLog(OsmandRenderer.class);

//	private Paint paint;
//
//	private Paint paintIcon;

	public static final int TILE_SIZE = 256; 
	private static final int MAX_V = 75;

//	private Map<float[], Stroke> dashEffect = new LinkedHashMap<float[], Stroke>();
	private Map<String, float[]> parsedDashEffects = new LinkedHashMap<String, float[]>();
	private Map<String, TexturePaint> shaders = new LinkedHashMap<String, TexturePaint>();

//	private DisplayMetrics dm;

	private TextRenderer textRenderer;

//	private FileWriter fileWriter;
//	private FileWriter binOut;
//	private StringBuffer multiDraw = new StringBuffer();
//	private int arrayIndex = 0;

	public class MapDataObjectPrimitive {
		BinaryMapDataObject obj;
		int typeInd;
		double order;
		int objectType;
	};

	private static class IconDrawInfo {
		float x = 0;
		float y = 0;
		String resId_1;
		String resId;
		String resId2;
		String resId3;
		String resId4;
		String resId5;
		String shieldId;
		int iconOrder;
		float iconSize;
	}
	
	public static class TextInfo {
		public String mText;
		public Path2D path;
	}
	
	public static class RenderingResult {
		public List<TextInfo> effectiveTextObjects = new ArrayList<TextInfo>();
	}

	/* package */
	public static class RenderingContext extends net.osmand.RenderingContext {
		List<TextDrawInfo> textToDraw = new ArrayList<TextDrawInfo>();
		List<IconDrawInfo> iconsToDraw = new ArrayList<IconDrawInfo>();
		RenderingResult result = new RenderingResult();
		Stroke[] oneWay ;
		Stroke[] reverseOneWay ;

		public RenderingContext() {
		}

		// use to calculate points
		Point2D tempPoint = new Point2D.Double();
		float cosRotateTileSize;
		float sinRotateTileSize;

		int shadowLevelMin = 256;
		int shadowLevelMax = 0;

		boolean ended = false;

		
		@Override
		protected byte[] getIconRawData(String data) {
			return RenderingIcons.getIconRawData(data);
//			return null;
		}
	}

	public OsmandRenderer() {
		textRenderer = new TextRenderer();
//		startWriter();
	}

	public Stroke getDashEffect(float pWidth, RenderingContext rc, float[] cachedValues, float st, int cap){
		float[] dashes = new float[cachedValues.length / 2];
		for (int i = 0; i < dashes.length; i++) {
			dashes[i] = rc.getDensityValue(cachedValues[i * 2]) + cachedValues[i * 2 + 1];
		}
		return new BasicStroke(pWidth, cap, BasicStroke.JOIN_BEVEL, 1.0f, dashes, st);
//		if(!dashEffect.containsKey(dashes)){
//			dashEffect.put(dashes,
//					new BasicStroke(pWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, dashes, st));
//		}
//		return dashEffect.get(dashes);
	}

	public TexturePaint getShader(String resId){
		
		if(shaders.get(resId) == null){
			BufferedImage bmp = RenderingIcons.getIcon(resId, true);
			if(bmp != null){
				TexturePaint sh = new TexturePaint(bmp, new Rectangle2D.Float(0f, 0f, bmp.getWidth(), bmp.getHeight()));
				shaders.put(resId, sh);
			} else {
				shaders.put(resId, null);
			}
		}	
		return shaders.get(resId);
	}
	
	private void put(TIntObjectHashMap<TIntArrayList> map, int k, int v){
		if(!map.containsKey(k)){
			map.put(k, new TIntArrayList());
		}
		map.get(k).add(v);
	}


	void drawObject(RenderingContext rc,  Graphics2D pGraphics2d, RenderingRuleSearchRequest req,
			List<MapDataObjectPrimitive> array, int objOrder) {
			//double polygonLimit = 100;
			//float orderToSwitch = 0;
			double minPolygonSize = 1. / rc.polygonMinSizeToDisplay;
			for (int i = 0; i < array.size(); i++) {
				rc.allObjects++;
				BinaryMapDataObject mObj = array.get(i).obj;
				TagValuePair pair = mObj.getMapIndex().decodeType(mObj.getTypes()[array.get(i).typeInd]);
				if (objOrder == 0) {
					if (array.get(i).order > minPolygonSize + ((int) array.get(i).order)) {
						continue;
					}
					// polygon
					drawPolygon(mObj, req, pGraphics2d, rc, pair);
				} else if (objOrder == 1 || objOrder == 2) {
					drawPolyline(mObj, req, pGraphics2d, rc, pair, mObj.getSimpleLayer(), objOrder == 1);
				} else if (objOrder == 3) {
					drawPoint(mObj, req, pGraphics2d, rc, pair, array.get(i).typeInd == 0);
				}
				if (i % 25 == 0 && rc.interrupted) {
					return;
				}
			}
		}
	
	public void generateNewBitmap(RenderingContext rc, List<BinaryMapDataObject> objects, Graphics2D pGraphics2d, 
				RenderingRuleSearchRequest render, IntermediateImageListener pListener) {
		long now = System.currentTimeMillis();
		// fill area
		if (rc.defaultColor != 0) {
			pGraphics2d.setColor(createColor(rc.defaultColor));
		}
		if (objects != null && !objects.isEmpty() && rc.width > 0 && rc.height > 0) {
			rc.cosRotateTileSize = (float) (Math.cos((float) Math.toRadians(rc.rotate)) * TILE_SIZE);
			rc.sinRotateTileSize = (float) (Math.sin((float) Math.toRadians(rc.rotate)) * TILE_SIZE);
			
			// put in order map
			List<MapDataObjectPrimitive>  pointsArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			List<MapDataObjectPrimitive> polygonsArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			List<MapDataObjectPrimitive>  linesArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
			sortObjectsByProperOrder(rc, objects, render, pointsArray, polygonsArray, linesArray);

			rc.lastRenderedKey = 0;
			drawObject(rc, pGraphics2d, render, polygonsArray, 0);
//			pListener.propagateImage();
			rc.lastRenderedKey = 5;
			if (rc.shadowRenderingMode > 1) {
				drawObject(rc, pGraphics2d, render, linesArray, 1);
			}
			rc.lastRenderedKey = 40;
			drawObject(rc, pGraphics2d, render, linesArray, 2);
			rc.lastRenderedKey = 60;
			pListener.propagateImage();

			drawObject(rc, pGraphics2d, render, pointsArray, 3);
			rc.lastRenderedKey = 125;
			pListener.propagateImage();


			long beforeIconTextTime = System.currentTimeMillis() - now;
			drawIconsOverCanvas(rc, pGraphics2d);

			textRenderer.drawTextOverCanvas(rc, pGraphics2d, rc.preferredLocale);

			long time = System.currentTimeMillis() - now;
			rc.renderingDebugInfo = String.format("Rendering: %s ms  (%s text)\n"
					+ "(%s points, %s points inside, %s of %s objects visible)",//$NON-NLS-1$
					time, time - beforeIconTextTime, rc.pointCount, rc.pointInsideCount, rc.visible, rc.allObjects);
			log.debug(rc.renderingDebugInfo);

		}
//		closeWriter();
//		fileWriter = null;
	}

//	public float getDensity(){
//		return dm.density;
//	}

	private void drawIconsOverCanvas(RenderingContext rc, Graphics2D pGraphics2d) {
		// 1. Sort text using text order
		Collections.sort(rc.iconsToDraw, new Comparator<IconDrawInfo>() {
			@Override
			public int compare(IconDrawInfo object1, IconDrawInfo object2) {
				return object1.iconOrder - object2.iconOrder;
			}
		});
		QuadRect bounds = new QuadRect(0, 0, rc.width, rc.height);
		bounds.inset(-bounds.width()/4, -bounds.height()/4);
		QuadTree<Rectangle2D> boundIntersections = new QuadTree<Rectangle2D>(bounds, 4, 0.6f);
		List<Rectangle2D> result = new ArrayList<Rectangle2D>();
		
		for (IconDrawInfo icon : rc.iconsToDraw) {
			if (icon.resId != null) {
				BufferedImage ico = RenderingIcons.getIcon(icon.resId, true);
				if (ico != null) {
					if (icon.y >= 0 && icon.y < rc.height && icon.x >= 0 && icon.x < rc.width) {
						int visbleWidth = icon.iconSize >= 0 ? (int) icon.iconSize : ico.getWidth();
						int visbleHeight = icon.iconSize >= 0 ? (int) icon.iconSize : ico.getHeight();
						boolean intersects = false;
						float coeff = rc.getDensityValue(rc.screenDensityRatio * rc.textScale);
						Rectangle2D rf = calculateRect(rc, icon, ico.getWidth(), ico.getHeight());
						Rectangle2D visibleRect = null;
						if (visbleHeight > 0 && visbleWidth > 0) {
							visibleRect = calculateRect(rc, icon, visbleWidth, visbleHeight);
							boundIntersections.queryInBox(new QuadRect(visibleRect.getMinX(), visibleRect.getMinY(), visibleRect.getMaxX(), visibleRect.getMaxY()), result);
							for (Rectangle2D r : result) {
								if (r.intersects(visibleRect.getMinX(), visibleRect.getMinY(), visibleRect.getMaxX(), visibleRect.getMaxY())) {
									intersects = true;
									break;
								}
							}
						}
						
						if (!intersects) {
							BufferedImage shield = icon.shieldId == null ? null : RenderingIcons.getIcon(icon.shieldId, true);
							if(shield != null) {
								Rectangle2D shieldRf = calculateRect(rc, icon, shield.getWidth(), shield.getHeight());
								if (coeff != 1f) {
									Rectangle2D src = new Rectangle2D.Float(0, 0, shield.getWidth(), shield.getHeight());
									drawBitmap(pGraphics2d, shield, shieldRf, src);
								} else {
									drawBitmap(pGraphics2d, shield, shieldRf);
								}	
							}
							if (coeff != 1f) {
								Rectangle2D src = new Rectangle2D.Float(0, 0, ico.getWidth(), ico.getHeight());
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId_1, true), rf, src);
								drawBitmap(pGraphics2d, ico, rf, src);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId2, true), rf, src);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId3, true), rf, src);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId4, true), rf, src);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId5, true), rf, src);
							} else {
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId_1, true), rf);
								drawBitmap(pGraphics2d, ico, rf);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId2, true), rf);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId3, true), rf);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId4, true), rf);
								drawBitmap(pGraphics2d, RenderingIcons.getIcon(icon.resId5, true), rf);
							}
							if(visibleRect != null) {
								visibleRect.add(-visibleRect.getWidth() / 4, -visibleRect.getHeight() / 4);
								boundIntersections.insert(visibleRect, 
										new QuadRect(visibleRect.getMinX(), visibleRect.getMinY(), visibleRect.getMaxX(), visibleRect.getMaxY()));
							}
						}
					}
				}
			}
			if (rc.interrupted) {
				return;
			}
		}
	}

	protected void drawBitmap(Graphics2D pGraphics2d, BufferedImage ico, Rectangle2D rf) {
		if(ico == null) {
			return;
		}
		pGraphics2d.drawImage(ico, (int)rf.getX(), (int)rf.getY(), null);
//		cv.drawBitmap(ico, rf.left, rf.top, paintIcon);
	}

	protected void drawBitmap(Graphics2D pGraphics2d, BufferedImage ico, Rectangle2D rf, Rectangle2D src) {
		if(ico == null) {
			return;
		}
		pGraphics2d.drawImage(ico, (int) rf.getX(), (int) rf.getY(), (int) rf.getMaxX(), (int) rf.getMaxY(),
				(int) src.getMinX(), (int) src.getMinY(), (int) src.getMaxX(), (int) src.getMaxY(), null);
		//		cv.drawBitmap(ico, src, rf, paintIcon);
	}

	private Rectangle2D calculateRect(RenderingContext rc, IconDrawInfo icon, int visbleWidth, int visbleHeight) {
		Rectangle2D rf;
		float coeff = rc.getDensityValue(rc.screenDensityRatio * rc.textScale);
		float left = icon.x - visbleWidth / 2 * coeff;
		float top = icon.y - visbleHeight / 2 * coeff;
		float width = visbleWidth * coeff;
		float height = visbleHeight * coeff;
		rf = new Rectangle2D.Double(left, top, width, height);
		return rf;
	}
	
	Comparator<MapDataObjectPrimitive> sortByOrder() {
		return new Comparator<MapDataObjectPrimitive>() {

			@Override
			public int compare(MapDataObjectPrimitive i, MapDataObjectPrimitive j) {
				if (i.order == j.order) {
					if (i.typeInd == j.typeInd) {
						if(i.obj.getPointsLength() == j.obj.getPointsLength()) {
							return 0;
						}
						return i.obj.getPointsLength() < j.obj.getPointsLength() ? -1 : 1;
					}
					return i.typeInd < j.typeInd ? -1 : 1;
				}
				return (i.order < j.order ? -1 : 1);
			}

		};
	}
	
	Comparator<MapDataObjectPrimitive> sortPolygonsOrder() {
		return new Comparator<MapDataObjectPrimitive>() {

			@Override
			public int compare(MapDataObjectPrimitive i, MapDataObjectPrimitive j) {
				if (i.order == j.order)
					return i.typeInd < j.typeInd ? -1 : 1;
				return (i.order > j.order) ? -1 : 1;
			}
		};
	}

	private void sortObjectsByProperOrder(RenderingContext rc, List<BinaryMapDataObject> objects,
			RenderingRuleSearchRequest render, 
			List<MapDataObjectPrimitive>  pointsArray, List<MapDataObjectPrimitive> polygonsArray,
			List<MapDataObjectPrimitive>  linesResArray) {
		int sz = objects.size();
		List<MapDataObjectPrimitive> linesArray = new ArrayList<OsmandRenderer.MapDataObjectPrimitive>();
		if (render != null) {
			render.clearState();

			float mult = (float) (1. / MapUtils.getPowZoom(Math.max(31 - (rc.zoom + 8), 0)));
			for (int i = 0; i < sz; i++) {
				BinaryMapDataObject o = objects.get(i);
				for (int j = 0; j < o.getTypes().length; j++) {
					int wholeType = o.getTypes()[j];
					int layer = 0;
					if (o.getPointsLength() > 1) {
						layer = o.getSimpleLayer();
					}

					TagValuePair pair = o.getMapIndex().decodeType(wholeType);
					if (pair != null) {
						render.setTagValueZoomLayer(pair.tag, pair.value, rc.zoom, layer, o);
						render.setBooleanFilter(render.ALL.R_AREA, o.isArea());
						render.setBooleanFilter(render.ALL.R_POINT, o.getPointsLength() == 1);
						render.setBooleanFilter(render.ALL.R_CYCLE, o.isCycle());
						if (render.search(RenderingRulesStorage.ORDER_RULES)) {
							int objectType = render.getIntPropertyValue(render.ALL.R_OBJECT_TYPE);
							int order = render.getIntPropertyValue(render.ALL.R_ORDER);
							MapDataObjectPrimitive mapObj = new MapDataObjectPrimitive();
							mapObj.objectType = objectType;
							mapObj.order = order;
							mapObj.typeInd = j;
							mapObj.obj = o;
							if(objectType == 3) {
								MapDataObjectPrimitive pointObj = mapObj;
								pointObj.objectType = 1;
								double area = polygonArea(mapObj, mult);
								if(area > MAX_V) { 
									mapObj.order = mapObj.order + (1. / area);
									polygonsArray.add(mapObj);
									pointsArray.add(pointObj); // TODO fix duplicate text? verify if it is needed for icon
								}
							} else if(objectType == 1) {
								pointsArray.add(mapObj);
							} else {
								linesArray.add(mapObj);
							}
							if (render.isSpecified(render.ALL.R_SHADOW_LEVEL)) {
								rc.shadowLevelMin = Math.min(rc.shadowLevelMin, order);
								rc.shadowLevelMax = Math.max(rc.shadowLevelMax, order);
								render.clearValue(render.ALL.R_SHADOW_LEVEL);
							}
						}

					}
				}

				if (rc.interrupted) {
					return;
				}
			}
		}
		Collections.sort(polygonsArray, sortByOrder());
		Collections.sort(pointsArray, sortByOrder());
		Collections.sort(linesArray, sortByOrder());
		filterLinesByDensity(rc, linesResArray, linesArray);
	}
	
	void filterLinesByDensity(RenderingContext rc, List<MapDataObjectPrimitive>  linesResArray,
			List<MapDataObjectPrimitive> linesArray) {
//		int roadsLimit = rc->roadsDensityLimitPerTile;
//		int densityZ = rc->roadDensityZoomTile;
//		if(densityZ == 0 || roadsLimit == 0) {
//			linesResArray = linesArray;
//			return;
//		}
//		linesResArray.reserve(linesArray.size());
//		UNORDERED(map)<int64_t, pair<int, int> > densityMap;
//		for (int i = linesArray.size() - 1; i >= 0; i--) {
//			bool accept = true;
//			int o = linesArray[i].order;
//			MapDataObject* line = linesArray[i].obj;
//			tag_value& ts = line->types[linesArray[i].typeInd];
//			if (ts.first == "highway") {
//				accept = false;
//				int64_t prev = 0;
//				for (uint k = 0; k < line->points.size(); k++) {
//					int dz = rc->getZoom() + densityZ;
//					int64_t x = (line->points[k].first) >> (31 - dz);
//					int64_t y = (line->points[k].second) >> (31 - dz);
//					int64_t tl = (x << dz) + y;
//					if (prev != tl) {
//						prev = tl;
//						pair<int, int>& p = densityMap[tl];
//						if (p.first < roadsLimit/* && p.second > o */) {
//							accept = true;
//							p.first++;
//							p.second = o;
//							densityMap[tl] = p;
//						}
//					}
//				}
//			}
//			if(accept) {
//				linesResArray.push_back(linesArray[i]);
//			}
//		}
//		reverse(linesResArray.begin(), linesResArray.end());
		// TODO
		linesResArray.addAll(linesArray);
	}

	private double polygonArea(MapDataObjectPrimitive mapObj, float mult) {
		double area = 0.;
		int j = mapObj.obj.getPointsLength() - 1;
		for (int i = 0; i < mapObj.obj.getPointsLength(); i++) {
			int px = mapObj.obj.getPoint31XTile(i);
			int py = mapObj.obj.getPoint31YTile(i);
			int sx = mapObj.obj.getPoint31XTile(j);
			int sy = mapObj.obj.getPoint31YTile(j);
			area += (sx + ((float) px)) * (sy - ((float) py));
			j = i;
		}
		return Math.abs(area) * mult * mult * .5;
	}

	private Point2D calcPoint(int xt, int yt, RenderingContext rc){
		rc.pointCount ++;
		double tx = xt / rc.tileDivisor;
		double ty = yt / rc.tileDivisor;
		double dTileX = (tx - rc.leftX);
		double dTileY = (ty - rc.topY);
		float x = (float) (rc.cosRotateTileSize * dTileX - rc.sinRotateTileSize * dTileY);
		float y = (float) (rc.sinRotateTileSize * dTileX + rc.cosRotateTileSize * dTileY);
		rc.tempPoint.setLocation(x, y);
		if(rc.tempPoint.getX() >= 0 && rc.tempPoint.getX() < rc.width && 
				rc.tempPoint.getY() >= 0 && rc.tempPoint.getY() < rc.height){
			rc.pointInsideCount++;
		}
		return rc.tempPoint;
	}
	
	private Point2D calcPoint(BinaryMapDataObject o, int ind, RenderingContext rc){
		return calcPoint(o.getPoint31XTile(ind), o.getPoint31YTile(ind), rc);
	}


	public void clearCachedResources(){
		shaders.clear();
	}
	
	private void drawPolygon(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Graphics2D pGraphics2d, RenderingContext rc, TagValuePair pair) {
		if(render == null || pair == null){
			return;
		}
		float xText = 0;
		float yText = 0;
		int zoom = rc.zoom;
		Path2D path = null;
		Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();
		
//		rc.main.color = Color.rgb(245, 245, 245);
		render.setInitialTagValueZoom(pair.tag, pair.value, zoom, obj);
		boolean rendered = render.search(RenderingRulesStorage.POLYGON_RULES);
		if(!rendered || !updatePaint(render, newGraphics, 0, true, rc)){
			newGraphics.dispose();
			return;
		}
		rc.visible++;
		int len = obj.getPointsLength();
//		if(len > 150) {
//			int[] ts = obj.getTypes();
//			System.err.println("Polygon " + len);
//			for(int i=0; i<ts.length; i++) {
//				System.err.println(obj.getMapIndex().decodeType(ts[i]));
//			}
//			return;
//		}
		for (int i = 0; i < obj.getPointsLength(); i++) {

			Point2D p = calcPoint(obj, i, rc);
			xText += p.getX();
			yText += p.getY();
			if (path == null) {
				path = new Path2D.Float();
				path.moveTo(p.getX(), p.getY());
			} else {
				path.lineTo(p.getX(), p.getY());
			}
		}
		int[][] polygonInnerCoordinates = obj.getPolygonInnerCoordinates();
		if (polygonInnerCoordinates != null && path != null) {
			path.setWindingRule(Path2D.WIND_EVEN_ODD);
			for (int j = 0; j < polygonInnerCoordinates.length; j++) {
				for (int i = 0; i < polygonInnerCoordinates[j].length; i += 2) {
					Point2D p = calcPoint(polygonInnerCoordinates[j][i], polygonInnerCoordinates[j][i + 1], rc);
					if(i==0){
						path.moveTo(p.getX(), p.getY());
					} else {
						path.lineTo(p.getX(), p.getY());
					}
				}
			}
		}

		if (path != null && len > 0) {
			newGraphics.fill(path);
			updateAndDraw(render, newGraphics, rc, path, true, false, 1);
			textRenderer.renderText(obj, render, pGraphics2d, rc, pair, xText / len, yText / len, null, null);
		}
		newGraphics.dispose();
	}
	
	public boolean updatePaint(RenderingRuleSearchRequest req, Graphics2D pGraphics, int ind, boolean area, RenderingContext rc){
		Paint p = new Paint();
		boolean res = updatePaint(req, p, ind, area, rc);
		p.updateGraphics(pGraphics);
		return res;
	}

	public boolean updatePaint(RenderingRuleSearchRequest req, Paint p, int ind, boolean area, RenderingContext rc){
		RenderingRuleProperty rColor;
		RenderingRuleProperty rStrokeW;
		RenderingRuleProperty rCap;
		RenderingRuleProperty rPathEff;
		
		if (ind == 0) {
			rColor = req.ALL.R_COLOR;
			rStrokeW = req.ALL.R_STROKE_WIDTH;
			rCap = req.ALL.R_CAP;
			rPathEff = req.ALL.R_PATH_EFFECT;
		} else if(ind == 1){
			rColor = req.ALL.R_COLOR_2;
			rStrokeW = req.ALL.R_STROKE_WIDTH_2;
			rCap = req.ALL.R_CAP_2;
			rPathEff = req.ALL.R_PATH_EFFECT_2;
		} else if(ind == -1){
			rColor = req.ALL.R_COLOR_0;
			rStrokeW = req.ALL.R_STROKE_WIDTH_0;
			rCap = req.ALL.R_CAP_0;
			rPathEff = req.ALL.R_PATH_EFFECT_0;
		} else if(ind == -2){
			rColor = req.ALL.R_COLOR__1;
			rStrokeW = req.ALL.R_STROKE_WIDTH__1;
			rCap = req.ALL.R_CAP__1;
			rPathEff = req.ALL.R_PATH_EFFECT__1;
		} else if(ind == 2){
			rColor = req.ALL.R_COLOR_3;
			rStrokeW = req.ALL.R_STROKE_WIDTH_3;
			rCap = req.ALL.R_CAP_3;
			rPathEff = req.ALL.R_PATH_EFFECT_3;
		} else if(ind == -3){
			rColor = req.ALL.R_COLOR__2;
			rStrokeW = req.ALL.R_STROKE_WIDTH__2;
			rCap = req.ALL.R_CAP__2;
			rPathEff = req.ALL.R_PATH_EFFECT__2;
		} else if(ind == 3){
			rColor = req.ALL.R_COLOR_4;
			rStrokeW = req.ALL.R_STROKE_WIDTH_4;
			rCap = req.ALL.R_CAP_4;
			rPathEff = req.ALL.R_PATH_EFFECT_4;
		} else {
			rColor = req.ALL.R_COLOR_5;
			rStrokeW = req.ALL.R_STROKE_WIDTH_5;
			rCap = req.ALL.R_CAP_5;
			rPathEff = req.ALL.R_PATH_EFFECT_5;
		}
		if(area){
			if(!req.isSpecified(rColor) && !req.isSpecified(req.ALL.R_SHADER)){
				return false;
			}
			p.setShader(null);
			p.setColorFilter(null);
			p.clearShadowLayer();
			p.setStyle(Style.FILL_AND_STROKE);
			p.setStrokeWidth(0);
		} else {
			if(!req.isSpecified(rStrokeW)){
				return false;
			}
			p.setShader(null);
			p.setColorFilter(null);
			p.clearShadowLayer();
			p.setStyle(Style.STROKE);
			float width = rc.getComplexValue(req, rStrokeW);
			width = Math.max(0f, width);
			int capValue = BasicStroke.CAP_BUTT;
			String cap = req.getStringPropertyValue(rCap);
			if(!Algorithms.isEmpty(cap)){
				capValue = Cap.valueOf(cap.toUpperCase()).getVal();
			}
			String pathEffect = req.getStringPropertyValue(rPathEff);
			if (!Algorithms.isEmpty(pathEffect)) {
				if(!parsedDashEffects.containsKey(pathEffect)) {
					String[] vls = pathEffect.split("_");
					float[] vs = new float[vls.length * 2];
					for(int i = 0; i < vls.length; i++) {
						int s = vls[i].indexOf(':');
						String pre = vls[i];
						String post = "";
						if(s != -1) {
							pre = vls[i].substring(0, i);
							post = vls[i].substring(i + 1);
						}
						if(pre.length() > 0) {
							vs[i*2 ] = Float.parseFloat(pre);
						}
						if(post.length() > 0) {
							vs[i*2 +1] = Float.parseFloat(post);
						}
					}
					parsedDashEffects.put(pathEffect, vs);
				}
				float[] cachedValues = parsedDashEffects.get(pathEffect);
				
				p.setStroke(getDashEffect(width, rc, cachedValues, 0, capValue));
			} else {
				p.setStroke(new BasicStroke(width, capValue, BasicStroke.JOIN_ROUND));
			}
		}
		p.setColor(createColor(req.getIntPropertyValue(rColor)));
		if(ind == 0){
			String resId = req.getStringPropertyValue(req.ALL.R_SHADER);
			if(resId != null){
				if(req.getIntPropertyValue(rColor) == 0) {
					p.setColor(Color.WHITE); // set color required by skia
				}
				p.setPaint(getShader(resId));
			}
			// do not check shadow color here
			if(rc.shadowRenderingMode == 1) {
				int shadowColor = req.getIntPropertyValue(req.ALL.R_SHADOW_COLOR);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				throw new IllegalArgumentException("Shadow not implemented here");
//				int shadowRadius = (int) rc.getComplexValue(req, req.ALL.R_SHADOW_RADIUS);
//				if (shadowColor == 0) {
//					shadowRadius = 0;
//				}
//				pGraphics2d.setShadowLayer(shadowRadius, 0, 0, shadowColor);
//				pGraphics2d.setColor(createColor(shadowColor));
			}
		}
		
		return true;
		
	}

	public Color createColor(int colorInt) {
		return ColorUtils.createARGB(colorInt);
	}
	

	private void drawPoint(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Graphics2D pGraphics2d, RenderingContext rc, TagValuePair pair, boolean renderText) {
		if(render == null || pair == null){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom, obj);
		render.search(RenderingRulesStorage.POINT_RULES);
		
		String resId = render.getStringPropertyValue(render.ALL.R_ICON);
		if(resId == null && !renderText){
			return;
		}
		int len = obj.getPointsLength();
		rc.visible++;
		Point2D ps = new Point2D.Double(0, 0);
		for (int i = 0; i < len; i++) {
			Point2D p = calcPoint(obj, i, rc);
			ps.setLocation(ps.getX() + p.getX(), ps.getY()+p.getY()); 
		}
		if(len > 1){
			ps.setLocation(ps.getX()/len, ps.getY()/len); 
		}

		if(resId != null){
			IconDrawInfo ico = new IconDrawInfo();
			ico.x = (float) ps.getX();
			ico.y = (float) ps.getY();
			ico.iconOrder = render.getIntPropertyValue(render.ALL.R_ICON_ORDER, 100);
			ico.iconSize = rc.getComplexValue(render, render.ALL.R_ICON_VISIBLE_SIZE, -1);
			ico.shieldId = render.getStringPropertyValue(render.ALL.R_SHIELD);
			ico.resId_1 = render.getStringPropertyValue(render.ALL.R_ICON__1);
			ico.resId = resId;
			ico.resId2 = render.getStringPropertyValue(render.ALL.R_ICON_2);
			ico.resId3 = render.getStringPropertyValue(render.ALL.R_ICON_3);
			ico.resId4 = render.getStringPropertyValue(render.ALL.R_ICON_4);
			ico.resId5 = render.getStringPropertyValue(render.ALL.R_ICON_5);
			rc.iconsToDraw.add(ico);
		}
		if (renderText) {
			textRenderer.renderText(obj, render, pGraphics2d, rc, pair, ps.getX(), ps.getY(), null, null);
		}

	}

	private void drawPolylineShadow(Graphics2D pGraphics2d, RenderingContext rc, Path2D pPath, int shadowColor, int shadowRadius) {
		// blurred shadows
		if (rc.shadowRenderingMode == 2 && shadowRadius > 0) {
			drawPath(pGraphics2d, pPath);
			throw new IllegalArgumentException("Shadow type not implemented");
		}

		// option shadow = 3 with solid border
		if (rc.shadowRenderingMode == 3 && shadowRadius > 0) {
			Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();
			if (newGraphics.getStroke() instanceof BasicStroke) {
				BasicStroke bs = (BasicStroke) newGraphics.getStroke();
				newGraphics.setStroke(new BasicStroke(bs.getLineWidth() + shadowRadius * 2));
			}
			newGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN));
			newGraphics.setColor(createColor(shadowColor));
			drawPath(newGraphics, pPath);
			newGraphics.dispose();
		}
	}

	
	private void drawPolyline(BinaryMapDataObject obj, RenderingRuleSearchRequest render, Graphics2D pGraphics2d, RenderingContext rc, TagValuePair pair, int layer,
			boolean drawOnlyShadow) {
		if(render == null || pair == null ){
			return;
		}
		int length = obj.getPointsLength();
		if(length < 2){
			return;
		}
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom, obj);
		render.setIntFilter(render.ALL.R_LAYER, layer);
		boolean rendered = render.search(RenderingRulesStorage.LINE_RULES);
		if(!rendered || !updatePaint(render, pGraphics2d, 0, false, rc)){
			return;
		}
		int oneway = 0;
		if(rc.zoom >= 16 && "highway".equals(pair.tag) ){ //$NON-NLS-1$
			if(obj.containsAdditionalType(obj.getMapIndex().onewayAttribute)) {
				oneway = 1;
			} else if(obj.containsAdditionalType(obj.getMapIndex().onewayReverseAttribute)){
				oneway = -1;
			}
		}

		rc.visible++;

		Path2D path = null;
		float xMid = 0;
		float yMid = 0;
		int middle = obj.getPointsLength() / 2;
		Point2D[] textPoints = null;
		if (!drawOnlyShadow) {
			textPoints = new Point2D[length];
		}

		boolean intersect = false;
		Point2D prev = null;
		for (int i = 0; i < length ; i++) {
			Point2D p = calcPoint(obj, i, rc);
			if(textPoints != null) {
				textPoints[i] = new Point2D.Double(p.getX(), p.getY());
			}
			if (!intersect) {
				if (p.getX() >= 0 && p.getY() >= 0 && p.getX() < rc.width && p.getY() < rc.height) {
					intersect = true;
				}
				if (!intersect && prev != null) {
					if ((p.getX() < 0 && prev.getX() < 0) || (p.getY() < 0 && prev.getY() < 0) || (p.getX() > rc.width && prev.getX() > rc.width)
							|| (p.getY() > rc.height && prev.getY() > rc.height)) {
						intersect = false;
					} else {
						intersect = true;
					}

				}
			}
			if (path == null) {
				path = new Path2D.Float();
				path.moveTo(p.getX(), p.getY());
			} else {
				if(i == middle){
					xMid = (float) p.getX();
					yMid = (float) p.getY();
				}
				path.lineTo(p.getX(), p.getY());
			}
			prev = p;
		}
		if (!intersect) {
//			System.err.println("Not intersect ");
//			int[] ts = obj.getTypes();
//			for(int i=0; i<ts.length; i++) {
//				System.err.println(obj.getMapIndex().decodeType(ts[i]));
//			}
			return;
		}
		if (path != null) {
			if(drawOnlyShadow) {
				int shadowColor = render.getIntPropertyValue(render.ALL.R_SHADOW_COLOR);
				int shadowRadius = (int) rc.getComplexValue(render, render.ALL.R_SHADOW_RADIUS);
				if(shadowColor == 0) {
					shadowColor = rc.shadowRenderingColor;
				}
				drawPolylineShadow(pGraphics2d, rc, path, shadowColor, shadowRadius);
			} else {
				boolean update = false;
				update = updateAndDraw(render, pGraphics2d, rc, path, true, false, -3);
				update = updateAndDraw(render, pGraphics2d, rc, path, true, false, -2);
				update = updateAndDraw(render, pGraphics2d, rc, path, true, false, -1);
				update = updateAndDraw(render, pGraphics2d, rc, path, update, true, 0);
				updateAndDraw(render, pGraphics2d, rc, path, true, false, 1);
				updateAndDraw(render, pGraphics2d, rc, path, true, false, 2);
				updateAndDraw(render, pGraphics2d, rc, path, true, false, 3);
				updateAndDraw(render, pGraphics2d, rc, path, true, false, 4);
			}
			
			if(oneway != 0 && !drawOnlyShadow){
				Stroke oldStroke = pGraphics2d.getStroke();
				Stroke[] strokes = oneway == -1? getReverseOneWayPaints(rc) :  getOneWayPaints(rc);
				for (int i = 0; i < strokes.length; i++) {
					pGraphics2d.setColor(createColor(0xff6c70d5));
					pGraphics2d.setStroke(strokes[i]);
					drawPath(pGraphics2d, path);
				}
				pGraphics2d.setStroke(oldStroke);
			}
			if (textPoints != null) {
				textRenderer.renderText(obj, render, pGraphics2d, rc, pair, xMid, yMid, path, textPoints);
			}
		}
	}

	private boolean updateAndDraw(RenderingRuleSearchRequest render, Graphics2D pGraphics2d, RenderingContext rc,
			Path2D path, boolean doUpdate, boolean pDontCheckUpdateResult, int pInd) {
		boolean update = false;
		Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();
		if (doUpdate){
			update = updatePaint(render, newGraphics, pInd, false, rc);
		}
		if(pDontCheckUpdateResult || update){
			drawPath(newGraphics, path);
		}
		newGraphics.dispose();
		return update;
	}

	private void drawPath(Graphics2D pGraphics2d, Path2D pPath) {
//		PathIterator it = pPath.getPathIterator(null);
//		boolean started = false;
//		int counter = 0;
//		while (!it.isDone()) {
//			float[] coords = new float[6];
//			int seg = it.currentSegment(coords);
//			switch(seg){
//			case PathIterator.SEG_MOVETO:
//				if(started){
//					finalizeDraw(counter);
//				}
//				started = true;
//				counter  = 0;
//			case PathIterator.SEG_LINETO:
//				write(coords[0]+"\n");
//				write(coords[1]+"\n");
//				counter++;
//				break;
//			case PathIterator.SEG_CLOSE:
//				break;
//			}
//			it.next();
//		} 
//		finalizeDraw(counter);
		pGraphics2d.draw(pPath);
	}

//	private void startWriter() {
//		try {
//			fileWriter = new FileWriter("Coords.java");
//			binOut = new FileWriter("coords.bin");
//			fileWriter.write("import java.nio.FloatBuffer;\n"
//					+ "public class Coords {\n"
//					+ "public static FloatBuffer coords = FloatBuffer.allocate(AMOUNT);\n static {\n");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//
//	
//	private void finalizeDraw(int counter) {
//		multiDraw.append("  " + counter + ", 1, " + arrayIndex + ", 0, \n" );
//		arrayIndex += counter;
//	}
//
//	private void write(String str) {
//		if (fileWriter != null) {
//			try {
//				binOut.write(str);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	private void closeWriter() {
//		if (fileWriter != null) {
//			try {
//				fileWriter.write("}\n public static int[] multi = new int[]{\n");
//				fileWriter.write(multiDraw.toString());
//				fileWriter.write("};\n public final int AMOUNT=" + arrayIndex + ";\n\n}\n");
//				fileWriter.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} 
//		}
//	}
//
//
		
	private static Stroke oneWayPaint(float pWidth, DashPathEffect pDashEffect){
		return new BasicStroke(pWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, pDashEffect.getDashes(), pDashEffect.getDashPhase());
//		oneWay.setStyle(Style.STROKE);
//		oneWay.setColor(0xff6c70d5);
//		oneWay.setAntiAlias(true);
	}
	
	public Stroke[] getReverseOneWayPaints(RenderingContext rc){
		if(rc.reverseOneWay == null){
			int rmin = (int)rc.getDensityValue(1);
			if(rmin > 2) {
				rmin = rmin / 2;
			}
			DashPathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10 * rmin, 152 }, 0);
			DashPathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12 + rmin, 9 * rmin, 152 }, 1);
			DashPathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 12 + 2 * rmin, 2 * rmin, 152 + 6 * rmin }, 1);
			DashPathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 12 + 3 * rmin, 1 * rmin, 152 + 6 * rmin }, 1);
			rc.reverseOneWay = new Stroke[4];
			rc.reverseOneWay[0] = oneWayPaint(rmin*2, arrowDashEffect1);
			rc.reverseOneWay[1] = oneWayPaint(rmin, arrowDashEffect2);
			rc.reverseOneWay[2] = oneWayPaint(rmin * 3, arrowDashEffect3);
			rc.reverseOneWay[3] = oneWayPaint(rmin*4, arrowDashEffect4);			
			
		}
		return rc.reverseOneWay;
	}
	
	public Stroke[] getOneWayPaints(RenderingContext rc){
		if(rc.oneWay == null){
			float rmin = rc.getDensityValue(1);
			if(rmin > 1) {
				rmin = rmin * 2 / 3;
			}
			DashPathEffect arrowDashEffect1 = new DashPathEffect(new float[] { 0, 12, 10 * rmin, 152 }, 0);
			DashPathEffect arrowDashEffect2 = new DashPathEffect(new float[] { 0, 12, 9 * rmin, 152 + rmin }, 1);
			DashPathEffect arrowDashEffect3 = new DashPathEffect(new float[] { 0, 12 + 6 * rmin, 2 * rmin, 152 + 2 * rmin}, 1);
			DashPathEffect arrowDashEffect4 = new DashPathEffect(new float[] { 0, 12 + 6 * rmin, 1 * rmin, 152 + 3 * rmin}, 1);
			rc.oneWay = new Stroke[4];
			rc.oneWay[0] = oneWayPaint(rmin, arrowDashEffect1);
			rc.oneWay[1] = oneWayPaint(rmin*2, arrowDashEffect2);
			rc.oneWay[2] = oneWayPaint(rmin*3, arrowDashEffect3);
			rc.oneWay[3] = oneWayPaint(rmin*4, arrowDashEffect4);			
		}
		return rc.oneWay;
	}
}
