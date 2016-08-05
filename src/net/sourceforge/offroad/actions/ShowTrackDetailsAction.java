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

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.SearchFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.LatLon;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.util.MapUtils;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.GraphPanel;

/**
 * @author foltin
 * @date 31.07.2016
 */
public class ShowTrackDetailsAction extends OffRoadAction {

	public static class WptPtDistStruct {
		WptPt mPoint;
		BinaryMapDataObject nearest = null;
		double distance = Integer.MAX_VALUE;
		public WptPtDistStruct(WptPt pPoint) {
			mPoint = pPoint;
		}
	}

	private SelectedGpxFile mSelectedGpxFile;
	private GraphPanel mGraphPanel;
	private JTextPane mContentDisplay;
	private boolean mAdjustmentCancelled;

	public ShowTrackDetailsAction(OsmWindow pContext, SelectedGpxFile pSelectedGpxFile) {
		super(pContext, pContext.getOffRoadString("offroad.track_details", pSelectedGpxFile.getGpxFile().getName()), null);
		mSelectedGpxFile = pSelectedGpxFile;
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
		String path = "file://" + mSelectedGpxFile.getGpxFile().path;
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
		mGraphPanel = new GraphPanel(new TreeMap<Long, Double>() );
		updateGraphPanel();
		contentPane.add(mGraphPanel, new GridBagConstraints(0, y++, 4, 1, 4.0, 10.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(articleLabel, new GridBagConstraints(0, y++, 4, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		JButton adjustElevationButton = new JButton(mContext.getOffRoadString("offroad.adjust_elevation"));
		JButton cancelElevationButton = new JButton(mContext.getOffRoadString("offroad.cancel_elevation"));
		cancelElevationButton.setEnabled(false);
		adjustElevationButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent pE) {
				new Thread(new Runnable() {

					public void run() {
						mContext.setWaitingCursor(true);
						mAdjustmentCancelled = false;
						adjustElevationButton.setEnabled(false);
						cancelElevationButton.setEnabled(true);
						try {
							adjustElevations(mSelectedGpxFile);
						} finally {
							adjustElevationButton.setEnabled(true);
							cancelElevationButton.setEnabled(false);
							mContext.setWaitingCursor(false);
							updateAnalysis();
							updateGraphPanel();
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
		GPXTrackAnalysis analysis = mSelectedGpxFile.getGpxFile().getAnalysis(System.currentTimeMillis());
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

	public void updateGraphPanel() {
		Map<Long, Double> elevation = new TreeMap<>();
		List<TrkSegment> pts = mSelectedGpxFile.getPointsToDisplay();
		for (TrkSegment n : pts) {
			for (WptPt pt : n.points) {
				elevation.put(pt.time, pt.ele);
			}
		}
		mGraphPanel.setScores(elevation);
	}

	public void adjustElevations(SelectedGpxFile pSelectedGpxFile) {
		List<WptPtDistStruct> allDistances = new Vector<>();
		// get bounding box
		int lb =  Integer.MAX_VALUE;
		int tb =  Integer.MAX_VALUE;
		int rb = -Integer.MAX_VALUE;
		int bb = -Integer.MAX_VALUE;
		// gather all points of the tracks
		for (Track track : pSelectedGpxFile.getGpxFile().tracks) {
			for (TrkSegment seg : track.segments) {
				for (WptPt pt : seg.points) {
					LatLon latLon = pt.getLatLon();
					lb = Math.min(lb, latLon.get31TileNumberX());
					rb = Math.max(rb, latLon.get31TileNumberX());
					tb = Math.min(tb, latLon.get31TileNumberY());
					bb = Math.max(bb, latLon.get31TileNumberY());
					allDistances.add(new WptPtDistStruct(pt));
				}
			}
		}
		// try to find height for each point.
		Map<String, BinaryMapIndexReader> files = mContext.getRenderer().getMetaInfoFiles();
		for (final BinaryMapIndexReader reader : files.values()) {
			if(!reader.containsMapData()){
				continue;
			}
			log.info("Processing " + reader.getFile() + ".");
			ResultMatcher<BinaryMapDataObject> resultMatcher = new ResultMatcher<BinaryMapDataObject>() {

				long lastPublishTime = System.currentTimeMillis();
				
				@Override
				public boolean publish(BinaryMapDataObject pObject) {
					for (int j = 1; j < pObject.getPointsLength(); j++) {
						double fromLat = MapUtils.get31LatitudeY(pObject.getPoint31YTile(j - 1));
						double fromLon = MapUtils.get31LongitudeX(pObject.getPoint31XTile(j - 1));
						double toLat = MapUtils.get31LatitudeY(pObject.getPoint31YTile(j));
						double toLon = MapUtils.get31LongitudeX(pObject.getPoint31XTile(j));
						for (WptPtDistStruct distStruct : allDistances) {
							if(distStruct.distance <= 5d){
								continue;
							}
							double dist = MapUtils.getOrthogonalDistance(distStruct.mPoint.getLatitude(),
									distStruct.mPoint.getLongitude(), fromLat, fromLon, toLat, toLon);
							if (dist < distStruct.distance) {
								distStruct.nearest = pObject;
								distStruct.distance = dist;
							}
						}
					}
					return false;
				}

				@Override
				public boolean isCancelled() {
					if(System.currentTimeMillis()-lastPublishTime > 1000){
						lastPublishTime = System.currentTimeMillis();
						updateElevationsInList(allDistances);
						updateGraphPanel();
					}
					return mAdjustmentCancelled;
				}

			};
			final SearchRequest<BinaryMapDataObject> req = BinaryMapIndexReader.buildSearchRequest(lb, rb, tb, bb,
					OsmWindow.MAX_ZOOM, new SearchFilter() {

						@Override
						public boolean accept(TIntArrayList types, MapIndex pIndex) {
							for (int j = 0; j < types.size(); j++) {
								int type = types.get(j);
								TagValuePair dType = pIndex.decodeType(type);
								if (dType != null && dType.tag.equals("contour") && dType.value.equals("elevation")){
									return true;
								}
							}
							return false;
						}
					}, resultMatcher);
			try {
				reader.searchMapIndex(req);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		updateElevationsInList(allDistances);
	}

	public void updateElevationsInList(List<WptPtDistStruct> allDistances) {
		for (WptPtDistStruct wptPtDistStruct : allDistances) {
			if(wptPtDistStruct.nearest != null){
				double ele1 = getElevation(wptPtDistStruct.nearest);
				log.debug("Elevations for " + wptPtDistStruct.mPoint.ele + ": " + ele1 + ", "  + wptPtDistStruct.distance);
				wptPtDistStruct.mPoint.ele = ele1;
			}
		}
	}
	public int getElevation(BinaryMapDataObject obj) {
		if(obj != null && !obj.getObjectNames().isEmpty()){
			String lengthString = obj.getOrderedObjectNames().values().iterator().next();
			int elev = Integer.parseInt(lengthString);
			return elev;
		}
		throw new IllegalArgumentException("obj " + obj.getName() + " has no name :-( :" + obj);
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

}
