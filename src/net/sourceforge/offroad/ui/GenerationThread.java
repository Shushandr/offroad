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
	public final RotatedTileBox mTileCopy;
	public final RotatedTileBox mTileCopyCacheCheck;
	protected BufferedImage mNewBitmap;
	protected RenderingResult mResult;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel, "Generation");
		mTileCopy = pTileCopy.copy();
		mTileCopyCacheCheck = pTileCopy.copy();
	}

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy, RotatedTileBox pTileCacheCheck) {
		super(pOsmBitmapPanel, "Generation");
		mTileCopy = pTileCopy.copy();
		mTileCopyCacheCheck = pTileCacheCheck.copy();
	}

	@Override
	public void runInBackground() {
		// Enable to visually check the background threads
		//if (mTileCopy.equals(mTileCopyCacheCheck)) return;
		if (mOsmBitmapPanel.isCached(mTileCopyCacheCheck)) return;
		// generate in background
		mNewBitmap = mOsmBitmapPanel.createImage();
		// render at 2x requested size to have some margin for scrolling
		mTileCopy.setPixelDimensions(mNewBitmap.getWidth(), mNewBitmap.getHeight());
		mResult = mOsmBitmapPanel.drawImage(mNewBitmap, mTileCopy, this);
	}
	
	public void runAfterThreadsBeforeHaveFinished() {
		// in the lazy case, the bitmap may not have been generated
		mOsmBitmapPanel.setImage(mNewBitmap, mTileCopy, mResult);
	}

	@Override
	public void propagateImage() {
		// partial updates rely on the cache handling this right!
		runAfterThreadsBeforeHaveFinished();
	}
}
