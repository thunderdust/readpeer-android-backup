package socialatwork.readpeer;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import socialatwork.readpeer.Cache.tdCacheManager;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class AnnotationCommentsActivity extends FragmentActivity {

	private final String TAG = "Annotation Comment Activity";

	/* Multimedia attachment related */
	private final int REQUEST_TAKE_PHOTO = 1;
	private final int REQUEST_RECORD_AUDIO = 2;
	private final int REQUEST_GALLERY_PHOTO = 3;
	private final int REQUEST_TAKE_VIDEO = 4;
	private final int REQUEST_GALLERY_VIDEO = 5;

	/* Annotation Comments Related */
	private JSONObject annotation_comment_object;
	private int annotation_comment_count = 0;
	private String[] annotation_comment_authors;
	private URL[] annotation_comment_authors_avatar_links;
	private URL[] annotation_comment_media_preview_links;
	private URL[] annotation_comment_media_links;
	private int[] annotation_comment_type;
	private boolean[] annotation_comment_hasmore;
	private String[] annotation_comment_uid;
	private String[] annotation_comment_time;
	private Drawable[] annotation_comment_author_avatar;
	private String[] annotation_comment_content;
	private String annotation_id;
	private String bookIndex;
	private String access_token;
	private JSONObject mCommentsObject;
	// keep data such as text, link, image for use of list view adapter
	private List<Map<String, Object>> mCommentsDataMapList;
	private AnnotationCommentsAdapter mAdapter;
	private ListView mCommentListView;
	private Dialog mReplyDialog;

	/* Caching */
	private tdCacheManager mCacheManager;
	private ImageNameGenerator mINGenerator;
	private SharedPreferences mAnnotationCommentsSharedPreferences;
	private final String ANNOTATION_COMMENTS_SHARED_PREFERENCE_NAME = "annotationCommentsInfo";

	private int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private int GET_COMMENTS_JSON_DONE = 1;
	private int GET_COMMENTS_JSON_FAIL = -1;
	private int PICTURE_DOWNLOAD_TIMEOUT_MESSAGE = 408;

	private static tdHttpClient mHttpClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_annotation_comments);
		initialize();
		// After getting JSON, handler will do the following updates such as
		// display and download images
		getCommentsJSON(annotation_id, 0);
	}

	final Handler commentsHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if (msg.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE) {
				Log.d(TAG, "download complete");
				// updateCommentList();
			}

			else if (msg.what == PICTURE_DOWNLOAD_TIMEOUT_MESSAGE) {
				Log.d(TAG, "download timeout");
				// updateCommentList();
			}

			else if (msg.what == GET_COMMENTS_JSON_DONE) {
				try {
					getAnnotationCommentsInfo(mCommentsObject);
					updateCommentList();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Close activity if loading comments failed
			else if (msg.what == GET_COMMENTS_JSON_FAIL) {
				Toast.makeText(getBaseContext(), "Fail to load comments",
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	};

	private void initialize() {

		mCacheManager = tdCacheManager.getCacheManagerInstance(this);
		mHttpClient = tdHttpClient.getClientInstance();
		mAnnotationCommentsSharedPreferences = this.getSharedPreferences(
				ANNOTATION_COMMENTS_SHARED_PREFERENCE_NAME,
				Context.MODE_PRIVATE);

		// get access token from shared preference which was stored 
		// during login process
		SharedPreferences tokenSP = this.getSharedPreferences("token",
				Context.MODE_PRIVATE);
		access_token = tokenSP.getString("token", null);
		Log.d(TAG, access_token);

		Intent intent = getIntent();
		annotation_id = intent.getStringExtra("aid");
		bookIndex = intent.getStringExtra("bid");
		mINGenerator = ImageNameGenerator.getInstance();
	}

	private void updateCommentList() {

		//Log.d(TAG, "------------------updating comment list-------------------");
		mCommentsDataMapList = new ArrayList<Map<String, Object>>();
		annotation_comment_author_avatar = new Drawable[annotation_comment_count];

		for (int i = 0; i < annotation_comment_count; i++) {

			Map<String, Object> commentMap = new HashMap<String, Object>();
			// add comment author name
			commentMap.put("author", annotation_comment_authors[i]);
			// add comment author image
			String avatarImageName = mINGenerator
					.getNameFromLink(annotation_comment_authors_avatar_links[i]);
			annotation_comment_author_avatar[i] = mCacheManager
					.getDrawableFromCache(avatarImageName);
			commentMap.put("avatar", annotation_comment_author_avatar[i]);
			// add comment content
			commentMap.put("comment", annotation_comment_content[i]);
			// add time
			commentMap.put("time", annotation_comment_time[i]);
			Log.d(TAG, "time:" + commentMap.get("time"));
			// add type
			commentMap.put("type", annotation_comment_type[i]);
			// add link
			commentMap.put("link", annotation_comment_media_links[i]);
			Log.d(TAG, "link:" + commentMap.get("link"));
			// add preview image link
			commentMap
					.put("preview", annotation_comment_media_preview_links[i]);
			mCommentsDataMapList.add(commentMap);
		}

		mAdapter = new AnnotationCommentsAdapter(mCommentsDataMapList, this);
		mCommentListView = (ListView) findViewById(R.id.listview_annotation_comments);
		mCommentListView.setAdapter(mAdapter);
		mCommentListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				// show reply dialog
				// Log.d(TAG, "trigger reply dialog");
				setUpCommentsReplyDialog(index);
			}
		});
	}

	// This method extract info from comment JSONObject and store them separately 
	private void getAnnotationCommentsInfo(JSONObject commentObject)
			throws Exception {

		annotation_comment_count = Integer.parseInt(commentObject
				.getString("number_of_comments")) - 1;

		Log.d(TAG, "comment count:" + annotation_comment_count);

		boolean hasMoreComments = false;

		if (commentObject.getString("has_more_comments").compareTo("true") == 0) {
			hasMoreComments = true;
		} else {
			hasMoreComments = false;
		}

		JSONArray annotation_comments_info = new JSONArray(
				commentObject.getString("comments"));

		annotation_comment_authors = new String[annotation_comment_count];

		annotation_comment_type = new int[annotation_comment_count];
		annotation_comment_hasmore = new boolean[annotation_comment_count];
		annotation_comment_uid = new String[annotation_comment_count];
		annotation_comment_media_links = new URL[annotation_comment_count];
		annotation_comment_time = new String[annotation_comment_count];
		annotation_comment_content = new String[annotation_comment_count];
		annotation_comment_authors_avatar_links = new URL[annotation_comment_count];
		annotation_comment_media_preview_links = new URL[annotation_comment_count];

		ArrayList<URL> imageURLs = new ArrayList<URL>();

		for (int i = 0; i < annotation_comment_count; i++) {

			JSONObject singleAnnotationCommentObject = annotation_comments_info
					.getJSONObject(i);

			annotation_comment_authors[i] = singleAnnotationCommentObject
					.getString("name");
			Log.d(TAG, annotation_comment_authors[i]);

			annotation_comment_uid[i] = singleAnnotationCommentObject
					.getString("uid");
			Log.d(TAG, annotation_comment_uid[i]);

			annotation_comment_authors_avatar_links[i] = new URL(
					singleAnnotationCommentObject.getString("picture"));

			imageURLs.add(annotation_comment_authors_avatar_links[i]);

			annotation_comment_type[i] = Integer
					.parseInt(singleAnnotationCommentObject.getString("type"));

			String previewImageLink = singleAnnotationCommentObject
					.getString("img");
			if (!previewImageLink.isEmpty()) {
				Log.d(TAG, "preview image link:" + previewImageLink);
				annotation_comment_media_preview_links[i] = new URL(
						previewImageLink);
				imageURLs.add(annotation_comment_media_preview_links[i]);
			} else {
				Log.d(TAG, "preview image link empty");
				annotation_comment_media_preview_links[i] = null;
			}

			String mediaLink = singleAnnotationCommentObject.getString("link");
			if (!mediaLink.isEmpty()) {
				Log.d(TAG, "media link:" + mediaLink);

				if (mediaLink.length() > 3) {
					annotation_comment_media_links[i] = new URL(mediaLink);
				}

				else {
					annotation_comment_media_links[i] = null;
				}
				// keep image information if media type is image
				if (annotation_comment_type[i] == 2) {
					imageURLs.add(annotation_comment_media_links[i]);
				}
			} else {
				Log.d(TAG, "media link is empty");
				annotation_comment_media_links[i] = null;
			}
			annotation_comment_content[i] = (String) singleAnnotationCommentObject
					.getJSONArray("body").get(0);

			annotation_comment_time[i] = singleAnnotationCommentObject
					.getString("changed_time");
		}
        // sep22
		//mCacheManager.downloadUncachedImages(imageURLs, commentsHandler);
	}

	private void getCommentsJSON(final String aid, int pageNumber) {

		String key = getAnnotationCommentsCacheKey(aid, pageNumber);
		// Get from local cache
		if (!mCacheManager.shouldDownloadJSONFromServer(
				mAnnotationCommentsSharedPreferences, key)) {
			try {
				mCommentsObject = getCommentsFromCache(aid, pageNumber);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// Get from server
		else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						mCommentsObject = getCommentsFromServer(aid, 0);
						if (mCommentsObject != null) {
							commentsHandler
									.sendEmptyMessage(GET_COMMENTS_JSON_DONE);
						} else {
							commentsHandler
									.sendEmptyMessage(GET_COMMENTS_JSON_FAIL);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

	private JSONObject getCommentsFromServer(String aid, int pageNumber)
			throws Exception {

		if (mHttpClient == null) {
			mHttpClient = tdHttpClient.getClientInstance();
		}
		JSONObject annotationCommentObject = new JSONObject(
				mHttpClient
						.getAnnotationComments(access_token, aid, pageNumber));

		// Save JSON Object into cache
		String cacheKey = getAnnotationCommentsCacheKey(aid, pageNumber);
		mCacheManager.setJSONCache(mAnnotationCommentsSharedPreferences,
				cacheKey, annotationCommentObject);
		return annotationCommentObject;
	}

	private JSONObject getCommentsFromCache(String aid, int pageNumber)
			throws Exception {

		String key = getAnnotationCommentsCacheKey(aid, pageNumber);
		return mCacheManager.getJSONCache(mAnnotationCommentsSharedPreferences,
				key);
	}

	private String getAnnotationCommentsCacheKey(String aid, int pageNumber) {
		return aid + "comments" + "-" + Integer.toString(pageNumber);
	}

	private void setUpCommentsReplyDialog(int commentIndex) {

		// get the comment author name about to reply to
		String replyTarget = (String) mCommentsDataMapList.get(commentIndex)
				.get("author");

		mReplyDialog = new Dialog(getBaseContext(), R.style.dialog_reply);
		mReplyDialog.setTitle("Reply to " + replyTarget);

		//EditText replyText = (EditText) mReplyDialog.findViewById(R.id.annotation_comment_text);

		final ImageButton soundButton = (ImageButton) findViewById(R.id.comment_btn_sound);
		soundButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// if the device has no camera, disable this function
				PackageManager pm = getPackageManager();
				boolean hasMicrophone = pm
						.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
				if (!hasMicrophone) {
					soundButton.setEnabled(false);
				}
				// start recorder
				Intent toRecorder = new Intent(AnnotationCommentsActivity.this,
						RecorderActivity.class);
				startActivityForResult(toRecorder, REQUEST_RECORD_AUDIO);
			}
		});

	}

}
