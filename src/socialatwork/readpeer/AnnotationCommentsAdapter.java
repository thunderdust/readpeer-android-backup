package socialatwork.readpeer;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import socialatwork.readpeer.Cache.tdCacheManager;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/* this class implements an adapter for list view of annotation comments.*/
public class AnnotationCommentsAdapter extends BaseAdapter {

	private List<Map<String, Object>> list;
	private LayoutInflater inflater;
	private final String TAG = "Annotation comments adapter";
	private Context context;
	private final int COMMENT_TYPE_PLAIN_TEXT = 0;
	private final int COMMENT_TYPE_IMAGE = 1;
	private final int COMMENT_TYPE_VIDEO = 2;
	private final int COMMENT_TYPE_AUDIO = 3;
	private final int COMMENT_TYPE_LINK = 4;
	private tdCacheManager mCacheManager;
	private ImageNameGenerator mINGenerator;
	private MediaPlayer mPlayer;

	public AnnotationCommentsAdapter(List<Map<String, Object>> list,
			Context context) {
		this.list = list;
		this.context = context;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mCacheManager = tdCacheManager.getCacheManagerInstance(context);
		mINGenerator = ImageNameGenerator.getInstance();
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Log.d(TAG, "getting view");
		Map<String, Object> dataItem = list.get(position);
		if (dataItem == null) {
			Log.e(TAG, "data item is null");
		}
		View cv = convertView;
		if (cv == null) {
			cv = inflater
					.inflate(R.layout.annotation_comment_item, null, false);
		}
		// load comment creator avatar
		ImageView commentAuthorAvatartIV = (ImageView) cv
				.findViewById(R.id.annotation_comment_userImage);
		commentAuthorAvatartIV.setBackgroundDrawable((Drawable) dataItem
				.get("avatar"));
		// load comment content
		TextView commentContentTV = (TextView) cv
				.findViewById(R.id.annotation_comment_content);
		int commentType = (Integer) dataItem.get("type");
		commentContentTV.setText((String) dataItem.get("comment"));
		// load media image button if necessary

		loadMediaButton(position, commentType, cv);
		cv.setClickable(true);
		cv.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "Click on comment trigger comment reply");
				Intent toPostComment = new Intent(context,
						AnnotationCommentPostActivity.class);
				toPostComment.putExtra("type", "reply");
				context.startActivity(toPostComment);
			}
		});
		return cv;
	}

	@SuppressWarnings("deprecation")
	private void loadMediaButton(final int position, int commentType,
			View convertView) {

		switch (commentType) {

		case COMMENT_TYPE_PLAIN_TEXT:
			break;

		case COMMENT_TYPE_IMAGE:
			Button imageButton = (Button) convertView
					.findViewById(R.id.annotation_comment_mediaContentButton);
			// Use URL to generate image name, look it up in cache and load it
			// as imageButton background
			URL previewImageURL = (URL) list.get(position).get("preview");
			String previewImageName = mINGenerator

			.getNameFromLink(previewImageURL);

			imageButton.setBackgroundDrawable((Drawable) mCacheManager
					.getDrawableFromCache(previewImageName));
			imageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String pictureName = mINGenerator
							.getNameFromLink((URL) list.get(position).get(
									"link"));
					String cachePath = mCacheManager.getCachePath();
					String picturePath = cachePath + pictureName;
					Intent toImageView = new Intent(context,
							AnnotationImageViewActivity.class);
					toImageView.putExtra("image path", picturePath);
					context.startActivity(toImageView);
				}
			});
			break;

		case COMMENT_TYPE_VIDEO:

			Button videoButton = (Button) convertView
					.findViewById(R.id.annotation_comment_mediaContentButton);
			videoButton.setBackgroundDrawable(context.getResources()
					.getDrawable(R.drawable.icon_video));
			String videoLink = (String) list.get(position).get("link");
			Intent toVideoView = new Intent(context,
					AnnotationVideoViewActivity.class);
			toVideoView.putExtra("video link", videoLink);
			context.startActivity(toVideoView);
			break;

		case COMMENT_TYPE_AUDIO:

			String audioLink = (String) list.get(position).get("link");
			try {
				setAudioButton(convertView, audioLink);
			} catch (Exception e) {
				e.printStackTrace();
			}

			break;

		case COMMENT_TYPE_LINK:

			Button linkButton = (Button) convertView
					.findViewById(R.id.annotation_comment_mediaContentButton);
			linkButton.setBackgroundDrawable(context.getResources()
					.getDrawable(R.drawable.icon_link));
			break;

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

	@SuppressWarnings("deprecation")
	private void setAudioButton(View convertView, String audioLink)
			throws Exception {

		final Button audioButton = (Button) convertView
				.findViewById(R.id.annotation_comment_mediaContentButton);
		audioButton.setBackgroundDrawable(context.getResources().getDrawable(
				R.drawable.icon_audio));

		// TODO Auto-generated method stub
		if (!mCacheManager.isNetworkConnected()) {
			audioButton
					.setBackgroundResource(R.drawable.btn_sound_lost_connection);
			audioButton.setClickable(false);
		}

		else {
			audioButton.setBackgroundResource(R.drawable.btn_play);
			mPlayer = new MediaPlayer();
			// Initialize the audio player with button and file link
			initializeSoundPlayer(mPlayer, audioButton, audioLink);

			mPlayer.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer arg0) {
					mPlayer.start();
					audioButton.setBackgroundResource(R.drawable.btn_stop);
				}

			});

			audioButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					if (mPlayer.isPlaying()) {
						mPlayer.stop();
						audioButton.setBackgroundResource(R.drawable.btn_play);
					}
					if (!mPlayer.isPlaying()) {
						// if player had been released, re-initialize it
						audioButton
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
}
