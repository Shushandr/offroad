/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package net.sourceforge.offroad.ui;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;

/**
 * @author foltin
 * @date 26.04.2016
 */
public class OffRoadUIThread implements Runnable {
	protected final static Log log = PlatformUtil.getLog(OffRoadUIThread.class);

	
	public interface OffRoadUIThreadListener {
		public void threadStarted();
		public void threadFinished();
	}

	protected OffRoadUIThread mNextThread = null;
	private boolean hasFinished = false;
	protected boolean mShouldContinue = false;
	protected OsmBitmapPanel mOsmBitmapPanel;
	private Set<OffRoadUIThreadListener> mListeners = new HashSet<>();

	public OffRoadUIThread(OsmBitmapPanel pOsmBitmapPanel) {
		mOsmBitmapPanel = pOsmBitmapPanel;
	}
	
	public void addListener(OffRoadUIThreadListener pListener){
		mListeners.add(pListener);
	}
	
	public void removeListener(OffRoadUIThreadListener pListener){
		mListeners.remove(pListener);
	}
	
	public void setNextThread(OffRoadUIThread pNextThread) {
		mNextThread = pNextThread;
	}

	public void shouldContinue(){
		mShouldContinue = true;
	}
	
	public void runInBackground() {
		// overwrite if needed.
	};

	public void runAfterThreadsBeforeHaveFinished() {
		// overwrite if needed.
	}

	@Override
	public void run() {
		try {
			for (OffRoadUIThreadListener listener : mListeners) {
				listener.threadStarted();
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			runInBackground();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		waitForThreadBeforeHaveFinished();
		try {
			runAfterThreadsBeforeHaveFinished();
		} catch (Exception e) {
			e.printStackTrace();
		}
		synchronized (this) {
			log.debug("THREAD:" + this + " Notify next threads...");
			if (mNextThread != null) {
				mNextThread.shouldContinue();
			}
			hasFinished = true;
		}
		for (OffRoadUIThreadListener listener : mListeners) {
			listener.threadFinished();
		}
	}

	protected void waitForThreadBeforeHaveFinished() {
		while(!mShouldContinue){
			// FIXME: Use notify.
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.debug("THREAD:" + this + " should continue.");
	}

	
	public boolean hasFinished() {
		return hasFinished;
	}

}
