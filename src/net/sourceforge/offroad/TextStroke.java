package net.sourceforge.offroad;

import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Taken from http://www.coderanch.com/t/507855/GUI/java/Drawing-text-line
 * 
 * @date 31.03.2016
 */
public class TextStroke implements Stroke {
	private String text;
	private Font font;
	private boolean stretchToFit = false;
	private boolean repeat = false;
	private AffineTransform t = new AffineTransform();
	private boolean rotate = false;

	private static final float FLATNESS = 1;

	public TextStroke(String text, Font font, boolean rotate) {
		this(text, font, false, false, rotate);
	}

	public TextStroke(String text, Font font, boolean stretchToFit, boolean repeat, boolean rotate) {
		this.font = font;
		this.stretchToFit = stretchToFit;
		this.repeat = repeat;
		this.rotate = rotate;
		if (rotate) {
			this.text = inverseString(text);
		} else {
			this.text = text;
		}
	}

	private String inverseString(String s) {
		String newString = "";
		int l = s.length();

		for (int i = l - 1; i >= 0; i--) {
			newString += s.charAt(i);
		}

		System.out.println("Inverted string: " + newString);
		return newString;
	}

	public Shape createStrokedShape(Shape shape) {
		Rectangle2D rect = shape.getBounds2D();
		int end = (int) (rect.getX() + rect.getWidth());
		System.out.println("Start r: " + rect.getX() + ", end r: " + end);
		FontRenderContext frc = new FontRenderContext(null, true, true);
		GlyphVector glyphVector = font.createGlyphVector(frc, text);

		GeneralPath result = new GeneralPath();
		PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
		float points[] = new float[6];
		float moveX = 0, moveY = 0;
		float lastX = 0, lastY = 0;
		float thisX = 0, thisY = 0;
		int type = 0;
		boolean first = false;
		float next = 0; // next character's advance along baseline
		int currentChar = 0;
		int glyphCount = glyphVector.getNumGlyphs();

		if (glyphCount == 0)
			return result;

		float factor = stretchToFit ? measurePathLength(shape) / (float) glyphVector.getLogicalBounds().getWidth()
				: 1.0f;
		float nextAdvance = 0;

		// Move start of drawing glyphs
		float pathLength = measurePathLength(shape);
		float pathFragmentNoText = pathLength - (float) glyphVector.getLogicalBounds().getWidth() - 5.00f;
		float lengthBehind = 0;

		while (lengthBehind < pathFragmentNoText && !it.isDone()) {
			type = it.currentSegment(points);
			switch (type) {
			case PathIterator.SEG_MOVETO:
				moveX = lastX = points[0];
				moveY = lastY = points[1];
				result.moveTo(moveX, moveY);
				first = true;
				nextAdvance = glyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
				next = nextAdvance;
				break;

			case PathIterator.SEG_CLOSE:
				points[0] = moveX;
				points[1] = moveY;
				// Fall into....

			case PathIterator.SEG_LINETO:
				thisX = points[0];
				thisY = points[1];
				float dx = thisX - lastX;
				float dy = thisY - lastY;
				lengthBehind += (float) Math.sqrt(dx * dx + dy * dy);

				first = false;
				lastX = thisX;
				lastY = thisY;

				break;
			}
			it.next();
		}

		while (currentChar < glyphCount && !it.isDone()) {
			type = it.currentSegment(points);
			switch (type) {
			case PathIterator.SEG_MOVETO:
				moveX = lastX = points[0];
				moveY = lastY = points[1];
				result.moveTo(moveX, moveY);
				first = true;
				nextAdvance = glyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
				next = nextAdvance;
				break;

			case PathIterator.SEG_CLOSE:
				points[0] = moveX;
				points[1] = moveY;
				// Fall into....

			case PathIterator.SEG_LINETO:
				thisX = points[0];
				thisY = points[1];
				float dx = thisX - lastX; // advance in x for this segment
				float dy = thisY - lastY; // increase in y for this segment
				float distance = (float) Math.sqrt(dx * dx + dy * dy); // length
																		// of
																		// the
																		// segment
				if (distance >= next) { // check if segment length is more than
										// next character's width
					float r = 1.0f / distance; // invert of the length of the
												// segment
					float angle = (float) Math.atan2(dy, dx); // angle of the
																// segment's
																// line
					while (currentChar < glyphCount && distance >= next) { // while
																			// there's
																			// a
																			// space
																			// for
																			// next
																			// character
						Shape glyph = glyphVector.getGlyphOutline(currentChar);
						Point2D p = glyphVector.getGlyphPosition(currentChar);
						float px = (float) p.getX(); // x position of current
														// letter relative to
														// string's origin
						float py = (float) p.getY(); // y position of current
														// letter relative to
														// string's origin
						float x = lastX + next * dx * r; // x of previous point
															// in the path +
						// (next character's advance along baseline + advance in
						// x for this segment)/invert of the length of the
						// segment)
						float y = lastY + next * dy * r;
						float advance = nextAdvance;
						nextAdvance = currentChar < glyphCount - 1
								? glyphVector.getGlyphMetrics(currentChar + 1).getAdvance() * 0.5f : 0;
						t.setToTranslation(x, y);
						if (rotate) {
							t.rotate(angle);
							t.rotate(Math.toRadians(180));
						} else {
							t.rotate(angle);
						}
						t.translate(-px - advance, -py);

						result.append(t.createTransformedShape(glyph), false);
						next += (advance + nextAdvance) * factor;
						currentChar++;
						if (repeat)
							currentChar %= glyphCount;
					}
				}
				next -= distance;
				first = false;
				lastX = thisX;
				lastY = thisY;
				break;
			}
			it.next();
		}

		return result;
	}

	public float measurePathLength(Shape shape) {
		PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
		float points[] = new float[6];
		float moveX = 0, moveY = 0;
		float lastX = 0, lastY = 0;
		float thisX = 0, thisY = 0;
		int type = 0;
		float total = 0;

		while (!it.isDone()) {
			type = it.currentSegment(points);
			switch (type) {
			case PathIterator.SEG_MOVETO:
				moveX = lastX = points[0];
				moveY = lastY = points[1];
				break;

			case PathIterator.SEG_CLOSE:
				points[0] = moveX;
				points[1] = moveY;
				// Fall into....

			case PathIterator.SEG_LINETO:
				thisX = points[0];
				thisY = points[1];
				float dx = thisX - lastX;
				float dy = thisY - lastY;
				total += (float) Math.sqrt(dx * dx + dy * dy);
				lastX = thisX;
				lastY = thisY;
				break;
			}
			it.next();
		}

		return total;
	}

}
