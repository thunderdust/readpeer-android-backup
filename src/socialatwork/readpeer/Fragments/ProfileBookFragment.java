package socialatwork.readpeer.Fragments;

import java.io.File;
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
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ProfileBookFragment extends Fragment {

	private final String TAG = "profile book fragment";;
	private ListView mBookListView;
	private JSONObject mBookJSON;
	private ProfileBookAdapter mAdapter;
	private List<Map<String, Object>> mBookInfoList;
	private ArrayList<URL> mBookImageURLs;
	private ArrayList<String> mLocalBookIndexList;
	private String uid;
	private String access_token;

	private tdHttpClient mHttpClient;
	private ImageNameGenerator mINGenerator;
	private tdCacheManager mCacheManager;
	private static Context mContext;

	private final int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private final int GET_BOOK_JSON_DONE = 1;
	private final int LOAD_INFO_FROM_JSON_DONE = 2;
	private final int LOAD_IAMGE_FROM_CACHE_DONE = 3;
	private final int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;
	/* Name user image with this String when image url is missing */
	private final String USER_IMAGE_NOT_FOUND = "NULL.jpg";

	private ArrayList<Drawable> bookCoverImages;

	private int bookCount = 0;

	private Handler bookInfoHandler = new Handler() {

		@Override
		public void handleMessage(Message m) {

			if (m.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE) {
				loadbookCoverImages();
			}

			else if (m.what == PICTURE_DOWNLOAD_TIMEOUT_MESSAGE) {
				Log.d(TAG,
						"-------------image downloading timeout--------------");
				loadbookCoverImages();
			}

			else if (m.what == GET_BOOK_JSON_DONE) {
				try {
					loadFollowerInfoToList(mBookJSON);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				// downloadFollowerImages();
				// loadbookCoverImages();
			}

			else if (m.what == LOAD_INFO_FROM_JSON_DONE) {
				// downloadFollowerImages();
				Log.d(TAG, "Load from json done");
				mAdapter = new ProfileBookAdapter(getActivity(), mBookInfoList);
				mBookListView.setAdapter(mAdapter);
			}

			else if (m.what == LOAD_IAMGE_FROM_CACHE_DONE) {
				// mAdapter.notifyDataSetChanged();
				// mAdapter = new ProfileBookAdapter(getActivity(),
				// mBookInfoList);
				// mBookListView.setAdapter(mAdapter);
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

		getBookInfoJSON();
		loadLocalBookIndex();
	}

	private class NullArgumentsException extends Exception {

		private static final long serialVersionUID = 1L;

		private NullArgumentsException(String msg) {
			super(msg);
		}
	}

	private void getBookInfoJSON() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					mBookJSON = new JSONObject(mHttpClient.getUserContent(
							access_token, "book", uid));
					bookInfoHandler.sendEmptyMessage(GET_BOOK_JSON_DONE);

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

		mBookInfoList = new ArrayList<Map<String, Object>>();
		mBookImageURLs = new ArrayList<URL>();

		bookCount = Integer.parseInt(infoObject.getString("number_of_book"));
		JSONArray followerInfoArray = infoObject.getJSONArray("book");

		for (int i = 0; i < bookCount; i++) {

			HashMap singleFollowerInfoMap = new HashMap<String, Object>();
			JSONObject singleFollowerJSON = followerInfoArray.getJSONObject(i);

			String bookName = singleFollowerJSON.getString("title");
			singleFollowerInfoMap.put("title", bookName);

			String author = singleFollowerJSON.getString("author");
			singleFollowerInfoMap.put("author", author);

			String annotationCount = singleFollowerJSON
					.getString("annotation_count");
			singleFollowerInfoMap.put("annotation_count", annotationCount);

			String bookCoverURLStr = singleFollowerJSON.getString("book_cover");
			Log.d(TAG, "image url:" + bookCoverURLStr);

			String totalPage = singleFollowerJSON.getString("total_page");
			singleFollowerInfoMap.put("total_page", totalPage);

			URL coverImageURL;
			if (!bookCoverURLStr.isEmpty()) {
				coverImageURL = new URL(bookCoverURLStr);
			} else {
				coverImageURL = null;
			}
			singleFollowerInfoMap.put("book_cover", coverImageURL);
			mBookImageURLs.add(coverImageURL);

			String bookID = singleFollowerJSON.getString("bid");
			singleFollowerInfoMap.put("bid", bookID);

			mBookInfoList.add(singleFollowerInfoMap);
		}
		bookInfoHandler.sendEmptyMessage(LOAD_INFO_FROM_JSON_DONE);
	}

	public void downloadFollowerImages() {

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					//sep22
					//mCacheManager.downloadUncachedImages(mBookImageURLs, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	// After downloading, load images from local cache
	private void loadbookCoverImages() {

		Log.d(TAG, "loading image from cache");
		bookCoverImages = new ArrayList<Drawable>();
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < bookCount; i++) {
					String imageName = (String) mBookInfoList.get(i).get(
							"imageName");
					// If no user image, use default one
					bookCoverImages.add(mCacheManager
							.getDrawableFromCache(imageName));
					Log.d(TAG, "loaded image for:" + i);
				}
				bookInfoHandler.sendEmptyMessage(LOAD_IAMGE_FROM_CACHE_DONE);
			}
		});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		Log.d(TAG, "on create view");
		// Inflate the layout for this fragment
		ViewGroup rootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_profile_book, container, false);
		if (rootView != null) {
			mBookListView = (ListView) rootView
					.findViewById(R.id.listview_profile_book);
		} else {
			Log.d(TAG, "fragment view is null");
		}
		return rootView;
	}

	// Check if certain book is in local storage by comparing
	// book id with mLocalBookIndexList
	private boolean isThisBookInLocalStorage(String bookIndex) {
		for (int i = 0; i < mLocalBookIndexList.size(); i++) {
			if (mLocalBookIndexList.get(i).compareTo(bookIndex) == 0) {
				return true;
			}
		}
		return false;
	}

	private File[] getLocalBookFiles() {
		if (Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			String storagePath = Environment.getExternalStorageDirectory()
					.getPath() + "/Readpeer/Books/";
			File folder = new File(storagePath);
			File[] fileList = folder.listFiles();
			return fileList;
		} else {
			return null;
		}
	}

	// Search local storage to find local book index
	private void loadLocalBookIndex() {

		mLocalBookIndexList = new ArrayList<String>();
		File[] localBookList = getLocalBookFiles();
		if (localBookList != null) {
			for (int i = 0; i < localBookList.length; i++) {
				String name = localBookList[i].getName();
				String index = name.split("-")[1];
				Log.d(TAG, "Adding index:" + index);
				mLocalBookIndexList.add(index);
			}
		} else {
			Log.d(TAG, "No local books");
		}
	}

	private class ProfileBookAdapter extends BaseAdapter {

		private int itemCount = 0;
		private Context context;
		private List<Map<String, Object>> bookInfoList;
		private String[] urlArray;
		private boolean mBusy = false;
		private ImageLoader mImageLoader;

		class ViewHolder {
			TextView mTextView;
			ImageView mImageView;
		}

		public void setFlagBusy(boolean isBusy) {
			this.mBusy = isBusy;
		}

		// Constructor for building such an adapter
		public ProfileBookAdapter(Context context,
				List<Map<String, Object>> list) {

			this.context = context;
			this.bookInfoList = list;
			this.itemCount = bookCount;
			mImageLoader = new ImageLoader(context);
			urlArray = new String[itemCount];
			for (int i = 0; i < itemCount; i++) {
				if ((URL) bookInfoList.get(i).get("book_cover") != null) {
					urlArray[i] = ((URL) bookInfoList.get(i).get("book_cover"))
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

		@SuppressWarnings("deprecation")
		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			// TODO Auto-generated method stub
			ViewHolder viewHolder = null;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.profile_book_item,
						null, false);
				viewHolder = new ViewHolder();
				viewHolder.mTextView = (TextView) convertView
						.findViewById(R.id.profile_bookDetailText);
				viewHolder.mImageView = (ImageView) convertView
						.findViewById(R.id.profile_bookCoverView);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			String url = urlArray[position % urlArray.length];
			viewHolder.mImageView.setImageResource(R.drawable.loading);

			if (url != null) {
				if (!mBusy) {
					mImageLoader
							.DisplayImage(url, viewHolder.mImageView, false);
				} else {
					mImageLoader
							.DisplayImage(url, viewHolder.mImageView, false);
				}
			}

			String bookDetail = "Book Name: "
					+ (String) bookInfoList.get(position).get("title")
					+ "\n\n"
					+ "Annotations: "
					+ (String) bookInfoList.get(position).get(
							"annotation_count") + "\n\n" + "Total pages: "
					+ (String) bookInfoList.get(position).get("total_page");
			viewHolder.mTextView.setText(bookDetail);

			final Button downloadBtn = (Button) convertView
					.findViewById(R.id.profile_download_book_button);
			String bid = (String) bookInfoList.get(position).get("bid");
			if (!isThisBookInLocalStorage(bid)) {
				downloadBtn.setText("Download");
			} else {
				downloadBtn.setText("Read Now");
			}

			return convertView;
		}
	}

}
