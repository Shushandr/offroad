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

import java.awt.geom.Point2D;
import java.util.List;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;

/**
 * @author foltin
 * @date 10.05.2016
 */
public interface IContextMenuProvider {

	String getObjectDescription(Object pO);

	PointDescription getObjectName(Object pO);

	boolean disableSingleTap();

	boolean disableLongPressOnMap();

	boolean isObjectClickable(Object pO);

	void collectObjectsFromPoint(Point2D pPoint, RotatedTileBox pTileBox, List<Object> pRes);

	LatLon getObjectLocation(Object pO);

}
