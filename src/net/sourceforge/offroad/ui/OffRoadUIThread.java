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

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 26.04.2016
 */
public class OffRoadUIThread implements Runnable {
	@Override
	public String toString() {
		return "OffRoadUIThread ["+this.getClass().getName() + "]";
	}

	protected final static Log log = PlatformUtil.getLog(OffRoadUIThread.class);

	
	public interface OffRoadUIThreadListener {
		public void threadStarted(OffRoadUIThread pThread);
		public void threadFinished(OffRoadUIThread pThread);
	}

	protected OffRoadUIThread mNextThread = null;
	private boolean hasFinished = false;
	protected Semaphore mShouldContinue = new Semaphore(0);
	protected OsmBitmapPanel mOsmBitmapPanel;
	private Set<OffRoadUIThreadListener> mListeners = new HashSet<>();
	protected String mName;

	public OffRoadUIThread(OsmBitmapPanel pOsmBitmapPanel, String pName) {
		mOsmBitmapPanel = pOsmBitmapPanel;
		mName = pName;
	}
	
	public void addListener(OffRoadUIThreadListener pListener){
		mListeners.add(pListener);
	}
	
	public void removeListener(OffRoadUIThreadListener pListener){
		mListeners.remove(pListener);
	}
	
	public synchronized void setNextThread(OffRoadUIThread pNextThread) {
		mNextThread = pNextThread;
	}

	public void shouldContinue(){
		mShouldContinue.release();
	}
	
	public void runInBackground() {
		// overwrite if needed.
	}

	/**
	 * Is NOT executed in the ui thread!
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	public void runAfterThreadsBeforeHaveFinished() throws InvocationTargetException, InterruptedException {
		// overwrite if needed.
	}

	@Override
	public void run() {
		try {
			for (OffRoadUIThreadListener listener : mListeners) {
				listener.threadStarted(this);
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
			listener.threadFinished(this);
		}
	}

	protected void waitForThreadBeforeHaveFinished() {
		try {
			mShouldContinue.acquire();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		log.debug("THREAD:" + this + " should continue.");
	}

	
	public synchronized boolean hasFinished() {
		return hasFinished;
	}

	protected synchronized boolean findSuccessor(Class<? extends OffRoadUIThread> pTypeOfSuccessor){
		if(mNextThread==null){
			return false;
		}
		if(pTypeOfSuccessor.isAssignableFrom(mNextThread.getClass())){
			return true;
		}
		return mNextThread.findSuccessor(pTypeOfSuccessor);
	}
	
	protected String printQueue() {
		String ret =  getName() + ", ";
		if(mNextThread!=null){
			ret += mNextThread.printQueue();
		}
		return ret;
	}
	
	protected OsmWindow getContext(){
		return mOsmBitmapPanel.getContext();
	}

	public String getName() {
		return mName;
	}

}
