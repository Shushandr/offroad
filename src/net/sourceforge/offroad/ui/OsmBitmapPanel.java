package net.sourceforge.offroad.ui;

import java.awt.BasicStroke;
import java.awt.Color;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.binary.OsmandIndex;
import net.osmand.binary.OsmandIndex.MapLevel;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.render.OsmandRenderer.RenderingResult;
import net.osmand.plus.views.DrawPolylineLayer;
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.res.OffRoadResources;
import net.sourceforge.offroad.ui.OffRoadUIThread.OffRoadUIThreadListener;
import net.sourceforge.offroad.ui.OsmBitmapPanel.CalculateUnzoomedPicturesAction.ImageStorage;

@SuppressWarnings("serial")
public class OsmBitmapPanel extends JPanel {
	private static final int INACTIVITY_TIME_IN_MILLISECONDS = 2000;

	private final static Log log = PlatformUtil.getLog(OsmBitmapPanel.class);

	private static final Color BACKGROUND_COLOR = Color.white;
	private OsmWindow mContext;
	private RotatedTileBox mCurrentTileBox;
	private boolean mShowCursor;
	private LatLon mCursorPosition = null;
	private int mCursorLength = 20;
	private BasicStroke mStroke;
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	private Map<OsmandMapLayer, Float> zOrders = new HashMap<OsmandMapLayer, Float>();
	private POIMapLayer mPoiLayer;
	private ExecutorService mThreadPool;
	private OffRoadUIThread mLastThread;
	private InactivityListener mInactivityListener;
	private CalculateUnzoomedPicturesAction mUnzoomedPicturesAction;
	private RoundButton mCompassButton;
	private List<ImageStorage> mEffectivelyDrawnImages = new ArrayList<>();

	private int mZoomCounter;

	private BufferedImage mLayerImage;
	private RotatedTileBox mLayerImageTileBox;

	private boolean mCursorRadiusEnabled = false;

	private double mCursorRadiusSizeInMeters = 100;
	private boolean mZoomIsRunning = false;

	private DrawPolylineLayer mPolylineLayer;


	public OsmBitmapPanel(OsmWindow pWin) {
		mContext = pWin;
		// absolute positioning
		this.setLayout(null);
		mUnzoomedPicturesAction = new CalculateUnzoomedPicturesAction();
		LatLon latLon = new LatLon(51.03325, 13.64656);
		int zoom = 17;
		if (mContext.getSettings().isLastKnownMapLocation()) {
			latLon = mContext.getSettings().getLastKnownMapLocation();
			zoom = mContext.getSettings().getLastKnownMapZoom();
		}
		setCurrentTileBox(new RotatedTileBoxBuilder().setLocation(latLon.getLatitude(), latLon.getLongitude()).setZoom(zoom)
				.setPixelDimensions(getWidth(), getHeight()).setRotate(0).setMapDensity(1d).build());
		mCursorLength = (int) (15 * mCurrentTileBox.getMapDensity());
		mStroke = new BasicStroke((float) (2f * mCurrentTileBox.getMapDensity()));
		RouteLayer routeLayer = new RouteLayer(mContext.getRoutingHelper());
		addLayer(routeLayer, 1);
		addLayer(new MapTextLayer(), 2);
		mPoiLayer = new POIMapLayer(pWin);
		addLayer(mPoiLayer, 3);
		addLayer(new PointNavigationLayer(mContext), 4);
		addLayer(new FavoritesLayer(), 5);
		addLayer(new GPXLayer(), 6);
		addLayer(new CursorDistanceLayer(), 7);
		addLayer(new MapInfoLayer(this, routeLayer), 8);
		mCompassButton = new RoundButton();
		addLayer(new MapControlsLayer(this), 9);
		mPolylineLayer = new DrawPolylineLayer(this);
		addLayer(mPolylineLayer, 10);
		DirectSearchLayer directSearchLayer = new DirectSearchLayer();
		// combine the action with the layer
		mContext.mDirectSearchAction.registerDirectSearchReceiver(directSearchLayer);
		addLayer(directSearchLayer, 10);
		DirectSearchSelectionLayer directSelectionLayer = new DirectSearchSelectionLayer();
		directSelectionLayer.setSearchProvider(mContext.mDirectSearchAction.getSelectionProvider());
		addLayer(directSelectionLayer, 11);
		
		for (OsmandMapLayer layer : layers) {
			layer.initLayer(this);
		}
		Action updateCursorAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mShowCursor = !mShowCursor;
				repaint();
			}
		};
		new Timer(INACTIVITY_TIME_IN_MILLISECONDS, updateCursorAction).start();
		mThreadPool = Executors.newFixedThreadPool(4);
		add(mCompassButton, getComponentCount()-1);
		int size = mCompassButton.getZoomedCircleRadius();
		mCompassButton.setBounds(100, 100, size, size);
	}

	public void init() {
		mInactivityListener = new InactivityListener(mContext.getWindow(), mUnzoomedPicturesAction);
		mInactivityListener.setIntervalInMillis(1000);
		mInactivityListener.start();
	}

	
	public List<ImageStorage> getEffectivelyDrawnImages(){
		synchronized (mEffectivelyDrawnImages) {
			return new ArrayList<ImageStorage>(mEffectivelyDrawnImages);
		}
	}
	
	public void setEffectivelyDrawnImages(List<ImageStorage> pList){
		synchronized (mEffectivelyDrawnImages) {
			mEffectivelyDrawnImages.clear();
			mEffectivelyDrawnImages.addAll(pList);
		}
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
		synchronized (mCurrentTileBox) {
			return mCurrentTileBox.getDensity();
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
		ArrayList<ImageStorage> effectivelyDrawn = new ArrayList<>();
		Graphics2D gd2 = (Graphics2D) g;
		Graphics2D g2 = createGraphics(gd2);
		// check for cached picture of bigger size to lay under:
		RotatedTileBox ctb = copyCurrentTileBox();
		LatLon screenLT = ctb.getLeftTopLatLon();
		LatLon screenRB = ctb.getRightBottomLatLon();
		int upperBound = ctb.getZoom();
		boolean imageFound=false;
		for(int biggerZoom = OsmWindow.MIN_ZOOM; biggerZoom <= OsmWindow.MAX_ZOOM; ++biggerZoom){
			List<ImageStorage> tblist = mUnzoomedPicturesAction.getTileBoxesForZoom(biggerZoom);
			// check for each, if the current image is contained
			for (ImageStorage tblistEntry : tblist) {
				RotatedTileBox rtb = tblistEntry.mTileBox;
				if ((rtb.intersects(screenLT, screenRB))) {
					// draw this under it:
					LatLon rtbLT = rtb.getLeftTopLatLon();
					LatLon rtbRB = rtb.getRightBottomLatLon();
					LatLon clalo = rtb.getCenterLatLon();
					double xc = ctb.getPixXFromLatLon(clalo.getLatitude(), clalo.getLongitude());
					double yc = ctb.getPixYFromLatLon(clalo.getLatitude(), clalo.getLongitude());
					float ctbRotate = ctb.getRotate();
					float theta = ctbRotate - rtb.getRotate();
					ctb.setRotate(rtb.getRotate());
					double x1 = ctb.getPixXFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
					double y1 = ctb.getPixYFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
					double x2 = ctb.getPixXFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
					double y2=  ctb.getPixYFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
					BufferedImage image = tblistEntry.mImage;
					if(image != null){
						effectivelyDrawn.add(tblistEntry);
						double thetaR = Math.toRadians(theta);
						g2.rotate(thetaR, xc, yc);
						g2.drawImage(image, (int)x1, (int)y1, (int)x2, (int)y2, 0,0, image.getWidth(), image.getHeight(), null);
						imageFound = true;
						g2.rotate(-thetaR, xc, yc);
					}
					ctb.setRotate(ctbRotate);
				}
			}
			// If no image was found till upperBound, go to max.
			if(biggerZoom == upperBound && imageFound){
				break;
			}
		}
		setEffectivelyDrawnImages(effectivelyDrawn);
//		if(mLayerImage != null && mLayerImageTileBox.equals(ctb)){
//			g2.drawImage(mLayerImage, 0, 0, null);
//		}
		drawLayers(ctb, g2, true);
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

	public RotatedTileBox copyCurrentTileBox() {
		RotatedTileBox ctb;
		synchronized (mCurrentTileBox) {
			ctb = mCurrentTileBox.copy();
		}
		return ctb;
	}

	boolean contains(RotatedTileBox bigger, RotatedTileBox smaller) {
		return bigger.containsLatLon(smaller.getLeftTopLatLon()) && bigger.containsLatLon(smaller.getRightBottomLatLon());
	}

	void setImage(BufferedImage pImage, RotatedTileBox pGenerationTileBox, RenderingResult pResult) {
		if(pImage != null){
			mUnzoomedPicturesAction.addToCache(pGenerationTileBox, pImage, pResult);
			repaint();
		}
	}

	public interface IntermediateImageListener {
		void propagateImage();
	}
	
	RenderingResult drawImage(BufferedImage pImage, RotatedTileBox pTileBox, IntermediateImageListener pListener) {
		clear(pImage);
		Graphics2D graphics = pImage.createGraphics();
		Graphics2D g2 = createGraphics(graphics);
		RenderingResult result = mContext.loadMGap(g2, pTileBox, pListener);
		g2.dispose();
		graphics.dispose();
		return result;
	}

	public Graphics2D createGraphics(Graphics2D graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2;
	}

	public void zoomChange(final int pWheelRotation, final Point pNewCenter) {
		final RotatedTileBox tileCopy = copyCurrentTileBox();
		int delta = pWheelRotation;
		int newZoom = tileCopy.getZoom() + delta;
		newZoom = (int) checkZoom(newZoom);
		delta = newZoom-tileCopy.getZoom();
		if(delta == 0){
			log.info("No zooming, as delta is zero");
			return;
		}
		ZoomAnimationThread animationThread = new ZoomAnimationThread(this, delta, pNewCenter);
		animationThread.addListener(new OffRoadUIThreadListener() {
			
			@Override
			public void threadStarted() {
				log.debug("Zooming started");
				mZoomIsRunning = true;
			}
			
			@Override
			public void threadFinished() {
				log.debug("Zooming stopped");
				mZoomIsRunning = false;
			}
		});
		RotatedTileBox destinationTileBox = animationThread.getDestinationTileBox(tileCopy.getZoom(), 10, 9);
		queue(animationThread);
		GenerationThread genThread = new LazyThread(this, destinationTileBox);
		mZoomCounter++;
		if(mZoomCounter >= 4){
			mZoomCounter = 0;
			// every fourth zoom, we generate
			genThread = new GenerationThread(this, destinationTileBox);
		}
		queue(genThread);
	}

	public boolean isZoomRunning() {
		return mZoomIsRunning;
	}

	
	public void queue(OffRoadUIThread pThread) {
		pThread.addListener(mInactivityListener);
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
		log.debug("New thread " + pThread + " is queued.");
		mThreadPool.execute(pThread);
		if(pThread instanceof GenerationThread){
			queue(new GenerateLayerOverlayThread(this, copyCurrentTileBox()));
		}
	}

	public float checkZoom(float newZoom) {
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
		RotatedTileBox tb = moveTileBox(pDeltaX, pDeltaY);
		queue(new LazyThread(this, tb));
	}

	public RotatedTileBox moveTileBox(float pDeltaX, float pDeltaY) {
		RotatedTileBox tb = copyCurrentTileBox();
		QuadPoint center = tb.getCenterPixelPoint();
		double newLat = tb.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY);
		double newLon = tb.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY);
		tb.setLatLonCenter(newLat, newLon);
		setCurrentTileBox(tb);
		return tb;
	}

	public void moveImageAnimatedInPercentage(float pDeltaX, float pDeltaY) {
		pDeltaX *= getWidth();
		pDeltaY *= getHeight();
		System.out.println("Moving by  "  +pDeltaX + ", " + pDeltaY);
		RotatedTileBox tileBox = copyCurrentTileBox();
		QuadPoint center = tileBox.getCenterPixelPoint();
		tileBox.setLatLonCenter(tileBox.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
				tileBox.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
		moveAnimated(pDeltaX, pDeltaY, tileBox);
	}
	public void moveAnimated(float pDeltaX, float pDeltaY, RotatedTileBox tileBox) {
		MoveAnimationThread animationThread = new MoveAnimationThread(this, pDeltaX, pDeltaY);
		queue(animationThread);
		queue(new GenerationThread(this, tileBox));
	}

	public BufferedImage createImage() {
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		return image;
	}

	public void dragImage(Point pTranslate) {
		moveTileBox(pTranslate.x, pTranslate.y);
		repaint();
	}

	public void setCursor(Point pCursorPoint) {
		mCursorPosition = getLatLon(pCursorPoint);
		System.out.println("Setting cursor to " + mCursorPosition);
		repaint();
	}

	public LatLon getLatLon(Point pCursorPoint) {
		synchronized (mCurrentTileBox) {
			return mCurrentTileBox.getLatLonFromPixel(pCursorPoint.x, pCursorPoint.y);
		}
	}

	public Point getPoint(LatLon pLatLon) {
		synchronized (mCurrentTileBox) {
			int posx = (int) mCurrentTileBox.getPixXFromLatLon(pLatLon);
			int posy = (int) mCurrentTileBox.getPixYFromLatLon(pLatLon);
			return new Point(posx, posy);
		}
	}
	
	public void move(LatLon pLocation, int pZoom) {
		RotatedTileBox tb = copyCurrentTileBox();
		tb.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
		int newZoom = (int) checkZoom(pZoom);
		tb.setZoom(newZoom);
		setCurrentTileBox(tb);
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

	/**
	 */
	public void drawLater() {
		RotatedTileBox tb = copyCurrentTileBox();
		tb.setPixelDimensions(getWidth(), getHeight());
		setCurrentTileBox(tb);
		queue(new GenerationThread(this, tb));
	}

	public void saveImage(File pSelectedFile) {
		// Create an image containing the map:
		BufferedImage myImage = (BufferedImage) createImage(
				getWidth(), getHeight());
		Graphics g = myImage.getGraphics();
		print(g);
		try {
			ImageIO.write(myImage, "png", pSelectedFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void rotateIncrement(double pDegrees) {
		queue(new GenerationThread(this, copyCurrentTileBox()));
	}

	public void directRotateIncrement(double pDegrees) {
		RotatedTileBox tb = copyCurrentTileBox();
		tb.setRotate((float) (pDegrees) + tb.getRotate());
		setCurrentTileBox(tb);
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
	
	public void drawLayers(RotatedTileBox pTileBox, Graphics2D lg, boolean pDrawDirectLayers){
		final QuadPoint c = pTileBox.getCenterPixelPoint();
		DrawSettings settings = new DrawSettings(false);
		List<OsmandMapLayer> layers = getLayers();
		for (int i = 0; i < layers.size(); i++) {
			OsmandMapLayer layer = layers.get(i);
			if(pDrawDirectLayers != (layer instanceof DirectOffroadLayer)){
				continue;
			}
			Graphics2D glayer = createGraphics(lg);
			try {
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					glayer.rotate(pTileBox.getRotate(), c.x, c.y);
				}
				layer.onPrepareBufferImage(glayer, pTileBox, settings);
				layer.onDraw(glayer, pTileBox, settings);
				// canvas.restore();
			} catch (Exception e) {
				// skip it
				e.printStackTrace();
			}
			glayer.dispose();
		}

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
	
	public void setCurrentTileBox(RotatedTileBox pCurrentTileBox) {
		mCurrentTileBox = pCurrentTileBox;
		mUnzoomedPicturesAction.setTileBox(pCurrentTileBox);
		log.debug("TILEBOX: " + pCurrentTileBox);
	}

	public void resizePanel() {
		drawLater();
		repaint();
	}

	public class CalculateUnzoomedPicturesAction extends AbstractAction {
		
		public class ImageStorage {
			public BufferedImage mImage;
			public RotatedTileBox mTileBox;
			public RenderingResult mResult;
			public ImageStorage(BufferedImage pImage, RotatedTileBox pTileBox, RenderingResult pResult) {
				super();
				mImage = pImage;
				mTileBox = pTileBox;
				mResult = pResult;
			}
		}

		private RotatedTileBoxCalculationOrder mTileOrder;
		private LinkedHashMap<RotatedTileBox, ImageStorage> mImageStore = new LinkedHashMap<RotatedTileBox, ImageStorage>(){
			@Override
			protected boolean removeEldestEntry(Map.Entry<RotatedTileBox, ImageStorage> eldest) {
		        return size() > 2 * mTileOrder.getSize();
		     }
		};

		public CalculateUnzoomedPicturesAction() {
			mTileOrder = new RotatedTileBoxCalculationOrder();
		}

		public List<ImageStorage> getTileBoxesForZoom(int pZoom) {
			synchronized (mImageStore) {
				ArrayList<ImageStorage> ret = new ArrayList<>();
				for (Entry<RotatedTileBox, ImageStorage> rtb : mImageStore.entrySet()) {
					if (rtb.getKey().getZoom() == pZoom) {
						ret.add(rtb.getValue());
					}
				}
				if(mLayerImageTileBox!= null && pZoom == mLayerImageTileBox.getZoom()){
					ret.add(new ImageStorage(mLayerImage, mLayerImageTileBox, new RenderingResult()));
				}
				return ret;
			}
		}

		public void setTileBox(RotatedTileBox pTb){
			synchronized (mImageStore) {
				mTileOrder.init(pTb);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent pE) {
			// check for empty queue:
			if(!isQueueEmpty()){
				return;
			}
			if(mTileOrder.hasNext()){
				RotatedTileBox tb =  mTileOrder.getNext();
				queue(new GenerationThread(OsmBitmapPanel.this, tb){
					@Override
					public void runAfterThreadsBeforeHaveFinished() {
						// instead of setting this, we store it:
						addToCache(mTileCopy, mNewBitmap, mResult);
					}
				});
			}
		}
		
		public void addToCache(RotatedTileBox pTileBox, BufferedImage pBitmap, RenderingResult pResult){
			synchronized (mImageStore) {
				log.debug("Adding  " + pTileBox + " to the cache.");
				mImageStore.put(pTileBox, new ImageStorage(pBitmap, pTileBox, pResult));
			}
			
		}
	}

	/**
	 * @return true, if the queue is currently empty.
	 */
	protected boolean isQueueEmpty() {
		if(mLastThread != null){
			synchronized(mLastThread){
				if(mLastThread.hasFinished()){
					return true;
				}
			}
			return false;
		}
		return true;
	}

	public OffRoadResources getResources() {
		return mContext.getResources();
	}

	public void setLayerImage(BufferedImage pLayerImage, RotatedTileBox pTileBox) {
		mLayerImage = pLayerImage;
		mLayerImageTileBox = pTileBox;
		repaint();
	}

	public double getCursorRadiusSizeInMeters() {
		return mCursorRadiusSizeInMeters ;
	}

	public boolean isCursorRadiusEnabled() {
		return mCursorRadiusEnabled ;
	}

	public void setCursorRadiusEnabled(boolean pCursorRadiusEnabled) {
		mCursorRadiusEnabled = pCursorRadiusEnabled;
	}

	public void setCursorRadiusSizeInMeters(double pCursorRadiusSizeInMeters) {
		mCursorRadiusSizeInMeters = pCursorRadiusSizeInMeters;
	}

	public OsmWindow getMyApplication() {
		return getApplication();
	}

	public JButton findViewById(int pMapCompassButton) {
		return mCompassButton;
	}
	
	public DrawPolylineLayer getPolylineLayer() {
		return mPolylineLayer;
	}
}