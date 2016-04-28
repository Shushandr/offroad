package net.sourceforge.offroad.ui;

import java.awt.Point;

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
		double dest = Math.pow(2, mWheelRotation);
		double start = 1.0d;
		int it = 10;
		double delta = (dest - start) / it;
		for (int i = 0; i < it; ++i) {
			mOsmBitmapPanel.scale = start + i * delta;
			// this is not correct. involve the size of the image.
			mOsmBitmapPanel.originX = (int) (mNewCenter.x - (mNewCenter.x) * mOsmBitmapPanel.scale);
			mOsmBitmapPanel.originY = (int) (mNewCenter.y - (mNewCenter.y) * mOsmBitmapPanel.scale);
			System.out.println(this+" Wheel= " + mWheelRotation + ", Setting scale to " + mOsmBitmapPanel.scale + ", delta = "
					+ delta + ", dest=" + dest);
			mOsmBitmapPanel.repaintAndWait();
		}
	}
}