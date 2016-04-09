package net.sourceforge.offroad.actions;
/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

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

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.logging.Log;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.DownloadOsmandIndexesHelper;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.IndexFileList;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.sourceforge.offroad.OsmWindow;

public class DownloadAction extends OffRoadAction {
	
	public class TypeTableCellRenderer extends DefaultTableCellRenderer {
	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        if (value instanceof DownloadActivityType) {
	        	DownloadActivityType dat = (DownloadActivityType)value;
				setText(dat.getString(mContext));
	        }
	        return this;
	    }
	}
	
	public class ArchiveSizeTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Double) {
				Double d = (Double) value;
				setText(""+d+" MB");
			}
			return this;
		}
	}
	
	public class RemoteDateTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Long) {
				Long d = (Long) value;
				setText(DateFormat.getDateTimeInstance().format(new Date(d)));
			}
			return this;
		}
	}
	
	
	
	interface IndexItemToColumn {
	    Object get(IndexItem pItem);
	}
	public static class DownloadTableColumn {
		private IndexItemToColumn mMapping;
		String mName;
		Class mClass;
		private TableCellRenderer mRenderer;
		public DownloadTableColumn(String pName, Class pClass, IndexItemToColumn pMapping, TableCellRenderer pRenderer) {
			super();
			mName = pName;
			mClass = pClass;
			mMapping = pMapping;
			mRenderer = pRenderer;
		}
	}
	public class DownloadTableModel extends AbstractTableModel {
		private Vector<DownloadTableColumn> mColumns = new Vector<>();
		private Vector<IndexItem> mRows = new Vector<>();
		public DownloadTableModel() {
			mColumns.addElement(new DownloadTableColumn("name", String.class, item -> item.getBasename(), new DefaultTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("type", DownloadActivityType.class, item -> item.getType(), new TypeTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("size", Double.class, item -> item.getArchiveSizeMB(), new ArchiveSizeTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("remotedate", Long.class, item->item.getTimestamp(), new RemoteDateTableCellRenderer()));
			for (DownloadTableColumn column : mColumns) {
				mTable.setDefaultRenderer(column.mClass, column.mRenderer);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return mColumns.get(columnIndex).mClass;
		}
		
		public void addRow(IndexItem pItem){
			mRows.add(pItem);
		}

		public IndexItem getItemAt(int pJ) {
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
			DownloadTableColumn column = mColumns.get(pColumnIndex);
			return column.mMapping.get(getItemAt(pRowIndex));
		}

		public List<IndexItem> getSelectedRows() {
			int[] selectedRows = mTable.getSelectedRows();
			Vector<IndexItem> res = new Vector<>();
			for (int i = 0; i < selectedRows.length; i++) {
				int j = selectedRows[i];
				res.add(getItemAt(j));
			}
			return res;
		}
	}

	private JTextField mTextField;
	private DownloadTableModel mSourceModel;
	private JTable mTable;
	private KeyAdapter mKeyListener;
	private MouseAdapter mMouseListener;

	public DownloadAction(OsmWindow pContext) {
		super(pContext);
	}

	private final static Log log = PlatformUtil.getLog(DownloadAction.class);

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		setWaitingCursor();
		mDialog.setTitle(getResourceString("download"));
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
		mSourceModel = new DownloadTableModel();
		mTable.setModel(mSourceModel);
		mTextField.getDocument().addDocumentListener(new FilterTextDocumentListener());
		mTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_DOWN) {
					pE.consume();
					mTable.requestFocus();
					selectFirstElementIfNecessary();
				}
				if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
					pE.consume();
					download(mSourceModel.getSelectedRows());
				}
			}

		});
		mKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
					pE.consume();
					download(mSourceModel.getSelectedRows());
				}
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
					download(mSourceModel.getSelectedRows());
				}
			}
		};
		mTable.addMouseListener(mMouseListener);
		contentPane.add(new JScrollPane(mTable), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		IndexFileList indexesList = DownloadOsmandIndexesHelper.getIndexesList(mContext);
		DownloadResources downloadResources = new DownloadResources(mContext);
		downloadResources.updateLoadedFiles();
		for (IndexItem item : indexesList.getIndexFiles()) {
			mSourceModel.addRow(item);
		}
		mDialog.pack();
		removeWaitingCursor();
		mDialog.setVisible(true);

	}

	public void download(List<IndexItem> pList) {
		DownloadFileHelper helper = new DownloadFileHelper(mContext);
		Vector<File> toReIndex = new Vector<>();
		try {
			for (IndexItem item : pList) {
				helper.downloadFile(item.createDownloadEntry(mContext), IProgress.EMPTY_PROGRESS, toReIndex,
						new DownloadFileShowWarning() {
					
					@Override
					public void showWarning(String pWarning) {
						System.err.println("DOWNLOAD WARNING: " + pWarning);
						
					}
				}, false);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	@Override
	public void save() {
		// TODO Auto-generated method stub

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
				String text = document.getText(0, document.getLength());
				return text;
			}

			public void run() {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						try {
							List<IndexItem> selectedValuesList = mSourceModel.getSelectedRows();
							Document document = event.getDocument();
							final String text = getText(document);
//							mTextFilter.setText(text);
//							// check, if selected items are still correct:
//							boolean filterAccepted = true;
//							for (IndexItem obj : selectedValuesList) {
//								if (!mTextFilter.accept(obj)) {
//									filterAccepted = false;
//								}
//							}
//							if (!filterAccepted) {
//								mTable.clearSelection();
//							}
//							mFilteredSourceModel.setFilter(mTextFilter);
						} catch (BadLocationException e) {
						}
					}
				});
			}
		}

	}

}
