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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 05.08.2016
 */
public class ElevationHelper {
	public interface LatLonHolder {
		LatLon getLatLon();
		double getLatitude();
		double getLongitude();
		void setElevation(double pEle1);
		double getElevation();
	}
	
	public interface LatLonGeneralization {
		List<LatLonHolder> getPoints();
		void updateGraphPanel();
		boolean isCancelled();
	}
	
	
	private final static Log log = PlatformUtil.getLog(ElevationHelper.class);

	public ElevationHelper() {
	}
	
	
	public static class WptPtDistStruct {
		LatLonHolder mPoint;
		BinaryMapDataObject nearest = null;
		double distance = Integer.MAX_VALUE;
		public WptPtDistStruct(LatLonHolder pPoint) {
			mPoint = pPoint;
		}
	}

	public void adjustElevations(LatLonGeneralization pInput, Map<String, BinaryMapIndexReader> files) {
		List<WptPtDistStruct> allDistances = new Vector<>();
		// get bounding box
		int lb =  Integer.MAX_VALUE;
		int tb =  Integer.MAX_VALUE;
		int rb = -Integer.MAX_VALUE;
		int bb = -Integer.MAX_VALUE;
		// gather all points of the tracks
		for (LatLonHolder latLonHolder : pInput.getPoints()) {
			LatLon latLon = latLonHolder.getLatLon();
			lb = Math.min(lb, latLon.get31TileNumberX());
			rb = Math.max(rb, latLon.get31TileNumberX());
			tb = Math.min(tb, latLon.get31TileNumberY());
			bb = Math.max(bb, latLon.get31TileNumberY());
			allDistances.add(new WptPtDistStruct(latLonHolder));
		}
		// try to find height for each point.
		for (final BinaryMapIndexReader reader : files.values()) {
			if(!reader.containsMapData()){
				continue;
			}
			log.info("Processing " + reader.getFile() + ".");
			ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {

				long lastPublishTime = System.currentTimeMillis();

				@Override
				public boolean publish(BinaryMapDataObject pObject) {
					for (int j = 1; j < pObject.getPointsLength(); j++) {
						double fromLat = MapUtils.get31LatitudeY(pObject.getPoint31YTile(j - 1));
						double fromLon = MapUtils.get31LongitudeX(pObject.getPoint31XTile(j - 1));
						double toLat = MapUtils.get31LatitudeY(pObject.getPoint31YTile(j));
						double toLon = MapUtils.get31LongitudeX(pObject.getPoint31XTile(j));
						for (WptPtDistStruct distStruct : allDistances) {
							if (distStruct.distance <= 5d) {
								continue;
							}
							double dist = MapUtils.getOrthogonalDistance(distStruct.mPoint.getLatitude(),
									distStruct.mPoint.getLongitude(), fromLat, fromLon, toLat, toLon);
							if (dist < distStruct.distance) {
								distStruct.nearest = pObject;
								distStruct.distance = dist;
							}
						}
					}
					return false;
				}

				@Override
				public boolean isCancelled() {
					if (System.currentTimeMillis() - lastPublishTime > 1000) {
						lastPublishTime = System.currentTimeMillis();
						updateElevationsInList(allDistances);
						pInput.updateGraphPanel();
					}
					return pInput.isCancelled();
				}

			};
			final SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(lb, rb, tb, bb,
					OsmWindow.MAX_ZOOM, (types, pIndex) -> {
						for (int j = 0; j < types.size(); j++) {
							int type = types.get(j);
							TagValuePair dType = pIndex.decodeType(type);
							if (dType != null && dType.tag.equals("contour") && dType.value.equals("elevation")){
								return true;
							}
						}
						return false;
					}, resultMatcher);
			try {
				reader.searchMapIndex(req);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		updateElevationsInList(allDistances);
	}

	public void updateElevationsInList(List<WptPtDistStruct> allDistances) {
		for (WptPtDistStruct wptPtDistStruct : allDistances) {
			if(wptPtDistStruct.nearest != null){
				double ele1 = getElevation(wptPtDistStruct.nearest);
				log.debug("Elevations for " + wptPtDistStruct.mPoint.getElevation() + ": " + ele1 + ", "  + wptPtDistStruct.distance);
				wptPtDistStruct.mPoint.setElevation(ele1);
			}
		}
	}
	public int getElevation(BinaryMapDataObject obj) {
		if(obj != null && !obj.getObjectNames().isEmpty()){
			String lengthString = obj.getOrderedObjectNames().values().iterator().next();
			return Integer.parseInt(lengthString);
		}
		throw new IllegalArgumentException("obj " + obj.getName() + " has no name :-( :" + obj);
	}

}
