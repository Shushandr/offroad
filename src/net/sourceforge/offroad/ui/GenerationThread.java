package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.osmand.data.RotatedTileBox;
import net.sourceforge.offroad.ui.OsmBitmapPanel.ScreenManipulation;

class GenerationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	protected final RotatedTileBox mTileCopy;
	protected BufferedImage mNewBitmap;
	private ScreenManipulation mManipulation;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy, ScreenManipulation pManipulation) {
		super(pOsmBitmapPanel);
		mTileCopy = pTileCopy.copy();
		mManipulation = (pManipulation==null)?null:new ScreenManipulation(pManipulation);
	}

	@Override
	public void runInBackground() {
		// generate in background
		mNewBitmap = mOsmBitmapPanel.newBitmap(mTileCopy);
	}
	
	public void runAfterThreadsBeforeHaveFinished() {
		// activate in foreground
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (mManipulation==null) {
					mOsmBitmapPanel.resetManipulation();
				} else {
					mOsmBitmapPanel.addScreenManipulation(mManipulation.negate());
				}
				// in the lazy case, the bitmap may not have been generated
				mOsmBitmapPanel.setImage(mNewBitmap, mTileCopy);
			}
		});
	}
	
	@Override
	public RotatedTileBox getDestinationTileBox() {
		return mTileCopy;
	}
}