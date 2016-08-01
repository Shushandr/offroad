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

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Timer;

import net.osmand.data.RotatedTileBox;
import net.sourceforge.offroad.R;

/**
 * @author foltin
 * @date 19.07.2016
 */
public class DirectSearchSelectionLayer extends DirectSearchLayer implements DirectOffroadLayer {

	private static final int INACTIVITY_TIME_IN_MILLISECONDS = 2000;

	private float mStrokeWidth;

	protected boolean mSelectedColor;

	/**
	 * 
	 */
	public DirectSearchSelectionLayer() {
		Action updatePaintAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				mSelectedColor = !mSelectedColor;
				if(mSelectedColor){
					highlightYellow.setColor(R.color.region_selected);
				} else {
					highlightYellow.setColor(R.color.poi_background);
				}
				mView.repaint();
			}
		};
		new Timer(INACTIVITY_TIME_IN_MILLISECONDS, updatePaintAction).start();

	}

	
	@Override
	public void initLayer(OsmBitmapPanel pView) {
		super.initLayer(pView);
		highlightYellow.setColor(R.color.region_selected);
		mStrokeWidth = highlightYellow.getStrokeWidth();
	}
	
	@Override
	public void onDraw(Graphics2D pCanvas, RotatedTileBox pTileBox, DrawSettings pSettings) {
		highlightYellow.setStrokeWidth(mStrokeWidth*2);
		super.onDraw(pCanvas, pTileBox, pSettings);
	}
}
