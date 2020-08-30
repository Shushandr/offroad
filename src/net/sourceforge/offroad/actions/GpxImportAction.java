package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;

import net.osmand.IndexConstants;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.helpers.Kml2Gpx;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.R;
import net.sourceforge.offroad.ui.AsyncTask;
import net.sourceforge.offroad.ui.ProgressDialog;

/**
 * @author Koen Rabaey
 */
public class GpxImportAction extends OffRoadAction {

	public GpxImportAction(OsmWindow pContext) {
		super(pContext);
	}

	public static final String KML_SUFFIX = ".kml";
	public static final String GPX_SUFFIX = ".gpx";

	@Override
	public void actionPerformed(ActionEvent pE) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		int result = chooser.showOpenDialog(mContext.getWindow());
		if(result == JFileChooser.CANCEL_OPTION){
			return;
		}
		File[] fileList = chooser.getSelectedFiles();
		for (File file : fileList) {
			File intentUri = file;
			String fileName = file.getName();
	
			final boolean isFileIntent = true;
			final boolean isOsmandSubdir = isSubDirectory(mContext.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));
	
			final boolean saveFile = !isFileIntent || !isOsmandSubdir;
	
			if (file != null && fileName.endsWith(KML_SUFFIX)) {
				handleKmlImport(intentUri, fileName, saveFile);
	//Issue 2275
	//		} else if (fileName != null && (fileName.contains("favourite")|| 
	//				fileName.contains("favorite"))) {
	//			handleFavouritesImport(intentUri, fileName, saveFile);
			} else {
	//			handleGpxImport(intentUri, fileName, saveFile);
				handleFavouritesImport(intentUri, fileName, saveFile);
			}
		}
	}

//	private void handleGpxImport(final URI gpxFile, final String fileName, final boolean save) {
//		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
//			ProgressDialog progress = null;
//
//			@Override
//			protected void onPreExecute() {
//				progress = ProgressDialog.show(/*mapActivity,*/ mContext.getString(R.string.loading_smth, ""), mContext.getString(R.string.loading_data));
//			}
//
//			@Override
//			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
//				InputStream is = null;
//				try {
//						is = new FileInputStream(gpxFile);
//						return GPXUtilities.loadGPXFile(mContext, is);
//				} catch (FileNotFoundException e) {
//					//
//				} finally {
//					if (is != null) try {
//						is.close();
//					} catch (IOException ignore) {
//					}
//				}
//				return null;
//			}
//
//			@Override
//			protected void onPostExecute(GPXUtilities.GPXFile result) {
//				progress.dismiss();
//				handleResult(result, fileName, save);
//			}
//		}.execute();
//	}
//
	private void handleFavouritesImport(final File pIntentUri, final String fileName, final boolean save) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(/*mapActivity,*/ mContext.getString(R.string.loading_smth, ""), mContext.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
						is = new FileInputStream(pIntentUri);
						return GPXUtilities.loadGPXFile(mContext, is);
				} catch (FileNotFoundException e) {
					//
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(final GPXUtilities.GPXFile result) {
				progress.dismiss();
				importFavourites(result, fileName, save);
			}
		}.execute();
	}

	private void importFavoritesImpl(final GPXFile gpxFile) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(/*mapActivity,*/ mContext.getString(R.string.loading_smth, ""), mContext.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				final List<FavouritePoint> favourites = asFavourites(gpxFile.points);
				final FavouritesDbHelper favoritesHelper = mContext.getFavorites();
				for (final FavouritePoint favourite : favourites) {
					favoritesHelper.deleteFavourite(favourite, false);
					favoritesHelper.addFavourite(favourite, false);
				}
				favoritesHelper.sortAll();
				favoritesHelper.saveCurrentPointsIntoFile();
				return null;
			}

			@Override
			protected void onPostExecute(GPXUtilities.GPXFile result) {
				progress.dismiss();
//				AccessibleToast.makeText(mapActivity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
//				final Intent newIntent = new Intent(mapActivity, mContext.getAppCustomization().getFavoritesActivity());
//				mapActivity.startActivity(newIntent);
			}
		}.execute();
	}

	private void handleKmlImport(final File pIntentUri, final String name, final boolean save) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(/*mapActivity,*/ mContext.getString(R.string.loading_smth, ""), mContext.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
						is = new FileInputStream(pIntentUri);
						final String result = Kml2Gpx.toGpx(is);
						if (result != null) {
							return GPXUtilities.loadGPXFile(mContext, new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
						}
				} catch (FileNotFoundException e) {
					//
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(GPXUtilities.GPXFile result) {
				progress.dismiss();
				handleResult(result, name, save);
			}
		}.execute();
	}

	private void handleResult(final GPXUtilities.GPXFile result, final String name, final boolean save) {
		if (result != null) {
			if (result.warning != null) {
//				AccessibleToast.makeText(mapActivity, result.warning, Toast.LENGTH_LONG).show();
			} else {
				if (save) {
					new SaveAsyncTask(result, name).execute();
				} else {
					showGpxOnMap(result);
				}
			}
		} else {
//			AccessibleToast.makeText(mapActivity, R.string.error_reading_gpx, Toast.LENGTH_LONG).show();
		}
	}

	private String saveImport(final GPXUtilities.GPXFile gpxFile, final String fileName) {
		final String warning;

		if (gpxFile.isEmpty() || fileName == null) {
			warning = mContext.getString(R.string.error_reading_gpx);
		} else {
			final File importDir = mContext.getAppPath(IndexConstants.GPX_IMPORT_DIR);
			//noinspection ResultOfMethodCallIgnored
			importDir.mkdirs();
			if (importDir.exists() && importDir.isDirectory() && importDir.canWrite()) {
				final GPXUtilities.WptPt pt = gpxFile.findPointToShow();
				final File toWrite = getFileToSave(fileName, importDir, pt);

				warning = GPXUtilities.writeGpxFile(toWrite, gpxFile, mContext);
				if (warning == null) {
					gpxFile.path = toWrite.getAbsolutePath();
				}
			} else {
				warning = mContext.getString(R.string.sd_dir_not_accessible);
			}
		}

		return warning;
	}

	private File getFileToSave(final String fileName, final File importDir, final GPXUtilities.WptPt pt) {
		final StringBuilder builder = new StringBuilder(fileName);
		if ("".equals(fileName)) {
			builder.append("import_").append(new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time))).append(GPX_SUFFIX); //$NON-NLS-1$
		}
		if (fileName.endsWith(KML_SUFFIX)) {
			builder.replace(builder.length() - KML_SUFFIX.length(), builder.length(), GPX_SUFFIX);
		} else if (!fileName.endsWith(GPX_SUFFIX)) {
			builder.append(GPX_SUFFIX);
		}
		return new File(importDir, builder.toString());
	}

	private class SaveAsyncTask extends AsyncTask<Void, Void, String> {
		private final GPXUtilities.GPXFile result;
		private final String name;

		private SaveAsyncTask(GPXUtilities.GPXFile result, final String name) {
			this.result = result;
			this.name = name;
		}

		@Override
		protected String doInBackground(Void... nothing) {
			return saveImport(result, name);
		}

		@Override
		protected void onPostExecute(final String warning) {
			final String msg = warning == null ? MessageFormat.format(mContext.getString(R.string.gpx_saved_sucessfully), result.path) : warning;
//			AccessibleToast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();

			showGpxOnMap(result);
		}

	}

	private void showGpxOnMap(final GPXUtilities.GPXFile result) {
		mContext.getSelectedGpxHelper().setGpxFileToDisplay(result);
		final GPXUtilities.WptPt moveTo = result.findPointToShow();
		if (moveTo != null) {
			mContext.move(new LatLon(moveTo.getLatitude(), moveTo.getLongitude()), null);
//			mapView.getAnimatedDraggingThread().startMoving(moveTo.lat, moveTo.lon, mapView.getZoom(), true);
		}
		mContext.getDrawPanel().drawLater();
//		mapView.refreshMap();
	}

	private void importFavourites(final GPXUtilities.GPXFile gpxFile, final String fileName, final boolean save) {
		handleResult(gpxFile, fileName, save);
//		importFavoritesImpl(gpxFile);
		
//		final DialogInterface.OnClickListener importFavouritesListener = new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				switch (which) {
//					case DialogInterface.BUTTON_POSITIVE:
//						importFavoritesImpl(gpxFile);
//						break;
//					case DialogInterface.BUTTON_NEGATIVE:
//						handleResult(gpxFile, fileName, save);
//						break;
//				}
//			}
//		};
//
//		new AlertDialog.Builder(mapActivity)
//				.setTitle(R.string.shared_string_import2osmand)
//				.setMessage(R.string.import_file_favourites)
//				.setPositiveButton(R.string.shared_string_import, importFavouritesListener)
//				.setNegativeButton(R.string.shared_string_save, importFavouritesListener)
//				.show();
	}

	private List<FavouritePoint> asFavourites(final List<GPXUtilities.WptPt> wptPts) {
		final List<FavouritePoint> favourites = new ArrayList<>();

		for (GPXUtilities.WptPt p : wptPts) {
			if (p.category != null) {
				final FavouritePoint fp = new FavouritePoint(p.lat, p.lon, p.name, p.category);
				if (p.desc != null) {
					fp.setDescription(p.desc);
				}
				favourites.add(fp);
			} else if (p.name != null) {
				favourites.add(new FavouritePoint(p.lat, p.lon, p.name, ""));
			}
		}

		return favourites;
	}

	/**
	 * Checks, whether the child directory is a subdirectory of the parent
	 * directory.
	 *
	 * @param parent the parent directory.
	 * @param child  the suspected child directory.
	 * @return true if the child is a subdirectory of the parent directory.
	 */
	public boolean isSubDirectory(File parent, File child) {
		try {
			parent = parent.getCanonicalFile();
			child = child.getCanonicalFile();

			File dir = child;
			while (dir != null) {
				if (parent.equals(dir)) {
					return true;
				}
				dir = dir.getParentFile();
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}

}
