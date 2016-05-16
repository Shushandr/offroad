package net.sourceforge.offroad.ui;

import net.sourceforge.offroad.ui.OsmBitmapPanel.ScreenManipulation;

class MoveAnimationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	private final float mDeltaX;
	private final float mDeltaY;

	MoveAnimationThread(OsmBitmapPanel pOsmBitmapPanel, float pDeltaX, float pDeltaY) {
		super(pOsmBitmapPanel);
		mDeltaX = pDeltaX;
		mDeltaY = pDeltaY;
	}

	@Override
	public void runAfterThreadsBeforeHaveFinished() {
		int it = 10;
		for (int i = 0; i < it; ++i) {
			mOsmBitmapPanel.addScreenManipulation(getScreenManipulation(it));
			mOsmBitmapPanel.repaintAndWait(50);
		}
	}

	ScreenManipulation getScreenManipulation(int it) {
		ScreenManipulation sm = new ScreenManipulation();
		sm.originX = -mDeltaX  / it;
		sm.originY = -mDeltaY  / it;
		return sm;
	}
	
	@Override
	public ScreenManipulation getScreenManipulationSum() {
		return getScreenManipulation(1);
	}
}