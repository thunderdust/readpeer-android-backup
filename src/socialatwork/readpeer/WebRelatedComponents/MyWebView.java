package socialatwork.readpeer.WebRelatedComponents;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewParent;
import android.webkit.WebView;

/* This overrode webview returns null for action mode callback, prevent the 
 * default action mode bar from displaying, because it conflicts with the 
 * customized long click which triggers text selection. */
public class MyWebView extends WebView {

	private String TAG = "My Web View";
	private ActionMode.Callback mActionModeCallback;

	public MyWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MyWebView(Context context) {
		super(context);
	}

	@Override
	public ActionMode startActionMode(ActionMode.Callback callback) {
		ViewParent parent = getParent();
		if (parent != null) {
			mActionModeCallback = new ActionBarCallBack();
			return parent.startActionModeForChild(this,mActionModeCallback);
		} else {
			return null;
		}
		// Log.d(TAG,"going to return null");
		// return null;
	}

	// Trivial callbacks to prevent default action callback menu
	class ActionBarCallBack implements ActionMode.Callback {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			Log.d(TAG, "on action item clicked");
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Log.d(TAG, "on action mode created");
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			// remove text highlight when context action bar is closed
			clearFocus();
			
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true;
		}
	}

}
