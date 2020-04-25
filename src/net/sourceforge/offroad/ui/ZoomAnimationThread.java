package net.sourceforge.offroad.ui;

import java.awt.Point;

import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;

class ZoomAnimationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	private final int mWheelRotation;
	private final Point mNewCenter;

	ZoomAnimationThread(OsmBitmapPanel pOsmBitmapPanel, int pWheelRotation, Point pNewCenter) {
		super(pOsmBitmapPanel, "ZoomAnimation");
		mWheelRotation = pWheelRotation;
		mNewCenter = pNewCenter;
	}

	@Override
	public void runAfterThreadsBeforeHaveFinished() {
		RotatedTileBox tb = mOsmBitmapPanel.copyCurrentTileBox();
		int originalZoom = tb.getZoom();
//		it -2 == v0 * (it-2)  + a (it-2)²
//				a =  ( it(1-v0) + 2*v0-2)/(it-2)²
		int it;
		if(mNextThread == null) {
			// use acceleration only if it is the last thread in the row.
			it = 30;
			float v0 = 2f;
			float a = (it*(1f-v0) + 2f*v0 - 2f)/(it-2f)/(it-2f);
			for (int i = 0; i < it-1; ++i) {
				float zoom = v0 * i + a*i*i;
				log.info("Zoom is " + zoom + " for step " + i + " and velocity " + v0 + " and acceleration " + a);
				tb = getDestinationTileBox(originalZoom, it, zoom);
				mOsmBitmapPanel.setCurrentTileBox(tb);
				log.debug("Set tile box " + tb + " and now repaint.");
				mOsmBitmapPanel.repaintAndWait(16);
				log.debug("Set tile box " + tb + " and now repaint. Done.");
			}
		} else {
			it = 10;
			for (int i = 0; i < it-2; ++i) {
				tb = getDestinationTileBox(originalZoom, it, i);
				mOsmBitmapPanel.setCurrentTileBox(tb);
				log.debug("Set tile box " + tb + " and now repaint.");
				mOsmBitmapPanel.repaintAndWait(16);
				log.debug("Set tile box " + tb + " and now repaint. Done.");
			}
		}
		tb = getDestinationTileBox(originalZoom, it, it-1);
		mOsmBitmapPanel.setCurrentTileBox(tb);
		log.debug("Set tile box " + tb + " and now repaint.");
		mOsmBitmapPanel.repaintAndWait(16);
		log.debug("Set tile box " + tb + " and now repaint. Done.");
	}

	public RotatedTileBox getDestinationTileBox(int originalZoom, float it, float i) {
		RotatedTileBox tb = mOsmBitmapPanel.copyCurrentTileBox();
		// get old center:
		LatLon oldCenter = tb.getLatLonFromPixel(mNewCenter.x, mNewCenter.y);
		if (i<it-1) {
			float destZoom = mOsmBitmapPanel.checkZoom(originalZoom+(0f+i*mWheelRotation)/it);
			int baseZoom = (int) Math.floor(destZoom);
			float fractZoom = destZoom- baseZoom;
			tb.setZoomAndAnimation(baseZoom, 0d, fractZoom);
		} else {
			tb.setZoomAndAnimation((int)mOsmBitmapPanel.checkZoom(originalZoom+mWheelRotation), 0d, 0d);
		}
		float oldCenterX = tb.getPixXFromLatLon(oldCenter);
		float oldCenterY = tb.getPixYFromLatLon(oldCenter);
		QuadPoint center = tb.getCenterPixelPoint();
		float originX = mNewCenter.x-oldCenterX;
		float originY = mNewCenter.y-oldCenterY;
		double newLat = tb.getLatFromPixel(center.x - originX, center.y - originY);
		double newLon = tb.getLonFromPixel(center.x - originX, center.y - originY);
		tb.setLatLonCenter(newLat, newLon);
		return tb;
	}

	
}
