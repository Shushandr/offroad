package net.sourceforge.offroad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.ValueHolder;
import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.inapp.util.Base64;
import net.osmand.plus.inapp.util.Base64DecoderException;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.OsmandRenderer.RenderingResult;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.DrawPolylineLayer;
import net.osmand.plus.views.DrawPolylineLayer.Polyline;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.actions.AboutDialogAction;
import net.sourceforge.offroad.actions.AddFavoriteAction;
import net.sourceforge.offroad.actions.ChangeIconSizeAction;
import net.sourceforge.offroad.actions.ChooseApplicationModeAction;
import net.sourceforge.offroad.actions.ChooseMetricSystemAction;
import net.sourceforge.offroad.actions.ChooseRendererAction;
import net.sourceforge.offroad.actions.ChooseRouteServiceAction;
import net.sourceforge.offroad.actions.ClearRouteAction;
import net.sourceforge.offroad.actions.CopyLocationToClipboardAction;
import net.sourceforge.offroad.actions.DeleteFavoriteAction;
import net.sourceforge.offroad.actions.DirectSearchAction;
import net.sourceforge.offroad.actions.DownloadAction;
import net.sourceforge.offroad.actions.ExportRouteAction;
import net.sourceforge.offroad.actions.ExportTracksAction;
import net.sourceforge.offroad.actions.GpxImportAction;
import net.sourceforge.offroad.actions.InsertPointIntoPolylineAction;
import net.sourceforge.offroad.actions.NavigationBackAction;
import net.sourceforge.offroad.actions.NavigationForwardAction;
import net.sourceforge.offroad.actions.NavigationRotationAction;
import net.sourceforge.offroad.actions.OffRoadAction.OffRoadMenuItem;
import net.sourceforge.offroad.actions.PoiFilterAction;
import net.sourceforge.offroad.actions.PointNavigationAction;
import net.sourceforge.offroad.actions.RemovePointFromPolylineAction;
import net.sourceforge.offroad.actions.RemovePolylineAction;
import net.sourceforge.offroad.actions.RouteAction;
import net.sourceforge.offroad.actions.SearchAddressAction;
import net.sourceforge.offroad.actions.SelectTrackAction;
import net.sourceforge.offroad.actions.SetCursorRadiusAction;
import net.sourceforge.offroad.actions.SetRenderingRule;
import net.sourceforge.offroad.actions.ShowFavoriteAction;
import net.sourceforge.offroad.actions.ShowPolylineDetailsAction;
import net.sourceforge.offroad.actions.ShowRouteDetailsAction;
import net.sourceforge.offroad.actions.ShowTargetPointAction;
import net.sourceforge.offroad.actions.ShowTrackDetailsAction;
import net.sourceforge.offroad.actions.ShowWikipediaAction;
import net.sourceforge.offroad.data.LocationAsMapObject;
import net.sourceforge.offroad.data.QuadRectExtendable;
import net.sourceforge.offroad.data.SQLiteImpl;
import net.sourceforge.offroad.data.persistence.ComponentLocationStorage;
import net.sourceforge.offroad.data.persistence.OsmWindowLocationStorage;
import net.sourceforge.offroad.res.OffRoadResources;
import net.sourceforge.offroad.res.Resources;
import net.sourceforge.offroad.ui.AmenityTablePanel;
import net.sourceforge.offroad.ui.AmenityTableUpdateThread;
import net.sourceforge.offroad.ui.BlindIcon;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OffRoadPopupMenuListener;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.OsmBitmapPanel.IntermediateImageListener;
import net.sourceforge.offroad.ui.OsmBitmapPanelMouseAdapter;
import net.sourceforge.offroad.ui.PoiFilterRenderer;

/**
 * OffRoad
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow  implements IRouteInformationListener {
	public static class MapPointStorage {

		private final LatLon mPoint;
		private final int mZoom;

		public MapPointStorage(LatLon pPoint, int pZoom) {
			mPoint = pPoint;
			mZoom = pZoom;
		}

	}

	private final static Log log = PlatformUtil.getLog(OsmWindow.class);

	
	public static final int MAX_ZOOM = 22;
	public static final String RENDERING_STYLES_DIR = "rendering_styles/"; //$NON-NLS-1$
	private static final String OSMAND_ICONS_DIR = RENDERING_STYLES_DIR + "style-icons/drawable-"; //$NON-NLS-1$
	public static final String IMAGE_PATH = "drawable-"; //$NON-NLS-1$
	public static final String PROXY_PORT = "proxy.port";
	public static final String PROXY_HOST = "proxy.host";
	public static final String PROXY_PASSWORD = "proxy.password";
	public static final String PROXY_USER = "proxy.user";
	public static final String PROXY_IS_AUTHENTICATED = "proxy.is_authenticated";
	public static final String PROXY_USE_SETTINGS = "proxy.use_settings";
	public static final String PROXY_EXCEPTION = "proxy.exception";
	private static final String OFFROAD_PROPERTIES = "offroad";
	public static final int MIN_ZOOM = 5;
	private static final String MODE_WORLD_WRITEABLE = "mode_world_writeable";
	private static final String VECTOR_INDEXES_CHECK = "vector_indexes_check";
	public static final String OSMAND_ICONS_DIR_PREFIX = "osmand_icons_dir_prefix";
	public static final String OSMAND_ICONS_DIR_DEFAULT_PREFIX = "hdpi";
	private static final String[] sOsmandIconsPrefixes = new String[]{"mdpi", OSMAND_ICONS_DIR_DEFAULT_PREFIX, "xhdpi", "xxhdpi"};

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
	
	private enum SearchType {
		AMENITY, ROUTE
	}

	private SearchType mSearchType = SearchType.AMENITY;


	private List<MapObject> mRouteResult = new Vector<>();


	private JMenuItem mAddFavourite;


	private JMenuItem mSelectTrack;


	private HashMap<String, BufferedImage> mBufferedImageCache = new HashMap<>();


	private JTextField mDirectSearchTextField;


	public DirectSearchAction mDirectSearchAction;


	private JPanel mDirectSearchPanel;


	private JCheckBox mDirectSearchFuzzy;


	private JButton mDirectSearchBackward;


	private JButton mDirectSearchForward;


	private JButton mDirectSearchClose;


	private boolean mDirectSearchVisible;


	private JLabel mQueueStatus; 
	
	private static void addFavoriteGroups(OsmWindow context, JMenu parent, List<FavoriteGroup> fgs) {
		HashMap<String, JMenu> groupMenus = new HashMap<>();
		for (FavoriteGroup fg : fgs) {
			JMenu groupMenu = groupMenus.getOrDefault(fg.name, null);
			if (groupMenu == null) {
				String[] levels = fg.name.split("/");
				JMenu prev = parent;
				String path = null;
				for (String currName : levels) {
					path = path == null ? currName : path + "/" + currName;
					JMenu curr = groupMenus.getOrDefault(path, null);
					if (curr == null) {
						curr = new JMenu(currName);
						prev.add(curr);
						groupMenus.put(path, curr);
					}
					prev = curr;
				}
				groupMenu = prev;
			}
			for (FavouritePoint fp : fg.points) {
				JMenuItem lFavoritesItem = new JMenuItem(new ShowFavoriteAction(context, fp));
				groupMenu.add(lFavoritesItem);
			}
		}
	}

	public void createAndShowUI() {
		mDirectSearchTextField = new JTextField(getOffRoadString("offroad.DirectSearchText"));
		mDirectSearchFuzzy = new JCheckBox(getOffRoadString("offroad.fuzzy_search"));
		mDirectSearchFuzzy.setFocusable(false);
		mDirectSearchClose = new JButton(new ImageIcon(readImageInternally("button_cancel.png")));
		mDirectSearchBackward = new JButton(new ImageIcon(readImageInternally("up.png")));
		mDirectSearchForward = new JButton(new ImageIcon(readImageInternally("down.png")));
		mDirectSearchAction = new DirectSearchAction(this, mDirectSearchTextField, mDirectSearchFuzzy,
				mDirectSearchBackward, mDirectSearchForward, mDirectSearchClose);
		mDirectSearchClose.addActionListener(pE -> {
			mStatusBar.remove(mDirectSearchPanel);
			mDirectSearchVisible = false;
			mFrame.revalidate();
		});
		mDrawPanel = new OsmBitmapPanel(this);
		mAdapter = new OsmBitmapPanelMouseAdapter(mDrawPanel);
		mDrawPanel.addMouseListener(mAdapter);
		mDrawPanel.addMouseMotionListener(mAdapter);
		mDrawPanel.addMouseWheelListener(mAdapter);
		
		mStatusLabel = new JLabel("!"); //$NON-NLS-1$
		mStatusLabel.setPreferredSize(mStatusLabel.getPreferredSize());
		mQueueStatus = new JLabel("!"); //$NON-NLS-1$
		mQueueStatus.setPreferredSize(mQueueStatus.getPreferredSize());
		mRouteProgressBar = new JProgressBar();
		mRouteProgressBar.setMaximum(100);
		mRouteProgressBar.setStringPainted(true);
		mRouteProgressBar.setVisible(false);
		mRouteProgressStatus = new JLabel("!");
		mDirectSearchPanel = new JPanel();
		mDirectSearchPanel.setLayout(new GridBagLayout());
		int x = 0;
		mDirectSearchPanel.add(mDirectSearchClose, new GridBagConstraints(x++, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		mDirectSearchPanel.add(mDirectSearchTextField, new GridBagConstraints(x++, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mDirectSearchPanel.add(mDirectSearchBackward, new GridBagConstraints(x++, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		mDirectSearchPanel.add(mDirectSearchForward, new GridBagConstraints(x++, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		mDirectSearchPanel.add(mDirectSearchFuzzy, new GridBagConstraints(x++, 0, 1, 1, 0, 1, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		adaptMenuMnemonics(mDirectSearchPanel.getComponents());
		mStatusBar=new JPanel();
		mStatusBar.setLayout(new GridBagLayout());
		x = 0;
		mStatusBar.add(mStatusLabel, new GridBagConstraints(x++, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
//		mStatusBar.add(mDirectSearchPanel, new GridBagConstraints(x++, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mDirectSearchVisible = false;
		x++;
		mStatusBar.add(mQueueStatus, new GridBagConstraints(x++, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mStatusBar.add(mRouteProgressStatus, new GridBagConstraints(x++, 0, 1, 1, 0, 1, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		mStatusBar.add(mRouteProgressBar, new GridBagConstraints(x++, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
		

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
		getSearchTextField().addActionListener(pE -> setPoiFilter(getCurrentPoiUIFilter(), getSearchTextField().getText()));
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
					break;
				case KeyEvent.VK_DOWN:
					if(mComboBox.getSelectedIndex()<mComboBox.getItemCount()-1){
						mComboBox.setSelectedIndex(mComboBox.getSelectedIndex()+1);
					}
					pE.consume();
					break;
				}
		}
		});
		mToolBar.add(getSearchTextField(), new GridBagConstraints(1, 0, 1, 1, 3, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10, 0, 10, 0), 0, 0));
		mComboBox = new JComboBox<>();
		mComboBoxModel = new DefaultComboBoxModel<>();
		mCurrentPoiFilter = mPoiFilters.getSearchByNamePOIFilter();
		mComboBoxModel.addElement(mPoiFilters.getSearchByNamePOIFilter());
		mComboBoxModel.addElement(mPoiFilters.getNominatimAddressFilter());
		mComboBoxModel.addElement(mPoiFilters.getNominatimPOIFilter());
		for (PoiUIFilter filter : mPoiFilters.getTopDefinedPoiFilters()) {
			mComboBoxModel.addElement(filter);
		}
		mComboBox.setModel(mComboBoxModel);
		mComboBox.setFocusable(false);
		mComboBox.addActionListener(pE -> {
			if(mComboBox.getSelectedIndex() >= 0){
				mCurrentPoiFilter = mComboBoxModel.getElementAt(mComboBox.getSelectedIndex());
			}
		});
		mComboBox.setRenderer(new PoiFilterRenderer<>());
		mToolBar.add(mComboBox, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		mFrame = new JFrame(getOffRoadString("offroad.string4")); //$NON-NLS-1$
		mFrame.setIconImage(readImageInternally("offroad_icon.png"));
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
		saveItem.addActionListener(pE -> {
			JFileChooser chooser = new JFileChooser();
			int showSaveDialog = chooser.showSaveDialog(mFrame);
			if(showSaveDialog == JFileChooser.APPROVE_OPTION){
				// TODO: Ask for overwrite.
				mDrawPanel.saveImage(chooser.getSelectedFile());
			}
		});
		jFileMenu.add(saveItem);
		JMenuItem importGpxItem = new JMenuItem(getOffRoadString("offroad.import_gpx")); //$NON-NLS-1$
		importGpxItem.addActionListener(new GpxImportAction(this));
		jFileMenu.add(importGpxItem);
		addToMenu(jFileMenu, "offroad.export_route", new ExportRouteAction(this), null);
		addToMenu(jFileMenu, "offroad.export_tracks", new ExportTracksAction(this), null);
		addToMenu(jFileMenu, "offroad.copy_location", new CopyLocationToClipboardAction(this), "alt C");
		addToMenu(jFileMenu, "offroad.exit", item -> closeWindow(), "control Q");
		menubar.add(jFileMenu);
		JMenu jSearchMenu = new JMenu(getOffRoadString("offroad.string7")); //$NON-NLS-1$
		JMenuItem findItem = new JMenuItem(getOffRoadString("offroad.string8")); //$NON-NLS-1$
		findItem.addActionListener(new SearchAddressAction(this));
		findItem.setAccelerator(KeyStroke.getKeyStroke("control F")); //$NON-NLS-1$
		jSearchMenu.add(findItem);
		
		JMenuItem gotoSearchFieldItem = new JMenuItem(getOffRoadString("offroad.gotoSearchField")); //$NON-NLS-1$
		gotoSearchFieldItem.addActionListener(pE -> {
			getSearchTextField().selectAll();
			getSearchTextField().requestFocus();
		});
		gotoSearchFieldItem.setAccelerator(KeyStroke.getKeyStroke("control K")); //$NON-NLS-1$
		jSearchMenu.add(gotoSearchFieldItem);
		addToMenu(jSearchMenu, null, mDirectSearchAction, "control shift F");
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
		jViewMenu.add(new JSeparator());
		JMenu jMetricSystemMenu = new JMenu(getString("unit_of_length"));
                jMetricSystemMenu.add(new OffRoadMenuItem(new ChooseMetricSystemAction(this, MetricsConstants.KILOMETERS_AND_METERS), jMetricSystemMenu));
                jMetricSystemMenu.add(new OffRoadMenuItem(new ChooseMetricSystemAction(this, MetricsConstants.MILES_AND_FOOTS), jMetricSystemMenu));
                jMetricSystemMenu.add(new OffRoadMenuItem(new ChooseMetricSystemAction(this, MetricsConstants.NAUTICAL_MILES), jMetricSystemMenu));
                jMetricSystemMenu.add(new OffRoadMenuItem(new ChooseMetricSystemAction(this, MetricsConstants.MILES_AND_YARDS), jMetricSystemMenu));
                jViewMenu.add(jMetricSystemMenu);
		JMenu jApplicationModeMenu = new JMenu(getString("app_modes_choose"));
		jApplicationModeMenu.add(new OffRoadMenuItem(new ChooseApplicationModeAction(this, ApplicationMode.DEFAULT), jApplicationModeMenu));
		jApplicationModeMenu.add(new OffRoadMenuItem(new ChooseApplicationModeAction(this, ApplicationMode.CAR), jApplicationModeMenu));
		jApplicationModeMenu.add(new OffRoadMenuItem(new ChooseApplicationModeAction(this, ApplicationMode.BICYCLE), jApplicationModeMenu));
		jApplicationModeMenu.add(new OffRoadMenuItem(new ChooseApplicationModeAction(this, ApplicationMode.PEDESTRIAN), jApplicationModeMenu));
		jViewMenu.add(jApplicationModeMenu);
		JMenu jRendererMenu = new JMenu(getOffRoadString("offroad.renderer"));
		for (String renderer : getRendererRegistry().getRendererNames()) {
			lViewItem = new OffRoadMenuItem(new ChooseRendererAction(this, getOffRoadString("offroad.renderer_"+renderer.replaceAll("[^a-zA-Z]", "_")), null, renderer), jRendererMenu);
			jRendererMenu.add(lViewItem);
		}
		jViewMenu.add(jRendererMenu);
		// rendering properties
		JMenu jRenderPropertiesMenu = new JMenu(getString("map_widget_renderer"));
		
		HashMap<String, JMenu> categoryMenus = new HashMap<>();
		for (RenderingRuleProperty customProp : getRenderingRulesStorage().PROPS.getCustomRules()) {
			if(customProp.getCategory()==null){
				continue;
			}
			if (!customProp.isBoolean() && !customProp.isString()) {
				continue;
			}
			if(!categoryMenus.containsKey(customProp.getCategory())){
				JMenu jMenu = new JMenu(getString("rendering_category_" + customProp.getCategory()));
				jRenderPropertiesMenu.add(jMenu);
				categoryMenus.put(customProp.getCategory(), jMenu);
			}
			JMenu jMenu = categoryMenus.get(customProp.getCategory());
			if (customProp.isBoolean()) {
				CommonPreference<Boolean> pref = prefs.getCustomRenderBooleanProperty(customProp.getAttrName());
				log.debug("PROP: "  + customProp.getAttrName()+ ", " + customProp.getCategory() + "=" + pref.get());
				JMenuItem item = new OffRoadMenuItem(new SetRenderingRule(this, customProp), jMenu);
				jMenu.add(item);
			} else {
				JMenu submenu = new JMenu(getString("rendering_attr_" + customProp.getAttrName() + "_name"));
				String defaultValue = customProp.getDefaultValueDescription();
				if (defaultValue != null) {
					// Add default item
					JMenuItem item = new OffRoadMenuItem(new SetRenderingRule(this, customProp, customProp.getDefaultValueDescription(), true), submenu);
					submenu.add(item);
				}
				for (String val: customProp.getPossibleValues())
				{
					JMenuItem item = new OffRoadMenuItem(new SetRenderingRule(this, customProp, val, false), submenu);
					submenu.add(item);
				}
				jMenu.add(submenu);
			}
		}
		jViewMenu.add(jRenderPropertiesMenu);
		JMenu jIconSizePropertiesMenu = new JMenu(getOffRoadString("offroad.icons_size_menu"));
		for (String prefix : sOsmandIconsPrefixes) {
			JMenuItem item = new OffRoadMenuItem(new ChangeIconSizeAction(this, prefix), jIconSizePropertiesMenu);
			jIconSizePropertiesMenu.add(item);
		}
		jViewMenu.add(jIconSizePropertiesMenu);
		menubar.add(jViewMenu);
		// Navigation
		JMenu jNavigationMenu = new JMenu(getOffRoadString("offroad.navigation")); //$NON-NLS-1$
		addToMenu(jNavigationMenu, null, new RouteAction(this, ApplicationMode.CAR), "F1");
		addToMenu(jNavigationMenu, null, new RouteAction(this, ApplicationMode.BICYCLE), "F2");
		addToMenu(jNavigationMenu, null, new RouteAction(this, ApplicationMode.PEDESTRIAN), "F3");
		jNavigationMenu.add(new JSeparator());
		addToMenu(jNavigationMenu, "offroad.go_source", new ShowTargetPointAction(this, TargetPointsHelper::getPointToStart), "control HOME");
		addToMenu(jNavigationMenu, "offroad.go_dest", new ShowTargetPointAction(this, TargetPointsHelper::getPointToNavigate), "control END");
		jNavigationMenu.add(new JSeparator());
		jNavigationMenu.add(new JMenuItem(new ClearRouteAction(this)));
		PointNavigationAction clearIntermediatePointsAction = new PointNavigationAction(this, "offroad.clear_intermediate_points",
				(pHelper, pPosition) -> {
					while(!pHelper.getIntermediatePoints().isEmpty()){
						pHelper.removeWayPoint(false, 0);
					}
				});
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
		addToMenu(jNavigationMenu, "offroad.up", item -> mDrawPanel.moveImageAnimatedInPercentage(0,-1f/3), "control UP");
		addToMenu(jNavigationMenu, "offroad.down", item -> mDrawPanel.moveImageAnimatedInPercentage(0,1f/3), "control DOWN");
		addToMenu(jNavigationMenu, "offroad.left", item -> mDrawPanel.moveImageAnimatedInPercentage(-1f/3,0), "control LEFT");
		addToMenu(jNavigationMenu, "offroad.right", item -> mDrawPanel.moveImageAnimatedInPercentage(1f/3,0), "control RIGHT");
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
		mAddFavourite = new JMenuItem(new AddFavoriteAction(this, getOffRoadString("offroad.addFavorite"), null, null));
		mAddFavourite.setAccelerator(KeyStroke.getKeyStroke("control D")); //$NON-NLS-1$
		mSelectTrack = new JMenuItem(new SelectTrackAction(this, getOffRoadString("offroad.selectTrack")));
		mSelectTrack.setAccelerator(KeyStroke.getKeyStroke("control T")); //$NON-NLS-1$
		jFavoritesMenu.addMenuListener(new MenuListener(){

			@Override
			public void menuSelected(MenuEvent pE) {
				// Must be dynamic, as the favorites may change...
				jFavoritesMenu.removeAll();
				jFavoritesMenu.add(mAddFavourite);
				addFavoriteGroups(OsmWindow.this, jFavoritesMenu, getFavorites().getFavoriteGroups());
				jFavoritesMenu.add(mSelectTrack);
			}

			@Override
			public void menuDeselected(MenuEvent pE) {
			}

			@Override
			public void menuCanceled(MenuEvent pE) {
			}});
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
//		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.CAR)));
//		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.BICYCLE)));
//		popupMenu.add(new JMenuItem(new RouteAction(this, ApplicationMode.PEDESTRIAN)));
//		popupMenu.add(new JSeparator());
		popupMenu.add(new JMenuItem(clearIntermediatePointsAction));
		popupMenu.add(new JMenuItem(clearAllPointsAction));
		popupMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.set_cursor_radius", -1d)));
		popupMenu.add(new JMenuItem(new SetCursorRadiusAction(this, "offroad.remove_cursor_radius", 0d)));
		mDrawPanel.setComponentPopupMenu(popupMenu);
		popupMenu.addPopupMenuListener(new OffRoadPopupMenuListener(this, popupMenu));
		mFrame.setJMenuBar(menubar);
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
		mFrame.addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent e) {
				mFrame.removeComponentListener(this);
				SwingUtilities.invokeLater(OsmWindow.this::checkMaps);
			}
		});
		mFrame.setVisible(true);
	}

	public void showDirectSearch() {
		if(!mDirectSearchVisible){
			mStatusBar.add(mDirectSearchPanel, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			mFrame.revalidate();
			mDirectSearchVisible = true;
			mDirectSearchTextField.selectAll();
		}
	}

	public void checkMaps() {
		MapRenderRepositories maps = getRenderer();
		boolean check = "true".equals(getOffroadProperties().getProperty(VECTOR_INDEXES_CHECK, "true"));
		if (check) {
			if (!maps.basemapExists()) {
				int result = JOptionPane.showConfirmDialog(mFrame, getString(R.string.basemap_missing),
						getString(R.string.base_world_map), JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null);
				if (result == JOptionPane.CANCEL_OPTION) {
					return;
				}
				if (result == JOptionPane.NO_OPTION) {
					getOffroadProperties().setProperty(VECTOR_INDEXES_CHECK, "false");
					return;
				}
				SwingUtilities.invokeLater(() -> {
					DownloadAction downloadAction = new DownloadAction(getInstance(), WorldRegion.WORLD_BASEMAP);
					downloadAction.actionPerformed(null);
				});
			}
		}
	}
	
	private void adaptMenuMnemonics(Component[] components) {
		for (Component comp : components) {
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
	
	/**
	 * @param jNavigationMenu
	 * @param name may be null, if set later.
	 * @param action
	 * @param keyStroke
	 */
	void addToMenu(JMenu jNavigationMenu, String name, ActionListener action, String keyStroke) {
		String actionName = (name!=null)?getOffRoadString(name):"UNKNOWN";
		if (name==null && action instanceof AbstractAction) {
			AbstractAction abstractAction = (AbstractAction) action;
			actionName = (String) abstractAction.getValue(AbstractAction.NAME);
		}
		JMenuItem navigationBackItem = new JMenuItem(actionName); //$NON-NLS-1$
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

	public static void main(String[] args) {
		final OsmWindow win = OsmWindow.getInstance();
		VersionInfo version = win.getVersion();
		log.info("Version: " + version.version + ", hash=" + version.hash);
		win.init();
		java.awt.EventQueue.invokeLater(win::createAndShowUI);

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

	private void init() {
		Dimension size = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		widthPixels = size.width;
		heightPixels = size.height;
		density = java.awt.Toolkit.getDefaultToolkit().getScreenResolution()/96f;
		mOsmAndLocationProvider = new OsmAndLocationProvider(this);
		mRegions = new OsmandRegions();
		mResourceManager = new ResourceManager(this);
		mResourceManager.indexingMaps(IProgress.EMPTY_PROGRESS);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
		startServer();
		mRendererRegistry = new RendererRegistry(this);
		mRendererRegistry.initRenderers(IProgress.EMPTY_PROGRESS);
		mRoutingHelper = new RoutingHelper(this);
		getRoutingHelper().addListener(this);
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
			JAXBContext jc = JAXBContext.newInstance(Resources.class);
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
		public String version;
		public String hash;
		public VersionInfo(String pVersion, String pHash) {
			super();
			version = pVersion;
			hash = pHash;
		}
		
	}
	public VersionInfo getVersion(){
		if(mVersionInfo == null){
			try {
				mVersionInfo = new VersionInfo("x.x.x", "0815");
				InputStream is = getResource("version.properties"); //$NON-NLS-1$
				if (is == null) {
					return mVersionInfo;
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
		log.info("Scaling fonts with scale " + pScale);
		for (Object next : UIManager.getLookAndFeelDefaults().keySet()) {
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


	public RenderingResult loadMGap(Graphics2D pG2, RotatedTileBox pTileRect, IntermediateImageListener pListener) {
		return getRenderer().loadMGap(pG2, pTileRect, getRenderingRulesStorage(), pListener);
	}

	public MapRenderRepositories getRenderer() {
		return mResourceManager.getRenderer();
	}

	static public void printClassPath() {
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			System.out.println("Classpath:"); //$NON-NLS-1$
			for (URL url : urls) {
				System.out.println(url.getFile());
			} 
		} else {
			System.out.println("Can' determine classpath for classloader " + cl);
		}
	}

	public InputStream getResource(String pIndex){
		if(pIndex != null){
			String name = pIndex;
			InputStream is = this.getClass().getResourceAsStream(name);
			if(is == null){
				name = "/" + pIndex; //$NON-NLS-1$
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
	
	public BufferedImage getBitmap(MapObject obj) {
		BufferedImage bmp = null;
		if (obj instanceof Amenity) {
			Amenity o = (Amenity) obj;
			String id = null;
			PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
			if (st != null) {
				if (RenderingIcons.containsSmallIcon(st.getIconKeyName())) {
					id = st.getIconKeyName();
				} else if (RenderingIcons.containsSmallIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
					id = st.getOsmTag() + "_" + st.getOsmValue();
				}
			}
			if (id != null) {
				bmp = RenderingIcons.getIcon(id, false);
			}
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
		return System.getProperty("user.home") + File.separator  + ".OffRoad" + File.separator + pIndex; //$NON-NLS-1$ //$NON-NLS-2$
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
		RotatedTileBox ctb = mDrawPanel.copyCurrentTileBox();
		// camera movement, find intermediate zoom, such that current and destination points are in:
		QuadRectExtendable joint = new QuadRectExtendable(ctb.getCenterLatLon());
		if(pQuadRectExtendable != null){
			joint.insert(pQuadRectExtendable.getTopLeft());
			joint.insert(pQuadRectExtendable.getBottomRight());
		}
		RotatedTileBox cameraTileBox = getCommonTileBox(pLocation, joint);
		RotatedTileBox tileBox = mDrawPanel.copyCurrentTileBox();
		tileBox.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
		tileBox = getCommonTileBox(tileBox, pLocation, pQuadRectExtendable);
		moveAnimated(cameraTileBox, ctb, ctb.getCenterLatLon());
		moveAnimated(tileBox, cameraTileBox, pLocation);
		setCursorPosition(pLocation);
	}

	public void moveAnimated(RotatedTileBox pNextTileBox, RotatedTileBox pCurrentTileBox, LatLon pLocation){
		if(pNextTileBox.getZoom() == pCurrentTileBox.getZoom()){
			// no zoom change at all:
			Point delta = pCurrentTileBox.getPoint(pNextTileBox.getLeftTopLatLon());
			mDrawPanel.moveAnimated(delta.x, delta.y, pNextTileBox);
		} else {
			mDrawPanel.zoomChange(pNextTileBox.getZoom()-pCurrentTileBox.getZoom(), pCurrentTileBox.getPoint(pLocation));
		}
		
	}
	
	protected RotatedTileBox getCommonTileBox(LatLon pLocation, QuadRectExtendable pQuadRectExtendable) {
		// make sure that all points of the rect are in:
		RotatedTileBox tileBox = mDrawPanel.copyCurrentTileBox();
		tileBox.setZoom(MAX_ZOOM);
		return getCommonTileBox(tileBox, pLocation, pQuadRectExtendable);
	}
	protected RotatedTileBox getCommonTileBox(RotatedTileBox pTileBox, LatLon pLocation, QuadRectExtendable pQuadRectExtendable) {
		if (pQuadRectExtendable!= null) {
//     		tileBox.setLatLonCenter(pLocation.getLatitude(), pLocation.getLongitude());
			zoomOutUntilFits(pTileBox, pQuadRectExtendable.getTopLeft());
			zoomOutUntilFits(pTileBox, pQuadRectExtendable.getBottomRight());
		}
		zoomOutUntilFits(pTileBox, pLocation);
		return pTileBox;
	}

	public void zoomOutUntilFits(RotatedTileBox tileBox, LatLon latlon) {
		while (!tileBox.containsLatLon(latlon)) {
			tileBox.setZoom(tileBox.getZoom() - 1);
		}
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
			// now, check if polyline is present:
			Polyline selectedPolyline = mDrawPanel.getPolylineLayer().getSelectedPolyline();
			double polyDist = 0;
			double polyArea = 0;
			if(selectedPolyline != null){
				polyDist = selectedPolyline.calculateLength();
				polyArea = selectedPolyline.calculateArea();
			}
			Object[] messageArguments = {distance,
					cursorPosition.getLatitude(),
					cursorPosition.getLongitude()};
			Object[] polyArguments =  {polyDist, polyArea};
			MessageFormat formatter = new MessageFormat(
					getOffRoadString("offroad.string47")); //$NON-NLS-1$
			String message = formatter.format(messageArguments);
			if (polyDist != 0 || polyArea != 0) {
				formatter = new MessageFormat(getOffRoadString("offroad.string47.poly"));
				message += " " + formatter.format(polyArguments);
			}
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

	public String getOffRoadString(String pKey, Object... pObj) {
		return MessageFormat.format(getOffRoadString(pKey), pObj);
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

	public void showQueueInformation(String pMsg) {
		mQueueStatus.setText(pMsg);
	}
	
	public void runInUIThread(Runnable pRunnable) {
		SwingUtilities.invokeLater(pRunnable);
	}
	
	public void addPoint(LatLon pPoint){
		MapPointStorage storage = new MapPointStorage(pPoint, getZoom());
		if(mPointStorageIndex < mPointStorage.size()-1){
			// remove all subsequent:
			mPointStorage.setSize(mPointStorageIndex + 1);
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
		queueAmenityTableUpdate();
		for (CursorPositionListener listener : mCursorPositionListeners) {
			listener.cursorPositionChanged(pLoc);
		}
	}

	private void queueAmenityTableUpdate() {
		// queue update of the amenity table.
		getDrawPanel().queue(new AmenityTableUpdateThread(getDrawPanel(), mAmenityTable), OsmBitmapPanel.PoolType.BACKGROUND);
	}

	
	public void runInUIThread(Runnable pRunnable, int pDelay) {
		Timer timer = new Timer(pDelay, pE -> pRunnable.run());
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
		return locale.getCountry().toLowerCase();
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
		return mDrawPanel.copyCurrentTileBox().getLatLonFromPixel(destination.x, destination.y);
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
		setWaitingCursor(true);
		mAmenityTable.setSearchResult(getSearchResult());
		setSearchType(SearchType.AMENITY);
		setWaitingCursor(false);
		getDrawPanel().refreshMap();
	}

	public List<MapObject> getSearchResult() {
		List<MapObject> result = new Vector<>();
		String filterId = getSettings().SELECTED_POI_FILTER_FOR_MAP.get();
		switch(mSearchType){
		case AMENITY:
			if (filterId != null) {
				PoiUIFilter filter = getPoiFilters().getFilterById(filterId);
				String filterString = getSettings().SELECTED_POI_FILTER_STRING_FOR_MAP.get();
				if(filterString != null && !filterString.isEmpty()){
					filter.setFilterByName(filterString);
				}
				LatLon latLon = getCursorPosition();
				result.addAll(filter.initializeNewSearch(latLon.getLatitude(), latLon.getLongitude(), -1,
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
				}));
			}
			break;
		case ROUTE:
			result.addAll(mRouteResult);
		}
		return result;
	}

//	public String getOffRoadString(String pString, Object[] pObjects) {
//		MessageFormat formatter = new MessageFormat(
//				getOffRoadString(pString)); //$NON-NLS-1$
//		return formatter.format(pObjects);
//	}

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
		mDrawPanel.removeAllLayers();
		// save properties:
		saveSettings();
		mFrame.dispose();
		System.exit(0);
	}

	/**
	 * Cached loading of buffered images.
	 * The image is the name of the image. Path and extension (.png) are 
	 * added by this method.
	 * 
	 * @param image
	 * @return
	 */
	public BufferedImage readImage(String image) {
		if(mBufferedImageCache.containsKey(image)){
			return mBufferedImageCache .get(image);
		}
		String path = IMAGE_PATH + getIconSize() + "/" + image + ".png";
		BufferedImage res = readImageInternally(path);
		mBufferedImageCache.put(image, res);
		return res;
	}

	protected BufferedImage readImageInternally(String path) {
		BufferedImage res = null;
		try {
			InputStream resource = getResource(path);
			if (resource == null) {
				log.error("Resource " + path + " not found!");
				return null;
			}
			res = ImageIO.read(resource);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	public void setRouteCalculated() {
		RouteCalculationResult route = getRoutingHelper().getRoute();
		List<RouteDirectionInfo> currentRoute = route.getRouteDirections();
		List<Location> locations = route.getImmutableAllLocations();
		List<MapObject> routeResult = new Vector<>();
		for (RouteDirectionInfo directionInfo : currentRoute) {
			routeResult.add(new LocationAsMapObject(locations.get(directionInfo.routePointOffset), directionInfo.getDescriptionRoutePart(),
					route.getDistanceToPoint(directionInfo.routePointOffset)));
		}
		mRouteResult = routeResult;
		setSearchType(SearchType.ROUTE);
	}

	private void setSearchType(SearchType pSearchType) {
		mSearchType = pSearchType;
		queueAmenityTableUpdate();
	}
	
	
	@Override
	public void newRouteIsCalculated(boolean pNewRoute, ValueHolder<Boolean> pShowToast) {
		float dist = getRoutingHelper().getRoute().getWholeDistance()/1000f;
		setStatus(getOffRoadString("offroad.routing_finished", dist));
		setRouteCalculated();
		getDrawPanel().drawLater();
	}

	@Override
	public void routeWasCancelled() {
		getDrawPanel().drawLater();
	}

	@Override
	public void routeWasFinished() {
	}

	public JMenuItem createJMenuItemForObject(IContextMenuProvider provider, Object am) {
		PointDescription pointDescription = provider.getObjectName(am);
		String name="UNKNOWN";
		if(pointDescription != null){
			name = pointDescription.getName();
		} else {
			// strange...
		}
		String description = provider.getObjectDescription(am);
		Icon icon = null;
		if (am instanceof MapObject) {
			MapObject mapObject = (MapObject) am;
			icon = getImageIcon(mapObject);
		}
		JMenuItem item = new JMenuItem(name, icon);
		item.setToolTipText(description);
		return item;
	}
	
	public javax.swing.Icon getImageIcon(MapObject am) {
		BufferedImage bitmap = getBitmap(am);
		if (bitmap != null) {
			return new ImageIcon(bitmap);
		} else {
			return new BlindIcon(20);
		}
	}

	public List<JMenuItem> getContextActionsForObject(IContextMenuProvider pProvider, Object pAm) {
		List<JMenuItem> result = new Vector<>();
		if (pAm instanceof Amenity) {
			Amenity am = (Amenity) pAm;
			JMenuItem item = createJMenuItemForObject(pProvider, pAm);
			item.addActionListener(pE -> {
				if (am.getType().isWiki()) {
					POIMapLayer.showWikipediaDialog(OsmWindow.this, OsmWindow.this, am);
				} else {
					String locationName = PointDescription.getLocationName(OsmWindow.this,
							am.getLocation().getLatitude(), am.getLocation().getLongitude(), true);
					POIMapLayer.showDescriptionDialog(OsmWindow.this, getInstance(),
							am.getAdditionalInfo().toString(), am.getName(getLanguage()));
				}
			});
			result.add(item);
		} 
		if (pAm instanceof FavouritePoint) {
			FavouritePoint point = (FavouritePoint) pAm;
			String editString = getOffRoadString("offroad.editFavourite", point.getName(), point.getCategory());
			JMenuItem item = new JMenuItem(new AddFavoriteAction(this, editString, null, point){
				@Override
				protected String getWindowTitle() {
					return editString;
				}
			});
			result.add(item);
			item = new JMenuItem(new DeleteFavoriteAction(this, getOffRoadString("offroad.deleteFavourite", point.getName(), point.getCategory()), null, point));
			result.add(item);
			
		}
		if (pAm instanceof TargetPoint) {
			TargetPoint targetPoint = (TargetPoint) pAm;
			PointNavigationAction removePointAction = new PointNavigationAction(this, "offroad.remove_navigation_point",
					(pHelper, pPosition) -> {
						if(targetPoint == pHelper.getPointToStart()){
							pHelper.clearStartPoint(false);
						}
						if(targetPoint == pHelper.getPointToNavigate()){
							pHelper.clearPointToNavigate(false);
						}
						int index = pHelper.getIntermediatePoints().indexOf(targetPoint);
						if(index >= 0){
							pHelper.removeWayPoint(false, index);
						}
					});

			JMenuItem item = new JMenuItem(removePointAction);
			result.add(item);
			
		}
		if (pAm instanceof WptPt) {
			WptPt waypt = (WptPt) pAm;
			// TODO: What to do here?
		}
		if (pAm instanceof SelectedGpxFile) {
			SelectedGpxFile sgf = (SelectedGpxFile) pAm;
			JMenuItem trackInfoItem = new JMenuItem(new ShowTrackDetailsAction(this, sgf.getGpxFile()));
			result.add(trackInfoItem);
		}
		if (pAm instanceof RouteCalculationResult) {
			RouteCalculationResult rcr = (RouteCalculationResult) pAm;
			JMenuItem routeInfoItem = new JMenuItem(new ShowRouteDetailsAction(this, rcr));
			result.add(routeInfoItem);
		}
		if (pAm instanceof Polyline) {
			Polyline polyline = (Polyline) pAm;
			result.add(new JMenuItem(new InsertPointIntoPolylineAction(this, polyline, getDrawPanel().getMouseLocation())));
			if (getDrawPanel().getPolylineLayer().isDragPoint(null, getDrawPanel().getMousePosition()) != null) {
				result.add(new JMenuItem(
						new RemovePointFromPolylineAction(this, polyline, getDrawPanel().getMouseLocation())));
			}
			result.add(new JMenuItem(new RemovePolylineAction(this, polyline)));
			result.add(new JMenuItem(new ShowPolylineDetailsAction(this, polyline)));
		}
		return result;
	}


	public String getOsmandIconsDir(){
		return OSMAND_ICONS_DIR + getIconSize() + "/";
	}

	public String getIconSize() {
		return getOffroadProperties().getProperty(OSMAND_ICONS_DIR_PREFIX, OSMAND_ICONS_DIR_DEFAULT_PREFIX);
	}

	public void openDocument(URL url) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				// fix for https://sourceforge.net/p/freemind/discussion/22102/thread/cf032151/?limit=25#c631
				URI uri = new URI(url.toString().replaceAll("^file:////", "file://"));
				desktop.browse(uri);
			} catch (Exception e) {
				log.fatal("Caught: " + e, e);
			}
		}
	}

	public DrawPolylineLayer getPolylineLayer(){
		return mDrawPanel.getPolylineLayer();
	}
	
	public static String marshall(Object pStorage, Class<?>[] pClasses){
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(pClasses, null);
			Marshaller m = jaxbContext.createMarshaller();
			StringWriter writer = new StringWriter();
			m.marshal(pStorage, writer);
			return writer.toString();
		} catch (JAXBException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static Object unmarshall(String pInput, Class<?>[] pClasses){
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(pClasses, null);
			Unmarshaller m = jaxbContext.createUnmarshaller();
			return m.unmarshal(new StringReader(pInput));
		} catch (JAXBException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public void moveDirectly(LatLon pLatLon) {
		if(mDrawPanel.isNearCenter(pLatLon, 0.5f)) {
			mDrawPanel.setCursor(pLatLon);
			return;
		}
		mDrawPanel.move(pLatLon, getZoom());
		mDrawPanel.setCursor(pLatLon);
	}

	
}

