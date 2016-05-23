package net.osmand.plus.views;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JMenu;

import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.MapAlgorithms;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.OsmBitmapPanel;

public abstract class OsmandMapLayer {
	private final static Log log = PlatformUtil.getLog(OsmandMapLayer.class);

	
	protected List<LatLon> fullObjectsLatLon;
	protected List<LatLon> smallObjectsLatLon;

	public abstract void initLayer(OsmBitmapPanel view);

	public abstract void onDraw(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings);

	public void onPrepareBufferImage(Graphics2D canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	public abstract void destroyLayer();

	public void onRetainNonConfigurationInstance(Map<String, Object> map) {
	}

	public void populateObjectContextMenu(LatLon latLon, Object o, JMenu adapter) {
	}

	public boolean onSingleTap(Point2D point, RotatedTileBox tileBox) {
		return false;
	}

	public boolean onLongPressEvent(Point2D point, RotatedTileBox tileBox) {
		return false;
	}

//	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
//		return false;
//	}
//
	public <Params> void executeTaskInBackground(Thread t) {
		t.run();
	}

	public boolean isPresentInFullObjects(LatLon latLon) {
		if (fullObjectsLatLon == null) {
			return true;
		} else if (latLon != null) {
			return fullObjectsLatLon.contains(latLon);
		}
		return false;
	}

	public boolean isPresentInSmallObjects(LatLon latLon) {
		if (smallObjectsLatLon != null && latLon != null) {
			return smallObjectsLatLon.contains(latLon);
		}
		return false;
	}

	/**
	 * This method returns whether canvas should be rotated as
	 * map rotated before {@link #onDraw(android.graphics.Canvas, net.osmand.data.RotatedTileBox, net.osmand.plus.views.OsmandMapLayer.DrawSettings)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public abstract boolean drawInScreenPixels();

	public static class DrawSettings {
		private final boolean nightMode;
		private final boolean updateVectorRendering;

		public DrawSettings(boolean nightMode) {
			this(nightMode, false);
		}

		public DrawSettings(boolean nightMode, boolean updateVectorRendering) {
			this.nightMode = nightMode;
			this.updateVectorRendering = updateVectorRendering;
		}

		public boolean isUpdateVectorRendering() {
			return updateVectorRendering;
		}

		public boolean isNightMode() {
			return nightMode;
		}
	}


	private boolean isIn(int x, int y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}

	public int calculateSplitPaths(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys,
								   TIntArrayList results) {
		int px = xs.get(0);
		int py = ys.get(0);
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int cnt = 0;
		boolean pin = isIn(px, py, 0, 0, w, h);
		Path2D path = null;
		for (int i = 1; i < xs.size(); i++) {
			int x = xs.get(i);
			int y = ys.get(i);
			boolean in = isIn(x, y, 0, 0, w, h);
			boolean draw = false;
			if (pin && in) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(x, y,
						px, py, 0, w, h, 0);
				if (intersection != -1) {
					draw = true;
				}
			}
			if (draw) {
				path = new Path2D.Double();
				results.add(px);
				results.add(py);
				results.add(x);
				results.add(y);
			}
			pin = in;
			px = x;
			py = y;
		}
		return cnt;
	}

	public int calculatePath(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys, Path2D pPath) {
		boolean start = false;
		int px = xs.get(0);
		int py = ys.get(0);
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int cnt = 0;
		boolean pin = isIn(px, py, 0, 0, w, h);
		for (int i = 1; i < xs.size(); i++) {
			int x = xs.get(i);
			int y = ys.get(i);
			boolean in = isIn(x, y, 0, 0, w, h);
			boolean draw = false;
			if (pin && in) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(x, y,
						px, py, 0, w, h, 0);
				if (intersection != -1) {
					px = (int) (intersection >> 32);
					py = (int) (intersection & 0xffffffff);
					draw = true;
				}
			}
			if (draw) {
				if (!start) {
					cnt++;
					pPath.moveTo(px, py);
				}
				pPath.lineTo(x, y);
				start = true;
			} else {
				start = false;
			}
			pin = in;
			px = x;
			py = y;
		}
		return cnt;
	}

	public QuadTree<QuadRect> initBoundIntersections(RotatedTileBox tileBox) {
		QuadRect bounds = new QuadRect(0, 0, tileBox.getPixWidth(), tileBox.getPixHeight());
		bounds.inset(-bounds.width() / 4, -bounds.height() / 4);
		return new QuadTree<>(bounds, 4, 0.6f);
	}

	public boolean intersects(QuadTree<QuadRect> boundIntersections, float x, float y, float width, float height) {
		List<QuadRect> result = new ArrayList<>();
		QuadRect visibleRect = calculateRect(x, y, width, height);
		boundIntersections.queryInBox(new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom), result);
		for (QuadRect r : result) {
			if (QuadRect.intersects(r, visibleRect)) {
				return true;
			}
		}
		boundIntersections.insert(visibleRect,
				new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom));
		return false;
	}

	public QuadRect calculateRect(float x, float y, float width, float height) {
		QuadRect rf;
		double left = x - width / 2.0d;
		double top = y - height / 2.0d;
		double right = left + width;
		double bottom = top + height;
		rf = new QuadRect(left, top, right, bottom);
		return rf;
	}

	public abstract class MapLayerData<T> {
		public int ZOOM_THRESHOLD = 1;
		public RotatedTileBox queriedBox;
		protected T results;
		protected Task currentTask;
		protected Task pendingTask;

		public RotatedTileBox getQueriedBox() {
			return queriedBox;
		}

		public T getResults() {
			return results;
		}

		public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
			return queriedData != null && queriedData.containsTileBox(newBox) && Math.abs(queriedData.getZoom() - newBox.getZoom()) <= ZOOM_THRESHOLD;
		}

		public void queryNewData(RotatedTileBox tileBox) {
			if (!queriedBoxContains(queriedBox, tileBox)) {
				Task ct = currentTask;
				if (ct == null || !queriedBoxContains(ct.getDataBox(), tileBox)) {
					RotatedTileBox original = tileBox.copy();
					RotatedTileBox extended = original.copy();
					extended.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
					Task task = new Task(original, extended);
					if (currentTask == null) {
						executeTaskInBackground(task);
					} else {
						pendingTask = task;
					}
				}

			}
		}

		public void layerOnPostExecute() {
		}

		public boolean isInterrupted() {
			return pendingTask != null;
		}

		protected abstract T calculateResult(RotatedTileBox tileBox);

		public class Task extends Thread {
			private RotatedTileBox dataBox;
			private RotatedTileBox requestedBox;

			public Task(RotatedTileBox requestedBox, RotatedTileBox dataBox) {
				this.requestedBox = requestedBox;
				this.dataBox = dataBox;
			}

			public RotatedTileBox getOriginalBox() {
				return requestedBox;
			}

			public RotatedTileBox getDataBox() {
				return dataBox;
			}

			@Override
			public void run() {
				if (queriedBoxContains(queriedBox, dataBox)) {
					return;
				}
				// FIXME: Correct?
				results = calculateResult(dataBox);
			}

			protected void onPreExecute() {
				currentTask = this;
			}

			protected void onPostExecute(T result) {
				if (result != null) {
					queriedBox = dataBox;
					results = result;
				}
				currentTask = null;
				if (pendingTask != null) {
					executeTaskInBackground(pendingTask);
					pendingTask = null;
				} else {
					layerOnPostExecute();
				}
			}
		}

		public void clearCache() {
			results = null;
			queriedBox = null;

		}

	}

	public BufferedImage readImage(String image, OsmBitmapPanel view) {
		return readImage(image, view.getContext());
	}
	
	public static BufferedImage readImage(String image, OsmWindow ctx) {
		try {
			InputStream resource = ctx.getResource(OsmWindow.IMAGE_PATH + image + ".png");
			if(resource == null){
				log.error("Resource " + image + " not found!");
				return null;
			}
			return ImageIO.read(resource);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
