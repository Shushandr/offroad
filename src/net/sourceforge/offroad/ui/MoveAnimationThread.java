package net.sourceforge.offroad.ui;

import net.osmand.data.RotatedTileBox;

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
			RotatedTileBox tb = mOsmBitmapPanel.moveTileBox(mDeltaX/it, mDeltaY/it);
			mOsmBitmapPanel.repaintAndWait(50);
		}
	}
}