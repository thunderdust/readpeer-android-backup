package socialatwork.readpeer;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.Cache.ImageLoader;
import socialatwork.readpeer.WebRelatedComponents.ReturnedContentHandler;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class NewsFeedActivity extends FragmentActivity {

	private static ReturnedContentHandler mContentHandler;
	private static String username;
	private static String avatar_url_link;
	private static String uid;
	private static String access_token;
	private static URL avatar_url;
	private static final String TAG = "News Feed Activity";

	private JSONObject mNewsFeedJSONObject;
	private List<Map<String, Object>> mNewsFeedList;
	private int mNewsFeedCount = 0;
	NewsFeedListAdapter mAdapter;
	private ListView mNewsListView;
	private Context mContext;

	/* INFO MAP KEY NAMES */

	private final String MAP_USERNAME = "username";
	private final String MAP_USER_IMAGE = "user_image";
	private final String MAP_NID = "nid";
	private final String MAP_TYPE = "type";
	private final String MAP_TIME = "time";
	private final String MAP_TITLE = "title";
	private final String MAP_BOOKNAME = "bookName";
	private final String MAP_COVER_URL = "cover";
	private final String MAP_HIGHLIGHT = "highlight";
	private final String MAP_ADDON = "addon";
	private final String MAP_LIKE_COUNT = "number_of_like";
	private final String MAP_COMMENT_COUNT = "number_of_comment";
	private final String MAP_GROUP_NAME = "group_name";

	private final int LIST_SPACE_BETWEEN_ITEM = 25;
	private tdHttpClient mHttpClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_newsfeed);
		mNewsListView = (ListView) findViewById(R.id.listview_newsfeed);
		mContext = this.getApplicationContext();
		getRequiredData();
		mContentHandler = ReturnedContentHandler.getHandlerInstance();
		getNewsFeed();
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

	public void getNewsFeed() {

		mHttpClient = tdHttpClient.getClientInstance();
		mHttpClient.clearCookies();
		new loadNewsFeed().execute();

	}

	// Loading image can be time/resource consuming, use async task to deal
	public class loadNewsFeed extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {

			String newsFeedStr = null;
			try {
				newsFeedStr = mHttpClient.getUserNewsFeed(uid, access_token,
						"0");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return newsFeedStr;
		}

		@Override
		protected void onPostExecute(String newsFeed) {
			if (newsFeed != null) {
				try {
					mNewsFeedJSONObject = new JSONObject(newsFeed);
					Log.d(TAG, "Created newsfeed object");
					loadNewsFeedInfoToList(mNewsFeedJSONObject);
					mAdapter = new NewsFeedListAdapter(mContext, mNewsFeedList);
					mNewsListView.setAdapter(mAdapter);
					mNewsListView.setDividerHeight(LIST_SPACE_BETWEEN_ITEM);

				} catch (JSONException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}

			else {
				mNewsFeedJSONObject = null;
			}
		}
	}

	private void loadNewsFeedInfoToList(JSONObject infoObject)
			throws JSONException, MalformedURLException {

		mNewsFeedList = new ArrayList<Map<String, Object>>();

		mNewsFeedCount = Integer.parseInt(infoObject
				.getString("number_of_newsfeed"));
		Log.d(TAG, "newsfeed count:" + mNewsFeedCount);
		JSONArray newsInfoArray = infoObject.getJSONArray("newsfeeds");

		for (int i = 0; i < mNewsFeedCount; i++) {

			HashMap singleNewsInfoMap = new HashMap<String, Object>();
			JSONObject singleNewsJSON = newsInfoArray.getJSONObject(i);

			String userName = singleNewsJSON.getString("name");
			singleNewsInfoMap.put(MAP_USERNAME, userName);

			String userImageURLStr = singleNewsJSON.getString("picture");
			singleNewsJSON.put(MAP_USER_IMAGE, userImageURLStr);

			String nid = singleNewsJSON.getString("nid");
			singleNewsInfoMap.put(MAP_NID, nid);

			String type = singleNewsJSON.getString("type");
			singleNewsInfoMap.put(MAP_TYPE, type);
			Log.d(TAG, "type:" + type);

			String time = singleNewsJSON.getString("changed");
			singleNewsInfoMap.put(MAP_TIME, time);

			String title = singleNewsJSON.getString("title");
			Log.d(TAG, "title:" + title);
			singleNewsInfoMap.put(MAP_TITLE, title);

			String bookName = singleNewsJSON.getString("book_name");
			singleNewsInfoMap.put(MAP_BOOKNAME, bookName);

			String bookCoverUrl = singleNewsJSON.getString("cover_url");
			singleNewsInfoMap.put(MAP_COVER_URL, bookCoverUrl);

			String highlight = singleNewsJSON.getString("content");
			singleNewsInfoMap.put(MAP_HIGHLIGHT, highlight);

			String addon = singleNewsJSON.getString("addon");
			singleNewsInfoMap.put(MAP_ADDON, addon);

			String numberOfLike = singleNewsJSON.getString("number_of_like");
			singleNewsInfoMap.put(MAP_LIKE_COUNT, numberOfLike);

			String numberOfComment = singleNewsJSON
					.getString("number_of_comment");
			singleNewsInfoMap.put(MAP_COMMENT_COUNT, numberOfComment);

			mNewsFeedList.add(singleNewsInfoMap);
		}
	}

	private class NewsFeedListAdapter extends BaseAdapter {

		private int itemCount = 0;
		private Context context;
		private List<Map<String, Object>> newsfeedList;
		private String[] userImageURLArray;
		private String[] previewImageURLArray;
		private boolean mBusy = false;
		private ImageLoader mImageLoader;
		private String[] newsType;

		private TextView mContentTextView;
		private TextView mTimeTextView;
		private ImageView mUserImageView;
		private ImageView mPreviewImageView;

		public void setFlagBusy(boolean isBusy) {
			this.mBusy = isBusy;
		}

		// Constructor for building such an adapter
		public NewsFeedListAdapter(Context context,
				List<Map<String, Object>> list) {

			this.context = context;
			this.newsfeedList = list;
			this.itemCount = mNewsFeedCount;
			mImageLoader = new ImageLoader(context);
			userImageURLArray = new String[itemCount];
			previewImageURLArray = new String[itemCount];
			newsType = new String[itemCount];

			for (int i = 0; i < itemCount; i++) {

				// Get user image URL
				String userImageURL = (String) newsfeedList.get(i).get(
						MAP_USER_IMAGE);
				if (userImageURL != null) {
					userImageURLArray[i] = userImageURL;
				} else {
					userImageURLArray[i] = null;
				}
				newsType[i] = (String) newsfeedList.get(i).get(MAP_TYPE);
			}
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return itemCount;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {

				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.newsfeed_item, null,
						false);
			}

			mContentTextView = (TextView) convertView
					.findViewById(R.id.newsfeed_description);

			mTimeTextView = (TextView) convertView
					.findViewById(R.id.newsfeed_time_textview);

			setTime(position, mTimeTextView);

			mUserImageView = (ImageView) convertView
					.findViewById(R.id.newsfeed_user_image);

			mPreviewImageView = (ImageView) convertView
					.findViewById(R.id.newsfeed_preview_image);

			String userImageUrl = userImageURLArray[position
					% userImageURLArray.length];
			mUserImageView.setImageResource(R.drawable.default_user_image);

			if (userImageUrl != null) {
				if (!mBusy) {
					mImageLoader.DisplayImage(userImageUrl, mUserImageView,
							false);
				} else {
					mImageLoader.DisplayImage(userImageUrl, mUserImageView,
							false);
				}
			}

			String likeCount = (String) this.newsfeedList.get(position).get(
					"number_of_like");
			String commentCount = (String) this.newsfeedList.get(position).get(
					"number_of_comment");

			Button likeCountBtn = (Button) convertView
					.findViewById(R.id.newsfeed_like_btn);
			likeCountBtn.setText(likeCount);

			Button commentCountBtn = (Button) convertView
					.findViewById(R.id.newsfeed_comment_btn);
			commentCountBtn.setText(commentCount);

			/* Render based on news feed type */
			renderNewsfeedContent(newsType[position], convertView, position);
			return convertView;
		}

		private void renderNewsfeedContent(String type, View convertView,
				int position) {

			if (type.compareTo("annotation") == 0) {

				for (int i = 0; i < itemCount; i++) {
					// Get preview image URL
					String previewImageURL = (String) newsfeedList.get(i).get(
							MAP_COVER_URL);
					if (previewImageURL != null) {
						previewImageURLArray[i] = previewImageURL;
					} else {
						previewImageURLArray[i] = null;
					}
				}

				String previewImageUrl = previewImageURLArray[position
						% previewImageURLArray.length];
				if (previewImageUrl != null) {
					if (!mBusy) {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					} else {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					}
				}

				String username = (String) this.newsfeedList.get(position).get(
						MAP_USERNAME);
				String highlight = (String) this.newsfeedList.get(position)
						.get(MAP_HIGHLIGHT);
				String bookName = (String) this.newsfeedList.get(position).get(
						MAP_BOOKNAME);

				String annotationContent = (String) this.newsfeedList.get(
						position).get(MAP_ADDON);
				Log.d(TAG, annotationContent);

				String description = "<font color=#000000>" + username
						+ "</font>" + " shared an annotation in "
						+ "<font color=#00CD00>" + bookName + "<br><br>"
						+ "</font>" + "<font color=#000000>" + highlight
						+ "</font>" + "<br><br>" + annotationContent;

				mContentTextView.setText(Html.fromHtml(description));
			}

			else if (type.compareTo("book") == 0) {

				for (int i = 0; i < itemCount; i++) {
					// Get preview image URL
					String previewImageURL = (String) newsfeedList.get(i).get(
							MAP_COVER_URL);
					if (previewImageURL != null) {
						previewImageURLArray[i] = previewImageURL;
					} else {
						previewImageURLArray[i] = null;
					}
				}

				String previewImageUrl = previewImageURLArray[position
						% previewImageURLArray.length];
				if (previewImageUrl != null) {
					if (!mBusy) {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					} else {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					}
				}

				String username = (String) this.newsfeedList.get(position).get(
						MAP_USERNAME);
				String bookName = (String) this.newsfeedList.get(position).get(
						MAP_TITLE);

				String description = "<font color=#000000>" + username
						+ "</font>" + " shared a book "
						+ "<font color=#00CD00>" + bookName + "<br><br>"
						+ "</font>";

				mContentTextView.setText(Html.fromHtml(description));

			}

			else if (type.compareTo("group") == 0) {

				for (int i = 0; i < itemCount; i++) {
					// Get preview image URL
					String previewImageURL = (String) newsfeedList.get(i).get(
							MAP_COVER_URL);
					if (previewImageURL != null) {
						previewImageURLArray[i] = previewImageURL;
					} else {
						previewImageURLArray[i] = null;
					}
				}

				String previewImageUrl = previewImageURLArray[position
						% previewImageURLArray.length];
				if (previewImageUrl != null) {
					if (!mBusy) {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					} else {
						mImageLoader.DisplayImage(previewImageUrl,
								mPreviewImageView, false);
					}
				}

				String username = (String) this.newsfeedList.get(position).get(
						MAP_USERNAME);
				String groupName = (String) this.newsfeedList.get(position)
						.get(MAP_TITLE);

				String description = "<font color=#000000>" + username
						+ "</font>" + " shared a group "
						+ "<font color=#00CD00>" + groupName + "<br><br>"
						+ "</font>";

				mContentTextView.setText(Html.fromHtml(description));
			}
		}

		private void setTime(int position, TextView tv) {

			String time = (String) this.newsfeedList.get(position)
					.get(MAP_TIME);
			Log.d(TAG, "time:" + time);
			if (time != null) {
				Long timestamp = Long.valueOf(time);
				Date d = new Date(timestamp);
				Log.d(TAG, "date locale:" + d.toLocaleString());
				String format = "yyyy-MM-dd HH:mm";
				SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
				String timeStr = sdf.format(d);
				tv.setText(timeStr);
			} else {
				tv.setText("");
			}
		}
	}
}
