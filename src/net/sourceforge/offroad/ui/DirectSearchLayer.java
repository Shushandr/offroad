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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.TextInfo;
import net.osmand.plus.views.OsmandMapLayer;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.actions.DirectSearchAction.DirectSearchReceiver;
import net.sourceforge.offroad.ui.OsmBitmapPanel.CalculateUnzoomedPicturesAction.ImageStorage;
import net.sourceforge.offroad.ui.Paint.Style;

/**
 * @author foltin
 * @date 06.07.2016
 */
public class DirectSearchLayer extends OsmandMapLayer implements DirectSearchReceiver {

	private OsmBitmapPanel mView;
	private Paint highlightYellow;
	private String mToSearch;
	private final static Log log = PlatformUtil.getLog(DirectSearchLayer.class);

	/**
	 * 
	 */
	public DirectSearchLayer() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.osmand.plus.views.OsmandMapLayer#initLayer(net.sourceforge.offroad.ui
	 * .OsmBitmapPanel)
	 */
	@Override
	public void initLayer(OsmBitmapPanel pView) {
		mView = pView;
		highlightYellow = new Paint();
		highlightYellow.setColor(mView.getResources().getColor(R.color.poi_background));
//		highlightYellow.setColor(new Color(0x3f000000, true));
		highlightYellow.setStyle(Style.STROKE);
		highlightYellow.setStrokeWidth(15);
		highlightYellow.setAntiAlias(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.osmand.plus.views.OsmandMapLayer#onDraw(java.awt.Graphics2D,
	 * net.osmand.data.RotatedTileBox,
	 * net.osmand.plus.views.OsmandMapLayer.DrawSettings)
	 */
	@Override
	public void onDraw(Graphics2D pCanvas, RotatedTileBox pTileBox, DrawSettings pSettings) {
		if (mToSearch == null || mToSearch.length()<3)
			return;
		List<ImageStorage> list = mView.getEffectivelyDrawnImages();
		int index = 0;
		for (ImageStorage imageStorage : list) {
			RotatedTileBox ctb = pTileBox.copy();
			RotatedTileBox rtb = imageStorage.mTileBox;
			if (ctb.getZoom() != rtb.getZoom())
				continue;
			Graphics2D g2 = mView.createGraphics(pCanvas);
			highlightYellow.updateGraphics(g2);
			LatLon rtbLT = rtb.getLeftTopLatLon();
			LatLon rtbRB = rtb.getRightBottomLatLon();
			LatLon clalo = rtb.getCenterLatLon();
			double xc = ctb.getPixXFromLatLon(clalo.getLatitude(), clalo.getLongitude());
			double yc = ctb.getPixYFromLatLon(clalo.getLatitude(), clalo.getLongitude());
			float ctbRotate = ctb.getRotate();
			float theta = ctbRotate - rtb.getRotate();
			ctb.setRotate(rtb.getRotate());
			double x1 = ctb.getPixXFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
			double y1 = ctb.getPixYFromLatLon(rtbLT.getLatitude(), rtbLT.getLongitude());
			double x2 = ctb.getPixXFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
			double y2 = ctb.getPixYFromLatLon(rtbRB.getLatitude(), rtbRB.getLongitude());
			double thetaR = Math.toRadians(theta);
			g2.rotate(thetaR, xc, yc);
			AffineTransform t = new AffineTransform();
			double sx = (x2 - x1) / imageStorage.mImage.getWidth();
			double sy = (y2 - y1) / imageStorage.mImage.getHeight();
			t.translate(x1, y1);
			t.scale(sx, sy);
			g2.transform(t);
			for (TextInfo to : imageStorage.mResult.effectiveTextObjects) {
				if (to.mText != null && findText(to)) {
					index++;
					if (index > 10000) {
						log.warn("Too many search results found. Rest skipped");
						break;
					}
					// now highlight them:
					g2.fill(to.path);
					g2.draw(to.path);
				}
			}
			g2.dispose();
			ctb.setRotate(ctbRotate);
		}
	}

	public boolean findText(TextInfo to) {
		String orig = to.mText.toLowerCase();
		return StringUtils.getLevenshteinDistance(orig, mToSearch, 3) >= 0 ;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.osmand.plus.views.OsmandMapLayer#destroyLayer()
	 */
	@Override
	public void destroyLayer() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.osmand.plus.views.OsmandMapLayer#drawInScreenPixels()
	 */
	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void getResult(String pToSearch) {
		mToSearch = pToSearch;
	}

}
