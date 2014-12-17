package socialatwork.readpeer;

import socialatwork.readpeer.Fragments.RecorderFragment;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;

public class RecorderActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Hide status bar and other OS-level chrome
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_recorder);
		FragmentManager fm = getSupportFragmentManager();
		Fragment recorderFragment = fm
				.findFragmentById(R.id.recorderFragmentContainer);

		if (recorderFragment == null) {
			recorderFragment = new RecorderFragment();
			fm.beginTransaction()
					.add(R.id.recorderFragmentContainer, recorderFragment).commit();
		}
	}
}
