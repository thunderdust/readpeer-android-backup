package socialatwork.readpeer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.Fragments.AnnotationFragment;
import socialatwork.readpeer.Fragments.AnnotationFragment.OnAnnotationSelectedListener;
import socialatwork.readpeer.ReadView.NegativeLineNumberException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ReadBookHtmlActivity extends FragmentActivity implements OnAnnotationSelectedListener{

	private static WebView mWebView;
	private final String TAG = "Read Book Html";
	// Basic information
	private String mBookIndex;
	private String mBookName;
	private String access_token;
	private String mUid;
	/*
	 * Page number of html page, as a parameter used in annotation uploading,
	 * starting from 0
	 */
	private int mPageIndex = 0;
	/* Html page index, used for html tag sequencing, starts from 1 */
	private int mHtmlPageIndex = 1;
	private String mHighlight;

	/* Must be an odd number >= 3 to have the central page index */
	private final int VIEW_PAGER_WINDOW_SIZE = 5;
	/* Page index of the pages loaded in view pager */
	private int[] viewPagerIndexList = new int[VIEW_PAGER_WINDOW_SIZE];
	// private static int pageWindowCentralIndex = 0;
	// Html file related
	private ArrayList<String> mHtmlFilePathList;
	private ArrayList<String> mHtmlFileNameList;
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
	private final int HIGHLIGHT_MINIMAL_LENGTH = 5;

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
							mWebView.evaluateJavascript(
									"javascript:annotation.highlight_selected_text()",
									new ValueCallback<String>() {

										@Override
										public void onReceiveValue(
												String highlightJSON) {
											if (highlightJSON != null) {
												annotateHighlight(highlightJSON);
											} else {
												Log.e(TAG,
														"highlight information is not returned");
											}
										}
									});
						}
					});
					return true;
				}
			});
		}
		super.onActionModeStarted(m);
	}

	private void annotateHighlight(String highlightJSON) {
		try {
			JSONObject jobj = new JSONObject(highlightJSON);
			Log.d(TAG, highlightJSON);
			String text = jobj.get("text").toString();
			Log.d(TAG, "text:" + text);
			int startOffset = Integer.parseInt(jobj.get("start").toString());
			Log.d(TAG, "start offset: " + startOffset);
			int endOffset = Integer.parseInt(jobj.get("end").toString());
			Log.d(TAG, "end offset: " + endOffset);
			if (text.length() >= HIGHLIGHT_MINIMAL_LENGTH) {

				Log.d(TAG, "selection is long enough");
				mHighlight = text;
				if (startOffset < 0) {
					// discard this
					// annotation
					Log.e(TAG, "start is negative");
					startAnnotationMessageDialog("Failed annotation is discarded");
				} else {
					/*
					 * prepare button action for the highlight dialog
					 */
					setUpAnnotateDialog(mHighlight, startOffset, endOffset);
					// fire the highlight
					// dialog
					if (mHighlightDialog != null) {
						mHighlightDialog.show();
					}
				}
			} else {
				Log.d(TAG, "selection too short");
				startAnnotationMessageDialog("selection too short");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void onAnnotationSelected(int startIndex, int endIndex) {
		// loadPage(startIndex);
		/*
		readView = (ReadView) findViewById(R.id.readview);
		annotationSpan = new SpannableStringBuilder(readView.getText());
		annotationSpan.clearSpans();
		annotationSpan.setSpan(new BackgroundColorSpan(Color.GREEN),
				startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		readView.setText(annotationSpan);

		int verticalPosition = mObservableScrollView.getScrollY();
		Log.d(TAG, "verticalPosition:" + verticalPosition);

		int currentLineOnTop = readView.getLayout().getLineForVertical(
				verticalPosition);
		Log.d(TAG, "current line on top:" + currentLineOnTop);
		int CharCountAbove = readView.getCharCountByEndOfLine(currentLineOnTop);

		int accumulatedCharCount = CharCountAbove;
		Log.d(TAG, "char count above:" + CharCountAbove);
		Log.d(TAG, "start index:" + startIndex);
		int updatedLineNum = currentLineOnTop;

		if (startIndex >= CharCountAbove) {
			Log.d(TAG, "roll ahead");
			while (startIndex >= accumulatedCharCount) {
				updatedLineNum++;
				try {
					accumulatedCharCount += readView
							.getCharNumInLine(updatedLineNum);
				} catch (NegativeLineNumberException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			int linesToAdd = updatedLineNum - currentLineOnTop;
			final float scrollOffset = linesToAdd * readView.getLineHeight();
			Log.d(TAG, "OFFSET:" + scrollOffset);
			mObservableScrollView.post(new Runnable() {
				@Override
				public void run() {
					mObservableScrollView.scrollBy(0, (int) scrollOffset);
				}
			});
		}

		else {
			Log.d(TAG, "roll back");
			while (startIndex < accumulatedCharCount) {

				updatedLineNum--;
				try {
					accumulatedCharCount -= readView
							.getCharNumInLine(updatedLineNum);
				} catch (NegativeLineNumberException e) {
					e.printStackTrace();
				}
			}
			int linesToSub = currentLineOnTop - updatedLineNum;
			final float scrollOffset = linesToSub * readView.getLineHeight();
			Log.d(TAG, "OFFSET:" + scrollOffset);
			mObservableScrollView.post(new Runnable() {
				@Override
				public void run() {
					mObservableScrollView.scrollBy(0, (int) scrollOffset);
				}
			});
		}
		*/

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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.read_book, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		// action with ID action_refresh was selected
		case R.id.show_annotation:

			FragmentManager fm = getSupportFragmentManager();
			// if open annotation container
			if (fm.findFragmentById(R.id.annotationFragmentContainer) == null) {
				Log.d(TAG,"annotation fragment is NULL");

				// Record current reading position
				Fragment annotationFragment = fm
						.findFragmentById(R.id.annotationFragmentContainer);
				if (annotationFragment == null) {
					annotationFragment = new AnnotationFragment();
				}
				Bundle annotationBundle = new Bundle();
				annotationBundle.putString("access_token", access_token);
				annotationBundle.putString("bookIndex", mBookIndex);
				annotationBundle.putString("uid", mUid);
				annotationFragment.setArguments(annotationBundle);
				fm.beginTransaction()
						.add(android.R.id.content,
								annotationFragment).commit();
				item.setTitle("Close Annotation");
				item.setIcon(getResources().getDrawable(
						R.drawable.btn_close_annotation));
				// Close annotation container
			} else {
				Log.d(TAG,"fragment already exists, Close annotation fragment");
				fm.beginTransaction()
						.remove(fm
								.findFragmentById(R.id.annotationFragmentContainer))
						.commit();
				item.setTitle("Show Annotation");
				item.setIcon(getResources().getDrawable(
						R.drawable.btn_show_annotation));
				// remove all highlights
				Log.d(TAG, "about to load javascript");
				mWebView.post(new Runnable() {
					@TargetApi(19)
					@Override
					public void run() {
						mWebView.evaluateJavascript(
								"javascript:annotation.remove_all_highlights()",
								null);
					}
				});
				
				// loadPage(positionBefore);
				// readView.removeSelection();
				// annotationSpan = new
				// SpannableStringBuilder(readView.getText());
				// annotationSpan.clearSpans();
				// readView.setText(annotationSpan);
				/*
				 * mObservableScrollView.post(new Runnable() {
				 * 
				 * @Override public void run() {
				 * mObservableScrollView.scrollTo(0, positionCopy); } });
				 */
			}
			break;

		case R.id.font_change:

			/*
			 * mFontDialog = new Dialog(this);
			 * mFontDialog.setContentView(R.layout.font_dialog);
			 * mFontDialog.setTitle(R.string.title_font_dialog); // record
			 * current font size final int ORIGINAL_FONT_SIZE =
			 * CURRENT_FONT_SIZE; positionCopy =
			 * mObservableScrollView.getScrollY();
			 * 
			 * // Set the font size control panel Button confirmBtn = (Button)
			 * mFontDialog .findViewById(R.id.font_dialog_confirm);
			 * confirmBtn.setOnClickListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { mFontDialog.dismiss();
			 * setText(); loadPage(positionCopy); } }); Button cancelBtn =
			 * (Button) mFontDialog .findViewById(R.id.font_dialog_cancel);
			 * cancelBtn.setOnClickListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { CURRENT_FONT_SIZE =
			 * ORIGINAL_FONT_SIZE; mFontDialog.dismiss(); } }); final TextView
			 * fontSizeText = (TextView) mFontDialog
			 * .findViewById(R.id.font_size_textview);
			 * fontSizeText.setText(Integer.toString(CURRENT_FONT_SIZE));
			 * 
			 * SeekBar fontSizeSeek = (SeekBar) mFontDialog
			 * .findViewById(R.id.font_dialog_seekbar);
			 * fontSizeSeek.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
			 * fontSizeSeek.setProgress(CURRENT_FONT_SIZE - MIN_FONT_SIZE);
			 * fontSizeSeek .setOnSeekBarChangeListener(new
			 * OnSeekBarChangeListener() {
			 * 
			 * @Override public void onProgressChanged(SeekBar seekBar, int
			 * progress, boolean fromUser) { int fontSize = progress +
			 * MIN_FONT_SIZE; fontSizeText.setText(Integer.toString(fontSize));
			 * }
			 * 
			 * @Override public void onStartTrackingTouch(SeekBar seekBar) { }
			 * 
			 * @Override public void onStopTrackingTouch(SeekBar seekBar) { int
			 * progress = seekBar.getProgress(); CURRENT_FONT_SIZE = progress +
			 * MIN_FONT_SIZE; } }); // Set font/paper color change control
			 * buttons setColorModeButtons(); mFontDialog.show(); break;
			 */
		default:
			break;
		}
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
				mHtmlPageIndex = mPageIndex + 1;
				Log.d(TAG, "corresponding page index: " + mPageIndex);
				Log.d(TAG, "corresponding html page index: " + mHtmlPageIndex);

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

	/*
	 * final class myJavascriptHandler { myJavascriptHandler() { }
	 * 
	 * // Requires Jelly bean or later
	 * 
	 * @JavascriptInterface public void sendToAndroid(String
	 * hightlightTextPlusOffset) { // this is called from JS with passed values
	 * Log.d(TAG, hightlightTextPlusOffset);
	 * 
	 * int separatorIndex = hightlightTextPlusOffset
	 * .lastIndexOf(hightlightAndOffsetSeparator); String text =
	 * hightlightTextPlusOffset.substring(0, separatorIndex); Log.d(TAG,
	 * "text: " + text); String endOffsetString =
	 * hightlightTextPlusOffset.substring( separatorIndex + 1,
	 * hightlightTextPlusOffset.length()); int endOffset =
	 * Integer.parseInt(endOffsetString); Log.d(TAG, "end offset: " +
	 * endOffset);
	 * 
	 * if (text.length() >= HIGHLIGHT_MINIMAL_LENGTH) {
	 * 
	 * Log.d(TAG, "selection is long enough"); mHighlight = text; int
	 * startOffset = endOffset - mHighlight.length(); Log.d(TAG,
	 * "start offset: " + startOffset); if (startOffset <= 0) { // discard this
	 * annotation Log.e(TAG, "start is negative");
	 * startAnnotationMessageDialog("Failed annotation is discarded"); } else {
	 * // prepare button action for the highlight dialog
	 * setUpAnnotateDialog(mHighlight, startOffset, endOffset); // fire the
	 * highlight dialog if (mHighlightDialog != null) { mHighlightDialog.show();
	 * } } } else { Log.d(TAG, "selection too short");
	 * startAnnotationMessageDialog("selection too short"); } } }
	 */

	private void startAnnotationMessageDialog(String msg) {

		final Dialog highlightTooShortAlert = new Dialog(
				ReadBookHtmlActivity.this, R.style.dialog_bookInfo);
		highlightTooShortAlert
				.setContentView(R.layout.dialog_selection_too_short);
		highlightTooShortAlert.setCancelable(true);
		highlightTooShortAlert.setCanceledOnTouchOutside(true);
		highlightTooShortAlert.setTitle(msg);
		Button b = (Button) highlightTooShortAlert
				.findViewById(R.id.btn_selection_too_short);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (highlightTooShortAlert != null)
					highlightTooShortAlert.dismiss();
			}
		});
		highlightTooShortAlert.show();
	}

	private void setUpAnnotateDialog(final String mHighlight,
			final int startOffset, final int endOffset) {
		Log.d(TAG, "Setting up annotation button");
		if (mHighlightDialog != null) {
			Log.d(TAG, "highlight dialog is NOT null");
			Button annotateBtn = (Button) mHighlightDialog
					.findViewById(R.id.btn_annotate);
			annotateBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					fireNewAnnotationFragment(mHighlight, startOffset,
							endOffset);
				}
			});

		} else {
			Log.e(TAG, "highlight dialog is null");
		}
	}

	private void fireNewAnnotationFragment(String highlightedText,
			int startOffset, int endOffset) {
		Log.d(TAG, "Ready to fire new annotation fragment");
		Intent toNewAnnotation = new Intent(this, AnnotationActivity.class);
		toNewAnnotation.putExtra("highlight", highlightedText);
		toNewAnnotation.putExtra("bid", mBookIndex);
		toNewAnnotation.putExtra("access_token", access_token);
		// Log.d(TAG, "access token: "+ access_token);
		toNewAnnotation.putExtra("uid", mUid);
		toNewAnnotation.putExtra("pid", mPageIndex);
		toNewAnnotation.putExtra("start", startOffset);
		toNewAnnotation.putExtra("end", endOffset);
		if (mHighlightDialog != null) {
			mHighlightDialog.dismiss();
		}
		startActivity(toNewAnnotation);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initializeWebView(final WebView mWebView, int filePathIndex) {
		// reset webview
		mWebView.loadUrl("about:blank");
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		// Turn off hardware acceleration
		mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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

		String assetPath = "file:///android_asset/"
				+ mHtmlFileNameList.get(filePathIndex);
		Log.d(TAG, "asset path:" + assetPath);

		// mWebView.addJavascriptInterface(new
		// myJavascriptHandler(),"valueCallback");
		mWebView.setWebChromeClient(new WebChromeClient());

		// Load annotating javascripts after html load is done
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				Log.d(TAG, "page is loaded");
				// loadAnnotatingJSFiles(view);

				/*
				 * String jsContent = ""; /* try { loadAnnotatingJSFiles(view);
				 * } catch (MalformedURLException e) { e.printStackTrace(); }
				 * 
				 * try{ InputStream is =
				 * getApplicationContext().getAssets().open("annotation.js");
				 * InputStreamReader isr = new InputStreamReader(is);
				 * BufferedReader br = new BufferedReader(isr); String line;
				 * while ((line = br.readLine())!=null){
				 * Log.d(TAG,"line is not NULL"); jsContent += line; }
				 * is.close(); } catch(Exception e){}
				 * view.loadUrl("javascript:("+jsContent + ")()");
				 */
			}
		});
		//mWebView.loadUrl(assetPath);
		// loadAnnotatingJSFiles(mWebView);
		mWebView.loadUrl(absolutePath);
	}

	// Load a webview with essential javascript files to enable annotating
	private void loadAnnotatingJSFiles(WebView w) {

		Log.d(TAG, "Loading annotation javascripts");
		String injectJSURL = "javascript:(function(){"
				+ "var script = document.createElement(\"script\");"
				+ "script.type='text/javascript';"
				+ "script.src=\"annotation.js\";"
				+ "document.getElementsByTagName('head').item(0).appendChild(script);"
				+ "})()";
		w.loadUrl(injectJSURL);
	}

	private void setWebViewList(int startPageIndex, int windowSize) {

		Log.d(TAG, "html file number: " + mHtmlFileNumber);
		if (startPageIndex < 0 || mHtmlFileNumber >= windowSize
				&& startPageIndex > mHtmlFileNumber - windowSize) {
			Log.e(TAG, "index out of range");
		}

		else {
			int webViewWindowSize = windowSize;
			if (mHtmlFileNumber < windowSize)
				webViewWindowSize = mHtmlFileNumber;
			mWebViewList = new ArrayList<WebView>();
			// Add pre-defined number of webviews into the list
			for (int i = 0; i < webViewWindowSize; i++) {
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
		mHtmlFileNameList = new ArrayList<String>();
		mHtmlFilePathList = getBookHtmlPath(mBookIndex, mBookName);
		mHtmlFileNameList = getBookHtmlName(mBookIndex, mBookName);
		mHtmlFileNumber = mHtmlFilePathList.size();

	}

	private ArrayList<String> getBookHtmlName(final String bookIndex,
			final String bookName) {

		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			String bookFolderPath = rootPath + "/Readpeer/Books/" + bookIndex
					+ "-" + bookName;
			File bookFolderFile = new File(bookFolderPath);

			if (bookFolderFile != null) {
				String[] filesList = bookFolderFile.list();
				ArrayList<String> htmlFileNameList = new ArrayList<String>();
				for (int i = 0; i < filesList.length; i++) {
					if (!filesList[i].endsWith("-info.txt")) {
						htmlFileNameList.add(filesList[i]);
					}
				}
				return htmlFileNameList;
			}
		} else
			return null;
		return null;
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

			if (bookFolderFile != null) {
				String[] filesList = bookFolderFile.list();
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
		return null;
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
		Log.d(TAG, "page index: " + mPager.getCurrentItem());
		return p.getView(mPager.getCurrentItem());
	}

	private void setCurrentPage(View pageToShow, ViewPager mPager,
			MyPagerAdapter p) {
		mPager.setCurrentItem(p.getItemPosition(pageToShow), true);
	}
	/* --- View Pager Settings and Attributes --- end */

}
