package socialatwork.readpeer.WebRelatedComponents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class bookDownloader {

	private static bookDownloader downloaderInstance;
	private static String TAG = "bookDownloader";
	private tdHttpClient mHttpClient;
	private String[] bookContent;
	private int[] pageWordCount;
	private StringBuffer bookContentWhole;

	// 'Synchronized' is to ensure there is only one instance
	public static synchronized bookDownloader getDownloaderInstance() {
		if (downloaderInstance == null) {
			downloaderInstance = new bookDownloader();
		}
		return downloaderInstance;
	}

	public void downloadBook(Context context, String uid, String bookIndex,
			String access_token, String bookName) throws JSONException,
			Exception {

		mHttpClient = tdHttpClient.getClientInstance();
		bookContentWhole = new StringBuffer();

		JSONObject bookObject = new JSONObject(mHttpClient.getBookDetails(
				bookIndex, access_token));
		Log.d(TAG, "book detail:" + bookObject.toString());
		String code = bookObject.getString("code");
		String message = bookObject.getString("message");

		int pageCount = Integer.parseInt(bookObject.getString("total_page"));
		bookContent = new String[pageCount];
		pageWordCount = new int[pageCount];
		pageWordCount[0] = 0;
		JSONObject bookBodyAll = bookObject.getJSONObject("body");
		for (int i = 0; i < pageCount; i++) {
			JSONObject singlePageObject = new JSONObject(
					bookBodyAll.getString(Integer.toString(i + 1)));
			bookContent[i] = singlePageObject.getString("text");
			bookContent[i] = htmlParser.parseHtmlString(bookContent[i]);
			bookContent[i] = PDFParser.parsePDFString(bookContent[i]);
			bookContentWhole.append(bookContent[i]);
			if (i > 0) {
				pageWordCount[i] = pageWordCount[i - 1]
						+ bookContent[i].length();
			} else {
				pageWordCount[i] = pageWordCount[i] + bookContent[i].length();
			}
			Log.i(TAG,
					"page word count:"
							+ Integer.toString(bookContent[i].length()));
			Log.i(TAG,
					"accumulated count:" + Integer.toString(pageWordCount[i]));
		}
		Log.i(TAG, "code:" + code);
		Log.i(TAG, "message:" + message);

		if (code.equals("200")) {
			Log.i(TAG, "Get book info successfully");
		} else {
			Log.e(TAG, "error getting book info:" + code);
		}
		// add this book in reading list on server
		boolean isBookAdded = mHttpClient.addUserBook(bookIndex,
				access_token, uid);
		if (!isBookAdded) {
			Log.e(TAG, "New book adding on server failed");
		} else {
			Log.d(TAG, "New book adding on server succeeded");
		}
		
		saveBookIntoDisk(context, bookName, bookIndex,
				bookContentWhole.toString());
	}

	private void saveBookIntoDisk(Context context, String bookName,
			String bookIndex, String content) {
		try {
			String storagePath = Environment.getExternalStorageDirectory()
					.getPath();
			String folderPath = storagePath + "/Readpeer/Books";
			// Create book name contain book index
			String filename = bookName.replace("-", " ") + "-" + bookIndex
					+ "-" + ".txt";

			// A list to put in shared preferences to store paging information
			HashSet<String> pageWordCountSet = new HashSet<String>();
			for (int i = 0; i < pageWordCount.length; i++) {
				pageWordCountSet.add(Integer.toString(pageWordCount[i]));
			}

			// Save book information in shared preference
			SharedPreferences sp = context.getSharedPreferences("bookInfo",
					Context.MODE_PRIVATE);

			/*
			 * NOTICE!!! every sp.eidt() will return a different editor object !
			 * Must use editor.commit() to save changes !
			 */
			SharedPreferences.Editor editor = sp.edit();
			editor.putStringSet(bookIndex, pageWordCountSet);
			editor.commit();

			File folder = new File(folderPath);

			if (!folder.exists()) {
				folder.mkdirs();
			}

			File bookFile = new File(folderPath, filename);
			FileWriter writer = new FileWriter(bookFile);
			writer.append(content);
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
