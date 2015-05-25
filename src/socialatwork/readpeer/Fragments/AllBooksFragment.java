package socialatwork.readpeer.Fragments;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.ImageNameGenerator;
import socialatwork.readpeer.R;
import socialatwork.readpeer.ReadBookActivity;
import socialatwork.readpeer.Cache.ImageLoader;
import socialatwork.readpeer.Cache.tdCacheManager;
import socialatwork.readpeer.WebRelatedComponents.ReturnedContentHandler;
import socialatwork.readpeer.WebRelatedComponents.bookDownloader;
import socialatwork.readpeer.WebRelatedComponents.bookHtmlDownloader;
import socialatwork.readpeer.WebRelatedComponents.onlinePictureDownloader;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class AllBooksFragment extends Fragment {

	private View view;// Cache page view
	private GridView bookShelf;
	private String uid;
	private String access_token;
	private final String TAG = "all book fragment";

	private String selectedBookID = null;
	private String selectedBookTitle = null;
	private String selectedBookAuthor = null;
	private String selectedBookTotalPage = null;
	private String selectedBookAnnotationCount = null;

	private onlinePictureDownloader picDownloader;
	private bookHtmlDownloader mBookHtmlDownloader;

	private ProgressDialog loadingDialog = null;
	private Dialog bookDetailDialog;

	// Handlers messages
	private int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	// private final int GET_BOOK_DONE = 1;
	private final int BOOK_DOWNLOAD_FINISHED = 2;
	private final int GET_BOOK_INFO_DONE = 3;
	private final int LOADING_DIALOG_TIME_OUT = 4;
	private int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;
	private final long loading_dialog_time_out_duration = 3000l;

	private ArrayList<String> mLocalBookIndexList;
	private String bookInfoByPage;
	private ArrayList<String> bookNames;
	private ArrayList<String> bookNamesPlusOne;
	private int bookNumber;
	private ArrayList<URL> bookCoverURLs;
	private ArrayList<String> singleBookInfo;
	private ArrayList<JSONObject> bookInfoJSONObjects;
	private ArrayList<Drawable> bookCovers;
	private int currentBookstorePage = 0;

	private final float BOOK_TITLE_FONT_SIZE_BIG = 15;
	private final float BOOK_TITLE_FONT_SIZE_MEDIUM = 12;
	private final float BOOK_TITLE_FONT_SIZE_SMALL = 10;
	private final long DEFINED_WAITING_TIME = 800L;

	private ShelfAdapter mAdapter;
	private tdCacheManager mCacheManager;

	private final int BOOKTYPE_MY = 0;
	private final int BOOKTYPE_ALL = 1;
	private final int BOOKTYPE_POPULAR = 2;

	// Type for parsing JSON string
	private static int TYPE_STRING = 1;
	private static int TYPE_INT = 2;
	private static int TYPE_BOOK = 3;

	private tdHttpClient mHttpClient;
	private static ReturnedContentHandler mContentHandler;

	/* Ensure adapter setting is after book data loading finish */
	/*
	 * Handle situations when book cover is downloaded, or book covers are
	 * loaded, or book content is downloaded
	 */
	final Handler bookDataHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE
					|| msg.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE) {
				Log.d(TAG,
						"cover images are downloaded, now can load from local cache");
				/* Must load required data before set adapter, very important! */
				try {
					getBookCovers();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (loadingDialog != null && loadingDialog.isShowing()) {
					loadingDialog.dismiss();
				}
				Log.d(TAG, "setting up grid view adapter");
				mAdapter = new ShelfAdapter(getActivity(), bookCoverURLs);
				bookShelf.setAdapter(mAdapter);

				// Message getCoverDoneMsg = new Message();
				// getCoverDoneMsg.what = GET_BOOK_DONE;
				// this.sendMessage(getCoverDoneMsg);
			}

			else if (msg.what == BOOK_DOWNLOAD_FINISHED) {

				if (bookDetailDialog != null) {
					bookDetailDialog.dismiss();
					Toast.makeText(getActivity().getApplicationContext(),
							"Download complete!", Toast.LENGTH_SHORT).show();
				}
			}

			// Only start to get book covers after finishing loading book info
			else if (msg.what == GET_BOOK_INFO_DONE) {
				try {
					downloadUncachedBookCovers();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			else if (msg.what == LOADING_DIALOG_TIME_OUT) {
				if (loadingDialog != null && loadingDialog.isShowing()) {
					loadingDialog.dismiss();
				}
			}
		}
	};

	// newInstance constructor for creating fragment
	public static AllBooksFragment newInstance() {
		AllBooksFragment f = new AllBooksFragment();
		return f;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.d(TAG, "On create fragment");
		super.onCreate(savedInstanceState);
		mBookHtmlDownloader = bookHtmlDownloader.getDownloaderInstance();
		loadLocalBookIndex();

		mHttpClient = tdHttpClient.getClientInstance();
		mContentHandler = ReturnedContentHandler.getHandlerInstance();
		mCacheManager = tdCacheManager.getCacheManagerInstance(getActivity());

		try {
			getUserInfo();
			// new thread to download book info
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						loadBookInfo(BOOKTYPE_POPULAR, 0);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();

		} catch (NullArgumentsException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getUserInfo() throws NullArgumentsException {
		if (getArguments() != null) {
			uid = getArguments().getString("uid");
			Log.i(TAG, uid);
			access_token = getArguments().getString("access_token");
			Log.i(TAG, access_token);
		} else {
			Log.e(TAG, "arguments is null");
			throw new NullArgumentsException("Arguments are null");
		}
	}

	private class NullArgumentsException extends Exception {

		private static final long serialVersionUID = 1L;

		private NullArgumentsException(String msg) {
			super(msg);
		}
	}

	// Search local storage to find local book index
	private void loadLocalBookIndex() {

		mLocalBookIndexList = new ArrayList<String>();
		File[] localBookList = getLocalBookFiles();
		if (localBookList != null) {
			for (int i = 0; i < localBookList.length; i++) {
				String fileName = localBookList[i].getName();
				String index = fileName.split("-")[0];
				Log.d(TAG, "Adding index:" + index);
				mLocalBookIndexList.add(index);
			}
		} else {
			Log.d(TAG, "No local books");
		}
	}

	// Check if certain book is in local storage by comparing
	// book id with mLocalBookIndexList
	private boolean isThisBookInLocalStorage(String bookIndex) {
		for (int i = 0; i < mLocalBookIndexList.size(); i++) {
			if (mLocalBookIndexList.get(i).compareTo(bookIndex) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d(TAG, "On create view");
		if (view == null) {
			view = inflater.inflate(R.layout.fragment_bookshelf, container,
					false);
		}
		// Remove parent if any
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			parent.removeView(view);
		}

		// Dialog ask user to wait
		loadingDialog = ProgressDialog.show(getActivity(), "Loading",
				"Loading contents for you");
		Message timeOutMsg = new Message();
		timeOutMsg.what = LOADING_DIALOG_TIME_OUT;
		bookDataHandler.sendMessageAtTime(timeOutMsg,
				loading_dialog_time_out_duration);

		bookShelf = (GridView) view.findViewById(R.id.book_shelf);
		bookShelf.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {

				if (index < bookNamesPlusOne.size()) {
					if (index < bookNamesPlusOne.size() - 1) {
						try {

							JSONObject book = bookInfoJSONObjects.get(index);
							selectedBookTitle = book.getString("title")
									.toString();
							Log.i(TAG, "book name:" + selectedBookTitle);
							selectedBookAuthor = book.getString("author");
							if (selectedBookAuthor == null) {
								selectedBookAuthor = "Unknown";
							}
							selectedBookAnnotationCount = book
									.getString("annotation_count");
							selectedBookTotalPage = book
									.getString("total_page");
							selectedBookID = book.getString("bid").toString();
							Log.i(TAG, "bid:" + selectedBookID);

						} catch (JSONException e1) {
							e1.printStackTrace();
						}

						bookDetailDialog = new Dialog(getActivity(),
								R.style.dialog_bookInfo);
						bookDetailDialog
								.requestWindowFeature(Window.FEATURE_NO_TITLE);
						bookDetailDialog
								.setContentView(R.layout.dialog_book_details);
						bookDetailDialog.setCanceledOnTouchOutside(true);

						ImageView bookCoverView = (ImageView) bookDetailDialog
								.findViewById(R.id.imageview_bookCover);

						// require API Level 16
						// bookCoverView.setBackground(bookCovers[index]);
						bookCoverView.setBackgroundDrawable(bookCovers
								.get(index));

						TextView bookTitleView = (TextView) bookDetailDialog
								.findViewById(R.id.textview_bookTitle);
						bookTitleView.setText(selectedBookTitle);

						TextView bookDetailView = (TextView) bookDetailDialog
								.findViewById(R.id.textview_bookInfo);
						bookDetailView.setText("Author:" + selectedBookAuthor
								+ "\n\n" + "Annotation:"
								+ selectedBookAnnotationCount + "\n\n"
								+ "Total pages:" + selectedBookTotalPage
								+ "\n\n");

						Button downloadBtn = (Button) bookDetailDialog
								.findViewById(R.id.btn_getBook);

						// Set button at download if book not exist in storage
						if (!isThisBookInLocalStorage(selectedBookID)) {
							downloadBtn.setText("Download");
							downloadBtn
									.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View arg0) {
											downloadBookHtmlRunnable r = new downloadBookHtmlRunnable();
											new Thread(r).start();
										}
									});
						} else {
							Log.d(TAG, "Has this book in storage");
							downloadBtn.setText("Read Now");
							downloadBtn
									.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View arg0) {
											Intent openBookIntent = new Intent(
													getActivity(),
													ReadBookActivity.class);
											openBookIntent.putExtra("bid",
													selectedBookID);
											openBookIntent.putExtra("uid", uid);
											openBookIntent.putExtra(
													"access_token",
													access_token);
											openBookIntent.putExtra(
													"book name",
													selectedBookTitle);
											bookDetailDialog.dismiss();
											startActivity(openBookIntent);
										}
									});
						}

						bookDetailDialog.show();
						// Last item for page jumping
					} else if (index == bookNamesPlusOne.size() - 1) {
						final Dialog pageJumpDialog = new Dialog(getActivity());
						pageJumpDialog.setTitle("Load books on other pages");
						pageJumpDialog
								.setContentView(R.layout.dialog_bookstore_page_jump);
						pageJumpDialog.setCanceledOnTouchOutside(true);
						final TextView pageIndicator = (TextView) pageJumpDialog
								.findViewById(R.id.text_page_indicator);
						pageIndicator.setText("Page "
								+ Integer.toString(currentBookstorePage));
						final SeekBar pageSeekBar = (SeekBar) pageJumpDialog
								.findViewById(R.id.page_jump_seekbar);
						pageSeekBar.setMax(100);
						pageSeekBar.setProgress(currentBookstorePage);
						pageSeekBar
								.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

									@Override
									public void onProgressChanged(
											SeekBar seekBar, int progress,
											boolean fromUser) {
										pageIndicator.setText("Page "
												+ Integer.toString(progress));
									}

									@Override
									public void onStartTrackingTouch(
											SeekBar seekBar) {
										// TODO Auto-generated method stub
									}

									@Override
									public void onStopTrackingTouch(
											SeekBar seekBar) {
										// TODO Auto-generated method stub
									}

								});
						Button jumpToButton = (Button) pageJumpDialog
								.findViewById(R.id.btn_jump_to);
						jumpToButton.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								int pageChosen = pageSeekBar.getProgress();
								pageJumpDialog.dismiss();
								// Dialog ask user to wait
								loadingDialog = ProgressDialog.show(
										getActivity(), "Loading",
										"Loading contents for you");
								try {
									loadBookInfo(BOOKTYPE_ALL, pageChosen);
									downloadUncachedBookCovers();
								} catch (Exception e) {
									e.printStackTrace();
								}
								// mAdapter.notifyDataSetChanged();
								Toast.makeText(getActivity(),
										"Jump to page " + pageChosen,
										Toast.LENGTH_LONG).show();
							}
						});
						ImageButton pageBackOneButton = (ImageButton) pageJumpDialog
								.findViewById(R.id.btn_page_backone);
						pageBackOneButton
								.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(View v) {
										int pageNow = pageSeekBar.getProgress();
										if (pageNow > 0) {
											pageSeekBar
													.setProgress(pageNow - 1);
										}
									}
								});
						ImageButton pageNextOneButton = (ImageButton) pageJumpDialog
								.findViewById(R.id.btn_page_nextone);
						pageNextOneButton
								.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(View v) {
										int pageNow = pageSeekBar.getProgress();
										if (pageNow < pageSeekBar.getMax()) {
											pageSeekBar
													.setProgress(pageNow + 1);
										}
									}
								});

						pageJumpDialog.show();
					}
				}
			}
		});
		return view;
	}// End of onCreateView

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

	class downloadBookHtmlRunnable implements Runnable {
		@Override
		public void run() {
			try {

				String html = mHttpClient.getBookHtmlByPage(access_token, "1",
						selectedBookID);
				Log.d(TAG, "html:" + html);
				mBookHtmlDownloader.downloadBookHtml(getActivity()
						.getApplicationContext(), uid, selectedBookID,
						selectedBookTitle, selectedBookAuthor, access_token);
				Message m = new Message();
				m.what = BOOK_DOWNLOAD_FINISHED;
				bookDataHandler.sendMessage(m);
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class ViewHolder {
		TextView mTextView;
		ImageView mImageView;
	}

	class ShelfAdapter extends BaseAdapter {

		private Context context;
		private ArrayList<URL> bookCoverURLs;
		private int itemCount;
		private boolean mBusy = false;

		public ShelfAdapter(Context context, ArrayList<URL> bookCoverURLs) {
			this.context = context;
			// Plus 1 for last item "load more"
			this.bookCoverURLs = bookCoverURLs;
			this.itemCount = bookCoverURLs.size() + 1;
		}

		public void setFlagBusy(boolean isBusy) {
			this.mBusy = isBusy;
		}

		@Override
		public int getCount() {
			// Plus 1 item for "load more" at last
			return itemCount;
		}

		@Override
		public Object getItem(int arg0) {
			return arg0;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(final int position, View contentView,
				ViewGroup parent) {

			contentView = LayoutInflater.from(
					getActivity().getApplicationContext()).inflate(
					R.layout.gridview_item_book, null);
			ViewHolder mViewHolder = new ViewHolder();
			mViewHolder.mTextView = (TextView) contentView
					.findViewById(R.id.bookCoverTextView);
			if (mViewHolder.mTextView == null) {
				Log.w(TAG, "ViewHolder Textview is NULL");
			}
			mViewHolder.mImageView = (ImageView) contentView
					.findViewById(R.id.bookCoverImageView);
			mViewHolder.mTextView.setTextColor(Color.WHITE);
			if (position < bookNamesPlusOne.size()) {
				// Normal book item
				if (position < bookNamesPlusOne.size() - 1) {
					String bookTitle = bookNamesPlusOne.get(position);

					if (bookTitle != null) {
						mViewHolder.mTextView.setText(bookTitle);
						// Set title font size according to title length
						setTitleTextSize(mViewHolder.mTextView, bookTitle);
					} else {
						mViewHolder.mTextView.setText("Unknown");
						setTitleTextSize(mViewHolder.mTextView, "unknown");
					}

					if (bookCovers.get(position) != null) {
						// Require API Level 16
						// tView.setBackground(bookCovers[position]);
						mViewHolder.mImageView.setBackgroundDrawable(bookCovers
								.get(position));
					} else {
						mViewHolder.mImageView
								.setBackgroundResource(R.drawable.default_cover);
					}

				}
				// Last item, load page button
				else if (position == bookNamesPlusOne.size() - 1) {
					mViewHolder.mTextView.setText("Load other pages");
					mViewHolder.mImageView
							.setBackgroundResource(R.drawable.icon_load_more);
				}
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

	private String getBookInfoByPage(int category, int page) throws Exception {
		Log.d(TAG, "category:" + category);
		Log.d(TAG, "page:" + page);
		String bookInfo = mHttpClient.getBooks(access_token, category,
				Integer.toString(page));
		return bookInfo;
	}

	private void loadBookInfo(int category, int pageIndex) throws Exception {

		bookInfoByPage = new String();
		bookNames = new ArrayList<String>();
		bookNamesPlusOne = new ArrayList<String>();
		bookNumber = 0;
		bookCoverURLs = new ArrayList<URL>();
		singleBookInfo = new ArrayList<String>();
		bookInfoJSONObjects = new ArrayList<JSONObject>();
		currentBookstorePage = pageIndex;

		bookInfoByPage = getBookInfoByPage(category, pageIndex);
		int bookNumInThisPage = Integer.valueOf(mContentHandler
				.getValueFromContentReturned(bookInfoByPage, "number_of_books",
						TYPE_INT));
		Log.d(TAG, "number of books in current page:" + bookNumInThisPage);
		bookNumber = bookNumInThisPage;

		JSONArray infoOfAllBookInthisPage = new JSONArray(
				mContentHandler.getValueFromContentReturned(bookInfoByPage,
						"books", TYPE_BOOK));

		singleBookInfo.add(infoOfAllBookInthisPage.toString());
		for (int k = 0; k < bookNumInThisPage; k++) {
			JSONObject book = (JSONObject) infoOfAllBookInthisPage.get(k);
			bookInfoJSONObjects.add(book);
			Log.d(TAG, book.toString());
			if (!book.get("title").toString().isEmpty()) {
				bookNames.add(book.get("title").toString());
				Log.d(TAG, "book name" + book.get("title").toString());
			} else {
				bookNames.add("No Title");
			}
			String bookCoverLink = (book.get("book_cover").toString());
			Log.d(TAG, bookCoverLink);
			bookCoverURLs.add(new URL(bookCoverLink));
		}

		bookNamesPlusOne = bookNames;
		// Add the last book, server as a button to load more books
		bookNamesPlusOne.add("Load more books");
		bookDataHandler.sendEmptyMessage(GET_BOOK_INFO_DONE);
	}

	// load book covers from cache
	private void getBookCovers() throws JSONException {

		Log.d(TAG, "getting book covers from local cache");
		bookCovers = new ArrayList<Drawable>();
		String rootPath = Environment.getExternalStorageDirectory().getPath();
		String applicationFolderPath = rootPath + "/Readpeer";
		String cacheFilePath = applicationFolderPath + "/Cache/Pictures/";

		for (int i = 0; i < bookNumber; i++) {
			JSONObject book = bookInfoJSONObjects.get(i);
			String bookCoverImagePath = cacheFilePath
					+ book.getString("bid").toString() + "-cover.jpg";
			Drawable bookCoverDrawable = Drawable
					.createFromPath(bookCoverImagePath);
			if (bookCoverDrawable != null) {
				Log.d(TAG, "Adding cover drawable");
				bookCovers.add(Drawable.createFromPath(bookCoverImagePath));
			} else {
				Log.e(TAG,
						"Book cover drawable created from local path is null");
				bookCovers.add(null);
			}
		}
	}

	// download covers that are not stored in local cache from server
	// after completion, this method will invoke
	// getBookCovers() to update UI
	private void downloadUncachedBookCovers() throws Exception {

		ArrayList<String> bookCoverImageNames = new ArrayList<String>();
		for (int i = 0; i < bookNumber; i++) {
			JSONObject book = bookInfoJSONObjects.get(i);
			String bookIndex = book.getString("bid").toString();
			bookCoverImageNames.add(bookIndex + "-cover.jpg");
		}
		mCacheManager.downloadUncachedImages(bookCoverURLs,
				bookCoverImageNames, bookDataHandler);
	}
}
