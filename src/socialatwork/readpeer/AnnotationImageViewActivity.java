package socialatwork.readpeer;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

/*This class allows full screen view of annotation attached images */
public class AnnotationImageViewActivity extends Activity {

	private final String TAG = "annotation image view";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_annotation_image_view);
		Bundle extras = getIntent().getExtras();
		String imagePath = extras.getString("image path");
		Log.d(TAG, imagePath);
		ImageView imageDisplayView = (ImageView) findViewById(R.id.annotation_image);
		Drawable d = Drawable.createFromPath(imagePath);
		if (d == null) {
			Log.d(TAG, "drawable is null");
		}

		imageDisplayView.setImageDrawable(d);
		// User's clicking on the image will exit full screen view
		imageDisplayView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
