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
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.osmand.data.LatLon;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.data.ElevationHelper;
import net.sourceforge.offroad.data.ElevationHelper.LatLonGeneralization;
import net.sourceforge.offroad.data.ElevationHelper.LatLonHolder;
import net.sourceforge.offroad.ui.GraphPanel;

/**
 * @author foltin
 * @date 31.07.2016
 */
public class ShowRouteDetailsAction extends OffRoadAction implements LatLonGeneralization {

	private RouteCalculationResult mRouteCalculationResult;
	private List<RouteHolder> mRouteHolderList;
	private GraphPanel mGraphPanel;
	private JTextPane mContentDisplay;
	private boolean mAdjustmentCancelled;
	private ElevationHelper mElevationHelper;
	private long now = System.currentTimeMillis();

	public ShowRouteDetailsAction(OsmWindow pContext, RouteCalculationResult pRouteCalculationResult) {
		super(pContext, pContext.getOffRoadString("offroad.route_details"), null);
		mRouteCalculationResult = pRouteCalculationResult;
		mElevationHelper = new ElevationHelper();
		List<RouteSegmentResult> pts = mRouteCalculationResult.getOriginalRoute();
		mRouteHolderList = new ArrayList<RouteHolder>();
		for (RouteSegmentResult rsr : pts) {
			for (int i = rsr.getStartPointIndex(); i != rsr.getEndPointIndex(); ) {
				RouteHolder holder = new RouteHolder(rsr, i);
				mRouteHolderList.add(holder);
				log.debug("Adding segment result " + rsr + " and index " + i + " and time " + holder.getTime());
				if(rsr.isForwardDirection()){
					i++;
				} else {
					i--;
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent pE) {
		createDialog();
		String title = (String) this.getValue(NAME);
		mDialog.setTitle(title);
		Container contentPane = mDialog.getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWeights = new double[] { 1.0f };
		gbl.rowWeights = new double[] { 1.0f };
		contentPane.setLayout(gbl);
		int y = 0;
		mContentDisplay = new JTextPane();
		mContentDisplay.setContentType("text/html"); // let the text pane know this is what you want
		updateAnalysis();
		mContentDisplay.setEditable(false); // as before
		mContentDisplay.setBackground(null); // this is the same as a JLabel
		mContentDisplay.setBorder(null); // remove the border
		contentPane.add(new JScrollPane(mContentDisplay), new GridBagConstraints(0, y++, 4, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mGraphPanel = new GraphPanel(new TreeMap<Long, Double>() );
		updateGraphPanel();
		contentPane.add(mGraphPanel, new GridBagConstraints(0, y++, 4, 1, 4.0, 10.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		JButton adjustElevationButton = new JButton(mContext.getOffRoadString("offroad.adjust_elevation_calculation"));
		JButton cancelElevationButton = new JButton(mContext.getOffRoadString("offroad.cancel_elevation_calculation"));
		cancelElevationButton.setEnabled(false);
		adjustElevationButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent pE) {
				new Thread(new Runnable() {

					public void run() {
						mContext.setWaitingCursor(true);
						mGraphPanel.setBackgroundColor(Color.LIGHT_GRAY);
						mGraphPanel.setDrawText(mContext.getOffRoadString("offroad.Calculating"));
						mAdjustmentCancelled = false;
						adjustElevationButton.setEnabled(false);
						cancelElevationButton.setEnabled(true);
						try {
							mElevationHelper.adjustElevations(ShowRouteDetailsAction.this, mContext.getRenderer().getMetaInfoFiles());
						} finally {
							adjustElevationButton.setEnabled(true);
							cancelElevationButton.setEnabled(false);
							mContext.setWaitingCursor(false);
							updateAnalysis();
							updateGraphPanel();
							mGraphPanel.setBackgroundColor(Color.WHITE);
							mGraphPanel.setDrawText(null);
						}
					}
				}).start();
			}});
		cancelElevationButton.addActionListener(l -> mAdjustmentCancelled=true);
		contentPane.add(adjustElevationButton, new GridBagConstraints(0, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(cancelElevationButton, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		JButton okButton = new JButton(mContext.getString(R.string.shared_string_ok));
		okButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent pE) {
				disposeDialog();
			}});
		contentPane.add(okButton, new GridBagConstraints(3, y++, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		mDialog.getRootPane().setDefaultButton(okButton);
		// select region:
		mDialog.pack();
		decorateDialog();
		mDialog.setVisible(true);
		
	}

	public void updateAnalysis() {
		String content = "<table border='2'><thead><th>Key</th><th>Value</th></thead><tbody>";
		String inBrk = "</td><td align='right'>";
		content += "<tr><td>" + mContext.getOffRoadString("totalDistance") + inBrk + toDist(mRouteCalculationResult.getWholeDistance()) + "</td></tr>";
		content += "<tr><td>" + mContext.getOffRoadString("totalTime") + inBrk + toTimeString((long) (mRouteCalculationResult.getRoutingTime()*1000f)) + "</td></tr>";
		content += "</tbody></table";
		mContentDisplay.setText(content ); // showing off
	}

	@Override
	public void updateGraphPanel() {
		Map<Long, Double> elevation = new TreeMap<>();
		long totalTime = now;
		for (RouteHolder routeHolder : mRouteHolderList) {
			elevation.put(new Long(totalTime), routeHolder.getElevation());
			totalTime += (long) routeHolder.getTime();
		}
		mGraphPanel.setScores(elevation);
	}


	private String toDist(double pValue) {
		return MessageFormat.format("{0,number,#} m", pValue);
	}
	
	private String toTimeString(long pF) {
		return DateFormat.getTimeInstance().format(pF);
	}


	private static class RouteHolder implements LatLonHolder {
		private int mIndex;
		public RouteSegmentResult mRsr;
		public double ele;
		private LatLon mLatLon;

		public RouteHolder(RouteSegmentResult pRsr, int pIndex) {
			super();
			mRsr = pRsr;
			mIndex = pIndex;
			mLatLon = mRsr.getPoint(mIndex);
		}

		public float getTime() {
			return mRsr.getSegmentTime()*1000f/Math.abs(mRsr.getStartPointIndex()-mRsr.getEndPointIndex());
		}

		
		@Override
		public LatLon getLatLon() {
			return mLatLon;
		}

		@Override
		public double getLatitude() {
			return getLatLon().getLatitude();
		}

		@Override
		public double getLongitude() {
			return getLatLon().getLongitude();
		}

		@Override
		public void setElevation(double pEle1) {
			ele = pEle1;
		}

		@Override
		public double getElevation() {
			return ele;
		}
	}
	
	@Override
	public List<LatLonHolder> getPoints() {
		ArrayList<LatLonHolder> res = new ArrayList<>();
		res.addAll(mRouteHolderList);
		return res;
	}

	@Override
	public boolean isCancelled() {
		return mAdjustmentCancelled;
	}

}
