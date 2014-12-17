package socialatwork.readpeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.WebRelatedComponents.onlinePictureDownloader;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class tdCacheManager {

	private static tdCacheManager CacheManagerInstance;
	private static String TAG = "CacheManager";
	private static onlinePictureDownloader picDownloader;
	private final long CACHE_TIME_OUT = 30000l; // 30 seconds
	private static Context mContext;

	// 'Synchronized' is to ensure there is only one instance
	public static synchronized tdCacheManager getCacheManagerInstance(Context c) {
		if (CacheManagerInstance == null) {
			CacheManagerInstance = new tdCacheManager();
			setCacheManagerContext(c);
		}
		return CacheManagerInstance;
	}

	/*
	 * This method must be used to provide a context for many functions in this
	 * CacheManager class
	 */
	private static void setCacheManagerContext(Context c) {
		mContext = c;
	}

	public String getCachePath() {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {

			String cachePath = Environment.getExternalStorageDirectory()
					.getPath() + "/Readpeer/Cache/Pictures/";
			return cachePath;
		}

		else {
			Log.e(TAG, "No SD card found");
			return null;
		}
	}

	public boolean hasImageInCache(String imageName) {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String filePath = getCachePath() + imageName;
			File imageFile = new File(filePath);
			if (imageFile.exists())
				return true;
			else
				return false;
		}
		return false;
	}

	public boolean hasJSONCache(SharedPreferences sp, String key) {

		return sp.contains(key);
	}

	public JSONObject getJSONCache(SharedPreferences sp, String key)
			throws JSONException {

		return new JSONObject(sp.getString(key, null));
	}

	// Save JSON to local cache. Saved time will also be stored.
	public void setJSONCache(SharedPreferences sp, String key,
			JSONObject targetJSON) {

		SharedPreferences.Editor editor = sp.edit();
		String targetString = null;
		if (targetJSON != null) {
			targetString = targetJSON.toString();
		} else {
			Log.e(TAG, "TARGET JSON IS NULL");
		}
		editor.putString(key, targetString);
		editor.commit();
		setJSONCacheSavedTime(sp, key);
	}

	// Use cache to store the save time of a certain key
	private void setJSONCacheSavedTime(SharedPreferences sp, String key) {

		SharedPreferences.Editor editor = sp.edit();
		String timeKey = key + "-save-time";
		editor.putString(timeKey, String.valueOf(new Date().getTime()));
		editor.commit();
	}

	public long getJSONCacheSavedTime(SharedPreferences sp, String key) {

		String timeKey = key + "-save-time";
		return Long.parseLong(sp.getString(timeKey, null));
	}

	public boolean isJSONCacheTimedOut(SharedPreferences sp, String key) {

		String timeKey = key + "-save-time";
		String savedTimeString = sp.getString(timeKey, null);

		// if no saved time record in cache means the data is never download
		if (savedTimeString == null) {
			Log.d(TAG, "new cache not timed out");
			return false;
		}

		else {
			long savedTime = Long.parseLong(sp.getString(timeKey, null));
			long currentTime = new Date().getTime();
			if (currentTime - savedTime > CACHE_TIME_OUT) {
				Log.d(TAG, "old cache timed out");
				return true;
			} else {
				Log.d(TAG, "old cache haven't timed out");
				return false;
			}
		}
	}

	// Download any user image from server that are not already exists in cache
	public void downloadUncachedImages(ArrayList<URL> imageURLs, ArrayList<String> imageNames,
			Handler downloadHandler) throws Exception {

		if (picDownloader == null) {
			picDownloader = onlinePictureDownloader
					.getDownloaderInstance(downloadHandler);
		}
		picDownloader.downloadImageFromUrl(imageURLs,imageNames);
	}

	public boolean isNetworkConnected() {
		if (mContext != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager
					.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				Log.d(TAG, "Network is connected");
				return mNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	public boolean shouldDownloadJSONFromServer(SharedPreferences sp, String key) {

		String timeKey = key + "-save-time";
		boolean shouldDownloadJSONFromServer = false;

		if (isNetworkConnected()) {
			// if has local cache
			if (sp.contains(timeKey)) {

				Log.d(TAG, "Local cache exists");
				String savedTime = sp.getString(timeKey, null);

				if (savedTime != null) {
					if (isJSONCacheTimedOut(sp, key)) {
						shouldDownloadJSONFromServer = true;
					}

					else {
						shouldDownloadJSONFromServer = false;
					}
				}
				// when not sure the saved time, redownload annotations
				else {
					shouldDownloadJSONFromServer = true;
				}
			}
			// if no local cache found
			else {
				Log.d(TAG, "No local cache");
				shouldDownloadJSONFromServer = true;
			}
		}// if no network
		else {
			shouldDownloadJSONFromServer = false;
		}
		return shouldDownloadJSONFromServer;
	}

	/*
	 * return the drawable created from the cache image provided if cache not
	 * found, return a default image
	 */
	public Drawable getDrawableFromCache(String imageName) {
		Drawable userImage = null;
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String applicationFolderPath = rootPath + "/Readpeer";
			String imageFilePath = applicationFolderPath + "/Cache/Pictures/"
					+ imageName;
			File imageFile = new File(imageFilePath);

			if (imageFile.exists()) {
				userImage = Drawable.createFromPath(imageFilePath);
			} else {
				userImage = mContext.getResources().getDrawable(
						R.drawable.default_user_image);
			}
		}
		return userImage;
	}

	public void saveBitmapToCache(Bitmap bmp, String imageName)
			throws IOException, InterruptedException {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String applicationFolderPath = rootPath + "/Readpeer";
			String imageFolderPath = applicationFolderPath + "/Cache/Pictures";
			File pictureFolder = new File(imageFolderPath);
			if (!pictureFolder.exists()) {
				pictureFolder.mkdirs();
			}

			File imageFile = new File(pictureFolder, imageName);
			FileOutputStream fOut = new FileOutputStream(imageFile);
			bmp.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
			fOut.close();
		}
	}
}
