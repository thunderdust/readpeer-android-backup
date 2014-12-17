package socialatwork.readpeer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

public class AnnotationCommentPostActivity extends FragmentActivity {

	private final int REQUEST_TAKE_PHOTO = 1;
	private final int REQUEST_RECORD_AUDIO = 2;
	private final int REQUEST_GALLERY_PHOTO = 3;
	private final int REQUEST_TAKE_VIDEO = 4;
	private final int REQUEST_GALLERY_VIDEO = 5;
	private final int SELECTED_TEXT_MAX_DISPLAY_NUM = 50;
	private final int SUBMIT_SUCCESSFUL = 1;
	private final int SUBMIT_FAIL = 0;

	private final int PREVIEW_AUDIO = 1;
	private final int PREVIEW_PHOTO = 2;
	private final int PREVIEW_VIDEO = 3;
	private final int PREVIEW_LINK = 4;
	
	private File mediaFile = null;
	// media files are attachements in annotations
	private Bitmap mediaBitmap = null;
	private MediaPlayer mPlayer;
	private String mLink = null;
	private static final String TAG = "ANNOTATION COMMENT POST ACTIVITY";

	private static tdHttpClient mHttpClient;
	private String annotationID;
	private String uid;
	private String pid = "0"; // comment page id
	private String subject = "";
	private String access_token;
	private boolean hasFileAttached = false;
	private String mediaType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Hide status bar and other OS-level chrome
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// Make keyboard resize to avoid overlap on layouts
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		setContentView(R.layout.activity_annotation_comments_post);

		Bundle extra = getIntent().getExtras();

		access_token = extra.getString("access_token");
		Log.d(TAG, "token:" + access_token);
		annotationID = extra.getString("aid");
		Log.d(TAG, "annotation id:" + annotationID);
		uid = extra.getString("uid");

		final ImageButton imgButton = (ImageButton) findViewById(R.id.comment_btn_image);
		imgButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// if the device has no camera, disable this function
				Dialog optionsDialog = new Dialog(getWindow().getDecorView()
						.getContext());
				optionsDialog
						.setContentView(R.layout.image_upload_option_dialog);
				optionsDialog.setTitle("Attach image");
				optionsDialog.setCanceledOnTouchOutside(true);
				initializeDialogItems(optionsDialog);
				optionsDialog.show();
			}

			private void initializeDialogItems(final Dialog optionsDialog) {

				final TextView tv_from_album = (TextView) optionsDialog
						.findViewById(R.id.text_from_album);
				tv_from_album.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent e) {
						if (e.getAction() == MotionEvent.ACTION_DOWN) {
							tv_from_album.setBackgroundColor(0xFFD3D3D3);
							return true;
						} else if (e.getAction() == MotionEvent.ACTION_UP) {
							tv_from_album.setBackgroundColor(0xFF32CD32);
							optionsDialog.dismiss();
							Intent openGalleryIntent = new Intent();
							openGalleryIntent
									.setAction(Intent.ACTION_GET_CONTENT);
							openGalleryIntent.setType("image/*");
							startActivityForResult(openGalleryIntent,
									REQUEST_GALLERY_PHOTO);
							return true;
						}
						return false;
					}
				});

				final TextView tv_take_picture = (TextView) optionsDialog
						.findViewById(R.id.text_take_picture);
				// Disable click if device has no camera
				PackageManager pm = getPackageManager();
				boolean hasCamera = pm
						.hasSystemFeature(PackageManager.FEATURE_CAMERA)
						|| pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
						|| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Camera
								.getNumberOfCameras() > 0);
				if (!hasCamera) {
					tv_take_picture.setClickable(false);
				} else {
					tv_take_picture.setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent e) {
							if (e.getAction() == MotionEvent.ACTION_DOWN) {
								tv_take_picture.setBackgroundColor(0xFFD3D3D3);
								return true;
							} else if (e.getAction() == MotionEvent.ACTION_UP) {
								tv_take_picture.setBackgroundColor(0xFF32CD32);
								optionsDialog.dismiss();
								Intent toCamera = new Intent(
										AnnotationCommentPostActivity.this,
										CameraActivity.class);
								startActivityForResult(toCamera,
										REQUEST_TAKE_PHOTO);
								return true;
							}
							return false;
						}
					});
				}
			}
		});

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
				Intent toRecorder = new Intent(
						AnnotationCommentPostActivity.this,
						RecorderActivity.class);
				startActivityForResult(toRecorder, REQUEST_RECORD_AUDIO);
			}
		});

		/* Attach Link and video are currently deprecated. */

		ImageButton linkButton = (ImageButton) findViewById(R.id.comment_btn_link);
		linkButton.setVisibility(View.INVISIBLE);

		// Button confirmBtn = (Button) findViewById(R.id.btn_link_confirm);
		// final EditText linkET = (EditText) findViewById(R.id.text_link);
		// confirmBtn.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// mLink = linkET.getText().toString();
		// // updatePreviews(PREVIEW_LINK,null);
		// }
		// });

		linkButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Dialog attachLinkDialog = new Dialog(getWindow().getDecorView()
						.getContext());
				attachLinkDialog.setContentView(R.layout.link_attach_dialog);
				attachLinkDialog.setTitle("Attach links");
				attachLinkDialog.setCanceledOnTouchOutside(true);
				attachLinkDialog.show();
			}
		});

		final ImageButton videoButton = (ImageButton) findViewById(R.id.comment_btn_video);
		videoButton.setVisibility(View.INVISIBLE);
		videoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// if the device has no camera, disable this function
				Dialog videoOptionsDialog = new Dialog(getWindow()
						.getDecorView().getContext());
				videoOptionsDialog
						.setContentView(R.layout.video_upload_option_dialog);
				videoOptionsDialog.setTitle("Attach video");
				videoOptionsDialog.setCanceledOnTouchOutside(true);
				initializeDialogItems(videoOptionsDialog);
				videoOptionsDialog.show();
			}

			private void initializeDialogItems(final Dialog optionsDialog) {

				final TextView tv_from_gallery = (TextView) optionsDialog
						.findViewById(R.id.text_from_gallery);
				tv_from_gallery.setOnTouchListener(new OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent e) {
						if (e.getAction() == MotionEvent.ACTION_DOWN) {
							tv_from_gallery.setBackgroundColor(0xFFD3D3D3);
							return true;
						} else if (e.getAction() == MotionEvent.ACTION_UP) {
							tv_from_gallery.setBackgroundColor(0xFF32CD32);
							optionsDialog.dismiss();
							Intent openGalleryIntent = new Intent();
							openGalleryIntent
									.setAction(Intent.ACTION_GET_CONTENT);
							openGalleryIntent.setType("video/*");
							startActivityForResult(openGalleryIntent,
									REQUEST_GALLERY_VIDEO);
							return true;
						}
						return false;
					}
				});

				final TextView tv_record_video = (TextView) optionsDialog
						.findViewById(R.id.text_record_video);
				// Disable click if device has no camera
				PackageManager pm = getPackageManager();
				boolean hasCamera = pm
						.hasSystemFeature(PackageManager.FEATURE_CAMERA)
						|| pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
						|| (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Camera
								.getNumberOfCameras() > 0);
				if (!hasCamera) {
					tv_record_video.setClickable(false);
					tv_record_video.setBackgroundColor(0xFFBEBEBE);
				} else {
					tv_record_video.setOnTouchListener(new OnTouchListener() {

						@Override
						public boolean onTouch(View v, MotionEvent e) {
							if (e.getAction() == MotionEvent.ACTION_DOWN) {
								tv_record_video.setBackgroundColor(0xFFD3D3D3);
								return true;
							} else if (e.getAction() == MotionEvent.ACTION_UP) {
								tv_record_video.setBackgroundColor(0xFF32CD32);
								optionsDialog.dismiss();
								Intent takeVideoIntent = new Intent(
										MediaStore.ACTION_VIDEO_CAPTURE);
								if (takeVideoIntent
										.resolveActivity(getPackageManager()) != null) {
									startActivityForResult(takeVideoIntent,
											REQUEST_TAKE_VIDEO);
								}
								return true;
							}
							return false;
						}
					});
				}
			}
		});

		// <------------------ deprecated -------------------------->

		Button submitBtn = (Button) findViewById(R.id.comment_btn_submit);
		submitBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							submitAnnotation();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		});
		Button cancelBtn = (Button) findViewById(R.id.comment_btn_cancel);
		cancelBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		EditText commentET = (EditText) findViewById(R.id.annotation_comment_text);
		commentET.requestFocus();
	}

	final Handler submitHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == SUBMIT_SUCCESSFUL) {
				onSubmit(true);
			} else if (msg.what == SUBMIT_FAIL) {
				onSubmit(false);
			}
		}
	};

	protected void submitAnnotation() throws Exception {

		mHttpClient = tdHttpClient.getClientInstance();
		EditText commentET = (EditText) findViewById(R.id.annotation_comment_text);
		// Check if input in comment text area is empty
		String comment = commentET.getText().toString();
		Pattern p = Pattern.compile("\\s*|\t|\r|\n");
		Matcher m = p.matcher(comment);
		String commentEmptyTester = m.replaceAll("");
		Log.i(TAG, "tester size:" + commentEmptyTester.length());
		boolean isSubmissionSuccessful = false;

		if (!commentEmptyTester.isEmpty()) {
			Log.i(TAG, "comment is:" + comment);
			Log.i(TAG, "comment length:" + comment.length());
			// Pure text annotation
			if (!hasFileAttached) {
				Log.i(TAG, "pure text annotation");
				isSubmissionSuccessful = mHttpClient.postCommentWithoutFile(
						annotationID, access_token, comment, pid, subject);
			} else {
				if (mediaType != null) {
					Log.i(TAG, "Ready to upload files");
					if (mediaFile != null) {
						File mediaFileToBeUpload = mediaFile;

						String returnedInformation = mHttpClient.uploadFile(
								access_token, uid, mediaType,
								mediaFileToBeUpload);
						Log.i(TAG, "returnedInformation:" + returnedInformation);
						JSONObject returnedInfo = new JSONObject(
								returnedInformation);
						JSONObject fileInfo = (JSONObject) returnedInfo
								.get("media");
						String fid = fileInfo.getString("fid");
						isSubmissionSuccessful = mHttpClient
								.postCommentWithFile(access_token,
										annotationID, subject, comment, fid,
										pid);
					} else {
						isSubmissionSuccessful = false;
					}
				}
			}
		} else {
			Log.i(TAG, "comment is null");

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(getBaseContext(),
							"Comment cannot be empty:(", Toast.LENGTH_SHORT)
							.show();
				}
			});
			isSubmissionSuccessful = false;
		}
		if (isSubmissionSuccessful) {
			submitHandler.sendEmptyMessage(SUBMIT_SUCCESSFUL);
		} else {
			submitHandler.sendEmptyMessage(SUBMIT_FAIL);
		}
	}

	@TargetApi(19)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "been in on activityresult");
		if (resultCode != Activity.RESULT_OK) {
			Log.w(TAG, "result is not OK !");
			return;
		}

		switch (requestCode) {

		case REQUEST_TAKE_PHOTO: {
			// Create a new photo object;
			hasFileAttached = true;
			mediaType = "image";
			String photoPath = data.getExtras().getString("photoPath");
			if (photoPath != null) {
				mediaFile = new File(photoPath);
				Log.i("log", "filename:" + photoPath);
				updatePreviews(PREVIEW_PHOTO, photoPath);
			} else {
				Log.i("debug", "the photo path is null");
			}
			break;
		}

		case REQUEST_RECORD_AUDIO: {
			hasFileAttached = true;
			mediaType = "audio";
			String audioPath = data.getExtras().getString("audioPath");
			if (audioPath != null) {
				mediaFile = new File(audioPath);
				Log.i("log", "audio filename:" + audioPath);
				updatePreviews(PREVIEW_AUDIO, audioPath);
			} else {
				Log.i("debug", "the audio path is null");
			}
			break;
		}

		case REQUEST_GALLERY_PHOTO: {
			hasFileAttached = true;
			mediaType = "image";
			final Uri uri = data.getData();
			String path = getRealPathFromURI(this, uri);
			Log.d(TAG, "gallery photo path:" + path);

			// Create file according to uri, for the use of annotation uploading
			final Handler bitmapHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == 1) {
						Log.d(TAG, "Bitmap handling done");
						updatePreviews(PREVIEW_PHOTO, "GALLERY PHOTO");
					} else if (msg.what == -1) {
						Log.d(TAG, "Bitmap handling unsuccessful");
					}
				}
			};

			Runnable bitmapHandleRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						mediaBitmap = handleBitmap(uri);
						if (mediaBitmap != null)
							bitmapHandler.sendEmptyMessage(1);
						else
							bitmapHandler.sendEmptyMessage(-1);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			new Thread(bitmapHandleRunnable).start();
			break;
		}

		case REQUEST_GALLERY_VIDEO: {
			hasFileAttached = true;
			mediaType = "video";
			Uri videoFromGalleryUri = data.getData();
			String videoFromGalleryPath = getRealPathFromURI(this,
					videoFromGalleryUri);
			mediaFile = new File(videoFromGalleryPath);
			updatePreviews(PREVIEW_VIDEO, videoFromGalleryPath);
			break;
		}

		case REQUEST_TAKE_VIDEO: {
			hasFileAttached = true;
			mediaType = "video";
			Uri videoUri = data.getData();
			String path = getRealPathFromURI(this, videoUri);
			updatePreviews(PREVIEW_VIDEO, path);
			break;
		}

		default:
			break;
		}
	}

	private String getRealPathFromURI(Context context, Uri contentUri) {
		Cursor cursor = null;
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			cursor = context.getContentResolver().query(contentUri, proj, null,
					null, null);
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private Bitmap handleBitmap(Uri uri) {

		Bitmap bitmap = null;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1;
			options.inPreferQualityOverSpeed = false;
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			InputStream is = getContentResolver().openInputStream(uri);
			bitmap = BitmapFactory.decodeStream(is, null, options);
			if (bitmap != null) {
				Log.d(TAG, "bitmap is not null");
			}

			String filename = System.currentTimeMillis() + ".jpg";
			String folderPath = Environment.getExternalStorageDirectory()
					.getPath() + "/Readpeer/Temp";
			File folder = new File(folderPath);
			if (!folder.exists()) {
				folder.mkdirs();
			}
			File tempFile = new File(folderPath, filename);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bitmap.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
			byte[] bitmapdata = bos.toByteArray();
			// write the bytes in file
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(bitmapdata);
			fos.flush();
			fos.close();
			mediaFile = tempFile;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	private void updatePreviews(int previewCase, final String resourceReference) {

		ImageButton audioButton = (ImageButton) findViewById(R.id.comment_btn_play);
		ImageView photoPreview = (ImageView) findViewById(R.id.comment_photoView);
		ImageView previewBtnView = (ImageView) findViewById(R.id.comment_image_play_preview);

		switch (previewCase) {
		case PREVIEW_AUDIO: {
			Log.d(TAG, "audio preview");
			audioButton.setVisibility(View.VISIBLE);
			photoPreview.setVisibility(View.INVISIBLE);
			previewBtnView.setVisibility(View.INVISIBLE);

			audioButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mPlayer = new MediaPlayer();
					try {
						mPlayer.setDataSource(resourceReference);
						mPlayer.prepare();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					mPlayer.start();
				}
			});
			break;
		}

		case PREVIEW_PHOTO: {
			Log.d(TAG, "photo preview");
			photoPreview.setVisibility(View.VISIBLE);
			// Make sure audio preview is hidden
			audioButton.setVisibility(View.INVISIBLE);
			// Make sure video preview image is hidden
			previewBtnView.setVisibility(View.INVISIBLE);

			// Photo from camera and hence have a absolute path
			if (resourceReference != "GALLERY PHOTO") {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				options.inPreferQualityOverSpeed = false;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				Bitmap bitmap = BitmapFactory.decodeFile(resourceReference,
						options);
				if (bitmap != null) {
					photoPreview.setImageBitmap(bitmap);
				} else {
					Log.i("debug", "bitmap is null");
				}
			}
			// Photo from gallery and hence update through <mediaBitmap>
			else {
				try {
					photoPreview.setImageBitmap(mediaBitmap);
				} catch (NullPointerException e) {
					Log.e(TAG, "preview bitmap is null!");
					e.printStackTrace();
				}
			}
			break;
		}

		case PREVIEW_VIDEO:
			Log.d(TAG, "video preview");
			// Make sure audio preview is hidden
			audioButton.setVisibility(View.INVISIBLE);
			// Display the "play button" mark upon video thumbnail
			previewBtnView.setVisibility(View.VISIBLE);
			photoPreview.setVisibility(View.VISIBLE);
			// Capture a thumbnail bitmap as preview
			Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(
					resourceReference, MediaStore.Video.Thumbnails.MINI_KIND);
			photoPreview.setImageBitmap(thumbnail);
			break;

		case PREVIEW_LINK:
			Log.d(TAG, "link preview");
			break;
		}
	}

	// Ready for submit the annotation. Must do all checking, and release all
	// resources.
	public void onSubmit(boolean submissionStatus) {
		if (submissionStatus) {
			Toast.makeText(this, "Comment post successfully ",
					Toast.LENGTH_SHORT).show();
			finish();
		} else {
			Toast.makeText(this, "Comment post failed", Toast.LENGTH_SHORT)
					.show();
		}
	}
}