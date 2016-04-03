package net.sourceforge.offroad.actions;
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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.AbstractAction;

import org.apache.commons.logging.Log;

import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.DownloadOsmandIndexesHelper;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.IndexFileList;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.sourceforge.offroad.OsmWindow;

public class DownloadAction extends AbstractAction {
	private final static Log log = PlatformUtil.getLog(DownloadAction.class);

	public static void main(String[] args) throws InterruptedException {
		OsmWindow ctx = OsmWindow.getInstance();
		IndexFileList indexesList = DownloadOsmandIndexesHelper.getIndexesList(ctx);
		DownloadResources downloadResources = new DownloadResources(ctx);
		downloadResources.updateLoadedFiles();
		for (IndexItem item : indexesList.getIndexFiles()) {
			if(item.getFileName().equals("Germany_brandenburg_europe_2.obf.zip")){
				System.out.println("Downloading " + item.getSize() + " bytes for " + item.getFileName());
				DownloadFileHelper helper = new DownloadFileHelper(ctx);
				Vector<File> toReIndex = new Vector<>();
				helper.downloadFile(item.createDownloadEntry(ctx), IProgress.EMPTY_PROGRESS, toReIndex, new DownloadFileShowWarning(){

					@Override
					public void showWarning(String pWarning) {
						System.err.println("DOWNLOAD WARNING: " + pWarning);
						
					}}, false);
			}
		}
		for (IndexItem item : downloadResources.getItemsToUpdate()) {
			System.out.println(item.getFileName());
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent pE) {

	}

}
