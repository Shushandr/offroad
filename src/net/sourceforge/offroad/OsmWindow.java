package net.sourceforge.offroad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
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
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.inapp.util.Base64;
import net.osmand.plus.inapp.util.Base64DecoderException;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.actions.AddFavoriteAction;
import net.sourceforge.offroad.actions.ChooseRendererAction;
import net.sourceforge.offroad.actions.ChooseRouteServiceAction;
import net.sourceforge.offroad.actions.ClearRouteAction;
import net.sourceforge.offroad.actions.DownloadAction;
import net.sourceforge.offroad.actions.GpxImportAction;
import net.sourceforge.offroad.actions.NavigationBackAction;
import net.sourceforge.offroad.actions.NavigationForwardAction;
import net.sourceforge.offroad.actions.NavigationRotationAction;
import net.sourceforge.offroad.actions.OffRoadAction.OffRoadMenuItem;
import net.sourceforge.offroad.actions.PoiFilterAction;
import net.sourceforge.offroad.actions.PointNavigationAction;
import net.sourceforge.offroad.actions.PointNavigationAction.HelperAction;
import net.sourceforge.offroad.actions.RouteAction;
import net.sourceforge.offroad.actions.SearchAddressAction;
import net.sourceforge.offroad.actions.ShowFavoriteAction;
import net.sourceforge.offroad.actions.ShowWikipediaAction;
import net.sourceforge.offroad.data.QuadRectExtendable;
import net.sourceforge.offroad.data.SQLiteImpl;
import net.sourceforge.offroad.data.persistence.ComponentLocationStorage;
import net.sourceforge.offroad.data.persistence.OsmWindowLocationStorage;
import net.sourceforge.offroad.res.OffRoadResources;
import net.sourceforge.offroad.res.ResourceTest;
import net.sourceforge.offroad.res.Resources;
import net.sourceforge.offroad.ui.AmenityTablePanel;
import net.sourceforge.offroad.ui.BlindIcon;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.OsmBitmapPanelMouseAdapter;
import net.sourceforge.offroad.ui.PoiFilterRenderer;
import net.sourceforge.offroad.ui.SetCursorRadiusAction;

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
			RotatedTileBox tileBox = getDrawPanel().copyCurrentTileBox();
			List<Amenity> res = new Vector<Amenity>();
			POIMapLayer poiLayer = getDrawPanel().getPoiLayer();
			poiLayer.getAmenityFromPoint(tileBox, mAdapter.getMouseEvent().getPoint(), res);
			HashSet<Amenity> resSet = new HashSet<>(res);
			System.out.println("res: " + res);
			for (Amenity am : resSet) {
				String name = am.getName(getLanguage());
				JMenuItem item = new JMenuItem(name, getImageIcon(am));
				item.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent pE) {
						if(am.getType().isWiki()){
							POIMapLayer.showWikipediaDialog(OsmWindow.this, OsmWindow.this, am);
						} else {
							String locationName = PointDescription.getLocationName(OsmWindow.this,
	                                am.getLocation().getLatitude(), am.getLocation().getLongitude(), true);
							POIMapLayer.showDescriptionDialog(OsmWindow.this, getInstance(), am.getAdditionalInfo().toString(), name);
						}
					}
				});
				items.add(item);
				mMenu.add(item);
			}
		}

		public javax.swing.Icon getImageIcon(Amenity am) {
			BufferedImage bitmap = getBitmap(am);
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
			popupMenuWillBecomeInvisible(pE);
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

	
	public static final int MAX_ZOOM = 22;
	public static final String RENDERING_STYLES_DIR = "rendering_styles/"; //$NON-NLS-1$
	public static final String OSMAND_ICONS_DIR = RENDERING_STYLES_DIR + "style-icons/drawable-xxhdpi/"; //$NON-NLS-1$
	public static final String IMAGE_PATH = "drawable-xhdpi/"; //$NON-NLS-1$
	public static final String PROXY_PORT = "proxy.port";
	public static final String PROXY_HOST = "proxy.host";
	public static final String PROXY_PASSWORD = "proxy.password";
	public static final String PROXY_USER = "proxy.user";
	public static final String PROXY_IS_AUTHENTICATED = "proxy.is_authenticated";
	public static final String PROXY_USE_SETTINGS = "proxy.use_settings";
	public static final String PROXY_EXCEPTION = "proxy.exception";


	private static final String OFFROAD_PROPERTIES = "offroad";


	public static final int MIN_ZOOM = 1;
	private static OsmWindow sInstance = null;
	private ResourceManager mResourceManager;
	private OffRoadSettings settings = new OffRoadSettings(this);
	private OsmandSettings prefs = new OsmandSettings(this, settings);
	private Properties mOffroadProperties = (Properties) settings.getPreferenceObject("offroad");
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
	private PropertyResourceBundle mOffroadResources;
	private Vector<MapPointStorage> mPointStorage = new Vector<>();
	private int mPointStorageIndex = -1;
	private JPanel mStatusBar;
	private PoiFiltersHelper mPoiFilters;
	private AmenityTablePanel mAmenityTable;
	private MapMarkersHelper mMapMarkersHelper;


	private JToolBar mToolBar;
	private JTextField mSearchTextField;
	private JComboBox<PoiUIFilter> mComboBox;
	private DefaultComboBoxModel<PoiUIFilter> mComboBoxModel;
	private boolean mSearchBarVisible = true;
	Vector<CursorPositionListener> mCursorPositionListeners = new Vector<>();
	private PoiUIFilter mCurrentPoiFilter;


	private SQLiteImpl mSqLiteImpl;


	private FavouritesDbHelper mFavorites;


	private OsmAndLocationProvider mOsmAndLocationProvider;


	private JSplitPane mSplitPane;


	private OffRoadResources mOffRoadResources;


	private GpxSelectionHelper mGpxSelectionHelper;


	private SavingTrackHelper mSavingTrackHelper;


	private VersionInfo mVersionInfo;

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

		mToolBar = new JToolBar(JToolBar.HORIZONTAL);
		mToolBar.setLayout(new GridBagLayout());
		mToolBar.add(new JLabel(getOffRoadString("offroad.search")), new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		
		mSearchTextField = new JTextField();
		mSearchTextField.getInputMap().put(KeyStroke.getKeyStroke("control LEFT"), "none");
		mSearchTextField.getInputMap().put(KeyStroke.getKeyStroke("control RIGHT"), "none");
		mSearchTextField.setText(getSettings().SELECTED_POI_FILTER_STRING_FOR_MAP.get());
		mAmenityTable = new AmenityTablePanel(this);
		getSearchTextField().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent pE) {
				setPoiFilter(getCurrentPoiUIFilter(), getSearchTextField().getText());
			}
		});
		getSearchTextField().addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent pE) {
			}
			
			@Override
			public void keyReleased(KeyEvent pE) {
			}
			
			@Override
			public void keyPressed(KeyEvent pE) {
				if(!pE.isControlDown()){
					return;
				}
				switch (pE.getKeyCode()) {
				case KeyEvent.VK_UP:
					if(mComboBox.getSelectedIndex()>0){
						mComboBox.setSelectedIndex(mComboBox.getSelectedIndex()-1);
					}
					pE.consume();
					return;
				case KeyEvent.VK_DOWN:
					if(mComboBox.getSelectedIndex()<mComboBox.getItemCount()-1){
						mComboBox.setSelectedIndex(mComboBox.getSelectedIndex()+1);
					}
					pE.consume();
					return;
				}
		}
		});
		mToolBar.add(getSearchTextField(), new GridBagConstraints(1, 0, 1, 1, 3, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10, 0, 10, 0), 0, 0));
		mComboBox = new JComboBox<PoiUIFilter>();
		mComboBoxModel = new DefaultComboBoxModel<PoiUIFilter>();
		mCurrentPoiFilter = mPoiFilters.getSearchByNamePOIFilter();
		mComboBoxModel.addElement(mPoiFilters.getSearchByNamePOIFilter());
		mComboBoxModel.addElement(mPoiFilters.getNominatimAddressFilter());
		mComboBoxModel.addElement(mPoiFilters.getNominatimPOIFilter());
		for (PoiUIFilter filter : mPoiFilters.getTopDefinedPoiFilters()) {
			mComboBoxModel.addElement(filter);
		}
		mComboBox.setModel(mComboBoxModel);
		mComboBox.setFocusable(false);
		mComboBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent pE) {
				if(mComboBox.getSelectedIndex() >= 0){
					mCurrentPoiFilter = mComboBoxModel.getElementAt(mComboBox.getSelectedIndex());
				}
			}
		});
		mComboBox.setRenderer(new PoiFilterRenderer());
		mToolBar.add(mComboBox, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		mFrame = new JFrame(getOffRoadString("offroad.string4")); //$NON-NLS-1$
		mFrame.getContentPane().setLayout(new BorderLayout());
		mFrame.getContentPane().add(mToolBar, BorderLayout.NORTH);
		mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mAmenityTable, mDrawPanel);
		mFrame.getContentPane().add(mSplitPane);
		mFrame.getContentPane().add(mStatusBar, BorderLayout.SOUTH);
		mFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent pE) {
				closeWindow();
			}
		});
		mFrame.setResizable(true);
		mDrawPanel.addComponentListener(mAdapter);
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
		JMenuItem importGpxItem = new JMenuItem(getOffRoadString("offroad.import_gpx")); //$NON-NLS-1$
		importGpxItem.addActionListener(new GpxImportAction(this));
		jFileMenu.add(importGpxItem);
		addToMenu(jFileMenu, "offroad.export_route", new ExportRouteAction(this), null);
		addToMenu(jFileMenu, "offroad.exit", item -> closeWindow(), "control Q");
		menubar.add(jFileMenu);
		JMenu jSearchMenu = new JMenu(getOffRoadString("offroad.string7")); //$NON-NLS-1$
		JMenuItem findItem = new JMenuItem(getOffRoadString("offroad.string8")); //$NON-NLS-1$
		findItem.addActionListener(new SearchAddressAction(this));
		findItem.setAccelerator(KeyStroke.getKeyStroke("control F")); //$NON-NLS-1$
		jSearchMenu.add(findItem);
		JMenuItem gotoSearchFieldItem = new JMenuItem(getOffRoadString("offroad.gotoSearchField")); //$NON-NLS-1$
		gotoSearchFieldItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent pE) {
				getSearchTextField().selectAll();
				getSearchTextField().requestFocus();
			}
		});
		gotoSearchFieldItem.setAccelerator(KeyStroke.getKeyStroke("control K")); //$NON-NLS-1$
		jSearchMenu.add(gotoSearchFieldItem);
		menubar.add(jSearchMenu);
		// Download
		JMenu jDownloadMenu = new JMenu(getOffRoadString("offroad.download")); //$NON-NLS-1$
		JMenuItem lDownloadItem = new JMenuItem(getOffRoadString("offroad.string11")); //$NON-NLS-1$
		lDownloadItem.addActionListener(new DownloadAction(this));
		lDownloadItem.setAccelerator(KeyStroke.getKeyStroke("control G")); //$NON-NLS-1$
		jDownloadMenu.add(lDownloadItem);
		menubar.add(jDownloadMenu);
		// View
		JMenu jViewMenu = new JMenu(getOffRoadString("offroad.view")); //$NON-NLS-1$
		JMenuItem lViewItem = new JMenuItem(getOffRoadString("offroad.toggle_search")); //$NON-NLS-1$
		lViewItem.addActionListener(item-> toggleSearchBar());
		lViewItem.setAccelerator(KeyStroke.getKeyStroke("F11")); //$NON-NLS-1$
		jViewMenu.add(lViewItem);
		jViewMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.cursor_radius_1km", 1000d)));
		jViewMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.cursor_radius_2km", 2000d)));
		jViewMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.cursor_radius_5km", 5000d)));
		jViewMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.remove_cursor_radius", 0d)));
		JMenu jRendererMenu = new JMenu(getOffRoadString("offroad.renderer"));
		for (String renderer : getRendererRegistry().getRendererNames()) {
			lViewItem = new JMenuItem(new ChooseRendererAction(this, getOffRoadString("offroad.renderer_"+renderer.replaceAll("[^a-zA-Z]", "_")), null, renderer));
			jRendererMenu.add(lViewItem);
		}
		jViewMenu.add(jRendererMenu);
		menubar.add(jViewMenu);
		// Navigation
		JMenu jNavigationMenu = new JMenu(getOffRoadString("offroad.navigation")); //$NON-NLS-1$
		jNavigationMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.CAR)));
		jNavigationMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.BICYCLE)));
		jNavigationMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.PEDESTRIAN)));
		jNavigationMenu.add(new JMenuItem(new ClearRouteAction(this)));
		PointNavigationAction clearIntermediatePointsAction = new PointNavigationAction(this, "offroad.clear_intermediate_points",
				new HelperAction(){
			@Override
			public void act(TargetPointsHelper pHelper, LatLon pPosition) {
				while(!pHelper.getIntermediatePoints().isEmpty()){
					pHelper.removeWayPoint(false, 0);
				}
			}});
		PointNavigationAction clearAllPointsAction = new PointNavigationAction(this, "offroad.clear_all_points",
				(helper, pos) -> helper.removeAllWayPoints(false, false));
		jNavigationMenu.add(clearIntermediatePointsAction);
		jNavigationMenu.add(clearAllPointsAction);
		jNavigationMenu.add(new JSeparator());
		JMenu lRoutingServiceMenu = new JMenu(getOffRoadString("offroad.routing_service"));
		for (int i = 0; i < RouteService.values().length; i++) {
			RouteService service = RouteService.values()[i];
			if(service.isAvailable(this)){
				JMenuItem lRoutingServiceItem = new OffRoadMenuItem(new ChooseRouteServiceAction(this, service), lRoutingServiceMenu);
				lRoutingServiceMenu.add(lRoutingServiceItem);
			}
		}
		jNavigationMenu.add(lRoutingServiceMenu);
		jNavigationMenu.add(new JSeparator());
		addToMenu(jNavigationMenu, "offroad.up", item -> mDrawPanel.moveImageAnimated(0,-1f/3), "control UP");
		addToMenu(jNavigationMenu, "offroad.down", item -> mDrawPanel.moveImageAnimated(0,1f/3), "control DOWN");
		addToMenu(jNavigationMenu, "offroad.left", item -> mDrawPanel.moveImageAnimated(-1f/3,0), "control LEFT");
		addToMenu(jNavigationMenu, "offroad.right", item -> mDrawPanel.moveImageAnimated(1f/3,0), "control RIGHT");
		jNavigationMenu.add(new JSeparator());
		addToMenu(jNavigationMenu, "offroad.zoomin", item -> mAdapter.addWheelEvent(-1, mDrawPanel.copyCurrentTileBox()), "control PLUS");
		addToMenu(jNavigationMenu, "offroad.zoomout", item -> mAdapter.addWheelEvent(1, mDrawPanel.copyCurrentTileBox()), "control MINUS");
		jNavigationMenu.add(new JSeparator());
		addToMenu(jNavigationMenu, "offroad.back", new NavigationBackAction(this), "alt LEFT");
		addToMenu(jNavigationMenu, "offroad.forward", new NavigationForwardAction(this), "alt RIGHT");
		jNavigationMenu.add(new JSeparator());
		addToMenu(jNavigationMenu, "offroad.reset_north", new NavigationRotationAction(this).setAbsolute(0d), "alt HOME");
		addToMenu(jNavigationMenu, "offroad.increase_rotation", new NavigationRotationAction(this).setIncrement(30d), "alt PAGE_UP");
		addToMenu(jNavigationMenu, "offroad.decrease_rotation", new NavigationRotationAction(this).setIncrement(-30d), "alt PAGE_DOWN");
		menubar.add(jNavigationMenu);
		// PointOfInterest
		JMenu jPointOfInterestMenu = new JMenu(getOffRoadString("offroad.PointOfInterest")); //$NON-NLS-1$
		JMenuItem lPointOfInterestOffItem = new OffRoadMenuItem(new PoiFilterAction(this, null, true), jPointOfInterestMenu);
		jPointOfInterestMenu.add(lPointOfInterestOffItem);
		for (PoiUIFilter filter : mPoiFilters.getTopDefinedPoiFilters()) {
			JMenuItem lPointOfInterestItem = new OffRoadMenuItem(new PoiFilterAction(this, filter, false), jPointOfInterestMenu); 
			jPointOfInterestMenu.add(lPointOfInterestItem);
		}
		menubar.add(jPointOfInterestMenu);
		// Favorites
		JMenu jFavoritesMenu = new JMenu(getOffRoadString("offroad.Favorites")); //$NON-NLS-1$
		JMenuItem lAddFavourite = new JMenuItem(new AddFavoriteAction(this, getOffRoadString("offroad.addFavorite"), null, null));
		lAddFavourite.setAccelerator(KeyStroke.getKeyStroke("control D")); //$NON-NLS-1$
		jFavoritesMenu.add(lAddFavourite);
		for (FavoriteGroup	 fg : getFavorites().getFavoriteGroups()) {
			JMenu groupMenu = new JMenu(fg.name);
			for (FavouritePoint fp : fg.points) {
				JMenuItem lFavoritesItem = new JMenuItem(new ShowFavoriteAction(this, fp));
				groupMenu.add(lFavoritesItem);
			}
			jFavoritesMenu.add(groupMenu);
		}
		JMenuItem lSelectTrack = new JMenuItem(new SelectTrackAction(this, getOffRoadString("offroad.selectTrack")));
		lSelectTrack.setAccelerator(KeyStroke.getKeyStroke("control T")); //$NON-NLS-1$
		jFavoritesMenu.add(lSelectTrack);
		menubar.add(jFavoritesMenu);
		JMenu jHelpMenu = new JMenu(getOffRoadString("offroad.Help")); //$NON-NLS-1$
		addToMenu(jHelpMenu, "offroad.about", new AboutDialogAction(this), null);
		menubar.add(jHelpMenu);
		
		adaptMenuMnemonics(menubar.getComponents());
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new JMenuItem(new PointNavigationAction(this, "offroad.set_start_point",
				(helper, pos) -> helper.setStartPoint(pos, false, null))));
		popupMenu.add(new JMenuItem(new PointNavigationAction(this, "offroad.set_intermediate_point",
				(helper, pos) -> helper.navigateToPoint(pos, false, helper.getIntermediatePoints().size()))));
		popupMenu.add(new JMenuItem(new PointNavigationAction(this, "offroad.set_destination_point",
				(helper, pos) -> helper.navigateToPoint(pos, false, -1))));
		popupMenu.add(new JSeparator());
		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.CAR)));
		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.BICYCLE)));
		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.PEDESTRIAN)));
		popupMenu.add(new JSeparator());
		popupMenu.add(new JMenuItem(clearIntermediatePointsAction));
		popupMenu.add(new JMenuItem(clearAllPointsAction));
		popupMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.set_cursor_radius", -1d)));
		popupMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.remove_cursor_radius", 0d)));
		mDrawPanel.setComponentPopupMenu(popupMenu);
		popupMenu.addPopupMenuListener(new PoiContextMenuListener(popupMenu));
		mFrame.setJMenuBar(menubar);
		mDrawPanel.init();
		mFrame.pack();
		OsmWindowLocationStorage storage = (OsmWindowLocationStorage) ComponentLocationStorage.decorateDialog(this, mFrame, getClass().getName());
		if (storage!= null) {
			mFrame.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent pE) {
					int splitPanePosition = storage.getSplitLocation();
					int lastSplitPanePosition = storage.getSplitLocation();
					if (mSplitPane != null && splitPanePosition != -1 && lastSplitPanePosition != -1) {
						mSplitPane.setDividerLocation(splitPanePosition);
						mSplitPane.setLastDividerLocation(lastSplitPanePosition);
					}
					mFrame.removeComponentListener(this);
				}
			});
		}
		mFrame.setVisible(true);
	}

	private void adaptMenuMnemonics(Component[] components) {
		for (int i = 0; i < components.length; i++) {
			Component comp = components[i];
			if (comp instanceof AbstractButton) {
				AbstractButton but = (AbstractButton) comp;
				setMnemonic(but);
			}
			if (comp instanceof JMenu) {
				JMenu cont = (JMenu) comp;
				adaptMenuMnemonics(cont.getPopupMenu().getComponents());
			}
		}
	}

	public static boolean isMacOsX() {
		boolean underMac = false;
		String osName = System.getProperty("os.name");
		if (osName.startsWith("Mac OS")) {
			underMac = true;
		}
		return underMac;
	}
	
	private void setMnemonic(AbstractButton item){
		String rawLabel = item.getText();
		item.setText(rawLabel.replaceFirst("&([^ ])", "$1"));
		int mnemoSignIndex = rawLabel.indexOf("&");
		if (mnemoSignIndex >= 0 && mnemoSignIndex + 1 < rawLabel.length()) {
			char charAfterMnemoSign = rawLabel.charAt(mnemoSignIndex + 1);
			if (charAfterMnemoSign != ' ') {
				// no mnemonics under Mac OS:
				if (!isMacOsX()) {
					item.setMnemonic(charAfterMnemoSign);
					// sets the underline to exactly this character.
					item.setDisplayedMnemonicIndex(mnemoSignIndex);
				}
			}
		}
	}
	
	void addToMenu(JMenu jNavigationMenu, String name, ActionListener action, String keyStroke) {
		JMenuItem navigationBackItem = new JMenuItem(getOffRoadString(name)); //$NON-NLS-1$
		navigationBackItem.addActionListener(action);
		if (keyStroke != null) {
			navigationBackItem.setAccelerator(KeyStroke.getKeyStroke(keyStroke)); //$NON-NLS-1$
		}
		jNavigationMenu.add(navigationBackItem);
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
		log.error("TRANSLATE ME: " + pString);
		return "TRANSLATE_ME:" + pString; //$NON-NLS-1$
	}

	public OsmBitmapPanel getDrawPanel() {
		return mDrawPanel;
	}

	public static void main(String[] args) throws XmlPullParserException, IOException {
		final OsmWindow win = OsmWindow.getInstance();
		VersionInfo version = win.getVersion();
		log.info("Version: " + version.version + ", hash=" + version.hash);
		win.init();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				win.createAndShowUI();
			}
		});

	}

	protected void saveSettings() {
		RotatedTileBox tileBox = mDrawPanel.copyCurrentTileBox();
		prefs.setLastKnownMapLocation(tileBox.getLatitude(), tileBox.getLongitude());
		prefs.setLastKnownMapZoom(tileBox.getZoom());
		getSettings().SELECTED_POI_FILTER_STRING_FOR_MAP.set(mSearchTextField.getText());
		OsmWindowLocationStorage storage = new OsmWindowLocationStorage();
		storage.setSplitLocation(mSplitPane.getDividerLocation());
		ComponentLocationStorage.storeDialogPositions(this, mFrame, storage, getClass().getName());
		settings.save();
	}

	public OsmWindow() {
		initStrings();
		setupProxy();
		prefs.APPLICATION_MODE.set(ApplicationMode.DEFAULT);
		prefs.MAP_PREFERRED_LOCALE.set(getLanguage());
		prefs.PREFERRED_LOCALE.set(getLanguage());
		mStrings = new R.string();
	}

	private void init() throws XmlPullParserException, IOException {
		Dimension size = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		widthPixels = size.width;
		heightPixels = size.height;
		density = java.awt.Toolkit.getDefaultToolkit().getScreenResolution()/96f;
		mOsmAndLocationProvider = new OsmAndLocationProvider(this);
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
		mRendererRegistry.initRenderers(IProgress.EMPTY_PROGRESS);
		mRoutingHelper = new RoutingHelper(this);
		mGeocodingLookupService = new GeocodingLookupService(this);
		mTargetPointsHelper = new TargetPointsHelper(this);
		mPoiFilters = new PoiFiltersHelper(this);
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
		mMapMarkersHelper = new MapMarkersHelper(this);
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

	private void setupProxy() {
		// proxy settings
		Properties props = (Properties) settings.getPreferenceObject(OFFROAD_PROPERTIES);
		if("true".equals(settings.getString(props, PROXY_USE_SETTINGS, ""))) {
			if ("true".equals(settings.getString(props, PROXY_IS_AUTHENTICATED, ""))) {
				try {
					Authenticator.setDefault(new ProxyAuthenticator(settings.getString(props, PROXY_USER, ""), new String(Base64.decode(settings.getString(props, PROXY_PASSWORD, "")))));
				} catch (Base64DecoderException e) {
					e.printStackTrace();
				}
			}
			System.setProperty("http.proxyHost", settings.getString(props, PROXY_HOST, ""));
			System.setProperty("http.proxyPort", settings.getString(props, PROXY_PORT, ""));
			System.setProperty("https.proxyHost", settings.getString(props, PROXY_HOST, ""));
			System.setProperty("https.proxyPort", settings.getString(props, PROXY_PORT, ""));
			System.setProperty("http.nonProxyHosts", settings.getString(props, PROXY_EXCEPTION, ""));
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

	public static class VersionInfo {
		String version;
		String hash;
		public VersionInfo(String pVersion, String pHash) {
			super();
			version = pVersion;
			hash = pHash;
		}
		
	}
	public VersionInfo getVersion(){
		if(mVersionInfo == null){
			try {
				InputStream is = getResource("version.properties"); //$NON-NLS-1$
				if (is == null) {
					return null;
				}
				PropertyResourceBundle bundle = new PropertyResourceBundle(is);
				is.close();
				mVersionInfo = new VersionInfo(bundle.getString("version"), bundle.getString("hash"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mVersionInfo;
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
	
	public BufferedImage getBitmap(Amenity o) {
		String id = null;
		PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
		if (st != null) {
			if (RenderingIcons.containsSmallIcon(st.getIconKeyName())) {
				id = st.getIconKeyName();
			} else if (RenderingIcons.containsSmallIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
				id = st.getOsmTag() + "_" + st.getOsmValue();
			}
		}
		BufferedImage bmp = null;
		if (id != null) {
			bmp = RenderingIcons.getIcon(id, false);
		}
		return bmp;
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
		if (sInstance == null) {
			sInstance = new OsmWindow();
		}
		return sInstance;
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
		RotatedTileBox tileBox = mDrawPanel.copyCurrentTileBox();
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
		setCursorPosition(pLocation);
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
			LatLon mousePosition = mDrawPanel.copyCurrentTileBox().getLatLonFromPixel(e.getX(), e.getY());
			double distance = MapUtils.getDistance(mousePosition, cursorPosition)/1000d;
			Object[] messageArguments = { new Double(distance),
					new Double(cursorPosition.getLatitude()),
					new Double(cursorPosition.getLongitude()) };
			MessageFormat formatter = new MessageFormat(
					getOffRoadString("offroad.string47")); //$NON-NLS-1$
			String message = formatter.format(messageArguments);
//			mStatusLabel.setBackground(Color.GRAY);
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

	public MapMarkersHelper getMapMarkersHelper() {
		return mMapMarkersHelper;
	}
	
	public RoutingHelper getRoutingHelper() {
		return mRoutingHelper;
	}

	public GeocodingLookupService getGeocodingLookupService() {
		return mGeocodingLookupService;
	}

	public OsmAndLocationProvider getLocationProvider() {
		return mOsmAndLocationProvider;
	}

	public MapPoiTypes getPoiTypes() {
		return mMapPoiTypes;
	}

	public MouseEvent getLastMouseEvent() {
		return mAdapter.getMouseEvent();
	}
	
	public RenderingRulesStorage getRenderingRulesStorage() {
		RenderingRulesStorage storage = mRendererRegistry.getCurrentSelectedRenderer();
//		System.out.println("\n\n--------- TEXT ----- ");
//		storage.printDebug(storage.TEXT_RULES, System.out);
		return storage;
	}

	public void showToastMessage(String pMsg) {
//		mStatusLabel.setBackground(Color.red);
		mStatusLabel.setText(pMsg);
//		mStatusLabel.repaint();
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
		return getDrawPanel().copyCurrentTileBox().getZoom();
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
	
	public interface CursorPositionListener {
		void cursorPositionChanged(LatLon pPosition);
	}

	
	public void addCursorPositionListener(CursorPositionListener pListener){
		mCursorPositionListeners.add(pListener);
	}
	public void removeCursorPositionListener(CursorPositionListener pListener){
		mCursorPositionListeners.remove(pListener);
	}
			
	
	public void setCursorPosition(Point pPoint) {
		mDrawPanel.setCursor(pPoint);
		setCursorPosition(mDrawPanel.getCursorPosition());
	}

	public void setCursorPosition(LatLon pLoc) {
		mDrawPanel.setCursor(pLoc);
		addPoint(pLoc);
		for (CursorPositionListener listener : mCursorPositionListeners) {
			listener.cursorPositionChanged(pLoc);
		}
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
		Locale locale = Locale.getDefault();
		return locale.getLanguage().toLowerCase();
	}

	public String getCountry() {
		Locale locale = Locale.getDefault();
		String ct = locale.getCountry().toLowerCase();
		return ct;
	}

	public PoiFiltersHelper getPoiFilters() {
		return mPoiFilters;
	}

	public void showWikipedia(String pContent, String pTitle, String pArticle) {
		ShowWikipediaAction action = new ShowWikipediaAction(this, pContent, pTitle, pArticle);
		action.actionPerformed(null);
	}

	public LatLon getCursorPosition() {
		LatLon cursorPosition = mDrawPanel.getCursorPosition();
		if(cursorPosition == null){
			cursorPosition = getCenterPosition();
			setCursorPosition(cursorPosition);
		}
		return cursorPosition;
	}
	
	public LatLon getMouseLocation() {
		MouseEvent lastMouseEvent = getLastMouseEvent();
		Point destination;
		if(lastMouseEvent == null){
			destination = new Point(0,0);
		} else {
			destination = lastMouseEvent.getPoint();
		}
		LatLon destLatLon = mDrawPanel.copyCurrentTileBox().getLatLonFromPixel(destination.x, destination.y);
		return destLatLon;
	}

	public PoiUIFilter getCurrentPoiUIFilter() {
		return mCurrentPoiFilter;
	}

	public LatLon getCenterPosition() {
		return mDrawPanel.copyCurrentTileBox().getCenterLatLon();
	}

	/**
	 * @param filter == null: means, clear filter and table
	 * @param pFilterText 
	 */
	public void setPoiFilter(PoiUIFilter filter, String pFilterText) {
		String filterId = null;
		if (filter != null) {
			filterId = filter.getFilterId();
		}
		getSettings().SELECTED_POI_FILTER_FOR_MAP.set(filterId);
		getSettings().SELECTED_POI_FILTER_STRING_FOR_MAP.set(pFilterText);
		refreshSearchTable();
		getDrawPanel().refreshMap();
	}

	public void refreshSearchTable() {
		setWaitingCursor(true);
		List<Amenity> result = new Vector<>();
		String filterId = getSettings().SELECTED_POI_FILTER_FOR_MAP.get();
		if (filterId != null) {
			PoiUIFilter filter = getPoiFilters().getFilterById(filterId);
			String filterString = getSettings().SELECTED_POI_FILTER_STRING_FOR_MAP.get();
			filter.setFilterByName(filterString);
			LatLon latLon = getCursorPosition();
			result = filter.initializeNewSearch(latLon.getLatitude(), latLon.getLongitude(), -1,
					new ResultMatcher<Amenity>() {

						@Override
						public boolean publish(Amenity pObject) {
							log.debug("Adding " + pObject.getName(getLanguage()));
							return true;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					});
		}
		mAmenityTable.setSearchResult(result);
		setWaitingCursor(false);
	}

	public String getOffRoadString(String pString, Object[] pObjects) {
		MessageFormat formatter = new MessageFormat(
				getOffRoadString(pString)); //$NON-NLS-1$
		return formatter.format(pObjects);
	}

	public JTextField getSearchTextField() {
		return mSearchTextField;
	}

	void toggleSearchBar() {
		if(mSearchBarVisible){
			mFrame.getContentPane().remove(mToolBar);
//			mFrame.getContentPane().remove(mAmenityTable);
			mSplitPane.setDividerLocation(0);
		} else {
			mFrame.getContentPane().add(mToolBar, BorderLayout.NORTH);
//			mFrame.getContentPane().add(mAmenityTable, BorderLayout.WEST);
		}
		mSearchBarVisible = ! mSearchBarVisible;
		mFrame.revalidate();
		mDrawPanel.requestFocus();
	}

	public SQLiteAPI getSQLiteAPI() {
		if(mSqLiteImpl == null){
			mSqLiteImpl = new SQLiteImpl(this);
		}
		return mSqLiteImpl;
	}

	public File getDatabasePath(String pFavouriteDbName) {
		return getAppPath(pFavouriteDbName);
	}

	public File getFileStreamPath(String pFileToBackup) {
		return getAppPath(pFileToBackup);
	}

	public FavouritesDbHelper getFavorites() {
		if(mFavorites == null){
			mFavorites = new FavouritesDbHelper(this);
			mFavorites.loadFavorites();
		}
		return mFavorites;
	}

	public Properties getOffroadProperties() {
		return mOffroadProperties;
	}

	public OffRoadResources getResources() {
		if(mOffRoadResources == null){
			mOffRoadResources = new OffRoadResources(this);
		}
		return mOffRoadResources;
	}
	
	

	public GpxSelectionHelper getSelectedGpxHelper() {
		if(mGpxSelectionHelper==null){
			mGpxSelectionHelper = new GpxSelectionHelper(this, getSavingTrackHelper());
			mGpxSelectionHelper.loadGPXTracks(IProgress.EMPTY_PROGRESS);
		}
		return mGpxSelectionHelper;
	}

	protected SavingTrackHelper getSavingTrackHelper() {
		if (mSavingTrackHelper==null) {
			mSavingTrackHelper = new SavingTrackHelper(this);
		}
		return mSavingTrackHelper;
	}

	private void closeWindow() {
		// save properties:
		saveSettings();
		mFrame.dispose();
		System.exit(0);
	}
	
}
