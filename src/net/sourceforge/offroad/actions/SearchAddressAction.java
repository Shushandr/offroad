package net.sourceforge.offroad.actions;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.Collator;
import java.util.ArrayList;
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
import net.osmand.data.City;
import net.osmand.plus.resources.RegionAddressRepository;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.FilteredListModel;

public class SearchAddressAction extends AbstractAction {

	private OsmWindow mContext;
	private JDialog mDialog;
	private JTextField mFilterTextRegionField;
	private FilteredListModel mFilteredRegionModel;
	private JList<RegionAddressRepository> mRegionList;
	private JTextField mFilterTextCityField;
	private JList<City> mCityList;
	private FilteredListModel mFilteredCityModel;
	private DefaultListModel<City> mSourceCityModel;
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
		mFilterTextRegionField.getDocument().addDocumentListener(new FilterTextDocumentListener());
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
		mRegionList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent pE) {
				if(mRegionList.isSelectionEmpty()){
					return;
				}
				// else enable city list:
				RegionAddressRepository region = mContext.getResourceManager().getRegionRepository(mRegionList.getSelectedValue().getFileName());
				if(region != null){
					mSourceCityModel.clear();
					// preload cities
					region.preloadCities(new ResultMatcher<City>() {
						
						@Override
						public boolean publish(City object) {
							mSourceCityModel.addElement(object);
							return true;
						}
						
						@Override
						public boolean isCancelled() {
							return false;
						}
					});
					mFilteredCityModel = new FilteredListModel(mSourceCityModel);
					mCityList.setModel(mFilteredCityModel);
					mFilteredCityModel.setFilter(new FilteredListModel.Filter() {
						public boolean accept(Object element) {
							return true; 
						}
					});
				}
				
			}
		});
		contentPane.add(mRegionList, new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		// cities:
		contentPane.add(new JLabel(getResourceString("city")), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mFilterTextCityField = new JTextField();
//		mFilterTextCityField.getDocument().addDocumentListener(new FilterTextDocumentListener());
		contentPane.add(mFilterTextCityField, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		mSourceCityModel = new DefaultListModel<City>();
		mFilteredCityModel = new FilteredListModel(mSourceCityModel);
		mCityList = new JList<City>(mFilteredCityModel);
		mCityList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent pE) {
				// else enable street list:
				
			}
		});
		contentPane.add(new JScrollPane(mCityList), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0,
				GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		

		mDialog.pack();
		mDialog.setVisible(true);

	}

	private final class FilterTextDocumentListener implements DocumentListener {
		private static final int TYPE_DELAY_TIME = 500;

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
							mFilteredRegionModel.setFilter(new FilteredListModel.Filter() {
								public boolean accept(Object element) {
									if (element instanceof RegionAddressRepository) {
										RegionAddressRepository repo = (RegionAddressRepository) element;
										if(repo.getFileName().toLowerCase().contains(text.toLowerCase())){
											return true;
										}
									}
									return false; 
								}
							});
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

}
