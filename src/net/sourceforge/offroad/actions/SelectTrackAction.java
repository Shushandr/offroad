/** 
   OffRoad
   Copyright (C) 2016 name

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package net.sourceforge.offroad.actions;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.logging.Log;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.helpers.GpxUiHelper;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 24.05.2016
 */
public class SelectTrackAction extends OffRoadAction  {
	
	public static class TrackItem {
		@Override
		public String toString() {
			return "TrackItem [mFile=" + mFile + ", mGpxFile=" + mGpxFile + "]";
		}

		File mFile;
		private GPXFile mGpxFile;

		public String getFile() {
			return mFile.getName();
		}

		public TrackItem(File pFile, GPXFile pGpxFile) {
			super();
			mFile = pFile;
			mGpxFile = pGpxFile;
		}

		public boolean isSelected() {
			return mGpxFile != null;
		}
		
		public Date getDate(){
			if(isSelected()){
				WptPt lastPoint = mGpxFile.getLastPoint();
				if(lastPoint != null)	
					return new Date(lastPoint.time);
			}
			return null;
		}
		
		public String getAuthor(){
			if(isSelected()){
				return mGpxFile.author;
			}
			return "";
		}
		
		public String getTotalDistance(){
			if(isSelected()){
				return ""+mGpxFile.getAnalysis(0).totalDistance;
			}
			return "";
		}
	}

	public SelectTrackAction(OsmWindow pContext, 	String pTitle) {
		super(pContext, pTitle, null);
	}

	public static class GpxDateTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Date) {
				Date d = (Date) value;
				setText(DateFormat.getDateTimeInstance().format(d));
			}
			return this;
		}
	}

	public static class SelectedTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Boolean) {
				Boolean d = (Boolean) value;
				setText(d.toString());
			}
			return this;
		}
	}
	
	interface TrackItemToColumn {
		Object get(TrackItem pItem);
	}

	public static class TrackTableColumn {
		String mName;
		public Class<?> mClass;
		private TrackItemToColumn mMapping;
		private TableCellRenderer mRenderer;
		public TrackTableColumn(String pName, Class<?> pClass, TrackItemToColumn pMapping,
				TableCellRenderer pRenderer) {
			super();
			mName = pName;
			mClass = pClass;
			mMapping = pMapping;
			mRenderer = pRenderer;
		}

	}

	public class TrackTableModel extends AbstractTableModel {
		private Vector<TrackTableColumn> mColumns = new Vector<>();
		private Vector<TrackItem> mRows = new Vector<>();

		public TrackTableModel() {
			mColumns.addElement(new TrackTableColumn("selected", Boolean.class, TrackItem::isSelected,
					new SelectedTableCellRenderer()));
			mColumns.addElement(new TrackTableColumn("name", String.class, TrackItem::getFile,
					new DefaultTableCellRenderer()));
			mColumns.addElement(new TrackTableColumn("author", String.class, TrackItem::getAuthor,
					new DefaultTableCellRenderer()));
			mColumns.addElement(new TrackTableColumn("date", Date.class, TrackItem::getDate,
					new GpxDateTableCellRenderer()));
			mColumns.addElement(new TrackTableColumn("distance", String.class, TrackItem::getTotalDistance,
					new DefaultTableCellRenderer()));
			for (TrackTableColumn column : mColumns) {
				mTable.setDefaultRenderer(column.mClass, column.mRenderer);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return mColumns.get(columnIndex).mClass;
		}

		@Override
		public String getColumnName(int pColumn) {
			return getResourceString(mColumns.get(pColumn).mName);
		}

		public void addRow(TrackItem pItem) {
			mRows.add(pItem);
		}

		public TrackItem getItemAt(int pJ) {
			return mRows.get(pJ);
		}

		@Override
		public int getRowCount() {
			return mRows.size();
		}

		@Override
		public int getColumnCount() {
			return mColumns.size();
		}

		@Override
		public Object getValueAt(int pRowIndex, int pColumnIndex) {
			TrackTableColumn column = mColumns.get(pColumnIndex);
			return column.mMapping.get(getItemAt(pRowIndex));
		}

		public List<TrackItem> getSelectedRows() {
			int[] selectedRows = mTable.getSelectedRows();
			Vector<TrackItem> res = new Vector<>();
			for (int selectedRow : selectedRows) {
				int j = mTable.convertRowIndexToModel(selectedRow);
				res.add(getItemAt(j));
			}
			return res;
		}
	}

	private JTextField mTextField;
	private TrackTableModel mSourceModel;
	private JTable mTable;
	private KeyAdapter mKeyListener;
	private MouseAdapter mMouseListener;
	private TableRowSorter<TrackTableModel> mSorter;

	public SelectTrackAction(OsmWindow pContext) {
		super(pContext);
	}

	private final static Log log = PlatformUtil.getLog(SelectTrackAction.class);

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		setWaitingCursor();
		mDialog.setTitle(getResourceString("select_track"));
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		contentPane.add(new JLabel(getResourceString("filter")), new GridBagConstraints(0, y, 1, 1, 1.0, 1.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mTextField = new JTextField();
		contentPane.add(mTextField, new GridBagConstraints(1, y++, 1, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		mTable = new JTable();
		// FIXME: Size dependent
		mTable.setRowHeight(UIManager.getFont("Table.font").getSize());
		mSourceModel = new TrackTableModel();
		mTable.setModel(mSourceModel);
		mTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		mSorter = new TableRowSorter<>(mSourceModel);
		mTable.setRowSorter(mSorter);

		mTextField.getDocument().addDocumentListener(new FilterTextDocumentListener());
		mTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_DOWN) {
					pE.consume();
					mTable.requestFocus();
					selectFirstElementIfNecessary();
				}
			}

		});
		// override enter (taken from http://stackoverflow.com/questions/13516730/disable-enter-key-from-moving-down-a-row-in-jtable)
		mTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
	    mTable.getActionMap().put("Enter", new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent ae) {
	        	toggleSelectedItems(mSourceModel.getSelectedRows());
	        }
	    });

		mKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_UP) {
					if (mTable.getSelectedRowCount() == 0) {
						pE.consume();
						requestFocusToTextField();
					}
				}
				super.keyTyped(pE);
			}
		};
		mTable.addKeyListener(mKeyListener);
		mMouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() >= 2) {
					// Double-click detected
					evt.consume();
					toggleSelectedItems(mSourceModel.getSelectedRows());
				}
			}
		};
		mTable.addMouseListener(mMouseListener);
		contentPane.add(new JScrollPane(mTable), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		List<SelectedGpxFile> selectedGPXFiles = mContext.getSelectedGpxHelper().getSelectedGPXFiles();
		List<String> fileList = GpxUiHelper.getSortedGPXFilenames(mContext.getAppPath(IndexConstants.GPX_IMPORT_DIR), true);
		for (String file : fileList) {
			GPXFile gpxFile = null;
			for (SelectedGpxFile selFile : selectedGPXFiles) {
				String path = selFile.getGpxFile().path;
				if(path.equals(file)){
					gpxFile = selFile.getGpxFile();
					break;
				}
			}
			mSourceModel.addRow(new TrackItem(new File(file), gpxFile));
		}
		mDialog.pack();
		decorateDialog();
		removeWaitingCursor();
		decorateDialog();
		mDialog.setVisible(true);

	}

	protected void toggleSelectedItems(List<TrackItem> pList) {
		if(pList.isEmpty()){
			return;
		}
		boolean select = !pList.get(0).isSelected();
		for (TrackItem trackItem : pList) {
			log.info("Select ("+select + ") of item " + trackItem);
			if(select){
				if(trackItem.mGpxFile == null){
					trackItem.mGpxFile = GPXUtilities.loadGPXFile(mContext, trackItem.mFile);
				}
			} else {
				// deselect:
				trackItem.mGpxFile = null;
			}
		}
		mContext.getSelectedGpxHelper().clearAllGpxFileToShow();
		for (TrackItem trackItem : mSourceModel.mRows) {
			if (trackItem.mGpxFile != null) {
				mContext.getSelectedGpxHelper().selectGpxFile(trackItem.mGpxFile, true, false);
			}
		}
		mContext.getDrawPanel().drawLater();
		mSourceModel.fireTableDataChanged();
	}

	public void selectFirstElementIfNecessary() {
		if (mTable.getSelectedRowCount() == 0 && mTable.getModel().getRowCount() > 0) {
			mTable.setRowSelectionInterval(0, 0);
		}
	}

	public void requestFocusToTextField() {
		mTextField.selectAll();
		mTextField.requestFocus();
	}


	private final class FilterTextDocumentListener implements DocumentListener {
		private static final int TYPE_DELAY_TIME = 500;

		public FilterTextDocumentListener() {
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
						Document document = event.getDocument();
						final String text = getText(document);
						RowFilter<TrackTableModel, Object> rf = null;
						// If current expression doesn't parse, don't
						// update.
						try {
							rf = RowFilter.regexFilter("(?i)" + text, 1);
						} catch (java.util.regex.PatternSyntaxException e) {
							return;
						}
						mSorter.setRowFilter(rf);
					} catch (BadLocationException e) {
					}
				});
			}
		}

	}

}
