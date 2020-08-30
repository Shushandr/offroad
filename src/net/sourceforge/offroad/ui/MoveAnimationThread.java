package net.sourceforge.offroad.ui;

import net.osmand.data.RotatedTileBox;

class MoveAnimationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	private final float mDeltaX;
	private final float mDeltaY;

	MoveAnimationThread(OsmBitmapPanel pOsmBitmapPanel, float pDeltaX, float pDeltaY) {
		super(pOsmBitmapPanel, "MoveAnimation");
		mDeltaX = pDeltaX;
		mDeltaY = pDeltaY;
	}

	@Override
	public void runAfterThreadsBeforeHaveFinished() {
		int it;
		if(mNextThread == null) {
			// use acceleration only if it is the last thread in the row.
			it = 30;
			float v0 = 2f;
			float a = (it*(1f-v0) + 2f*v0 - 2f)/(it-2f)/(it-2f);
			float accumulatedX = 0f;
			float accumulatedY = 0f;
			for (int i = 0; i < it-1; ++i) {
				float delta = v0 * i + a*i*i;
				mOsmBitmapPanel.moveTileBox(-accumulatedX + mDeltaX*delta/it, -accumulatedY + mDeltaY*delta/it);
				accumulatedX = mDeltaX*delta/it;
				accumulatedY = mDeltaY*delta/it;
				mOsmBitmapPanel.repaintAndWait(50);
			}
			mOsmBitmapPanel.moveTileBox(-accumulatedX + mDeltaX, -accumulatedY + mDeltaY);
		} else {
			it = 10;
			for (int i = 0; i < it; ++i) {
				mOsmBitmapPanel.moveTileBox(mDeltaX/it, mDeltaY/it);
				mOsmBitmapPanel.repaintAndWait(50);
			}
		}
	}
}
