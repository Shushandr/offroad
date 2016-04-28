package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.osmand.data.RotatedTileBox;

class GenerationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	private final RotatedTileBox mTileCopy;
	private BufferedImage mNewBitmap;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel);
		mTileCopy = pTileCopy;
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
				mOsmBitmapPanel.setImage(mNewBitmap);
			}
		});
	}
	
	@Override
	public RotatedTileBox getDestinationTileBox() {
		return mTileCopy;
	}
}