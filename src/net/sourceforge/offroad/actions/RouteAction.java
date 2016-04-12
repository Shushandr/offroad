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

package net.sourceforge.offroad.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.Action;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.sourceforge.offroad.OsmBitmapPanel;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 12.04.2016
 */
public class RouteAction extends OffRoadAction {

	public RouteAction(OsmWindow pCtx) {
		super(pCtx);
		this.putValue(Action.NAME, "route");
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		// get destination point:
		Point destination = mContext.getLastMouseEvent().getPoint();
		OsmBitmapPanel drawPanel = mContext.getDrawPanel();
		LatLon destLatLon = drawPanel.getTileBox().getLatLonFromPixel(destination.x, destination.y);
		LatLon start = drawPanel.getCursorPosition();
		Location startLocation = new Location("");
		startLocation.setLatitude(start.getLatitude());
		startLocation.setLongitude(start.getLongitude());
		System.out.println("Routing from " + startLocation + " to " + destLatLon);
		mContext.getRoutingHelper().setFinalAndCurrentLocation(destLatLon, new ArrayList<LatLon>(), startLocation);
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.offroad.actions.OffRoadAction#save()
	 */
	@Override
	public void save() {

	}

}
