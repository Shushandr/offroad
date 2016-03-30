package net.sourceforge.osmanx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

/**
 * OffRoad cosmos macrocosmos cosmopolitain cosmetic cosmic osmosis osmium
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow {

	private static void createAndShowUI(OsmWindow pWin) {
		STDrawPanel drawPanel = new STDrawPanel(pWin);
		STMouseAdapter mAdapter = new STMouseAdapter(drawPanel);
		drawPanel.addMouseListener(mAdapter);
		drawPanel.addMouseMotionListener(mAdapter);
		drawPanel.addMouseWheelListener(mAdapter);

		JFrame frame = new JFrame("OffRoad");
		frame.getContentPane().add(drawPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(true);
		frame.addComponentListener(mAdapter);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	@SuppressWarnings("serial")
	public static class STDrawPanel extends JPanel {
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
		private OsmWindow mWin;
		private RotatedTileBox mTileBox;
		private double scale = 1.0d;
		private int originX = 0;
		private int originY = 0;
		private Thread mAnimationThread;

		public STDrawPanel(OsmWindow pWin) {
			mWin = pWin;
			clear();
			mTileBox = new RotatedTileBoxBuilder().setLocation(51.03325, 13.64656).setZoom(17)
					.setPixelDimensions(bImage.getWidth(), bImage.getHeight()).setRotate(0).build();
		}

		private void clear() {
			Graphics g = bImage.getGraphics();
			g.setColor(BACKGROUND_COLOR);
			g.fillRect(0, 0, bImage.getWidth(), bImage.getHeight());
			g.dispose();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			AffineTransform at = g2.getTransform();
			AffineTransform oldTransform = (AffineTransform) at.clone();
			at.scale(scale, scale);
			g2.setTransform(at);
			g2.drawImage(bImage, (int)(originX/scale), (int)(originY/scale), null);
			g2.setTransform(oldTransform);
		}

		private void generateImage() {
			scale = 1.0d;
			originX = 0;
			originY = 0;
			clear();
			Graphics2D g2 = bImage.createGraphics();
			mWin.loadMGap(g2, mTileBox);
			g2.dispose();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(bImage.getWidth(), bImage.getHeight());
		}

		public void setColor(Color color) {
			this.color = color;
		}

		public void zoomChange(final int pWheelRotation, final Point pNewCenter) {
			final int newZoom = mTileBox.getZoom() + pWheelRotation;
			if ( newZoom < 1 ) {
				return;
			}
			if(mAnimationThread != null && mAnimationThread.isAlive()){
				return;
			}
			LatLon latLonNewCenter = mTileBox.getLatLonFromPixel(pNewCenter.x, pNewCenter.y);
			final RotatedTileBox tileCopy = mTileBox.copy();
			tileCopy.setZoom(newZoom);
			final float deltaX = tileCopy.getPixXFromLatLon(latLonNewCenter.getLatitude(), latLonNewCenter.getLongitude())-pNewCenter.x;
			final float deltaY = tileCopy.getPixYFromLatLon(latLonNewCenter.getLatitude(), latLonNewCenter.getLongitude())-pNewCenter.y;
			// now move the tileCopy that latLonNewCenter is at the same pixel position as before.
			double latFromPixel = tileCopy.getLatFromPixel(tileCopy.getCenterPixelX()+deltaX, tileCopy.getCenterPixelY()+deltaY);
			double lonFromPixel = tileCopy.getLonFromPixel(tileCopy.getCenterPixelX()+deltaX, tileCopy.getCenterPixelY()+deltaY);
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
						originX = (int) (pNewCenter.x-(pNewCenter.x)*scale); 
						originY = (int) (pNewCenter.y-(pNewCenter.y)*scale); 
						System.out.println("Wheel= " + pWheelRotation + ", Setting scale to " + scale + ", delta = " + delta + ", dest=" + dest);
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
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							mTileBox = tileCopy;
							generateImage();
						}
					});
				}

			};
			mAnimationThread.start();
			
		}

		public void moveImage(float pDeltaX, float pDeltaY) {
			QuadPoint center = mTileBox.getCenterPixelPoint();
			mTileBox.setLatLonCenter(mTileBox.getLatFromPixel(center.x + pDeltaX, center.y + pDeltaY),
					mTileBox.getLonFromPixel(center.x + pDeltaX, center.y + pDeltaY));
			generateImage();
		}

		public void newBitmap() {
			bImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			mTileBox.setPixelDimensions(getWidth(), getHeight());
			generateImage();

		}

		public void dragImage(Point pTranslate) {
			originX = pTranslate.x;
			originY = pTranslate.y;
			repaint();
		}
	}

	public static class STMouseAdapter extends MouseAdapter implements ComponentListener {
		private STDrawPanel drawPanel;
		private Point startPoint;

		public STMouseAdapter(STDrawPanel drawPanel) {
			this.drawPanel = drawPanel;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			startPoint = e.getPoint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			double delx = e.getX() - startPoint.getX();
			double dely = e.getY() - startPoint.getY();
			drawPanel.moveImage(-(float) delx, -(float) dely);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Point point = e.getPoint();
			point.translate(-startPoint.x, -startPoint.y);
			drawPanel.dragImage(point);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent pE) {
			drawPanel.zoomChange(-pE.getWheelRotation(), pE.getPoint());
		}

		@Override
		public void componentResized(ComponentEvent pE) {
			drawPanel.newBitmap();
		}

		@Override
		public void componentMoved(ComponentEvent pE) {
		}

		@Override
		public void componentShown(ComponentEvent pE) {
		}

		@Override
		public void componentHidden(ComponentEvent pE) {
		}
	}

	private RenderingRulesStorage mRenderingRulesStorage;
	private MapRenderRepositories mMapRenderRepositories;

	public static void main(String[] args) throws XmlPullParserException, IOException {
		final OsmWindow win = new OsmWindow();
		win.init();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				createAndShowUI(win);
			}
		});

	}

	public void loadMGap(Graphics2D pG2, RotatedTileBox pTileRect) {
		mMapRenderRepositories.loadMGap(pG2, pTileRect, mRenderingRulesStorage);
	}

	private void init() throws XmlPullParserException, IOException {
		mRenderingRulesStorage = getRenderingRulesStorage();
		mMapRenderRepositories = getMapRenderRepositories();
	}

	private MapRenderRepositories getMapRenderRepositories() throws FileNotFoundException, IOException {
		MapRenderRepositories mapRenderRepositories = new MapRenderRepositories();
		Path dir = Paths.get("/home/foltin/programming/java/osmand/maps/");
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{obf}")) {
			for (Path entry : stream) {
				System.out.println(entry.getFileName());
				BinaryMapIndexReader reader = getReader(entry.toString());
				mapRenderRepositories.initializeNewResource(IProgress.EMPTY_PROGRESS, reader.getFile(), reader);
			}
		} catch (IOException x) {
			// IOException can never be thrown by the iteration.
			// In this snippet, it can // only be thrown by newDirectoryStream.
			System.err.println(x);
		}
		return mapRenderRepositories;
	}

	private BinaryMapIndexReader getReader(String path) throws FileNotFoundException, IOException {
		File fl = new File(path);
		RandomAccessFile raf = new RandomAccessFile(fl, "r");

		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
		System.out.println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		return reader;
	}

	public RenderingRulesStorage getRenderingRulesStorage() throws XmlPullParserException, IOException {
		// RenderingRulesStorage.STORE_ATTTRIBUTES = true;
		// InputStream is =
		// RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		final String loc = "/home/foltin/programming/java/osmand/OsmAnd-resources/rendering_styles/";
		String defaultFile = loc + "UniRS.render.xml";
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		InputStream is = new FileInputStream(loc + "default.render.xml");
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(is, "UTF-8");
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if (tagName.equals("renderingConstant")) {
						if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
							renderingConstants.put(parser.getAttributeValue("", "name"),
									parser.getAttributeValue("", "value"));
						}
					}
				}
			}
		} finally {
			is.close();
		}
		RenderingRulesStorage storage = new RenderingRulesStorage("default", renderingConstants);
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref)
					throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, renderingConstants);
				// depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name
				// + ".render.xml"), ref);
				depends.parseRulesFromXmlInputStream(new FileInputStream(loc + name + ".render.xml"), ref);
				return depends;
			}
		};
		is = new FileInputStream(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);

		// storage = new RenderingRulesStorage("", null);
		// new DefaultRenderingRulesStorage().createStyle(storage);
		// for (RenderingRuleProperty p : storage.PROPS.getCustomRules()) {
		// System.out.println(p.getCategory() + " " + p.getName() + " " +
		// p.getAttrName());
		// }
		return storage;
	}
}
