package socialatwork.readpeer.WebRelatedComponents;

import android.util.Log;

public class ReturnedContentHandler {

	private static final String TAG = "content handler";
	private static String ACCESS_TOKEN = null;
	private static String USERNAME = null;
	private static String UID = null;
	private static String AVATAR_URL = null;
	private static int TYPE_STRING = 1;
	private static int TYPE_INT = 2;
	private static int TYPE_BOOK = 3;

	private static ReturnedContentHandler handlerInstance;

	// 'Synchronized' is to ensure there is only one instance
	public static synchronized ReturnedContentHandler getHandlerInstance() {
		if (handlerInstance == null) {
			handlerInstance = new ReturnedContentHandler();
		}
		return handlerInstance;
	}

	// Get value from returned content according to field names and data type
	//
	public String getValueFromContentReturned(String content, String keyword,
			int type) {
		// This is from the content format:
		// "field name1":"field value1","field name2":"field value2",etc

		int keywordStartIndex = content.indexOf(keyword);
		int valueStartIndex = 0;
		int valueEndIndex = 0;
		Log.i(TAG, "keyword start:" + Integer.toString(keywordStartIndex));
		// No such substring
		if (keywordStartIndex == -1)
			return null;
		else {

			if (type == TYPE_STRING) {

				valueStartIndex = keywordStartIndex + keyword.length() + 3;
				Log.i(TAG, "keyword length:" + keyword.length());
				Log.i(TAG, "value start:" + valueStartIndex);
				valueEndIndex = content.indexOf("\"", valueStartIndex);
				Log.i(TAG, "Value end:" + Integer.toString(valueEndIndex));
				return content.substring(valueStartIndex, valueEndIndex);
			} else if (type == TYPE_INT) {

				valueStartIndex = keywordStartIndex + keyword.length() + 2;
				Log.i(TAG, "keyword length:" + keyword.length());
				Log.i(TAG, "value start:" + valueStartIndex);
				valueEndIndex = content.indexOf(",", valueStartIndex);
				Log.i(TAG, "Value end:" + Integer.toString(valueEndIndex));
				return content.substring(valueStartIndex, valueEndIndex);
			} else if (type == TYPE_BOOK) {

				valueStartIndex = content.indexOf("[", keywordStartIndex);
				Log.i(TAG, "keyword length:" + keyword.length());
				Log.i(TAG, "value start:" + valueStartIndex);
				valueEndIndex = content.indexOf("]", valueStartIndex)+1;
				Log.i(TAG, "Value end:" + Integer.toString(valueEndIndex));
				//Log.i(TAG, "book info string:" + content.substring(valueStartIndex, valueEndIndex));
				return content.substring(valueStartIndex, valueEndIndex);
				
			} else {
				return null;
			}
		}
	}

}
