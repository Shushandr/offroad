/** 
   OffRoad
   Copyright (C) 2017 Christian Foltin

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

import java.awt.Point;

/**
 * @author foltin
 * @date 16.05.2017
 */
public interface ISelectionInterface {
	public interface IDragInformation {
		
	}

	boolean isSelection(Point p);
	void setSelection(Point p);
	/**
	 * @param pLastDragPoint
	 * @param pPoint
	 * @return a non-null value means true! The value has to be stored and given on subsequent 
	 * drags to the drag method.
	 */
	IDragInformation isDragPoint(Point pLastDragPoint, Point pPoint);
	void drag(Point pDeltaPoint, IDragInformation pInformation);
}
