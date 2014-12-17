package socialatwork.readpeer;

import java.lang.reflect.InvocationTargetException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/* This class uses a full screen web view to load annotation videos */
public class AnnotationVideoViewActivity extends Activity {
	private final String TAG = "annotation video view";
	private WebView mWebView;

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_annotation_video_view);
		Bundle extras = getIntent().getExtras();
		String videoLink = extras.getString("video link");
		Log.i(TAG, videoLink);
		mWebView = (WebView) findViewById(R.id.annotation_video_webview);
		mWebView.getSettings().setPluginState(PluginState.ON);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return false;
			}
		});
		mWebView.loadUrl(videoLink);

	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			mWebView.getClass().getMethod("onPause")
					.invoke(mWebView, (Object[]) null);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			mWebView.getClass().getMethod("onResume")
					.invoke(mWebView, (Object[]) null);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onDestroy() {

		if (mWebView != null) {

			mWebView.clearCache(false);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				mWebView.loadUrl("");
			} else {
				mWebView.clearView();
			}
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				mWebView.freeMemory();
			}
			ViewGroup parent = (ViewGroup) mWebView.getParent();
			if (parent != null) {
				parent.removeView(mWebView);
			}
			mWebView.removeAllViews();
			mWebView.destroy();
		}

		super.onDestroy();
	}
}
