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

import net.osmand.data.RotatedTileBox;

/**
 * Only generates the image, if for at least one second no other following thread was queued.
 * @author foltin
 * @date 20.05.2016
 */
public class LazyThread extends GenerationThread {

	LazyThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel, pTileCopy);
	}

	public void runInBackground() {
		int counter = 0;
		while(!findSuccessor(GenerationThread.class)){
			try {
				int millis = 20;
				counter += millis;
				Thread.sleep(millis);
				if(counter >=1000){
					// we have waited one second.
					// this is not done, unless we have for one second no other thread:
					log.debug("THREAD:" + this + " executes its background task.");
					super.runInBackground();
					return;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug("THREAD:" + this + " should continue without background task: " + printQueue());
	}

}
