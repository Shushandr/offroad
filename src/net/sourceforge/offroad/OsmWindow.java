package net.sourceforge.offroad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.actions.DownloadAction;
import net.sourceforge.offroad.actions.RouteAction;
import net.sourceforge.offroad.actions.SearchAddressAction;
import net.sourceforge.offroad.data.QuadRectExtendable;

/**
 * OffRoad
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow {

	static final int MAX_ZOOM = 22;
	public static final String RENDERING_STYLES_DIR = "rendering_styles/";
	public static final String OSMAND_ICONS_DIR = "rendering_styles/style-icons/drawable-xxhdpi/";
	private static OsmWindow minstance = null;
	private RenderingRulesStorage mRenderingRulesStorage;
	private ResourceManager mResourceManager;
	private OffRoadSettings settings = new OffRoadSettings(this);
	private OsmandSettings prefs = new OsmandSettings(this, settings);
	private R.string mStrings;
	private OsmandRegions mRegions;
	private OsmBitmapPanel mDrawPanel;
	private OsmBitmapPanelMouseAdapter mAdapter;
	private JFrame mFrame;
	private JLabel mStatusLabel;
	private Timer mMouseMoveTimer;
	private GeoServer mGeoServer;
	public int widthPixels;
	public float density;
	public int heightPixels;
	private RendererRegistry mRendererRegistry;
	private TargetPointsHelper mTargetPointsHelper;
	private RoutingHelper mRoutingHelper;
	private GeocodingLookupService mGeocodingLookupService;
	private MapPoiTypes mMapPoiTypes;
	private int mDontUpdateStatusLabelCounter;

	public void createAndShowUI() {
		mDrawPanel = new OsmBitmapPanel(this);
		mAdapter = new OsmBitmapPanelMouseAdapter(mDrawPanel);
		mDrawPanel.addMouseListener(mAdapter);
		mDrawPanel.addMouseMotionListener(mAdapter);
		mDrawPanel.addMouseWheelListener(mAdapter);
		
		mStatusLabel = new JLabel("!");
		mStatusLabel.setPreferredSize(mStatusLabel.getPreferredSize());
		mMouseMoveTimer = new Timer(500, new StatusLabelAction() );
		mMouseMoveTimer.setRepeats(true);
		mMouseMoveTimer.start();

		mFrame = new JFrame("OffRoad");
		mFrame.addKeyListener(mAdapter);
		mFrame.getContentPane().setLayout(new BorderLayout());
		mFrame.getContentPane().add(mDrawPanel, BorderLayout.CENTER);
		mFrame.getContentPane().add(mStatusLabel, BorderLayout.SOUTH);
		mFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent pE) {
				// save properties:
				saveSettings();
				mFrame.dispose();
				System.exit(0);
			}
		});
		mFrame.setResizable(true);
		mFrame.addComponentListener(mAdapter);
		JMenuBar menubar = new JMenuBar();
		JMenu jSearchMenu = new JMenu("Search");
		JMenuItem findItem = new JMenuItem("Find...");
		findItem.addActionListener(new SearchAddressAction(this));
		findItem.setAccelerator(KeyStroke.getKeyStroke("control F"));
		jSearchMenu.add(findItem);
		menubar.add(jSearchMenu);
		JMenu jDownloadMenu = new JMenu(getString("download"));
		JMenuItem downloadItem = new JMenuItem("Download...");
		downloadItem.addActionListener(new DownloadAction(this));
		downloadItem.setAccelerator(KeyStroke.getKeyStroke("control D"));
		jDownloadMenu.add(downloadItem);
		menubar.add(jDownloadMenu);
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem routeMenu = new JMenuItem(new RouteAction(this));
		popupMenu.add(routeMenu);
		mDrawPanel.setComponentPopupMenu(popupMenu);
//		findItem.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent pE) {
//				String search = JOptionPane.showInputDialog("Search item");
//				QuadRect bounds = mDrawPanel.getTileBox().getLatLonBounds();
//				mResourceManager.searchAmenitiesByName(search, bounds.top, bounds.left, bounds.bottom, bounds.right,
//						mDrawPanel.getTileBox().getLatitude(), mDrawPanel.getTileBox().getLongitude(),
//						new ResultMatcher<Amenity>() {
//
//							@Override
//							public boolean publish(Amenity pObject) {
//								System.out.println("found: " + pObject);
//								return true;
//							}
//
//							@Override
//							public boolean isCancelled() {
//								return false;
//							}
//						});
//			}
//		});
		mFrame.setJMenuBar(menubar);
		mFrame.pack();
		mFrame.setLocationRelativeTo(null);
		mFrame.setVisible(true);
	}

	public void startServer() {
		String portFile = getAppPath("port.txt").getAbsolutePath();
		if (portFile == null) {
			return;
		}
		mGeoServer = new GeoServer(portFile, this);
		mGeoServer.start();
	}
	
	public String getString(String pString) {
		// TODO: Translate
		return pString;
	}

	public OsmBitmapPanel getDrawPanel() {
		return mDrawPanel;
	}

	public static void main(String[] args) throws XmlPullParserException, IOException {
		final OsmWindow win = OsmWindow.getInstance();
		win.init();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				win.createAndShowUI();
			}
		});

	}

	protected void saveSettings() {
		RotatedTileBox tileBox = mDrawPanel.getTileBox();
		prefs.setLastKnownMapLocation(tileBox.getLatitude(), tileBox.getLongitude());
		prefs.setLastKnownMapZoom(tileBox.getZoom());
		settings.save();
	}

	public OsmWindow() {
		prefs.APPLICATION_MODE.set(ApplicationMode.DEFAULT);
        Dimension size = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        widthPixels = size.width;
        heightPixels = size.height;
        density = java.awt.Toolkit.getDefaultToolkit().getScreenResolution()/96f;
		mStrings = new R.string();
		mRegions = new OsmandRegions();
		mResourceManager = new ResourceManager(this);
		mResourceManager.indexingMaps(IProgress.EMPTY_PROGRESS);
		scaleAllFonts(density);
		startServer();
		mRendererRegistry = new RendererRegistry(this);
		mTargetPointsHelper = new TargetPointsHelper(this);
		mRoutingHelper = new RoutingHelper(this);
		mGeocodingLookupService = new GeocodingLookupService(this);
		mMapPoiTypes = MapPoiTypes.getDefault();
	}
	
//	private void initPoiTypes() {
//		if (getAppPath("poi_types.xml").exists()) {
//			poiTypes.init(getAppPath("poi_types.xml").getAbsolutePath());
//		} else {
//			poiTypes.init();
//		}
//		poiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {
//
//			@Override
//			public String getTranslation(AbstractPoiType type) {
//				if (type.getBaseLangType() != null) {
//					return getTranslation(type.getBaseLangType()) + " ("
//							+ getLangTranslation(type.getLang()).toLowerCase() + ")";
//				}
//				try {
//					Field f = R.string.class.getField("poi_" + type.getIconKeyName());
//					if (f != null) {
//						Integer in = (Integer) f.get(null);
//						return getString(in);
//					}
//				} catch (Exception e) {
//					System.err.println("No translation for " + type.getIconKeyName() + " " + e.getMessage());
//				}
//				return null;
//			}
//		});
//	}

	public String getLangTranslation(String l) {
		try {
			java.lang.reflect.Field f = R.string.class.getField("lang_" + l);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return l;
	}

	public static void scaleAllFonts(float pScale) {
		for (Iterator i = UIManager.getLookAndFeelDefaults().keySet()
				.iterator(); i.hasNext();) {
			Object next = i.next();
			if (next instanceof String) {
				String key = (String) next;
				if (key.endsWith(".font")) {
					Font font = UIManager.getFont(key);
					Font biggerFont = font.deriveFont(pScale * font.getSize2D());
					// change ui default to bigger font
					UIManager.put(key, biggerFont);
				}				
			}
		}
	}


	public void loadMGap(Graphics2D pG2, RotatedTileBox pTileRect) {
		getRenderer().loadMGap(pG2, pTileRect, getRenderingRulesStorage());
	}

	public MapRenderRepositories getRenderer() {
		return mResourceManager.getRenderer();
	}

	private void init() throws XmlPullParserException, IOException {
		mRenderingRulesStorage = initRenderingRulesStorage();
	}

	public RenderingRulesStorage initRenderingRulesStorage() throws XmlPullParserException, IOException {
//		final String loc = getAppPath(RENDERING_STYLES_DIR).getAbsolutePath() + File.separator;
		final String loc = File.separator +  RENDERING_STYLES_DIR; // + File.separator;
		String defaultFile = loc + "default.render.xml";
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		InputStream is = this.getClass().getResourceAsStream(loc + "default.render.xml");
//		System.out.println("Stream: " +is + ", loc " + loc);
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
//				depends.parseRulesFromXmlInputStream(new FileInputStream(loc + name + ".render.xml"), ref);
				depends.parseRulesFromXmlInputStream(this.getClass().getResourceAsStream(loc + name + ".render.xml"), ref);
				return depends;
			}
		};
//		is = new FileInputStream(defaultFile);
		is = this.getClass().getResourceAsStream(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);

		return storage;
	}

	public InputStream getResource(String pIndex){
		if(pIndex != null){
			String name = pIndex;
			if(!name.startsWith(File.separator)){
				name = File.separator+pIndex;
			}
			return this.getClass().getResourceAsStream(name);
		}
		return null;
	}
	
	public File getAppPath(String pIndex) {
		if (pIndex == null) {
			pIndex = "";
		}
		String pathname = System.getProperty("user.dir") + System.getProperty("file.separator") + pIndex;
		System.out.println("Searching for " + pathname);
		return new File(pathname);
	}

	public OsmandSettings getSettings() {
		return prefs;
	}

	public String getString(int pKey) {
		return mStrings.hash.get(pKey);
	}

	public static OsmWindow getInstance() {
		if (minstance == null) {
			minstance = new OsmWindow();
		}
		return minstance;
	}

	public OsmandRegions getRegions() {
		return mRegions;
	}

	public ResourceManager getResourceManager() {
		return mResourceManager;
	}

	public Frame getWindow() {
		return mFrame;
	}

	public void move(LatLon pLocation, QuadRectExtendable pQuadRectExtendable) {
		// make sure that all points of the rect are in:
		if (pQuadRectExtendable!= null) {
			RotatedTileBox tileBox = mDrawPanel.getTileBox();
			tileBox.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
			tileBox.setZoom(MAX_ZOOM);
			while (!tileBox.containsLatLon(pQuadRectExtendable.left, pQuadRectExtendable.top)) {
				tileBox.setZoom(tileBox.getZoom() - 1);
			}
			while (!tileBox.containsLatLon(pQuadRectExtendable.right, pQuadRectExtendable.bottom)) {
				tileBox.setZoom(tileBox.getZoom() - 1);
			} 
		}
		mDrawPanel.move(pLocation);
		mDrawPanel.setCursor(pLocation);
	}

	public void setWaitingCursor(boolean waiting) {
		Component glassPane = mFrame.getRootPane().getGlassPane();
		if (waiting) {
			glassPane.setCursor(
					Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			glassPane.setVisible(true);
		} else {
			glassPane.setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			glassPane.setVisible(false);
		}
	}
	
	public class StatusLabelAction extends AbstractAction {

		@Override
		public void actionPerformed(ActionEvent pE) {
			if(mDontUpdateStatusLabelCounter>0){
				mDontUpdateStatusLabelCounter--;
				return;
			}
			// calculate the distance to the cursor
			MouseEvent e = getLastMouseEvent();
			LatLon cursorPosition = mDrawPanel.getCursorPosition();
			if(e == null || cursorPosition == null){
				mStatusLabel.setText("");
				return;
			}
			LatLon mousePosition = mDrawPanel.getTileBox().getLatLonFromPixel(e.getX(), e.getY());
			double distance = MapUtils.getDistance(mousePosition, cursorPosition)/1000d;
			Object[] messageArguments = { new Double(distance),
					new Double(cursorPosition.getLatitude()),
					new Double(cursorPosition.getLongitude()) };
			MessageFormat formatter = new MessageFormat(
					"Distance (bee-line) between mouse and cursor: {0,number,###,###.###}km. Position: {1,number,###.####}, {2,number,###.####}.");
			String message = formatter.format(messageArguments);
			mStatusLabel.setText(message);
		}
		
	}

	public RendererRegistry getRendererRegistry() {
		return mRendererRegistry;
	}

	public Builder getDefaultRoutingConfig() {
		long tm = System.currentTimeMillis();
		try {
			InputStream routingXml = getResource(IndexConstants.ROUTING_XML_FILE);
			if (routingXml != null) {
				try {
					return RoutingConfiguration.parseFromInputStream(routingXml);
				} catch (XmlPullParserException | IOException e) {
					throw new IllegalStateException(e);
				}
			} else {
				return RoutingConfiguration.getDefault();
			}
		} finally {
			long te = System.currentTimeMillis();
			if (te - tm > 30) {
				System.err.println("Defalt routing config init took " + (te - tm) + " ms");
			}
		}
	}

	public static GeneralRouter getRouter(net.osmand.router.RoutingConfiguration.Builder builder, ApplicationMode am) {
		GeneralRouter router = builder.getRouter(am.getStringKey());
		if (router == null && am.getParent() != null) {
			router = builder.getRouter(am.getParent().getStringKey());
		}
		return router;
	}

	public String getString(int pKey, Object... pObj) {
		return MessageFormat.format(getString(pKey), pObj);
	}

	public TargetPointsHelper getTargetPointsHelper() {
		return mTargetPointsHelper;
	}

	public RoutingHelper getRoutingHelper() {
		return mRoutingHelper;
	}

	public GeocodingLookupService getGeocodingLookupService() {
		return mGeocodingLookupService;
	}

	public Object getLocationProvider() {
		return null;
	}

	public MapPoiTypes getPoiTypes() {
		return mMapPoiTypes;
	}

	public MouseEvent getLastMouseEvent() {
		return mAdapter.getMouseEvent();
	}
	
	public RenderingRulesStorage getRenderingRulesStorage() {
		return mRenderingRulesStorage;
	}

	public void showToastMessage(String pMsg) {
		mStatusLabel.setText(pMsg);
		mDontUpdateStatusLabelCounter = 4;
	}

	public void runInUIThread(Runnable pRunnable) {
		SwingUtilities.invokeLater(pRunnable);
	}

}
