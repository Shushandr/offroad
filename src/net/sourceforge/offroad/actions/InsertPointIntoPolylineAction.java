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

package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.data.LatLon;
import net.osmand.plus.views.DrawPolylineLayer.Polyline;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 19.05.2017
 */
public class InsertPointIntoPolylineAction extends OffRoadAction {

	private Polyline mPolyline;
	private LatLon mLatLon;

	public InsertPointIntoPolylineAction(OsmWindow pContext, Polyline pPolyline, LatLon pLatLon) {
		super(pContext, pContext.getOffRoadString("offroad.insert_point_into_polyline"), null);
		mPolyline = pPolyline;
		mLatLon = pLatLon;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getDrawPanel().getPolylineLayer().insertPolylinePoint(mPolyline, mLatLon);
		mContext.getDrawPanel().repaint();
	}

}
