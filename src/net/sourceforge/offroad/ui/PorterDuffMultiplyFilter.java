package net.sourceforge.offroad.ui;

import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class PorterDuffMultiplyFilter implements Composite {
	private Color mColor;

	public PorterDuffMultiplyFilter(int pColor) {
		this(new Color(pColor, true));
	}
	public PorterDuffMultiplyFilter(Color pColor) {
		setColor(pColor);
	}

	@Override
	public CompositeContext createContext(ColorModel pSrcColorModel, ColorModel pDstColorModel, RenderingHints pHints) {
		return new CompositeContext() {

			@Override
			public void dispose() {
			}

			@Override
			public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
	            int srcDT = src.getSampleModel().getDataType();
				if ((srcDT != DataBuffer.TYPE_INT && srcDT != DataBuffer.TYPE_BYTE) ||
	                    dstIn.getSampleModel().getDataType() != DataBuffer.TYPE_INT ||
	                    dstOut.getSampleModel().getDataType() != DataBuffer.TYPE_INT) {
	                    throw new IllegalStateException(
	                            "Source and destination must store pixels as INT/BYTE: " + srcDT + "," + dstIn.getSampleModel().getDataType() + "," + dstOut.getSampleModel().getDataType());
	                }
				int width = Math.min(src.getWidth(), dstIn.getWidth());
				int height = Math.min(src.getHeight(), dstIn.getHeight());
				int[] srcPixels = new int[width];
				byte[] srcPixelsByte = new byte[4*width];
				int[] dstPixels = new int[width];

				for (int y = 0; y < height; y++) {
					if (srcDT == DataBuffer.TYPE_INT) {
						src.getDataElements(0, y, width, 1, srcPixels);
					} else {
						src.getDataElements(0, y, width, 1, srcPixelsByte);
						for (int i = 0; i < srcPixels.length; i++) {
							srcPixels[i] = srcPixelsByte[i*4+2] 
									| srcPixelsByte[i*4+1] << 8
									| srcPixelsByte[i*4] << 16
									| srcPixelsByte[i*4+3] << 24
									;
						}
					}
					dstIn.getDataElements(0, y, width, 1, dstPixels);
					for (int x = 0; x < width; x++) {
						// pixels are stored as INT_ARGB
						if((srcPixels[x] & 0xFFFFFF) != 0) {
							// if there is a pixel, take the color of the arrow:
							dstPixels[x] = (getColor().getAlpha() & 0xFF) << 24 | (getColor().getRed() & 0xFF) << 16
									| (getColor().getGreen() & 0xFF) << 8 | getColor().getBlue() & 0xFF;
						} else {
							// don't change
						}
					}
					dstOut.setDataElements(0, y, width, 1, dstPixels);
				}
			}

		};
	}

	public Color getColor() {
		return mColor;
	}

	public void setColor(Color pColor) {
		mColor = pColor;
	}
}