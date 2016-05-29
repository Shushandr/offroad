package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.osmand.data.RotatedTileBox;

class GenerationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	protected final RotatedTileBox mTileCopy;
	protected BufferedImage mNewBitmap;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel);
		mTileCopy = pTileCopy.copy();
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
				// in the lazy case, the bitmap may not have been generated
				mOsmBitmapPanel.setImage(mNewBitmap, mTileCopy);
			}
		});
	}
}