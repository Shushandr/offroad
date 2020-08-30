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

import javax.swing.Action;

import net.osmand.data.LatLon;
import net.osmand.plus.TargetPointsHelper;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 12.04.2016
 */
public class PointNavigationAction extends OffRoadAction {

	private HelperAction mAction;


	public interface HelperAction {
		public void act(TargetPointsHelper helper, LatLon pPosition);
	}
	
	public PointNavigationAction(OsmWindow pCtx, String pName, HelperAction pAction) {
		super(pCtx);
		mAction = pAction;
		this.putValue(Action.NAME, mContext.getOffRoadString(pName));
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		// get destination point:
		LatLon destLatLon = mContext.getMouseLocation();
		TargetPointsHelper helper = mContext.getTargetPointsHelper();
		mAction.act(helper, destLatLon);
		// remove route as well
		mContext.getRoutingHelper().clearCurrentRoute(null, null);
		mContext.getDrawPanel().drawLater();
	}


	/* (non-Javadoc)
	 * @see net.sourceforge.offroad.actions.OffRoadAction#save()
	 */
	@Override
	public void save() {

	}

}
