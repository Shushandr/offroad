package net.sourceforge.offroad.actions;

import java.awt.Component;
import java.awt.Container;
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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

/**
 * TODO:
 * * OK+Cancel Button
 * * Camera driving to the new place
 * * Bug fix for selection
 * * Multi-Selection
 * 
 * @author foltin
 * @date 09.04.2016
 */
public class SearchAddressAction extends OffRoadAction {

	MapObjectStore<RegionAsMapObject> mRegionStore;

	public SearchAddressAction(OsmWindow ctx) {
		super(ctx);
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
		createDialog();
		setWaitingCursor();
		mDialog.setTitle(getResourceString("search"));
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
					region.preloadCities(new TrivialResultMatcher<>());
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
			ArrayList<RegionAddressRepository> initialListToFilter = new ArrayList<>(
					mContext.getResourceManager().getAddressRepositories());
			for (RegionAddressRepository regionAddressRepository : initialListToFilter) {
				mRegionStore.mSourceModel.addElement(new RegionAsMapObject(regionAddressRepository));
			}
		}
		// cities:
		final MapObjectStore<City> cityStore = new MapObjectStore<City>() {

			@Override
			public Collection<Street> getSubObjects(City pObj) {
				mRegion.preloadStreets(pObj, new TrivialResultMatcher<>());
				return pObj.getStreets();
			}

			@Override
			public String loadSetting(OsmandSettings pSettings) {
				return pSettings.getLastSearchedCityName();
			}

			@Override
			public void saveSetting(OsmandSettings pSettings, String pValue) {
				City selectedCity = mList.getSelectedValue();
				Long id = 0l;
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
				mRegion.preloadBuildings(pObj, new TrivialResultMatcher<>());
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
		// select region:
		mRegionStore.mTextField.selectAll();
		mRegionStore.addSelectionListener();
		mDialog.pack();
		removeWaitingCursor();
		decorateDialog();
		mDialog.setVisible(true);

	}

	@Override
	public void save() {
		super.save();
		mRegionStore.save(mContext.getSettings());
	}


	protected LatLon getLatLon() {
		return mContext.getDrawPanel().copyCurrentTileBox().getCenterLatLon();
	}

	abstract class MapObjectStore<T extends MapObject> {
		RegionAddressRepository mRegion;
		JTextField mTextField;
		DefaultListModel<T> mSourceModel;
		FilteredListModel<T> mFilteredSourceModel;
		JList<T> mList;
		ListSelectionListener mSelectionListener;
		KeyListener mKeyListener;
		MouseListener mMouseListener;
		private MapObjectStore mNextStore;
		private T selected;
		private T previousSelected;
		private FilteredListModel<T>.Filter mTextFilter;

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
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(((MapObject) value).getName());
				return c;
			}
		}

		public int addMapObject(Container contentPane, int y, String pName) {
			contentPane.add(new JLabel(getResourceString(pName)), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			mTextField = new JTextField();
			contentPane.add(mTextField, new GridBagConstraints(1, y++, 1, 1, 4.0, 1.0, GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

			mSourceModel = new DefaultListModel<>();
			mFilteredSourceModel = new FilteredListModel<>(mSourceModel);
			mList = new JList<>(mFilteredSourceModel);
			mList.setCellRenderer(new MyRenderer());
			mTextFilter = mFilteredSourceModel.new Filter() {
				@Override
				public boolean accept(T pElement) {
					// FIXME: Translations!
					String text = getText().toLowerCase();
					String elementText = pElement.getName().toLowerCase();
					return elementText.contains(text);
				}
			};
			mTextField.getDocument()
					.addDocumentListener(new FilterTextDocumentListener<>(this));
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
			mSelectionListener = pE -> {
				if (mList.isSelectionEmpty()) {
					previousSelected = null;
					selected = null;
					mNextStore.clear();
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
			mNextStore.insertList(getSubObjects(selected), mRegion);
		}

		public void insertList(Collection<T> subObjects, RegionAddressRepository pRegion){
			mRegion = pRegion;
			mFilteredSourceModel.setFilter(null);
			mSourceModel.clear();
			for (T sub : subObjects) {
				mSourceModel.addElement(sub);
			}
		}
		
		public void moveToEntity(MapObject obj) {
			if (obj != null) {
				disposeDialog();
				// find correct zoom:
				QuadRectExtendable rect = null;
				if(selected != null){
					// trick: take all subobjects and take their hull.
					Collection<? extends MapObject> subObjects = getSubObjects(selected);
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
			mList.clearSelection();
			mSourceModel.clear();
			mTextField.setText("");
			mTextFilter.setText("");
			mFilteredSourceModel.setFilter(null);
			if(mNextStore != null){
				mNextStore.clear();
			}
		}


	}

	private final class FilterTextDocumentListener<T extends MapObject> implements DocumentListener {
		private static final int TYPE_DELAY_TIME = 500;
		private MapObjectStore<T> mMapObjectStore;

		public FilterTextDocumentListener(MapObjectStore<T> pMapObjectStore) {
			mMapObjectStore = pMapObjectStore;
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
				return document.getText(0, document.getLength());
			}

			public void run() {
				SwingUtilities.invokeLater(() -> {
					try {
						List<T> selectedValuesList = mMapObjectStore.mList.getSelectedValuesList();
						Document document = event.getDocument();
						final String text = getText(document);
						mMapObjectStore.mTextFilter.setText(text);
						// check, if selected items are still correct:
						boolean filterAccepted = true;
						for (T obj : selectedValuesList) {
							if(!mMapObjectStore.mTextFilter.accept(obj)){
								filterAccepted = false;
							}
						}
						if (!filterAccepted) {
							mMapObjectStore.mList.clearSelection();
						}
						mMapObjectStore.mFilteredSourceModel.setFilter(mMapObjectStore.mTextFilter);
					} catch (BadLocationException e) {
					}
				});
			}
		}

	}

}
