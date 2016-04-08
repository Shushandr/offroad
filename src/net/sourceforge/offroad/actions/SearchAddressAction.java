package net.sourceforge.offroad.actions;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.resources.RegionAddressRepository;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.data.QuadRectExtendable;
import net.sourceforge.offroad.data.RegionAsMapObject;
import net.sourceforge.offroad.data.TrivialResultMatcher;
import net.sourceforge.offroad.ui.FilteredListModel;

public class SearchAddressAction extends AbstractAction {

	private OsmWindow mContext;
	private JDialog mDialog;
	private MapObjectStore<RegionAsMapObject> mRegionStore;

	public SearchAddressAction(OsmWindow ctx) {
		mContext = ctx;
	}

	public static void addEscapeActionToDialog(JDialog dialog, Action action) {
		addKeyActionToDialog(dialog, action, "ESCAPE", "end_dialog");
	}

	public static void addKeyActionToDialog(JDialog dialog, Action action, String keyStroke, String actionId) {
		action.putValue(Action.NAME, actionId);
		// Register keystroke
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyStroke),
				action.getValue(Action.NAME));

		// Register action
		dialog.getRootPane().getActionMap().put(action.getValue(Action.NAME), action);
	}

	protected Comparator<? super RegionAddressRepository> createComparator() {
		return new Comparator<RegionAddressRepository>() {
			Collator col = Collator.getInstance();

			@Override
			public int compare(RegionAddressRepository lhs, RegionAddressRepository rhs) {
				return col.compare(lhs.getName(), rhs.getName());
			}
		};
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		mDialog = new JDialog(mContext.getWindow(), true /* modal */);
		setWaitingCursor();
		String windowTitle = "search";
		mDialog.setTitle(getResourceString(windowTitle));
		mDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		mDialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				disposeDialog();
			}
		});
		addEscapeActionToDialog(mDialog, new AbstractAction() {
			public void actionPerformed(ActionEvent arg0) {
				disposeDialog();
			}
		});
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		mRegionStore = new MapObjectStore<RegionAsMapObject>() {

			@Override
			public Collection<City> getSubObjects(RegionAsMapObject pObj) {
				RegionAddressRepository region = pObj.getRegion();
				// bad hack to fill mRegion field:
				this.mRegion = region;
				if (region != null) {
					// preload cities
					region.preloadCities(new TrivialResultMatcher<City>());
					return region.getLoadedCities();
				}
				return Collections.emptyList();
			}

			@Override
			public String loadSetting(OsmandSettings pSettings) {
				return pSettings.getLastSearchedRegion();
			}

			@Override
			public void saveSetting(OsmandSettings pSettings, String pValue) {
				pSettings.setLastSearchedRegion(pValue, getLatLon());
			}
		};
		y = mRegionStore.addMapObject(contentPane, y, "region");
		// fill region store:
		if (!mContext.getResourceManager().getAddressRepositories().isEmpty()) {
			ArrayList<RegionAddressRepository> initialListToFilter = new ArrayList<RegionAddressRepository>(
					mContext.getResourceManager().getAddressRepositories());
			for (RegionAddressRepository regionAddressRepository : initialListToFilter) {
				mRegionStore.mSourceModel.addElement(new RegionAsMapObject(regionAddressRepository));
			}
		}
		// cities:
		final MapObjectStore<City> cityStore = new MapObjectStore<City>() {

			@Override
			public Collection<Street> getSubObjects(City pObj) {
				mRegion.preloadStreets(pObj, new TrivialResultMatcher<Street>());
				return pObj.getStreets();
			}
			@Override
			public String loadSetting(OsmandSettings pSettings) {
				return pSettings.getLastSearchedCityName();
			}
			@Override
			public void saveSetting(OsmandSettings pSettings, String pValue) {
				City selectedCity = mList.getSelectedValue();
				Long id  = 0l;
				if (selectedCity != null) {
					id = selectedCity.getId();
				}
				pSettings.setLastSearchedCity(id, pValue, getLatLon());
			}
			
			@Override
			protected void addBoundingObjects(City pSelected, Vector<MapObject> pMoreObjects) {
				super.addBoundingObjects(pSelected, pMoreObjects);
				pMoreObjects.add(pSelected.getClosestCity());
			}
		};
		y = cityStore.addMapObject(contentPane, y, "city");

		MapObjectStore<Street> streetStore = new MapObjectStore<Street>() {

			@Override
			public Collection<Building> getSubObjects(Street pObj) {
				mRegion.preloadBuildings(pObj, new TrivialResultMatcher<Building>());
				return pObj.getBuildings();
			}
			@Override
			public String loadSetting(OsmandSettings pSettings) {
				return pSettings.getLastSearchedStreet();
			}
			@Override
			public void saveSetting(OsmandSettings pSettings, String pValue) {
				pSettings.setLastSearchedStreet(pValue, getLatLon());
			}
			@Override
			protected void addBoundingObjects(Street pSelected, Vector<MapObject> pMoreObjects) {
				super.addBoundingObjects(pSelected, pMoreObjects);
				pMoreObjects.addAll(pSelected.getIntersectedStreets());
			}
		};
		y = streetStore.addMapObject(contentPane, y, "street");
		MapObjectStore<Building> buildingStore = new MapObjectStore<Building>() {

			@Override
			public Collection<MapObject> getSubObjects(Building pObj) {
				return Collections.emptyList();
			}
			@Override
			public String loadSetting(OsmandSettings pSettings) {
				return pSettings.getLastSearchedBuilding();
			}
			@Override
			public void saveSetting(OsmandSettings pSettings, String pValue) {
				pSettings.setLastSearchedBuilding(pValue, getLatLon());
			}
		};
		y = buildingStore.addMapObject(contentPane, y, "building");
		// bindings:
		mRegionStore.setNextStore(cityStore).setNextStore(streetStore)
				.setNextStore(buildingStore);
		mRegionStore.load(mContext.getSettings());
		mRegionStore.addSelectionListener();
		mDialog.pack();
		removeWaitingCursor();
		mDialog.setVisible(true);

	}

	private void setWaitingCursor() {
		mDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		mContext.setWaitingCursor(true);
	}

	private void removeWaitingCursor() {
		mDialog.setCursor(Cursor.getDefaultCursor());
		mContext.setWaitingCursor(false);
	}
	
	protected LatLon getLatLon() {
		return mContext.getDrawPanel().getTileBox().getCenterLatLon();
	}

	private abstract class MapObjectStore<T extends MapObject> {
		RegionAddressRepository mRegion;
		JTextField mTextField;
		DefaultListModel<T> mSourceModel;
		FilteredListModel mFilteredSourceModel;
		JList<T> mList;
		ListSelectionListener mSelectionListener;
		KeyListener mKeyListener;
		MouseListener mMouseListener;
		private MapObjectStore mNextStore;
		private T selected;
		private T previousSelected;
		private FilteredListModel.Filter<T> mTextFilter;

		public abstract Collection<? extends MapObject> getSubObjects(T obj);

		public MapObjectStore setNextStore(MapObjectStore pNextStore) {
			mNextStore = pNextStore;
			return mNextStore;
		}

		public abstract String loadSetting(OsmandSettings pSettings);
		public abstract void saveSetting(OsmandSettings pSettings, String pValue);
		
		public void load(OsmandSettings pSettings) {
			String lastText = loadSetting(pSettings);
			mTextField.setText(lastText);
			mTextFilter.setText(lastText);
			mFilteredSourceModel.setFilter(mTextFilter);
			if(mFilteredSourceModel.getSize() >= 1){
				mList.setSelectedIndex(0);
			}
			if(mNextStore!=null){
				setSelectedItem(mList.getSelectedValue());
				mNextStore.load(pSettings);
			}
		}
		
		public void save(OsmandSettings pSettings) {
			String text = mTextField.getText();
			if(!mList.isSelectionEmpty()){
				text = mList.getSelectedValue().getName();
			}
			saveSetting(pSettings, text);
			if(mNextStore!=null){
				mNextStore.save(pSettings);
			}
		}
		class MyRenderer extends DefaultListCellRenderer {
			   public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			      Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			      setText(((MapObject) value).getName()); // where getValue is some method you implement that gets the text you want to render for the component
			      return c;
			}
		}			   
		public int addMapObject(Container contentPane, int y, String pName) {
			contentPane.add(new JLabel(getResourceString(pName)), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			mTextField = new JTextField();
			contentPane.add(mTextField, new GridBagConstraints(1, y++, 1, 1, 4.0, 1.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

			mSourceModel = new DefaultListModel<T>();
			mFilteredSourceModel = new FilteredListModel(mSourceModel);
			mList = new JList<T>(mFilteredSourceModel);
			mList.setCellRenderer(new MyRenderer());
			mTextFilter = new FilteredListModel.Filter<T>() {
				@Override
				public boolean accept(T pElement) {
					// FIXME: Translations!
					String text = getText().toLowerCase();
					String elementText = pElement.getName().toLowerCase();
					if (elementText.contains(text)) {
						return true;
					}
					return false;
				}
			};
			mTextField.getDocument()
					.addDocumentListener(new FilterTextDocumentListener(mFilteredSourceModel, mTextFilter, mList));
			mTextField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent pE) {
					if (pE.getKeyCode() == KeyEvent.VK_DOWN) {
						pE.consume();
						mList.requestFocus();
						selectFirstElementIfNecessary();
					}
					if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
						pE.consume();
						if(mNextStore != null){
							selectFirstElementIfNecessary();
							mNextStore.requestFocusToTextField();
						} else {
							// last field: show it:
							selectFirstElementIfNecessary();
							moveToEntity(mList.getSelectedValue());
						}
					}
				}

			});
			mKeyListener = new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent pE) {
					if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
						pE.consume();
						moveToEntity(mList.getSelectedValue());
					}
					if (pE.getKeyCode() == KeyEvent.VK_UP) {
						if (mList.getSelectedIndex() == 0) {
							pE.consume();
							requestFocusToTextField();
						}
					}
					super.keyTyped(pE);
				}
			};
			mList.addKeyListener(mKeyListener);
			mMouseListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent evt) {
					if (evt.getClickCount() >= 2) {
						// Double-click detected
						evt.consume();
						moveToEntity(mList.getSelectedValue());
					}
				}
			};
			mList.addMouseListener(mMouseListener);
			contentPane.add(new JScrollPane(mList), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0,
					GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
			return y;
		}

		public void selectFirstElementIfNecessary() {
			if (mList.isSelectionEmpty() && mList.getModel().getSize() > 0) {
				mList.setSelectedIndex(0);
			}
		}
		
		public void requestFocusToTextField(){
			mTextField.selectAll();
			mTextField.requestFocus();
		}

		/**
		 * @return the nextStore in order to be chained.
		 */
		public void addSelectionListener() {
			if(mNextStore == null){
				return;
			}
			mSelectionListener = new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent pE) {
					if (mList.isSelectionEmpty()) {
						return;
					}
					selected = mList.getSelectedValue();
					if (selected != null) {
						setWaitingCursor();
						if(!selected.equals(previousSelected)){
							mNextStore.clear();
						}
						previousSelected = selected;
						setSelectedItem(selected);
						removeWaitingCursor();
					}
				}
			};
			mList.addListSelectionListener(mSelectionListener);
			if(mNextStore != null){
				mNextStore.addSelectionListener();
			}
		}

		public void setSelectedItem(T pSelected) {
			if(pSelected == null){
				return;
			}
			selected = pSelected;
			Collection subObjects = getSubObjects(selected);
			mNextStore.mRegion = mRegion;
			mNextStore.mSourceModel.clear();
			for (Object sub : subObjects) {
				mNextStore.mSourceModel.addElement(sub);
			}
			mNextStore.mFilteredSourceModel.setFilter(new FilteredListModel.Filter() {
				public boolean accept(Object element) {
					return true;
				}
			});
		}
		
		public void moveToEntity(MapObject obj) {
			if (obj != null) {
				disposeDialog();
				// find correct zoom:
				QuadRectExtendable rect = null;
				if(selected != null){
					// trick: take all subobjects and take their hull.
					Collection<MapObject> subObjects = (Collection<MapObject>) getSubObjects(selected);
					Vector<MapObject> moreObjects = new Vector<>(subObjects);
					addBoundingObjects(selected, moreObjects);
					rect = new QuadRectExtendable(selected.getLocation());
					for (MapObject sub : subObjects) {
//						System.out.println("Adding " + sub + " to the bounding box.");
						rect.insert(sub.getLocation());
					}
					
				}
				mContext.move(obj.getLocation(), rect);
			}
		}

		protected void addBoundingObjects(T pSelected, Vector<MapObject> pMoreObjects) {
			// fill me, if there is more.
		}
		
		public void clear() {
			mSourceModel.clear();
			mTextField.setText("");
			mTextFilter.setText("");
			mFilteredSourceModel.setFilter(mTextFilter);
			if(mNextStore != null){
				mNextStore.clear();
			}
		}


	}

	private final class FilterTextDocumentListener implements DocumentListener {
		private static final int TYPE_DELAY_TIME = 500;
		private FilteredListModel mModel;
		private FilteredListModel.Filter mFilter;
		private JList mList;

		public FilterTextDocumentListener(FilteredListModel pModel, FilteredListModel.Filter pFilter, JList pList) {
			mModel = pModel;
			mFilter = pFilter;
			mList = pList;
		}

		private Timer mTypeDelayTimer = null;

		private synchronized void change(DocumentEvent event) {
			// stop old timer, if present:
			if (mTypeDelayTimer != null) {
				mTypeDelayTimer.cancel();
				mTypeDelayTimer = null;
			}
			mTypeDelayTimer = new Timer();
			mTypeDelayTimer.schedule(new DelayedTextEntry(event), TYPE_DELAY_TIME);
		}

		public void insertUpdate(DocumentEvent event) {
			change(event);
		}

		public void removeUpdate(DocumentEvent event) {
			change(event);

		}

		public void changedUpdate(DocumentEvent event) {
			change(event);

		}

		protected class DelayedTextEntry extends TimerTask {

			private final DocumentEvent event;

			DelayedTextEntry(DocumentEvent event) {
				this.event = event;
			}

			/**
			 * @throws BadLocationException
			 */
			private String getText(Document document) throws BadLocationException {
				String text = document.getText(0, document.getLength());
				return text;
			}

			public void run() {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						try {
							Document document = event.getDocument();
							final String text = getText(document);
							mFilter.setText(text);
							// check, if selected items are still correct:
							boolean filterAccepted = true;
							for (Object obj : mList.getSelectedValuesList()) {
								if(!mFilter.accept(obj)){
									filterAccepted = false;
								}
							}
							if (!filterAccepted) {
								mList.clearSelection();
							}
							mModel.setFilter(mFilter);
						} catch (BadLocationException e) {
						}
					}
				});
			}
		}

	}

	protected void disposeDialog() {
		mRegionStore.save(mContext.getSettings());
		mDialog.setVisible(false);
		mDialog.dispose();
	}

	private String getResourceString(String pWindowTitle) {
		return pWindowTitle;
	}

}
