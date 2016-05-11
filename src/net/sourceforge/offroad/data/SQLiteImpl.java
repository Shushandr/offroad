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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.osmand.plus.api.SQLiteAPI;

/**
 * @author foltin
 * @date 10.05.2016
 */
public class SQLiteImpl implements SQLiteAPI {

	public class SQLiteCursorWrapper implements SQLiteCursor {

		private ResultSet mResult;

		public SQLiteCursorWrapper(ResultSet pExecuteQuery) {
			mResult = pExecuteQuery;
		}

		@Override
		public String[] getColumnNames() {
			try {
				String[] ret = new String[mResult.getMetaData().getColumnCount()];
				for (int i = 0; i < ret.length; ++i) {
					ret[i] = mResult.getMetaData().getColumnLabel(i);
				}
				return ret;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public boolean moveToFirst() {
			try {
				return mResult.first();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public boolean moveToNext() {
			try {
				return mResult.next();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public String getString(int pInd) {
			try {
				return mResult.getString(pInd);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public double getDouble(int pInd) {
			try {
				return mResult.getDouble(pInd);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return 0d;
		}

		@Override
		public long getLong(int pInd) {
			try {
				return mResult.getLong(pInd);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return 0l;
		}

		@Override
		public long getInt(int pInd) {
			try {
				return mResult.getInt(pInd);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return 0;
		}

		@Override
		public byte[] getBlob(int pInd) {
			try {
				return mResult.getBytes(pInd);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void close() {
			try {
				mResult.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	public class SQLiteConnectionWrapper implements SQLiteConnection {

		private Connection mConn;

		public SQLiteConnectionWrapper(Connection pConn) {
			mConn = pConn;
		}

		@Override
		public void close() {
			try {
				mConn.close();
			} catch (SQLException e) {
				e.printStackTrace();
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
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void execSQL(String pQuery) {
			try {
				mConn.createStatement().executeQuery(pQuery);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void execSQL(String pQuery, Object[] pObjects) {
			try {
				PreparedStatement prep = mConn.prepareStatement(pQuery);
				for (int i = 0; i < pObjects.length; i++) {
					Object object = pObjects[i];
					prep.setObject(i, object);
				}
				prep.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public SQLiteStatement compileStatement(String pString) {
			throw new IllegalArgumentException("Not implemented");
		}

		@Override
		public void setVersion(int pNewVersion) {
		}

		@Override
		public int getVersion() {
			try {
				return mConn.getMetaData().getDatabaseMajorVersion();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return 3;
		}

		@Override
		public boolean isReadOnly() {
			try {
				return mConn.isReadOnly();
			} catch (SQLException e) {
				e.printStackTrace();
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
				e.printStackTrace();
			}
			return true;
		}

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
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:"+pName);
			conn.setReadOnly(pReadOnly);
			return new SQLiteConnectionWrapper(conn);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return null;
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

}
