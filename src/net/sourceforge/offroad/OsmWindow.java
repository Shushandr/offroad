package net.sourceforge.offroad;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.sourceforge.offroad.actions.SearchAddressAction;

/**
 * OffRoad
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow {

	public static final String RENDERING_STYLES_DIR = "rendering_styles/";
	public static final String OSMAND_ICONS_DIR = "rendering_styles/style-icons/drawable-xxhdpi/";
	private static OsmWindow minstance = null;
	private RenderingRulesStorage mRenderingRulesStorage;
	private ResourceManager mResourceManager;
	private OffRoadSettings settings = new OffRoadSettings(this);
	private OsmandSettings prefs = new OsmandSettings(settings);
	private R.string mStrings;
	private OsmandRegions mRegions;
	private OsmBitmapPanel mDrawPanel;
	private OsmBitmapPanelMouseAdapter mAdapter;
	private JFrame mFrame;

	public void createAndShowUI() {
		mDrawPanel = new OsmBitmapPanel(this);
		mAdapter = new OsmBitmapPanelMouseAdapter(mDrawPanel);
		mDrawPanel.addMouseListener(mAdapter);
		mDrawPanel.addMouseMotionListener(mAdapter);
		mDrawPanel.addMouseWheelListener(mAdapter);

		mFrame = new JFrame("OffRoad");
		mFrame.addKeyListener(mAdapter);
		mFrame.getContentPane().add(mDrawPanel);
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
		jSearchMenu.add(findItem);
		menubar.add(jSearchMenu);
		mFrame.setJMenuBar(menubar);
		mFrame.pack();
		mFrame.setLocationRelativeTo(null);
		mFrame.setVisible(true);
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
		mStrings = new R.string();
		mRegions = new OsmandRegions();
		mResourceManager = new ResourceManager(this);
		mResourceManager.indexingMaps(IProgress.EMPTY_PROGRESS);
	}

	public void loadMGap(Graphics2D pG2, RotatedTileBox pTileRect) {
		getRenderer().loadMGap(pG2, pTileRect, mRenderingRulesStorage);
	}

	public MapRenderRepositories getRenderer() {
		return mResourceManager.getRenderer();
	}

	private void init() throws XmlPullParserException, IOException {
		mRenderingRulesStorage = getRenderingRulesStorage();
	}

	public RenderingRulesStorage getRenderingRulesStorage() throws XmlPullParserException, IOException {
		final String loc = getAppPath(RENDERING_STYLES_DIR).getAbsolutePath() + File.separator;
		String defaultFile = loc + "default.render.xml";
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
				depends.parseRulesFromXmlInputStream(new FileInputStream(loc + name + ".render.xml"), ref);
				return depends;
			}
		};
		is = new FileInputStream(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);

		return storage;
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

	public void move(LatLon pLocation) {
		mDrawPanel.move(pLocation);
	}

}
