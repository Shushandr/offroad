/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2001  Joerg Mueller <joergmueller@bigfoot.com>
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sourceforge.offroad.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * @author Foltin
 * 
 */
public class RoundButton extends JButton {

	private BufferedImage mForegroundImage;
	private BufferedImage mBackgroundImage;
	private float mRotate = 0f;

	public RoundButton() {
		super();
		setModel(new DefaultButtonModel());
		init(null, null);
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		setBackground(Color.BLACK);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setFocusable(false);
		setAlignmentY(Component.TOP_ALIGNMENT);
		setUI(new RoundImageButtonUI());
	}

	public Dimension getPreferredSize() {
		return getUI().getPreferredSize(this);
	}
	
	public void setForegroundIcon(BufferedImage pForegroundImage){
		mForegroundImage = pForegroundImage;
	}
	public void setBackgroundIcon(BufferedImage pBackgroundImage){
		mBackgroundImage = pBackgroundImage;
	}

	/**
	 * @return
	 */
	public int getZoomedCircleRadius() {
		return 40;
	}

	class RoundImageButtonUI extends BasicButtonUI {
		protected Shape shape, base;

		protected void installDefaults(AbstractButton b) {
			super.installDefaults(b);
			clearTextShiftOffset();
			defaultTextShiftOffset = 0;
			b.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			b.setContentAreaFilled(false);
			b.setFocusPainted(false);
			b.setOpaque(false);
			b.setBackground(Color.BLACK);
			b.setAlignmentY(Component.TOP_ALIGNMENT);
			initShape(b);
		}

		public void paint(Graphics g, JComponent c) {
			super.paint(g, c);
			Graphics2D g2 = (Graphics2D) g.create();
			initShape(c);
			Rectangle bounds = shape.getBounds();
			int width = (int)bounds.getMaxX();
			int height = (int)bounds.getMaxY();
			g2.setColor(getBackground());
			g2.fillOval(bounds.x, bounds.y, width, height);
			if(mBackgroundImage!= null){
				g2.drawImage(mBackgroundImage, bounds.x, bounds.y, width, height, 0, 0, mBackgroundImage.getWidth(), mBackgroundImage.getHeight(), null);
			}
			if(mForegroundImage!= null){
				g2.rotate(Math.toRadians(mRotate), bounds.getCenterX(), bounds.getCenterY());
				g2.drawImage(mForegroundImage, bounds.x, bounds.y, width, height, 0, 0, mForegroundImage.getWidth(), mForegroundImage.getHeight(), null);
			}
		}

		public Dimension getPreferredSize(JComponent c) {
			JButton b = (JButton) c;
			Insets i = b.getInsets();
			int iw = getZoomedCircleRadius();
			return new Dimension(iw + i.right + i.left, iw + i.top + i.bottom);
		}

		private void initShape(JComponent c) {
			if (!c.getBounds().equals(base)) {
				Dimension s = c.getPreferredSize();
				base = c.getBounds();
				shape = new Ellipse2D.Float(0, 0, s.width - 1, s.height - 1);
			}
		}
	}

	public void dispose() {
	}

	public void setRotate(float pCachedRotate) {
		mRotate  = pCachedRotate;
		
	}

}
