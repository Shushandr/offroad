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
import java.awt.event.ActionListener;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.plus.render.OsmandRenderer.TextInfo;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.data.Pair;
import net.sourceforge.offroad.ui.OsmBitmapPanel.CalculateUnzoomedPicturesAction.ImageStorage;

/**
 * @author foltin
 * @date 06.07.2016
 */
public class DirectSearchAction extends OffRoadAction implements DocumentListener {

	public interface DirectSearchReceiver {
		void getSearchProvider(ISearchProvider pProvider);
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


	public DirectSearchAction(OsmWindow pContext, JTextField pTextField, JCheckBox pDirectSearchFuzzy) {
		super(pContext);
		mTextField = pTextField;
		mDirectSearchFuzzy = pDirectSearchFuzzy;
		mDirectSearchFuzzy.addActionListener(this);
		mProvider = new FuzzySearchProvider();
		mTextField.getDocument().addDocumentListener(this);
		// FIXME: Jump to the best hit!
		mTextField.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent pE) {
				// on enter jump to the first location:
				Entry<ImageStorage, TextInfo> hit = getFirstHit();
				if(hit != null){
					PathIterator it = hit.getValue().path.getPathIterator(null);
					if(!it.isDone()){
						float[] coords = new float[2];
						it.currentSegment(coords);
						mContext.move(hit.getKey().mTileBox.getLatLonFromPixel(coords[0], coords[1]), null);
					}
				}
			}});
	}

	Entry<ImageStorage, TextInfo> getFirstHit(){
		if(mProvider.isValid()){
			List<ImageStorage> list = mContext.getDrawPanel().getEffectivelyDrawnImages();
			for (ImageStorage imageStorage : list) {
				for (TextInfo to : imageStorage.mResult.effectiveTextObjects) {
					if(to.mText != null && mProvider.matches(to.mText)){
						return new Pair<ImageStorage, TextInfo>(imageStorage, to);
					}
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent pE) {
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
		// publish result to layer.
		for (DirectSearchReceiver directSearchReceiver : mDirectSearchReceiverList) {
			directSearchReceiver.getSearchProvider(mProvider);
		}
		mContext.getDrawPanel().drawLater();
		mTextField.requestFocusInWindow();
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

}
