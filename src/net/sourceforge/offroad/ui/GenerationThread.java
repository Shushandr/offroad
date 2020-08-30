package net.sourceforge.offroad.ui;

import java.awt.image.BufferedImage;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.RenderingResult;
import net.sourceforge.offroad.ui.OsmBitmapPanel.IntermediateImageListener;

class GenerationThread extends OffRoadUIThread implements IntermediateImageListener {
	/**
	 * 
	 */
	public RotatedTileBox mTileCopy;
	public RotatedTileBox mTileCopyCacheCheck;
	public final boolean mFlushCache;
	protected BufferedImage mNewBitmap;
	protected RenderingResult mResult;

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy) {
		super(pOsmBitmapPanel, "Generation");
		mTileCopy = pTileCopy.copy();
		mTileCopyCacheCheck = pTileCopy.copy();
		mFlushCache = false;
	}

	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy, boolean flushCache) {
		super(pOsmBitmapPanel, "Generation");
		mTileCopy = pTileCopy.copy();
		mTileCopyCacheCheck = pTileCopy.copy();
		mFlushCache = flushCache;
	}
	GenerationThread(OsmBitmapPanel pOsmBitmapPanel, RotatedTileBox pTileCopy, RotatedTileBox pTileCacheCheck) {
		super(pOsmBitmapPanel, "Generation");
		mTileCopy = pTileCopy.copy();
		mTileCopyCacheCheck = pTileCacheCheck.copy();
		mFlushCache = false;
	}

	public boolean isFlush() { return mFlushCache; }

	@Override
	public void runInBackground() {
		// Enable to visually check the background threads
		//if (mTileCopy.equals(mTileCopyCacheCheck)) return;
		if (!mFlushCache && mOsmBitmapPanel.isCached(mTileCopyCacheCheck)) return;
		mTileCopyCacheCheck = null; // no longer needed, free
		// generate in background
		mNewBitmap = mOsmBitmapPanel.createImage();
		// render at 2x requested size to have some margin for scrolling
		mTileCopy.setPixelDimensions(mNewBitmap.getWidth(), mNewBitmap.getHeight());
		mResult = mOsmBitmapPanel.drawImage(mNewBitmap, mTileCopy, this);
	}
	
	public void runAfterThreadsBeforeHaveFinished() {
		// in the lazy case, the bitmap may not have been generated
		mOsmBitmapPanel.setImage(mNewBitmap, mTileCopy, mResult, mFlushCache);
		// drop references so they get deallocated when removed from cache.
		mTileCopy = null;
		mNewBitmap = null;
		mResult = null;
	}

	@Override
	public void propagateImage() {
		// partial updates rely on the cache handling this right!
		runAfterThreadsBeforeHaveFinished();
	}
}
