package net.osmand.plus.base;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.TreeMap;

import net.osmand.plus.views.FavoritesLayer;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.ui.ColorUtils;
import net.sourceforge.offroad.ui.Drawable;
import net.sourceforge.offroad.ui.PorterDuffMultiplyFilter;

public class FavoriteImageDrawable extends Drawable {

	private int color;
//	private Paint paintIcon;
//	private Paint paintBackground;
	private BufferedImage favIcon;
	private BufferedImage favBackground;
	private boolean withShadow;
//	private Paint paintOuter;
//	private Paint paintInnerCircle;
	private BufferedImage listDrawable;
	private Rectangle listDrawable_Bounds = new Rectangle();
	private OsmWindow mContext;
	private PorterDuffMultiplyFilter paintIcon_Composite;
	private PorterDuffMultiplyFilter paintBackground_Composite;

	public FavoriteImageDrawable(OsmWindow ctx, int color, boolean withShadow) {
		mContext = ctx;
		this.withShadow = withShadow;
		this.color = color;
//		paintBackground = new Paint();
//		paintBackground.setColorFilter(new PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY));
		int col = color == 0 ? FavoritesLayer.DEFAULT_COLOR : color;
		paintBackground_Composite = new PorterDuffMultiplyFilter(new Color(col, true));
//		paintIcon = new Paint();
		favIcon = ctx.readImage("map_favorite");
		favBackground = ctx.readImage("map_white_favorite_shield");
		listDrawable = ctx.readImage("ic_action_fav_dark");

//		paintOuter = new Paint();
//		paintOuter.setAntiAlias(true);
//		paintOuter.setStyle(Style.FILL_AND_STROKE);
//		paintInnerCircle = new Paint();
//		paintInnerCircle.setStyle(Style.FILL_AND_STROKE);
//		paintOuter.setColor(color == 0 || color == Color.BLACK ? 0x88555555 : color);
//		paintInnerCircle.setColor(color == 0 || color == Color.BLACK ? getResources().getColor(R.color.color_favorite)
//				: color);
//		paintInnerCircle.setAntiAlias(true);
	}
	
	@Override
	protected void onBoundsChange(Rectangle bounds) {
		super.onBoundsChange(bounds);
		
		if (!withShadow) {
			Rectangle bs = new Rectangle(bounds);
			 //bs.inset((int) (4 * density), (int) (4 * density));
			bs.grow(bs.width / 4, bs.height / 4);
			listDrawable_Bounds = bs;
		}
	}

	@Override
	public int getIntrinsicHeight() {
		return favBackground.getHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return favBackground.getWidth();
	}

	public int getColor() {
		return color;
	}

	@Override
	public void draw(Graphics2D canvas) {
		Rectangle bs = getBounds();
		Graphics2D graphics = mContext.getDrawPanel().createGraphics(canvas);
		if(withShadow) {
			graphics.setComposite(paintBackground_Composite);
			graphics.drawImage(favBackground, (int)(bs.getCenterX() - favBackground.getWidth() / 2f), (int)(bs.getCenterY() - favBackground.getHeight() / 2f), null); //paintBackground
			if (paintIcon_Composite != null) {
				graphics.setComposite(paintIcon_Composite);
			} else {
				graphics.dispose();
				graphics = mContext.getDrawPanel().createGraphics(canvas);
			}
			graphics.drawImage(favIcon, (int)(bs.getCenterX() - favIcon.getWidth() / 2f), (int)(bs.getCenterY() - favIcon.getHeight() / 2f), null); // paintIcon
		} else {
			int min = Math.min(bs.width, bs.height);
			int r = (min * 4 / 10);
			int rs = (r - 1);
			graphics.setColor(ColorUtils.create(color == 0 ? 0x88555555 : color));
			graphics.drawOval(min / 2, min / 2, r, r); // paintOuter
			graphics.setColor(ColorUtils.create(color == 0 ? FavoritesLayer.DEFAULT_COLOR : color));
			graphics.drawOval(min / 2, min / 2, rs, rs); // paintInnerCircle
			canvas.drawImage(listDrawable, listDrawable_Bounds.x, listDrawable_Bounds.y, listDrawable_Bounds.width, listDrawable_Bounds.height, null);
//			listDrawable.draw(canvas);
		}
		graphics.dispose();
	}

	public void drawBitmapInCenter(Graphics2D canvas, int x, int y) {
		int dx = x - getIntrinsicWidth() / 2;
		int dy = y - getIntrinsicHeight() / 2;
		canvas.translate(dx, dy);
		draw(canvas);
		canvas.translate(-dx, -dy);
	}

	public void drawBitmapInCenter(Graphics2D canvas, float x, float y) {
		float dx = x - getIntrinsicWidth() / 2f;
		float dy = y - getIntrinsicHeight() / 2f;
		canvas.translate(dx, dy);
		draw(canvas);
		canvas.translate(-dx, -dy);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {
		Color origC = paintBackground_Composite.getColor();
		Color newColor = new Color(origC.getRed(), origC.getGreen(), origC.getBlue(), alpha);
		paintBackground_Composite.setColor(newColor);
	}

	@Override
	public void setColorFilter(Composite cf) {
		paintIcon_Composite = (PorterDuffMultiplyFilter) cf;
	}

	private static TreeMap<Integer, FavoriteImageDrawable> cache = new TreeMap<>();

	public static FavoriteImageDrawable getOrCreate(OsmWindow a, int color, boolean withShadow) {
		color = color | 0xff000000;
		int hash = (color << 2) + (withShadow ? 1 : 0);
		FavoriteImageDrawable drawable = cache.get(hash);
		if (drawable == null) {
			drawable = new FavoriteImageDrawable(a, color, withShadow);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			cache.put(hash, drawable);
		}
		return drawable;
	}

}
