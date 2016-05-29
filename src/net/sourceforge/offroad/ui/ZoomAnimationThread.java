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
		super(pOsmBitmapPanel);
		mWheelRotation = pWheelRotation;
		mNewCenter = pNewCenter;
	}

	@Override
	public void runAfterThreadsBeforeHaveFinished() {
		RotatedTileBox tb = mOsmBitmapPanel.copyCurrentTileBox();
		int originalZoom = tb.getZoom();
		int it = 10;
		for (int i = 0; i < it; ++i) {
			tb = getDestinationTileBox(originalZoom, it, i);
			mOsmBitmapPanel.setCurrentTileBox(tb);
			mOsmBitmapPanel.repaintAndWait(50);
		}
	}

	public RotatedTileBox getDestinationTileBox(int originalZoom, int it, int i) {
		RotatedTileBox tb = mOsmBitmapPanel.copyCurrentTileBox();
		// get old center:
		LatLon oldCenter = tb.getLatLonFromPixel(mNewCenter.x, mNewCenter.y);
		if (i<it-1) {
//				tb.setZoomAndAnimation(tb.getZoom(), tb.getZoomAnimation() + deltaB, tb.getZoomFloatPart()+deltaF);
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