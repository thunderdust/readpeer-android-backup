package socialatwork.readpeer;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {

	private final String TAG = "CameraPreview Class";
	private SurfaceHolder mHolder;
	private Camera mCamera;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);
		// mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mHolder.getSurface() == null) {
			return;
		}
		

		// the surface has changed size, update the camera preview size
		Camera.Parameters parameters = mCamera.getParameters();
		Size s = parameters.getPictureSize();
		parameters.setPreviewSize(s.width, s.height);
		s = getSmallestSupportedSize(parameters.getSupportedPictureSizes(),
				width, height);
		Log.i(TAG, "size:" + s.width + "height:" + s.height);
		parameters.setPictureSize(s.width, s.height);
		mCamera.setParameters(parameters);
        
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.e(TAG, "Fail to start preview", e);
			mCamera.release();
			mCamera = null;
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.e(TAG, "Preview fails");
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
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

}
