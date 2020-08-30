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

package net.sourceforge.offroad.data.persistence;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 17.05.2016
 */
@XmlRootElement
public class ComponentLocationStorage {
	private int x;
	private int y;
	private int width;
	private int height;

	
	public static String marshall(ComponentLocationStorage pStorage){
		Class<?>[] classes = getClasses();
		return OsmWindow.marshall(pStorage, classes);
	}

	static Class<?>[] getClasses() {
		return new Class<?>[] { ComponentLocationStorage.class, OsmWindowLocationStorage.class };
	}

	public static ComponentLocationStorage unmarshall(String pInput) {
		return (ComponentLocationStorage) OsmWindow.unmarshall(pInput, getClasses());
	}
	
	public static void main(String[] args) {
		ComponentLocationStorage st = new ComponentLocationStorage();
		st.x = 17;
		st.height = 100;
		String erg = marshall(st);
		System.out.println(erg);
		ComponentLocationStorage storage = unmarshall(erg);
		System.out.println(storage.height);
		OsmWindowLocationStorage os = new OsmWindowLocationStorage();
		os.setSplitLocation(12);
		os.setHeight(18);
		String res2 = marshall(os);
		System.out.println(res2);
		unmarshall(res2);
	}

	@XmlElement
	public int getX() {
		return x;
	}

	public void setX(int pX) {
		x = pX;
	}

	@XmlElement
	public int getY() {
		return y;
	}

	public void setY(int pY) {
		y = pY;
	}

	@XmlElement
	public int getWidth() {
		return width;
	}

	public void setWidth(int pWidth) {
		width = pWidth;
	}

	@XmlElement
	public int getHeight() {
		return height;
	}

	public void setHeight(int pHeight) {
		height = pHeight;
	}
	public static void storeDialogPositions(OsmWindow controller, Window dialog,
			ComponentLocationStorage storage,
			String window_preference_storage_property) {
		String result = storeDialogPositions(storage, dialog);
		controller.getOffroadProperties().put(window_preference_storage_property, result);
	}

	protected static String storeDialogPositions(ComponentLocationStorage storage,
			Window dialog) {
		storage.setX((dialog.getX()));
		storage.setY((dialog.getY()));
		storage.setWidth((dialog.getWidth()));
		storage.setHeight((dialog.getHeight()));
		return marshall(storage);
	}

	public static ComponentLocationStorage decorateDialog(OsmWindow controller,
			Window dialog, String window_preference_storage_property) {
		String marshalled = controller.getOffroadProperties()
				.getProperty(window_preference_storage_property);
		return decorateDialog(marshalled, dialog);
	}

	public static ComponentLocationStorage decorateDialog(String marshalled,
			Window dialog) {
		// String unmarshalled = controller.getProperty(
		// propertyName);
		if (marshalled != null) {
			ComponentLocationStorage storage = unmarshall(marshalled);
			if (storage != null) {
				// Check that location is on current screen.
				Dimension screenSize;
				if ("false".equals("place_dialogs_on_first_screen")) {
					Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
					screenSize = defaultToolkit.getScreenSize();
				} else {
					screenSize = new Dimension();
					screenSize.height = Integer.MAX_VALUE;
					screenSize.width = Integer.MAX_VALUE;
				}
				int delta = 20;
				dialog.setLocation(
						Math.min(storage.getX(), screenSize.width - delta),
						Math.min(storage.getY(), screenSize.height - delta));
				dialog.setSize(new Dimension(storage.getWidth(), storage
						.getHeight()));
				return storage;
			}
		}

		// set standard dialog size of no size is stored
		final Frame rootFrame = JOptionPane.getFrameForComponent(dialog);
		final Dimension prefSize = rootFrame.getSize();
		prefSize.width = prefSize.width * 3 / 4;
		prefSize.height = prefSize.height * 3 / 4;
		dialog.setSize(prefSize);
		return null;
	}
}
