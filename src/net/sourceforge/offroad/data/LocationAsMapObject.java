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

package net.sourceforge.offroad.data;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;

/**
 * @author foltin
 * @date 12.06.2016
 */
public class LocationAsMapObject extends MapObject {
	private Location mLocation;
	private int mDistance;

	public LocationAsMapObject(Location pLoc, String pName, int pDistance) {
		mLocation = pLoc;
		mDistance = pDistance;
		setLocation(mLocation.getLatitude(), mLocation.getLongitude());
		setFileOffset(0);
		setName(pName);
		setId(0L);
	}
	
	@Override
	public double getDistance(LatLon pCursorPosition) {
		return mDistance;
	}
}
