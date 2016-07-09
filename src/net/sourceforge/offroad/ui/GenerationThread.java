package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.RenderingResult;

class GenerationThread extends OffRoadUIThread {
	/**
	 * 
	 */
	protected final RotatedTileBox mTileCopy;
	protected BufferedImage mNewBitmap;
	protected RenderingResult mResult;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel);
		mTileCopy = pTileCopy.copy();
	}

	@Override
	public void runInBackground() {
		// generate in background
		BufferedImage image = mOsmBitmapPanel.createImage();
		mResult = mOsmBitmapPanel.drawImage(image, mTileCopy);
		mNewBitmap = image;
	}
	
	public void runAfterThreadsBeforeHaveFinished() {
		// activate in foreground
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// in the lazy case, the bitmap may not have been generated
				mOsmBitmapPanel.setImage(mNewBitmap, mTileCopy, mResult);
			}
		});
	}
}