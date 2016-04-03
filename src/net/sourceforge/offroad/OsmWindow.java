package net.sourceforge.offroad;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;

/**
 * OffRoad
 * 
 * 
 * @author foltin
 * @date 26.03.2016
 */
public class OsmWindow {

	private static final String BASE_DIR = "/home/foltin/programming/java/osmand/dist/";
	public static final String RENDERING_STYLES_DIR = BASE_DIR + "rendering_styles/";
	public static final String OSMAND_MAPS_DIR = BASE_DIR + "maps/";
	public static final String OSMAND_ICONS_DIR = BASE_DIR
			+ "rendering_styles/style-icons/drawable-xxhdpi/";

	public void createAndShowUI() {
		final OsmBitmapPanel drawPanel = new OsmBitmapPanel(this);
		OsmBitmapPanelMouseAdapter mAdapter = new OsmBitmapPanelMouseAdapter(drawPanel);
		drawPanel.addMouseListener(mAdapter);
		drawPanel.addMouseMotionListener(mAdapter);
		drawPanel.addMouseWheelListener(mAdapter);

		JFrame frame = new JFrame("OffRoad");
		frame.getContentPane().add(drawPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(true);
		frame.addComponentListener(mAdapter);
		JMenuBar menubar = new JMenuBar();
		JMenu jSearchMenu = new JMenu("Search");
		JMenuItem findItem = new JMenuItem("Find...");
		findItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent pE) {
				String search= JOptionPane.showInputDialog("Search item");
				QuadRect bounds = drawPanel.getTileBox().getLatLonBounds();
				mResourceManager.searchAmenitiesByName(search, bounds.top, bounds.left, bounds.bottom, bounds.right, drawPanel.getTileBox().getLatitude(), drawPanel.getTileBox().getLongitude(), new ResultMatcher<Amenity>(){

					@Override
					public boolean publish(Amenity pObject) {
						System.out.println("found: " + pObject);
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}});
			}});
		jSearchMenu.add(findItem);
		menubar.add(jSearchMenu);
		frame.setJMenuBar(menubar);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private RenderingRulesStorage mRenderingRulesStorage;
	private ResourceManager mResourceManager;

	public static void main(String[] args) throws XmlPullParserException, IOException {
		final OsmWindow win = new OsmWindow();
		win.init();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				win.createAndShowUI();
			}
		});

	}

	public OsmWindow() {
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
		// RenderingRulesStorage.STORE_ATTTRIBUTES = true;
		// InputStream is =
		// RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		final String loc = RENDERING_STYLES_DIR;
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
				// depends.parseRulesFromXmlInputStream(RenderingRulesStorage.class.getResourceAsStream(name
				// + ".render.xml"), ref);
				depends.parseRulesFromXmlInputStream(new FileInputStream(loc + name + ".render.xml"), ref);
				return depends;
			}
		};
		is = new FileInputStream(defaultFile);
		storage.parseRulesFromXmlInputStream(is, resolver);

		return storage;
	}

	public File getAppPath(String pIndex) {
		if(pIndex == null){
			pIndex = "";
		}
		String pathname = BASE_DIR+pIndex;
		System.out.println("Searching for " + pathname);
		return new File(pathname);
	}

	public OsmandSettings getSettings() {
		return prefs;
	}
	
	private OsmandSettings prefs = new OsmandSettings(new SettingsAPI() {
		
		@Override
		public String getString(Object pPref, String pKey, String pDefValue) {
			return pDefValue;
		}
		
		@Override
		public Object getPreferenceObject(String pKey) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public long getLong(Object pPref, String pKey, long pDefValue) {
			return pDefValue;
		}
		
		@Override
		public int getInt(Object pPref, String pKey, int pDefValue) {
			return pDefValue;
		}
		
		@Override
		public float getFloat(Object pPref, String pKey, float pDefValue) {
			return pDefValue;
		}
		
		@Override
		public boolean getBoolean(Object pPref, String pKey, boolean pDefValue) {
			return pDefValue;
		}
		
		@Override
		public SettingsEditor edit(Object pPref) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public boolean contains(Object pPref, String pKey) {
			// TODO Auto-generated method stub
			return false;
		}
	});


}
