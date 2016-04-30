package net.sourceforge.offroad.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.ValueHolder;
import net.osmand.binary.OsmandIndex;
import net.osmand.binary.OsmandIndex.MapLevel;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.RouteLayer;
import net.sourceforge.offroad.OsmWindow;

@SuppressWarnings("serial")
public class OsmBitmapPanel extends JPanel implements IRouteInformationListener {
	private final static Log log = PlatformUtil.getLog(OsmBitmapPanel.class);

	private static final int ST_WIDTH = 1400;
	private static final int ST_HEIGHT = 1000;
	private static final Color BACKGROUND_COLOR = Color.white;
	private static final float STROKE_WIDTH = 6f;
	private static final Stroke STROKE = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Color[] colors = { Color.black, Color.blue, Color.red, Color.green, Color.orange,
			Color.MAGENTA };

	private BufferedImage bImage = new BufferedImage(ST_WIDTH, ST_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private Color color = Color.black;
	private ArrayList<Point> points = new ArrayList<Point>();
	private int colorIndex = 0;
	private OsmWindow mContext;
	private RotatedTileBox mTileBox;
	private RotatedTileBox mCurrentTileBox;
	float scale = 1.0f;
	int originX = 0;
	int originY = 0;
	private OffRoadUIThread mAnimationThread;
	private GenerationThread mGenerationThread;
	private boolean mShowCursor;
	private LatLon mCursorPosition = null;
	private int mCursorLength = 20;
	private BasicStroke mStroke;
	private double mRotation = 0;
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	private Map<OsmandMapLayer, Float> zOrders = new HashMap<OsmandMapLayer, Float>();
	private POIMapLayer mPoiLayer;
	private ExecutorService mThreadPool;
	private OffRoadUIThread mLastThread;
	private InactivityListener mInactivityListener;
	private CalculateUnzoomedPicturesAction mUnzoomedPicturesAction;

	public OsmBitmapPanel(OsmWindow pWin) {
		mContext = pWin;
		clear(bImage);
		LatLon latLon = new LatLon(51.03325, 13.64656);
		int zoom = 17;
		if (mContext.getSettings().isLastKnownMapLocation()) {
			latLon = mContext.getSettings().getLastKnownMapLocation();
			zoom = mContext.getSettings().getLastKnownMapZoom();
		}
		setTileBox(new RotatedTileBoxBuilder().setLocation(latLon.getLatitude(), latLon.getLongitude()).setZoom(zoom)
				.setPixelDimensions(bImage.getWidth(), bImage.getHeight()).setRotate(0).setMapDensity(1d).build());
		mCurrentTileBox = mTileBox;
		mCursorLength = (int) (15 * mTileBox.getMapDensity());
		mStroke = new BasicStroke((float) (2f * mTileBox.getMapDensity()));
		addLayer(new RouteLayer(mContext.getRoutingHelper()), 1);
		addLayer(new MapTextLayer(), 2);
		mPoiLayer = new POIMapLayer(pWin);
		addLayer(mPoiLayer, 3);
		for (OsmandMapLayer layer : layers) {
			layer.initLayer(this);
		}
		mContext.getRoutingHelper().addListener(this);
		Action updateCursorAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mShowCursor = !mShowCursor;
				repaint();
			}
		};
		new Timer(1000, updateCursorAction).start();
		mThreadPool = Executors.newFixedThreadPool(4);
	}

	public void init() {
		mUnzoomedPicturesAction = new CalculateUnzoomedPicturesAction();
		mInactivityListener = new InactivityListener(mContext.getWindow(), mUnzoomedPicturesAction);
		mInactivityListener.setIntervalInMillis(5000);
		mInactivityListener.start();
	}


	public float getScaleCoefficient() {
		float scaleCoefficient = getDensity();
		OsmWindow dm = mContext;
		if (Math.min(dm.widthPixels / (dm.density * 160), dm.heightPixels / (dm.density * 160)) > 2.5f) {
			// large screen
			scaleCoefficient *= 1.5f;
		}
		return scaleCoefficient;
	}

	public float getDensity() {
		synchronized (mTileBox) {
			return mTileBox.getDensity();
		}
	}

	private void clear(BufferedImage pImage) {
		Graphics g = pImage.getGraphics();
		g.setColor(BACKGROUND_COLOR);
		g.fillRect(0, 0, pImage.getWidth(), pImage.getHeight());
		g.dispose();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D gd2 = (Graphics2D) g;
		Graphics2D g2 = createGraphics(gd2);
		g2.rotate(mRotation, getWidth() / 2, getHeight() / 2);
		g2.translate(originX, originY);
		g2.scale(scale, scale);
		// check for cached picture of bigger size to lay under:
		RotatedTileBox ctb;
		synchronized (mCurrentTileBox) {
			ctb = mCurrentTileBox.copy();
		}
		if(scale < 1.0d || originX != 0 || originY != 0) {
			LatLon screenLT = ctb.getLatLonFromPixel(-originX/scale, -originY/scale);
			LatLon screenRB = ctb.getLatLonFromPixel(-originX/scale + getWidth()/scale, -originY/scale + getHeight()/scale);
			L1: for(int biggerZoom = ctb.getZoom()-1; biggerZoom >= 1; --biggerZoom){
				List<RotatedTileBox> tblist = mUnzoomedPicturesAction.getTileBoxesForZoom(biggerZoom);
				// check for each, if the current image is contained
				for (RotatedTileBox rtb : tblist) {
					if (rtb.containsLatLon(screenLT) && rtb.containsLatLon(screenRB)) {
						//						log.debug("Found bigger tile box " + rtb);
						// draw this under it:
						LatLon rtbLT = rtb.getLeftTopLatLon();
						LatLon rtbRB = rtb.getRightBottomLatLon();
						double x1 = ctb.getPixXFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
						double y1 = ctb.getPixYFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
						double x2 = ctb.getPixXFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
						double y2=  ctb.getPixYFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
						BufferedImage image = mUnzoomedPicturesAction.getImage(rtb);
						if(image != null){
							g2.drawImage(image, (int)x1, (int)y1, (int)x2, (int)y2, 0,0, image.getWidth(), image.getHeight(), null);
							break L1;
						}
					}
				}
			}
		}
		g2.drawImage(bImage, 0, 0, null);
		// cursor:
		Stroke oldStroke = g2.getStroke();
		Color oldColor = g2.getColor();
		// do cursor
		if (mCursorPosition != null && mShowCursor && ctb.containsLatLon(mCursorPosition)) {
			int posx = (int) ctb.getPixXFromLatLon(mCursorPosition.getLatitude(),
					mCursorPosition.getLongitude());
			int posy = (int) ctb.getPixYFromLatLon(mCursorPosition.getLatitude(),
					mCursorPosition.getLongitude());
			int size_h = mCursorLength;
			g2.setStroke(mStroke);
			g2.setColor(Color.RED);
			g2.drawLine(posx - size_h, posy, posx + size_h, posy);
			g2.drawLine(posx, posy - size_h, posx, posy + size_h);
		}
		g2.setColor(oldColor);
		g2.setStroke(oldStroke);
		g2.dispose();
	}

	boolean contains(RotatedTileBox bigger, RotatedTileBox smaller) {
		return bigger.containsLatLon(smaller.getLeftTopLatLon()) && bigger.containsLatLon(smaller.getRightBottomLatLon());
	}

	void setImage(BufferedImage pImage) {
		scale = 1.0f;
		originX = 0;
		originY = 0;
		mRotation = 0;
		bImage = pImage;
		repaint();
	}

	private void drawImage(BufferedImage pImage, RotatedTileBox pTileBox) {
		clear(pImage);
		Graphics2D graphics = pImage.createGraphics();
		Graphics2D g2 = createGraphics(graphics);
		mContext.loadMGap(g2, pTileBox);
		final QuadPoint c = pTileBox.getCenterPixelPoint();
		DrawSettings settings = new DrawSettings(false);
		for (int i = 0; i < layers.size(); i++) {
			Graphics2D glayer = createGraphics(g2);
			try {
				OsmandMapLayer layer = layers.get(i);
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					glayer.rotate(pTileBox.getRotate(), c.x, c.y);
				}
				layer.onPrepareBufferImage(glayer, pTileBox, settings);
				// canvas.restore();
			} catch (IndexOutOfBoundsException e) {
				// skip it
				// canvas.restore();
			}
			glayer.dispose();
		}
		g2.dispose();
		graphics.dispose();
	}

	public Graphics2D createGraphics(Graphics2D graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(bImage.getWidth(), bImage.getHeight());
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void zoomChange(final int pWheelRotation, final Point pNewCenter) {
		final RotatedTileBox tileCopy = copyLatestTileBox();
		int newZoom = tileCopy.getZoom() + pWheelRotation;
		newZoom = checkZoom(newZoom);
		LatLon latLonNewCenter = tileCopy.getLatLonFromPixel(pNewCenter.x, pNewCenter.y);
		tileCopy.setZoom(newZoom);
		final float deltaX = tileCopy.getPixXFromLatLon(latLonNewCenter.getLatitude(), latLonNewCenter.getLongitude())
				- pNewCenter.x;
		final float deltaY = tileCopy.getPixYFromLatLon(latLonNewCenter.getLatitude(), latLonNewCenter.getLongitude())
				- pNewCenter.y;
		// now move the tileCopy that latLonNewCenter is at the same pixel
		// position as before.
		double latFromPixel = tileCopy.getLatFromPixel(tileCopy.getCenterPixelX() + deltaX,
				tileCopy.getCenterPixelY() + deltaY);
		double lonFromPixel = tileCopy.getLonFromPixel(tileCopy.getCenterPixelX() + deltaX,
				tileCopy.getCenterPixelY() + deltaY);
		tileCopy.setLatLonCenter(latFromPixel, lonFromPixel);
		mAnimationThread = new ZoomAnimationThread(this, pWheelRotation, pNewCenter);
		queue(mAnimationThread);
		mGenerationThread = new GenerationThread(this, tileCopy);
		queue(mGenerationThread);
	}

	private RotatedTileBox copyLatestTileBox() {
		synchronized (mTileBox) {
			return mTileBox.copy();
		}
	}

	public void queue(OffRoadUIThread pThread) {
		synchronized (mTileBox) {
			if(pThread.getDestinationTileBox() != null){
				mTileBox = pThread.getDestinationTileBox().copy();
			}
		}
		if(mLastThread != null){
			synchronized(mLastThread){
				if(!mLastThread.hasFinished()){
					mLastThread.setNextThread(pThread);
				} else {
					pThread.shouldContinue();
				}
			}
		} else {
			pThread.shouldContinue();
		}
		mLastThread = pThread;
		mThreadPool.execute(pThread);
		// TODO: Do this for every thread start and end.
		mInactivityListener.eventDispatched(null);
	}

	public int checkZoom(int newZoom) {
		MapLevel mapInstance = OsmandIndex.MapLevel.getDefaultInstance();
		int minZoom = mapInstance.hasMinzoom() ? mapInstance.getMinzoom() : 1;
		if (newZoom < minZoom) {
			newZoom = minZoom;
		}
		int maxZoom = mapInstance.hasMaxzoom() ? mapInstance.getMaxzoom() : OsmWindow.MAX_ZOOM;
		if (newZoom > maxZoom) {
			newZoom = maxZoom;
		}
		return newZoom;
	}

	public void moveImage(float pDeltaX, float pDeltaY) {
		RotatedTileBox tb = copyLatestTileBox();
		QuadPoint center = tb.getCenterPixelPoint();
		double newLat = tb.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY);
		double newLon = tb.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY);
		if (false) {
			BufferedImage image = createImage();
			// create new image
			clear(image);
			Graphics2D graphics2d = image.createGraphics();
			if (pDeltaX > 0) {
				float gapCenterXPoint = tb.getPixWidth() + pDeltaX / 2;
				float gapCenterYPoint = tb.getCenterPixelY() + pDeltaY;
				double gapLat = tb.getLatFromPixel(gapCenterXPoint, gapCenterYPoint);
				double gapLon = tb.getLonFromPixel(gapCenterXPoint, gapCenterYPoint);
				RotatedTileBox gapXBox = new RotatedTileBoxBuilder().density(tb.getDensity()).setZoom(tb.getZoom())
						.setPixelDimensions((int) pDeltaX, tb.getPixHeight()).setLocation(gapLat, gapLon)
						.setMapDensity(tb.getMapDensity()).build();
				float transX = tb.getPixWidth() - pDeltaX;
				graphics2d.translate(transX, 0f);
				mContext.loadMGap(graphics2d, gapXBox);
				graphics2d.translate(-transX, 0f);
			}
			if (pDeltaY > 0) {
				float gapCenterXPoint = tb.getCenterPixelX() + pDeltaX;
				float gapCenterYPoint = tb.getPixHeight() + pDeltaY / 2;
				double gapLat = tb.getLatFromPixel(gapCenterXPoint, gapCenterYPoint);
				double gapLon = tb.getLonFromPixel(gapCenterXPoint, gapCenterYPoint);
				RotatedTileBox gapYBox = new RotatedTileBoxBuilder().density(tb.getDensity()).setZoom(tb.getZoom())
						.setPixelDimensions(tb.getPixWidth(), (int) pDeltaY).setLocation(gapLat, gapLon)
						.setMapDensity(tb.getMapDensity()).build();
				float transY = tb.getPixHeight() - pDeltaY;
				graphics2d.translate(0, transY);
				mContext.loadMGap(graphics2d, gapYBox);
				graphics2d.translate(0, -transY);
			}
			if (pDeltaX < 0) {
				float gapCenterXPoint = pDeltaX / 2;
				float gapCenterYPoint = tb.getCenterPixelY() + pDeltaY;
				double gapLat = tb.getLatFromPixel(gapCenterXPoint, gapCenterYPoint);
				double gapLon = tb.getLonFromPixel(gapCenterXPoint, gapCenterYPoint);
				RotatedTileBox gapXBox = new RotatedTileBoxBuilder().density(tb.getDensity()).setZoom(tb.getZoom())
						.setPixelDimensions((int) -pDeltaX, tb.getPixHeight()).setLocation(gapLat, gapLon)
						.setMapDensity(tb.getMapDensity()).build();
				mContext.loadMGap(graphics2d, gapXBox);
			}
			if (pDeltaY < 0) {
				float gapCenterXPoint = tb.getCenterPixelX() + pDeltaX;
				float gapCenterYPoint = pDeltaY / 2;
				double gapLat = tb.getLatFromPixel(gapCenterXPoint, gapCenterYPoint);
				double gapLon = tb.getLonFromPixel(gapCenterXPoint, gapCenterYPoint);
				RotatedTileBox gapYBox = new RotatedTileBoxBuilder().density(tb.getDensity()).setZoom(tb.getZoom())
						.setPixelDimensions(tb.getPixWidth(), (int) -pDeltaY).setLocation(gapLat, gapLon)
						.setMapDensity(tb.getMapDensity()).build();
				mContext.loadMGap(graphics2d, gapYBox);
			}
			// copy the old image inside:
			graphics2d.drawImage(bImage, (int) -pDeltaX, (int) -pDeltaY, bImage.getWidth(), bImage.getHeight(), null);
			tb.setLatLonCenter(newLat, newLon);
			graphics2d.dispose();
			setImage(image);
		} else {
			tb.setLatLonCenter(newLat, newLon);
			queue(new GenerationThread(this, tb));
//			drawImage(bImage);
//			setImage(bImage);
		}
	}

	public void moveImageAnimated(float pDeltaX, float pDeltaY) {
		pDeltaX *= getWidth();
		pDeltaY *= getHeight();
		System.out.println("Moving by  "  +pDeltaX + ", " + pDeltaY);
		RotatedTileBox tileBox = copyLatestTileBox();
		QuadPoint center = tileBox.getCenterPixelPoint();
		final RotatedTileBox tileCopy = tileBox.copy();
		tileCopy.setLatLonCenter(tileBox.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
				tileBox.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
		mAnimationThread = new MoveAnimationThread(this, pDeltaX, pDeltaY);
		queue(mAnimationThread);
		mGenerationThread = new GenerationThread(this, tileCopy);
		queue(mGenerationThread);
	}

	public BufferedImage newBitmap(RotatedTileBox pTileBox) {
		BufferedImage image = createImage();
		drawImage(image, pTileBox);
		return image;
	}

	public BufferedImage createImage() {
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		return image;
	}

	public void dragImage(Point pTranslate) {
		originX = pTranslate.x;
		originY = pTranslate.y;
		repaint();
	}

	public RotatedTileBox getTileBox() {
		synchronized (mTileBox) {
			return mTileBox.copy();
		}
	}

	public void setTileBox(RotatedTileBox pTileBox) {
		mTileBox = pTileBox;
	}

	public void setCursor(Point pCursorPoint) {
		synchronized (mTileBox) {
			mCursorPosition = mTileBox.getLatLonFromPixel(pCursorPoint.x, pCursorPoint.y);
		}
		System.out.println("Setting cursor to " + mCursorPosition);
		repaint();
	}

	public void move(LatLon pLocation, int pZoom) {
		RotatedTileBox tb = copyLatestTileBox();
		tb.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
		int newZoom = checkZoom(pZoom);
		tb.setZoom(newZoom);
		queue(new GenerationThread(this, tb));
	}

	public void setCursor(LatLon pLocation) {
		mCursorPosition = pLocation;
		repaint();
	}

	public LatLon getCursorPosition() {
		return mCursorPosition;
	}

	public OsmWindow getContext() {
		return mContext;
	}

	@Override
	public void newRouteIsCalculated(boolean pNewRoute, ValueHolder<Boolean> pShowToast) {
		drawLater();
	}

	@Override
	public void routeWasCancelled() {
		drawLater();
	}

	/**
	 */
	public void drawLater() {
		RotatedTileBox tb = copyLatestTileBox();
		tb.setPixelDimensions(getWidth(), getHeight());
		mGenerationThread = new GenerationThread(this, tb);
		queue(mGenerationThread);
	}

	@Override
	public void routeWasFinished() {
	}

	public void saveImage(File pSelectedFile) {
		try {
			ImageIO.write(bImage, "png", pSelectedFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void rotateIncrement(double pPreciseWheelRotation) {
		RotatedTileBox tb = copyLatestTileBox();
		tb.setRotate((float) (tb.getRotate() + 10 * pPreciseWheelRotation));
		queue(new GenerationThread(this, tb));
//		drawImage(bImage);
//		setImage(bImage);
	}

	public void directRotateIncrement(double pPreciseWheelRotation) {
		mRotation += Math.toRadians(10 * pPreciseWheelRotation);
		repaint();
	}

	public OsmandSettings getSettings() {
		return mContext.getSettings();
	}

	public OsmWindow getApplication() {
		return mContext;
	}

	public boolean isLayerVisible(OsmandMapLayer layer) {
		return layers.contains(layer);
	}

	public float getZorder(OsmandMapLayer layer) {
		Float z = zOrders.get(layer);
		if (z == null) {
			return 10;
		}
		return z;
	}

	public synchronized void addLayer(OsmandMapLayer layer, float zOrder) {
		int i = 0;
		for (i = 0; i < layers.size(); i++) {
			if (zOrders.get(layers.get(i)) > zOrder) {
				break;
			}
		}
		layer.initLayer(this);
		layers.add(i, layer);
		zOrders.put(layer, zOrder);
	}

	public synchronized void removeLayer(OsmandMapLayer layer) {
		while (layers.remove(layer))
			;
		zOrders.remove(layer);
		layer.destroyLayer();
	}

	public synchronized void removeAllLayers() {
		while (layers.size() > 0) {
			removeLayer(layers.get(0));
		}
	}

	public List<OsmandMapLayer> getLayers() {
		return layers;
	}

	@SuppressWarnings("unchecked")
	public <T extends OsmandMapLayer> T getLayerByClass(Class<T> cl) {
		for (OsmandMapLayer lr : layers) {
			if (cl.isInstance(lr)) {
				return (T) lr;
			}
		}
		return null;
	}

	public void refreshMap() {
		drawLater();
	}

	public POIMapLayer getPoiLayer() {
		return mPoiLayer;
	}

	void repaintAndWait(int pWaitMillies) {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					repaint();
				}
			});
			Thread.sleep(pWaitMillies);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public RotatedTileBox getCurrentTileBox() {
		return mCurrentTileBox;
	}

	public void setCurrentTileBox(RotatedTileBox pCurrentTileBox) {
		mCurrentTileBox = pCurrentTileBox;
		mUnzoomedPicturesAction.setTileBox(pCurrentTileBox);
	}

	public void resizePanel() {
		Dimension size = getSize();
		QuadPoint centerPixelPoint = getTileBox().getCenterPixelPoint();
		Point diff = new Point();
		diff.x = -(int) (centerPixelPoint.x - size.getWidth()/2f);
		diff.y = -(int) (centerPixelPoint.y - size.getHeight()/2f);
		dragImage(diff);
		drawLater();
	}

	public class CalculateUnzoomedPicturesAction extends AbstractAction {

		private BinaryOrder mBinaryOrder;
		private RotatedTileBox mTileBox2;
		private HashMap<RotatedTileBox, BufferedImage> mImageStore = new HashMap<>();

		public CalculateUnzoomedPicturesAction() {
			mBinaryOrder = new BinaryOrder();
		}

		public BufferedImage getImage(RotatedTileBox pRtb) {
			synchronized (mImageStore) {
				return mImageStore.get(pRtb);
			}
		}

		public List<RotatedTileBox> getTileBoxesForZoom(int pZoom) {
			synchronized (mImageStore) {
				ArrayList<RotatedTileBox> ret = new ArrayList<>();
				for (RotatedTileBox rtb : mImageStore.keySet()) {
					if (rtb.getZoom() == pZoom) {
						ret.add(rtb);
					}
				}
				return ret;
			}
		}

		public void setTileBox(RotatedTileBox pTb){
			synchronized (mImageStore) {
				mTileBox2 = pTb;
				mBinaryOrder.init(1, pTb.getZoom()-1);
				for (Iterator it = mImageStore.entrySet().iterator(); it.hasNext();) {
					Entry<RotatedTileBox, BufferedImage> entry = (Map.Entry<RotatedTileBox, BufferedImage>) it.next();
					RotatedTileBox rtb = entry.getKey();
					if(!contains(rtb, pTb)){
						it.remove();
					} else {
						mBinaryOrder.alreadyDone(rtb.getZoom());
					}
				}
				log.info("After setting a new tile box we have " + mImageStore.size() + " cache entries.");
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent pE) {
			if(mBinaryOrder.hasNext()){
				int nextZoom = mBinaryOrder.getNext();
				// calculate image:
				RotatedTileBox tb = mTileBox2.copy();
				tb.setZoom(nextZoom);
				queue(new GenerationThread(OsmBitmapPanel.this, tb){
					@Override
					public void runAfterThreadsBeforeHaveFinished() {
						// instead of setting this, we store it:
						synchronized (mImageStore) {
							System.out.println("Adding zoom " + mTileCopy.getZoom() + " to the cache.");
							mImageStore.put(mTileCopy, mNewBitmap);
						}
					}
					@Override
					public RotatedTileBox getDestinationTileBox() {
						// the tile boxes calculated here are not relevant for display
						return null;
					}
				});
			}
		}
	}


}