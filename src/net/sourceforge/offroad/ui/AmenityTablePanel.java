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

package net.sourceforge.offroad.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.plus.poi.PoiUIFilter;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;

/**
 * @author foltin
 * @date 23.04.2016
 */
public class AmenityTablePanel extends JPanel {

	private final static Log log = PlatformUtil.getLog(AmenityTablePanel.class);

	public static class ImageHolder {
		public BufferedImage image;
		public MapObject item;
		public ImageHolder(BufferedImage pImage, MapObject pItem) {
			super();
			image = pImage;
			item = pItem;
		}
		String getTypeTranslation(){
			if (item instanceof Amenity) {
				Amenity amenity = (Amenity) item;
				return amenity.getType().getTranslation();
			}
			return null;
		}
		String getPoiIconKeyName(){
			if (item instanceof Amenity) {
				Amenity amenity = (Amenity) item;
				return amenity.getType().getPoiTypeByKeyName(amenity.getSubType()).getIconKeyName();
			}
			return null;
		}
		
	}
	
	public static class ImageTableCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof ImageHolder) {
				ImageHolder holder = (ImageHolder) value;
				BufferedImage img = holder.image;
				if(img != null) {
					setIcon(new ImageIcon(img));
					setSize(new Dimension(img.getWidth(), img.getHeight()));
				} else {
					setIcon(new BlindIcon(20));
					setSize(new Dimension(20, 20));
				}
				setText("");
				setToolTipText(holder.getTypeTranslation());
			}
			return this;
		}

	}

	public static final MessageFormat formatDistance = new MessageFormat("{0, number,##.#} km", Locale.US);
	private static final int ICON_COLUMN_INDEX = 0;

	public class DistanceTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value == null) {
				setText(mContext.getString(R.string.search_position_undefined));
			} else if (value instanceof Double) {
				Double d = (Double) value;
				setText(formatDistance.format(new Object[] {d}));
			}
			return this;
		}
	}

	interface AmenityToColumn {
		Object get(MapObject pItem);
	}

	public static class AmenityTableColumn {
		private AmenityToColumn mMapping;
		String mName;
		Class<?> mClass;
		private TableCellRenderer mRenderer;

		public AmenityTableColumn(String pName, Class<?> pClass, AmenityToColumn pMapping, TableCellRenderer pRenderer) {
			super();
			mName = pName;
			mClass = pClass;
			mMapping = pMapping;
			mRenderer = pRenderer;
		}
	}

	public class AmenityTableModel extends AbstractTableModel {
		private Vector<AmenityTableColumn> mColumns = new Vector<>();
		private Vector<MapObject> mRows = new Vector<>();

		public AmenityTableModel() {
			mColumns.addElement(new AmenityTableColumn("icon", ImageHolder.class, item -> new ImageHolder(mContext.getBitmap(item), item),
					new ImageTableCellRenderer()));
			mColumns.addElement(new AmenityTableColumn("name", String.class,
					item -> item.getName(mContext.getLanguage()), new DefaultTableCellRenderer()));
			mColumns.addElement(new AmenityTableColumn("distance", Double.class,
					item -> item.getDistance(mContext.getCursorPosition()) / 1000d,
					new DistanceTableCellRenderer()));
			for (AmenityTableColumn column : mColumns) {
				mTable.setDefaultRenderer(column.mClass, column.mRenderer);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return mColumns.get(columnIndex).mClass;
		}

		@Override
		public String getColumnName(int pColumn) {
			return mContext.getOffRoadString("offroad.amenity." + mColumns.get(pColumn).mName);
		}

		public void addRow(MapObject pItem) {
			mRows.add(pItem);
		}

		public MapObject getItemAt(int pJ) {
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
			AmenityTableColumn column = mColumns.get(pColumnIndex);
			Object result = column.mMapping.get(getItemAt(pRowIndex));
			// log.info("Object at " + pRowIndex + "," + pColumnIndex + "=" +
			// result);
			return result;
		}

		public MapObject getSelectedRow() {
			int sel = mTable.getSelectedRow();
			if (sel < 0) {
				return null;
			}
			return getItemAt(mTable.convertRowIndexToModel(sel));
		}

		public List<MapObject> getSelectedRows() {
			int[] selectedRows = mTable.getSelectedRows();
			Vector<MapObject> res = new Vector<>();
			for (int selectedRow : selectedRows) {
				int j = mTable.convertRowIndexToModel(selectedRow);
				res.add(getItemAt(j));
			}
			return res;
		}

		public void clear() {
			mRows.clear();
		}
		
		public int getDistanceColumn() {
			for (int i = 0; i < mColumns.size(); i++) {
				if(mColumns.get(i).mClass == Double.class){
					return i;
				}
			}
			return -1;
		}
		
	}

	private OsmWindow mContext;
	private JTable mTable;
	private AmenityTableModel mSourceModel;
	private TableRowSorter<AmenityTableModel> mSorter;
	private KeyAdapter mKeyListener;
	private MouseAdapter mMouseListener;
	private PoiUIFilter mFilter;

	public AmenityTablePanel(OsmWindow pContext) {
		mContext = pContext;
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		setLayout(gbl);
		int y = 0;
		mTable = new JTable();
		// FIXME: Size dependent
		// mTable.setRowHeight(UIManager.getFont("Table.font").getSize());
		mSourceModel = new AmenityTableModel();
		mTable.setModel(mSourceModel);
		mTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		mSorter = new TableRowSorter<>(mSourceModel);
		// FIXME: The knowledge of the index is not preferable:
		mSorter.setComparator(ICON_COLUMN_INDEX, (Comparator<ImageHolder>) (pO1, pO2) -> {
			try {
				return pO1.getPoiIconKeyName().compareTo(pO2.getPoiIconKeyName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		});

		mTable.setRowSorter(mSorter);

		mKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent pE) {
				if (pE.getKeyCode() == KeyEvent.VK_ENTER) {
					pE.consume();
					select(mSourceModel.getSelectedRow());
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
					select(mSourceModel.getSelectedRow());
				}
			}
		};
		mTable.addMouseListener(mMouseListener);
		add(new JScrollPane(mTable), new GridBagConstraints(0, y++, 2, 1, 1.0, 4.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}

	protected void select(MapObject pSelectedRow) {
		if(pSelectedRow == null){
			return;
		}
		LatLon loc = pSelectedRow.getLocation();
		mContext.move(loc, null);
	}

	public void setSearchResult(List<MapObject> pResult) {
		mSourceModel.clear();
		for (MapObject am : pResult) {
			mSourceModel.addRow(am);
		}
		mSourceModel.fireTableDataChanged();
		updateRowHeights();
	}

	private void recalculateDistances() {
		// recalculate distances.
		int distanceColumn = mSourceModel.getDistanceColumn();
		for (int i = 0; i < mSourceModel.getRowCount(); i++) {
			mSourceModel.fireTableCellUpdated(i, distanceColumn);
		}
	}

	private void updateRowHeights() {
		for (int row = 0; row < mTable.getRowCount(); row++) {
			int rowHeight = mTable.getRowHeight();

			for (int column = 0; column < mTable.getColumnCount(); column++) {
				Component comp = mTable.prepareRenderer(mTable.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
			}
			mTable.setRowHeight(row, rowHeight);
		}
	}

}
