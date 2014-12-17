package socialatwork.readpeer.Fragments;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import socialatwork.readpeer.AnnotationCommentPostActivity;
import socialatwork.readpeer.AnnotationCommentsActivity;
import socialatwork.readpeer.AnnotationImageViewActivity;
import socialatwork.readpeer.AnnotationVideoViewActivity;
import socialatwork.readpeer.ImageNameGenerator;
import socialatwork.readpeer.R;
import socialatwork.readpeer.tdCacheManager;
import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

public class AnnotationFragment extends Fragment {

	private View view;// Cache page view

	private static tdHttpClient mHttpClient;
	private String access_token;
	private String bookIndex;
	private String uid;
	private static JSONArray annotationInfo;
	private static final String TAG = "Annotation Fragment";

	/* Annotation related variables */
	private static int annotationCount = 0;
	private static String[] annotation_username;
	// private static String[] annotation_highlight;
	private int[] annotation_start_index;
	private int[] annotation_end_index;
	private String[] annotation_comment;
	private int[] annotation_like_num;
	private int[] annotation_comment_num;
	private String[] annotation_type;
	private int currentAnnotationSortedIndex = 0;
	private ArrayList<URL> annotation_user_image_url;
	// private String[] annotation_user_image_name;
	private String[] annotation_media_links;
	private String[] annotation_ids;
	private String[] annotation_page_numbers;
	private String[] annotation_time;
	private boolean[] isAnnotationLiked;
	// use this array to keep sorted index of annotations
	private int[] annotation_sorting_index;

	private Dialog sortAnnotationDialog;

	private final int SORTING_BY_TIME_EARLIEST_FIRST = 0;
	private final int SORTING_BY_TIME_LATEST_FIRST = 1;
	private final int SORTING_BY_COMMENTS_COUNT = 2;
	private final int SORTING_BY_LIKE_COUNT = 3;

	// Handler messages
	private final int PICTURE_DOWNLOAD_COMPLETED_MESSAGE = 0;
	private final int ANNOTATION_GET_DONE = 1;
	private final int JSON_GET_DONE = 2;

	private final String ANNOTATION_SHARED_PREFERENCE_NAME = "annotationInfo";

	private MediaPlayer mPlayer;
	private tdCacheManager mCacheManager;
	private SharedPreferences mAnnotationSharedPreferences;
	private ImageNameGenerator mGenerator;

	JSONObject mAnnotationJSONObject;

	OnAnnotationSelectedListener mListener;

	// newInstance constructor for creating fragment
	public static AnnotationFragment newInstance() {
		AnnotationFragment f = new AnnotationFragment();
		return f;
	}

	public interface OnAnnotationSelectedListener {
		public void onAnnotationSelected(int startIndex, int endIndex);
	}

	@Override
	public void onDestroyView() {
		if (mPlayer != null) {
			mPlayer.release();
		}
		super.onDestroyView();
	}

	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		try {
			mListener = (OnAnnotationSelectedListener) a;
		} catch (ClassCastException e) {
			throw new ClassCastException(a.toString()
					+ "must implement OnAnnotationSelectedListener");
		}
	}

	final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if (msg.what == ANNOTATION_GET_DONE) {
				// Present annotation information
				if (annotationCount > 0) {
					// Set up all the UI components of annotation display
					initializeAnnotationDisplayInterface();
					sortAnnotationInfo(SORTING_BY_TIME_LATEST_FIRST);
					currentAnnotationSortedIndex = 0;
					setAnnotation(annotation_sorting_index[currentAnnotationSortedIndex]);
				} else {
					Toast.makeText(getActivity(),
							"No Annotations in this book now.",
							Toast.LENGTH_SHORT).show();
				}
			}

			else if (msg.what == JSON_GET_DONE) {

			}

			else if (msg.what == PICTURE_DOWNLOAD_COMPLETED_MESSAGE) {

			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCacheManager = tdCacheManager.getCacheManagerInstance(getActivity());
		mAnnotationSharedPreferences = getActivity().getSharedPreferences(
				ANNOTATION_SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		mGenerator = ImageNameGenerator.getInstance();

		if (getArguments() != null) {
			bookIndex = getArguments().getString("bookIndex");
			access_token = getArguments().getString("access_token");
			uid = getArguments().getString("uid");
		} else {
			Log.i(TAG, "arguments is null");
		}
		try {
			getAnnotation(bookIndex);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (view == null) {
			view = inflater.inflate(R.layout.fragment_annotation, container,
					false);
		}
		// Remove parent if any
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			parent.removeView(view);
		}
		return view;
	}

	private void initializeAnnotationDisplayInterface() {

		Button prevAnnotationBtn = (Button) view
				.findViewById(R.id.btn_lastAnnotation);
		Button nextAnnotationBtn = (Button) view
				.findViewById(R.id.btn_nextAnnotation);

		prevAnnotationBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPlayer != null) {
					// Release media player if any
					mPlayer.release();
				}

				if (currentAnnotationSortedIndex == 0) {
					Toast.makeText(v.getContext(),
							"This is already the first annotation",
							Toast.LENGTH_SHORT).show();
				} else if (currentAnnotationSortedIndex >= 1) {
					currentAnnotationSortedIndex--;
					setAnnotation(annotation_sorting_index[currentAnnotationSortedIndex]);
					Log.d(TAG,
							"actual index:"
									+ annotation_sorting_index[currentAnnotationSortedIndex]);
				} else {
					Log.w(TAG, "annotation index is negative");
					setAnnotation(annotation_sorting_index[0]);
				}
			}
		});

		nextAnnotationBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (mPlayer != null) {
					// Release media player if any
					mPlayer.release();
				}
				// TODO Auto-generated method stub
				if (currentAnnotationSortedIndex == annotationCount - 1
						&& currentAnnotationSortedIndex >= 0) {
					Toast.makeText(v.getContext(),
							"This is already the last annotation",
							Toast.LENGTH_SHORT).show();
				} else if (currentAnnotationSortedIndex < annotationCount
						&& currentAnnotationSortedIndex >= 0) {
					currentAnnotationSortedIndex++;
					setAnnotation(annotation_sorting_index[currentAnnotationSortedIndex]);
					Log.d(TAG,
							"actual index:"
									+ annotation_sorting_index[currentAnnotationSortedIndex]);
				} else {
					Log.w(TAG,
							"annotation index is negative or bigger than annotation count");
					setAnnotation(annotation_sorting_index[0]);
				}
			}
		});

		final Button likeBtn = (Button) view.findViewById(R.id.btn_like);
		likeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				// Like function is only enabled when network is connected
				if (mCacheManager.isNetworkConnected()) {
					int currentAnnotationIndex = annotation_sorting_index[currentAnnotationSortedIndex];
					if (!isAnnotationLiked[currentAnnotationIndex]) {
						boolean isLikeSuccessful;
						try {
							isLikeSuccessful = mHttpClient.likeAnnotation(
									annotation_ids[currentAnnotationIndex],
									uid, access_token);

							if (isLikeSuccessful) {
								// update like counts, .....
								annotation_like_num[currentAnnotationIndex]++;
								String likeCount = Integer
										.toString(annotation_like_num[currentAnnotationIndex]);
								likeBtn.setText(likeCount);
								likeBtn.setBackgroundResource(R.drawable.icon_liked);
								isAnnotationLiked[currentAnnotationIndex] = true;
							} else {
								Toast.makeText(getActivity(), "Like failed :(",
										Toast.LENGTH_SHORT).show();
							}

						} catch (ParseException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

					else if (isAnnotationLiked[currentAnnotationIndex]) {
						// update like counts, .....
						annotation_like_num[currentAnnotationIndex]--;
						String likeCount = Integer
								.toString(annotation_like_num[currentAnnotationIndex]);
						likeBtn.setText(likeCount);
						likeBtn.setBackgroundResource(R.drawable.icon_like);
						isAnnotationLiked[currentAnnotationIndex] = false;
					}
				}

				else {
					Toast.makeText(getActivity(),
							"Cannot like without network:(", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

		Button sortAnnotationButton = (Button) view.findViewById(R.id.btn_sort);
		setAnnotationSortDialog();
		sortAnnotationButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sortAnnotationDialog.show();
			}
		});
	}

	// method to retrieve time integer data
	private int getDateNumberFromDateString(String date) {
		String dateString = date.split(",")[1];
		String[] dateSeperated = dateString.split("/");
		if (dateSeperated.length == 3) {
			int year = Integer.parseInt(dateSeperated[2]);
			int day = Integer.parseInt(dateSeperated[1]);
			int month = Integer.parseInt(dateSeperated[0]);
			int dateNumber = year * 10000 + month * 100 + day;
			return dateNumber;
		} else {
			return -1;
		}
	}

	// method to retrieve time string data
	private String getDateStringFromDateString(String date) {
		String dateString = date.split(",")[1];
		String[] dateSeperated = dateString.split("/");
		if (dateSeperated.length == 3) {
			String year = "20" + dateSeperated[2];
			String day = dateSeperated[1];
			String month = dateSeperated[0];
			return year + "-" + month + "-" + day;
		} else {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	private void setAnnotation(int annotationIndex) {

		// Log.i(TAG, "SETTING ANNOTATION:" + annotationIndex);
		TextView annotationCommentTV = (TextView) view
				.findViewById(R.id.annotation_comment);

		annotationCommentTV.setScroller(new Scroller(this.getActivity()));
		annotationCommentTV.setVerticalScrollBarEnabled(true);
		annotationCommentTV.setMovementMethod(new ScrollingMovementMethod());

		annotationCommentTV.setClickable(true);
		annotationCommentTV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d(TAG, "Click on annotation comment trigger comment post");
				Intent toPostComment = new Intent(getActivity(),
						AnnotationCommentPostActivity.class);
				toPostComment.putExtra("access_token", access_token);
				toPostComment.putExtra("uid", uid);

				int currentAnnotationIndex = annotation_sorting_index[currentAnnotationSortedIndex];
				String currentAnnotationID = annotation_ids[currentAnnotationIndex];
				toPostComment.putExtra("aid", currentAnnotationID);

				getActivity().startActivity(toPostComment);
			}
		});

		Button likeBtn = (Button) view.findViewById(R.id.btn_like);
		likeBtn.setText(Integer.toString(annotation_like_num[annotationIndex]));
		if (isAnnotationLiked[annotationIndex]) {
			likeBtn.setBackgroundResource(R.drawable.icon_liked);
		} else {
			likeBtn.setBackgroundResource(R.drawable.icon_like);
		}

		Button commentBtn = (Button) view.findViewById(R.id.btn_comments);
		commentBtn.setText(Integer
				.toString(annotation_comment_num[annotationIndex]));

		commentBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.d(TAG, "comment button clicked");
				int currentAnnotationIndex = annotation_sorting_index[currentAnnotationSortedIndex];
				String currentAnnotationID = annotation_ids[currentAnnotationIndex];
				// jump to annotation comment activity
				Intent toAnnotationCommentActivity = new Intent(getActivity(),
						AnnotationCommentsActivity.class);
				toAnnotationCommentActivity.putExtra("bid", bookIndex);
				toAnnotationCommentActivity
						.putExtra("aid", currentAnnotationID);
				startActivity(toAnnotationCommentActivity);
			}
		});

		String commentBody = annotation_comment[annotationIndex];
		String commentWithHtmlStyle = "<font color='grey'><b>"
				+ annotation_username[annotationIndex] + "</b>" + " at "
				+ annotation_time[annotationIndex] + "</font>" + "<br>"
				+ commentBody;
		String wholeComment = commentWithHtmlStyle;
		annotationCommentTV.setText(Html.fromHtml(wholeComment));

		TextView annotationCountTV = (TextView) view
				.findViewById(R.id.annotation_count);

		// Get the drawable image name
		String imageName = mGenerator.getNameFromLink(annotation_user_image_url
				.get(annotationIndex));
		// Get the image from cache
		Drawable annotation_creater_image = mCacheManager
				.getDrawableFromCache(imageName);
		ImageView annotationCreaterImage = (ImageView) view
				.findViewById(R.id.annotation_userImage);
		if (annotation_creater_image != null)
			annotationCreaterImage
					.setBackgroundDrawable(annotation_creater_image);

		String annotationType = annotation_type[annotationIndex];
		try {
			setMediaContentButton(annotationType, annotationIndex);
		} catch (Exception e) {
			e.printStackTrace();
		}

		int currentAnnotationPageNumber = currentAnnotationSortedIndex + 1;
		annotationCountTV.setText(currentAnnotationPageNumber + "/"
				+ annotationCount);
		int highlightedStartIndex = annotation_start_index[annotationIndex];
		int highlightedEndIndex = annotation_end_index[annotationIndex];
		mListener.onAnnotationSelected(highlightedStartIndex,
				highlightedEndIndex);
	}

	@SuppressWarnings("deprecation")
	private void setMediaContentButton(String annotationType,
			final int annotationIndex) throws Exception {
		final Button mediaContentBtn = (Button) view
				.findViewById(R.id.annotation_mediaContentButton);

		if (annotationType.compareTo("0") == 0) {
			Log.w(TAG, "plain text type");
			mediaContentBtn.setBackgroundDrawable(null);
		}

		else if (annotationType.compareTo("1") == 0) {
			Log.d(TAG, "type is picture");
			final URL imageURL = new URL(
					annotation_media_links[annotationIndex]);
			final String contentImageName = mGenerator
					.getNameFromLink(imageURL);

			final Handler h = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == -1) {
						Log.i(TAG, "annotation image download failed");
						mediaContentBtn
								.setBackgroundResource(R.drawable.icon_pic_not_found);
						mediaContentBtn.setClickable(false);
					}
					// if image download successfully, update image button
					else if (msg.what == 1) {
						updateButtonImage(mediaContentBtn, contentImageName);
					}
				}
			};

			class AnnotationImageDownloadRunnable implements Runnable {

				public Bitmap bmp;

				@Override
				public void run() {

					try {
						BitmapFactory.Options options = new BitmapFactory.Options();
						// Low quality for preview
						options.inSampleSize = 4;
						options.inPreferQualityOverSpeed = false;
						options.inPreferredConfig = Bitmap.Config.RGB_565;
						options.inPurgeable = true;
						bmp = BitmapFactory.decodeStream(imageURL
								.openConnection().getInputStream(), null,
								options);
						if (bmp != null) {
							mCacheManager.saveBitmapToCache(bmp,
									contentImageName);
							bmp.recycle();
							h.sendEmptyMessage(1);
						} else {
							h.sendEmptyMessage(-1);
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}// end of runnable class

			if (!mCacheManager.hasImageInCache(contentImageName)) {
				mediaContentBtn.setBackgroundResource(R.drawable.icon_loading);
				Log.d(TAG, "download image for content button");
				AnnotationImageDownloadRunnable myRunnable = new AnnotationImageDownloadRunnable();
				new Thread(myRunnable).start();
			}
			// if image exists in cache, just load it from SD card
			else {
				h.sendEmptyMessage(1);
			}
		}

		else if (annotationType.compareTo("2") == 0) {
			Log.w(TAG, "Type is video");
			mediaContentBtn.setBackgroundResource(R.drawable.icon_film);
			final Intent toVideoPlay = new Intent(this.getActivity(),
					AnnotationVideoViewActivity.class);
			toVideoPlay.putExtra("video link",
					annotation_media_links[annotationIndex]);
			mediaContentBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(toVideoPlay);
				}
			});
		}

		else if (annotationType.compareTo("3") == 0) {

			Log.d(TAG, "sound annotation");
			// Disable sound play when no network
			if (!mCacheManager.isNetworkConnected()) {
				mediaContentBtn
						.setBackgroundResource(R.drawable.btn_sound_lost_connection);
				mediaContentBtn.setClickable(false);
			}

			else {
				mediaContentBtn.setBackgroundResource(R.drawable.btn_play);
				mPlayer = new MediaPlayer();
				final String audioFileLink = annotation_media_links[annotationIndex];
				// Initialize the audio player with button and file link
				initializeSoundPlayer(mPlayer, mediaContentBtn, audioFileLink);

				mPlayer.setOnPreparedListener(new OnPreparedListener() {

					@Override
					public void onPrepared(MediaPlayer arg0) {
						mPlayer.start();
						mediaContentBtn
								.setBackgroundResource(R.drawable.btn_stop);
					}

				});

				mediaContentBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						if (mPlayer.isPlaying()) {
							mPlayer.stop();
							mediaContentBtn
									.setBackgroundResource(R.drawable.btn_play);
						}
						if (!mPlayer.isPlaying()) {
							// if player had been released, re-initialize it
							mediaContentBtn
									.setBackgroundResource(R.drawable.btn_loading);
							mPlayer.setLooping(false);
							try {
								mPlayer.prepare();
							} catch (IllegalStateException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		}

		else if (annotationType.compareTo("4") == 0) {
			Log.d(TAG, "link annotation");
			mediaContentBtn.setBackgroundResource(R.drawable.icon_link);
		}

		else {
			Log.d(TAG, "annotation type:" + annotationType);
		}
	}

	private boolean isFileExist(String fileAbsolutePath) {

		File targetFile = new File(fileAbsolutePath);
		return targetFile.exists();
	}

	@SuppressWarnings("deprecation")
	protected void updateButtonImage(Button imageButton, String imageName) {

		String imageFilePath = mCacheManager.getCachePath() + imageName;
		File imageFile = new File(imageFilePath);
		final Drawable d = Drawable.createFromPath(imageFile.getAbsolutePath());
		if (d != null) {
			imageButton.setBackgroundDrawable(d);
			final Intent toImageDisplay = new Intent(this.getActivity(),
					AnnotationImageViewActivity.class);
			toImageDisplay.putExtra("image path", imageFile.getAbsolutePath());

			imageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(toImageDisplay);
				}
			});
		}
		// when original image is removed, download fail etc, display a
		// default image
		else {
			Log.d(TAG, "target image is null");
			imageButton.setBackgroundResource(R.drawable.icon_pic_not_found);
		}
	}

	private void initializeSoundPlayer(final MediaPlayer player,
			final Button mediaButton, String mediaLink) throws Exception {

		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setDataSource(mediaLink);
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				player.stop();
				// Must do so on completion, otherwise take extra resource
				mediaButton.setBackgroundResource(R.drawable.btn_play);
			}
		});
	}

	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<</********* Annotation Related
	// Functions ************/<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	// <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

	private void setAnnotationSortDialog() {

		sortAnnotationDialog = new Dialog(getActivity());
		sortAnnotationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		sortAnnotationDialog.setContentView(R.layout.dialog_annotation_sort);
		sortAnnotationDialog.setCanceledOnTouchOutside(true);

		if (sortAnnotationDialog != null) {
			Button sortByTimeEarliestBtn = (Button) sortAnnotationDialog
					.findViewById(R.id.sort_bytime_earliest);
			sortByTimeEarliestBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sortAnnotationInfo(SORTING_BY_TIME_EARLIEST_FIRST);
					sortAnnotationDialog.dismiss();
				}
			});

			Button sortByTimeLatestBtn = (Button) sortAnnotationDialog
					.findViewById(R.id.sort_bytime_latest);
			sortByTimeLatestBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sortAnnotationInfo(SORTING_BY_TIME_LATEST_FIRST);
					sortAnnotationDialog.dismiss();
				}
			});

			Button sortByCommentsBtn = (Button) sortAnnotationDialog
					.findViewById(R.id.sort_by_comments);
			sortByCommentsBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sortAnnotationInfo(SORTING_BY_COMMENTS_COUNT);
					sortAnnotationDialog.dismiss();
				}
			});

			Button sortByLikesBtn = (Button) sortAnnotationDialog
					.findViewById(R.id.sort_by_likes);
			sortByLikesBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					sortAnnotationInfo(SORTING_BY_LIKE_COUNT);
					sortAnnotationDialog.dismiss();
				}
			});
		}
	}

	private void sortAnnotationInfo(int sortingType) {

		switch (sortingType) {
		case SORTING_BY_TIME_EARLIEST_FIRST:
			// it is default order so do nothing
			break;

		case SORTING_BY_TIME_LATEST_FIRST:
			// just reverse the order
			for (int i = 0; i < annotationCount; i++) {
				annotation_sorting_index[i] = annotationCount - i - 1;
			}
			break;

		case SORTING_BY_COMMENTS_COUNT:
			// bubble sort by comparing comment count
			Log.d(TAG, "sorting by comments count now");
			int SORTING_BY_COMMENTS_COUNT_FLAG = 1;
			for (int i = 0; i < annotationCount - 1
					&& SORTING_BY_COMMENTS_COUNT_FLAG == 1; i++) {
				SORTING_BY_COMMENTS_COUNT_FLAG = 0;
				for (int j = 0; j < annotationCount - 1 - i; j++) {
					if (annotation_comment_num[annotation_sorting_index[j]] < annotation_comment_num[annotation_sorting_index[j + 1]]) {
						Log.d(TAG, "should swap");
						int indexContainer = annotation_sorting_index[j];
						annotation_sorting_index[j] = annotation_sorting_index[j + 1];
						annotation_sorting_index[j + 1] = indexContainer;
						SORTING_BY_COMMENTS_COUNT_FLAG = 1;
					}
				}
			}
			break;

		case SORTING_BY_LIKE_COUNT:
			// bubble sort by comparing like count
			Log.d(TAG, "sorting by like count now");
			int SORTING_BY_LIKE_COUNT_FLAG = 1;
			for (int i = 0; i < annotationCount - 1
					&& SORTING_BY_LIKE_COUNT_FLAG == 1; i++) {
				SORTING_BY_LIKE_COUNT_FLAG = 0;
				for (int j = 0; j < annotationCount - 1 - i; j++) {
					if (annotation_like_num[annotation_sorting_index[j]] < annotation_like_num[annotation_sorting_index[j + 1]]) {
						Log.d(TAG, "should swap");
						int indexContainer = annotation_sorting_index[j];
						annotation_sorting_index[j] = annotation_sorting_index[j + 1];
						annotation_sorting_index[j + 1] = indexContainer;
						SORTING_BY_LIKE_COUNT_FLAG = 1;
					}
				}
			}
			break;
		default:
			break;
		}
		// After sorting, reset annotation fragment to new first position
		currentAnnotationSortedIndex = 0;
		setAnnotation(annotation_sorting_index[currentAnnotationSortedIndex]);
	}

	private void getAnnotationInfo(JSONObject annotationObject)
			throws Exception {
		// String code = annotationObject.getString("code");
		// String message = annotationObject.getString("message");

		if (annotationObject != null) {
			annotationCount = Integer.parseInt(annotationObject
					.getString("number_of_annotations"));

			if (annotationCount < 1) {
				Log.w(TAG, "Annotation count is less than 1");

			} else {
				annotationInfo = new JSONArray(
						annotationObject.getString("annotations"));

				// Log.i(TAG, "code:" + code);
				// Log.i(TAG, "message:" + message);
				// Log.i(TAG, "number:" + Integer.toString(annotationCount));
				// Log.i(TAG, "annotationInfo:" + annotationInfo);

				annotation_username = new String[annotationCount];
				// annotation_highlight = new String[annotationCount];
				annotation_comment = new String[annotationCount];
				annotation_like_num = new int[annotationCount];
				annotation_comment_num = new int[annotationCount];
				annotation_start_index = new int[annotationCount];
				annotation_end_index = new int[annotationCount];
				annotation_user_image_url = new ArrayList<URL>();
				// annotation_user_image_name = new String[annotationCount];
				annotation_media_links = new String[annotationCount];
				annotation_sorting_index = new int[annotationCount];
				annotation_type = new String[annotationCount];
				annotation_ids = new String[annotationCount];
				annotation_page_numbers = new String[annotationCount];
				annotation_time = new String[annotationCount];
				isAnnotationLiked = new boolean[annotationCount];

				for (int i = 0; i < annotationCount; i++) {
					JSONObject singleAnnotationObject = (JSONObject) annotationInfo
							.get(i);

					annotation_ids[i] = (String) singleAnnotationObject
							.get("aid");
					annotation_page_numbers[i] = (String) singleAnnotationObject
							.get("page");
					Log.d(TAG, "aid:" + annotation_ids[i] + " pid:"
							+ annotation_page_numbers[i]);
					annotation_time[i] = getDateStringFromDateString((String) singleAnnotationObject
							.get("changed"));
					Log.d(TAG, "annotation time:" + annotation_time[i]);
					isAnnotationLiked[i] = false;

					JSONObject commentObject = (JSONObject) singleAnnotationObject
							.get("comment");
					// Log.i(TAG, commentObject.toString());
					annotation_type[i] = (String) singleAnnotationObject
							.get("annotation_category");
					Log.d(TAG, "annotation category:" + annotation_type[i]);

					annotation_username[i] = commentObject.getString("name");
					// annotation_user_image_name[i] = annotation_username[i]+
					// ".jpg";
					annotation_start_index[i] = Integer
							.parseInt(singleAnnotationObject.get("start")
									.toString());
					annotation_end_index[i] = Integer
							.parseInt(singleAnnotationObject.get("end")
									.toString());
					// Log.i(TAG,"start:"+
					// singleAnnotationObject.get("start").toString());
					annotation_comment[i] = commentObject.getString("subject");
					annotation_like_num[i] = Integer
							.parseInt(singleAnnotationObject.get("like_count")
									.toString());

					if ((Integer) singleAnnotationObject.get("liked") == 1) {
						isAnnotationLiked[i] = true;
					} else if ((Integer) singleAnnotationObject.get("liked") == 0) {
						isAnnotationLiked[i] = false;
					}

					annotation_comment_num[i] = Integer
							.parseInt(singleAnnotationObject
									.getString("comment_count")) - 1;
					annotation_user_image_url.add(new URL(commentObject
							.getString("picture")));
					annotation_media_links[i] = commentObject.getString("link");
					// Initially keep sorting index as the way it is
					annotation_sorting_index[i] = i;

				}
				//sep22
				//mCacheManager.downloadUncachedImages(annotation_user_image_url,null);
				handler.sendEmptyMessage(ANNOTATION_GET_DONE);
			}
		} else {
			annotationCount = 0;
		}
	}

	private void getAnnotation(String bookID) throws Exception {

		String key = bookID + "-annotation";

		if (mCacheManager.shouldDownloadJSONFromServer(
				mAnnotationSharedPreferences, key)) {
			getAnnotationFromServer(bookID);
		}

		else {

			mAnnotationJSONObject = mCacheManager.getJSONCache(
					mAnnotationSharedPreferences, key);
			getAnnotationInfo(mAnnotationJSONObject);
		}
	}

	final Handler getAnnotationHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if (msg.what == ANNOTATION_GET_DONE) {
				try {
					getAnnotationInfo(mAnnotationJSONObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	private void getAnnotationFromServer(final String bookID)
			throws JSONException, Exception, Exception {

		Log.d(TAG, "Getting annotations from server");
		mHttpClient = tdHttpClient.getClientInstance();
		final String key = bookID + "-annotation";

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mAnnotationJSONObject = new JSONObject(
							mHttpClient.getAnnotation(bookID, access_token));
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				mCacheManager.setJSONCache(mAnnotationSharedPreferences, key,
						mAnnotationJSONObject);
				getAnnotationHandler.sendEmptyMessage(ANNOTATION_GET_DONE);
			}
		}).start();
	}
}