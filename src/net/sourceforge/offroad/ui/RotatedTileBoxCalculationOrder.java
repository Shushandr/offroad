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

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.RotatedTileBox.RotatedTileBoxBuilder;
import net.osmand.util.MapAlgorithms;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 15.05.2016
 */
public class RotatedTileBoxCalculationOrder {

	private RotatedTileBox mTb;
	private BoxDeltaDescription[] mOrder = new BoxDeltaDescription[]{
		// Ensure there's extra boundaries at current position
		// Disabled since it causes annoying flicker
		//new BoxDeltaDescription(0,  0,  0),
		// For zooming out/coarse view at boundary
            new BoxDeltaDescription(1, 0, 0),
            new BoxDeltaDescription(4, 0, 0),
	};
	private int mIndex = 0;
	
	
	public RotatedTileBoxCalculationOrder() {
		// first hasNext should get false.
		mIndex = mOrder.length;
	}

	public void init(RotatedTileBox pTb) {
		mTb = pTb.copy();
		mIndex = 0;
	}

	public boolean hasNext(){
		return mIndex < mOrder.length;
	}

	public RotatedTileBox getNext() {
		RotatedTileBox tb = mTb.copy();
		BoxDeltaDescription desc = mOrder[mIndex];
		LatLon centerLatLon = tb.getLatLonFromPixel((desc.xDelta + 0.5f) * tb.getPixWidth(), (desc.yDelta + 0.5f)*tb.getPixHeight());
		tb.setLatLonCenter(centerLatLon.getLatitude(), centerLatLon.getLongitude());
		tb.setZoom(tb.getZoom()-desc.zoomDelta);
		BoxDeltaDescription descNew;
		do {
			mIndex++;
			if(!hasNext()){
				break;
			}
			descNew = mOrder[mIndex];
		} while((mTb.getZoom() - descNew.zoomDelta) < OsmWindow.MIN_ZOOM);
		return tb;
	}
	
	private static class BoxDeltaDescription {
		int zoomDelta;
		int xDelta;
		int yDelta;
		public BoxDeltaDescription(int pZoomDelta, int pXDelta, int pYDelta) {
			super();
			zoomDelta = pZoomDelta;
			xDelta = pXDelta;
			yDelta = pYDelta;
		}
	}
	
	
	public int getSize(){
		return mOrder.length;
	}

	public static void main(String[] args) {
		System.out.println(MapAlgorithms.calculateIntersection(-3, -3, 1, 1, -1, 2, -1, 2));
		RotatedTileBoxBuilder builder = new RotatedTileBoxBuilder();
		RotatedTileBox tb = builder.setLocation(51, 13).setPixelDimensions(100, 200).setZoom(7).build();
		RotatedTileBoxCalculationOrder order = new RotatedTileBoxCalculationOrder();
		order.init(tb);
		while(order.hasNext()){
			System.out.println(order.getNext());
		}
	}
	
}
