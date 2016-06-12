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

import net.osmand.plus.ApplicationMode;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

/**
 * @author foltin
 * @date 12.06.2016
 */
public class ChooseApplicationModeAction extends OffRoadAction implements SelectableAction {

	private ApplicationMode mApplicationMode;

	public ChooseApplicationModeAction(OsmWindow pContext, ApplicationMode pApplicationMode) {
		super(pContext, pContext.getString(pApplicationMode.getStringResource()), null);
		mApplicationMode = pApplicationMode;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getSettings().APPLICATION_MODE.set(mApplicationMode);
		mContext.getDrawPanel().drawLater();
	}
	
	
	@Override
	public boolean isSelected() {
		return mApplicationMode.equals(mContext.getSettings().APPLICATION_MODE.get());
	}

}
