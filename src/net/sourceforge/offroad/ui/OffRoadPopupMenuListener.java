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

package net.sourceforge.offroad.ui;

import java.util.List;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapLayer;
import net.sourceforge.offroad.OsmWindow;

/** Organizes all context clicks on layers. 
 * 
 * @author foltin
 * @date 16.06.2016
 */
public class OffRoadPopupMenuListener implements PopupMenuListener {

	private JPopupMenu mMenu;
	private Vector<JMenuItem> items = new Vector<>();
	private OsmWindow mContext;

	public OffRoadPopupMenuListener(OsmWindow pContext, JPopupMenu pMenu) {
		mContext = pContext;
		mMenu = pMenu;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent pE) {
		items.clear();
		RotatedTileBox tileBox = getDrawPanel().copyCurrentTileBox();
		List<OsmandMapLayer> layers = getDrawPanel().getLayers();
		for (OsmandMapLayer layer : layers) {
			if (layer instanceof IContextMenuProvider) {
				IContextMenuProvider provider = (IContextMenuProvider) layer;
				List<Object> res = new Vector<>();
				provider.collectObjectsFromPoint(getDrawPanel().getMousePosition(), tileBox, res);
				for (Object am : res) {
					// ask controller, which actions are possible for the given object:
					List<JMenuItem> actionsForObject = mContext.getContextActionsForObject(provider, am);
					for (JMenuItem item : actionsForObject) {
						items.add(item);
						mMenu.add(item);
					}
				}
			}
		}
	}
	
	OsmBitmapPanel getDrawPanel() {
		return mContext.getDrawPanel();
	}
	
	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent pE) {
		for (JMenuItem jMenuItem : items) {
			mMenu.remove(jMenuItem);
		}
		items.clear();
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent pE) {
		popupMenuWillBecomeInvisible(pE);
	}

}
