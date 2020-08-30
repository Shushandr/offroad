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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.osmand.data.RotatedTileBox;

/**
 * @author foltin
 * @date 30.05.2016
 */
public class GenerateLayerOverlayThread extends OffRoadUIThread {

	private RotatedTileBox mTileBox;

	public GenerateLayerOverlayThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel, "Overlay");
		mTileBox = pTileCopy;
	}

	public void runInBackground() {
		int counter = 0;
		while(mNextThread == null){
			try {
				int millis = 20;
				counter += millis;
				Thread.sleep(millis);
				// TODO: this creates issues for other background tasks
				// due to blocking isQueueEmpty.
				// However removing this logic currently results in too
				// much work being done, so leave it as-is for now
				if(counter >=1000){
					// we have waited a second.
					// this is not done, unless we have for one second no other thread:
					log.debug("THREAD:" + this + " executes its background task.");
					createLayers();
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug("THREAD:" + this + " should continue without background task.");
	}

	private void createLayers() {
		// layers
		Dimension size = mOsmBitmapPanel.getSize();
		BufferedImage layerImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D lg = layerImage.createGraphics();
		mOsmBitmapPanel.drawLayers(mTileBox, lg, false);
		mOsmBitmapPanel.setLayerImage(layerImage, mTileBox);
	}

}
