package net.sourceforge.offroad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.actions.DownloadAction;
import net.sourceforge.offroad.actions.NavigationBackAction;
import net.sourceforge.offroad.actions.NavigationForwardAction;
import net.sourceforge.offroad.actions.OffRoadAction.OffRoadMenuItem;
import net.sourceforge.offroad.actions.PoiFilterAction;
import net.sourceforge.offroad.actions.RouteAction;
import net.sourceforge.offroad.actions.SearchAddressAction;
import net.sourceforge.offroad.data.QuadRectExtendable;
import net.sourceforge.offroad.res.ResourceTest;
import net.sourceforge.offroad.res.Resources;
import net.sourceforge.offroad.ui.BlindIcon;

/**
 * OffRoad
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow {
	private class PoiContextMenuListener implements PopupMenuListener {
		
		private JPopupMenu mMenu;
		private Vector<JMenuItem> items = new Vector<>();

		public PoiContextMenuListener(JPopupMenu pMenu) {
			mMenu = pMenu;
		}
		
		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent pE) {
			RotatedTileBox tileBox = getDrawPanel().getTileBox();
			List<Amenity> res = new Vector<Amenity>();
			POIMapLayer poiLayer = getDrawPanel().getPoiLayer();
			poiLayer.getAmenityFromPoint(tileBox, mAdapter.getMouseEvent().getPoint(), res);
			HashSet<Amenity> resSet = new HashSet<>(res);
			System.out.println("res: " + res);
			for (Amenity am : resSet) {
				JMenuItem item = new JMenuItem(am.getName(), getImageIcon(poiLayer, am));
				item.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent pE) {
						POIMapLayer.showDescriptionDialog(OsmWindow.this, getInstance(), am.getAdditionalInfo().toString(), am.toString());
					}
				});
				items.add(item);
				mMenu.add(item);
			}
		}

		public javax.swing.Icon getImageIcon(POIMapLayer poiLayer, Amenity am) {
			BufferedImage bitmap = poiLayer.getBitmap(am);
			if(bitmap != null)	{
				return new ImageIcon(bitmap);
			} else {
				return new BlindIcon(20);
			}
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent pE) {
			for (JMenuItem jMenuItem : items) {
				mMenu.remove(jMenuItem);
			}
			items.clear();
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent pE) {
			// TODO Auto-generated method stub
			
		}
	}

	public class MapPointStorage {

		private LatLon mPoint;
		private int mZoom;

		public MapPointStorage(LatLon pPoint, int pZoom) {
			mPoint = pPoint;
			mZoom = pZoom;
		}

	}

	private final static Log log = PlatformUtil.getLog(OsmWindow.class);

	
	static final int MAX_ZOOM = 22;
	public static final String RENDERING_STYLES_DIR = "rendering_styles/"; //$NON-NLS-1$
	public static final String OSMAND_ICONS_DIR = RENDERING_STYLES_DIR + "style-icons/drawable-xxhdpi/"; //$NON-NLS-1$
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
	private JLabel mRouteProgressStatus;
	private JProgressBar mRouteProgressBar;
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
	private Resources mResourceStrings;


	public static final String IMAGE_PATH = "drawable-xxhdpi/"; //$NON-NLS-1$


	private PropertyResourceBundle mOffroadResources;


	private Vector<MapPointStorage> mPointStorage = new Vector<>();
	private int mPointStorageIndex = -1;


	private JPanel mStatusBar;


	private PoiFiltersHelper mPoiFilters;

	public void createAndShowUI() {
		mDrawPanel = new OsmBitmapPanel(this);
		mAdapter = new OsmBitmapPanelMouseAdapter(mDrawPanel);
		mDrawPanel.addMouseListener(mAdapter);
		mDrawPanel.addMouseMotionListener(mAdapter);
		mDrawPanel.addMouseWheelListener(mAdapter);
		
		mStatusLabel = new JLabel("!"); //$NON-NLS-1$
		mStatusLabel.setPreferredSize(mStatusLabel.getPreferredSize());
		mRouteProgressBar = new JProgressBar();
		mRouteProgressBar.setMaximum(100);
		mRouteProgressBar.setStringPainted(true);
		mRouteProgressBar.setVisible(false);
		mRouteProgressStatus = new JLabel("!");
		mStatusBar=new JPanel();
		mStatusBar.setLayout(new GridBagLayout());
		mStatusBar.add(mStatusLabel, new GridBagConstraints(0, 0, 1, 1, 3, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mStatusBar.add(mRouteProgressStatus, new GridBagConstraints(1, 0, 1, 1, 0, 1, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		mStatusBar.add(mRouteProgressBar, new GridBagConstraints(2, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		

		mMouseMoveTimer = new Timer(500, new StatusLabelAction() );
		mMouseMoveTimer.setRepeats(true);
		mMouseMoveTimer.start();

		mFrame = new JFrame(getOffRoadString("offroad.string4")); //$NON-NLS-1$
		mFrame.addKeyListener(mAdapter);
		mFrame.getContentPane().setLayout(new BorderLayout());
		mFrame.getContentPane().add(mDrawPanel, BorderLayout.CENTER);
		mFrame.getContentPane().add(mStatusBar, BorderLayout.SOUTH);
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
		JMenu jFileMenu = new JMenu(getOffRoadString("offroad.string5")); //$NON-NLS-1$
		JMenuItem saveItem = new JMenuItem(getOffRoadString("offroad.string6")); //$NON-NLS-1$
		saveItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent pE) {
				JFileChooser chooser = new JFileChooser();
				int showSaveDialog = chooser.showSaveDialog(mFrame);
				if(showSaveDialog == JFileChooser.APPROVE_OPTION){
					mDrawPanel.saveImage(chooser.getSelectedFile());
				}
			}
		});
		jFileMenu.add(saveItem);
		menubar.add(jFileMenu);
		JMenu jSearchMenu = new JMenu(getOffRoadString("offroad.string7")); //$NON-NLS-1$
		JMenuItem findItem = new JMenuItem(getOffRoadString("offroad.string8")); //$NON-NLS-1$
		findItem.addActionListener(new SearchAddressAction(this));
		findItem.setAccelerator(KeyStroke.getKeyStroke("control F")); //$NON-NLS-1$
		jSearchMenu.add(findItem);
		menubar.add(jSearchMenu);
		// Download
		JMenu jDownloadMenu = new JMenu(getOffRoadString("offroad.download")); //$NON-NLS-1$
		JMenuItem lDownloadItem = new JMenuItem(getOffRoadString("offroad.string11")); //$NON-NLS-1$
		lDownloadItem.addActionListener(new DownloadAction(this));
		lDownloadItem.setAccelerator(KeyStroke.getKeyStroke("control D")); //$NON-NLS-1$
		jDownloadMenu.add(lDownloadItem);
		menubar.add(jDownloadMenu);
		// Navigation
		JMenu jNavigationMenu = new JMenu(getOffRoadString("offroad.navigation")); //$NON-NLS-1$
		JMenuItem navigationBackItem = new JMenuItem(getOffRoadString("offroad.back")); //$NON-NLS-1$
		navigationBackItem.addActionListener(new NavigationBackAction(this));
		navigationBackItem.setAccelerator(KeyStroke.getKeyStroke("alt LEFT")); //$NON-NLS-1$
		jNavigationMenu.add(navigationBackItem);
		JMenuItem navigationForwardItem = new JMenuItem(getOffRoadString("offroad.forward")); //$NON-NLS-1$
		navigationForwardItem.addActionListener(new NavigationForwardAction(this));
		navigationForwardItem.setAccelerator(KeyStroke.getKeyStroke("alt RIGHT")); //$NON-NLS-1$
		jNavigationMenu.add(navigationForwardItem);
		menubar.add(jNavigationMenu);
		// PointOfInterest
		JMenu jPointOfInterestMenu = new JMenu(getOffRoadString("offroad.PointOfInterest")); //$NON-NLS-1$
		JMenuItem lPointOfInterestOffItem = new OffRoadMenuItem(new PoiFilterAction(this, null), jPointOfInterestMenu);
		jPointOfInterestMenu.add(lPointOfInterestOffItem);
		for (PoiUIFilter filter : mPoiFilters.getTopDefinedPoiFilters()) {
			JMenuItem lPointOfInterestItem = new OffRoadMenuItem(new PoiFilterAction(this, filter), jPointOfInterestMenu); 
			jPointOfInterestMenu.add(lPointOfInterestItem);
		}
		menubar.add(jPointOfInterestMenu);
		
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem routeMenu = new JMenuItem(new RouteAction(this));
		popupMenu.add(routeMenu);
		mDrawPanel.setComponentPopupMenu(popupMenu);
		popupMenu.addPopupMenuListener(new PoiContextMenuListener(popupMenu));
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
		String portFile = getAppPath("port.txt").getAbsolutePath(); //$NON-NLS-1$
		if (portFile == null) {
			return;
		}
		mGeoServer = new GeoServer(portFile, this);
		mGeoServer.start();
	}
	
	public String getOffRoadString(String pString) {
		if(mOffroadResources.containsKey(pString)){
			return mOffroadResources.getString(pString);
		}
		return "TRANSLATE_ME:" + pString; //$NON-NLS-1$
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
		initStrings();
		prefs.APPLICATION_MODE.set(ApplicationMode.DEFAULT);
        Dimension size = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        widthPixels = size.width;
        heightPixels = size.height;
        density = java.awt.Toolkit.getDefaultToolkit().getScreenResolution()/96f;
		mStrings = new R.string();
		mRegions = new OsmandRegions();
		mResourceManager = new ResourceManager(this);
		mResourceManager.indexingMaps(IProgress.EMPTY_PROGRESS);
		if(System.getProperty("HIDPI")!=null){ //$NON-NLS-1$
			scaleAllFonts(1.3f);
		} else {
			scaleAllFonts(density);
		}
		startServer();
		mRendererRegistry = new RendererRegistry(this);
		mTargetPointsHelper = new TargetPointsHelper(this);
		mRoutingHelper = new RoutingHelper(this);
		mPoiFilters = new PoiFiltersHelper(this);
		mGeocodingLookupService = new GeocodingLookupService(this);
		mMapPoiTypes = MapPoiTypes.getDefault();
		mMapPoiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {
			@Override
			public String getTranslation(AbstractPoiType type) {
				if (type.getBaseLangType() != null) {
					return getTranslation(type.getBaseLangType()) + " ("
							+ getLangTranslation(type.getLang()).toLowerCase() + ")";
				}
				return getString("poi_" + type.getIconKeyName());
			}
		});
	}

	private void initStrings() {
		// read resources:
		String ct = getCountry();
		loadStrings(ct, "strings.xml");
		loadStrings(ct, "phrases.xml");
		// get offroad strings:
		try {
			mOffroadResources = getLanguageResources(ct);
			if(mOffroadResources==null){
				mOffroadResources = getLanguageResources("en"); //$NON-NLS-1$
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadStrings(String ct, String fileName) {
		InputStream is = getResource("res/values-" + ct + "/" + fileName);
		if (is == null) {
			is = getResource("res/values/" + fileName); // $NON-NLS-1$
		}
		log.info("Trying to load resources " + is); //$NON-NLS-1$
		Resources resourceStrings;
		try {
			JAXBContext jc = JAXBContext.newInstance(ResourceTest.class.getPackage().getName());
			Unmarshaller u = jc.createUnmarshaller();
			resourceStrings = (Resources) u.unmarshal(is);
		} catch (JAXBException e) {
			e.printStackTrace();
			resourceStrings = new Resources();
		}
		if(mResourceStrings == null){
			mResourceStrings = new Resources();
		}
		mResourceStrings.getString().addAll(resourceStrings.getString());
	}

	private PropertyResourceBundle getLanguageResources(String lang)
			throws IOException {
		InputStream is = getResource("res/OffRoad_Resources_" + lang //$NON-NLS-1$
				+ ".properties"); //$NON-NLS-1$
		if (is == null) {
			return null;
		}
		PropertyResourceBundle bundle = new PropertyResourceBundle(is);
		is.close();
		return bundle;
	}

	public String getLangTranslation(String l) {
		return getString("lang_" + l);
	}

	public static void scaleAllFonts(float pScale) {
		for (Iterator i = UIManager.getLookAndFeelDefaults().keySet()
				.iterator(); i.hasNext();) {
			Object next = i.next();
			if (next instanceof String) {
				String key = (String) next;
				if (key.endsWith(".font")) { //$NON-NLS-1$
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
		final String loc = RENDERING_STYLES_DIR; 
		String res; 
		res = loc + "default.render.xml";
//		res = loc + "LightRS.render.xml";
//		res = loc + "UniRS.render.xml";
//		res = loc + "regions.render.xml";
		String defaultFile = res;
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		InputStream is = getResource(res);
		if(is == null){
			System.err.println("Can't find resource " + res); //$NON-NLS-1$
			printClassPath();
			System.exit(1);
		}
		try {
			XmlPullParser parser = PlatformUtil.newXMLPullParser();
			parser.setInput(is, "UTF-8"); //$NON-NLS-1$
			int tok;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if (tagName.equals("renderingConstant")) { //$NON-NLS-1$
						if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) { //$NON-NLS-1$ //$NON-NLS-2$
							renderingConstants.put(parser.getAttributeValue("", "name"), //$NON-NLS-1$ //$NON-NLS-2$
									parser.getAttributeValue("", "value")); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		} finally {
			is.close();
		}
		RenderingRulesStorage storage = new RenderingRulesStorage("default", renderingConstants); //$NON-NLS-1$
		final RenderingRulesStorageResolver resolver = new RenderingRulesStorageResolver() {
			@Override
			public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref)
					throws XmlPullParserException, IOException {
				RenderingRulesStorage depends = new RenderingRulesStorage(name, renderingConstants);
				depends.parseRulesFromXmlInputStream(getResource(loc + name + ".render.xml"), ref); //$NON-NLS-1$
				return depends;
			}
		};
		is = getResource(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);

		return storage;
	}
	
	static public void printClassPath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();
		System.out.println("Classpath:"); //$NON-NLS-1$
		for (URL url : urls) {
			System.out.println(url.getFile());
		}

	}

	public InputStream getResource(String pIndex){
		if(pIndex != null){
			String name = pIndex;
			InputStream is = this.getClass().getResourceAsStream(name);
			if(is == null){
				name = "/"+pIndex; //$NON-NLS-1$
				is = this.getClass().getResourceAsStream(name);
				if(is == null){
					System.err.println("ERROR: Resource not found: "  + pIndex); //$NON-NLS-1$
					printClassPath();
				} else {
					System.err.println("WARNING: Found path as " + name + " instead of " + pIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return is;
		}
		return null;
	}
	
	public File getAppPath(String pIndex) {
		if (pIndex == null) {
			pIndex = ""; //$NON-NLS-1$
		}
		String pathname = getAppPathName(pIndex);
		log.info("Searching for " + pathname); //$NON-NLS-1$
		return new File(pathname);
	}

	public static String getAppPathName(String pIndex) {
		String pathname = System.getProperty("user.home") + File.separator  + ".OffRoad" + File.separator + pIndex; //$NON-NLS-1$ //$NON-NLS-2$
		return pathname;
	}

	public OsmandSettings getSettings() {
		return prefs;
	}

	public String getString(int pKey) {
		String stringKey = mStrings.hash.get(pKey);
		return getString(stringKey);
	}

	public String getString(String stringKey) {
		for (Resources.String str : mResourceStrings.getString()) {
			if(str.getName() != null && str.getName().equals(stringKey)){
				log.debug("String " + stringKey + "=" + str.getValue()) ;
				return str.getValue();
			}
		}
		log.error("String key " + stringKey + " not found!");
		return stringKey;
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
		RotatedTileBox tileBox = mDrawPanel.getTileBox();
		if (pQuadRectExtendable!= null) {
			tileBox.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
			tileBox.setZoom(MAX_ZOOM);
			while (!tileBox.containsLatLon(pQuadRectExtendable.left, pQuadRectExtendable.top)) {
				tileBox.setZoom(tileBox.getZoom() - 1);
			}
			while (!tileBox.containsLatLon(pQuadRectExtendable.right, pQuadRectExtendable.bottom)) {
				tileBox.setZoom(tileBox.getZoom() - 1);
			} 
		}
		mDrawPanel.move(pLocation, tileBox.getZoom());
		mDrawPanel.setCursor(pLocation);
		addPoint(pLocation);
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
				mStatusLabel.setText(""); //$NON-NLS-1$
				return;
			}
			LatLon mousePosition = mDrawPanel.getTileBox().getLatLonFromPixel(e.getX(), e.getY());
			double distance = MapUtils.getDistance(mousePosition, cursorPosition)/1000d;
			Object[] messageArguments = { new Double(distance),
					new Double(cursorPosition.getLatitude()),
					new Double(cursorPosition.getLongitude()) };
			MessageFormat formatter = new MessageFormat(
					getOffRoadString("offroad.string47")); //$NON-NLS-1$
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
				System.err.println("Routing configuration not found!"); //$NON-NLS-1$
				return RoutingConfiguration.getDefault();
			}
		} finally {
			long te = System.currentTimeMillis();
			if (te - tm > 30) {
				System.err.println("Defalt routing config init took " + (te - tm) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	public void addPoint(LatLon pPoint){
		MapPointStorage storage = new MapPointStorage(pPoint, getZoom());
		if(mPointStorageIndex != mPointStorage.size()-1){
			// remove all subsequent:
			for(int i=mPointStorageIndex+1; i < mPointStorage.size() ; ++i){
				mPointStorage.remove(i);
			}
		}
		mPointStorage.add(storage);
		mPointStorageIndex = mPointStorage.size()-1;
	}

	public int getZoom() {
		return getDrawPanel().getTileBox().getZoom();
	}

	public void back() {
		if(mPointStorage.isEmpty()){
			return;
		}
		if(mPointStorageIndex <= 0 || mPointStorageIndex >= mPointStorage.size()){
			return;
		}
		mPointStorageIndex--;
		MapPointStorage pointStorage = mPointStorage.get(mPointStorageIndex);
		mDrawPanel.move(pointStorage.mPoint, pointStorage.mZoom);
		mDrawPanel.setCursor(pointStorage.mPoint);
	}

	public void forward() {
		if(mPointStorage.isEmpty()){
			return;
		}
		if(mPointStorageIndex < 0 || mPointStorageIndex >= mPointStorage.size()-1){
			return;
		}
		mPointStorageIndex++;
		MapPointStorage pointStorage = mPointStorage.get(mPointStorageIndex);
		mDrawPanel.move(pointStorage.mPoint, pointStorage.mZoom);
		mDrawPanel.setCursor(pointStorage.mPoint);
	}
	
	public void setCursor(Point pPoint) {
		mDrawPanel.setCursor(pPoint);
		addPoint(mDrawPanel.getCursorPosition());
	}

	public void runInUIThread(Runnable pRunnable, int pDelay) {
		Timer timer = new Timer(pDelay, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent pE) {
				pRunnable.run();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	public void setProgress(int pPercent){
		if(pPercent < 100){
			mRouteProgressBar.setVisible(true);
			mRouteProgressBar.setValue(pPercent);
		} else {
			mRouteProgressBar.setVisible(false);
		}
	}
	
	public void setStatus(String pStatus){
		mRouteProgressStatus.setText(pStatus);
	}

	public String getLanguage() {
		// FIXME: What is meant here?
		return getCountry();
	}

	public String getCountry() {
		Locale locale = Locale.getDefault();
		String ct = locale.getCountry().toLowerCase();
		return ct;
	}

	public PoiFiltersHelper getPoiFilters() {
		return mPoiFilters;
	}
	

}
