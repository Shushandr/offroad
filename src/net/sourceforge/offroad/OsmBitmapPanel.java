package net.sourceforge.offroad;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.osmand.ValueHolder;
import net.osmand.binary.OsmandIndex;
import net.osmand.binary.OsmandIndex.MapLevel;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.RouteLayer;

@SuppressWarnings("serial")
public class OsmBitmapPanel extends JPanel implements IRouteInformationListener {
	private class GenerationThread extends Thread {
		private final RotatedTileBox mTileCopy;
		private BufferedImage mNewBitmap;
		private Thread mWaitForThread;

		private GenerationThread(RotatedTileBox pTileCopy, Thread pWaitForThread) {
			mTileCopy = pTileCopy;
			mWaitForThread = pWaitForThread;
		}

		@Override
		public void run() {
			// generate in background
			setTileBox(mTileCopy);
			mNewBitmap = newBitmap();
			// wait
			if (mWaitForThread != null && mWaitForThread.isAlive()) {
				try {
					mWaitForThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			// activate in foreground
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					setImage(mNewBitmap);
				}
			});
		}
	}

	private static final int ST_WIDTH = 1400;
	private static final int ST_HEIGHT = 1000;
	private static final Color BACKGROUND_COLOR = Color.white;
	private static final float STROKE_WIDTH = 6f;
	private static final Stroke STROKE = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND,
			BasicStroke.JOIN_ROUND);
	private static final Color[] colors = { Color.black, Color.blue, Color.red, Color.green, Color.orange,
			Color.MAGENTA };

	private BufferedImage bImage = new BufferedImage(ST_WIDTH, ST_HEIGHT, BufferedImage.TYPE_INT_RGB);
	private Color color = Color.black;
	private ArrayList<Point> points = new ArrayList<Point>();
	private int colorIndex = 0;
	private OsmWindow mContext;
	private RotatedTileBox mTileBox;
	private double scale = 1.0d;
	private int originX = 0;
	private int originY = 0;
	private Thread mAnimationThread;
	private GenerationThread mGenerationThread;
	private boolean mShowCursor;
	private LatLon mCursorPosition = null;
	private int mCursorLength = 20;
	private BasicStroke mStroke;
	private RouteLayer mRouteLayer;

	public OsmBitmapPanel(OsmWindow pWin) {
		mContext = pWin;
		clear(bImage);
		LatLon latLon = new LatLon(51.03325, 13.64656);
		int zoom = 17;
		if(mContext.getSettings().isLastKnownMapLocation()){
			latLon = mContext.getSettings().getLastKnownMapLocation();
			zoom = mContext.getSettings().getLastKnownMapZoom();
		}
		setTileBox(new RotatedTileBoxBuilder().setLocation(latLon.getLatitude(), latLon.getLongitude()).setZoom(zoom)
				.setPixelDimensions(bImage.getWidth(), bImage.getHeight()).setRotate(0).setMapDensity(1d).build());
		mCursorLength = (int) (15 * mTileBox.getMapDensity());
		mStroke = new BasicStroke( (float) (2f * mTileBox.getMapDensity()));
		mRouteLayer = new RouteLayer(mContext.getRoutingHelper());
		mRouteLayer.initLayer(this);
		mContext.getRoutingHelper().addListener(this);
		Action updateCursorAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mShowCursor = !mShowCursor;
				repaint();
			}
		};
		new Timer(1000, updateCursorAction).start();
		

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
		return mTileBox.getDensity();
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
		Graphics2D g2 = (Graphics2D) g;
		AffineTransform at = g2.getTransform();
		AffineTransform oldTransform = (AffineTransform) at.clone();
		at.translate(originX, originY);
		at.scale(scale, scale);
		g2.setTransform(at);
		g2.drawImage(bImage, 0, 0, null);
		// cursor:
		Stroke oldStroke = g2.getStroke();
		Color oldColor = g2.getColor();
		// do cursor
		if (mCursorPosition != null && mShowCursor && getTileBox().containsLatLon(mCursorPosition)){
			int posx = (int) getTileBox().getPixXFromLatLon(mCursorPosition.getLatitude(), mCursorPosition.getLongitude());
			int posy = (int) getTileBox().getPixYFromLatLon(mCursorPosition.getLatitude(), mCursorPosition.getLongitude());
			int size_h = mCursorLength;
			g2.setStroke(mStroke);
			g2.setColor(Color.RED);
			g2.drawLine(posx - size_h, posy, posx
					+ size_h, posy);
			g2.drawLine(posx, posy - size_h, posx,
					posy + size_h);
		}
		g2.setColor(oldColor);
		g2.setStroke(oldStroke);
		g2.setTransform(oldTransform);

	}

	private void setImage(BufferedImage pImage) {
		scale = 1.0d;
		originX = 0;
		originY = 0;
		bImage = pImage;
		repaint();
	}

	private void drawImage(BufferedImage pImage) {
		clear(pImage);
		Graphics2D g2 = pImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		mContext.loadMGap(g2, getTileBox());
		try {
			// rotate if needed
			if (!mRouteLayer.drawInScreenPixels()) {
				final QuadPoint c = mTileBox.getCenterPixelPoint();

				g2.rotate(mTileBox.getRotate(), c.x, c.y);
			}
			mRouteLayer.onPrepareBufferImage(g2, mTileBox, new DrawSettings(false));
		} catch (Exception e) {
			e.printStackTrace();
		}
		g2.dispose();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(bImage.getWidth(), bImage.getHeight());
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void zoomChange(final int pWheelRotation, final Point pNewCenter) {
		int newZoom = getTileBox().getZoom() + pWheelRotation;
		MapLevel mapInstance = OsmandIndex.MapLevel.getDefaultInstance();
		int minZoom = mapInstance.hasMinzoom() ? mapInstance.getMinzoom() : 1;
		if (newZoom < minZoom) {
			newZoom = minZoom;
		}
		// FIXME: Magic number
		int maxZoom = mapInstance.hasMaxzoom() ? mapInstance.getMaxzoom() : OsmWindow.MAX_ZOOM;
		if (newZoom > maxZoom) {
			newZoom = maxZoom;
		}

		if (mAnimationThread != null && mAnimationThread.isAlive()) {
			return;
		}
		if (mGenerationThread != null && mGenerationThread.isAlive()) {
			return;
		}
		LatLon latLonNewCenter = getTileBox().getLatLonFromPixel(pNewCenter.x, pNewCenter.y);
		final RotatedTileBox tileCopy = getTileBox().copy();
		tileCopy.setZoom(newZoom);
		final float deltaX = tileCopy.getPixXFromLatLon(latLonNewCenter.getLatitude(),
				latLonNewCenter.getLongitude()) - pNewCenter.x;
		final float deltaY = tileCopy.getPixYFromLatLon(latLonNewCenter.getLatitude(),
				latLonNewCenter.getLongitude()) - pNewCenter.y;
		// now move the tileCopy that latLonNewCenter is at the same pixel
		// position as before.
		double latFromPixel = tileCopy.getLatFromPixel(tileCopy.getCenterPixelX() + deltaX,
				tileCopy.getCenterPixelY() + deltaY);
		double lonFromPixel = tileCopy.getLonFromPixel(tileCopy.getCenterPixelX() + deltaX,
				tileCopy.getCenterPixelY() + deltaY);
		tileCopy.setLatLonCenter(latFromPixel, lonFromPixel);
		mAnimationThread = new Thread() {

			@Override
			public void run() {
				double dest = Math.pow(2, pWheelRotation);
				double start = 1.0d;
				int it = 10;
				double delta = (dest - start) / it;
				for (int i = 0; i < it; ++i) {
					scale = start + i * delta;
					// this is not correct. involve the size of the image.
					originX = (int) (pNewCenter.x - (pNewCenter.x) * scale);
					originY = (int) (pNewCenter.y - (pNewCenter.y) * scale);
					// System.out.println("Wheel= " + pWheelRotation + ",
					// Setting scale to " + scale + ", delta = " + delta +
					// ", dest=" + dest);
					try {
						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {
								repaint();
							}
						});
						Thread.sleep(50);
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		};
		mAnimationThread.start();
		mGenerationThread = new GenerationThread(tileCopy, mAnimationThread);
		mGenerationThread.start();
	}

	public void moveImage(float pDeltaX, float pDeltaY) {
		QuadPoint center = getTileBox().getCenterPixelPoint();
		getTileBox().setLatLonCenter(getTileBox().getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
				getTileBox().getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
		drawImage(bImage);
		setImage(bImage);
	}

	public void moveImageAnimated(final float pDeltaX, final float pDeltaY) {
		if (mAnimationThread != null && mAnimationThread.isAlive()) {
			return;
		}
		if (mGenerationThread != null && mGenerationThread.isAlive()) {
			return;
		}
		QuadPoint center = getTileBox().getCenterPixelPoint();
		final RotatedTileBox tileCopy = getTileBox().copy();
		tileCopy.setLatLonCenter(getTileBox().getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
				getTileBox().getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
		mAnimationThread = new Thread() {

			@Override
			public void run() {
				int it = 10;
				for (int i = 0; i < it; ++i) {
					originX = -(int) pDeltaX * i / it;
					originY = -(int) pDeltaY * i / it;
					try {
						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {
								repaint();
							}
						});
						Thread.sleep(50);
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		};
		mAnimationThread.start();
		mGenerationThread = new GenerationThread(tileCopy, mAnimationThread);
		mGenerationThread.start();
	}
	
	
	
	public BufferedImage newBitmap() {
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		getTileBox().setPixelDimensions(getWidth(), getHeight());
		drawImage(image);
		return image;
	}

	public void dragImage(Point pTranslate) {
		originX = pTranslate.x;
		originY = pTranslate.y;
		repaint();
	}

	public void createNewBitmap() {
		mGenerationThread = new GenerationThread(getTileBox(), null);
		mGenerationThread.start();
	}

	public RotatedTileBox getTileBox() {
		return mTileBox;
	}

	public void setTileBox(RotatedTileBox pTileBox) {
		mTileBox = pTileBox;
	}
	
	public void setCursor(Point pCursorPoint){
		mCursorPosition = mTileBox.getLatLonFromPixel(pCursorPoint.x, pCursorPoint.y);
		System.out.println("Setting cursor to " + mCursorPosition);
		repaint();
	}

	public void move(LatLon pLocation) {
		mTileBox.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
		drawImage(bImage);
		setImage(bImage);
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
		// is in UI thread
		drawImage(bImage);
		setImage(bImage);
	}

	@Override
	public void routeWasCancelled() {
		drawImage(bImage);
		setImage(bImage);
	}

	@Override
	public void routeWasFinished() {
	}
}