package socialatwork.readpeer.Fragments;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import socialatwork.readpeer.R;
import socialatwork.readpeer.R.id;
import socialatwork.readpeer.R.layout;
import socialatwork.readpeer.R.string;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class RecorderFragment extends Fragment {

	private Button btn_record;
	private MediaRecorder mMediaRecorder = null;
	// private List<String> recList = new ArrayList<String>();
	private File recordsFolder = null;
	private File tempFile = null;
	private String temp = "rec_"; // Prefix for temporary files
	private Boolean isRecording = false;
	private String fileName;
	private String filePath;
	private String recFolderPath;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_recorder, container, false);
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String rootPath = Environment.getExternalStorageDirectory()
					.getPath();
			// Create folder for readpeer application if not exist
			String applicationFolderPath = rootPath + "/Readpeer";
			File applicationFolder = new File(applicationFolderPath);
			if (!applicationFolder.exists()) {
				applicationFolder.mkdir();
			}
			// Create records folder if not exist
			recFolderPath = applicationFolderPath + "/Records";
			recordsFolder = new File(recFolderPath);
			if (!recordsFolder.exists()) {
				recordsFolder.mkdir();
			}

		} else {
			Toast.makeText(getActivity(), "Please insert SD card",
					Toast.LENGTH_SHORT).show();
		}

		btn_record = (Button) v.findViewById(R.id.btn_record);
		btn_record.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!isRecording) {
					btn_record.setText("Stop Recording");
					isRecording = true;
					try {
						tempFile = File.createTempFile(temp, ".m4a",
								recordsFolder);
						fileName = tempFile.getName();
						Log.i("debug", fileName);
						filePath = recFolderPath + "/" + fileName;
						mMediaRecorder = new MediaRecorder();

						mMediaRecorder
								.setAudioSource(MediaRecorder.AudioSource.MIC);
						mMediaRecorder
								.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
						mMediaRecorder
								.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
						mMediaRecorder.setAudioEncodingBitRate(16);
						mMediaRecorder.setAudioSamplingRate(44100);
						mMediaRecorder.setOutputFile(tempFile.getAbsolutePath());
						mMediaRecorder.prepare();
						mMediaRecorder.start();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (isRecording) {
					btn_record.setText(R.string.btn_record);
					mMediaRecorder.stop();
					mMediaRecorder.release();
					mMediaRecorder = null;

					Intent i = new Intent();
					i.putExtra("audioPath", filePath);
					getActivity().setResult(Activity.RESULT_OK, i);
					getActivity().finish();
				}
			}
		});
		return v;
	}

	/**
	 * Play record files
	 * 
	 * @param file
	 */
	public void PlayRecord(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(file), "audio");
		this.startActivity(intent);
	}

	class MusicFilter implements FilenameFilter {

		public boolean accept(File dir, String filename) {
			// TODO Auto-generated method stub
			return (filename.endsWith(".m4a"));
		}

	}

}
