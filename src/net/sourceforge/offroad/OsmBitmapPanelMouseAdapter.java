package net.sourceforge.offroad;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.Timer;

public class OsmBitmapPanelMouseAdapter extends MouseAdapter implements ComponentListener {
	private class ZoomPerformer implements ActionListener {
		private int mCounter;
		private Point mPoint;

		public void addWheelEvent(MouseWheelEvent pE) {
			mCounter += pE.getWheelRotation();
			mPoint = pE.getPoint();
		}

		public void actionPerformed(ActionEvent evt) {
			drawPanel.zoomChange(-mCounter, mPoint);
			mCounter = 0;
		}
	}

	private OsmBitmapPanel drawPanel;
	private Point startPoint;
	private Timer mZoomTimer;
	private ZoomPerformer mZoomPerformer;

	public OsmBitmapPanelMouseAdapter(OsmBitmapPanel drawPanel) {
		this.drawPanel = drawPanel;
		int delay = 100; // milliseconds
		mZoomPerformer = new ZoomPerformer();
		mZoomTimer = new Timer(delay, mZoomPerformer);
		mZoomTimer.setRepeats(false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		startPoint = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		double delx = e.getX() - startPoint.getX();
		double dely = e.getY() - startPoint.getY();
		drawPanel.moveImage(-(float) delx, -(float) dely);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Point point = e.getPoint();
		point.translate(-startPoint.x, -startPoint.y);
		drawPanel.dragImage(point);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent pE) {
		pE.consume();
		mZoomPerformer.addWheelEvent(pE);
		mZoomTimer.restart();
	}

	@Override
	public void componentResized(ComponentEvent pE) {
		drawPanel.createNewBitmap();
	}

	@Override
	public void componentMoved(ComponentEvent pE) {
	}

	@Override
	public void componentShown(ComponentEvent pE) {
	}

	@Override
	public void componentHidden(ComponentEvent pE) {
	}
	
	@Override
	public void mouseClicked(MouseEvent pE) {
		drawPanel.setCursor(pE.getPoint());
	}
	
}