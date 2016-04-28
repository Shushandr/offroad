package net.sourceforge.offroad.ui;

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
			mOsmBitmapPanel.originX = -(int) mDeltaX * i / it;
			mOsmBitmapPanel.originY = -(int) mDeltaY * i / it;
			mOsmBitmapPanel.repaintAndWait();
		}
	}
}