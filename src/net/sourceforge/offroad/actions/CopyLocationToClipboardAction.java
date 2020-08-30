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

package net.sourceforge.offroad.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import net.osmand.data.LatLon;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 20.06.2016
 */
public class CopyLocationToClipboardAction extends OffRoadAction {

	public CopyLocationToClipboardAction(OsmWindow pContext) {
		super(pContext, null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		String link = getLink(mContext.getCursorPosition(), mContext.getCenterPosition(), mContext.getZoom());
		// Put link into clipboard.
		getClipboard().setContents(new StringSelection(link), null);
	}

	public static String getLink(LatLon position, LatLon mapCenter, int zoom) {
		String layer = "M";
		/*
		 * The embedded link would work for IE, too. But it is not easy to
		 * configure as a bounding box is necessary. It reads like
		 * osm.org/export/embed.html?bbox=...
		 */
		String link = "http://www.openstreetmap.org/?" + "mlat=" + position.getLatitude() + "&mlon=" + position.getLongitude()
				+ "&lat=" + mapCenter.getLatitude() + "&lon=" + mapCenter.getLongitude() + "&zoom=" + zoom + "&layers=" + layer;
		return link;
	}

	public static Clipboard getClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}

}
