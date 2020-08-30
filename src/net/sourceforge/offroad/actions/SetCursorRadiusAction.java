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

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 31.05.2016
 */
public class SetCursorRadiusAction extends OffRoadAction {

	private double mRadius;

	public SetCursorRadiusAction(OsmWindow pContext, String pName, double pRadius) {
		super(pContext, pContext.getOffRoadString(pName), null);
		mRadius = pRadius;
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		LatLon cursorPosition = mContext.getCursorPosition();
		if(mRadius == 0d){
			mContext.getDrawPanel().setCursorRadiusEnabled(false);
		} else {
			mContext.getDrawPanel().setCursorRadiusEnabled(true);
			LatLon destLatLon = mContext.getMouseLocation();
			double distance = mRadius;
			if(mRadius == -1d){
				distance = MapUtils.getDistance(cursorPosition, destLatLon);
			}
			mContext.getDrawPanel().setCursorRadiusSizeInMeters(distance);
		}
		mContext.getDrawPanel().drawLater();
	}

}
