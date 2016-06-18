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
import net.osmand.plus.views.FavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.plus.views.PointNavigationLayer;
import net.osmand.plus.views.RouteLayer;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.data.Pair;
import net.sourceforge.offroad.res.OffRoadResources;
import net.sourceforge.offroad.ui.OffRoadUIThread.OffRoadUIThreadListener;

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

	private int mZoomCounter;

	private BufferedImage mLayerImage;
	private RotatedTileBox mLayerImageTileBox;

	private boolean mCursorRadiusEnabled = false;

	private double mCursorRadiusSizeInMeters = 100;
	private boolean mZoomIsRunning = false;


	public OsmBitmapPanel(OsmWindow pWin) {
		mContext = pWin;
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
		mCompassButton.setLocation(100, 100);
	}

	public void init() {
		mInactivityListener = new InactivityListener(mContext.getWindow(), mUnzoomedPicturesAction);
		mInactivityListener.setIntervalInMillis(1000);
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
		Graphics2D gd2 = (Graphics2D) g;
		Graphics2D g2 = createGraphics(gd2);
		// check for cached picture of bigger size to lay under:
		RotatedTileBox ctb = copyCurrentTileBox();
		LatLon screenLT = ctb.getLeftTopLatLon();
		LatLon screenRB = ctb.getRightBottomLatLon();
		int upperBound = ctb.getZoom();
		boolean imageFound=false;
		for(int biggerZoom = OsmWindow.MIN_ZOOM; biggerZoom <= OsmWindow.MAX_ZOOM; ++biggerZoom){
			List<Entry<RotatedTileBox,BufferedImage>> tblist = mUnzoomedPicturesAction.getTileBoxesForZoom(biggerZoom);
			// check for each, if the current image is contained
			for (Entry<RotatedTileBox, BufferedImage> tblistEntry : tblist) {
				RotatedTileBox rtb = tblistEntry.getKey();
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
					BufferedImage image = tblistEntry.getValue();
					if(image != null){
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
//		if(mLayerImage != null && mLayerImageTileBox.equals(ctb)){
//			g2.drawImage(mLayerImage, 0, 0, null);
//		}
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

	void setImage(BufferedImage pImage, RotatedTileBox pGenerationTileBox) {
		if(pImage != null){
			mUnzoomedPicturesAction.addToCache(pGenerationTileBox, pImage);
			repaint();
		}
	}

	private void drawImage(BufferedImage pImage, RotatedTileBox pTileBox) {
		clear(pImage);
		Graphics2D graphics = pImage.createGraphics();
		Graphics2D g2 = createGraphics(graphics);
		mContext.loadMGap(g2, pTileBox);
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

	public void zoomChange(final int pWheelRotation, final Point pNewCenter) {
		if(mZoomIsRunning){
			log.info("Don't zoom as there is something running.");
			return ;
		}
		final RotatedTileBox tileCopy = copyCurrentTileBox();
		int delta = pWheelRotation;
		int newZoom = tileCopy.getZoom() + delta;
		newZoom = (int) checkZoom(newZoom);
		delta = newZoom-tileCopy.getZoom();
		if(delta == 0){
			return;
		}
		ZoomAnimationThread animationThread = new ZoomAnimationThread(this, delta, pNewCenter);
		animationThread.addListener(new OffRoadUIThreadListener() {
			
			@Override
			public void threadStarted() {
				mZoomIsRunning = true;
			}
			
			@Override
			public void threadFinished() {
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

	public void moveImageAnimated(float pDeltaX, float pDeltaY) {
		pDeltaX *= getWidth();
		pDeltaY *= getHeight();
		System.out.println("Moving by  "  +pDeltaX + ", " + pDeltaY);
		RotatedTileBox tileBox = copyCurrentTileBox();
		QuadPoint center = tileBox.getCenterPixelPoint();
		tileBox.setLatLonCenter(tileBox.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
				tileBox.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
		MoveAnimationThread animationThread = new MoveAnimationThread(this, pDeltaX, pDeltaY);
		queue(animationThread);
		queue(new GenerationThread(this, tileBox));
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
		moveTileBox(pTranslate.x, pTranslate.y);
		repaint();
	}

	public void setCursor(Point pCursorPoint) {
		synchronized (mCurrentTileBox) {
			mCursorPosition = mCurrentTileBox.getLatLonFromPixel(pCursorPoint.x, pCursorPoint.y);
		}
		System.out.println("Setting cursor to " + mCursorPosition);
		repaint();
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

		private RotatedTileBoxCalculationOrder mTileOrder;
		private LinkedHashMap<RotatedTileBox, BufferedImage> mImageStore = new LinkedHashMap<RotatedTileBox, BufferedImage>(){
			protected boolean removeEldestEntry(Map.Entry eldest) {
		        return size() > 2 * mTileOrder.getSize();
		     }
		};

		public CalculateUnzoomedPicturesAction() {
			mTileOrder = new RotatedTileBoxCalculationOrder();
		}

		public BufferedImage getImage(RotatedTileBox pRtb) {
			synchronized (mImageStore) {
				return mImageStore.get(pRtb);
			}
		}

		public List<Entry<RotatedTileBox, BufferedImage>> getTileBoxesForZoom(int pZoom) {
			synchronized (mImageStore) {
				ArrayList<Entry<RotatedTileBox, BufferedImage>> ret = new ArrayList<>();
				for (Entry<RotatedTileBox, BufferedImage> rtb : mImageStore.entrySet()) {
					if (rtb.getKey().getZoom() == pZoom) {
						ret.add(rtb);
					}
				}
				if(mLayerImageTileBox!= null && pZoom == mLayerImageTileBox.getZoom()){
					ret.add(new Pair<RotatedTileBox, BufferedImage>(mLayerImageTileBox, mLayerImage));
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
						addToCache(mTileCopy, mNewBitmap);
					}
				});
			}
		}
		
		public void addToCache(RotatedTileBox pTileBox, BufferedImage pBitmap){
			synchronized (mImageStore) {
				log.debug("Adding  " + pTileBox + " to the cache.");
				mImageStore.put(pTileBox, pBitmap);
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

}