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
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(getClasses(), null);
			Marshaller m = jaxbContext.createMarshaller();
			StringWriter writer = new StringWriter();
			m.marshal(pStorage, writer);
			return writer.toString();
		} catch (JAXBException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	static Class[] getClasses() {
		return new Class[]{ComponentLocationStorage.class, OsmWindowLocationStorage.class};
	}
	
	public static ComponentLocationStorage unmarshall(String pInput){
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(getClasses(), null);
			Unmarshaller m = jaxbContext.createUnmarshaller();
			ComponentLocationStorage storage = (ComponentLocationStorage) m.unmarshal(new StringReader(pInput));
			return storage;
		} catch (JAXBException ex) {
			ex.printStackTrace();
		}
		return null;
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
		String marshalled = marshall(storage);
		String result = marshalled;
		return result;
	}

	public static ComponentLocationStorage decorateDialog(OsmWindow controller,
			Window dialog, String window_preference_storage_property) {
		String marshalled = controller.getOffroadProperties()
				.getProperty(window_preference_storage_property);
		ComponentLocationStorage result = decorateDialog(marshalled, dialog);
		return result;
	}

	public static ComponentLocationStorage decorateDialog(String marshalled,
			Window dialog) {
		// String unmarshalled = controller.getProperty(
		// propertyName);
		if (marshalled != null) {
			ComponentLocationStorage storage = (ComponentLocationStorage) unmarshall(marshalled);
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
