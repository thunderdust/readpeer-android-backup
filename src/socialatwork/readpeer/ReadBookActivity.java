package socialatwork.readpeer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.nio.CharBuffer;
import java.util.HashSet;
import java.util.Set;

import socialatwork.readpeer.ReadView.NegativeLineNumberException;
import socialatwork.readpeer.Fragments.AnnotationFragment;
import socialatwork.readpeer.Fragments.AnnotationFragment.OnAnnotationSelectedListener;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ReadBookActivity extends FragmentActivity implements
		OnAnnotationSelectedListener {

	private static ReadView readView;
	private ObservableScrollView mObservableScrollView;
	private ImageView mBusyIndicator;
	private Dialog mFontDialog;

	/* Book loading variables */
	BufferedReader reader;
	CharBuffer buffer;
	private int whole_book_length;

	/* Paging related variables */
	private int position;

	final int FLIP_DISTANCE = 50;

	// private MotionEvent mEvent;

	private final int DEFAULT_FONT_SIZE = 22;
	private final int MIN_FONT_SIZE = 12;
	private final int MAX_FONT_SIZE = 40;
	private int CURRENT_FONT_SIZE = DEFAULT_FONT_SIZE;

	private final String COLOR_READ_BLUE = "#5F9EA0";
	private final String COLOR_READ_GREEN = "#006400";

	private static int highlightStartPageNum = 0;

	private String access_token;
	private static int[] mPageWordCountArray;
	private static Set<String> pageWordCountSet;
	private int pageCount;
	private String mBookTitle;
	private String mBookIndex;
	private String mUid;
	private final String TAG = "read book";

	private int mTouchX;
	private int mTouchY;
	private final static int DEFAULT_SELECTION_LEN = 5;

	/* Screen Adapting parameters */
	private int parentLayoutHeight = 0;
	private int parentLayoutWidth = 0;
	private boolean isFirstTimeSetLayout = true;
	private SpannableStringBuilder annotationSpan;

	private int positionCopy = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_read_book);

		readView = (ReadView) findViewById(R.id.readview);

		/* Enable text selection is crucial for annotation function */
		readView.setTextIsSelectable(true);

		mObservableScrollView = (ObservableScrollView) findViewById(R.id.scroller);
		// Disable hardware acceleration to improve performance
		mObservableScrollView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		readView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				readView.removeSelection();
				showSelectionCursors(mTouchX, mTouchY);
				return true;
			}
		});

		readView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				readView.hideCursor();
			}
		});
		readView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mTouchX = (int) event.getX();
				mTouchY = (int) event.getY();
				return false;
			}
		});

		// registerForContextMenu(readView);
		Bundle extra = getIntent().getExtras();
		mBookIndex = extra.getString("bid");
		access_token = extra.getString("access_token");
		mBookTitle = extra.getString("book name");
		setTitle(mBookTitle);
		mUid = extra.getString("uid");

		// Log.d(TAG, "uid:" + mUid);
		// Log.d(TAG, access_token);

		readView.setAccessToken(access_token);
		readView.setUid(mUid);
		readView.setBookIndex(mBookIndex);
		readView.setPageNumber(highlightStartPageNum);

		try {
			loadBook(access_token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Load from shared preference stored when download this book
		getPagingInformationFromSharedPreference();
		loadPage(0);
	}

	private void getPagingInformationFromSharedPreference() {

		pageWordCountSet = new HashSet<String>();
		SharedPreferences sp = this.getSharedPreferences("bookInfo",
				Context.MODE_PRIVATE);

		if (!sp.contains(mBookIndex)) {
			Log.e(TAG, "preference DONT have this index key");
		} else {
			Log.d(TAG, "preference have this key");
		}

		pageWordCountSet = sp.getStringSet(mBookIndex, null);

		if (pageWordCountSet != null) {
			mPageWordCountArray = new int[pageWordCountSet.size()];
			Object[] pageWordCountObjects = new Object[pageWordCountSet.size()];
			pageWordCountSet.toArray(pageWordCountObjects);

			for (int i = 0; i < pageWordCountSet.size(); i++) {
				mPageWordCountArray[i] = Integer
						.parseInt((String) pageWordCountObjects[i]);
			}
		} else {
			Toast.makeText(
					getBaseContext(),
					"Error: book information is not found,cannot process book content.",
					Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	private void showSelectionCursors(int x, int y) {
		int start = readView.getPreciseOffset(x, y);

		if (start > -1) {
			int end = start + DEFAULT_SELECTION_LEN;
			if (end >= readView.getText().length()) {
				end = readView.getText().length() - 1;
			}
			readView.showSelectionControls(start, end);
		}
	}

	private void loadBook(String access_token) throws Exception {

		String storagePath = Environment.getExternalStorageDirectory()
				.getPath();
		String filename = storagePath + "/Readpeer/Books/" + mBookTitle + "-"
				+ mBookIndex + "-" + ".txt";
		File bookFile = new File(filename);

		if (!bookFile.exists()) {
			Log.e(TAG, "File Not Exists");
		} else {

			FileInputStream fs = new FileInputStream(bookFile);
			int size = fs.available();
			byte[] byteBuffer = new byte[size];
			fs.read(byteBuffer);
			fs.close();
			String content = new String(byteBuffer);

			Log.d(TAG, "book content:" + content);
			readView.setText(content);
			readView.setTextSize(DEFAULT_FONT_SIZE);
		}
	}

	private void loadPage(int position) {

		if (isFirstTimeSetLayout) {
			Log.i(TAG, "Intialize the layout for readView");
			final FrameLayout parentLayout = (FrameLayout) findViewById(R.id.readViewParent);
			parentLayoutHeight = parentLayout.getLayoutParams().height;
			parentLayoutWidth = parentLayout.getLayoutParams().width;

			mObservableScrollView.getLayoutParams().height = parentLayoutHeight;
			mObservableScrollView.getLayoutParams().width = parentLayoutWidth;

			readView.getLayoutParams().height = parentLayoutHeight;
			readView.getLayoutParams().width = parentLayoutWidth;

			mObservableScrollView.setVerticalScrollBarEnabled(true);
			mObservableScrollView.setScrollbarFadingEnabled(true);
			isFirstTimeSetLayout = false;
		}
		mObservableScrollView.scrollTo(0, position);

	}

	private void setText() {
		if (mBusyIndicator == null) {
			mBusyIndicator = (ImageView) findViewById(R.id.imageview_busy);
		}
		mBusyIndicator.setVisibility(View.VISIBLE);

		final Handler messageHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == CURRENT_FONT_SIZE) {
					Log.d(TAG, "change done");
					if (mBusyIndicator != null) {
						mBusyIndicator.setVisibility(View.INVISIBLE);
					}
				}
			}
		};

		Runnable setTextRunnbale = new Runnable() {
			@Override
			public void run() {

				readView.setTextSize(CURRENT_FONT_SIZE);
				messageHandler.sendEmptyMessage(CURRENT_FONT_SIZE);
			}
		};
		messageHandler.post(setTextRunnbale);
	}

	// Device layout is very different with web site layout. Must convert
	// the start and end index of annotation to fit web site page standard.
	// in this method, startPageNum will be computed
	public static int convertStartIndexToWebsiteStandard(int currentIndex) {
		for (int i = 0; i < mPageWordCountArray.length; i++) {
			if (currentIndex < mPageWordCountArray[i]) {
				highlightStartPageNum = i + 1;
				readView.setPageNumber(highlightStartPageNum);
				if (i < 1)
					return currentIndex;
				else
					return currentIndex - mPageWordCountArray[i - 1];
			}
		}
		// Error
		return -1;
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

				// Record current reading position
				positionCopy = mObservableScrollView.getScrollY();
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
						.add(R.id.annotationFragmentContainer,
								annotationFragment).commit();
				item.setTitle("Close Annotation");
				item.setIcon(getResources().getDrawable(
						R.drawable.btn_close_annotation));
				// Close annotation container
			} else {
				fm.beginTransaction()
						.remove(fm
								.findFragmentById(R.id.annotationFragmentContainer))
						.commit();
				item.setTitle("Show Annotation");
				item.setIcon(getResources().getDrawable(
						R.drawable.btn_show_annotation));
				// loadPage(positionBefore);
				readView.removeSelection();
				annotationSpan = new SpannableStringBuilder(readView.getText());
				annotationSpan.clearSpans();
				readView.setText(annotationSpan);
				mObservableScrollView.post(new Runnable() {
					@Override
					public void run() {
						mObservableScrollView.scrollTo(0, positionCopy);
					}
				});
			}
			break;

		case R.id.font_change:

			mFontDialog = new Dialog(this);
			mFontDialog.setContentView(R.layout.font_dialog);
			mFontDialog.setTitle(R.string.title_font_dialog);
			// record current font size
			final int ORIGINAL_FONT_SIZE = CURRENT_FONT_SIZE;
			positionCopy = mObservableScrollView.getScrollY();

			// Set the font size control panel
			Button confirmBtn = (Button) mFontDialog
					.findViewById(R.id.font_dialog_confirm);
			confirmBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mFontDialog.dismiss();
					setText();
					loadPage(positionCopy);
				}
			});
			Button cancelBtn = (Button) mFontDialog
					.findViewById(R.id.font_dialog_cancel);
			cancelBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CURRENT_FONT_SIZE = ORIGINAL_FONT_SIZE;
					mFontDialog.dismiss();
				}
			});
			final TextView fontSizeText = (TextView) mFontDialog
					.findViewById(R.id.font_size_textview);
			fontSizeText.setText(Integer.toString(CURRENT_FONT_SIZE));

			SeekBar fontSizeSeek = (SeekBar) mFontDialog
					.findViewById(R.id.font_dialog_seekbar);
			fontSizeSeek.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
			fontSizeSeek.setProgress(CURRENT_FONT_SIZE - MIN_FONT_SIZE);
			fontSizeSeek
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							int fontSize = progress + MIN_FONT_SIZE;
							fontSizeText.setText(Integer.toString(fontSize));
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							int progress = seekBar.getProgress();
							CURRENT_FONT_SIZE = progress + MIN_FONT_SIZE;
						}
					});
			// Set font/paper color change control buttons
			setColorModeButtons();
			mFontDialog.show();
			break;
		default:
			break;
		}
		return true;
	}

	private void setColorModeButtons() {

		ImageButton fontPaperColorSettingButton1 = (ImageButton) mFontDialog
				.findViewById(R.id.btn_read_mode_1);
		ImageButton fontPaperColorSettingButton2 = (ImageButton) mFontDialog
				.findViewById(R.id.btn_read_mode_2);

		ImageButton fontPaperColorSettingButton3 = (ImageButton) mFontDialog
				.findViewById(R.id.btn_read_mode_3);

		ImageButton fontPaperColorSettingButton4 = (ImageButton) mFontDialog
				.findViewById(R.id.btn_read_mode_4);

		fontPaperColorSettingButton1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				readView.setTextColor(android.graphics.Color.WHITE);
				readView.setBackgroundColor(android.graphics.Color.BLACK);
				mFontDialog.dismiss();
			}
		});

		fontPaperColorSettingButton2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				readView.setTextColor(android.graphics.Color.BLACK);
				readView.setBackgroundColor(android.graphics.Color.WHITE);
				mFontDialog.dismiss();
			}
		});

		fontPaperColorSettingButton3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				readView.setTextColor(android.graphics.Color.WHITE);
				readView.setBackgroundColor(android.graphics.Color
						.parseColor(COLOR_READ_BLUE));
				mFontDialog.dismiss();
			}
		});

		fontPaperColorSettingButton4.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				readView.setTextColor(android.graphics.Color.WHITE);
				readView.setBackgroundColor(android.graphics.Color
						.parseColor(COLOR_READ_GREEN));
				mFontDialog.dismiss();
			}
		});

	}

	// Highlight text when annotation is selected
	@Override
	public void onAnnotationSelected(int startIndex, int endIndex) {
		// loadPage(startIndex);
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

	}
}