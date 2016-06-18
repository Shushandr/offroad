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

import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * @author foltin
 * @date 10.05.2016
 */
public class Drawable {


	private Rectangle mBounds;

	public void setBounds(int pX, int pY, int pWidth, int pHeight) {
		mBounds = new Rectangle(pX, pY, pWidth, pHeight);
	}

	public int getIntrinsicHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getIntrinsicWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void draw(Graphics2D pCanvas) {
		// TODO Auto-generated method stub
		
	}

	public int getOpacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setAlpha(int pAlpha) {
		// TODO Auto-generated method stub
		
	}

	protected void onBoundsChange(Rectangle bounds) {
		// TODO Auto-generated method stub
		
	}

	public Rectangle getBounds() {
		return mBounds;
	}

	public void setBounds(Rectangle pBounds) {
		mBounds = pBounds;
	}

	public void setColorFilter(Composite pCf) {
		// TODO Auto-generated method stub
		
	}

	public int getMinimumHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getMinimumWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setChangingConfigurations(int pConfigs) {
		// TODO Auto-generated method stub
		
	}

}
