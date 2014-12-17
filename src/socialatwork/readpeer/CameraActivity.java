package socialatwork.readpeer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

public class CameraActivity extends FragmentActivity {

	private final String TAG = "Camera Activity";
	private final int PICTURE_QUALITY_INDEX = 40;
	private Camera mCamera;
	private View mProgressContainer;
	private SurfaceView mSurfaceView;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Hide status bar and other OS-level chrome
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_camera);

		mCamera = getCameraInstance();
		mProgressContainer = findViewById(R.id.camera_progressContainer);
		mProgressContainer.setVisibility(View.INVISIBLE);

		ImageButton takePictureButton = (ImageButton) findViewById(R.id.btn_camera_takePic);
		// if click take photo button, take picture and preview shall be closed
		takePictureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCamera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						mCamera.takePicture(mShutterCallback, null,
								mPictureCallback);
					}
				});
			}
		});
		mSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);
		SurfaceHolder mHolder = mSurfaceView.getHolder();
		// For device brefore 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mHolder.addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.d(TAG, "surface destroyed");
				if (mCamera != null) {
					mCamera.stopPreview();
					// ?????????????
					mCamera.release();
					mCamera = null;
				}
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "Creating surface");
				try {
					mCamera.setPreviewDisplay(holder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e(TAG, "Error setting up preview display", e);
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				Log.i(TAG, "Surface changed!");
				if (mCamera == null) {
					Log.d(TAG, "camera is null");
					return;
				}
				// the surface has changed size, update the camera preview size
				Camera.Parameters parameters = mCamera.getParameters();
				Size s = parameters.getPictureSize();
				parameters.setPreviewSize(s.width, s.height);
				s = getBiggestSupportedSize(
						parameters.getSupportedPictureSizes(), width, height);
				Log.i(TAG, "size:" + s.width + "height:" + s.height);
				parameters.setPictureSize(s.width, s.height);
				mCamera.setParameters(parameters);
				try {
					mCamera.startPreview();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Log.e(TAG, "Fail to start preview", e);
					mCamera.release();
					mCamera = null;
				}
			}
		});

	}

	// A trivial algorithm to find the most suitable size for surface view
	// Yet this method makes the size of bitmap big and resource-consuming while
	// loading it. Use it carefully.
	private Size getBiggestSupportedSize(List<Size> sizes, int width, int height) {
		Size bestSize = sizes.get(0);
		int largestArea = bestSize.width * bestSize.height;
		for (Size s : sizes) {
			int area = s.width * s.height;
			if (area > largestArea) {
				bestSize = s;
				largestArea = area;
			}
		}
		return bestSize;
	}

	// A trivial algorithm to find the smallest size supported by the camera
	private Size getSmallestSupportedSize(List<Size> sizes, int width,
			int height) {
		Size bestSize = sizes.get(0);
		int smallestArea = bestSize.width * bestSize.height;
		for (Size s : sizes) {
			int area = s.width * s.height;
			if (area < smallestArea) {
				bestSize = s;
				smallestArea = area;
			}
		}
		return bestSize;
	}

	private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {

		@Override
		public void onShutter() {
			// Display the progress indicator
			mProgressContainer.setVisibility(View.VISIBLE);
		}
	};

	private Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			finish();
			Log.e(TAG, "fail to open camera");
		}
		return c;
	}

	private PictureCallback mPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(final byte[] data, Camera camera) {

			final Handler messageHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what == 1) {
						Log.d(TAG, "Save done");
						finish();
					} else if (msg.what == -1) {
						Log.d(TAG, "Save unsuccessful");
						finish();
					}
				}
			};

			Runnable savePictureRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						boolean isSaveDone = savePicture(data);
						if (isSaveDone)
							messageHandler.sendEmptyMessage(1);
						else
							messageHandler.sendEmptyMessage(-1);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			new Thread(savePictureRunnable).start();
		}

		private boolean savePicture(byte[] data) {
			// Create a filename
			String filename = System.currentTimeMillis() + ".jpg";
			// Save the jpeg data to disk
			FileOutputStream os = null;
			String storagePath = Environment.getExternalStorageDirectory()
					.getPath();
			String folderPath = storagePath + "/Readpeer/Photos";
			String photoPath = folderPath + "/" + filename;
			File folder = new File(folderPath);

			if (!folder.exists()) {
				folder.mkdirs();
			}

			File photoFile = new File(folderPath, filename);
			try {
				os = new FileOutputStream(photoFile);
				os.write(data);
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "Error writing to or close file " + filename, e);
				setResult(Activity.RESULT_CANCELED);
				finish();
			}

			String dummyfilename = System.currentTimeMillis() + ".jpg";
			File f = new File(folderPath, dummyfilename);
			String dummyPhotoPath = folderPath + "/" + dummyfilename;
			try {
				FileOutputStream dummyOS = new FileOutputStream(f);
				BitmapFactory.Options options = new BitmapFactory.Options();
				// Low quality for efficient transmission
				options.inSampleSize = 1;
				options.inPreferQualityOverSpeed = false;
				options.inPreferredConfig = Bitmap.Config.RGB_565;
				Bitmap bm = BitmapFactory.decodeFile(photoPath, options);
				bm.compress(Bitmap.CompressFormat.JPEG, PICTURE_QUALITY_INDEX,
						dummyOS);
				dummyOS.flush();
				dummyOS.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				setResult(Activity.RESULT_CANCELED);
				finish();
			} catch (IOException e) {
				e.printStackTrace();
				setResult(Activity.RESULT_CANCELED);
				finish();
			}
			boolean isOriginalPhotoDeleted = photoFile.delete();

			if (isOriginalPhotoDeleted) {
				Log.i(TAG, "JPEG saved as" + dummyfilename);
				Intent i = new Intent();
				Log.i("debug", "the string path is:" + photoPath);
				i.putExtra("photoPath", dummyPhotoPath);
				setResult(Activity.RESULT_OK, i);
			} else {
				setResult(Activity.RESULT_CANCELED);
			}

			return isOriginalPhotoDeleted;
		}
	};

	@Override
	protected void onDestroy() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		super.onDestroy();
	}
}
