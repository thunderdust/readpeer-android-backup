package socialatwork.readpeer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.Cache.ImageLoader;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileFollowingActivity extends FragmentActivity {

	private final String TAG = "profile following activity";;
	private ListView mfollowingListView;
	private JSONObject mfollowingJSON;
	private ProfileFollowingAdapter mAdapter;
	private List<Map<String, Object>> mFollowingInfoList;
	private ArrayList<URL> mfollowingImageURLs;
	private String uid;
	private String access_token;

	private tdHttpClient mHttpClient;
	private ImageNameGenerator mINGenerator;
	private tdCacheManager mCacheManager;
	private static Context mContext;

	private final int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private final int GET_FOLLOWING_JSON_DONE = 1;
	private final int LOAD_INFO_FROM_JSON_DONE = 2;
	private final int LOAD_IAMGE_FROM_CACHE_DONE = 3;
	private final int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;
	/* Name user image with this String when image url is missing */
	private final String USER_IMAGE_NOT_FOUND = "NULL.jpg";

	private ArrayList<Drawable> userImages;

	private int followingCount = 0;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message m) {

			if (m.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE) {
				// loadUserImages();
			}

			else if (m.what == PICTURE_DOWNLOAD_TIMEOUT_MESSAGE) {
				Log.d(TAG,
						"-------------image downloading timeout--------------");
				// loadUserImages();
			}

			else if (m.what == GET_FOLLOWING_JSON_DONE) {
				try {
					loadFollowingInfoToList(mfollowingJSON);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}

				// downloadFollowerImages();
				// loadUserImages();
			}

			else if (m.what == LOAD_INFO_FROM_JSON_DONE) {
				// downloadFollowerImages();
				mAdapter = new ProfileFollowingAdapter(mContext,
						mFollowingInfoList);
				mfollowingListView.setAdapter(mAdapter);
			}

			else if (m.what == LOAD_IAMGE_FROM_CACHE_DONE) {
				// mAdapter.notifyDataSetChanged();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.d(TAG, "On create fragment");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile_following);
		// mContentHandler = ReturnedContentHandler.getHandlerInstance();
		mContext = this.getApplicationContext();
		mINGenerator = ImageNameGenerator.getInstance();
		mCacheManager = tdCacheManager.getCacheManagerInstance(mContext);
		mHttpClient = tdHttpClient.getClientInstance();
		mfollowingListView = (ListView) findViewById(R.id.listview_profile_following);
		if (getIntent() != null) {
			uid = getIntent().getStringExtra("uid");
			Log.i(TAG, uid);
			access_token = getIntent().getStringExtra("access_token");
			Log.i(TAG, access_token);
		} else {
			Log.e(TAG, "arguments is null");
			try {
				throw new NullArgumentsException("Arguments are null");
			} catch (NullArgumentsException e) {
				e.printStackTrace();
			}
		}

		getFollowerInfoJSON();
	}

	private class NullArgumentsException extends Exception {

		private static final long serialVersionUID = 1L;

		private NullArgumentsException(String msg) {
			super(msg);
		}
	}

	private void getFollowerInfoJSON() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mfollowingJSON = new JSONObject(
							mHttpClient.getFollowersOrFollowing("following",
									access_token, uid));
					handler.sendEmptyMessage(GET_FOLLOWING_JSON_DONE);

				} catch (JSONException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void loadFollowingInfoToList(JSONObject infoObject)
			throws JSONException, MalformedURLException {

		mFollowingInfoList = new ArrayList<Map<String, Object>>();
		mfollowingImageURLs = new ArrayList<URL>();

		followingCount = Integer.parseInt(infoObject
				.getString("number_of_following"));
		JSONArray followerInfoArray = infoObject.getJSONArray("following");

		for (int i = 0; i < followingCount; i++) {

			HashMap singleFollowingInfoMap = new HashMap<String, Object>();
			JSONObject singleFollowerJSON = followerInfoArray.getJSONObject(i);

			String followerName = singleFollowerJSON.getString("name");
			Log.d(TAG, "following:" + followerName);
			singleFollowingInfoMap.put("username", followerName);

			String userImageURLStr = singleFollowerJSON.getString("picture");
			Log.d(TAG, "image url:" + userImageURLStr);

			URL userImageURL;
			if (!userImageURLStr.isEmpty()) {
				userImageURL = new URL(userImageURLStr);
			} else {
				Log.w(TAG, "empty url");
				userImageURL = null;
			}
			singleFollowingInfoMap.put("image", userImageURL);
			mfollowingImageURLs.add(userImageURL);

			String userID = singleFollowerJSON.getString("uid");
			Log.d(TAG, "follower uid:" + uid);
			singleFollowingInfoMap.put("userid", userID);

			mFollowingInfoList.add(singleFollowingInfoMap);
		}
		handler.sendEmptyMessage(LOAD_INFO_FROM_JSON_DONE);
	}

	public void downloadFollowerImages() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//sep22
					//mCacheManager.downloadUncachedImages(mfollowingImageURLs,null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// After downloading, load images from local cache
	private void loadUserImages() {

		Log.d(TAG, "loading image from cache");
		userImages = new ArrayList<Drawable>();
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < followingCount; i++) {
					String imageName = (String) mFollowingInfoList.get(i).get(
							"imageName");
					// If no user image, use default one
					userImages.add(mCacheManager
							.getDrawableFromCache(imageName));
					Log.d(TAG, "loaded image for:" + i);
				}
				handler.sendEmptyMessage(LOAD_IAMGE_FROM_CACHE_DONE);
			}
		});
	}

	private class ProfileFollowingAdapter extends BaseAdapter {

		private int itemCount = 0;
		private Context context;
		private List<Map<String, Object>> followingInfoList;
		private boolean mBusy = false;
		private String[] urlArray;
		private ImageLoader mImageLoader;

		class ViewHolder {
			TextView mTextView;
			ImageView mImageView;
		}

		public void setFlagBusy(boolean isBusy) {
			this.mBusy = isBusy;
		}

		// Constructor for building such an adapter
		public ProfileFollowingAdapter(Context context,
				List<Map<String, Object>> list) {

			this.context = context;
			this.followingInfoList = list;
			this.itemCount = followingCount;
			mImageLoader = new ImageLoader(context);
			urlArray = new String[itemCount];

			for (int i = 0; i < itemCount; i++) {
				URL imageURL = (URL) followingInfoList.get(i).get("image");
				if (imageURL != null) {
					urlArray[i] = ((URL) followingInfoList.get(i).get("image"))
							.toString();
				} else {
					urlArray[i] = null;
				}
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

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			ViewHolder viewHolder = null;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(
						R.layout.follower_following_item, null, false);
				viewHolder = new ViewHolder();
				viewHolder.mTextView = (TextView) convertView
						.findViewById(R.id.follower_following_username);
				viewHolder.mImageView = (ImageView) convertView
						.findViewById(R.id.follower_following_user_image);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			convertView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					Intent toNewProfile = new Intent(view.getContext(),
							ProfileActivity.class);
					toNewProfile.putExtra("access_token", access_token);
					toNewProfile.putExtra(
							"username",
							(String) followingInfoList.get(position).get(
									"username"));
					toNewProfile.putExtra("uid", (String) followingInfoList
							.get(position).get("userid"));
					toNewProfile
							.putExtra("avatar_url", ((URL) followingInfoList
									.get(position).get("image")).toString());
					view.getContext().startActivity(toNewProfile);
				}

			});

			String url = urlArray[position % urlArray.length];
			viewHolder.mImageView
					.setImageResource(R.drawable.default_user_image);

			if (url != null) {
				if (!mBusy) {
					mImageLoader
							.DisplayImage(url, viewHolder.mImageView, false);
				} else {
					mImageLoader
							.DisplayImage(url, viewHolder.mImageView, false);
				}
			}

			String username = (String) this.followingInfoList.get(position)
					.get("username");
			viewHolder.mTextView.setText(username);

			final Button followBtn = (Button) convertView
					.findViewById(R.id.btn_follow);

			final boolean isFollowing = true;
			followBtn.setText("Unfollow");
			followBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					// If following, click the button will unfollow the user
					if (followBtn.getText().toString().compareTo("Unfollow") == 0) {

						String targetUserID = (String) followingInfoList.get(
								position).get("userid");
						boolean isUnfollowDone = false;

						try {
							isUnfollowDone = mHttpClient.unfollowUser(uid,
									targetUserID, access_token);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (isUnfollowDone) {
							followBtn.setText("Follow");
						} else {
							Toast.makeText(context, "Unfollow failed",
									Toast.LENGTH_SHORT).show();
						}
					}

					else {

						String targetUserID = (String) followingInfoList.get(
								position).get("userid");
						boolean isFollowDone = false;

						try {
							isFollowDone = mHttpClient.followUser(uid,
									targetUserID, access_token);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (isFollowDone) {
							followBtn.setText("Unfollow");
						} else {
							Toast.makeText(context, "Follow failed",
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
			return convertView;
		}
	}
}
