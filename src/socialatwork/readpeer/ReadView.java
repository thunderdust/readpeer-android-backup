package socialatwork.readpeer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

//Costumed view class for reading page
public class ReadView extends TextView {

	private int SELECTION_COLOR = android.graphics.Color.GREEN;
	private String TAG = "readview";
	private int mDefaultSelectionColor = android.graphics.Color.GREEN;
	private SelectionInfo mSelection;
	private final int[] mTempCoords = new int[2];
	private SelectionCursorController mSelectionController;

	private Dialog annotationDialog;
	private Button makeAnnotationConfirmButton;

	private String uid;
	private String bookIndex;
	private String access_token;
	private int annotationStartIndex;
	private int annotationEndIndex;
	private int pageNumber;

	public ReadView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ReadView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ReadView(Context context) {
		super(context);
		init();
	}

	public void setBookIndex(String bookIndex) {
		this.bookIndex = bookIndex;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setAccessToken(String token) {
		this.access_token = token;
	}

	public void setPageNumber(int num) {
		this.pageNumber = num;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		setBackgroundColor(Color.WHITE);
		// resize();
	}

	/*
	 * public int resize() { CharSequence oldContent = getText(); CharSequence
	 * newContent = oldContent.subSequence(0, getCharNum());
	 * setText(newContent); return oldContent.length() - newContent.length(); }
	 */

	/* Get character count on current view */
	public int getCharNum() {
		return getLayout().getLineEnd(getLineNum());
	}

	public int getCharNumInLine(int lineNum) throws NegativeLineNumberException {

		int lineNumber = lineNum;
		if (lineNumber < 0) {
			lineNumber = 0;
			Log.e(TAG, "line number is less than 0!");
			// throw new
			// NegativeLineNumberException("Line Number is negative walao");
		}

		int charOffsetOnLineEnd = getLayout().getLineEnd(lineNumber);
		Log.d(TAG, "end offset:" + charOffsetOnLineEnd);
		int charOffsetOnLineStart = getLayout().getLineStart(lineNumber);
		Log.d(TAG, "start offset:" + charOffsetOnLineStart);
		int num = charOffsetOnLineEnd - charOffsetOnLineStart + 1;
		if (num > 0)
			return num;
		else
			return 0;
	}

	public class NegativeLineNumberException extends Exception {

		private static final long serialVersionUID = 1L;

		private NegativeLineNumberException(String msg) {
			super(msg);
		}
	}

	// Include the specific line
	public int getCharCountByEndOfLine(int lineNum) {
		return getLayout().getLineEnd(lineNum);
	}

	public int getLineNum() {
		Layout layout = getLayout();
		int topOfLastLine = getHeight() - getPaddingTop() - getPaddingBottom()
				- getLineHeight();
		return layout.getLineForVertical(topOfLastLine);
	}

	private void init() {
		mSelection = new SelectionInfo();
		mSelectionController = new SelectionCursorController();
		setDefaultSelectionColor(SELECTION_COLOR);
		final ViewTreeObserver observer = getViewTreeObserver();
		if (observer != null) {
			observer.addOnTouchModeChangeListener(mSelectionController);
		}
		annotationDialog = new Dialog(getContext());
		annotationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		annotationDialog.setTitle("Make annotation");
		annotationDialog.setCanceledOnTouchOutside(true);
		annotationDialog.setContentView(R.layout.dialog_make_annotation);

		Window mWindow = annotationDialog.getWindow();
		WindowManager.LayoutParams lp = mWindow.getAttributes();
		lp.dimAmount = 0.2f;
		mWindow.setAttributes(lp);

		makeAnnotationConfirmButton = (Button) annotationDialog
				.findViewById(R.id.btn_make_annotation);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		// TextView will take care of the color span of the text when it's being
		// scroll by
		// its parent ScrollView. But the cursors position is not handled.
		// Calling snapToSelection
		// will move the cursors along with the selection.
		if (getParent() instanceof ObservableScrollView) {
			((ObservableScrollView) getParent())
					.addOnScrollChangedListener(new OnScrollChangedListener() {
						@Override
						public void onScrollChanged(
								ObservableScrollView scrollView, int x, int y,
								int oldx, int oldy) {
							mSelectionController.snapToSelection();
						}
					});
		}
	}

	public void setDefaultSelectionColor(int color) {
		mDefaultSelectionColor = color;
	}

	/**
	 * set the selection beginning at {@code start} with the specified
	 * {@code length} for the given {@code duration}
	 * 
	 * @param color
	 *            the color of the selection
	 * @param start
	 *            the start offset
	 * @param length
	 *            the length of the selection
	 * @param duration
	 *            the duration the selection, in second, to be last, -1 to
	 *            indicate forever
	 */
	public void setSelection(int color, int start, int length, int duration) {

		// make sure we are not selecting beyond the text
		int end = start + length >= getText().length() ? getText().length() - 1
				: start + length;

		if (start + length > getText().length() || end <= start) {
			return;
		}

		mSelection = new SelectionInfo(getText(),
				new BackgroundColorSpan(color), start, end);
		mSelection.select();
		removeSelection(duration);
	}

	/**
	 * set the selection beginning at {@code start} with the specified
	 * {@code length} for the given {@code duration} with the default color
	 * 
	 * @param start
	 *            the start offset
	 * @param length
	 *            the length of the selection
	 * @param duration
	 *            the duration of the selection to be last, -1 to indicate
	 *            forever
	 */
	public void setSelection(int start, int length, int duration) {
		setSelection(mDefaultSelectionColor, start, length, duration);
	}

	/**
	 * set the selection beginning at {@code start} with the spcified
	 * {@code length}
	 * 
	 * @param start
	 *            the starting offset
	 * @param length
	 *            the length of the selection
	 */
	public void setSelection(int start, int length) {
		setSelection(start, length, -1);
	}

	/**
	 * removes the current selection immediately
	 */
	public void removeSelection() {
		mSelection.remove();
	}

	public void removeSelection(int delay) {
		removeSelection(mSelection, delay);
	}

	/**
	 * remove the given span style from the main text after the specified time
	 * 
	 * @param selection
	 *            the span style to be removed
	 * @param delay
	 *            the milliseconds to wait before remove the style
	 */
	public void removeSelection(final SelectionInfo selection, int delay) {
		if (delay >= 0) {
			Runnable runnable = new Runnable() {
				public void run() {
					selection.remove();
				}
			};
			postDelayed(runnable, delay);
		}
	}

	/**
	 * show the selection cursors and select the text between the specified
	 * offset
	 * 
	 * @param start
	 *            the start offset
	 * @param end
	 *            the end offset
	 */
	public void showSelectionControls(int start, int end) {
		assert (start >= 0);
		assert (end >= 0);
		assert (start < getText().length());
		assert (end < getText().length());

		mSelectionController.show(start, end);
	}

	/**
	 * @return the current selection information
	 */
	public SelectionInfo getSelection() {
		return mSelection;
	}

	/**
	 * @return the y position
	 */
	private int getScrollYInternal() {
		int y = this.getScrollY();

		// a TextView inside a ScrollView is not scrolled, so getScrollY()
		// returns 0.
		// We must use getScrollY() from the ScrollView instead
		if (this.getParent() instanceof ScrollView) {
			y += ((ScrollView) this.getParent()).getScrollY();

			// do this to compensate for the height of the status bar
			final int[] coords = mTempCoords;
			((ScrollView) this.getParent()).getLocationInWindow(coords);
			y -= coords[1];
		}

		return y;
	}

	/**
	 * @return the x position
	 */
	@SuppressWarnings("unused")
	private int getScrollXInternal() {
		int x = this.getScrollX();

		// a TextView inside a ScrollView is not scrolled, so getScrollX()
		// returns 0.
		// We must use getScrollX() from the ScrollView instead
		if (this.getParent() instanceof ScrollView) {
			x += ((ScrollView) this.getParent()).getScrollX();
		}
		return x;
	}

	/**
	 * Gets the character offset of (x, y). If (x, y) lies on the right half of
	 * the character, it returns the offset of the next character. If (x, y)
	 * lies on the left half of the character, it returns the offset of this
	 * character.
	 * 
	 * @param x
	 *            x coordinate relative to this TextView
	 * @param y
	 *            y coordinate relative to this TextView
	 * @return the offset at (x,y), -1 if error occurs
	 */
	public int getOffset(int x, int y) {
		Layout layout = getLayout();
		int offset = -1;

		if (layout != null) {
			int topVisibleLine = layout.getLineForVertical(y);
			offset = layout.getOffsetForHorizontal(topVisibleLine, x);
		}

		return offset;
	}

	/**
	 * Gets the character offset where (x, y) is pointing to.
	 * 
	 * @param x
	 *            x coordinate relative to this TextView
	 * @param y
	 *            y coordinate relative to this TextView
	 * @return the offset at (x, y), -1 if error occurs
	 * 
	 * @see {@link #getOffset(int, int)}
	 */
	public int getPreciseOffset(int x, int y) {
		Layout layout = getLayout();

		if (layout != null) {
			int topVisibleLine = layout.getLineForVertical(y);
			int offset = layout.getOffsetForHorizontal(topVisibleLine, x);

			int offset_x = (int) layout.getPrimaryHorizontal(offset);
			if (offset_x > x) {
				return layout.getOffsetToLeftOf(offset);
			}
		}
		return getOffset(x, y);
	}

	// //////////////////////////////////////////////
	// copied & modified from Android source code //
	// //////////////////////////////////////////////

	/**
	 * Get the offset character closest to the specified absolute position.
	 * 
	 * @param x
	 *            The horizontal absolute position of a point on screen
	 * @param y
	 *            The vertical absolute position of a point on screen
	 * @return the character offset for the character whose position is closest
	 *         to the specified position. Returns -1 if there is no layout.
	 */
	@SuppressWarnings("unused")
	public int getOffsetFromRaw(int x, int y) {
		if (getLayout() == null)
			return -1;

		y -= getTotalPaddingTop();
		// Clamp the position to inside of the view.
		y = Math.max(0, y);
		y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
		y += getScrollY();

		// if the textview is inside a scrollview, the above getScrollY() will
		// return 0
		// even if it's scrolled. Must do getScrollY() from the ScrollView
		// instead.
		if (this.getParent() instanceof ScrollView) {
			ScrollView scrollView = (ScrollView) this.getParent();
			y += scrollView.getScrollY();

			// do this to compensate for the height of the status bar
			final int[] coords = mTempCoords;
			scrollView.getLocationInWindow(coords);
			y -= coords[1];
		}

		final int line = getLayout().getLineForVertical(y);
		final int offset = getOffsetForHorizontal(line, x);
		return offset;
	}

	// //////////////////////////////////////////////
	// copied & modified from Android source code //
	// //////////////////////////////////////////////

	/**
	 * Get the offset character closest to the specified absolute position. If
	 * the resulting offset is too close to the previous offset, return the
	 * previous offset instead.
	 * 
	 * @param x
	 *            raw x
	 * @param y
	 *            raw y
	 * @param previousOffset
	 *            previous offset
	 * @return offset of the specified (x,y)
	 */
	private int getHysteresisOffset(int x, int y, int previousOffset) {
		final Layout layout = getLayout();
		if (layout == null)
			return -1;

		y -= getTotalPaddingTop();
		// Clamp the position to inside of the view.
		y = Math.max(0, y);
		y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
		y += getScrollYInternal();

		int line = getLayout().getLineForVertical(y);

		// this block is required because of how Android Layout for
		// TextView works - if 'offset' equals to the last character of a line,
		// then
		//
		// * getLineForOffset(offset) will result the NEXT line
		// * getPrimaryHorizontal(offset) will return 0 because the next
		// insertion point is on the next line
		// * getOffsetForHorizontal(line, x) will not return the last offset of
		// a line no matter where x is
		// These are highly undesired and is worked around with the HACK BLOCK
		//
		// @see Moon+ Reader/Color Note - see how it can't select the last
		// character of a line unless you move
		// the cursor to the beginning of the next line.
		//
		// //////////////////////////////////////////////////////////////////////////////
		if (isEndOfLineOffset(previousOffset)) {
			// we have to minus one from the offset so that the code below to
			// find
			// the previous line can work correctly.
			int left = (int) layout.getPrimaryHorizontal(previousOffset - 1);
			int right = (int) layout.getLineRight(line);
			int threshold = (right - left) / 2; // half the width of the last
												// character
			if (x > right - threshold) {
				previousOffset -= 1;
			}
		}
		// /////////////////////////////////////////////////////////////////////////////////

		final int previousLine = layout.getLineForOffset(previousOffset);
		final int previousLineTop = layout.getLineTop(previousLine);
		final int previousLineBottom = layout.getLineBottom(previousLine);
		final int hysteresisThreshold = (previousLineBottom - previousLineTop) / 2;

		// If new line is just before or after previous line and y position is
		// less than
		// hysteresisThreshold away from previous line, keep cursor on previous
		// line.
		if (((line == previousLine + 1) && ((y - previousLineBottom) < hysteresisThreshold))
				|| ((line == previousLine - 1) && ((previousLineTop - y) < hysteresisThreshold))) {
			line = previousLine;
		}

		int offset = getOffsetForHorizontal(line, x);

		// This allow the user to select the last character of a line without
		// moving the
		// cursor to the next line. (As Layout.getOffsetForHorizontal does not
		// return the
		// offset of the last character of the specified line)
		//
		// But this function will probably get called again immediately, must
		// decrement the offset
		// by 1 to compensate for the change made below. (see previous block)
		// /////////////////////////////////////////////////////////////////////////////
		if (offset < getText().length() - 1) {
			if (isEndOfLineOffset(offset + 1)) {
				int left = (int) layout.getPrimaryHorizontal(offset);
				int right = (int) layout.getLineRight(line);
				int threshold = (right - left) / 2; // half the width of the
													// last character
				if (x > right - threshold) {
					offset += 1;
				}
			}
		}
		// ////////////////////////////////////////////////////////////////////////////////

		return offset;
	}

	/**
	 * Checks whether the specified offset is at the end of a line.
	 * <p/>
	 * PRECONDITION: assumes layout exists and is valid
	 * 
	 * @param offset
	 *            the offset to check
	 * @return true if the offset is at the end of a line, false otherwise.
	 */
	private boolean isEndOfLineOffset(int offset) {
		if (offset > 0) {
			return getLayout().getLineForOffset(offset) == getLayout()
					.getLineForOffset(offset - 1) + 1;
		}
		return false;
	}

	// //////////////////////////////////////////////
	// copied & modified from Android source code //
	// //////////////////////////////////////////////
	private int getOffsetForHorizontal(int line, int x) {
		x -= getTotalPaddingLeft();
		// Clamp the position to inside of the view.
		x = Math.max(0, x);
		x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
		x += getScrollX();

		// if the textview is inside a scrollview, the above getScrollX() will
		// return 0
		// even if it's scrolled. Must do getScrollX() from the ScrollView
		// instead.
		if (this.getParent() instanceof ScrollView) {
			ScrollView scrollView = (ScrollView) this.getParent();
			x += scrollView.getScrollX();
		}

		return getLayout().getOffsetForHorizontal(line, x);
	}

	/**
	 * get the (x,y) screen coordinates from the specified offset.
	 * 
	 * @param offset
	 *            the offset
	 * @param scroll_y
	 *            the scroll distant to take away
	 * @param coords
	 *            the returned x, y coordinate array, must have a length of 2
	 */
	private void getXY(int offset, int scroll_y, int[] coords) {
		assert (coords.length >= 2);

		coords[0] = coords[1] = -1;
		Layout layout = getLayout();

		if (layout != null) {
			int line = layout.getLineForOffset(offset);
			int base = layout.getLineBottom(line);

			coords[0] = (int) layout.getPrimaryHorizontal(offset); // x
			coords[1] = base - scroll_y; // y
		}
	}

	/**
	 * Get the (x,y) screen coordinate from the specified offset. If the
	 * specified offset is beyond the end of the line, move the offset to the
	 * beginning of the next line.
	 * 
	 * @param offset
	 *            the offset
	 * @param scroll_y
	 *            the scroll distance to take away.
	 * @param coords
	 *            the returned x, y coordinate array, muust have a length of 2
	 */
	private void getAdjusteStartXY(int offset, int scroll_y, int[] coords) {
		if (offset < getText().length()) {
			final Layout layout = getLayout();
			if (layout != null) {
				if (isEndOfLineOffset(offset + 1)) {
					float a = layout.getPrimaryHorizontal(offset);
					float b = layout.getLineRight(layout
							.getLineForOffset(offset));
					if (a == b) {
						// this means the we encounter a new line character, i
						// think.
						offset += 1;
					}
				}
			}
		}
		getXY(offset, scroll_y, coords);
	}

	/**
	 * Get the (x,y) screen coordinate from the specified offset. If the offset
	 * is the at the end of a wrapped line, return the (x,y) at the end of that
	 * line instead of the (x, y) at the beginning of the next line (which is
	 * the default behaviour for Android)
	 * 
	 * @param offset
	 *            the offset
	 * @param scroll_y
	 *            the scroll distance to take away
	 * @param coords
	 *            the returned x, y coordinate array, must have a length of 2
	 */
	private void getAdjustedEndXY(int offset, int scroll_y, int[] coords) {
		if (offset > 0) {
			final Layout layout = getLayout();
			if (layout != null) {
				// if (this_line > prev_line) {
				if (isEndOfLineOffset(offset)) {
					// if we are at the end of a line, calculate the X using
					// getLineRight instead of
					// getPrimaryHorizontal.
					// (Because getPrimaryHorizontal returns 0 for offset
					// sitting at the end of a line.
					// getPrimaryHorizontal returns the next insertion point,
					// which will be the next line)
					int prev_line = layout.getLineForOffset(offset - 1);
					float right = layout.getLineRight(prev_line);
					int y = layout.getLineBottom(prev_line);
					coords[0] = (int) right;
					coords[1] = y - scroll_y;
					return;
				}
			}
		}
		getXY(offset, scroll_y, coords);
	}

	public void hideCursor() {
		mSelectionController.hide();
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	// ///////////////////////////INNER
	// CLASSES//////////////////////////////////
	// /////// copied, heavily modified and simplified from TextView
	// ////////////
	// //////////////////////////////////////////////////////////////////////////

	/**
	 * Manages the two cursors that control the selection. Internal used only.
	 * For outsider, please use SelectionModifier
	 */
	private class SelectionCursorController implements
			ViewTreeObserver.OnTouchModeChangeListener {

		/**
		 * The two cursor handle that controls the selection. Note that the two
		 * cursors are allowed to swap positions and thus the name of the handle
		 * has no bearing on the relative position of the handle to each other.
		 * (e.g. mStartHandle can be positioned leagally at an offset greater
		 * than the offset mEndHandle is resting on)
		 */
		private CursorHandle mStartHandle;
		private CursorHandle mEndHandle;

		/**
		 * whether the selection controller is displaying on the screen
		 */
		private boolean mIsShowing;

		public SelectionCursorController() {
			mStartHandle = new CursorHandle(this);
			mEndHandle = new CursorHandle(this);
		}

		/**
		 * snap the cursors to the current selection
		 */
		public void snapToSelection() {

			if (mIsShowing) {
				int a = ReadView.this.getSelection().getStart();
				int b = ReadView.this.getSelection().getEnd();

				int start = Math.min(a, b);
				int end = Math.max(a, b);

				// find the corresponding handle for the start/end calculated
				// above
				CursorHandle startHandle = start == a ? mStartHandle
						: mEndHandle;
				CursorHandle endHandle = end == b ? mEndHandle : mStartHandle;

				final int[] coords = mTempCoords;
				int scroll_y = ReadView.this.getScrollYInternal();

				ReadView.this.getAdjusteStartXY(start, scroll_y, coords);
				startHandle.pointTo(coords[0], coords[1]);

				ReadView.this.getAdjustedEndXY(end, scroll_y, coords);
				endHandle.pointTo(coords[0], coords[1]);
			}
		}

		/**
		 * show the selection cursor at the specified offsets and select the
		 * text between the specified offsets.
		 * 
		 * @param start
		 *            the offset of the first cursor
		 * @param end
		 *            the offset of the second cursor
		 */
		public void show(int start, int end) {

			int a = Math.min(start, end);
			int b = Math.max(start, end);

			final int[] coords = mTempCoords;
			int scroll_y = ReadView.this.getScrollY();

			ReadView.this.getAdjusteStartXY(a, scroll_y, coords);
			mStartHandle.show(coords[0], coords[1]);

			ReadView.this.getAdjustedEndXY(b, scroll_y, coords);
			mEndHandle.show(coords[0], coords[1]);

			mIsShowing = true;
			select(a, b);
		}

		/**
		 * hide the cursor and selection
		 */
		public void hide() {
			if (mIsShowing) {
				ReadView.this.removeSelection();
				mStartHandle.hide();
				mEndHandle.hide();
				mIsShowing = false;
			}
		}

		/**
		 * redraw the moving cursor and update the selection if required
		 * 
		 * @param cursorHandle
		 *            the CursorHandle (LEFT or RIGHT)
		 * @param x
		 *            the x coordinate the cursor is pointing to on the screen
		 *            (raw)
		 * @param y
		 *            the y coordinate the cursor is pointing to on the screen
		 *            (raw)
		 * @param oldx
		 *            the previous x position the cursor pointed to
		 * @param oldy
		 *            the previous y position the cursor pointed to
		 */
		public void updatePosition(CursorHandle cursorHandle, int x, int y,
				int oldx, int oldy) {
			if (!mIsShowing) {
				return;
			}

			int old_offset = cursorHandle == mStartHandle ? ReadView.this
					.getSelection().getStart() : ReadView.this.getSelection()
					.getEnd();

			int offset = ReadView.this.getHysteresisOffset(x, y, old_offset);

			if (offset != old_offset) {

				if (cursorHandle == mStartHandle) {
					ReadView.this.getSelection().setStart(offset);
				} else {
					ReadView.this.getSelection().setEnd(offset);
				}
				ReadView.this.getSelection().select();
			}

			cursorHandle.pointTo(x, y);
		}

		/**
		 * Select the textview between start and end.
		 * <p/>
		 * Precondition: start, end must be valid
		 * 
		 * @param start
		 *            the starting offset
		 * @param end
		 *            the ending offset
		 */
		private void select(int start, int end) {
			ReadView.this.setSelection(Math.min(start, end),
					Math.abs(end - start));
		}

		@SuppressWarnings("unused")
		public boolean isShowing() {
			return mIsShowing;
		}

		@Override
		public void onTouchModeChanged(boolean isInTouchMode) {
			if (!isInTouchMode) {
				hide();
			}
		}
	}

	/**
	 * represents a single cursor
	 */
	private class CursorHandle extends View {

		/**
		 * the {@link PopupWindow} containing the cursor drawable
		 */
		private final PopupWindow mContainer;

		/**
		 * the drawble of the cursor
		 */
		private Drawable mDrawable;

		/**
		 * whether the user is dragging the cursor
		 */
		@SuppressWarnings("unused")
		private boolean mIsDragging;

		/**
		 * the controller that's controlling the cursor
		 */
		private SelectionCursorController mController;

		/**
		 * the height of the cursor
		 */
		private int mHeight;

		/**
		 * the width of the cursor
		 */
		private int mWidth;

		/**
		 * the x coordinate of the "pointer" of the cursor
		 */
		private int mHotspotX;

		/**
		 * the y coordinate of the "pointer" of the cursor which is usually the
		 * top, so it's zero.
		 */
		private int mHotspotY;

		/**
		 * Adjustment to add to the Raw x, y coordinate of the touch position to
		 * get the location of where the cursor is pointing to
		 */
		private int mAdjustX;
		private int mAdjustY;

		private int mOldX;
		private int mOldY;

		public CursorHandle(SelectionCursorController controller) {
			super(ReadView.this.getContext());

			mController = controller;

			mDrawable = getResources().getDrawable(R.drawable.cursor);

			mContainer = new PopupWindow(this);
			mContainer.setClippingEnabled(false);

			/*
			 * My Note getIntrinsicWidth() returns the width of the drawable
			 * after it has been scaled to the current device's density e.g. if
			 * the drawable is a 15 x 20 image and we load the image on a Nexus
			 * 4 (which has a density of 2.0), getIntrinsicWidth() shall return
			 * 15 * 2 = 30
			 */
			mHeight = mDrawable.getIntrinsicHeight();
			mWidth = mDrawable.getIntrinsicWidth();

			// the PopupWindow has an initial dimension of (0, 0)
			// must set the width/height of the popupwindow in order for it to
			// be drawn
			mContainer.setWidth(mWidth);
			mContainer.setHeight(mHeight);

			// this is the location of where the pointer is relative to the
			// cursor itself
			// if the left and right cursor are different, mHotspotX will need
			// to be calculated
			// differently for each cursor. Currently, I'm using the same left
			// and right cursor

			// mHotspotX = mWidth / 2;
			mHotspotX = 0;
			mHotspotY = 0;

			invalidate();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			setMeasuredDimension(mWidth, mHeight);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			mDrawable.setBounds(0, 0, mWidth, mHeight);
			mDrawable.draw(canvas);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {

			int rawX = (int) event.getRawX();
			int rawY = (int) event.getRawY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				// calculate distance from the (x,y) of the finger to where the
				// cursor
				// points to
				mAdjustX = mHotspotX - (int) event.getX();
				mAdjustY = mHotspotY - (int) event.getY();
				mOldX = mAdjustX + rawX;
				mOldY = mAdjustY + rawY;

				mIsDragging = true;
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL: {
				Log.d(TAG, "up or cancel");
				mIsDragging = false;
				mController.snapToSelection();

				final Intent toAnnotation = new Intent(getContext(),
						AnnotationActivity.class);
				makeAnnotationConfirmButton
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {

								int annotationStart = getSelection().getStart();
								int annotationEnd = getSelection().getEnd();
								String annotationText = (String) getSelection()
										.getSelectedText().toString();

								toAnnotation.putExtra("bid", bookIndex);
								toAnnotation.putExtra("access_token",
										access_token);
								toAnnotation.putExtra("uid", uid);
								annotationStartIndex = ReadBookActivity
										.convertStartIndexToWebsiteStandard(annotationStart);
								annotationEndIndex = annotationStartIndex
										+ annotationEnd - annotationStart;
								toAnnotation.putExtra("start",
										annotationStartIndex);
								toAnnotation
										.putExtra("end", annotationEndIndex);
								toAnnotation.putExtra("selected text",
										annotationText);
								toAnnotation.putExtra("page", pageNumber);
								annotationDialog.dismiss();
								getContext().startActivity(toAnnotation);
							}
						});
				annotationDialog.show();
				break;
			}
			case MotionEvent.ACTION_MOVE: {
				// calculate the raw (x, y) the cursor is POINTING TO
				int x = mAdjustX + rawX;
				int y = mAdjustY + rawY;
				mController.updatePosition(this, x, y, mOldX, mOldY);
				mOldX = x;
				mOldY = y;
				break;
			}
			}
			return true; // consume the event
		}

		public boolean isShowing() {
			return mContainer.isShowing();
		}

		/**
		 * Show the cursor pointing to the specified point.
		 * 
		 * @param x
		 *            the x coordinate of the point relative to the TextView
		 * @param y
		 *            the y coordinate of the point relative to the TextView
		 */
		public void show(int x, int y) {
			final int[] coords = mTempCoords;
			ReadView.this.getLocationInWindow(coords);

			coords[0] += x - mHotspotX;
			coords[1] += y - mHotspotY;
			mContainer.showAtLocation(ReadView.this, Gravity.NO_GRAVITY,
					coords[0], coords[1]);
		}

		/**
		 * move the cursor to point the (x,y) location on the screen.
		 * 
		 * @param x
		 *            the x coordinate on the screen
		 * @param y
		 *            the y coordinate on the screen
		 */
		private void pointTo(int x, int y) {
			if (isShowing()) {
				mContainer.update(x - mHotspotX, y - mHotspotY, -1, -1);
			}
		}

		/**
		 * hide this cursor
		 */
		public void hide() {
			mIsDragging = false;
			mContainer.dismiss();
		}
	}
}