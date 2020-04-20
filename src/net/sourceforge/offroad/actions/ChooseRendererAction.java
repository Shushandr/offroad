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

import javax.swing.Icon;

import net.osmand.plus.render.RendererRegistry;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

/**
 * @author foltin
 * @date 30.05.2016
 */
public class ChooseRendererAction extends OffRoadAction implements SelectableAction {

	private String mRenderer;

	public ChooseRendererAction(OsmWindow pContext, String pName, Icon pIcon, String pRenderer) {
		super(pContext, pName, pIcon);
		mRenderer = pRenderer;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		RendererRegistry registry = mContext.getRendererRegistry();
		registry.setCurrentSelectedRender(registry.getRenderer(mRenderer));
		mContext.getSettings().RENDERER.set(mRenderer);
		mContext.getDrawPanel().flushCacheAndDrawLater();
	}
	
	@Override
	public boolean isSelected() {
		return mRenderer.equals(mContext.getRendererRegistry().getCurrentSelectedRenderer().getName());
	}

}
