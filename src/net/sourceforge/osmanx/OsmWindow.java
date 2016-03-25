package net.sourceforge.osmanx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

public class OsmWindow {

	private static void createAndShowUI(OsmWindow pWin) {
		STDrawPanel drawPanel = new STDrawPanel(pWin);
		STMouseAdapter mAdapter = new STMouseAdapter(drawPanel);
		drawPanel.addMouseListener(mAdapter);
		drawPanel.addMouseMotionListener(mAdapter);
		drawPanel.addMouseWheelListener(mAdapter);

		JFrame frame = new JFrame("Drawing");
		frame.getContentPane().add(drawPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
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

		public STDrawPanel(OsmWindow pWin) {
			mWin = pWin;
			clear();
			mTileBox = new RotatedTileBoxBuilder().setLocation(49.2082,7.0285).setZoom(11)
					.setPixelDimensions(bImage.getWidth(), bImage.getHeight()).setRotate(0).build();
		}

		private void clear() {
			Graphics g = bImage.getGraphics();
			g.setColor(BACKGROUND_COLOR);
			g.fillRect(0, 0, ST_WIDTH, ST_HEIGHT);
			g.dispose();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(bImage, 0, 0, null);
		}

		private void generateImage() {
			clear();
			Graphics2D g2 = bImage.createGraphics();
			mWin.loadMGap(g2, mTileBox);
			g2.dispose();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(ST_WIDTH, ST_HEIGHT);
		}

		public void setColor(Color color) {
			this.color = color;
		}


		public void zoomChange(int pWheelRotation) {
			mTileBox.setZoom(mTileBox.getZoom()+pWheelRotation);
			generateImage();
		}

		public void moveImage(float pDeltaX, float pDeltaY) {
			QuadPoint center = mTileBox.getCenterPixelPoint();
			mTileBox.setLatLonCenter(mTileBox.getLatFromPixel(center.x + pDeltaX, center.y+pDeltaY),mTileBox.getLonFromPixel(center.x + pDeltaX, center.y+pDeltaY));
			generateImage();
		}
	}

	public static class STMouseAdapter extends MouseAdapter {
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
			double delx = e.getX()-startPoint.getX();
			double dely = e.getY()-startPoint.getY();
			drawPanel.moveImage(-(float)delx, -(float)dely);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent pE) {
			drawPanel.zoomChange(pE.getWheelRotation());
		}
	}

	private RenderingRulesStorage mRenderingRulesStorage;
	private BinaryMapIndexReader mBinaryMapIndexReader;
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
		mBinaryMapIndexReader = getBinaryMapIndexReader();
		mMapRenderRepositories = getMapRenderRepositories(mBinaryMapIndexReader);
	}

	private MapRenderRepositories getMapRenderRepositories(BinaryMapIndexReader pBinaryMapIndexReader) {
		MapRenderRepositories mapRenderRepositories = new MapRenderRepositories();
		mapRenderRepositories.initializeNewResource(IProgress.EMPTY_PROGRESS, pBinaryMapIndexReader.getFile(), pBinaryMapIndexReader);
		return mapRenderRepositories;
	}

	public BinaryMapIndexReader getBinaryMapIndexReader() throws IOException {
		File fl = new File("/home/foltin/programming/java/osmand/maps/Germany_saarland_europe_2.obf");
		RandomAccessFile raf = new RandomAccessFile(fl, "r");

		BinaryMapIndexReader reader = new BinaryMapIndexReader(raf, fl);
		System.out.println("VERSION " + reader.getVersion()); //$NON-NLS-1$
		long time = System.currentTimeMillis();
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
