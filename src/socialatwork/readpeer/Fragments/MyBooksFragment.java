package socialatwork.readpeer.Fragments;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.R;
import socialatwork.readpeer.ReadBookActivity;
import socialatwork.readpeer.ReadBookHtmlActivity;
import socialatwork.readpeer.WebRelatedComponents.bookDownloader;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MyBooksFragment extends Fragment {

	private View view;// Cache page view
	private GridView bookShelf;
	private String uid;
	private String access_token;
	private final String TAG = "my book fragment";

	// private static String myBookInfo;
	private ArrayList<String> bookNames;
	private ArrayList<String> bookIDs;
	private ArrayList<Drawable> bookCovers;
	// update every time when user login
	private ArrayList<String> readingListBookIDsOnServer;
	private ArrayList<String> readingListBookNamesOnServer;
	private ArrayList<String> readingListBookCoverURLsOnServer;
	private int readingCountOnServer;
	private String readingBooksInfo;
	boolean isUserBookDeletedOnServer = false;

	private static int myBookNum = 0;
	private ProgressDialog dialog = null;
	private ShelfAdapter mAdapter;
	private tdHttpClient mHttpClient;
	private bookDownloader mBookDownloader;

	private final float BOOK_TITLE_FONT_SIZE_BIG = 15;
	private final float BOOK_TITLE_FONT_SIZE_MEDIUM = 12;
	private final float BOOK_TITLE_FONT_SIZE_SMALL = 10;
	private final int BOOK_MANAGEMENT_DIALOG_WIDTH = 600;
	private final float BOOK_MANAGEMENT_DIALOG_ALPHA = 0.9f;
	private final float BOOK_MANAGEMENT_DIALOG_DIM = 0.8f;

	private final String GET_READING_LIST_FROM_SERVER_FAIL = "Fail to update user reading list!";
	private final int HANDLER_GET_READING_LIST_INFO_DONE = 2;
	private final int HANDLER_GET_READING_LIST_INFO_FAIL = 3;
	private final int LOADING_DIALOG_TIME_OUT = 4;
	private final long loading_dialog_time_out_duration = 3000l;

	/* Ensure adapter setting is after book data loading finish */
	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				dialog.dismiss();
				mAdapter = new ShelfAdapter(getActivity(), bookNames);
				bookShelf.setAdapter(mAdapter);
				break;
			case LOADING_DIALOG_TIME_OUT:
				if (dialog != null && dialog.isShowing()) {
					dialog.dismiss();
				}
			default:
				break;
			}
		}
	};

	// Use this handler to update reading list every time user login
	final Handler readingListUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_GET_READING_LIST_INFO_DONE:
				readingListBookIDsOnServer = new ArrayList<String>();
				readingListBookNamesOnServer = new ArrayList<String>();
				readingListBookCoverURLsOnServer = new ArrayList<String>();
				JSONObject booksInfoObject;
				try {
					booksInfoObject = new JSONObject(readingBooksInfo);
					JSONArray booksInfoList = booksInfoObject
							.getJSONArray("books");
					int bookCount = booksInfoList.length();
					readingCountOnServer = bookCount;
					for (int i = 0; i < bookCount; i++) {
						JSONObject singleBookInfoObject = (JSONObject) booksInfoList
								.get(bookCount);
						String bookID = singleBookInfoObject.getString("bid");
						readingListBookIDsOnServer.add(bookID);
						String bookName = singleBookInfoObject
								.getString("title");
						readingListBookNamesOnServer.add(bookName);
						String bookCoverUrl = singleBookInfoObject
								.getString("book_cover");
						readingListBookCoverURLsOnServer.add(bookCoverUrl);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				break;
			case HANDLER_GET_READING_LIST_INFO_FAIL:
				Toast.makeText(getActivity().getApplicationContext(),
						GET_READING_LIST_FROM_SERVER_FAIL, Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	// newInstance constructor for creating fragment
	public static MyBooksFragment newInstance() {
		MyBooksFragment f = new MyBooksFragment();
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHttpClient = tdHttpClient.getClientInstance();
		Log.d(TAG, "on create");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		//Log.d(TAG, "on create view");
		if (view == null) {
			view = inflater.inflate(R.layout.fragment_mybook, container, false);
		}
		// Remove parent if any
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			parent.removeView(view);
		}
		// Dialog ask user to wait
		dialog = ProgressDialog.show(getActivity(), "Loading",
				"Loading contents for you");
		Message timeOutMsg = new Message();
		timeOutMsg.what = LOADING_DIALOG_TIME_OUT;
		handler.sendMessageAtTime(timeOutMsg, loading_dialog_time_out_duration);

		if (getArguments() != null) {
			uid = getArguments().getString("uid");
			Log.i(TAG, uid);
			access_token = getArguments().getString("access_token");
			Log.i(TAG, access_token);
		} else {
			Log.i(TAG, "arguments is null");
		}

		bookNames = new ArrayList<String>();
		bookCovers = new ArrayList<Drawable>();
		bookIDs = new ArrayList<String>();
		// Get local & remote user reading books
		updateLocalBooks();
		try {
			getUserReadingList(access_token, uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Must sync after both side finish ufpdating
		try {
			// localAndServerReadingSynchronize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		bookShelf = (GridView) view.findViewById(R.id.book_shelf);
		bookShelf.setLongClickable(true);

		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		double diagonalPixels = Math.sqrt((Math.pow(dm.widthPixels, 2) + Math
				.pow(dm.heightPixels, 2)));
		double screenSize = diagonalPixels / (160 * dm.density);
		Log.d(TAG, "getScreenSize() physical size£º " + screenSize);

		if (screenSize > 6) {
			// tablet
			Log.d(TAG, "Tablet size");
			bookShelf.setNumColumns(4);
		} else {
			// phone
			Log.d(TAG, "Phone size");
			bookShelf.setNumColumns(3);
		}

		// Click item -> open book
		bookShelf.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				String bookID = bookIDs.get(index);
				String bookTitle = bookNames.get(index);

				if (index >= myBookNum) {

				} else {
					//Intent openBookIntent = new Intent(getActivity(),ReadBookActivity.class);
					Intent openBookHtmlIntent = new Intent(getActivity(),ReadBookHtmlActivity.class);
					openBookHtmlIntent.putExtra("bid", bookID);
					openBookHtmlIntent.putExtra("uid", uid);
					openBookHtmlIntent.putExtra("access_token", access_token);
					openBookHtmlIntent.putExtra("book name", bookTitle);
					startActivity(openBookHtmlIntent);
				}
			}
		});

		// Long press item -> delete/favorite dialog
		bookShelf.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int index, long arg3) {

				final String bookID = bookIDs.get(index);
				final String bookTitle = bookNames.get(index);
				Dialog bookManageDialog = new Dialog(getActivity());
				bookManageDialog.setTitle("Manage this book");
				bookManageDialog.setCanceledOnTouchOutside(true);
				bookManageDialog.setContentView(R.layout.dialog_book_manage);

				Window mWindow = bookManageDialog.getWindow();
				WindowManager.LayoutParams lp = mWindow.getAttributes();
				lp.alpha = BOOK_MANAGEMENT_DIALOG_ALPHA;
				lp.dimAmount = BOOK_MANAGEMENT_DIALOG_DIM;
				lp.width = BOOK_MANAGEMENT_DIALOG_WIDTH;
				mWindow.setAttributes(lp);

				Button deleteBtn = (Button) bookManageDialog
						.findViewById(R.id.btn_delete);
				Drawable iconDelete = getResources().getDrawable(
						R.drawable.icon_delete);
				deleteBtn.setCompoundDrawablesWithIntrinsicBounds(iconDelete,
						null, null, null);
				deleteBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						boolean isDeleted = deleteALocalBook(bookTitle, bookID);
						if (isDeleted) {
							bookNames.remove(index);
							bookIDs.remove(index);
							bookCovers.remove(index);
							mAdapter.notifyDataSetChanged();
							Toast.makeText(getActivity(), "Deletion completed",
									Toast.LENGTH_SHORT).show();
						} else {
							Log.e(TAG, "Book deletion failed!");
						}
					}
				});

				Button favoriteBtn = (Button) bookManageDialog
						.findViewById(R.id.btn_add_to_favorite);
				Drawable iconFavorite = getResources().getDrawable(
						R.drawable.icon_favorite);
				favoriteBtn.setCompoundDrawablesWithIntrinsicBounds(
						iconFavorite, null, null, null);
				favoriteBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

					}
				});

				bookManageDialog.show();
				return true;
			}
		});
		return view;
	}

	// Public for use in bookshelf activity
	public void updateLocalBooks() {

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				updateLocalBookInfoByScanningDisk();
				getBookCovers(myBookNum);
				Message msg = new Message();
				msg.what = 1;
				handler.sendMessage(msg);
			}
		}, 1500L);
	}
	
	private void updateLocalBookInfoByScanningDisk(){
		
		/* Reset data for reloading */
		bookNames.clear();
		bookIDs.clear();
		bookCovers.clear();
		Log.d(TAG, "Updating");
		File[] fileList = getLocalBookFiles();
		if (fileList != null) {
			myBookNum = fileList.length;
			Log.d(TAG,"book number:"+myBookNum);
		} else {
			myBookNum = 0;
		}
		
		for (int i = 0; i < myBookNum; i++) {
			// book index is in between "- -" of the file name
			String fileName = fileList[i].getName();
			//Log.d(TAG,"fileName:"+fileName);
			String bookIndex = fileName.split("-")[0];
			String bookName = fileName.split("-")[1];
			bookNames.add(bookName);
			bookIDs.add(bookIndex);
		}
	}

	private void localAndServerReadingSynchronize() throws Exception {
		if (myBookNum < readingCountOnServer) {// User add new reading on remote
			// Find out which books are new, download them
			// keep the ID of the new books
			ArrayList<String> newUserBookIDListOnServer = new ArrayList<String>();
			ArrayList<String> newUserBookNameListOnServer = new ArrayList<String>();
			ArrayList<String> newUserBookCoverURLListOnServer = new ArrayList<String>();
			mBookDownloader = bookDownloader.getDownloaderInstance();

			for (int i = 0; i < readingCountOnServer; i++) {

				String serverBookID = readingListBookIDsOnServer.get(i);
				if (!bookIDs.contains(serverBookID)) {
					newUserBookIDListOnServer.add(serverBookID);
					String newBookName = readingListBookNamesOnServer.get(i);
					downloadBook(serverBookID, newBookName);
					newUserBookNameListOnServer.add(newBookName);
					newUserBookCoverURLListOnServer
							.add(readingListBookCoverURLsOnServer.get(i));
				}
			}
		} else if (myBookNum > readingCountOnServer) {// User add new book
														// locally and haven't
														// add it on remote
			// compute how many unadded books, use as size of the bookID array
			int unaddedBookNum = myBookNum - readingCountOnServer;
			String[] unaddedUserBookList = new String[unaddedBookNum];
			int addedBookCount = 0;
			for (int i = 0; i < myBookNum; i++) {
				String localBookID = bookIDs.get(i);
				if (!readingListBookIDsOnServer.contains(localBookID)) {
					unaddedUserBookList[addedBookCount] = localBookID;
					addedBookCount++;
				}
			}
			new addUserBookOnServerTask().execute(unaddedUserBookList);
		}
	}

	private class addUserBookOnServerTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... bookIDs) {
			for (int i = 0; i < bookIDs.length; i++) {
				try {
					mHttpClient.addUserBook(bookIDs[i], access_token, uid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private void downloadBook(String bookID, String bookName) throws Exception {

		mBookDownloader.downloadBook(getActivity().getApplicationContext(),
				uid, bookID, access_token, bookName);
		// Add the newly downloaded books into book list
		bookNames.add(bookName);
		bookIDs.add(bookID);
		// bookCovers.add(index);
		mAdapter.notifyDataSetChanged();
	}

	private void getUserReadingList(final String access_token,
			final String userID) throws Exception {

		readingListUpdateHandler.post(new Runnable() {

			@Override
			public void run() {
				try {
					readingBooksInfo = mHttpClient.getUserReadings(userID,
							access_token);
					if (readingBooksInfo != null) {
						readingListUpdateHandler
								.sendEmptyMessage(HANDLER_GET_READING_LIST_INFO_DONE);
					} else {
						readingListUpdateHandler
								.sendEmptyMessage(HANDLER_GET_READING_LIST_INFO_FAIL);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private class deleteUserBookOnServerTask extends
			AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... bookID) {
			try {
				isUserBookDeletedOnServer = mHttpClient.deleteUserBook(
						bookID[0], access_token, uid);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	// Delete a book from local storage -> update user's library on server side
	private boolean deleteALocalBook(String bookName, final String bookID) {

		// delete from server side first
		// use global boolean <isUserBookDeletedOnServer> to indicate if
		// deletion is successful
		new deleteUserBookOnServerTask().execute(bookID);
		if (isUserBookDeletedOnServer) {
			// Reset the global boolean value
			isUserBookDeletedOnServer = false;
			if (Environment.getExternalStorageState().equals(
					android.os.Environment.MEDIA_MOUNTED)) {
				String storagePath = Environment.getExternalStorageDirectory()
						.getPath() + "/Readpeer/Books/";
				String bookPath = storagePath + bookName + "-" + bookID + "-"
						+ ".txt";
				File bookFile = new File(bookPath);
				boolean isDeleted = bookFile.delete();
				return isDeleted;

			} else {
				Log.e(TAG, "No SD card found");
				Toast.makeText(getActivity(), "Error: No SD card found",
						Toast.LENGTH_SHORT).show();
				return false;
			}
		} else {
			return false;
		}
	}

	private File[] getLocalBookFiles() {
		if (Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			String storagePath = Environment.getExternalStorageDirectory()
					.getPath() + "/Readpeer/Books/";
			File folder = new File(storagePath);
			File[] fileList = folder.listFiles();
			return fileList;
		} else {
			return null;
		}
	}

	class ShelfAdapter extends BaseAdapter {

		private Context context;
		private ArrayList<String> bookNames;

		public ShelfAdapter(Context context, ArrayList<String> bookNames) {
			this.context = context;
			this.bookNames = bookNames;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return bookNames.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return arg0;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView(int position, View contentView, ViewGroup arg2) {
			// TODO Auto-generated method stub

			contentView = LayoutInflater.from(
					getActivity().getApplicationContext()).inflate(
					R.layout.gridview_item_book, null);

			TextView tView = (TextView) contentView
					.findViewById(R.id.bookCoverTextView);
			ImageView iView = (ImageView) contentView
					.findViewById(R.id.bookCoverImageView);
			tView.setTextColor(Color.WHITE);

			//Log.i(TAG, "position:" + Integer.toString(position));
			if (bookNames.size() > position) {
				String bookTitle = bookNames.get(position);
				setTitleTextSize(tView, bookTitle);

				if (bookTitle != null) {
					tView.setText(bookTitle);
				} else {
					tView.setText("Loading");
				}
				if (bookCovers.get(position) != null) {
					iView.setBackgroundDrawable(bookCovers.get(position));
				} else {
					iView.setBackgroundResource(R.drawable.default_cover);
				}
			} else {
				contentView.setClickable(false);
				contentView.setVisibility(View.INVISIBLE);
			}
			return contentView;
		}
	}

	private void setTitleTextSize(TextView titleTextView, String title) {

		if (title.length() < 30) {
			titleTextView.setTextSize(BOOK_TITLE_FONT_SIZE_BIG);
		} else if (title.length() < 50) {
			titleTextView.setTextSize(BOOK_TITLE_FONT_SIZE_MEDIUM);
		} else {
			titleTextView.setTextSize(BOOK_TITLE_FONT_SIZE_SMALL);
		}
	}

	private void getBookCovers(int number) {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String applicationFolderPath = rootPath + "/Readpeer";
			String pictureFilePath = applicationFolderPath + "/Cache/Pictures/";
			for (int i = 0; i < number; i++) {
				String bookCoverImagePath = pictureFilePath + bookIDs.get(i)
						+ "-cover.jpg";
				bookCovers.add(Drawable.createFromPath(bookCoverImagePath));
			}
		}
	}
}
