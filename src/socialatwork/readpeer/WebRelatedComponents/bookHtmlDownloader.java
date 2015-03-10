package socialatwork.readpeer.WebRelatedComponents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class bookHtmlDownloader {

	private static bookHtmlDownloader downloaderInstance;
	private static String TAG = "bookDownloader";
	private tdHttpClient mHttpClient;
	private String[] bookHtmlPages;
	private int pageCount;
	private StringBuffer bookContentWhole;

	/* Script reference and meta information */
	//private final String JQUERY_REF = "<script src='jquery-1.8.3.js'></script>";
	//private final String RANGY_REF1 = "<script src='rangy-core.js'></script>";
	//private final String RANGY_REF2 = "<script src='rangy-serializer.js'></script>";
	//private final String RANGY_REF3 = "<script src='rangy-textrange.js'></script>";
	//private final String RANGY_REF4 = "<script src='android.selection.js'></script>";
	//private final String RANGY_REF5 = "<script src='rangy-highlighter.js'></script>";
	private final String HIGHLIGHTER_REF = "<script src='annotation.js'></script>";
	

	// 'Synchronized' is to ensure there is only one instance
	public static synchronized bookHtmlDownloader getDownloaderInstance() {
		if (downloaderInstance == null) {
			downloaderInstance = new bookHtmlDownloader();
		}
		return downloaderInstance;
	}

	public void downloadBookHtml(Context context, String uid, String bookIndex,
			String bookName, String author, String access_token)
			throws JSONException, Exception {

		mHttpClient = tdHttpClient.getClientInstance();
		bookContentWhole = new StringBuffer();

		JSONObject bookObject = new JSONObject(mHttpClient.getBookDetails(
				bookIndex, access_token));
		Log.d(TAG, "book detail:" + bookObject.toString());
		// String code = bookObject.getString("code");
		// String message = bookObject.getString("message");

		pageCount = Integer.parseInt(bookObject.getString("total_page"));
		bookHtmlPages = new String[pageCount];

		for (int i = 0; i < pageCount; i++) {
			// html page index starts with 1
			bookHtmlPages[i] = mHttpClient.getBookHtmlByPage(access_token,
					Integer.toString(i + 1), bookIndex);
			
			if (bookHtmlPages[i] != null) {
				Log.d(TAG, "Get page" + (i + 1) + "successfully");
				//bookHtmlPages[i] = injectScriptReference(bookHtmlPages[i]);
				Log.d(TAG,bookHtmlPages[i]);
			} else {
				Log.e(TAG, "Get page" + (i + 1) + "failed");
			}
		}
		boolean isBookAddedToReadingList = addBookToReadingList(bookIndex,
				access_token, uid);
		saveBookHtmlIntoDisk(context, bookIndex, bookName, author,
				bookHtmlPages);
	}

	/*
	 * This method inject reference to javascript files into downloaded html
	 * pages
	 */
	private String injectScriptReference(String content) {

		int position = content.indexOf("<head>");
		String part1 = content.substring(0, position);
		Log.d(TAG,"substring 1: "+ part1);
		String part2 = content.substring(position,content.length());
		Log.d(TAG,"substring 2: "+part2);
		
		//String test = "ddd<head>1234567890</head>sssss";
		//String[] parts = test.split("</head>", 2);
		//Log.d(TAG,"part 1: "+ parts[0]);
		//Log.d(TAG,"part 2: "+ parts[1]);
		StringBuffer sb = new StringBuffer("");
		//sb.append(JQUERY_REF);
		//sb.append("\n");
		//sb.append(RANGY_REF1);
		//sb.append("\n");
		//sb.append(RANGY_REF2);
		//sb.append("\n");
		//sb.append(RANGY_REF3);
		//sb.append("\n");
		//sb.append(RANGY_REF4);
		//sb.append("\n");
		//sb.append(RANGY_REF5);
		//sb.append("\n");
		sb.append(HIGHLIGHTER_REF);
		sb.append("\n");
		sb.append(content);
		return sb.toString();
	}

	private boolean addBookToReadingList(String bookID, String access_token,
			String userID) throws Exception {
		// add this book in reading list on server
		boolean isBookAdded = mHttpClient.addUserBook(bookID, access_token,
				userID);
		if (!isBookAdded) {
			Log.e(TAG, "New book adding on server failed");
		} else {
			Log.d(TAG, "New book adding on server succeeded");
		}
		return isBookAdded;
	}

	private void saveBookHtmlIntoDisk(Context context, String bookIndex,
			String bookName, String author, String[] contentHtmls)
			throws IOException {

		Boolean isSDPresent = android.os.Environment.getExternalStorageState()
				.equals(android.os.Environment.MEDIA_MOUNTED);
		if (isSDPresent) {
			String storagePath = Environment.getExternalStorageDirectory()
					.getPath();
			String folderPath = storagePath + "/Readpeer/Books/" + bookIndex
					+ "-" + bookName;
			File folder = new File(folderPath);
			if (!folder.exists()) {
				folder.mkdirs();
			}
			saveBookInfoOnDisk(bookIndex, bookName, author, folderPath);
			// save html by page on disk
			for (int i = 0; i < contentHtmls.length; i++) {

				String htmlFileName = bookIndex + "-" + (i + 1) + ".html";
				File bookHtmlFile = new File(folderPath, htmlFileName);
				FileWriter htmlWriter = new FileWriter(bookHtmlFile);
				htmlWriter.append(contentHtmls[i]);
				htmlWriter.flush();
				htmlWriter.close();
			}
		} else {
			Log.e(TAG, "SD not available");
		}
	}

	private void saveBookInfoOnDisk(String bookIndex, String bookName,
			String author, String folderPath) throws IOException {

		String bookInfoFileName = bookIndex + "-info.txt";
		String bookInfoContent = "book_name=" + bookName + "\n" + "author="
				+ author;
		File bookInfoFile = new File(folderPath, bookInfoFileName);
		FileWriter bookInfoWriter = new FileWriter(bookInfoFile);
		bookInfoWriter.append(bookInfoContent);
		bookInfoWriter.flush();
		bookInfoWriter.close();
	}
}
