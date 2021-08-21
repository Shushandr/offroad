package net.osmand.plus.render;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.sourceforge.offroad.OsmWindow;

public class RenderingIcons {
	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);

	private static Map<String, BufferedImage> iconsBmp = new LinkedHashMap<String, BufferedImage>();

	public static byte[] getIconRawData(String s) {
		try {
			final InputStream inputStream = getIconStream(s);
			if (inputStream == null) return null;
			final ByteArrayOutputStream proxyOutputStream = new ByteArrayOutputStream(1024);
			final byte[] ioBuffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(ioBuffer)) >= 0) {
				proxyOutputStream.write(ioBuffer, 0, bytesRead);
			}
			inputStream.close();
			final byte[] bitmapData = proxyOutputStream.toByteArray();
			log.info("Icon data length is " + bitmapData.length); //$NON-NLS-1$
			// BufferedImage dm =
			// android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0,
			// bitmapData.length) ;
			// if(dm != null){
			// System.out.println("IC " + s +" " + dm.getHeight() + "x" +
			// dm.getWidth());
			// }
			// if(android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0,
			// bitmapData.length) == null)
			// throw new Exception();
			return bitmapData;
		} catch (Throwable e) {
			log.error("Failed to get byte stream from icon", e); //$NON-NLS-1$
			return null;
		}
	}

	public static BufferedImage getBigIcon(String s) {
		try {
			return ImageIO.read(getIconStream("mx_" + s));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static BufferedImage getIcon(String s, boolean includeShader) {
		if (s == null) {
			return null;
		}
		if (includeShader && iconsBmp.containsKey("h_" + s)) {
			return iconsBmp.get("h_" + s);
		}
		if (iconsBmp.containsKey(s)) {
			return iconsBmp.get(s);
		}
		try {
			InputStream stream = null;
			if (includeShader) stream = getIconStream("h_" + s);
			if (stream != null) s = "h_" + s;
			else stream = getIconStream(s);
			BufferedImage bmp = stream == null ? null : ImageIO.read(stream);
			iconsBmp.put(s, bmp);
		} catch (IOException e) {
			e.printStackTrace();
			iconsBmp.put(s, null);
		}
		return iconsBmp.get(s);
	}

	public static InputStream getIconStream(String s) throws IOException {
		OsmWindow context = OsmWindow.getInstance();
		s = "/" + context.getOsmandIconsDir() + s + ".png";
		InputStream res = OsmWindow.class.getResourceAsStream(s);
		return res;
	}

	public static void initIcons() {
		iconsBmp.clear();
	}

}
