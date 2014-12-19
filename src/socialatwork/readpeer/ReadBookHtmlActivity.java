package socialatwork.readpeer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.bossturban.webviewmarker.TextSelectionSupport;

public class ReadBookHtmlActivity extends FragmentActivity {

	private static WebView mWebView;
	private final String TAG = "Read Book Html";
	// Basic information
	private String mBookIndex;
	private String mBookName;
	private String access_token;
	private String mUid;
	/* Page number of html page, as a parameter used in annotation uploading */
	private int mPageIndex;
	private String mHighlight;

	/* Must be an odd number >= 3 to have the central page index */
	private final int VIEW_PAGER_WINDOW_SIZE = 5;
	/* Page index of the pages loaded in view pager */
	private int[] viewPagerIndexList = new int[VIEW_PAGER_WINDOW_SIZE];
	// private static int pageWindowCentralIndex = 0;
	// Html file related
	private ArrayList<String> mHtmlFilePathList;
	private int mHtmlFileNumber;
	private final int HANDLER_MESSAGE_GET_HTML_DONE = 0;
	private List<WebView> mWebViewList;
	private MyPagerAdapter mPagerAdapter;
	private ViewPager mViewPager;
	private ActionMode mActionMode = null;

	private static Dialog mHighlightDialog;
	/*
	 * the string used to separate highlight text and its offset, which are
	 * combined as one string when passed from JS
	 */
	private final String hightlightAndOffsetSeparator = "_";
	/* Set to ignore meaningless highlights */
	private final int HIGHLIGHT_MINIMAL_LENGTH = 6;

	// private TextSelectionSupport mTextSelectionSupport;

	/* Customized context action bar (CAB) */
	@Override
	public void onActionModeStarted(ActionMode m) {
		Log.d(TAG, "action mode stared");
		if (mActionMode == null) {
			mActionMode = m;
			Menu menu = m.getMenu();
			// Remove the default menu items
			menu.clear();
			Log.d(TAG, "size of menu: " + menu.size());
			m.getMenuInflater()
					.inflate(R.menu.read_contextual_action_bar, menu);
			MenuItem firstItem = menu.getItem(0);
			firstItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Log.d(TAG, "menu item " + item.getTitle() + " is clicked");
					if (mWebView == null) {
						mWebView = getCurrentWebView();
					}
					Log.d(TAG, "about to load javascript");

					mWebView.post(new Runnable() {
						@TargetApi(19)
						@Override
						public void run() {
							mWebView.evaluateJavascript("javascript:android.selection.getSelectionOffset();", null);
						}
					});
					return true;
				}
			});
		}
		super.onActionModeStarted(m);
	}

	public WebView getCurrentWebView() {

		if (mViewPager == null) {
			if (mPagerAdapter == null) {
				mPagerAdapter = new MyPagerAdapter();
			}
			setUpViewPager();
		}
		int index = mViewPager.getCurrentItem();
		return (WebView) mViewPager.getChildAt(index);
	}

	@Override
	public void onActionModeFinished(ActionMode m) {
		mActionMode = null;
		Log.d(TAG, "action mode is finished");
		super.onActionModeFinished(m);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "options menu item selected");
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_read_book_html);
		setupBasicValues();
		setWebViewList(0, VIEW_PAGER_WINDOW_SIZE);
		// mWebView = (WebView) findViewById(R.id.read_book_webview);
		mPagerAdapter = new MyPagerAdapter();
		setUpViewPager();
		mHighlightDialog = new Dialog(ReadBookHtmlActivity.this,
				R.style.dialog_account);
		mHighlightDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mHighlightDialog.setContentView(R.layout.dialog_highlight_menu);
		mHighlightDialog.setCanceledOnTouchOutside(true);
		Button annotateCancelButton = (Button) mHighlightDialog
				.findViewById(R.id.btn_cancel_annotate);
		annotateCancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mHighlightDialog != null) {
					mHighlightDialog.dismiss();
				}
			}
		});

		for (int i = 0; i < mWebViewList.size(); i++) {
			addView(mPagerAdapter, mWebViewList.get(i));
		}

	}

	private void setUpViewPager() {

		mViewPager = (ViewPager) findViewById(R.id.read_book_viewPager);
		if (mPagerAdapter != null) {
			mViewPager.setAdapter(mPagerAdapter);
		}
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageSelected(int index) {
				// Update current html page number
				mPageIndex = viewPagerIndexList[index];
				Log.d(TAG, "corresponding html page index: " + mPageIndex);

				View currentView = getCurrentPage(mPagerAdapter, mViewPager);
				// Log.d(TAG, "selected index: " + index);
				// Log.d(TAG, "total file number: " + mHtmlFileNumber);
				Log.d(TAG, "total views: " + mViewPager.getChildCount());
				Log.d(TAG, "current item: " + mViewPager.getCurrentItem());

				// Delete the first one, add one more
				if (index == VIEW_PAGER_WINDOW_SIZE / 2 + 1) {
					Log.d(TAG, "the second last one: " + index);

					// if not second last html page
					if (mPageIndex < mHtmlFileNumber - 2) {

						WebView mWebView = new WebView(getApplicationContext());
						initializeWebView(mWebView, mPageIndex + 2);
						addView(mPagerAdapter, mWebView);

						Log.d(TAG,
								"current size: " + mViewPager.getChildCount());
						Log.d(TAG,
								"current view index: "
										+ mViewPager.getCurrentItem());

						removeView(mPagerAdapter, mPagerAdapter.getView(0),
								mViewPager);

						Log.d(TAG,
								"current size: " + mViewPager.getChildCount());
						Log.d(TAG,
								"current view index: "
										+ mViewPager.getCurrentItem());

						// mViewPager.setCurrentItem(index - 1);
						setCurrentPage(currentView, mViewPager, mPagerAdapter);

						// Update reference to HTML page index
						for (int i = 0; i < viewPagerIndexList.length; i++) {
							viewPagerIndexList[i] = viewPagerIndexList[i] + 1;
						}
					}
				}

				// Delete the first one, add one more
				else if (index == 1) {
					Log.d(TAG, "the second first one: " + index);
					// if not second last html page
					if (mPageIndex > 1) {

						WebView mWebView = new WebView(getApplicationContext());
						initializeWebView(mWebView, mPageIndex - 2);
						addView(mPagerAdapter, mWebView, 0);

						Log.d(TAG,
								"current size: " + mViewPager.getChildCount());
						Log.d(TAG,
								"current view index: "
										+ mViewPager.getCurrentItem());

						removeView(mPagerAdapter, mPagerAdapter
								.getView(mPagerAdapter.getCount() - 1),
								mViewPager);

						Log.d(TAG,
								"current size: " + mViewPager.getChildCount());
						Log.d(TAG,
								"current view index: "
										+ mViewPager.getCurrentItem());

						// mViewPager.setCurrentItem(index - 1);
						setCurrentPage(currentView, mViewPager, mPagerAdapter);

						// Update reference to HTML page index
						for (int i = 0; i < viewPagerIndexList.length; i++) {
							viewPagerIndexList[i] = viewPagerIndexList[i] - 1;
						}
					}
				}
			}
		});
	}

	private int getScale() {

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;

		Double val = Double.valueOf(width) / Double.valueOf(720);
		val = val * 100d;
		return val.intValue();

		/*
		 * int scale = (int) getResources().getDisplayMetrics().density; return
		 * scale;
		 */
	}

	private int getDeviceWidth() {

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		return width;
	}

	final class myJavascriptHandler {
		myJavascriptHandler() {
		}

		// Requires Jelly bean or later
		@JavascriptInterface
		public void sendToAndroid(String text)
				throws startOffsetNonPositiveException {
			// this is called from JS with passed values
			Log.d(TAG, text);
			/*
			int separatorIndex = hightlightTextPlusOffset
					.lastIndexOf(hightlightAndOffsetSeparator);
			String text = hightlightTextPlusOffset.substring(0, separatorIndex);
			Log.d(TAG, "text: " + text);
			String endOffsetString = hightlightTextPlusOffset.substring(
					separatorIndex + 1, hightlightTextPlusOffset.length());
			Log.d(TAG, "endOffsetString: " + endOffsetString
					+ " String length: " + endOffsetString.length());
			int endOffset = Integer.parseInt(endOffsetString);

			if (text.length() >= HIGHLIGHT_MINIMAL_LENGTH) {

				mHighlight = text;
				int startOffset = endOffset - mHighlight.length();
				if (startOffset <= 0) {
					throw new startOffsetNonPositiveException(
							"start offset of a highlight cannot be non-positive!");
				} else {
					// prepare button action for the highlight menu
					setUpAnnotateButton(mHighlight, startOffset, endOffset);
				}
			} else {
			}
			*/
		}
	}

	private void setUpAnnotateButton(final String mHighlight,
			final int startOffset, final int endOffset) {
		if (mHighlightDialog != null) {
			Button annotateBtn = (Button) mHighlightDialog
					.findViewById(R.id.btn_annotate);
			annotateBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					fireNewAnnotationFragment(mHighlight, startOffset,
							endOffset);
				}
			});
		}
	}

	class startOffsetNonPositiveException extends Exception {
		public startOffsetNonPositiveException(String errorMessage) {
			super(errorMessage);
		}
	}

	private void fireNewAnnotationFragment(String highlightedText,
			int startOffset, int endOffset) {
		Intent toNewAnnotation = new Intent(this, AnnotationActivity.class);
		toNewAnnotation.putExtra("highlight", highlightedText);
		toNewAnnotation.putExtra("bid", mBookIndex);
		toNewAnnotation.putExtra("access_token", access_token);
		// Log.d(TAG, "access token: "+ access_token);
		toNewAnnotation.putExtra("uid", mUid);
		toNewAnnotation.putExtra("pid", mPageIndex);
		toNewAnnotation.putExtra("start", startOffset);
		toNewAnnotation.putExtra("end", endOffset);
		startActivity(toNewAnnotation);
	}

	private void initializeWebView(final WebView mWebView, int filePathIndex) {
		// reset webview
		mWebView.loadUrl("about:blank");
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);

		// Enable zoom in zoom out

		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setDisplayZoomControls(false);

		// Display the 1:1 whole page initially
		// int initialScale = getScale();
		// Log.d(TAG, "initial scale: " + initialScale);
		mWebView.setInitialScale(100);

		String absolutePath = "file:////"
				+ mHtmlFilePathList.get(filePathIndex);
		Log.d(TAG, "absolute path:" + absolutePath);

		mWebView.addJavascriptInterface(new myJavascriptHandler(),
				"valueCallback");

		/*
		 * mTextSelectionSupport = TextSelectionSupport.support(this, mWebView);
		 * mTextSelectionSupport .setSelectionListener(new
		 * TextSelectionSupport.SelectionListener() {
		 * 
		 * @Override public void startSelection() { }
		 * 
		 * @Override public void selectionChanged(String text) { //
		 * Toast.makeText(ReadBookHtmlActivity.this, //
		 * text,Toast.LENGTH_SHORT).show(); mWebView.post(new Runnable() {
		 * 
		 * @Override public void run() {
		 * mWebView.loadUrl("javascript:android.selection.getSelectionOffset();"
		 * ); mHighlightDialog.show();
		 * 
		 * } }); }
		 * 
		 * @Override public void endSelection() {
		 * mWebView.loadUrl("javascript:android.selection.clearSelection();"); }
		 * }); mWebView.setWebViewClient(new WebViewClient() { public void
		 * onScaleChanged(WebView view, float oldScale, float newScale) {
		 * Log.d(TAG, "Scale Changed !"); try {
		 * mTextSelectionSupport.onScaleChanged(oldScale, newScale); } catch
		 * (Exception e) { e.printStackTrace(); } Log.d(TAG, "old:" + oldScale);
		 * Log.d(TAG, "new:" + newScale); } });
		 */
		mWebView.loadUrl("file:///android_asset/content.html");
		// mWebView.loadUrl(absolutePath);
	}

	private void setWebViewList(int startPageIndex, int windowSize) {

		if (startPageIndex < 0 || startPageIndex > mHtmlFileNumber - windowSize) {
			Log.e(TAG, "index out of range");
		}

		else {
			mWebViewList = new ArrayList<WebView>();
			// Add pre-defined number of webviews into the list
			for (int i = 0; i < windowSize; i++) {
				WebView mWebView = new WebView(this);
				initializeWebView(mWebView, startPageIndex + i);
				mWebViewList.add(mWebView);
				viewPagerIndexList[i] = startPageIndex + i;
			}
		}
	}

	final Handler getHtmlFileHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_MESSAGE_GET_HTML_DONE:

			default:
				break;
			}
		}
	};

	// Take essential information from the intent which triggers this activity
	private void setupBasicValues() {
		Bundle extra = getIntent().getExtras();
		mBookIndex = extra.getString("bid");
		Log.d(TAG, "bid: " + mBookIndex);
		access_token = extra.getString("access_token");
		Log.d(TAG, "access_token:" + access_token);
		mBookName = extra.getString("book name");
		Log.d(TAG, "book name: " + mBookName);
		mPageIndex = 0;
		mHighlight = "";
		setTitle(mBookName);
		mUid = extra.getString("uid");
		mHtmlFilePathList = new ArrayList<String>();
		mHtmlFilePathList = getBookHtmlPath(mBookIndex, mBookName);
		mHtmlFileNumber = mHtmlFilePathList.size();

	}

	private ArrayList<String> getBookHtmlPath(final String bookIndex,
			final String bookName) {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String bookFolderPath = rootPath + "/Readpeer/Books/" + bookIndex
					+ "-" + bookName;
			File bookFolderFile = new File(bookFolderPath);

			if (bookFolderFile == null) {
				return null;
			} else {
				String[] filesList = bookFolderFile.list();
				int fileCount = filesList.length;
				ArrayList<String> htmlFilePathList = new ArrayList<String>();
				for (int i = 0; i < filesList.length; i++) {
					if (!filesList[i].endsWith("-info.txt")) {
						htmlFilePathList.add(bookFolderPath + "/"
								+ filesList[i]);
					}
				}
				return htmlFilePathList;
			}
		} else
			return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.read_book, menu);
		return true;
	}

	/* start --- View Pager Settings and Attributes --- */
	private class MyPagerAdapter extends PagerAdapter {

		private ArrayList<View> mWebViewList = new ArrayList<View>();

		@Override
		public int getItemPosition(Object o) {
			return mWebViewList.indexOf(o);
		}

		@Override
		public int getCount() {
			return mWebViewList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == (arg1);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(mWebViewList.get(position));
		}

		/*
		 * Used by ViewPager. Called when ViewPager needs a page to display; it
		 * is our job to add the page to the container, which is normally the
		 * ViewPager itself. Since all our pages are persistent, we simply
		 * retrieve it from our "views" ArrayList.
		 */
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			View v = mWebViewList.get(position);
			container.addView(v);
			return v;
		}

		// Add view at end of list, return its index
		public int addView(View v) {
			return addView(v, mWebViewList.size());
		}

		public int addView(View v, int position) {
			mWebViewList.add(position, (WebView) v);
			notifyDataSetChanged();
			return position;
		}

		// -----------------------------------------------------------------------------
		// Removes "view" from "views".
		// Retuns position of removed view.
		// The app should call this to remove pages; not used by ViewPager.
		public int removeView(ViewPager pager, View v) {
			return removeView(pager, mWebViewList.indexOf(v));
		}

		// -----------------------------------------------------------------------------
		// Removes the "view" at "position" from "views".
		// Retuns position of removed view.
		// The app should call this to remove pages; not used by ViewPager.
		public int removeView(ViewPager pager, int position) {
			/*
			 * ViewPager doesn't have a delete method; the closest is to set the
			 * adapter again. When doing so, it deletes all its views. Then we
			 * can delete the view from from the adapter and finally set the
			 * adapter to the pager again. Note that we set the adapter to null
			 * before removing the view from "views" - that's because while
			 * ViewPager deletes all its views, it will call destroyItem which
			 * will in turn cause a null pointer ref.
			 */
			pager.setAdapter(null);
			mWebViewList.remove(position);
			notifyDataSetChanged();
			pager.setAdapter(this);
			return position;
		}

		// -----------------------------------------------------------------------------
		// Returns the "view" at "position".
		// The app should call this to retrieve a view; not used by ViewPager.
		public View getView(int position) {
			return mWebViewList.get(position);
		}
	}

	private void addView(MyPagerAdapter p, View newPage) {
		int pageIndex = p.addView(newPage);
		// mViewPager.setCurrentItem(pageIndex,true);
		Log.d(TAG, "page index: " + pageIndex);
	}

	private void addView(MyPagerAdapter p, View newPage, int index) {
		int pageIndex = p.addView(newPage, index);
		Log.d(TAG, "page index: " + pageIndex);
	}

	private void removeView(MyPagerAdapter p, View unwantedPage,
			ViewPager mPager) {
		int pageIndex = p.removeView(mPager, unwantedPage);
		Log.d(TAG, "page index: " + pageIndex);

	}

	private View getCurrentPage(MyPagerAdapter p, ViewPager mPager) {
		return p.getView(mPager.getCurrentItem());
	}

	private void setCurrentPage(View pageToShow, ViewPager mPager,
			MyPagerAdapter p) {
		mPager.setCurrentItem(p.getItemPosition(pageToShow), true);
	}
	/* --- View Pager Settings and Attributes --- end */

}
