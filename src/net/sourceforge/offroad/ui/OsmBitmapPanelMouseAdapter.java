package net.sourceforge.offroad.ui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.Timer;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.sourceforge.offroad.OsmWindow;

public class OsmBitmapPanelMouseAdapter extends MouseAdapter implements ComponentListener {
	private static final int ZOOM_DELAY_MILLISECONDS = 100;
	private static final int ROTATE_DELAY_MILLISECONDS = 500;
	private final static Log log = PlatformUtil.getLog(OsmBitmapPanelMouseAdapter.class);

	public class ZoomPerformer implements ActionListener {
		private int mCounter;
		private Point mPoint;

		public void addWheelEvent(int pWheelRotation, Point pPoint) {
			mCounter += pWheelRotation;
			mPoint = pPoint;
		}

		public void actionPerformed(ActionEvent evt) {
			log.info("Zoom action is fired");
			if(drawPanel.isZoomRunning()){
				log.info("Don't zoom as there is something running. Try later automatically.");
				mZoomTimer.restart();
			} else {
				drawPanel.zoomChange(-mCounter, mPoint);
				mCounter = 0;
			}
		}
	}

	private class RotatePerformer implements ActionListener {
		private double mDegrees;
		private Point mPoint;
		
		public void addWheelEvent(double pWheelRotation, Point pPoint) {
			mDegrees += pWheelRotation;
			mPoint = pPoint;
		}
		
		public void actionPerformed(ActionEvent evt) {
			drawPanel.rotateIncrement(mDegrees);
			mDegrees = 0;
		}
	}
	
	private OsmBitmapPanel drawPanel;
	private Point lastDragPoint;
	private Timer mZoomTimer;
	private ZoomPerformer mZoomPerformer;
	private Timer mRotateTimer;
	private RotatePerformer mRotatePerformer;
	private MouseEvent mMouseEvent;
	private OsmWindow mContext;

	public OsmBitmapPanelMouseAdapter(OsmBitmapPanel drawPanel) {
		this.drawPanel = drawPanel;
		mContext = drawPanel.getContext();
		mZoomPerformer = new ZoomPerformer();
		mZoomTimer = new Timer(ZOOM_DELAY_MILLISECONDS, mZoomPerformer);
		mZoomTimer.setRepeats(false);
		mRotatePerformer = new RotatePerformer();
		mRotateTimer = new Timer(ROTATE_DELAY_MILLISECONDS, mRotatePerformer);
		mRotateTimer.setRepeats(false);
	}

	public ZoomPerformer getZoomPerformer() {
		return mZoomPerformer;
	}
	
	public boolean isPopup(MouseEvent e) {
		return e.isPopupTrigger();
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		lastDragPoint = new Point(e.getPoint());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// no button is pressed in that event. !?!
		Point deltaPoint = lastDragPoint;
		if(deltaPoint == null){
			return;
		}
		deltaPoint.translate(-e.getPoint().x, -e.getPoint().y);
		lastDragPoint = new Point(e.getPoint());
		drawPanel.dragImage(deltaPoint);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(isPopup(e)){
			return;
		}
		drawPanel.drawLater();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent pE) {
		pE.consume();
		if(pE.isControlDown()){
			// rotate:
			mRotatePerformer.addWheelEvent(10*pE.getPreciseWheelRotation(), pE.getPoint());
			mRotateTimer.restart();
			drawPanel.directRotateIncrement(10*pE.getPreciseWheelRotation());
			return;
		}
//		drawPanel.directZoomIncrement(pE.getWheelRotation(), pE.getPoint());
		log.info("Wheel event detected: " + pE);
		mZoomPerformer.addWheelEvent(pE.getWheelRotation(), pE.getPoint());
		mZoomTimer.restart();
	}

	@Override
	public void componentResized(ComponentEvent pE) {
		log.info("Resize event received: " + pE);
		drawPanel.resizePanel();
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
		if(e.isShiftDown()){
			// do a polyline
			mContext.addPolylinePoint(e.getPoint());
		} else {
			mContext.setCursorPosition(e.getPoint());
		}
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

	public Timer getZoomTimer() {
		return mZoomTimer;
	}

	public void addWheelEvent(int pI, RotatedTileBox pCurrentTileBox) {
		mZoomPerformer.addWheelEvent(pI, new Point(pCurrentTileBox.getCenterPixelX(), pCurrentTileBox.getCenterPixelY()));
		mZoomTimer.restart();
		
	}
	
}