package socialatwork.readpeer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import socialatwork.readpeer.Fragments.AllBooksFragment;
import socialatwork.readpeer.Fragments.AllBooksFragment.OnNewBookDownloadedListener;
import socialatwork.readpeer.Fragments.MyBooksFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class BookShelfActivity extends FragmentActivity implements
		OnNewBookDownloadedListener {

	private static String site_username;
	private static String avatar_url_link;
	private static String uid;
	private static String access_token;
	private static final String TAG = "bookshelf";

	private AllBooksFragment mAllBooksFragment;
	private MyBooksFragment mMyBooksFragment;

	private Dialog accountMenuDialog;
	private Dialog functionMenuDialog;

	private ViewPager mViewPager;
	private ViewPagerAdapter mViewPagerAdapter;
	private ArrayList<Fragment> fragments;
	private EditText title;

	private Drawable userAvatar;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Hide the window title
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_book_shelf);

		initFragments();
		initViewPager();
		try {
			getUserData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		passUserInfo();

		Button functionButton = (Button) findViewById(R.id.btn_function);
		Drawable iconFunction = getResources().getDrawable(
				R.drawable.icon_function);
		functionButton.setCompoundDrawablesWithIntrinsicBounds(iconFunction,
				null, null, null);
		functionButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				initialzeFunctionMenu();
				functionMenuDialog.show();
			}

			private void initialzeFunctionMenu() {

				functionMenuDialog = new Dialog(BookShelfActivity.this,
						R.style.dialog_account);
				functionMenuDialog
						.requestWindowFeature(Window.FEATURE_NO_TITLE);
				functionMenuDialog
						.setContentView(R.layout.dialog_function_menu);
				functionMenuDialog.setCanceledOnTouchOutside(true);
				Window mWindow = functionMenuDialog.getWindow();
				WindowManager.LayoutParams lp = mWindow.getAttributes();
				lp.alpha = 0.9f;
				mWindow.setAttributes(lp);
				mWindow.setGravity(Gravity.START | Gravity.TOP);

				Button bookmarkButton = (Button) functionMenuDialog
						.findViewById(R.id.btn_bookmark);
				bookmarkButton.setText("Bookmarks");
				Drawable bookmarkIcon = getResources().getDrawable(
						R.drawable.icon_bookmark);
				bookmarkButton.setCompoundDrawablesWithIntrinsicBounds(
						bookmarkIcon, null, null, null);
			}
		});

		Button accountButton = (Button) findViewById(R.id.btn_myAccount);
		Drawable iconAccount = getResources().getDrawable(
				R.drawable.icon_account);
		accountButton.setCompoundDrawablesWithIntrinsicBounds(null, null,
				iconAccount, null);
		accountButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startMenuDialog();
			}

			private void startMenuDialog() {

				accountMenuDialog = new Dialog(BookShelfActivity.this,
						R.style.dialog_account);
				accountMenuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				accountMenuDialog.setContentView(R.layout.dialog_account_menu);

				Window mWindow = accountMenuDialog.getWindow();
				WindowManager m = getWindowManager();
				WindowManager.LayoutParams lp = mWindow.getAttributes();
				lp.alpha = 0.9f;
				mWindow.setAttributes(lp);
				mWindow.setGravity(Gravity.END | Gravity.TOP);
				accountMenuDialog.setCanceledOnTouchOutside(true);

				Button usernameBtn = (Button) accountMenuDialog
						.findViewById(R.id.account_username);
				usernameBtn.setText(site_username);
				usernameBtn.setCompoundDrawablesWithIntrinsicBounds(userAvatar,
						null, null, null);
				usernameBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						final Intent toProfile = new Intent(
								BookShelfActivity.this, ProfileActivity.class);
						toProfile.putExtra("access_token", access_token);
						toProfile.putExtra("uid", uid);
						toProfile.putExtra("avatar_url", avatar_url_link);
						toProfile.putExtra("username", site_username);
						accountMenuDialog.dismiss();
						startActivity(toProfile);
					}
				});

				Button newsfeedBtn = (Button) accountMenuDialog
						.findViewById(R.id.account_newsfeed);
				newsfeedBtn.setText("Newsfeed");
				Drawable newsfeedIcon = getResources().getDrawable(
						R.drawable.icon_newsfeed);
				newsfeedBtn.setCompoundDrawablesWithIntrinsicBounds(
						newsfeedIcon, null, null, null);
				newsfeedBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent toNewsFeed = new Intent(BookShelfActivity.this,
								NewsFeedActivity.class);
						toNewsFeed.putExtra("access_token", access_token);
						toNewsFeed.putExtra("uid", uid);
						accountMenuDialog.dismiss();
						startActivity(toNewsFeed);
					}
				});

				Button logoutBtn = (Button) accountMenuDialog
						.findViewById(R.id.account_logout);
				logoutBtn.setText("Log out");
				Drawable logoutIcon = getResources().getDrawable(
						R.drawable.icon_log_out);
				logoutBtn.setCompoundDrawablesWithIntrinsicBounds(logoutIcon,
						null, null, null);
				logoutBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent toLogIn = new Intent(BookShelfActivity.this,
								LogInActivity.class);
						toLogIn.putExtra("isAutoLoginCanceled", true);
						accountMenuDialog.dismiss();
						startActivity(toLogIn);
						finish();
					}
				});

				accountMenuDialog.show();
			}
		});

		title = (EditText) findViewById(R.id.EditText_bookshelf_name);
		title.setText(R.string.title_myBook);
		title.setFocusable(false);
		title.setClickable(false);
	}

	private void initFragments() {
		// TODO Auto-generated method stub
		mMyBooksFragment = MyBooksFragment.newInstance();
		mAllBooksFragment = AllBooksFragment.newInstance();
		fragments = new ArrayList<Fragment>();
		fragments.add(mMyBooksFragment);
		fragments.add(mAllBooksFragment);
	}

	private void initViewPager() {
		// TODO Auto-generated method stub
		mViewPager = (ViewPager) findViewById(R.id.viewpager_bookshelf);
		mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(),
				fragments);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(0);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			// This method will be invoked when a new page becomes selected.
			@Override
			public void onPageSelected(int position) {

				switch (position) {
				case 0:
					title.setText(R.string.title_myBook);
					break;
				case 1:
					title.setText(R.string.title_allBook);
					break;

				default:
					title.setText(R.string.title_allBook);
					break;
				}
			}

			// This method will be invoked when the current page is scrolled
			@Override
			public void onPageScrolled(int position, float positionOffset,
					int positionOffsetPixels) {
			}

			// Called when the scroll state changes:
			// SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
			@Override
			public void onPageScrollStateChanged(int state) {
				// Code goes here
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.quit_confirmation)
					.setCancelable(false)
					.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									finish();
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.setCanceledOnTouchOutside(true);
			alert.show();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public class ViewPagerAdapter extends FragmentPagerAdapter {

		private ArrayList<Fragment> fragmentList;

		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		public ViewPagerAdapter(FragmentManager fm,
				ArrayList<Fragment> fragments) {
			super(fm);
			this.fragmentList = fragments;
		}

		@Override
		public Fragment getItem(int index) {
			return fragmentList.get(index);
		}

		@Override
		public int getCount() {
			return fragmentList.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			String tabLabel = null;
			switch (position) {
			case 0:
				tabLabel = getString(R.string.title_myBook);
				break;
			case 1:
				tabLabel = getString(R.string.title_allBook);
				break;
			default:
				tabLabel = getString(R.string.title_allBook);
				break;
			}
			return tabLabel;
		}
	}

	private void getUserData() throws IOException {
		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			access_token = extras.getString("access_token");
			uid = extras.getString("uid");
			site_username = extras.getString("username");
			avatar_url_link = extras.getString("avatar_url");
			Log.i(TAG, uid);
			Log.i(TAG, access_token);
			Log.i(TAG, avatar_url_link);
			Log.i(TAG, site_username);
		} else {
			Log.e(TAG, "intent is null");
		}

		avatar_url_link = urlParser(avatar_url_link);
		URL avatar_url = new URL(avatar_url_link);
		new loadAvatarPictureTask().execute(avatar_url);
	}

	private class loadAvatarPictureTask extends AsyncTask<URL, Void, Bitmap> {

		URL picURL;

		@Override
		protected Bitmap doInBackground(URL... urls) {
			this.picURL = urls[0];
			Bitmap bm = null;
			try {
				HttpURLConnection conn = (HttpURLConnection) picURL
						.openConnection();
				conn.setDoInput(true);
				conn.setConnectTimeout(3000);
				conn.setRequestMethod("GET");
				conn.connect();
				InputStream stream = conn.getInputStream();
				bm = BitmapFactory.decodeStream(stream);
				stream.close();
				conn.disconnect();
				return bm;
			} catch (Exception e) {
				e.printStackTrace();
				return bm;
			}
		}

		protected void onPostExecute(Bitmap bm) {
			if (bm != null) {
				Bitmap avatarBitmap = Bitmap.createScaledBitmap(bm, 60, 60,
						true);
				userAvatar = new BitmapDrawable(getResources(), avatarBitmap);
			}
		}
	}

	private String urlParser(String originalURL) {
		Log.i("profile page", "before parsing:" + originalURL);
		originalURL = originalURL.replace("\\", "");
		Log.i("profile page", "after parsing:" + originalURL);
		return originalURL;
	}

	private void passUserInfo() {
		Bundle bundle = new Bundle();
		bundle.putString("access_token", access_token);
		bundle.putString("uid", uid);
		bundle.putString("username", site_username);

		// Bundle bookStoreBundle = new Bundle();
		// bookStoreBundle.putString("access_token", access_token);
		// bookStoreBundle.putString("uid", uid);
		// bookStoreBundle.putInt("bookCount", bookNumer);
		// bookStoreBundle.putStringArrayList("bookName", bookNames);
		// bookStoreBundle.putStringArrayList("bookCoverUrl", bookCoverUrl);
		// bookStoreBundle.putStringArrayList("bookInfo", bookInfoArray);

		mAllBooksFragment.setArguments(bundle);
		mMyBooksFragment.setArguments(bundle);
	}

	@Override
	public void notifyNewBookDownloaded() {
		// TODO Auto-generated method stub
		MyBooksFragment f = (MyBooksFragment) mViewPagerAdapter.getItem(0);
		f.updateLocalBooks();
	}
}
