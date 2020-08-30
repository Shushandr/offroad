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
import java.util.List;

import net.osmand.data.City;
import net.osmand.data.LatLon;
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
		name = getRegion().getName();//.replace('_', ' ');
		enName = getRegion().getLang();
		names = new HashMap<>();
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
	
	
	@Override
	public LatLon getLocation() {
		// getEstimatedRegionCenter does not work, because it is null.
		List<City> loadedCities = getRegion().getLoadedCities();
		if(loadedCities.isEmpty()){
			return null;
		}
		QuadRectExtendable qr = new QuadRectExtendable(loadedCities.get(0).getLocation());
		for (City city : loadedCities) {
			LatLon cityLoc = city.getLocation();
//			System.out.println(cityLoc + " is location of " + city);
			qr.insert(cityLoc);
		}
		return qr.getCenter();
	}
	
}
