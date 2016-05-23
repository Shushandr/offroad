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

import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.sourceforge.offroad.OsmWindow;

/**
 * @author foltin
 * @date 23.05.2016
 */
public abstract class SQLiteOpenHelper {

	private OsmWindow mCtx;
	private String mDatabaseName;
	private int mDatabaseVersion;

	public SQLiteOpenHelper(OsmWindow pCtx, String pDatabaseName, Object pObject, int pDatabaseVersion) {
		mCtx = pCtx;
		mDatabaseName = pDatabaseName;
		mDatabaseVersion = pDatabaseVersion;
		if(!mCtx.getSQLiteAPI().isDatabaseExistent(pDatabaseName)){
			SQLiteConnection db = getWritableDatabase();
			onCreate(db);
			db.setVersion(pDatabaseVersion);
			db.close();
		}
		SQLiteConnection db2 = getWritableDatabase();
		if(db2.getVersion() != pDatabaseVersion){
			onUpgrade(db2, db2.getVersion(), pDatabaseVersion);
			db2.setVersion(pDatabaseVersion);
		}
		db2.close();
	}

	public abstract void onCreate(SQLiteConnection pDb);

	public abstract void onUpgrade(SQLiteConnection pDb, int pOldVersion, int pNewVersion);
	
	public SQLiteConnection getWritableDatabase() {
		return mCtx.getSQLiteAPI().getOrCreateDatabase(mDatabaseName, false);
	}
	public SQLiteConnection getReadableDatabase() {
		return mCtx.getSQLiteAPI().getOrCreateDatabase(mDatabaseName, true);
	}
	


}
