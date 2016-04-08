/** 
   OffRoad
   Copyright (C) 2016 foltin

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

import java.util.HashMap;

import net.osmand.data.MapObject;
import net.osmand.plus.resources.RegionAddressRepository;

/**
 * @author foltin
 * @date 08.04.2016
 */
public class RegionAsMapObject extends MapObject {
	private RegionAddressRepository mRegion;

	public RegionAsMapObject(RegionAddressRepository pRegion) {
		setRegion(pRegion);
		name = getRegion().getName();
		enName = getRegion().getLang();
		names = new HashMap<String, String>();
		names.put(getRegion().getLang(), name);
		location = getRegion().getEstimatedRegionCenter();
		fileOffset = 0;
		// FIXME: ???
		id = (long) getRegion().getFile().hashCode();
	}

	public RegionAddressRepository getRegion() {
		return mRegion;
	}

	public void setRegion(RegionAddressRepository pRegion) {
		mRegion = pRegion;
	}
	
	
}
