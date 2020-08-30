/*
 * EditServer.java - OffRoad server
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sourceforge.offroad;

import java.io.BufferedReader;
//{{{ Imports
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Random;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Inter-process communication.
 * <p>
 * 
 * The edit server protocol is very simple. <code>$HOME/.jedit/server</code> is
 * an ASCII file containing two lines, the first being the port number, the
 * second being the authorization key.
 * <p>
 * 
 * You connect to that port on the local machine, sending the authorization key
 * as four bytes in network byte order, followed by the length of the BeanShell
 * script as two bytes in network byte order, followed by the script in UTF8
 * encoding. After the socked is closed, the BeanShell script will be executed
 * by FreeMind.
 * <p>
 * 
 * The snippet is executed in the AWT thread. None of the usual BeanShell
 * variables (view, buffer, textArea, editPane) are set so the script has to
 * figure things out by itself.
 * <p>
 * 
 * In most cases, the script will call the static
 * {@link #handleClient(boolean,String,String[])} method, but of course more
 * complicated stuff can be done too.
 * 
 * @author Slava Pestov
 * @version $Id: EditServer.java 19384 2011-02-23 16:50:37Z k_satoda $
 */
public class GeoServer extends Thread {
	private final static Log log = PlatformUtil.getLog(GeoServer.class);

	private final OsmWindow mFrame;

	// {{{ EditServer constructor
	GeoServer(String portFile, OsmWindow pFrame) {
		super("OffRoad server daemon [" + portFile + "]");
		mFrame = pFrame;
		setDaemon(true);
		this.portFile = Paths.get(portFile);

		try {
			// On Unix, set permissions of port file to rw-------,
			// so that on broken Unices which give everyone read
			// access to user home dirs, people can't see your
			// port file (and hence send arbitriary BeanShell code
			// your way. Nasty.)
			// This MUST use the newByteChannel function as to guarantee
			// that creating the file, setting attributes and opening it
			// is fully atomic, or there would be race conditions where
			// an attacker can place a file with weaker permissions.
			Files.deleteIfExists(this.portFile);
			ByteChannel out = Files.newByteChannel(this.portFile, EnumSet.of(CREATE_NEW, WRITE),
					PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));

			// Bind to any port on localhost; accept 2 simultaneous
			// connection attempts before rejecting connections
			socket = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"));
			authKey = new Random().nextInt(Integer.MAX_VALUE);
			int port = socket.getLocalPort();

			String data = "b\n" + port + "\n" + authKey + "\n";
			try {
				out.write(ByteBuffer.wrap(data.getBytes()));
			} finally {
				out.close();
			}

			ok = true;

			log.info("OffRoad server started on port " + socket.getLocalPort());
			log.info("Authorization key is " + authKey);
		} catch (IOException io) {
			/*
			 * on some Windows versions, connections to localhost fail if the
			 * network is not running. To avoid confusing newbies with weird
			 * error messages, log errors that occur while starting the server
			 * as NOTICE, not ERROR
			 */
			log.info("" + io);
		}
	} // }}}

	// {{{ run() method
	public void run() {
		for (;;) {
			if (abort)
				return;

			Socket client = null;
			try {
				client = socket.accept();

				// Stop script kiddies from opening the edit
				// server port and just leaving it open, as a
				// DoS
				client.setSoTimeout(1000);

				log.info(client + ": connected");

				DataInputStream in = new DataInputStream(client.getInputStream());

				if (!handleClient(client, in)) {
					// abort = true;
				}
			} catch (Exception e) {
				if (!abort)
					log.info("" + e);
				abort = true;
			} finally {
				/*
				 * if(client != null) { try { client.close(); } catch(Exception
				 * e) { logger.info(e); }
				 * 
				 * client = null; }
				 */
			}
		}
	} // }}}

	// {{{ isOK() method
	boolean isOK() {
		return ok;
	} // }}}

	// {{{ getPort method
	public int getPort() {
		return socket.getLocalPort();
	} // }}}

	// {{{ stopServer() method
	void stopServer() {
		abort = true;
		try {
			socket.close();
			Files.deleteIfExists(portFile);
		} catch (IOException io) {
		}
	} // }}}

	// {{{ Private members

	// {{{ Instance variables
	private Path portFile;
	private ServerSocket socket;
	private int authKey;
	private boolean ok;
	private boolean abort;

	// }}}

	// {{{ handleClient() method
	private boolean handleClient(final Socket client, DataInputStream in) throws Exception {
		int key = in.readInt();
		if (key != authKey) {
			log.info(client + ": wrong" + " authorization key (got " + key + ", expected " + authKey + ")");
			in.close();
			client.close();

			return false;
		} else {
			// Reset the timeout
			client.setSoTimeout(0);

			log.info(client + ": authenticated" + " successfully");

			final String script = in.readUTF();
			log.info(script);

			SwingUtilities.invokeLater(() -> {
				String[] coords = script.split(":");
				if (coords.length != 2) {
					log.info("Wrong argument count. Expected 2, got " + coords.length);
					return;
				}
				double lat = Double.parseDouble(coords[0]);
				double lon = Double.parseDouble(coords[1]);
				mFrame.moveDirectly(new LatLon(lat, lon));
			});
			in.close();
			client.close();

			return true;
		}
	} // }}}

	// }}}

	public static void main(String[] args) {
		String portFile = args[0];
		if (portFile == null) {
			return;
		}
		// {{{ Try connecting to another running FreeMind instance
		if (portFile != null && new File(portFile).exists()) {
			try {
				BufferedReader in = new BufferedReader(new FileReader(portFile));
				String check = in.readLine();
				if (!check.equals("b"))
					throw new Exception("Wrong port file format");

				int port = Integer.parseInt(in.readLine());
				int key = Integer.parseInt(in.readLine());

				Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port);
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.writeInt(key);

				String script;
				// Put url to open here
				script = args[1];
				out.writeUTF(script);

				System.out.println("Waiting for server");
				// block until its closed
				try {
					socket.getInputStream().read();
				} catch (Exception e) {
				}

				in.close();
				out.close();

				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
