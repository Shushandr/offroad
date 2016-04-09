package net.sourceforge.offroad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.util.MapUtils;
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
	private OsmandSettings prefs = new OsmandSettings(settings);
	private R.string mStrings;
	private OsmandRegions mRegions;
	private OsmBitmapPanel mDrawPanel;
	private OsmBitmapPanelMouseAdapter mAdapter;
	private JFrame mFrame;
	private JLabel mStatusLabel;
	private Timer mMouseMoveTimer;

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
		scaleAllFonts(2.0f);
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
			// calculate the distance to the cursor
			MouseEvent e = mAdapter.getMouseEvent();
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

}
