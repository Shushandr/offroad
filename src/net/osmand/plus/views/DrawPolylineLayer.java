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

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.DirectOffroadLayer;
import net.sourceforge.offroad.ui.IContextMenuProvider;
import net.sourceforge.offroad.ui.OsmBitmapPanel;
import net.sourceforge.offroad.ui.Paint;

/**
 * @author foltin
 * @date 08.05.2017
 */
public class DrawPolylineLayer extends OsmandMapLayer implements DirectOffroadLayer, IContextMenuProvider {

	private OsmBitmapPanel mDrawPanel;
	private Paint area;
	private Vector<LatLon> mPolyline = new Vector<>();
	
	public Vector<LatLon> getSelectedPolyline(){
		return mPolyline;
	}

	public DrawPolylineLayer(OsmBitmapPanel pOsmBitmapPanel) {
		mDrawPanel = pOsmBitmapPanel;
	}

	@Override
	public void initLayer(OsmBitmapPanel pView) {
		area = new Paint();
		area.setColor(mDrawPanel.getResources().getColor(R.color.region_downloading));

	}

	@Override
	public void onDraw(Graphics2D pCanvas, RotatedTileBox pTileBox, DrawSettings pSettings) {
		Vector<LatLon> polyline = getSelectedPolyline();
		if(polyline.isEmpty()){
			return;
		}
		area.updateGraphics(pCanvas);
		for (int i = 0; i < polyline.size(); i++) {
			LatLon point = polyline.get(i);
			if(i<polyline.size()-1){
				LatLon nextPoint = polyline.get(i+1);
				int locationX1 = (int) pTileBox.getPixXFromLatLon(point);
				int locationY1 = (int) pTileBox.getPixYFromLatLon(point);
				int locationX2 = (int) pTileBox.getPixXFromLatLon(nextPoint);
				int locationY2 = (int) pTileBox.getPixYFromLatLon(nextPoint);
				pCanvas.drawLine(locationX1, locationY1, locationX2, locationY2);
			}
		}
	}

	@Override
	public void destroyLayer() {
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
	}

	@Override
	public LatLon getObjectLocation(Object pO) {
		return null;
	}
	public void addPolylinePoint(Point pPoint) {
		if(mPolyline.isEmpty()){
			LatLon cursorPosition = mDrawPanel.getCursorPosition();
			if(cursorPosition == null){
				return;
			}
			mPolyline.add(cursorPosition);
		}
		mPolyline.add(mDrawPanel.getLatLon(pPoint));
	}


}
