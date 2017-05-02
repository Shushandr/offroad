package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.RenderingResult;
import net.sourceforge.offroad.ui.OsmBitmapPanel.IntermediateImageListener;

class GenerationThread extends OffRoadUIThread implements IntermediateImageListener {
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
		mNewBitmap = mOsmBitmapPanel.createImage();
		mResult = mOsmBitmapPanel.drawImage(mNewBitmap, mTileCopy, this);
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

	@Override
	public void propagateImage() {
		runAfterThreadsBeforeHaveFinished();
	}
}