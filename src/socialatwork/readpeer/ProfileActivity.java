package socialatwork.readpeer;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

import socialatwork.readpeer.WebRelatedComponents.ReturnedContentHandler;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileActivity extends FragmentActivity {

	private static ReturnedContentHandler mContentHandler;
	private static String username;
	private static String avatar_url_link;
	private static String uid;
	private static String access_token;
	private static URL avatar_url;
	private static String numOBooks;
	private static String numOAnnotations;
	private static String numOFollowings;
	private static String numOFollowers;
	private static final String TAG = "profile";
	private static int TYPE_STRING = 1;
	private static int TYPE_INT = 2;
	private static int TYPE_BOOK = 3;
	private final String STATISTIC_TAG_BOOK = "book";
	private final String STATISTIC_TAG_FOLLOWER = "follower";
	private final String STATISTIC_TAG_FOLLOWING = "following";

	// private static onlinePictureDownloader mPictureDownloader;

	private tdHttpClient mHttpClient;
	private FragmentTabHost mTabHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_profile);

		getRequiredData();
		Bundle userDataBundle = getUserDataBundle();
		// Fragment profileBookFragment =
		// getSupportFragmentManager().findFragmentById(R.id.fragment_profile_book);
		// profileBookFragment.setArguments(userDataBundle);
		// mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
		// mTabHost.setup(this,
		// getSupportFragmentManager(),android.R.id.tabcontent);
		/*
		 * mTabHost.addTab(mTabHost.newTabSpec("tab_book").setIndicator("Book"),
		 * ProfileBookFragment.class, userDataBundle); /* mTabHost.addTab(
		 * mTabHost.newTabSpec("tab_following").setIndicator("Following"),
		 * ProfileFollowingFragment.class, userDataBundle); mTabHost.addTab(
		 * mTabHost.newTabSpec("tab_follower").setIndicator("Follower"),
		 * ProfileFollowerFragment.class, userDataBundle);
		 */
		setStatisticModules();
		mContentHandler = ReturnedContentHandler.getHandlerInstance();
		// mPictureDownloader = onlinePictureDownloader.getDownloaderInstance();
		try {
			loadProfileInfo(access_token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setStatisticModules() {

		LinearLayout bookCountLL = (LinearLayout) findViewById(R.id.statistic_book);
		setOnClickListenerForStatisticModules(STATISTIC_TAG_BOOK, bookCountLL);
		LinearLayout followerCountLL = (LinearLayout) findViewById(R.id.statistic_follower);
		setOnClickListenerForStatisticModules(STATISTIC_TAG_FOLLOWER,
				followerCountLL);
		LinearLayout followingCountLL = (LinearLayout) findViewById(R.id.statistic_following);
		setOnClickListenerForStatisticModules(STATISTIC_TAG_FOLLOWING,
				followingCountLL);
	}

	private void setOnClickListenerForStatisticModules(String moduleTag,
			LinearLayout statisticModule) {

		final Bundle newBundle = getUserDataBundle();
		if (moduleTag.equalsIgnoreCase(STATISTIC_TAG_BOOK)) {
			statisticModule.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.d(TAG, "book statistic clicked");
					Intent toBookPage = new Intent(ProfileActivity.this,
							ProfileBookActivity.class);
					toBookPage.putExtras(newBundle);
					startActivity(toBookPage);
				}
			});
		} else if (moduleTag.equalsIgnoreCase(STATISTIC_TAG_FOLLOWER)) {
			statisticModule.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.d(TAG, "follower statistic clicked");
					Intent toFollowerPage = new Intent(ProfileActivity.this,
							ProfileFollowerActivity.class);
					toFollowerPage.putExtras(newBundle);
					startActivity(toFollowerPage);
				}
			});
		} else if (moduleTag.equalsIgnoreCase(STATISTIC_TAG_FOLLOWING)) {
			statisticModule.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Log.d(TAG, "following statistic clicked");
					Intent toFollowingPage = new Intent(ProfileActivity.this,
							ProfileFollowingActivity.class);
					toFollowingPage.putExtras(newBundle);
					startActivity(toFollowingPage);
				}
			});
		}
	}

	private void getRequiredData() {
		// TODO Auto-generated method stub
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			access_token = extras.getString("access_token");
			uid = extras.getString("uid");
			username = extras.getString("username");
			avatar_url_link = extras.getString("avatar_url");
		} else {
			Log.e(TAG, "intent is null");
		}
	}

	/* Pass essential data to tab fragments */
	private Bundle getUserDataBundle() {

		// ProfileFollowerFragment pf = new ProfileFollowerFragment();
		// ProfileFollowingFragment pfing = new ProfileFollowingFragment();
		// ProfileBookFragment pb = new ProfileBookFragment();

		Bundle bundle = new Bundle();
		bundle.putString("access_token", access_token);
		bundle.putString("uid", uid);
		bundle.putString("username", username);
		return bundle;
	}

	public void loadProfileInfo(String token) throws Exception {

		mHttpClient = tdHttpClient.getClientInstance();
		mHttpClient.clearCookies();

		TextView bookNumTV = (TextView) findViewById(R.id.text_numOBooks);
		TextView annotationNumVT = (TextView) findViewById(R.id.text_numOAnnotations);
		TextView followingNumTV = (TextView) findViewById(R.id.text_numOFollowing);
		TextView followerNumTV = (TextView) findViewById(R.id.text_numOFollower);
		EditText usernameET = (EditText) findViewById(R.id.editText_Username);
		usernameET.setFocusable(false);

		ImageView avatarIV = (ImageView) findViewById(R.id.imageView_Avatar);

		String content = mHttpClient.getUserContent(token, "profile", uid);
		// Log.d("profile returned content", content);
		JSONObject profileObject = new JSONObject(content);

		numOBooks = profileObject.getString("num_of_books");
		numOAnnotations = profileObject.getString("num_of_annotations");
		numOFollowings = profileObject.getString("num_of_following");
		numOFollowers = profileObject.getString("num_of_follower");

		avatar_url_link = urlParser(avatar_url_link);

		try {
			avatar_url = new URL(avatar_url_link);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Load avatar image from url
		new loadPictureTask().execute(avatarIV);
		// mPictureDownloader.downloadImageFromUrl(avatar_url);
		// Bitmap bm = mPictureDownloader.getImage();

		usernameET.setText(username);
		annotationNumVT.setText(numOAnnotations);
		bookNumTV.setText(numOBooks);
		followerNumTV.setText(numOFollowers);
		followingNumTV.setText(numOFollowings);
	}

	private String urlParser(String originalURL) {
		Log.i("profile page", "before parsing:" + originalURL);
		originalURL = originalURL.replace("\\", "");
		Log.i("profile page", "after parsing:" + originalURL);
		return originalURL;
	}

	// Loading image can be time/resource consuming, use async task to deal
	public class loadPictureTask extends AsyncTask<ImageView, Void, Bitmap> {

		ImageView imageView = null;

		@Override
		protected Bitmap doInBackground(ImageView... imageViews) {

			this.imageView = imageViews[0];
			Bitmap bm = null;
			try {
				HttpURLConnection conn = (HttpURLConnection) avatar_url
						.openConnection();
				conn.setDoInput(true);
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				conn.connect();
				InputStream stream = conn.getInputStream();
				bm = BitmapFactory.decodeStream(stream);
				stream.close();
				return bm;
			} catch (Exception e) {

				String msg = e.getMessage();
				if (msg != null)
					Log.v("img", msg);
				else
					Log.e(TAG, "msg is null");
				return bm;
			}
		}

		protected void onPostExecute(Bitmap bm) {
			if (bm != null) {
				imageView.setImageBitmap(bm);
			}
		}
	}

}