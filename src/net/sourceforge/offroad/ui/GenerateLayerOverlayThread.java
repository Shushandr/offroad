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
import java.util.List;

import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;

/**
 * @author foltin
 * @date 30.05.2016
 */
public class GenerateLayerOverlayThread extends OffRoadUIThread {

	private RotatedTileBox mTileBox;

	public GenerateLayerOverlayThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel);
		mTileBox = pTileCopy;
	}

	public void runInBackground() {
		int counter = 0;
		while(mNextThread == null){
			try {
				int millis = 20;
				counter += millis;
				Thread.sleep(millis);
				if(counter >=500){
					// we have waited half a second.
					// this is not done, unless we have for one second no other thread:
					log.info("THREAD:" + this + " executes its background task.");
					createLayers();
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("THREAD:" + this + " should continue without background task.");
	}

	private void createLayers() {
		// layers
		Dimension size = mOsmBitmapPanel.getSize();
		BufferedImage layerImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D lg = layerImage.createGraphics();
		final QuadPoint c = mTileBox.getCenterPixelPoint();
		DrawSettings settings = new DrawSettings(false);
		List<OsmandMapLayer> layers = mOsmBitmapPanel.getLayers();
		for (int i = 0; i < layers.size(); i++) {
			Graphics2D glayer = mOsmBitmapPanel.createGraphics(lg);
			try {
				OsmandMapLayer layer = layers.get(i);
				// rotate if needed
				if (!layer.drawInScreenPixels()) {
					glayer.rotate(mTileBox.getRotate(), c.x, c.y);
				}
				layer.onPrepareBufferImage(glayer, mTileBox, settings);
				layer.onDraw(glayer, mTileBox, settings);
				// canvas.restore();
			} catch (Exception e) {
				// skip it
				e.printStackTrace();
			}
			glayer.dispose();
		}
		mOsmBitmapPanel.setLayerImage(layerImage, mTileBox);
	}

}
