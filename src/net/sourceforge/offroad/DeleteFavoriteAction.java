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

package net.sourceforge.offroad;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import net.osmand.data.FavouritePoint;
import net.sourceforge.offroad.actions.OffRoadAction;

/**
 * @author foltin
 * @date 17.06.2016
 */
public class DeleteFavoriteAction extends OffRoadAction {

	private FavouritePoint mPoint;

	public DeleteFavoriteAction(OsmWindow pContext, String pName, Icon pIcon, FavouritePoint pPoint) {
		super(pContext, pName, pIcon);
		mPoint = pPoint;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getFavorites().deleteFavourite(mPoint);
	}

}
