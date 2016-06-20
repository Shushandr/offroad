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

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 20.06.2016
 */
public class ShowTargetPointAction extends OffRoadAction {
	private TargetPointSelector mSelector;
	private final static Log log = PlatformUtil.getLog(ShowTargetPointAction.class);

	public interface TargetPointSelector {
		public TargetPoint chooseTargetPoint(TargetPointsHelper helper);

	}

	public ShowTargetPointAction(OsmWindow pContext, TargetPointSelector pSelector) {
		super(pContext, null, null);
		mSelector = pSelector;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		TargetPoint t = mSelector.chooseTargetPoint(mContext.getTargetPointsHelper());
		if (t != null) {
			mContext.move(t.point, null);
		} else {
			log.warn("Target Point is null");
		}
	}

}
