package socialatwork.readpeer.WebRelatedComponents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import socialatwork.readpeer.ImageNameGenerator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class onlinePictureDownloader {

	private static String TAG = "imageDownloader";

	private static onlinePictureDownloader downloaderInstance;
	private ArrayList<String> downloadItemNameList;
	private ArrayList<URL> downloadItemURLList;
	private ArrayList<String> picNames;

	public Handler mHandler;
	private int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;
	private long PICTURE_DOWNLOAD_TIMEOUT = 5000l;
	private long PICTURE_DOWNLOAD_TERMINATE_TIME = 5500l;

	/*
	 * 'Synchronized' is to ensure there is only one instance. the parameter
	 * handler is for convenience of notifying caller that downloading task has
	 * finished. if no need for notification can set handler to null
	 */
	public static synchronized onlinePictureDownloader getDownloaderInstance(
			Handler h) {
		if (downloaderInstance == null) {
			downloaderInstance = new onlinePictureDownloader();
		}
		downloaderInstance.mHandler = h;
		return downloaderInstance;
	}

	// from the input list, filter those without local cache and add them to
	// download list
	private void getDownloadItems(ArrayList<URL> URLs) {

		downloadItemNameList = new ArrayList<String>();
		downloadItemURLList = new ArrayList<URL>();

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {

			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String saveFolderPath = rootPath + "/Readpeer/Cache/Pictures/";
			int downloadItemsCount = 0;

			for (int i = 0; i < URLs.size(); i++) {
				String imageName = picNames.get(i);
				String imageFilePath = saveFolderPath + imageName;
				File imageFile = new File(imageFilePath);
				
				downloadItemNameList.add(picNames.get(i));
				downloadItemURLList.add(URLs.get(i));
				downloadItemsCount++;
				/*
				// if the item is not cached, add it to download task list
				if (!imageFile.exists()) {
					Log.d(TAG, "Need to be downloaded");
					Log.d(TAG, imageName);
					downloadItemNameList.add(picNames.get(i));
					downloadItemURLList.add(URLs.get(i));
					downloadItemsCount++;
				}
				*/
			}
		}
		else {
			Log.e(TAG, "No SD Card");
		}
	}

	public void downloadImageFromUrl(ArrayList<URL> imageUrl, ArrayList<String> picNames)
			throws IOException {
		getImage newGetImage = new getImage();
		this.picNames = picNames;
		newGetImage.execute(imageUrl);
	}

	/*
	 * private String getImageNameFromUrl(URL imageUrl) { // TODO Auto-generated
	 * method stub String urlString = imageUrl.toString(); String[] subStrings;
	 * subStrings = urlString.split("/"); String imageName; int nameEndIndex =
	 * subStrings[subStrings.length - 1].lastIndexOf("?"); if (nameEndIndex < 0)
	 * {
	 * 
	 * imageName = subStrings[subStrings.length - 1]; } else { imageName =
	 * subStrings[subStrings.length - 1].substring(0, nameEndIndex); }
	 * Log.i(TAG, "image name cut from url:" + imageName); return imageName; }
	 */

	// In background download the image
	public class getImage extends AsyncTask<ArrayList<URL>, Void, Void> {

		@Override
		protected void onPreExecute() {

			// Send time out msg if download takes too long
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mHandler.sendEmptyMessage(PICTURE_DOWNLOAD_TIMEOUT_MESSAGE);
					cancel(true);
				}
			}, PICTURE_DOWNLOAD_TIMEOUT);

			// Cancel the task if the task takes even longer
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					cancel(true);
				}
			}, PICTURE_DOWNLOAD_TERMINATE_TIME);
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mHandler != null) {
				mHandler.sendEmptyMessage(PICTURE_DOWNLOAD_COMPLETED_MESSAGE);
			}
			Log.d(TAG, "Image Download completed");
		}

		@Override
		protected Void doInBackground(ArrayList<URL>... urls) {

			String imageName;
			HttpURLConnection conn;
			InputStream stream;
			FileOutputStream fOut;
			File pictureFile;
			String pictureFilePath = null;
			File pictureFolder = null;
			Bitmap image;

			/* Build image save path, create folder if not any exists */
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				String rootPath = Environment.getExternalStorageDirectory()
						.getPath();
				String applicationFolderPath = rootPath + "/Readpeer";
				pictureFilePath = applicationFolderPath + "/Cache/Pictures";
				pictureFolder = new File(pictureFilePath);
				if (!pictureFolder.exists()) {
					pictureFolder.mkdirs();
				}
				getDownloadItems(urls[0]);

				for (int i = 0; i < downloadItemURLList.size(); i++) {

					imageName = downloadItemNameList.get(i);
					pictureFile = new File(pictureFolder, imageName);
					try {
						conn = (HttpURLConnection) urls[0].get(i)
								.openConnection();
						conn.setDoInput(true);
						conn.connect();
						stream = conn.getInputStream();
						image = BitmapFactory.decodeStream(stream);
						fOut = new FileOutputStream(pictureFile);
						image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
						Log.i(TAG, "got image");
						stream.close();
						fOut.flush();
						fOut.close();
						conn.disconnect();
					} catch (Exception e) {
						// Log.v("img", e.getMessage());
						Log.e(TAG, "error downloading the image");
					}
				}
			} else
				Log.e(TAG, "SD card not found");
			return null;
		}
	}
}