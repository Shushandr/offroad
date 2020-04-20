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

import net.osmand.plus.render.RenderingIcons;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

/**
 * @author foltin
 * @date 30.06.2016
 */
public class ChangeIconSizeAction extends OffRoadAction implements SelectableAction {

	private String mIconsDirPrefix;

	public ChangeIconSizeAction(OsmWindow pContext, String pIconsDirPrefix) {
		super(pContext, pContext.getOffRoadString("offroad.icon_prefix_" + pIconsDirPrefix), null);
		mIconsDirPrefix = pIconsDirPrefix;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getOffroadProperties().setProperty(OsmWindow.OSMAND_ICONS_DIR_PREFIX, mIconsDirPrefix);
		RenderingIcons.initIcons();
		mContext.getDrawPanel().flushCacheAndDrawLater();
	}

	@Override
	public boolean isSelected() {
		return mIconsDirPrefix.equals(mContext.getOffroadProperties().getProperty(OsmWindow.OSMAND_ICONS_DIR_PREFIX,
				OsmWindow.OSMAND_ICONS_DIR_DEFAULT_PREFIX));
	}

}
