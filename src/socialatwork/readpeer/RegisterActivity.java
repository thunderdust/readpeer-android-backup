package socialatwork.readpeer;

import socialatwork.readpeer.WebRelatedComponents.tdHttpClient;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {

	private String username, password, email = null;
	private tdHttpClient mHttpClient;
	private static final String TAG = "register";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_register);
		Button submitBtn = (Button) findViewById(R.id.btn_submit);
		final EditText userNameText = (EditText) findViewById(R.id.register_name);
		final EditText passwordText = (EditText) findViewById(R.id.register_password);
		final EditText emailText = (EditText) findViewById(R.id.register_email);

		submitBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				username = userNameText.getText().toString();
				password = passwordText.getText().toString();
				email = emailText.getText().toString();
				if (username == null || password == null || email == null) {
					Toast.makeText(getApplicationContext(),
							"Please fill in all required fields",
							Toast.LENGTH_SHORT).show();
				} else if (password.length() < 6) {
					Toast.makeText(
							getApplicationContext(),
							"Your password is too short, at least 6 digits or words",
							Toast.LENGTH_LONG).show();
				} else {
					mHttpClient = tdHttpClient.getClientInstance();
					attemptSignUp();

				}
			}
		});
	}

	public void attemptSignUp() {

		Log.v(TAG, "Trying to register");
		mHttpClient.clearCookies();
		boolean isSuccessful = false;
		try {
			isSuccessful = mHttpClient.signUp(TAG, password, email);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
		if (isSuccessful) {
			Toast.makeText(this, "sign up successfully", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "sign up fails", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.register, menu);
		return super.onCreateOptionsMenu(menu);
	}
}
