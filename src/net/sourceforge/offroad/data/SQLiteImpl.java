/** 
   OffRoad
   Copyright (C) 2016 Christian Foltin

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

package net.sourceforge.offroad.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.sqlite.SQLiteConfig;

import net.osmand.PlatformUtil;
import net.osmand.plus.api.SQLiteAPI;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 10.05.2016
 */
public class SQLiteImpl implements SQLiteAPI {
	private final static Log log = PlatformUtil.getLog(SQLiteImpl.class);
	private OsmWindow mContext;

	
	public static class SQLiteCursorWrapper implements SQLiteCursor {

		private ResultSet mResult;

		public SQLiteCursorWrapper(ResultSet pExecuteQuery) {
			mResult = pExecuteQuery;
		}

		@Override
		public String[] getColumnNames() {
			String[] ret = null;
			try {
				ret = new String[mResult.getMetaData().getColumnCount()];
				for (int i = 0; i < ret.length; ++i) {
					ret[i] = mResult.getMetaData().getColumnLabel(i);
				}
				return ret;
			} catch (SQLException e) {
				log.error("SQLException:"+Arrays.toString(ret), e);
			}
			return null;
		}

		@Override
		public boolean moveToFirst() {
			try {
				return mResult.next();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
			return false;
		}

		@Override
		public boolean moveToNext() {
			try {
				return mResult.next();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
			return false;
		}

		@Override
		public String getString(int pInd) {
			try {
				return mResult.getString(pInd+1);
			} catch (SQLException e) {
				log.error("SQLException:"+pInd, e);
			}
			return null;
		}

		@Override
		public double getDouble(int pInd) {
			try {
				return mResult.getDouble(pInd+1);
			} catch (SQLException e) {
				log.error("SQLException:"+pInd, e);
			}
			return 0d;
		}

		@Override
		public long getLong(int pInd) {
			try {
				return mResult.getLong(pInd+1);
			} catch (SQLException e) {
				log.error("SQLException:"+pInd, e);
			}
			return 0l;
		}

		@Override
		public int getInt(int pInd) {
			try {
				return mResult.getInt(pInd+1);
			} catch (SQLException e) {
				log.error("SQLException:"+pInd, e);
			}
			return 0;
		}

		@Override
		public byte[] getBlob(int pInd) {
			try {
				return mResult.getBytes(pInd+1);
			} catch (SQLException e) {
				log.error("SQLException:"+pInd, e);
			}
			return null;
		}

		@Override
		public void close() {
			try {
				mResult.close();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
		}

	}

	public class SQLiteConnectionWrapper implements SQLiteConnection {

		private Connection mConn;
		private String mName;

		public SQLiteConnectionWrapper(Connection pConn, String pName) {
			mConn = pConn;
			mName = pName;
		}

		@Override
		public void close() {
			try {
				mConn.close();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
		}

		@Override
		public SQLiteCursor rawQuery(String pSql, String[] pSelectionArgs) {
			if (pSelectionArgs != null && pSelectionArgs.length > 0) {
				throw new IllegalArgumentException("Not Implemented");
			}
			try {
				return new SQLiteCursorWrapper(mConn.createStatement().executeQuery(pSql));
			} catch (SQLException e) {
				log.error("SQLException:"+pSql, e);
			}
			return null;
		}

		@Override
		public void execSQL(String pQuery) {
			try {
				mConn.createStatement().executeUpdate(pQuery);
			} catch (SQLException e) {
				log.error("SQLException:"+pQuery, e);
			}
		}

		@Override
		public void execSQL(String pQuery, Object[] pObjects) {
			try {
				PreparedStatement prep = mConn.prepareStatement(pQuery);
				for (int i = 0; i < pObjects.length; i++) {
					Object object = pObjects[i];
					prep.setObject(i+1, object);
				}
				prep.execute();
			} catch (SQLException e) {
				log.error("SQLException:"+pQuery, e);
			}
		}

		@Override
		public SQLiteStatement compileStatement(String pString) {
			throw new IllegalArgumentException("Not implemented");
		}

		@Override
		public void setVersion(int pNewVersion) {
			mContext.getOffroadProperties().setProperty("version_"+mName, ""+pNewVersion);
		}

		@Override
		public int getVersion() {
			Properties prop = mContext.getOffroadProperties();
			String key = "version_" + mName;
			if(prop.containsKey(key)){
				return Integer.parseInt(""+prop.get(key));
			}
			return 0;
		}

		@Override
		public boolean isReadOnly() {
			try {
				return mConn.isReadOnly();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
			return true;
		}

		@Override
		public boolean isDbLockedByOtherThreads() {
			// FIXME: Implement me.
			return false;
		}

		@Override
		public boolean isClosed() {
			try {
				return mConn.isClosed();
			} catch (SQLException e) {
				log.error("SQLException:", e);
			}
			return true;
		}

		@Override
		public boolean isOpen() {
			return !isClosed();
		}
		
	}

	public SQLiteImpl(OsmWindow pOsmWindow) {
		mContext = pOsmWindow;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.osmand.plus.api.SQLiteAPI#getOrCreateDatabase(java.lang.String,
	 * boolean)
	 */
	@Override
	public SQLiteConnection getOrCreateDatabase(String pName, boolean pReadOnly) {
		try {
			SQLiteConfig config = new SQLiteConfig();
			String file = getDatabaseFileName(pName);
			if(pReadOnly && new File(file).exists()){
				config.setReadOnly(pReadOnly);
			}
			Connection conn = DriverManager.getConnection("jdbc:sqlite:"+file, config.toProperties());
			return new SQLiteConnectionWrapper(conn, pName);
		} catch (SQLException e) {
			log.error("SQLException:"+pName, e);
		}
		return null;
	}

	protected String getDatabaseFileName(String pName) {
		return mContext.getAppPathName(pName)+".db";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.osmand.plus.api.SQLiteAPI#openByAbsolutePath(java.lang.String,
	 * boolean)
	 */
	@Override
	public SQLiteConnection openByAbsolutePath(String pPath, boolean pReadOnly) {
		throw new IllegalArgumentException("Not implemented");
	}

	public static void main(String[] args) {
		SQLiteImpl impl = new SQLiteImpl(OsmWindow.getInstance());
		SQLiteConnection conn = impl.getOrCreateDatabase("test", true);
		System.out.println("Version: " + conn.getVersion());
		conn.setVersion(17);
		conn.execSQL("create table if not exists 'bla' (name TEXT);");
		conn.execSQL("insert into 'bla' values ('testvalue');");
		SQLiteCursor query = conn.rawQuery(
				"SELECT name, name FROM bla ORDER BY name DESC", null); //$NON-NLS-1$//$NON-NLS-2$
		if (query.moveToFirst()) {
			do {
				String name = query.getString(1);
				System.out.println("Content:  " + name);
			} while (query.moveToNext());
		}
		query.close();
		conn.close();
	}

	@Override
	public boolean isDatabaseExistent(String pName) {
		String file = getDatabaseFileName(pName);
		return new File(file).exists();
	}
	
}
