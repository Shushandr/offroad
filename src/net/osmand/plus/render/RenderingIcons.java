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
	public static class ResEntry {
		public final JarEntry jar;
		public File file;
		public ResEntry(JarEntry jar) { this.jar = jar; file = null; }
		public ResEntry(File file) { jar = null; this.file = file; }
	}

	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);

	private static Map<String, ResEntry> shaderIcons = new LinkedHashMap<String, ResEntry>();
	private static Map<String, ResEntry> smallIcons = new LinkedHashMap<String, ResEntry>();
	private static Map<String, ResEntry> bigIcons = new LinkedHashMap<String, ResEntry>();
	private static Map<String, BufferedImage> iconsBmp = new LinkedHashMap<String, BufferedImage>();
	// private static DisplayMetrics dm;

	public static boolean containsSmallIcon(String s) {
		return smallIcons.containsKey(s);
	}

	public static boolean containsBigIcon(String s) {
		return bigIcons.containsKey(s);
	}

	public static byte[] getIconRawData(String s) {
		ResEntry resId = shaderIcons.get(s);
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
	// ResEntry i = bigIcons.get(s);
	// if (i == null) {
	// return 0;
	// }
	// return i;
	// }

	public static BufferedImage getBigIcon(String s) {
		ResEntry resId = bigIcons.get(s);
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
			ResEntry resId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (resId != null) {
				BufferedImage bmp;
				try {
					bmp = ImageIO.read(getIconStream(resId));
					iconsBmp.put(s, bmp);
				} catch (IOException e) {
					e.printStackTrace();
					iconsBmp.put(s, null);
				}
			} else if (smallIcons.isEmpty()) {
				// fallback to loading from file system
				OsmWindow context = OsmWindow.getInstance();
				String osmandIconsDir = context.getOsmandIconsDir();
				osmandIconsDir = RenderingIcons.class.getProtectionDomain().getCodeSource().getLocation().getFile() + osmandIconsDir;
				BufferedImage bmp;
				try {
					if (includeShader && !new File(osmandIconsDir + s + ".png").exists()) s = "h_" + s;
					bmp = ImageIO.read(new FileInputStream(osmandIconsDir + s + ".png"));
					iconsBmp.put(s, bmp);
				} catch (IOException e) {
					e.printStackTrace();
					iconsBmp.put(s, null);
				}
			}
		}
		return iconsBmp.get(s);
	}

	public static InputStream getIconStream(ResEntry resId) throws IOException {
		if (resId.jar == null) {
			return new FileInputStream(resId.file);
		}
		URLConnection urlCon = sJarResource.openConnection();
		JarFile jar = ((JarURLConnection) urlCon).getJarFile();
		return jar.getInputStream(resId.jar);
	}

	public static ResEntry getResId(String id) {
		return id.startsWith("h_") ? shaderIcons.get(id.substring(2)) : smallIcons.get(id);
	}

	static {
		initIcons();
	}

	static URL sJarResource; 
	
	public static void initIcons() {
		iconsBmp.clear();
		try {
			Enumeration<URL> en;
			OsmWindow context = OsmWindow.getInstance();
			String osmandIconsDir = context.getOsmandIconsDir();
			en = context.getClass().getClassLoader()
					.getResources(osmandIconsDir);
			System.out.println("icon resources present: " + en.hasMoreElements());
			boolean found = false;
			File resDir = null;
			while (en.hasMoreElements()) {
				URL resource = en.nextElement();
				System.out.println("Trying resource " + resource);
				URLConnection conn = resource.openConnection();
				if (conn instanceof JarURLConnection) {
					JarURLConnection urlcon = (JarURLConnection) conn;
					try (JarFile jar = urlcon.getJarFile();) {
						Enumeration<JarEntry> entries = jar.entries();
						while (entries.hasMoreElements()) {
							JarEntry jarEntry = entries.nextElement();
							String entry = jarEntry.getName();
							if (!entry.startsWith(osmandIconsDir)) {
								continue;
							}
							found = true;
							ResEntry r = new ResEntry(jarEntry);
							sJarResource = resource;
							String f = entry.toString().substring(osmandIconsDir.length())
									.replaceFirst("\\.png", "");
							if (f.startsWith("h_")) {
								shaderIcons.put(f.substring(2), r);
							} else if (f.startsWith("mm_")) {
								smallIcons.put(f.substring(3), r);
							} else if (f.startsWith("mx_")) {
								bigIcons.put(f.substring(3), r);
							}
						}
					}
				} else if (new File(resource.getFile()).isDirectory()) {
					resDir = new File(resource.getFile());
				}
			}
			if (!found && resDir != null) {
				final File[] files = resDir.listFiles();
				for (File file : files) {
					ResEntry r = new ResEntry(file);
					String f = file.getName().replaceFirst("\\.png", "");
					if (f.startsWith("h_")) {
						shaderIcons.put(f.substring(2), r);
					} else if (f.startsWith("mm_")) {
						smallIcons.put(f.substring(3), r);
					} else if (f.startsWith("mx_")) {
						bigIcons.put(f.substring(3), r);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e);
		}
	}

}
