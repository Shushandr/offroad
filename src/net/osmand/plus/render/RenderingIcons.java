package net.osmand.plus.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

	private static Map<String, JarEntry> shaderIcons = new LinkedHashMap<String, JarEntry>();
	private static Map<String, JarEntry> smallIcons = new LinkedHashMap<String, JarEntry>();
	private static Map<String, JarEntry> bigIcons = new LinkedHashMap<String, JarEntry>();
	private static Map<String, BufferedImage> iconsBmp = new LinkedHashMap<String, BufferedImage>();
	// private static DisplayMetrics dm;

	public static boolean containsSmallIcon(String s) {
		return smallIcons.containsKey(s);
	}

	public static boolean containsBigIcon(String s) {
		return bigIcons.containsKey(s);
	}

	public static byte[] getIconRawData(String s) {
		JarEntry resId = shaderIcons.get(s);
		if (resId == null) {
			resId = smallIcons.get(s);
		}
		if (resId == null)
			return null;

		try {
			final InputStream inputStream = getIconStream(resId);
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

	// public static int getBigIconResourceId(String s) {
	// JarEntry i = bigIcons.get(s);
	// if (i == null) {
	// return 0;
	// }
	// return i;
	// }

	public static BufferedImage getBigIcon(String s) {
		JarEntry resId = bigIcons.get(s);
		if (resId != null) {
			try {
				return ImageIO.read(getIconStream(resId));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static BufferedImage getIcon(String s, boolean includeShader) {
		if (s == null) {
			return null;
		}
		if (includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		if (!iconsBmp.containsKey(s)) {
			JarEntry resId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (resId != null) {
				BufferedImage bmp;
				try {
					bmp = ImageIO.read(getIconStream(resId));
					iconsBmp.put(s, bmp);
				} catch (IOException e) {
					e.printStackTrace();
					iconsBmp.put(s, null);
				}
			} else {
				iconsBmp.put(s, null);
			}
		}
		return iconsBmp.get(s);
	}

	public static InputStream getIconStream(JarEntry resId) throws IOException {
		URLConnection urlCon = sJarResource.openConnection();
		JarFile jar = ((JarURLConnection) urlCon).getJarFile();
		return jar.getInputStream(resId);
	}

	public static JarEntry getResId(String id) {
		return id.startsWith("h_") ? shaderIcons.get(id.substring(2)) : smallIcons.get(id);
	}

	static {
		initIcons();
	}

	static URL sJarResource; 
	
	public static void initIcons() {
		try {
			Enumeration<URL> en;
			en = OsmWindow.getInstance().getClass().getClassLoader()
					.getResources(File.separator + OsmWindow.OSMAND_ICONS_DIR);
			if (en.hasMoreElements()) {
				URL resource = en.nextElement();
				JarURLConnection urlcon = (JarURLConnection) (resource.openConnection());
				try (JarFile jar = urlcon.getJarFile();) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						JarEntry jarEntry = entries.nextElement();
						String entry = jarEntry.getName();
						if (!entry.startsWith(OsmWindow.OSMAND_ICONS_DIR)) {
							continue;
						}
						sJarResource = resource;
						String f = entry.toString().substring(OsmWindow.OSMAND_ICONS_DIR.length()).replaceFirst("\\.png", "");
						if (f.startsWith("h_")) {
							shaderIcons.put(f.substring(2), jarEntry);
						} else if (f.startsWith("mm_")) {
							smallIcons.put(f.substring(3), jarEntry);
						} else if (f.startsWith("mx_")) {
							bigIcons.put(f.substring(3), jarEntry);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e);
		}
	}

}
