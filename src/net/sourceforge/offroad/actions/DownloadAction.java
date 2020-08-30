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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.download.DownloadOsmandIndexesHelper;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.IndexFileList;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.ResourceManager;
import net.sourceforge.offroad.OsmWindow;

public class DownloadAction extends OffRoadAction {

	public static final int MAX_NUMBER_OF_FILES = 10;

	public class TypeTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof DownloadActivityType) {
				DownloadActivityType dat = (DownloadActivityType) value;
				setText(dat.getString(mContext));
			}
			return this;
		}
	}
    public static final MessageFormat formatGb = new MessageFormat("{0, number,#.##} GB", Locale.US);
    public static final MessageFormat formatMb = new MessageFormat("{0, number,##.#} MB", Locale.US);

	public static class ArchiveSizeTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Double) {
				Double d = (Double) value;
				setText(formatMb.format(new Object[]{d}));
			}
			return this;
		}
	}

	public static class RemoteDateTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Long) {
				Long d = (Long) value;
				setText(DateFormat.getDateTimeInstance().format(new Date(d)));
			}
			return this;
		}
	}

	enum DownloadStatus {
		DOWNLOADED, UPDATEABLE, NOTPRESENT
	}

	public class DownloadedTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof DownloadStatus) {
				DownloadStatus d = (DownloadStatus) value;
				setText((d == DownloadStatus.NOTPRESENT)?"":getResourceString("offroad.download_"+d.name()));
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
		Class<?> mClass;
		private TableCellRenderer mRenderer;

		public DownloadTableColumn(String pName, Class<?> pClass, IndexItemToColumn pMapping,
				TableCellRenderer pRenderer) {
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
			mColumns.addElement(new DownloadTableColumn("offroad.download_present", DownloadStatus.class, DownloadAction.this::getDownloadStatus,
					new DownloadedTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("offroad.download_name", String.class, IndexItem::getBasename,
					new DefaultTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("offroad.download_type", DownloadActivityType.class, IndexItem::getType,
					new TypeTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("offroad.download_size", Double.class, IndexItem::getArchiveSizeMB,
					new ArchiveSizeTableCellRenderer()));
			mColumns.addElement(new DownloadTableColumn("offroad.download_remotedate", Long.class, IndexItem::getTimestamp,
					new RemoteDateTableCellRenderer()));
			for (DownloadTableColumn column : mColumns) {
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

		public void addRow(IndexItem pItem) {
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
			for (int selectedRow : selectedRows) {
				int j = mTable.convertRowIndexToModel(selectedRow);
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
	private TableRowSorter<DownloadTableModel> mSorter;
	private JLabel mProgressStatus;
	private JProgressBar mProgressBar;
	private String mInitialFilter;

	public DownloadAction(OsmWindow pContext) {
		this(pContext, null);
	}
	
	public DownloadAction(OsmWindow pContext, String pInitialFilter) {
		super(pContext);
		mInitialFilter = pInitialFilter;
	}

	public DownloadStatus getDownloadStatus(IndexItem pItem) {
		String fileName = pItem.getTargetFileName();
		ResourceManager rm = mContext.getResourceManager();
		Map<String, String> indexFileNames = rm.getIndexFileNames();
		if(indexFileNames.containsKey(fileName)) {
			String localDate = indexFileNames.get(fileName);
			String remoteDate = pItem.getDate(DateFormat.getDateInstance());
			if(localDate != null && localDate.equals(remoteDate)){
				return DownloadStatus.DOWNLOADED;
			}
			return DownloadStatus.UPDATEABLE;
		}
		return DownloadStatus.NOTPRESENT;
	}


	private final static Log log = PlatformUtil.getLog(DownloadAction.class);
	private DownloadResources mDownloadResources;
	private Thread mDownloadThread;
	private boolean mIsDownloadInterrupted;
	private JButton mInterruptDownload;
	private JButton mDownloadButton;
	private JButton mDeleteButton;

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		setWaitingCursor();
		mDialog.setTitle(getResourceString("offroad.download"));
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
		mSourceModel = new DownloadTableModel();
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
		mKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
					pE.consume();
					doDownload();
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
					doDownload();
				}
			}
		};
		mTable.addMouseListener(mMouseListener);
		contentPane.add(new JScrollPane(mTable), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		mProgressStatus = new JLabel("!");
		contentPane.add(mProgressStatus, new GridBagConstraints(0, y, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		mProgressBar = new JProgressBar();
		mProgressBar.setStringPainted(true);
		contentPane.add(mProgressBar, new GridBagConstraints(1, y++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mDownloadButton = new JButton(getResourceString("offroad.downloadButton"));
		mDownloadButton.addActionListener(e -> doDownload());
		mDownloadButton.setEnabled(false);
		mTable.getSelectionModel().addListSelectionListener(pE12 -> {
			mDownloadButton.setEnabled(mTable.getSelectedRowCount() > 0 && mTable.getSelectedRowCount() <= MAX_NUMBER_OF_FILES);
			mDeleteButton.setEnabled(mTable.getSelectedRowCount() > 0);
		});
		contentPane.add(mDownloadButton, new GridBagConstraints(0, y, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mInterruptDownload = new JButton(getResourceString("offroad.interruptDownload"));
		mInterruptDownload.addActionListener(pE1 -> mIsDownloadInterrupted = true);
		mInterruptDownload.setEnabled(false);
		contentPane.add(mInterruptDownload, new GridBagConstraints(1, y, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		mDeleteButton = new JButton(getResourceString("offroad.deleteButton"));
		mDeleteButton.addActionListener(e -> doDelete());
		mDeleteButton.setEnabled(false);
		contentPane.add(mDeleteButton, new GridBagConstraints(2, y++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		IndexFileList indexesList = DownloadOsmandIndexesHelper.getIndexesList(mContext);
		mDownloadResources = new DownloadResources(mContext);
		mDownloadResources.updateLoadedFiles();
		for (IndexItem item : indexesList.getIndexFiles()) {
			if (item.getType() == DownloadActivityType.NORMAL_FILE || item.getType() == DownloadActivityType.ROADS_FILE
					|| item.getType() == DownloadActivityType.WIKIPEDIA_FILE || item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
				mSourceModel.addRow(item);
			}
		}
		if(mInitialFilter != null){
			mTextField.setText(mInitialFilter);
			// bad hack to select the first element after the filter has applied.
			new Timer().schedule(new TimerTask() {
				public void run() {
					if (mTable.getRowCount() > 0) {
						mTable.setRowSelectionInterval(0, 0);
					}
				}
			}, FilterTextDocumentListener.TYPE_DELAY_TIME*2);
		}
		mDialog.pack();
		decorateDialog();
		removeWaitingCursor();
		mDialog.setVisible(true);

	}

	public void download(List<IndexItem> pList) {
		//check download thread:
		if(mDownloadThread != null && mDownloadThread.isAlive()){
			JOptionPane.showMessageDialog(mDialog, getResourceString("offroad.downloadAlreadyStarted"), getResourceString("offroad.downloadAlreadyStartedTitle"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(pList.size() > MAX_NUMBER_OF_FILES){
			JOptionPane.showMessageDialog(mDialog, getResourceString("offroad.downloadTooManyFiles"), getResourceString("offroad.downloadTooManyFilesTitle"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		mIsDownloadInterrupted = false;
		mInterruptDownload.setEnabled(true);
		final DownloadFileHelper helper = new DownloadFileHelper(mContext);
		final Vector<File> toReIndex = new Vector<>();
		mDownloadThread = new Thread(() -> {
			for (final IndexItem item : pList) {
				System.out.println("Starting download for " + item);
				final IProgress progress = new IProgress() {

					@Override
					public void startTask(final String pTaskName, final int pWork) {
						SwingUtilities.invokeLater(() -> {
							mProgressStatus.setText(pTaskName);
							mProgressBar.setMaximum(pWork);
						});
					}

					@Override
					public void startWork(int pWork) {
						SwingUtilities.invokeLater(() -> mProgressBar.setValue(0));
					}

					@Override
					public void progress(int pDeltaWork) {
					}

					@Override
					public void remaining(int pRemainingWork) {
						SwingUtilities.invokeLater(() -> mProgressBar.setValue(mProgressBar.getMaximum() - pRemainingWork));
					}

					@Override
					public void finishTask() {
						SwingUtilities.invokeLater(() -> {
							mProgressBar.setValue(mProgressBar.getMaximum());
							if (mIsDownloadInterrupted) {
								mProgressStatus.setText(getResourceString("offroad.downloadInterrupted"));
							} else {
								mProgressStatus.setText(getResourceString("offroad.downloadDone"));
							}
						});
					}

					@Override
					public boolean isIndeterminate() {
						return false;
					}

					@Override
					public boolean isInterrupted() {
						return mIsDownloadInterrupted;
					}
				};
				try {
					helper.downloadFile(item.createDownloadEntry(mContext), progress, toReIndex,
							pWarning -> System.err.println("DOWNLOAD WARNING: " + pWarning), false);
					String result = reindexFiles(toReIndex);
					SwingUtilities.invokeLater(() -> mProgressStatus.setText(result));
					mDownloadResources.updateLoadedFiles();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SwingUtilities.invokeLater(() -> mInterruptDownload.setEnabled(false));
		});
		mDownloadThread.start();
	}

    private String reindexFiles(List<File> filesToReindex) {

	    boolean vectorMapsToReindex = false;
	    // reindex vector maps all at one time
	    ResourceManager manager = mContext.getResourceManager();
	    for (File f : filesToReindex) {
	            if (f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
	                    vectorMapsToReindex = true;
	            }
	    }
	    List<String> warnings = new ArrayList<>();
	    manager.indexVoiceFiles(IProgress.EMPTY_PROGRESS);
	    if (vectorMapsToReindex) {
	            warnings = manager.indexingMaps(IProgress.EMPTY_PROGRESS);
	    }
	    List<String> wns = manager.indexAdditionalMaps(IProgress.EMPTY_PROGRESS);
	    if (wns != null) {
	            warnings.addAll(wns);
	    }
	
	    if (!warnings.isEmpty()) {
	            return warnings.get(0);
	    }
	    return null;
	}

	
	public void selectFirstElementIfNecessary() {
		if (mTable.getSelectedRowCount() == 0 && mTable.getRowCount() > 0) {
			mTable.setRowSelectionInterval(0, 0);
		}
	}

	public void requestFocusToTextField() {
		mTextField.selectAll();
		mTextField.requestFocus();
	}


	public void doDownload() {
		download(mSourceModel.getSelectedRows());
	}

	public void doDelete() {
		for (final IndexItem item : mSourceModel.getSelectedRows()) {
			mContext.getResourceManager().closeFile(item.getTargetFileName());
			item.getTargetFile(mContext).delete();
		}
		mDownloadResources.updateLoadedFiles();
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
						List<IndexItem> selectedValuesList = mSourceModel.getSelectedRows();
						Document document = event.getDocument();
						final String text = getText(document);
						RowFilter<DownloadTableModel, Object> rf = null;
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
