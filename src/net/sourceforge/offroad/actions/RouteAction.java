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

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.Action;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.routing.RoutingHelper.RouteCalculationProgressCallback;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 12.04.2016
 */
public class RouteAction extends OffRoadAction implements RouteCalculationProgressCallback {

	private ApplicationMode mMode;

	public RouteAction(OsmWindow pCtx, ApplicationMode pMode) {
		super(pCtx);
		mMode = pMode;
		this.putValue(Action.NAME, mContext.getOffRoadString("offroad.route", new Object[]{pMode.getStringKey()}));
		mContext.getRoutingHelper().setProgressBar(this);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		// get destination point:
		LatLon destLatLon = mContext.getMouseLocation();
		LatLon start =  mContext.getCursorPosition();
		Location startLocation = new Location("");
		startLocation.setLatitude(start.getLatitude());
		startLocation.setLongitude(start.getLongitude());
		System.out.println("Routing from " + startLocation + " to " + destLatLon);
		mContext.getRoutingHelper().setAppMode(mMode);
		mContext.getRoutingHelper().setFinalAndCurrentLocation(destLatLon, new ArrayList<LatLon>(), startLocation);
	}


	/* (non-Javadoc)
	 * @see net.sourceforge.offroad.actions.OffRoadAction#save()
	 */
	@Override
	public void save() {

	}

	@Override
	public void updateProgress(int pProgress) {
		mContext.setProgress(pProgress);
		mContext.setStatus("");
	}

	@Override
	public void finish() {
		mContext.setProgress(100);
		float dist = mContext.getRoutingHelper().getRoute().getWholeDistance()/1000f;
		mContext.setStatus(mContext.getOffRoadString("offroad.routing_finished", new Object[]{dist}));
	}

	@Override
	public void showError(String pMessage) {
		mContext.showToastMessage(pMessage);
	}

}
