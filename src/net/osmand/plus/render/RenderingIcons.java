package net.osmand.plus.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.sourceforge.offroad.OsmWindow;

public class RenderingIcons {
	private static final Log log = PlatformUtil.getLog(RenderingIcons.class);
	
	private static Map<String, Path> shaderIcons = new LinkedHashMap<String, Path>();
	private static Map<String, Path> smallIcons = new LinkedHashMap<String, Path>();
	private static Map<String, Path> bigIcons = new LinkedHashMap<String, Path>();
	private static Map<String, BufferedImage> iconsBmp = new LinkedHashMap<String, BufferedImage>();
//	private static DisplayMetrics dm;
	
	public static boolean containsSmallIcon(String s){
		return smallIcons.containsKey(s);
	}
	
	public static boolean containsBigIcon(String s){
		return bigIcons.containsKey(s);
	}
	
	public static byte[] getIconRawData(String s) {
		Path resId = shaderIcons.get(s);
		if(resId == null) {
			 resId = smallIcons.get(s);
		}
		if(resId == null)
			return null;
			
		try {
			final InputStream inputStream = new FileInputStream(resId.toFile());
			final ByteArrayOutputStream proxyOutputStream = new ByteArrayOutputStream(1024);
            final byte[] ioBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(ioBuffer)) >= 0) {
				proxyOutputStream.write(ioBuffer, 0, bytesRead);
			}
			inputStream.close();
			final byte[] bitmapData = proxyOutputStream.toByteArray();
			log.info("Icon data length is " + bitmapData.length); //$NON-NLS-1$
//			BufferedImage dm = android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length) ;
//			if(dm != null){
//				System.out.println("IC " + s +" " + dm.getHeight() + "x" + dm.getWidth());
//			}
			//if(android.graphics.BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length) == null)
			//	throw new Exception();
            return bitmapData;
		} catch(Throwable e) {
			log.error("Failed to get byte stream from icon", e); //$NON-NLS-1$
			return null;
		}
	}
	
//	public static int getBigIconResourceId(String s) {
//		Path i = bigIcons.get(s);
//		if (i == null) {
//			return 0;
//		}
//		return i;
//	}
	
	public static BufferedImage getBigIcon(String s) {
		Path resId = bigIcons.get(s);
		if (resId != null) {
			try {
				return ImageIO.read(resId.toFile());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static BufferedImage getIcon(String s, boolean includeShader) {
		if(s == null) {
			return null;
		}
		if(includeShader && shaderIcons.containsKey(s)) {
			s = "h_" + s;
		}
		if (!iconsBmp.containsKey(s)) {
			Path resId = s.startsWith("h_") ? shaderIcons.get(s.substring(2)) : smallIcons.get(s);
			if (resId != null) {
				BufferedImage bmp;
				try {
					bmp = ImageIO.read(resId.toFile());
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
	
	public static Path getResId(String id) {
		return id.startsWith("h_") ? shaderIcons.get(id.substring(2)) : smallIcons.get(id);
	}

	static {
		initIcons();
	}

	public static void initIcons() {
		Path dir = Paths.get(OsmWindow.getInstance().getAppPath(OsmWindow.OSMAND_ICONS_DIR).getAbsolutePath());
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{png}")) {
			for (Path entry : stream) {
				String f = entry.getFileName().toString().replaceFirst("\\.png", "");
//				System.out.println(f);
				if (f.startsWith("h_")) {
					shaderIcons.put(f.substring(2), entry);
				} else if( f.startsWith("mm_")) {
					smallIcons.put(f.substring(3), entry);
				} else if (f.startsWith("mx_")) {
					bigIcons.put(f.substring(3), entry);
				}
			}
		} catch (IOException x) {
			// IOException can never be thrown by the iteration.
			// In this snippet, it can // only be thrown by newDirectoryStream.
			System.err.println(x);
		}
	}
	
}
