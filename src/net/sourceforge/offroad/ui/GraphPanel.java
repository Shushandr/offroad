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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Adapted from https://gist.github.com/roooodcastro/6325153
 * 
 *
 * @author Rodrigo
 */
public class GraphPanel extends JPanel {

	private int width = 800;
	private int heigth = 400;
	private int padding = 25;
	private int labelPadding = 25;
	private Color lineColor = new Color(44, 102, 230, 180);
	private Color pointColor = new Color(100, 100, 100, 180);
	private Color gridColor = new Color(200, 200, 200, 200);
	private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
	private int pointWidth = 8;
	private int numberYDivisions = 10;
	private Map<Long, Double> scores;
	private Color mBackgroundColor;
	private String mDrawText;

	public GraphPanel(Map<Long, Double> pScores) {
		this.scores = pScores;
		mBackgroundColor = Color.WHITE;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Long minTime = getMinTime();
		double maxScore = getMaxScore() + padding;
		long ydist = getMaxTime()-minTime;
		double xScale = ((double) getWidth() - (2 * padding) - labelPadding) / (ydist);
		double yScale = ((double) getHeight() - 2 * padding - labelPadding) / (maxScore - getMinScore());

		List<Point> graphPoints = new ArrayList<>();
		for (Long ypoint : scores.keySet()) {
			int x1 = (int) ( (ypoint - minTime+0d) * xScale + padding + labelPadding);
			int y1 = (int) ((maxScore - scores.get(ypoint)) * yScale + padding);
			graphPoints.add(new Point(x1, y1));
		}

		g2.setColor(mBackgroundColor);
		g2.fillRect(padding + labelPadding, padding, getWidth() - (2 * padding) - labelPadding,
				getHeight() - 2 * padding - labelPadding);
		g2.setColor(Color.BLACK);

		// create hatch marks and grid lines for y axis.
		for (int i = 0; i < numberYDivisions + 1; i++) {
			int x0 = padding + labelPadding;
			int x1 = pointWidth + padding + labelPadding;
			int y0 = getHeight()
					- ((i * (getHeight() - padding * 2 - labelPadding)) / numberYDivisions + padding + labelPadding);
			int y1 = y0;
			if (scores.size() > 0) {
				g2.setColor(gridColor);
				g2.drawLine(padding + labelPadding + 1 + pointWidth, y0, getWidth() - padding, y1);
				g2.setColor(Color.BLACK);
				String yLabel = ((int) ((getMinScore()
						+ (maxScore - getMinScore()) * ((i * 1.0) / numberYDivisions)))) + "";
				FontMetrics metrics = g2.getFontMetrics();
				int labelWidth = metrics.stringWidth(yLabel);
				g2.drawString(yLabel, x0 - labelWidth - 5, y0 + (metrics.getHeight() / 2) - 3);
			}
			g2.drawLine(x0, y0, x1, y1);
		}

		// and for x axis
		for (int i = 0; i < scores.size(); i++) {
			if (scores.size() > 1) {
				int x0 = (int) (i * (getWidth() - padding * 2.0d - labelPadding) / scores.size() + padding + labelPadding);
				int x1 = x0;
				int y0 = getHeight() - padding - labelPadding;
				int y1 = y0 - pointWidth;
				if ((i % ((int) ((scores.size() / 5.0)) + 1)) == 0) {
					g2.setColor(gridColor);
					g2.drawLine(x0, getHeight() - padding - labelPadding - 1 - pointWidth, x1, padding);
					g2.setColor(Color.BLACK);
					String xLabel = DateFormat.getDateTimeInstance().format(minTime + ydist *i / scores.size());
					FontMetrics metrics = g2.getFontMetrics();
					int labelWidth = metrics.stringWidth(xLabel);
					g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + 3);
				}
				g2.drawLine(x0, y0, x1, y1);
			}
		}

		// create x and y axes
		g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, padding + labelPadding, padding);
		g2.drawLine(padding + labelPadding, getHeight() - padding - labelPadding, getWidth() - padding,
				getHeight() - padding - labelPadding);

		Stroke oldStroke = g2.getStroke();
		g2.setColor(lineColor);
		g2.setStroke(GRAPH_STROKE);
		for (int i = 0; i < graphPoints.size() - 1; i++) {
			int x1 = graphPoints.get(i).x;
			int y1 = graphPoints.get(i).y;
			int x2 = graphPoints.get(i + 1).x;
			int y2 = graphPoints.get(i + 1).y;
			g2.drawLine(x1, y1, x2, y2);
		}

		g2.setStroke(oldStroke);
		g2.setColor(pointColor);
		for (Point graphPoint : graphPoints) {
			int x = graphPoint.x - pointWidth / 2;
			int y = graphPoint.y - pointWidth / 2;
			int ovalW = pointWidth;
			int ovalH = pointWidth;
			g2.fillOval(x, y, ovalW, ovalH);
		}
		
		if(mDrawText != null){
			drawRotate(g2, mDrawText);
		}
	}
	
	public void drawRotate(Graphics2D g2, String text) {
		double place = Math.sqrt(getWidth()*getWidth()+getHeight()*getHeight());
		double angle = Math.acos(getWidth()/place);
		Graphics2D g2d = (Graphics2D) g2.create();
		g2d.setFont(g2d.getFont().deriveFont(128f));
		int stringWidth = g2d.getFontMetrics().stringWidth(text);
		g2d.translate(getWidth()/2f, getHeight()/2f);
		g2d.rotate(-angle);
		double effectiveStringWidth = place/(2f*stringWidth);
		g2d.scale(effectiveStringWidth,effectiveStringWidth);
		g2d.drawString(text, (int) (-0.5f*stringWidth), 0);
		g2d.dispose();
	}  

	// @Override
	// public Dimension getPreferredSize() {
	// return new Dimension(width, heigth);
	// }

	private double getMinScore() {
		double minScore = Double.MAX_VALUE;
		for (Double score : scores.values()) {
			minScore = Math.min(minScore, score);
		}
		return minScore;
	}

	private double getMaxScore() {
		double maxScore = Double.MIN_VALUE;
		for (Double score : scores.values()) {
			maxScore = Math.max(maxScore, score);
		}
		return maxScore;
	}

	private Long getMinTime() {
		long minTime = Long.MAX_VALUE;
		for (Long Time : scores.keySet()) {
			minTime = Math.min(minTime, Time);
		}
		return minTime;
	}
	
	private Long getMaxTime() {
		long maxTime = Long.MIN_VALUE;
		for (Long Time : scores.keySet()) {
			maxTime = Math.max(maxTime, Time);
		}
		return maxTime;
	}
	
	public void setScores(Map<Long, Double> scores) {
		this.scores = scores;
		invalidate();
		this.repaint();
	}

	public Map<Long, Double> getScores() {
		return scores;
	}

	private static void createAndShowGui() {
		Map<Long, Double> scores = new LinkedHashMap<>();
		Random random = new Random();
		int maxDataPoints = 40;
		int maxScore = 10;
		long time = System.currentTimeMillis();
		for (int i = 0; i < maxDataPoints; i++) {
			scores.put(time, random.nextDouble() * maxScore);
			time += random.nextDouble() * 1000*60;
		}
		GraphPanel mainPanel = new GraphPanel(scores);
		mainPanel.setDrawText("Calculating");
		mainPanel.setPreferredSize(new Dimension(800, 600));
		JFrame frame = new JFrame("DrawGraph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(GraphPanel::createAndShowGui);
	}

	public Color getBackgroundColor() {
		return mBackgroundColor;
	}

	public void setBackgroundColor(Color pBackgroundColor) {
		mBackgroundColor = pBackgroundColor;
	}

	public String getDrawText() {
		return mDrawText;
	}

	public void setDrawText(String pDrawText) {
		mDrawText = pDrawText;
	}
}
