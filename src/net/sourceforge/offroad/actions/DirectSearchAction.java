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

package net.sourceforge.offroad.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sourceforge.offroad.ui.OsmBitmapPanel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.OsmandRenderer.TextInfo;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.data.Pair;
import net.sourceforge.offroad.ui.OsmBitmapPanel.DrawnImageInfo;

/**
 * @author foltin
 * @date 06.07.2016
 */
public class DirectSearchAction extends OffRoadAction implements DocumentListener {

	private class TextLocationComparator implements Comparator<Pair<TextInfo, DrawnImageInfo>> {
		private RotatedTileBox mTileBox;

		public TextLocationComparator(RotatedTileBox pTileBox) {
			mTileBox = pTileBox;
		}

		@Override
		public int compare(Pair<TextInfo, DrawnImageInfo> pO1, Pair<TextInfo, DrawnImageInfo> pO2) {
			// compare both locations given that they should be at least some pixels apart.
			LatLon l1 = getLocation(pO1);
			LatLon l2 = getLocation(pO2);
			int diffX = (int) (mTileBox.getPixXFromLatLon(l1) - mTileBox.getPixXFromLatLon(l2));
			if(Math.abs(diffX)<2){
				int diffY = (int) (mTileBox.getPixYFromLatLon(l1) - mTileBox.getPixYFromLatLon(l2));
					if(Math.abs(diffX)<2){
						// TODO: Here, take the tilebox, that is closer to the current center.
						return 0;
					}	
					return diffY;
			}
			return diffX;
		}
	}

	public static class NullProvider implements ISearchProvider {

		@Override
		public String getSearchString() {
			return "";
		}

		@Override
		public boolean matches(String pCandidate) {
			return false;
		}

		@Override
		public boolean isValid() {
			return false;
		}
	}

	public interface DirectSearchReceiver {
		void setSearchProvider(ISearchProvider pProvider);
	}
	
	public interface ISearchProvider {
		String getSearchString();
		boolean matches(String pCandidate);
		/**
		 * @return true, if a search can be performed with this input.
		 */
		boolean isValid();
	}

	
	public class DefaultSearchProvider implements ISearchProvider {

		protected String mSearchString;

		public DefaultSearchProvider() {
			mSearchString = mTextField.getText();
		}

		@Override
		public String getSearchString() {
			return mSearchString;
		}

		@Override
		public boolean matches(String pCandidate) {
			return pCandidate.contains(getSearchString());
		}

		@Override
		public boolean isValid() {
			return mSearchString != null && mSearchString.length()>=3;
		}
	}
	
	public class CaseInsensitiveSearchProvider extends DefaultSearchProvider {
		

		public CaseInsensitiveSearchProvider() {
			mSearchString = mTextField.getText().toLowerCase();
		}

		@Override
		public boolean matches(String pCandidate) {
			return pCandidate.toLowerCase().contains(getSearchString());
		}
		
	}
	
	public class FuzzySearchProvider extends DefaultSearchProvider {


		public FuzzySearchProvider() {
			super();
		}

		@Override
		public boolean matches(String pCandidate) {
			double dist = StringUtils.getJaroWinklerDistance(pCandidate.toLowerCase(), getSearchString().toLowerCase());
			return dist >= 0.7f;
		}
		
	}
	
	public class SelectionSearchProvider implements ISearchProvider {


		public SelectionSearchProvider() {
		}
		
		@Override
		public String getSearchString() {
			return (isValid())?getText():null;
		}

		@Override
		public boolean matches(String pCandidate) {
			return getText().equals(pCandidate);
		}

		public String getText() {
			return mSearchHit.getKey().mText;
		}

		@Override
		public boolean isValid() {
			return mSearchHit != null;
		}
	}
	
	private List<DirectSearchReceiver> mDirectSearchReceiverList = new ArrayList<>();

	public void registerDirectSearchReceiver(DirectSearchReceiver pDirectSearchReceiver) {
		mDirectSearchReceiverList.add(pDirectSearchReceiver);
	}

	public void deregisterDirectSearchReceiver(DirectSearchReceiver pDirectSearchReceiver) {
		mDirectSearchReceiverList.remove(pDirectSearchReceiver);
	}

	
	private final static Log log = PlatformUtil.getLog(DirectSearchAction.class);

	
	private static final int TYPE_DELAY_TIME = 500;

	private Timer mTypeDelayTimer = null;

	private JTextField mTextField;


	private ISearchProvider mProvider;


	private JCheckBox mDirectSearchFuzzy;


	private JButton mDirectSearchForward;


	private JButton mDirectSearchBackward;
	
	private int mSearchIndex = -1;
	private Pair<TextInfo,DrawnImageInfo> mSearchHit;


	private JButton mDirectSearchClose;

	private synchronized void change(DocumentEvent event) {
		// stop old timer, if present:
		if (mTypeDelayTimer != null) {
			mTypeDelayTimer.stop();
			mTypeDelayTimer = null;
		}
		mTypeDelayTimer = new Timer(TYPE_DELAY_TIME, this);
		mTypeDelayTimer.setRepeats(false);
		mTypeDelayTimer.start();
	}


	public DirectSearchAction(OsmWindow pContext, JTextField pTextField, JCheckBox pDirectSearchFuzzy, JButton pDirectSearchBackward, JButton pDirectSearchForward, JButton pDirectSearchClose) {
		super(pContext, pContext.getOffRoadString("offroad.show_direct_search"), null);
		mTextField = pTextField;
		mDirectSearchFuzzy = pDirectSearchFuzzy;
		mDirectSearchBackward = pDirectSearchBackward;
		mDirectSearchForward = pDirectSearchForward;
		mDirectSearchClose = pDirectSearchClose;
		mDirectSearchFuzzy.addActionListener(this);
		mProvider = new FuzzySearchProvider();
		mTextField.getDocument().addDocumentListener(this);
		mTextField.addActionListener(event -> moveToNextHit());
		mDirectSearchForward.addActionListener(event -> moveToNextHit());
		mDirectSearchBackward.addActionListener(event -> moveToPreviousHit());
		mDirectSearchClose.addActionListener(event->setEnabled(false));
	}

	Entry<OsmBitmapPanel.DrawnImageInfo, TextInfo> getFirstHit(){
		if(mProvider.isValid()){
			List<DrawnImageInfo> list = mContext.getDrawPanel().getEffectivelyDrawnImages();
			for (DrawnImageInfo imageStorage : list) {
				for (TextInfo to : imageStorage.mResult.effectiveTextObjects) {
					if(to.mText != null && mProvider.matches(to.mText)){
						return new Pair<>(imageStorage, to);
					}
				}
			}
		}
		return null;
	}

	TreeSet<Pair<TextInfo,DrawnImageInfo>> getHits(){
		TreeSet<Pair<TextInfo, DrawnImageInfo>> res = new TreeSet<>(new TextLocationComparator(mContext.getDrawPanel().copyCurrentTileBox()));
		if(mProvider.isValid()){
			List<DrawnImageInfo> list = mContext.getDrawPanel().getEffectivelyDrawnImages();
			for (DrawnImageInfo imageStorage : list) {
				for (TextInfo to : imageStorage.mResult.effectiveTextObjects) {
					if(to.mText != null && mProvider.matches(to.mText)){
						res.add(new Pair<>(to, imageStorage));
					}
				}
			}
		}
		return res;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.showDirectSearch();
		if(mContext.getOffRoadString("offroad.DirectSearchText").equals(mTextField.getText())){
			mProvider = new NullProvider();
			mTextField.setBackground(null);
		} else {
			if(mDirectSearchFuzzy.isSelected()){
				mProvider = new FuzzySearchProvider();
			} else {
				mProvider = new CaseInsensitiveSearchProvider();
			}
			if(!mProvider.isValid() || getFirstHit() != null) {
				mTextField.setBackground(null);
			} else {
				mTextField.setBackground(Color.red);
			}
		}
		publishProvider();
	}

	protected void publishProvider() {
		// publish result to layer.
		for (DirectSearchReceiver directSearchReceiver : mDirectSearchReceiverList) {
			directSearchReceiver.setSearchProvider(mProvider);
		}
		mContext.getDrawPanel().drawLater();
		mTextField.requestFocusInWindow();
	}
	
	public ISearchProvider getSelectionProvider(){
		return new SelectionSearchProvider();
	}

	@Override
	public void insertUpdate(DocumentEvent pE) {
		change(pE);
	}

	@Override
	public void removeUpdate(DocumentEvent pE) {
		change(pE);
	}

	@Override
	public void changedUpdate(DocumentEvent pE) {
		change(pE);
	}

	public void moveToNextHit() {
		TreeSet<Pair<TextInfo, DrawnImageInfo>> hits = getHits();
		if(mSearchIndex+1 < hits.size()){
			mSearchIndex++;
		}
		jumpToHit(hits);
	}

	public void moveToPreviousHit() {
		TreeSet<Pair<TextInfo, DrawnImageInfo>> hits = getHits();
		if(mSearchIndex-1  >= 0){
			mSearchIndex--;
		}
		jumpToHit(hits);
	}
	
	public Pair<TextInfo, DrawnImageInfo> getHit(TreeSet<Pair<TextInfo, DrawnImageInfo>> pHits) {
		if(mSearchIndex < 0 || mSearchIndex >= pHits.size()){
			mSearchIndex = 0;
		}
		int index = 0;
		for (Pair<TextInfo, DrawnImageInfo> hit : pHits) {
			if(index == mSearchIndex){
				return hit;
			}
			index++;
		}
		return null;
	}
	

	public LatLon getLocation(Pair<TextInfo, DrawnImageInfo> hit) {
		PathIterator it = hit.getKey().path.getPathIterator(null);
		if(!it.isDone()){
			float[] coords = new float[2];
			it.currentSegment(coords);
			DrawnImageInfo storage = hit.getValue();
			return storage.mTileBox.getLatLonFromPixel(coords[0], coords[1]);
		}
		return null;
	}
	
	public void jumpToHit(TreeSet<Pair<TextInfo,DrawnImageInfo>> pHits) {
		Pair<TextInfo, DrawnImageInfo> hit = getHit(pHits);
		mSearchHit = hit;
		if(hit == null){
			return;
		}
		LatLon destination = getLocation(hit);
		if(destination != null){
			RotatedTileBox ctb = mContext.getDrawPanel().copyCurrentTileBox();
			RotatedTileBox dest = ctb.copy();
			dest.setLatLonCenter(destination);
			mContext.moveAnimated(dest, ctb, destination);
			mContext.setCursorPosition(destination);
		}
	}
	
	public void setEnabled(boolean pEnabled){
		if(!pEnabled){
			mProvider = new NullProvider();
			mSearchHit = null;
			publishProvider();
		} else {
			actionPerformed(null);
		}
	}

}
