package net.sourceforge.offroad.actions;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
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

import net.osmand.ResultMatcher;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.plus.resources.RegionAddressRepository;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.FilteredListModel;

public class SearchAddressAction extends AbstractAction {

	private OsmWindow mContext;
	private JDialog mDialog;
	private JTextField mFilterTextRegionField;
	private FilteredListModel mFilteredRegionModel;
	private JList<RegionAddressRepository> mRegionList;
	private DefaultListModel<RegionAddressRepository> mSourceRegionModel;

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
		contentPane.add(new JLabel(getResourceString("region")), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mFilterTextRegionField = new JTextField();
		contentPane.add(mFilterTextRegionField, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		if (mContext.getResourceManager().getAddressRepositories().isEmpty()) {
		}
		ArrayList<RegionAddressRepository> initialListToFilter = new ArrayList<RegionAddressRepository>(
				mContext.getResourceManager().getAddressRepositories());
		mSourceRegionModel = new DefaultListModel<RegionAddressRepository>();
		for (RegionAddressRepository regionAddressRepository : initialListToFilter) {
			mSourceRegionModel.addElement(regionAddressRepository);
		}

		mFilteredRegionModel = new FilteredListModel(mSourceRegionModel);
		mRegionList = new JList<RegionAddressRepository>(mFilteredRegionModel);
		mFilteredRegionModel.setFilter(new FilteredListModel.Filter() {
			public boolean accept(Object element) {
				return true; 
			}
		});
		mFilterTextRegionField.getDocument().addDocumentListener(new FilterTextDocumentListener(mFilteredRegionModel, new FilteredListModel.Filter() {
			public boolean accept(Object element) {
				if (element instanceof RegionAddressRepository) {
					RegionAddressRepository repo = (RegionAddressRepository) element;
					if(repo.getFileName().toLowerCase().contains(getText().toLowerCase())){
						return true;
					}
				}
				return false; 
			}
		}));
		contentPane.add(mRegionList, new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		// cities:
		final MapObjectStore<City> cityStore = new MapObjectStore<City>(){

			@Override
			public Collection getSubObjects(City pObj) {
				mRegion.preloadStreets(pObj, new ResultMatcher<Street>() {

					@Override
					public boolean publish(Street pObject) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
				return pObj.getStreets();
			}};
		y = cityStore.addMapObject(contentPane, y, "city");		

		mRegionList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent pE) {
				if(mRegionList.isSelectionEmpty()){
					return;
				}
				// else enable city list:
				RegionAddressRepository region = mRegionList.getSelectedValue();
				if(region != null){
					// preload cities
					region.preloadCities(new ResultMatcher<City>() {
						
						@Override
						public boolean publish(City object) {
							return true;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					});
					cityStore.mRegion = region;
					cityStore.mSourceModel.clear();
					for (City city : region.getLoadedCities()) {
							cityStore.mSourceModel.addElement(city);
					}
					cityStore.mFilteredSourceModel.setFilter(new FilteredListModel.Filter() {
						public boolean accept(Object element) {
							return true; 
						}
					});
				}
				
			}
		});

		
		MapObjectStore<Street> streetStore = new MapObjectStore<Street>(){

			@Override
			public Collection getSubObjects(Street pObj) {
				mRegion.preloadBuildings(pObj, new ResultMatcher<Building>() {

					@Override
					public boolean publish(Building pObject) {
						return true;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				});
				return pObj.getBuildings();
			}};
		y = streetStore.addMapObject(contentPane, y, "street");		
		cityStore.addSelectionListener(streetStore);
		MapObjectStore<Building> buildingStore = new MapObjectStore<Building>(){

			@Override
			public Collection getSubObjects(Building pObj) {
				return Collections.emptyList();
			}};
		y = buildingStore.addMapObject(contentPane, y, "building");		
		streetStore.addSelectionListener(buildingStore);
		mDialog.pack();
		mDialog.setVisible(true);

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
		
		public abstract Collection getSubObjects(T obj);

		public int addMapObject(Container contentPane, int y, String pName) {
			contentPane.add(new JLabel(getResourceString(pName)), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			mTextField = new JTextField();
			contentPane.add(mTextField, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			
			mSourceModel = new DefaultListModel<T>();
			mFilteredSourceModel = new FilteredListModel(mSourceModel);
			mList = new JList<T>(mFilteredSourceModel);
			mTextField.getDocument().addDocumentListener(new FilterTextDocumentListener(mFilteredSourceModel, new FilteredListModel.Filter<T>() {
				@Override
				public boolean accept(T pElement) {
					// FIXME: Translations!
					if(pElement.getName().toLowerCase().contains(getText().toLowerCase())){
						return true;
					}
					
					return false; 
				}
			}));
			mKeyListener = new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent pE) {
					if(pE.getKeyCode()== KeyEvent.VK_ENTER){
						pE.consume();
						moveToEntity(mList.getSelectedValue());
					}
					super.keyTyped(pE);
				}
			};
			mList.addKeyListener(mKeyListener);
			mMouseListener = new MouseAdapter() {
				public void mouseClicked(MouseEvent evt) {
					JList list = (JList)evt.getSource();
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

		public void addSelectionListener(final MapObjectStore nextStore) {
			mSelectionListener = new ListSelectionListener() {
				
				@Override
				public void valueChanged(ListSelectionEvent pE) {
					if(mList.isSelectionEmpty()){
						return;
					}
					T selected = mList.getSelectedValue();
					if(selected != null){
						nextStore.mRegion = mRegion;
						nextStore.mSourceModel.clear();
						for (Object sub : getSubObjects(selected)) {
								nextStore.mSourceModel.addElement(sub);
						}
						nextStore.mFilteredSourceModel.setFilter(new FilteredListModel.Filter() {
							public boolean accept(Object element) {
								return true; 
							}
						});
					}
					
				}
			};
			mList.addListSelectionListener(mSelectionListener);
		}
	}


	private final class FilterTextDocumentListener implements DocumentListener {
		private static final int TYPE_DELAY_TIME = 500;
		private FilteredListModel mModel;
		private FilteredListModel.Filter mFilter;

		public FilterTextDocumentListener(FilteredListModel pModel, FilteredListModel.Filter pFilter) {
			mModel = pModel;
			mFilter = pFilter;
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
							mModel.setFilter(mFilter);
						} catch (BadLocationException e) {
						}
					}
				});
			}
		}

	}

	protected void disposeDialog() {
		mDialog.setVisible(false);
		mDialog.dispose();
	}

	private String getResourceString(String pWindowTitle) {
		return pWindowTitle;
	}

	public void moveToEntity(MapObject obj) {
		if (obj != null) {
			disposeDialog();
			mContext.move(obj.getLocation());
		}
	}

}
