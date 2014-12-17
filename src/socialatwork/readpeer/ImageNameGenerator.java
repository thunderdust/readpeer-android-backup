package socialatwork.readpeer;

import java.net.URL;

import android.util.Log;

public class ImageNameGenerator {

	private static ImageNameGenerator mGenerator;
	private final String TAG = "Image Name Generator";
	private final int NAME_MAX_LENGTH = 100;

	public static synchronized ImageNameGenerator getInstance() {
		if (mGenerator == null) {
			mGenerator = new ImageNameGenerator();
		}
		return mGenerator;
	}

	public String getNameFromLink(URL imageLink) {

		// Get link string from url
		String link = imageLink.toString();
		// Remove http head
		link = link.replaceAll("http://", "");
		// Remove illegal characters
		link = link.replaceAll("[<.*?>-_\\/\"\']", "");
		// Cut the name if length too long
		if (link.length() > NAME_MAX_LENGTH) {
			int length = link.length();
			link = link.substring(length - NAME_MAX_LENGTH - 1, length - 1);
		}
		String name = link + ".jpg";
		Log.d(TAG, "final name:" + name);
		return name;
	}
}
