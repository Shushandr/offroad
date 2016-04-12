package net.sourceforge.offroad;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.Timer;

public class OsmBitmapPanelMouseAdapter extends MouseAdapter implements ComponentListener, KeyListener {
	private class ZoomPerformer implements ActionListener {
		private int mCounter;
		private Point mPoint;

		public void addWheelEvent(int pWheelRotation, Point pPoint) {
			mCounter += pWheelRotation;
			mPoint = pPoint;
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
	private MouseEvent mMouseEvent;

	public OsmBitmapPanelMouseAdapter(OsmBitmapPanel drawPanel) {
		this.drawPanel = drawPanel;
		int delay = 100; // milliseconds
		mZoomPerformer = new ZoomPerformer();
		mZoomTimer = new Timer(delay, mZoomPerformer);
		mZoomTimer.setRepeats(false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		startPoint = e.getPoint();
	}

	public boolean isPopup(MouseEvent e) {
		return e.getButton() != MouseEvent.BUTTON1;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		double delx = e.getX() - startPoint.getX();
		double dely = e.getY() - startPoint.getY();
		drawPanel.moveImage(-(float) delx, -(float) dely);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		Point point = e.getPoint();
		point.translate(-startPoint.x, -startPoint.y);
		drawPanel.dragImage(point);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent pE) {
		pE.consume();
		mZoomPerformer.addWheelEvent(pE.getWheelRotation(), pE.getPoint());
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
	public void mouseClicked(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		drawPanel.setCursor(e.getPoint());
	}

	@Override
	public void keyTyped(KeyEvent pE) {
		
	}

	@Override
	public void keyPressed(KeyEvent pE) {
		int height = drawPanel.getHeight();
		int width = drawPanel.getWidth();
		switch (pE.getKeyCode()) {
		case KeyEvent.VK_UP:
			drawPanel.moveImageAnimated(0,-height/3);
			return;
		case KeyEvent.VK_DOWN:
			drawPanel.moveImageAnimated(0,height/3);
			return;
		case KeyEvent.VK_LEFT:
			drawPanel.moveImageAnimated(-width/3,0);
			return;
		case KeyEvent.VK_RIGHT:
			drawPanel.moveImageAnimated(width/3,0);
			return;
		case KeyEvent.VK_MINUS:
			mZoomPerformer.addWheelEvent(1, new Point(drawPanel.getTileBox().getCenterPixelX(), drawPanel.getTileBox().getCenterPixelY()));
			mZoomTimer.restart();
			return;
		case KeyEvent.VK_PLUS:
			mZoomPerformer.addWheelEvent(-1, new Point(drawPanel.getTileBox().getCenterPixelX(), drawPanel.getTileBox().getCenterPixelY()));
			mZoomTimer.restart();
			return;
		}
	}

	@Override
	public void keyReleased(KeyEvent pE) {
	}
	
	@Override
	public void mouseMoved(MouseEvent pE) {
		setMouseEvent(pE);
	}

	public MouseEvent getMouseEvent() {
		return mMouseEvent;
	}

	public void setMouseEvent(MouseEvent pMouseEvent) {
		mMouseEvent = pMouseEvent;
	}
	
}