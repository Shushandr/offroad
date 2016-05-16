package net.sourceforge.offroad.ui;

import java.awt.Point;

import net.sourceforge.offroad.ui.OsmBitmapPanel.ScreenManipulation;

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
		int it = 10;
		for (int i = 0; i < it; ++i) {
			ScreenManipulation sm = getScreenManipulation(it);
			mOsmBitmapPanel.addScreenManipulation(sm);
			mOsmBitmapPanel.repaintAndWait(50);
		}
	}

	ScreenManipulation getScreenManipulation(int it) {
		float dest = (float) Math.pow(2, mWheelRotation);
		float delta = (dest-1f) / it;
		ScreenManipulation sm = new ScreenManipulation();
		sm.scale = delta;
		// this is not correct. involve the size of the image.
		sm.originX = (mNewCenter.x * (1f-dest) / it);
		sm.originY = (mNewCenter.y * (1f-dest) / it);
		return sm;
	}
	
	public ScreenManipulation getScreenManipulationSum() {
		ScreenManipulation sm = getScreenManipulation(1);
		return sm;
	}

}