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
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
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
public class ShowTrackDetailsAction extends OffRoadAction implements LatLonGeneralization {

	private GPXFile mGpxFile;
	private GraphPanel mGraphPanel;
	private JTextPane mContentDisplay;
	private boolean mAdjustmentCancelled;
	private ElevationHelper mElevationHelper;

	public ShowTrackDetailsAction(OsmWindow pContext, GPXFile pGpxFile) {
		super(pContext, pContext.getOffRoadString("offroad.track_details", pGpxFile.getName()), null);
		mGpxFile = pGpxFile;
		mElevationHelper = new ElevationHelper();
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
		String path = "file://" + mGpxFile.path;
		String htmlContent = "<html><body><a href=''>" + path + "</a></body></html>";
		mContentDisplay = new JTextPane();
		mContentDisplay.setContentType("text/html"); // let the text pane know this is what you want
		updateAnalysis();
		mContentDisplay.setEditable(false); // as before
		mContentDisplay.setBackground(null); // this is the same as a JLabel
		mContentDisplay.setBorder(null); // remove the border
		JLabel articleLabel = new JLabel(htmlContent);
		articleLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		articleLabel.setToolTipText(title);
		articleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(path));
                } catch (URISyntaxException | IOException ex) {
                	ex.printStackTrace();
                }
            }
        });
		contentPane.add(new JScrollPane(mContentDisplay), new GridBagConstraints(0, y++, 4, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		mGraphPanel = new GraphPanel(new TreeMap<>() );
		updateGraphPanel();
		contentPane.add(mGraphPanel, new GridBagConstraints(0, y++, 4, 1, 4.0, 10.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(articleLabel, new GridBagConstraints(0, y++, 4, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		JButton adjustElevationButton = new JButton(mContext.getOffRoadString("offroad.adjust_elevation"));
		JButton cancelElevationButton = new JButton(mContext.getOffRoadString("offroad.cancel_elevation"));
		cancelElevationButton.setEnabled(false);
		adjustElevationButton.addActionListener(pE12 -> new Thread(() -> {
			mContext.setWaitingCursor(true);
			mGraphPanel.setBackgroundColor(Color.LIGHT_GRAY);
			mGraphPanel.setDrawText(mContext.getOffRoadString("offroad.Calculating"));
			mAdjustmentCancelled = false;
			adjustElevationButton.setEnabled(false);
			cancelElevationButton.setEnabled(true);
			try {
				mElevationHelper.adjustElevations(ShowTrackDetailsAction.this, mContext.getRenderer().getMetaInfoFiles());
			} finally {
				adjustElevationButton.setEnabled(true);
				cancelElevationButton.setEnabled(false);
				mContext.setWaitingCursor(false);
				updateAnalysis();
				updateGraphPanel();
				mGraphPanel.setDrawText(null);
				mGraphPanel.setBackgroundColor(Color.WHITE);
			}
		}).start());
		cancelElevationButton.addActionListener(l -> mAdjustmentCancelled=true);
		contentPane.add(adjustElevationButton, new GridBagConstraints(0, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(cancelElevationButton, new GridBagConstraints(1, y++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		JButton okButton = new JButton(mContext.getString(R.string.shared_string_ok));
		okButton.addActionListener(pE1 -> disposeDialog());
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
		GPXTrackAnalysis analysis = mGpxFile.getAnalysis(System.currentTimeMillis());
		String inBrk = "</td><td align='right'>";
		if(analysis.isTimeSpecified()){
			content += "<tr><td>" + mContext.getOffRoadString("startTime") + inBrk + toString(analysis.startTime) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("endTime") + inBrk + toString(analysis.endTime) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("timeSpan") + inBrk + toTimeString(analysis.timeSpan) + "</td></tr>";
		}
		if(analysis.isTimeMoving()){
			content += "<tr><td>" + mContext.getOffRoadString("timeMoving") + inBrk + toTimeString(analysis.timeMoving) + "</td></tr>";
		}
		content += "<tr><td>" + mContext.getOffRoadString("totalDistance") + inBrk + toDist(analysis.totalDistance) + "</td></tr>";
		if(analysis.isTimeMoving()){
			content += "<tr><td>" + mContext.getOffRoadString("totalDistanceMoving") + inBrk + toDist(analysis.totalDistanceMoving) + "</td></tr>";
		}
		content += "<tr><td>" + mContext.getOffRoadString("points") + inBrk + analysis.points + "</td></tr>";
		if(analysis.isSpeedSpecified()){
			content += "<tr><td>" + mContext.getOffRoadString("avgSpeed") + inBrk + toVelocity(analysis.avgSpeed) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("maxSpeed") + inBrk + toVelocity(analysis.maxSpeed) + "</td></tr>";
		}
		if(analysis.isElevationSpecified()){
			content += "<tr><td>" + mContext.getOffRoadString("minElevation") + inBrk + toDist(analysis.minElevation) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("maxElevation") + inBrk + toDist(analysis.maxElevation) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("diffElevation") + inBrk + (toDist(analysis.maxElevation-analysis.minElevation)) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("diffElevationUp") + inBrk + toDist(analysis.diffElevationUp) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("diffElevationDown") + inBrk + toDist(analysis.diffElevationDown) + "</td></tr>";
			content += "<tr><td>" + mContext.getOffRoadString("avgElevation") + inBrk + toDist(analysis.avgElevation) + "</td></tr>";
		}
		content += "</tbody></table";
		mContentDisplay.setText(content ); // showing off
	}

	@Override
	public void updateGraphPanel() {
		Map<Long, Double> elevation = new TreeMap<>();
		List<TrkSegment> pts = mGpxFile.proccessPoints();
		for (TrkSegment n : pts) {
			for (WptPt pt : n.points) {
				elevation.put(pt.time, pt.ele);
			}
		}
		mGraphPanel.setScores(elevation);
	}


	private String toVelocity(float pValue) {
		return MessageFormat.format("{0,number,#.##} km/h", pValue);
	}

	private String toDist(double pValue) {
		return MessageFormat.format("{0,number,#} m", pValue);
	}

	private String toString(long pStartTime) {
		return DateFormat.getDateTimeInstance().format(pStartTime);
	}
	private String toTimeString(long pStartTime) {
		return DateFormat.getTimeInstance().format(pStartTime);
	}

	private static class WptHolder implements LatLonHolder {
		public WptHolder(WptPt pPt) {
			super();
			mPt = pPt;
		}

		public WptPt mPt;

		@Override
		public LatLon getLatLon() {
			return mPt.getLatLon();
		}

		@Override
		public double getLatitude() {
			return mPt.getLatitude();
		}

		@Override
		public double getLongitude() {
			return mPt.getLongitude();
		}

		@Override
		public void setElevation(double pEle1) {
			mPt.ele = pEle1;
		}

		@Override
		public double getElevation() {
			return mPt.ele;
		}
	}
	
	@Override
	public List<LatLonHolder> getPoints() {
		ArrayList<LatLonHolder> res = new ArrayList<>();
		List<TrkSegment> pts = mGpxFile.proccessPoints();
		for (TrkSegment n : pts) {
			for (WptPt pt : n.points) {
				res.add(new WptHolder(pt));
			}
		}
		return res;
	}

	@Override
	public boolean isCancelled() {
		return mAdjustmentCancelled;
	}

}
