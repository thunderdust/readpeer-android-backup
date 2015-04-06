package socialatwork.readpeer.Fragments;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.ImageNameGenerator;
import socialatwork.readpeer.R;
import socialatwork.readpeer.Cache.ImageLoader;
import socialatwork.readpeer.Cache.tdCacheManager;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
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

public class ProfileFollowerFragment extends Fragment {

	private final String TAG = "profile follower fragment";;
	private ListView mfollowerListView;
	private JSONObject mfollowerJSON;
	private ProfileFollowerAdapter mAdapter;
	private List<Map<String, Object>> mFollowerInfoList;
	private ArrayList<URL> mfollowerImageURLs;
	private String uid;
	private String access_token;

	private tdHttpClient mHttpClient;
	private ImageNameGenerator mINGenerator;
	private tdCacheManager mCacheManager;
	private static Context mContext;

	private final int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private final int GET_FOLLOWER_JSON_DONE = 1;
	private final int LOAD_INFO_FROM_JSON_DONE = 2;
	private final int LOAD_IAMGE_FROM_CACHE_DONE = 3;
	private final int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;
	/* Name user image with this String when image url is missing */
	private final String USER_IMAGE_NOT_FOUND = "NULL.jpg";

	private ArrayList<Drawable> userImages;

	private int followerCount = 0;

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

			else if (m.what == GET_FOLLOWER_JSON_DONE) {
				try {
					loadFollowerInfoToList(mfollowerJSON);
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
				mAdapter = new ProfileFollowerAdapter(getActivity(),
						mFollowerInfoList);
				mfollowerListView.setAdapter(mAdapter);
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

		// mContentHandler = ReturnedContentHandler.getHandlerInstance();
		mContext = getActivity();
		mINGenerator = ImageNameGenerator.getInstance();
		mCacheManager = tdCacheManager.getCacheManagerInstance(mContext);
		mHttpClient = tdHttpClient.getClientInstance();

		if (getArguments() != null) {
			uid = getArguments().getString("uid");
			Log.i(TAG, uid);
			access_token = getArguments().getString("access_token");
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
					mfollowerJSON = new JSONObject(
							mHttpClient.getFollowersOrFollowing("follower",
									access_token, uid));
					handler.sendEmptyMessage(GET_FOLLOWER_JSON_DONE);

				} catch (JSONException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void loadFollowerInfoToList(JSONObject infoObject)
			throws JSONException, MalformedURLException {

		mFollowerInfoList = new ArrayList<Map<String, Object>>();
		mfollowerImageURLs = new ArrayList<URL>();

		followerCount = Integer.parseInt(infoObject
				.getString("number_of_follower"));
		JSONArray followerInfoArray = infoObject.getJSONArray("follower");

		for (int i = 0; i < followerCount; i++) {

			HashMap singleFollowerInfoMap = new HashMap<String, Object>();
			JSONObject singleFollowerJSON = followerInfoArray.getJSONObject(i);

			String followerName = singleFollowerJSON.getString("name");
			Log.d(TAG, "follower:" + followerName);
			singleFollowerInfoMap.put("username", followerName);

			String userImageURLStr = singleFollowerJSON.getString("picture");
			Log.d(TAG, "image url:" + userImageURLStr);

			URL userImageURL;
			if (!userImageURLStr.isEmpty()) {
				userImageURL = new URL(userImageURLStr);
			} else {
				Log.w(TAG, "empty url");
				userImageURL = null;
			}
			singleFollowerInfoMap.put("image", userImageURL);
			mfollowerImageURLs.add(userImageURL);

			String userID = singleFollowerJSON.getString("uid");
			Log.d(TAG, "follower uid:" + uid);
			singleFollowerInfoMap.put("userid", userID);

			mFollowerInfoList.add(singleFollowerInfoMap);
		}
		handler.sendEmptyMessage(LOAD_INFO_FROM_JSON_DONE);
	}

	public void downloadFollowerImages() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//sep22
					//mCacheManager.downloadUncachedImages(mfollowerImageURLs,null);
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
				for (int i = 0; i < followerCount; i++) {
					String imageName = (String) mFollowerInfoList.get(i).get(
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d(TAG, "on create view");
		// Inflate the layout for this fragment
		ViewGroup rootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_profile_follower, container, false);
		if (rootView != null) {
			mfollowerListView = (ListView) rootView
					.findViewById(R.id.listview_profile_follower);
		} else {
			Log.d(TAG, "fragment view is null");
		}
		return rootView;
	}

	private class ProfileFollowerAdapter extends BaseAdapter {

		private int itemCount = 0;
		private Context context;
		private List<Map<String, Object>> followerInfoList;
		private String[] urlArray;
		private boolean mBusy = false;
		private ImageLoader mImageLoader;

		// Constructor for building such an adapter
		public ProfileFollowerAdapter(Context context,
				List<Map<String, Object>> list) {

			this.context = context;
			this.followerInfoList = list;
			this.itemCount = followerCount;
			mImageLoader = new ImageLoader(context);
			urlArray = new String[itemCount];
			for (int i = 0; i < itemCount; i++) {
				URL imageURL = (URL) followerInfoList.get(i).get("image");
				if (imageURL != null) {
					urlArray[i] = imageURL.toString();
				} else {
					urlArray[i] = null;
				}
			}
		}

		class ViewHolder {
			TextView mTextView;
			ImageView mImageView;
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

			String url = urlArray[position % urlArray.length];
			viewHolder.mImageView
					.setImageResource(R.drawable.default_user_image);

			if (url != null) {
				if (!mBusy) {
					mImageLoader
							.DisplayImage(url, viewHolder.mImageView, false);
				} else {
					mImageLoader.DisplayImage(url, viewHolder.mImageView, true);
				}
			}

			String username = (String) this.followerInfoList.get(position).get(
					"username");
			viewHolder.mTextView.setText(username);

			final Button followBtn = (Button) convertView
					.findViewById(R.id.btn_follow);
			followBtn.setText("Block");
			followBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// click the button will block the user
					if (followBtn.getText().toString().compareTo("Block") == 0) {

						String targetUserID = (String) followerInfoList.get(
								position).get("userid");
						boolean isBlockDone = false;
						try {
							isBlockDone = mHttpClient.blockFollower(uid,
									access_token, targetUserID);
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (isBlockDone) {
							followBtn.setText("Blocked");
						} else {
							Toast.makeText(context, "Block failed",
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
			return convertView;
		}
	}
}
