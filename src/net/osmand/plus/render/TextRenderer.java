package net.osmand.plus.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.render.OsmandRenderer.TextInfo;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.sf.junidecode.Junidecode;
import net.sourceforge.offroad.TextStroke;
import net.sourceforge.offroad.ui.ColorUtils;

public class TextRenderer {
	private static final int MINIMAL_DISTANCE_BETWEEN_SHIELDS_IN_PIXEL = 200;
	private final static Log log = PlatformUtil.getLog(TextRenderer.class);


//	private Paint paintText;
//	private Paint paintIcon;
//	private Typeface defaultTypeface;
//	private Typeface boldItalicTypeface;
//	private Typeface italicTypeface;
//	private Typeface boldTypeface;

	static class TextDrawInfo {

		public TextDrawInfo(String text) {
			this.text = text;
		}

		String text = null;
		Path2D drawOnPath = null;
		QuadRect bounds = null;
		float vOffset = 0;
		float centerX = 0;
		float pathRotate = 0;
		float centerY = 0;
		float textSize = 0;
		float minDistance = 0;
		int textColor = Color.BLACK.getRGB();
		int textShadow = 0;
		int textWrap = 0;
		boolean bold = false;
		boolean italic = false;
		String shieldRes = null;
		String shieldResIcon = null;
		int textOrder = 100;
		int textShadowColor = Color.WHITE.getRGB();

		public void fillProperties(RenderingContext rc, RenderingRuleSearchRequest render, float centerX, float centerY) {
			this.centerX = centerX;
			// used only for draw on path where centerY doesn't play role
			this.vOffset = (int) rc.getComplexValue(render, render.ALL.R_TEXT_DY);
			this.centerY = centerY + this.vOffset;
			textColor = render.getIntPropertyValue(render.ALL.R_TEXT_COLOR);
			if (textColor == 0) {
				textColor = Color.BLACK.getRGB();
			}
			textSize = rc.getComplexValue(render, render.ALL.R_TEXT_SIZE) ;
			textShadow = (int) rc.getComplexValue(render, render.ALL.R_TEXT_HALO_RADIUS);
			textShadowColor = render.getIntPropertyValue(render.ALL.R_TEXT_HALO_COLOR);
			if(textShadowColor == 0) {
				textShadowColor = Color.WHITE.getRGB();
			}
			textWrap = (int) rc.getComplexValue(render, render.ALL.R_TEXT_WRAP_WIDTH);
			bold = render.getIntPropertyValue(render.ALL.R_TEXT_BOLD, 0) > 0;
			italic = render.getIntPropertyValue(render.ALL.R_TEXT_ITALIC, 0) > 0;
			minDistance = rc.getComplexValue(render, render.ALL.R_TEXT_MIN_DISTANCE);
			if (render.isSpecified(render.ALL.R_TEXT_SHIELD)) {
				shieldRes = render.getStringPropertyValue(render.ALL.R_TEXT_SHIELD);
			}
			if (render.isSpecified(render.ALL.R_ICON)) {
				shieldResIcon = render.getStringPropertyValue(render.ALL.R_ICON);
			}
			textOrder = render.getIntPropertyValue(render.ALL.R_TEXT_ORDER, 100);
		}
	}

	public TextRenderer() {
//		paintText = new Paint();
//		paintText.setStyle(Style.FILL);
//		paintText.setStrokeWidth(1);
//		paintText.setColor(Color.BLACK);
//		paintText.setTextAlign(Align.CENTER);
//		defaultTypeface = Typeface.create("Droid Serif", Typeface.NORMAL);
//		boldItalicTypeface = Typeface.create("Droid Serif", Typeface.BOLD_ITALIC);
//		italicTypeface = Typeface.create("Droid Serif", Typeface.ITALIC);
//		boldTypeface = Typeface.create("Droid Serif", Typeface.BOLD);
//		paintText.setTypeface(defaultTypeface); //$NON-NLS-1$
//		paintText.setAntiAlias(true);
//
//		paintIcon = new Paint();
//		paintIcon.setStyle(Style.STROKE);
		
	}

	private double sqr(double a) {
		return a * a;
	}
	
	private double fsqr(double pD) {
		return pD * pD;
	}

	boolean intersects(QuadRect tRect, float tRot, QuadRect sRect, float sRot) {
		if (Math.abs(tRot) < Math.PI / 15 && Math.abs(sRot) < Math.PI / 15) {
			return QuadRect.intersects(tRect, sRect);
		}
		double dist = Math.sqrt(sqr(tRect.centerX() - sRect.centerX()) + sqr(tRect.centerY() - sRect.centerY()));
		if (dist < MINIMAL_DISTANCE_BETWEEN_SHIELDS_IN_PIXEL) {
			return true;
		}

		// difference close to 90/270 degrees
		if (Math.abs(Math.cos(tRot - sRot)) < 0.3) {
			// rotate one rectangle to 90 degrees
			tRot += Math.PI / 2;
			double l = tRect.centerX() - tRect.height() / 2;
			double t = tRect.centerY() - tRect.width() / 2;
			tRect = new QuadRect(l, t, l + tRect.height(), t + tRect.width());
		}

		// determine difference close to 180/0 degrees
		if (Math.abs(Math.sin(tRot - sRot)) < 0.3) {
			// rotate t box
			// (calculate offset for t center suppose we rotate around s center)
			float diff = (float) (-Math.atan2(tRect.centerX() - sRect.centerX(), tRect.centerY() - sRect.centerY()) + Math.PI / 2);
			diff -= sRot;
			double left = sRect.centerX() + dist * Math.cos(diff) - tRect.width() / 2;
			double top = sRect.centerY() - dist * Math.sin(diff) - tRect.height() / 2;
			QuadRect nRect = new QuadRect(left, top, left + tRect.width(), top + tRect.height());
			return QuadRect.intersects(nRect, sRect);
		}

		// TODO other cases not covered
		return QuadRect.intersects(tRect, sRect);
	}

	void drawTestBox(Graphics2D pGraphics2d, Rectangle2D r, float rot, String text) {
		Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();

//		cv.save();
		newGraphics.translate(r.getCenterX(), r.getCenterY());
		newGraphics.rotate((float) (rot * 180 / Math.PI));
		Rectangle2D rs = new Rectangle2D.Double(-r.getWidth() / 2, -r.getHeight() / 2, r.getWidth() / 2, r.getHeight() / 2);
		newGraphics.drawRect((int)rs.getX(), (int)rs.getY(), (int)rs.getWidth(), (int)rs.getHeight());
		if (text != null) {
//			paintText.setTextSize(paintText.getTextSize() - 4);
//			System.out.println("Text " + text+ "; c=( " + rs.getCenterX() + ", " + rs.getCenterY() + ")");
			newGraphics.drawString(text, (int)rs.getCenterX(), (int)rs.getCenterY());
//			paintText.setTextSize(paintText.getTextSize() + 4);
		}
//		cv.restore();
		newGraphics.dispose();
	}

	List<TextDrawInfo> tempSearch = new ArrayList<TextDrawInfo>();

	private boolean findTextIntersection(Graphics2D pGraphics2d, RenderingContext rc, QuadTree<TextDrawInfo> boundIntersections, TextDrawInfo text) {
		// for test purposes
//		drawTestBox(cv, text.bounds, text.pathRotate, text.text);
		boundIntersections.queryInBox(text.bounds, tempSearch);
		for (int i = 0; i < tempSearch.size(); i++) {
			TextDrawInfo t = tempSearch.get(i);
			if (intersects(text.bounds, text.pathRotate, t.bounds, t.pathRotate)) {
				return true;
			}
		}
		if (text.minDistance > 0) {
			QuadRect boundsSearch = new QuadRect(text.bounds);
			boundsSearch.inset(-Math.max(rc.getDensityValue(5.0f), text.minDistance), -rc.getDensityValue(15));
			boundIntersections.queryInBox(boundsSearch, tempSearch);
			// drawTestBox(cv, &boundsSearch, text.pathRotate, paintIcon, text.text, NULL/*paintText*/);
			for (int i = 0; i < tempSearch.size(); i++) {
				TextDrawInfo t = tempSearch.get(i);
				if (t.minDistance > 0 && t.text.equals(text.text) &&
						intersects(boundsSearch, text.pathRotate, t.bounds, t.pathRotate)) {
					return true;
				}
			}
		}
		boundIntersections.insert(text, text.bounds);
		return false;
	}

	private void drawTextOnCanvas(Graphics2D pGraphics2d, String text, float centerX, float centerY, int shadowColor, float textShadow) {
		Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();
		centerX -= newGraphics.getFontMetrics().stringWidth(text)/2;
		if (textShadow > 0) {
			Color c = newGraphics.getColor();
//			paintText.setStyle(Style.STROKE);
			newGraphics.setColor(createColor(shadowColor));
			newGraphics.setStroke(new BasicStroke(2 + textShadow));
			newGraphics.drawString(text, centerX, centerY);
			// reset
			newGraphics.setStroke(new BasicStroke(2f));
//			paintText.setStrokeWidth(2);
//			paintText.setStyle(Style.FILL);
//			paintText.setColor(c);
			newGraphics.setColor(c);
		}
		newGraphics.drawString(text, centerX, centerY);
		newGraphics.dispose();
	}

	public void drawTextOverCanvas(RenderingContext rc, Graphics2D pGraphics2d, String preferredLocale) {
		int size = rc.textToDraw.size();
		Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();

		// 1. Sort text using text order
		Collections.sort(rc.textToDraw, new Comparator<TextDrawInfo>() {
			@Override
			public int compare(TextDrawInfo object1, TextDrawInfo object2) {
				return object1.textOrder - object2.textOrder;
			}
		});
		QuadRect r = new QuadRect(0, 0, rc.width, rc.height);
		r.inset(-100, -100);
		QuadTree<TextDrawInfo> nonIntersectedBounds = new QuadTree<TextDrawInfo>(r, 4, 0.6f);

		for (int i = 0; i < size; i++) {
			TextDrawInfo text = rc.textToDraw.get(i);
			if (text.text != null && text.text.length() > 0) {
				if (preferredLocale.length() > 0) {
					text.text = Junidecode.unidecode(text.text);
				}

				// sest text size before finding intersection (it is used there)
				float textSize = text.textSize * rc.textScale;
				int fontStyle = 0;
				if(text.bold && text.italic) {
					fontStyle = Font.BOLD | Font.ITALIC;
				} else if(text.bold) {
					fontStyle = Font.BOLD;
				} else if(text.italic) {
					fontStyle = Font.ITALIC;
				} else {
					fontStyle = Font.PLAIN;
				}
				Font textFont = newGraphics.getFont().deriveFont(fontStyle, textSize);
				newGraphics.setFont(textFont);
//				paintText.setFakeBoldText(text.bold);
				
				newGraphics.setColor(createColor(text.textColor));
				// align center y
				FontMetrics metr = newGraphics.getFontMetrics();
				text.centerY += - metr.getAscent();

				// calculate if there is intersection
				boolean intersects = findTextIntersection(newGraphics, rc, nonIntersectedBounds, text);
				if (!intersects) {
					if (text.drawOnPath != null) {
						float vOffset = text.vOffset - ( metr.getAscent()/2 + metr.getDescent());
						if (text.textShadow > 0) {
							newGraphics.setColor(createColor(text.textShadowColor));
							newGraphics.setStroke(new TextStroke(text.text, textFont, false, false, -vOffset));
							newGraphics.draw(text.drawOnPath);
						}
						newGraphics.setColor(createColor(text.textColor));
						newGraphics.setStroke(new TextStroke(text.text, textFont, false, false, -vOffset));
						newGraphics.draw(text.drawOnPath);
						newGraphics.setStroke(new BasicStroke(0f));
					} else {
						drawShieldIcon(rc, newGraphics, text, text.shieldRes);
						drawShieldIcon(rc, newGraphics, text, text.shieldResIcon);

						drawWrappedText(newGraphics, text, textSize);
					}
				}
			}
		}
		newGraphics.dispose();
	}

	public Color createColor(int colorInt) {
		return ColorUtils.create(colorInt);
	}

	private void drawShieldIcon(RenderingContext rc, Graphics2D pGraphics2d, TextDrawInfo text, String sr) {
		if (sr != null) {
			float coef = rc.getDensityValue(rc.screenDensityRatio * rc.textScale);
			BufferedImage ico = RenderingIcons.getIcon(sr, true);
			if (ico != null) {
				log.debug("Got shield icon " + sr + ":" + ico.getWidth()+"x" + ico.getHeight());
				float left = text.centerX - ico.getWidth() / 2 * coef - 0.5f;
				float top = text.centerY - ico.getHeight() / 2 * coef - pGraphics2d.getFontMetrics().getDescent() - 0.5f;
				if(rc.screenDensityRatio != 1f){
					Rectangle2D rf = new Rectangle2D.Float(left, top, left + ico.getWidth() * coef, 
							top + ico.getHeight() * coef);
					Rectangle2D src = new Rectangle2D.Float(0, 0, ico.getWidth(), ico
							.getHeight());
					pGraphics2d.drawImage(ico, (int) rf.getX(), (int) rf.getY(), (int) rf.getMaxX(), (int) rf.getMaxY(),
							(int) src.getMinX(), (int) src.getMinY(), (int) src.getMaxX(), (int) src.getMaxY(), null);
				} else {
					pGraphics2d.drawImage(ico, (int)left, (int)top, null);
				}
			}
		}
	}

	private void drawWrappedText(Graphics2D pGraphics2d, TextDrawInfo text, float textSize) {
		if (text.textWrap == 0) {
			// set maximum for all text
			text.textWrap = 40;
		}

		if (text.text.length() > text.textWrap) {
			int start = 0;
			int end = text.text.length();
			int lastSpace = -1;
			int line = 0;
			int pos = 0;
			int limit = 0;
			while (pos < end) {
				lastSpace = -1;
				limit += text.textWrap;
				while (pos < limit && pos < end) {
					if (!Character.isLetterOrDigit(text.text.charAt(pos))) {
						lastSpace = pos;
					}
					pos++;
				}
				if (lastSpace == -1 || pos == end) {
					drawTextOnCanvas(pGraphics2d, text.text.substring(start, pos), text.centerX, text.centerY + line * (textSize + 2), 
							text.textShadowColor, text.textShadow);
					start = pos;
				} else {
					drawTextOnCanvas(pGraphics2d, text.text.substring(start, lastSpace), text.centerX, text.centerY + line * (textSize + 2),
							text.textShadowColor, text.textShadow);
					start = lastSpace + 1;
					limit += (start - pos) - 1;
				}
				line++;

			}
		} else {
			drawTextOnCanvas(pGraphics2d, text.text, text.centerX, text.centerY, text.textShadowColor, text.textShadow);
		}
	}
	
	private void createTextDrawInfo(final BinaryMapDataObject o, RenderingRuleSearchRequest render, Graphics2D pGraphics2d, RenderingContext rc, TagValuePair pair, final double xMid, double yMid,
			Path2D pPath, final Point2D[] points, String name, String tagName) {
		render.setInitialTagValueZoom(pair.tag, pair.value, rc.zoom, o);
		render.setIntFilter(render.ALL.R_TEXT_LENGTH, name.length());
		render.setStringFilter(render.ALL.R_NAME_TAG, tagName);
		if(render.search(RenderingRulesStorage.TEXT_RULES)){
			if(render.getFloatPropertyValue(render.ALL.R_TEXT_SIZE) > 0){
				final TextDrawInfo text = new TextDrawInfo(name);
				text.fillProperties(rc, render, (float)xMid, (float)yMid);
				final String tagName2 = render.getStringPropertyValue(render.ALL.R_NAME_TAG2);
				if (!Algorithms.isEmpty(tagName2)) {
					o.getObjectNames().forEachEntry(new TIntObjectProcedure<String>() {
						@Override
						public boolean execute(int tagid, String nname) {
							String tagNameN2 = o.getMapIndex().decodeType(tagid).tag;
							if (tagName2.equals(tagNameN2)) {
								if (nname != null && nname.trim().length() > 0 && !name.equals(nname)) {
									text.text += " (" + nname +")";
								}
								return false;
							}
							return true;
						}
					});

				}
//				paintText.setTextSize(text.textSize);
				Graphics2D newGraphics = (Graphics2D) pGraphics2d.create();
				float textSize = text.textSize * rc.textScale;
				int fontStyle = 0;
				if(text.bold && text.italic) {
					fontStyle = Font.BOLD | Font.ITALIC;
				} else if(text.bold) {
					fontStyle = Font.BOLD;
				} else if(text.italic) {
					fontStyle = Font.ITALIC;
				} else {
					fontStyle = Font.PLAIN;
				}
				Font textFont = newGraphics.getFont().deriveFont(fontStyle, textSize);
				newGraphics.setFont(textFont);
				FontMetrics metr = newGraphics.getFontMetrics();
				int stringWidth = metr.stringWidth(name);
				int stringHeight = metr.getHeight();
				text.bounds = new QuadRect(0, 0, stringWidth, stringHeight);
				text.bounds.inset(-rc.getDensityValue(3), -rc.getDensityValue(10));
				boolean display = true;
				if(pPath != null) {
					text.drawOnPath = pPath;
					display = calculatePathToRotate(rc, text, points,
							render.getIntPropertyValue(render.ALL.R_TEXT_ON_PATH, 0) != 0);
				}
				if(text.drawOnPath == null) {
					text.bounds.offset(text.centerX, text.centerY);
					// shift to match alignment
					text.bounds.offset(-text.bounds.width()/2, - text.bounds.height()/2);
				} else {
					text.bounds.offset(text.centerX - text.bounds.width()/2, text.centerY - text.bounds.height()/2);
				}
				if(display) {
					TextInfo info = new TextInfo();
					info.mText = text.text;
					info.path = (text.drawOnPath!=null)?new Path2D.Double(text.drawOnPath):text.bounds.toPath2D();
					rc.result.effectiveTextObjects.add(info);
					rc.textToDraw.add(text);
				}
			}
		}
	}
	
	public void renderText(final BinaryMapDataObject obj, final RenderingRuleSearchRequest render, final Graphics2D pGraphics2d, final RenderingContext rc, 
			final TagValuePair pair, final double xMid, final double yMid, final Path2D pPath, final Point2D[] points) {
		final TIntObjectHashMap<String> map = obj.getObjectNames();
		if (map != null) {
			// find preferred language key (first the given language, then English, then the rest)
			Integer langTagL = obj.getMapIndex().getRule("name:" + rc.preferredLocale, null);
			boolean langContainedL = langTagL != null && map.containsKey(langTagL);
			if(!langContainedL){
				langTagL = obj.getMapIndex().getRule("name:en", null);
				langContainedL = langTagL != null && map.containsKey(langTagL);
			}
			int nameEncodingType = obj.getMapIndex().nameEncodingType;
			// assign final variables:
			boolean langContained = langContainedL;
			int langTag = (langContained)?langTagL:nameEncodingType;
			map.forEachEntry(new TIntObjectProcedure<String>() {
				@Override
				public boolean execute(int tag, String name) {
					if (name != null && name.trim().length() > 0) {
						if (langContained && tag == nameEncodingType) {
							return true;
						} 
						String nameTag = obj.getMapIndex().decodeType(tag).tag;
						boolean skip = false;
						if(nameTag.startsWith("name")) {
							if (tag != langTag) {
								skip = true;
							} else {
								nameTag = "";
							}
						}
						if(!skip) {
//							System.out.println("tag: " + nameTag + " (" + tag + "), name="+name + ", isName="+isName);
							createTextDrawInfo(obj, render, pGraphics2d, rc, pair, xMid, yMid, pPath, points, name, nameTag);
						}
					}
					return true;
				}
			});
		}
	}

	
	boolean calculatePathToRotate(RenderingContext rc, TextDrawInfo p, Point2D[] points, boolean drawOnPath) {
		int len = points.length;
		if (!drawOnPath) {
			p.drawOnPath = null;
			// simply calculate rotation of path used for shields
			float px = 0;
			float py = 0;
			for (int i = 1; i < len; i++) {
				px += points[i].getX() - points[i - 1].getX();
				py += points[i].getY() - points[i - 1].getY();
			}
			if (px != 0 || py != 0) {
				p.pathRotate = (float) (-Math.atan2(px, py) + Math.PI / 2);
			}
			return true;
		}

		boolean inverse = false;
		float roadLength = 0;
		boolean prevInside = false;
		float visibleRoadLength = 0;
		float textw = (float) p.bounds.width();
		int last = 0;
		int startVisible = 0;
		float[] distances = new float[points.length - 1];

		float normalTextLen = 1.5f * textw;
		for (int i = 0; i < len; i++, last++) {
			boolean inside = points[i].getX() >= 0 && points[i].getX() <= rc.width &&
					points[i].getY() >= 0 && points[i].getY() <= rc.height;
			if (i > 0) {
				float d = (float) Math.sqrt(fsqr(points[i].getX() - points[i - 1].getX()) + 
						fsqr(points[i].getY() - points[i - 1].getY()));
				distances[i-1]= d;
				roadLength += d;
				if(inside) {
					visibleRoadLength += d;
					if(!prevInside) {
						startVisible = i - 1;
					}
				} else if(prevInside) {
					if(visibleRoadLength >= normalTextLen) {
						break;
					}
					visibleRoadLength = 0;
				}

			}
			prevInside = inside;
		}
		if (textw >= roadLength) {
			return false;
		}
		int startInd = 0;
		int endInd = len;

		if(textw < visibleRoadLength &&  last - startVisible > 1) {
			startInd = startVisible;
			endInd = last;
			// display long road name in center
			if (visibleRoadLength > 3 * textw) {
				boolean ch ;
				do {
					ch = false;
					if(endInd - startInd > 2 && visibleRoadLength - distances[startInd] > normalTextLen){
						visibleRoadLength -= distances[startInd];
						startInd++;
						ch = true;
					}
					if(endInd - startInd > 2 && visibleRoadLength - distances[endInd - 2] > normalTextLen){
						visibleRoadLength -= distances[endInd - 2];
						endInd--;
						ch = true;
					}
				} while(ch);
			}
		}
		// shrink path to display more text
		if (startInd > 0 || endInd < len) {
			// find subpath
			Path2D path = new Path2D.Double(); 
			for (int i = startInd; i < endInd; i++) {
				if (i == startInd) {
					path.moveTo(points[i].getX(), points[i].getY());
				} else {
					path.lineTo(points[i].getX(), points[i].getY());
				}
			}
			p.drawOnPath = path;
		}
		// calculate vector of the road (px, py) to proper rotate it
		float px = 0;
		float py = 0;
		for (int i = startInd + 1; i < endInd; i++) {
			px += points[i].getX() - points[i - 1].getX();
			py += points[i].getY() - points[i - 1].getY();
		}
		float scale = 0.5f;
		float plen = (float) Math.sqrt(px * px + py * py);
		// vector ox,oy orthogonal to px,py to measure height
		float ox = -py;
		float oy = px;
		if(plen > 0) {
			float rot = (float) (-Math.atan2(px, py) + Math.PI / 2);
			if (rot < 0) rot += Math.PI * 2;
			if (rot > Math.PI / 2f && rot < 3 * Math.PI / 2f) {
				rot += Math.PI;
				inverse = true;
				ox = -ox;
				oy = -oy;
			}
			p.pathRotate = rot;
			ox *= (p.bounds.height() / plen) / 2;
			oy *= (p.bounds.height() / plen) / 2;
		}

		p.centerX = (float) (points[startInd].getX() + scale * px + ox);
		p.centerY = (float) (points[startInd].getY() + scale * py + oy);
//		p.hOffset = 0;

		if (inverse) {
			Path2D path = new Path2D.Double();
			for (int i = endInd - 1; i >= startInd; i--) {
				if (i == endInd - 1) {
					path.moveTo(points[i].getX(), points[i].getY());
				} else {
					path.lineTo(points[i].getX(), points[i].getY());
				}
			}
			p.drawOnPath = path;
		}
		return true;
	}


}
