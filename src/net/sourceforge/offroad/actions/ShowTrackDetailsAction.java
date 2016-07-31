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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.GraphPanel;

/**
 * @author foltin
 * @date 31.07.2016
 */
public class ShowTrackDetailsAction extends OffRoadAction {

	private SelectedGpxFile mSelectedGpxFile;

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
		JTextPane contentDisplay = new JTextPane();
		contentDisplay.setContentType("text/html"); // let the text pane know this is what you want
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
		contentDisplay.setText(content ); // showing off
		contentDisplay.setEditable(false); // as before
		contentDisplay.setBackground(null); // this is the same as a JLabel
		contentDisplay.setBorder(null); // remove the border
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
		contentPane.add(new JScrollPane(contentDisplay), new GridBagConstraints(0, y++, 4, 1, 4.0, 1.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		Map<Long, Double> elevation = new TreeMap<>();
		List<TrkSegment> pts = mSelectedGpxFile.getPointsToDisplay();
		for (TrkSegment n : pts) {
			for (WptPt pt : n.points) {
				elevation.put(pt.time, pt.ele);
			}
		}
		GraphPanel panel = new GraphPanel(elevation );
		contentPane.add(panel, new GridBagConstraints(0, y++, 4, 1, 4.0, 10.0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		contentPane.add(articleLabel, new GridBagConstraints(0, y++, 4, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
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
