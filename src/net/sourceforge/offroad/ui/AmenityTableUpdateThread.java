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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.SwingUtilities;

import net.osmand.data.LatLon;
import net.osmand.data.MapObject;

/**
 * @author foltin
 * @date 12.06.2016
 */
public class AmenityTableUpdateThread extends OffRoadUIThread {

	private List<MapObject> mSearchResult;
	private AmenityTablePanel mAmenityTable;

	public AmenityTableUpdateThread(OsmBitmapPanel pOsmBitmapPanel, AmenityTablePanel pAmenityTable) {
		super(pOsmBitmapPanel, "AmenityTableUpdate");
		mAmenityTable = pAmenityTable;
	}

	@Override
	public void runInBackground() {
		// this takes time (in fact, the Internet may be asked...)
		mSearchResult = getContext().getSearchResult();
	}
	
	@Override
	public void runAfterThreadsBeforeHaveFinished() throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(() -> mAmenityTable.setSearchResult(mSearchResult));
	}

}
