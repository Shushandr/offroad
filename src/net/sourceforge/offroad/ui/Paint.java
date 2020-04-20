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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.TexturePaint;

/**
 * @author foltin
 * @date 23.05.2016
 */
public class Paint {
	
	public enum Style {
		STROKE, FILL_AND_STROKE, FILL
	}

	public enum Align {
		LEFT, CENTER, RIGHT
	}

	private boolean mAntialias;
	private Style mStrokeStyle;
	private boolean mFilterBitmap;
	private boolean mDither;
	private float mTextSize;
	private Align mTextAlign;
	private boolean mFakeBoldText;
	private Color mColor;
	private Composite mCompositeFilter;
	private Object mShader;
	private boolean mClearShadowLayer;
	private TexturePaint mTexturePaint;
	private float mStrokeWidth=2f;
	private Stroke mStroke;
	private boolean mFilterBufferedImage;

	public void setAntiAlias(boolean pAntialias) {
		mAntialias = pAntialias;
	}

	public void setStyle(Style pStrokeStyle) {
		mStrokeStyle = pStrokeStyle;
	}

	public void setFilterBitmap(boolean pFilterBitmap) {
		mFilterBitmap = pFilterBitmap;
	}

	public void setDither(boolean pDither) {
		mDither = pDither;
	}

	public void setTextSize(float pTextSize) {
		mTextSize = pTextSize;
	}

	public void setTextAlign(Align pAlign) {
		mTextAlign = pAlign;
	}

	public void setFakeBoldText(boolean pFakeBoldText) {
		mFakeBoldText = pFakeBoldText;
	}

	public void setColor(Color pColor) {
		mColor = pColor;
	}

	public void setColor(int pColor) {
		mColor = ColorUtils.create(pColor);
	}

	public void setColorFilter(Composite pCompositeFilter) {
		mCompositeFilter = pCompositeFilter;
	}

	public void setShader(Object pShader) {
		mShader = pShader;
	}

	public void clearShadowLayer() {
		mClearShadowLayer = true;
	}

	public void setPaint(TexturePaint pTexturePaint) {
		mTexturePaint = pTexturePaint;
	}

	public void setStrokeWidth(float pStrokeWidth) {
		mStrokeWidth = pStrokeWidth;
	}

	public Graphics2D updateGraphics(Graphics2D g2){
		if(mCompositeFilter!=null){
			g2.setComposite(mCompositeFilter);
		}
		if(mColor != null){
			g2.setColor(mColor);
		}
		if(mStroke != null){
			g2.setStroke(mStroke);
		} else {
			g2.setStroke(new BasicStroke(mStrokeWidth));
		}
		if(mTexturePaint != null){
			g2.setPaint(mTexturePaint);
		}
		if(mAntialias){
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		Font oldFont = g2.getFont();
		if (mTextSize != 0f) {
			g2.setFont(new Font(oldFont.getFontName(), oldFont.getStyle(), (int) mTextSize));
		}
		return g2;
	}

	public void setStroke(Stroke pDashEffect) {
		mStroke = pDashEffect;
	}

	public float getStrokeWidth() {
		return mStrokeWidth;
	}

	public int getColor() {
		return mColor.getRGB() + mColor.getAlpha()<<24;
	}

	public Color getColorAsColor() {
		return mColor;
	}
	
	public void setFilterBufferedImage(boolean pFilterBufferedImage) {
		// TODO: what to do with that?
		mFilterBufferedImage = pFilterBufferedImage;
	}
	
}
