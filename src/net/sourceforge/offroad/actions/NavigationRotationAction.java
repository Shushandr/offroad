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

import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.OsmBitmapPanel;

/**
 * @author foltin
 * @date 17.04.2016
 */
public class NavigationRotationAction extends OffRoadAction {

	private double mIncrement;
	private boolean mIsAbsolute = false;

	public NavigationRotationAction(OsmWindow pContext) {
		super(pContext);
	}

	public NavigationRotationAction setIncrement(double pIncrement){
		mIncrement = pIncrement;
		mIsAbsolute = false;
		return this;
	}
	
	public NavigationRotationAction setAbsolute(double pNewAngle){
		mIsAbsolute = true;
		mIncrement = pNewAngle;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		OsmBitmapPanel drawPanel = mContext.getDrawPanel();
		double incr = mIncrement;
		if(mIsAbsolute){
			incr -= mContext.getDrawPanel().copyCurrentTileBox().getRotate();
		}
		drawPanel.directRotateIncrement(incr);
		drawPanel.rotateIncrement(incr);
	}

	/* (non-Javadoc)
	 * @see net.sourceforge.offroad.actions.OffRoadAction#save()
	 */
	@Override
	public void save() {
	}

}
