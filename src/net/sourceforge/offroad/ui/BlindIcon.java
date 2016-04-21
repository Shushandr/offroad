package net.sourceforge.offroad.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class BlindIcon implements Icon {

	private int length;

	public BlindIcon(int length) {
		this.length = length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconHeight()
	 */
	public int getIconHeight() {
		return length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconWidth()
	 */
	public int getIconWidth() {
		return length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics,
	 * int, int)
	 */
	public void paintIcon(Component arg0, Graphics arg1, int arg2, int arg3) {
	}

}
