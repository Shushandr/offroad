/** 
   OffRoad
   Copyright (C) 2017 Christian Foltin

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

package net.osmand.plus.views;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.plus.views.DrawPolylineLayer.PolylineDistance;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.DirectOffroadLayer;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.ISelectionInterface;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.Paint;

/**
 * @author foltin
 * @date 08.05.2017
 */
/**
 * @author foltin
 * @date 03.10.2017
 */
public class DrawPolylineLayer extends OsmandMapLayer
		implements ISelectionInterface, DirectOffroadLayer, IContextMenuProvider {

	private static class PolylinePointDragInformation implements IDragInformation {
		public Polyline mPolyline;
		public int mIndex;
	}

	private static final int SELECTION_RADIUS = 20;

	public static class EdgeDistance {
		double distance;
		Polyline mPolyline;
		int index;

		public EdgeDistance(double pDistance, Polyline pPolyline, int pIndex, Point pPointP) {
			super();
			distance = pDistance;
			mPolyline = pPolyline;
			index = pIndex;
			mPointP = pPointP;
		}

		@Override
		public String toString() {
			return "EdgeDistance [distance=" + distance + ", mPolyline=" + mPolyline + ", index=" + index + ", mPointP="
					+ mPointP + "]";
		}

		private Point mPointP;
	}

	public static class PolylineDistance {
		double distance;
		Polyline mPolyline;
		int mIndexStartingSegment;
		int mIndexEndingSegment;
		public PolylineDistance(double pDistance, Polyline pPolyline, int pIndexStartingSegment,
				int pIndexEndingSegment) {
			super();
			distance = pDistance;
			mPolyline = pPolyline;
			mIndexStartingSegment = pIndexStartingSegment;
			mIndexEndingSegment = pIndexEndingSegment;
		}
		@Override
		public String toString() {
			return "PolylineDistance [distance=" + distance + ", mPolyline=" + mPolyline + ", mIndexStartingSegment="
					+ mIndexStartingSegment + ", mIndexEndingSegment=" + mIndexEndingSegment + "]";
		}
	}
	
	private final static org.apache.commons.logging.Log log = PlatformUtil.getLog(DrawPolylineLayer.class);
	private static final String POLYLINES_STORAGE = "POLYLINES_STORAGE";

	
	@XmlRootElement
	public static class Polylines  {
		private Vector<Polyline> mPolylineItems = new Vector<>();
		public boolean contains(Object pO) {
			return mPolylineItems.contains(pO);
		}
		public int indexOf(Object pO) {
			return mPolylineItems.indexOf(pO);
		}
		public boolean remove(Object pO) {
			return mPolylineItems.remove(pO);
		}
		public int size() {
			return mPolylineItems.size();
		}
		public boolean isEmpty() {
			return mPolylineItems.isEmpty();
		}
		public Polyline get(int pIndex) {
			return mPolylineItems.get(pIndex);
		}
		public boolean add(Polyline pE) {
			return mPolylineItems.add(pE);
		}
		@XmlElement(name="polyline")
		public Vector<Polyline> getPolylineItems() {
			return mPolylineItems;
		}
		public void setPolylineItems(Vector<Polyline> pItems) {
			mPolylineItems = pItems;
		}
		public Polylines() {
		}
		
		
	}
	
	@XmlRootElement
	public static class Polyline {
		private Vector<LatLon> mCoordinates = new Vector<>();
		public LatLon set(int pIndex, LatLon pElement) {
			return mCoordinates.set(pIndex, pElement);
		}
		public boolean contains(Object pO) {
			return mCoordinates.contains(pO);
		}
		public int indexOf(Object pO) {
			return mCoordinates.indexOf(pO);
		}
		public boolean remove(Object pO) {
			return mCoordinates.remove(pO);
		}
		public int size() {
			return mCoordinates.size();
		}
		public boolean isEmpty() {
			return mCoordinates.isEmpty();
		}
		public LatLon get(int pIndex) {
			return mCoordinates.get(pIndex);
		}
		public boolean add(LatLon pE) {
			return mCoordinates.add(pE);
		}
		@XmlElement(name="latlon")
		public Vector<LatLon> getCoordinates() {
			return mCoordinates;
		}
		public void setCoordinates(Vector<LatLon> pItems) {
			mCoordinates = pItems;
		}
		
		public Polyline() {
		}
		public EdgeDistance getDistanceToEdges(LatLon pDest, OsmBitmapPanel pDrawPanel) {
			return getDistanceToEdges(pDrawPanel.getPoint(pDest), pDrawPanel);
		}
		public EdgeDistance getDistanceToEdges(Point pDest, OsmBitmapPanel pDrawPanel) {
			double minDist = Double.MAX_VALUE;
			Point minPointP = null;
			int index = 0;
			int minIndex = -1;
			for (LatLon latLonP : this.getCoordinates()) {
				Point pointP = pDrawPanel.getPoint(latLonP);
				double dist = pDest.distance(pointP);
				if (dist < minDist) {
					minIndex = index;
					minDist = dist;
					minPointP = pointP;
				}
				index++;
			}
			EdgeDistance ret = new EdgeDistance(minDist, this, minIndex, minPointP);
			return ret;
		}

		public double getDistance(Point pDest, OsmBitmapPanel pDrawPanel) {
			PolylineDistance distanceInfo = getDistanceInformation(pDest, pDrawPanel);
			if(distanceInfo == null) {
				return Double.MAX_VALUE;
			}
			return distanceInfo.distance;
		}
		public PolylineDistance getDistanceInformation(Point pDest, OsmBitmapPanel pDrawPanel) {
			double minDist = Double.MAX_VALUE;
			Point lastPointInLine = null;
			int indexAfterSegment = 0;
			int minDistanceIndex = -1;
			for (LatLon latLonP : this.getCoordinates()) {
				Point pointP = pDrawPanel.getPoint(latLonP);
				if (lastPointInLine != null) {
					double dist = getDistance(pDest, pointP, lastPointInLine);
					if(dist < minDist) {
						minDistanceIndex = indexAfterSegment;
					}
					minDist = Math.min(dist, minDist);
				}
				lastPointInLine = pointP;
				indexAfterSegment++;
			}
			if(minDistanceIndex < 0) {
				return null;
			}
			return new PolylineDistance(minDist, this, minDistanceIndex-1, minDistanceIndex);
		}

		public double getDistance(Point pDest, Point pointA, Point pointB) {
			// adapted from
			// http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
			float x1 = pointA.x;
			float y1 = pointA.y;
			float x2 = pointB.x;
			float y2 = pointB.y;
			float x = pDest.x;
			float y = pDest.y;
			float A = x - x1;
			float B = y - y1;
			float C = x2 - x1;
			float D = y2 - y1;

			float dot = A * C + B * D;
			float len_sq = C * C + D * D;
			float param = -1;
			// in case of 0 length line
			if (len_sq != 0) {
				param = dot / len_sq;
			}

			float xx, yy;

			if (param < 0) {
				xx = x1;
				yy = y1;
			} else if (param > 1) {
				xx = x2;
				yy = y2;
			} else {
				xx = x1 + param * C;
				yy = y1 + param * D;
			}

			float dx = x - xx;
			float dy = y - yy;
			return Math.sqrt(dx * dx + dy * dy);
		}


		public double calculateArea() {
			Vector<Node> v = new Vector<>();
			for (LatLon latLon : this.getCoordinates()) {
				v.add(new Node(latLon.getLatitude(), latLon.getLongitude(), 1));
			}
			return OsmMapUtils.getArea(v);
		}

		/**
		 * @return the length of the polyline in kilometers
		 */
		public float calculateLength() {
			float polyDist = 0f;
			for (int i = 0; i < size(); i++) {
				LatLon pos = get(i);
				if (i + 1 < size()) {
					LatLon pos2 = get(i + 1);
					polyDist += MapUtils.getDistance(pos, pos2);
				}
			}
			polyDist /= 1000d;
			return polyDist;
		}
		public void insert(LatLon pLatLon, int pIndexToInsert) {
			mCoordinates.insertElementAt(pLatLon, pIndexToInsert);
		}
	}

	private OsmBitmapPanel mDrawPanel;
	private Paint area;
	private Paint areaSelected;
	private Polylines mPolylines = new Polylines();
	private int mSelectedIndex = -1;

	public Polyline getSelectedPolyline() {
		if (checkIndex()) {
			return mPolylines.get(mSelectedIndex);
		}
		return null;
	}

	public boolean checkIndex() {
		return mSelectedIndex >= 0 && mSelectedIndex < mPolylines.size();
	}

	public DrawPolylineLayer(OsmBitmapPanel pOsmBitmapPanel) {
		mDrawPanel = pOsmBitmapPanel;
	}

	@Override
	public void initLayer(OsmBitmapPanel pView) {
		area = new Paint();
		area.setColor(mDrawPanel.getResources().getColor(R.color.region_downloading));
		area.setStrokeWidth(2.0f * mDrawPanel.getScaleCoefficient());
		areaSelected = new Paint();
		areaSelected.setColor(mDrawPanel.getResources().getColor(R.color.region_downloading));
		areaSelected.setStrokeWidth(4.0f * mDrawPanel.getScaleCoefficient());
		// get storage:
		String storageResult = (String) pView.getContext().getOffroadProperties().get(POLYLINES_STORAGE);
		if(storageResult != null){
			Object storage = OsmWindow.unmarshall(storageResult, getStorageClasses());
			if (storage instanceof Polylines) {
				Polylines polylines = (Polylines) storage;
				mPolylines = polylines;
			}
		}
	}

	@Override
	public void onDraw(Graphics2D pCanvas, RotatedTileBox pTileBox, DrawSettings pSettings) {
		Polyline selPolyline = getSelectedPolyline();
		for (Polyline polyline : mPolylines.getPolylineItems()) {
			if (selPolyline == polyline) {
				areaSelected.updateGraphics(pCanvas);
			} else {
				area.updateGraphics(pCanvas);
			}
			for (int i = 0; i < polyline.size(); i++) {
				LatLon point = polyline.get(i);
				int locationX1 = (int) pTileBox.getPixXFromLatLon(point);
				int locationY1 = (int) pTileBox.getPixYFromLatLon(point);
				if (i < polyline.size() - 1) {
					LatLon nextPoint = polyline.get(i + 1);
					int locationX2 = (int) pTileBox.getPixXFromLatLon(nextPoint);
					int locationY2 = (int) pTileBox.getPixYFromLatLon(nextPoint);
					pCanvas.drawLine(locationX1, locationY1, locationX2, locationY2);
				}
				if (selPolyline == polyline) {
					// draw little circles around the edges, scale with factor
					int circleRadius = (int) (8f * mDrawPanel.getScaleCoefficient());
					pCanvas.fillOval(locationX1 - circleRadius, locationY1 - circleRadius, 2 * circleRadius,
							2 * circleRadius);
				}
			}
		}
	}

	@Override
	public void destroyLayer() {
		// save information
		String res = OsmWindow.marshall(mPolylines, getStorageClasses());
		log.info("Storage: " + res);
		OsmWindow.getInstance().getOffroadProperties().put(POLYLINES_STORAGE, res);
	}

	public Class[] getStorageClasses() {
		return new Class[]{Polyline.class, LatLon.class, Polylines.class};
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public String getObjectDescription(Object pO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PointDescription getObjectName(Object pO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isObjectClickable(Object pO) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void collectObjectsFromPoint(Point2D pPoint, RotatedTileBox pTileBox, List<Object> pRes) {
		Point pDest = convertToPoint(pPoint);
		for (Polyline polyline : mPolylines.getPolylineItems()) {
			if (polyline.getDistance(pDest, mDrawPanel) < SELECTION_RADIUS) {
				pRes.add(polyline);
			}
		}

	}

	public Point convertToPoint(Point2D pPoint) {
		return new Point((int) pPoint.getX(), (int) pPoint.getY());
	}

	@Override
	public LatLon getObjectLocation(Object pO) {
		return null;
	}

	public void addPolylinePoint(Point pPoint) {
		Polyline polyline = getSelectedPolyline();
		// nothing selected? then add a new line
		if (polyline == null) {
			polyline = new Polyline();
			mPolylines.add(polyline);
			mSelectedIndex = mPolylines.size() - 1;
		}
		if (polyline.isEmpty()) {
			LatLon cursorPosition = mDrawPanel.getCursorPosition();
			if (cursorPosition == null) {
				return;
			}
			polyline.add(cursorPosition);
		}
		polyline.add(mDrawPanel.getLatLon(pPoint));
	}

	public void endPolyline() {
		mSelectedIndex = -1;
	}

	@Override
	public boolean isSelection(Point pDest) {
		for (Polyline polyline : mPolylines.getPolylineItems()) {
			if (polyline.getDistance(pDest, mDrawPanel) < SELECTION_RADIUS) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setSelection(Point pDest) {
		int i = 0;
		for (Polyline polyline : mPolylines.getPolylineItems()) {
			if (polyline.getDistance(pDest, mDrawPanel) < SELECTION_RADIUS) {
				mSelectedIndex = i;
				return;
			}
			i++;
		}
	}

	@Override
	public IDragInformation isDragPoint(Point pLastDragPoint, Point pPoint) {
		if (!checkIndex())
			return null;
		EdgeDistance distanceToEdges = mPolylines.get(mSelectedIndex).getDistanceToEdges(pPoint, mDrawPanel);
		if (distanceToEdges.distance < SELECTION_RADIUS) {
			PolylinePointDragInformation info = new PolylinePointDragInformation();
			info.mPolyline = distanceToEdges.mPolyline;
			info.mIndex = distanceToEdges.index;
			return info;
		}
		return null;
	}

	@Override
	public void drag(Point pNewPoint, IDragInformation pInfo) {
		if (!checkIndex())
			return;
		if (pInfo instanceof PolylinePointDragInformation) {
			PolylinePointDragInformation polyInfo = (PolylinePointDragInformation) pInfo;
			int indexOf = polyInfo.mIndex;
			Polyline polyline = polyInfo.mPolyline;
			if (indexOf < polyline.size()) {
				polyline.set(indexOf, mDrawPanel.getLatLon(pNewPoint));
			}
		}
	}

	public void remove(Polyline pPolyline) {
		Polyline sel = null;
		if (checkIndex()) {
			sel = mPolylines.get(mSelectedIndex);
		}
		mPolylines.remove(pPolyline);
		if (sel != null && mPolylines.contains(sel)) {
			mSelectedIndex = mPolylines.indexOf(sel);
		} else {
			mSelectedIndex = -1;
		}
	}

	/**Adds a new (inner) point to an existing polyline.
	 * @param pPolyline
	 * @param pLatLon
	 */
	public void insertPolylinePoint(Polyline pPolyline, LatLon pLatLon) {
		if (!checkIndex())
			return;
		Polyline selectedPolyline = mPolylines.get(mSelectedIndex);
		PolylineDistance distanceToPolyline = selectedPolyline.getDistanceInformation(mDrawPanel.getPoint(pLatLon), mDrawPanel);
		int ind = 1;
		if(distanceToPolyline != null) {
			ind = distanceToPolyline.mIndexEndingSegment;
		}
		selectedPolyline.insert(pLatLon, ind);
	}

	public void removePolylinePoint(Polyline pPolyline, LatLon pLatLon) {
		if (!checkIndex())
			return;
		EdgeDistance distanceToEdges = mPolylines.get(mSelectedIndex).getDistanceToEdges(pLatLon, mDrawPanel);
		if (distanceToEdges.distance < SELECTION_RADIUS) {
			Polyline poly = distanceToEdges.mPolyline;
			poly.remove(poly.get(distanceToEdges.index));
		}
	}

}
