package socialatwork.readpeer.WebRelatedComponents;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class tdHttpClient {

	private final String USER_NAME = "name";
	private final String PASSWORD = "password";
	private final String EMAIL = "email";
	private final String SOCIAL_TYPE = "social_network_type";
	private final String SOCIAL_ID = "social_network_id";
	private final String SOCIAL_NAME = "social_network_name";
	private final String USER_URL = "http://readpeer.com/api/users";
	private final String BOOK_URL = "http://readpeer.com/api/books";
	private final String ANNOTATION_URL = "http://readpeer.com/api/annotations";
	private final String API_KEY = "readpeer_api_key";
	private final String API_SECRET = "readpeer_api_secret";
	private final String readpeer_api_key = "462294414349540";
	private final String readpeer_api_secret = "637704063335ed9448e2297936cb58ca";
	private final String URL_WITH_API_PARAMETERS = USER_URL + "?" + API_KEY
			+ "=" + readpeer_api_key + "&" + API_SECRET + "="
			+ readpeer_api_secret;
	private final String SUCCESS_CODE = "\"code\":200";
	private final String FAILURE_CODE = "\"code\":1002";
	private final String TAG = "http client";

	private final int BOOKTYPE_MY = 0;
	private final int BOOKTYPE_ALL = 1;
	private final int BOOKTYPE_POPULAR = 2;

	private URLEncoder mURLEncoder;
	private static tdHttpClient clientInstance;
	private DefaultHttpClient mHttpClient;

	tdHttpClient() {
		mHttpClient = new DefaultHttpClient();
	}

	// 'Synchronized' is to ensure there is only one client instance
	public synchronized static tdHttpClient getClientInstance() {
		if (clientInstance == null) {
			clientInstance = new tdHttpClient();
		}
		return clientInstance;
	}

	/*
	 * SECTION START <sign up/ sign in, log out/ log in account operation
	 * methods>
	 */
	public String logIn(String username, String password) throws Exception {

		// List<NameValuePair> loginPair = new ArrayList<NameValuePair>();
		// loginPair.add(new BasicNameValuePair(API_KEY, readpeer_api_key));
		// loginPair.add(new BasicNameValuePair(API_SECRET,
		// readpeer_api_secret));
		// loginPair.add(new BasicNameValuePair(USER_NAME, username));
		// loginPair.add(new BasicNameValuePair(PASSWORD, password));
		// loginPut.setEntity(new UrlEncodedFormEntity(loginPair));
		String requestURL = URL_WITH_API_PARAMETERS + "&" + USER_NAME + "="
				+ URLEncoder.encode(username, "utf-8") + "&" + PASSWORD + "="
				+ password;
		HttpPut loginPut = new HttpPut(requestURL);
		Log.i(TAG, "before getting response");
		HttpResponse response = mHttpClient.execute(loginPut);
		Log.i(TAG, "tried to get response");

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "LOGIN SUCCESSFUL");
				return content;
			}// unauthorized
			else if (content.contains(FAILURE_CODE)) {
				Log.w(TAG, "unauthorized");
				// Log.i(TAG, content);
				return "unauthorized";
			}
		}
		return null;
	}

	public boolean signUp(String username, String password, String email)
			throws Exception {
		HttpPost signUpPost = new HttpPost(USER_URL + "?ss");
		List<NameValuePair> signupPair = new ArrayList<NameValuePair>();
		signupPair.add(new BasicNameValuePair(API_KEY, readpeer_api_key));
		signupPair.add(new BasicNameValuePair(API_SECRET, readpeer_api_secret));
		signupPair.add(new BasicNameValuePair(USER_NAME, username));
		signupPair.add(new BasicNameValuePair(PASSWORD, password));
		signupPair.add(new BasicNameValuePair(EMAIL, email));

		signUpPost.setEntity(new UrlEncodedFormEntity(signupPair));
		HttpResponse response = mHttpClient.execute(signUpPost);
		HttpEntity entity = response.getEntity();
		InputStream inputStream = entity.getContent();
		String content = inputStreamToString(inputStream);
		response.getEntity().consumeContent();

		if (response != null) {
			if (content.contains(SUCCESS_CODE)) {
				Log.e(TAG, "sign up success");
				// Log.i(TAG, content);
				return true;
			} else {
				Log.e(TAG, "sign up fail");
				// Log.i(TAG, content);
				return false;
			}
		}
		return false;
	}

	/*
	 * public boolean socialSignUp(int type, String id, String name) throws
	 * Exception {
	 * 
	 * HttpPost socialSignUpPost = new HttpPost(USER_URL); List<NameValuePair>
	 * socialSignupPair = new ArrayList<NameValuePair>();
	 * 
	 * socialSignupPair.add(new BasicNameValuePair(SOCIAL_TYPE, Integer
	 * .toString(type))); socialSignupPair.add(new BasicNameValuePair(SOCIAL_ID,
	 * id)); socialSignupPair.add(new BasicNameValuePair(SOCIAL_NAME, name));
	 * 
	 * socialSignUpPost.setEntity(new UrlEncodedFormEntity(socialSignupPair));
	 * HttpResponse response = mHttpClient.execute(socialSignUpPost);
	 * 
	 * if (response != null) { int status =
	 * response.getStatusLine().getStatusCode(); // sign up successfully if
	 * (status == 200) {
	 * 
	 * return true;
	 * 
	 * } else { Log.e(TAG, "sign up fail"); InputStream inputStream =
	 * response.getEntity().getContent(); String message =
	 * inputStreamToString(inputStream); return false; } } return false; }
	 */

	/* Get user information such as profile and newsfeed */
	public String getUserContent(String access_token, String contentType,
			String uid) throws Exception {

		String requestContentURL = USER_URL + "/" + uid + "/" + contentType
				+ "?" + "access_token=" + access_token;

		HttpGet getContent = new HttpGet(requestContentURL);
		Log.i(TAG, "before getting response,URL:" + requestContentURL);
		HttpResponse response = mHttpClient.execute(getContent);
		Log.i(TAG, "tried to get response");

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			Log.i(TAG, content);
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET CONTENT SUCCESSFUL");
				return content;
			}// unauthorized
			else if (content.contains(FAILURE_CODE)) {
				Log.w(TAG, "unauthorized");
				return "unauthorized";
			}
		} else {
			Log.e(TAG, "return content is null");
			return null;
		}
		return null;
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	/*
	 * SECTION START <Book Related Methods>
	 */
	
	/* This method retrieve html of the specified page of certain book */
	public String getBookHtmlByPage (String access_token, String page_num, String bookID) throws Exception{
		String requestBookHtmlURL = BOOK_URL+"/"+"2903"+"/html"+"?access_token="+access_token+"&page_number="+page_num;
		HttpGet getBookHtml = new HttpGet(requestBookHtmlURL);
		HttpResponse response = mHttpClient.execute(getBookHtml);
		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			if (content.contains("html")) {
				Log.i(TAG, "GET BOOK HTML SUCCESSFUL");
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET BOOK HTML FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;	
	}

	/* Get book list on user's reading list from server */
	public String getUserReadings(String userID, String access_token) throws Exception {
		
		String requestBookURL = USER_URL +"/"+userID+"/library";
		HttpGet getUserReading = new HttpGet(requestBookURL);
		HttpResponse response = mHttpClient.execute(getUserReading);
		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET USER READING LIST SUCCESSFUL");
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET USER READING LIST FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}
	
	public boolean deleteUserBook(String bookID, String access_token,String uid) throws Exception{
		
		URL url = new URL(USER_URL + "/" + uid + "/library"
				+ "?access_token=" + access_token + "&book_id=" + bookID);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setRequestMethod("DELETE");
		httpCon.connect();
		InputStream inputStream = httpCon.getInputStream();
		String responseStr = inputStreamToString(inputStream);

		if (responseStr != null) {
			if (responseStr.contains(SUCCESS_CODE)) {
				Log.i(TAG, "DELETE USER BOOK SUCCEED");
				Log.d(TAG, responseStr);
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "DELETE USER BOOK FAILED");
				Log.d(TAG, responseStr);
				return false;
			}
		}
		return false;		
	}
	
	public boolean addUserBook(String bookID, String access_token, String uid) throws Exception{
		String bookDeletionUrl = USER_URL + "/" + uid + "/library";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs
				.add(new BasicNameValuePair("access_token", access_token));
		nameValuePairs.add(new BasicNameValuePair("book_id", bookID));
		HttpPost deleteBookPost = new HttpPost(bookDeletionUrl);
		deleteBookPost.setEntity(new UrlEncodedFormEntity(nameValuePairs,
				HTTP.UTF_8));
		HttpResponse response = mHttpClient.execute(deleteBookPost);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "BOOK DELETION SUCCEED");
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "BOOK DELETION FAILED");
				return false;
			}
		}
		return false;
	}

	public String getBooks(String access_token, int bookType, String page)
			throws Exception, IOException {

		Log.d(TAG, "page:" + page);
		String requestBookURL = null;
		switch (bookType) {
		case BOOKTYPE_MY:
			requestBookURL = BOOK_URL + "/my" + "?" + "access_token" + "="
					+ access_token + "&" + "page" + "=" + page;
			break;
		case BOOKTYPE_ALL:
			requestBookURL = BOOK_URL + "/total" + "?" + "access_token" + "="
					+ access_token + "&" + "page" + "=" + page;
			break;
		case BOOKTYPE_POPULAR:
			requestBookURL = BOOK_URL + "/popular" + "?" + "access_token" + "="
					+ access_token + "&" + "page" + "=" + page;
			break;
		}

		HttpGet getBook = new HttpGet(requestBookURL);
		HttpResponse response = mHttpClient.execute(getBook);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET BOOKS SUCCESSFUL");
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET BOOKS FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	public String getBookDetails(String bookId, String access_token)
			throws Exception {
		String requestBookDetailURL = BOOK_URL + "/" + bookId + "/full"
				+ "?access_token=" + access_token;
		HttpGet getBookDetail = new HttpGet(requestBookDetailURL);
		HttpResponse response = mHttpClient.execute(getBookDetail);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET BOOK DETAILS SUCCESSFUL");
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET BOOK DETAILS FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	/*
	 * SECTION START <Annotation/comments related methods>
	 */

	public String getAnnotation(String bookId, String access_token)
			throws Exception, IOException {
		String getAnnotationURL = BOOK_URL + "/" + bookId + "/annotations"
				+ "?access_token=" + access_token;
		HttpGet getAnnotation = new HttpGet(getAnnotationURL);
		HttpResponse response = mHttpClient.execute(getAnnotation);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, "annotation:" + content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET ANNOTATION SUCCESSFUL");
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET ANNOTATION FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	public boolean createAnnotationWithoutFile(String access_token, String uid,
			String msg, String annotation_category, String book_id,
			String page_id, String type, int start, int end, String selected)
			throws Exception, IOException {

		String setAnnotationURL = "http://readpeer.com/api/annotations/add";

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs
				.add(new BasicNameValuePair("access_token", access_token));
		nameValuePairs.add(new BasicNameValuePair("uid", uid));
		nameValuePairs.add(new BasicNameValuePair("msg", msg));
		nameValuePairs.add(new BasicNameValuePair("annotation_category",
				annotation_category));
		nameValuePairs.add(new BasicNameValuePair("book_id", book_id));
		nameValuePairs.add(new BasicNameValuePair("page_id", page_id));
		nameValuePairs.add(new BasicNameValuePair("type", type));
		nameValuePairs.add(new BasicNameValuePair("start", Integer
				.toString(start)));
		nameValuePairs
				.add(new BasicNameValuePair("end", Integer.toString(end)));
		nameValuePairs.add(new BasicNameValuePair("selected", selected));

		HttpPost setAnnotation = new HttpPost(setAnnotationURL);
		setAnnotation.setEntity(new UrlEncodedFormEntity(nameValuePairs,
				HTTP.UTF_8));
		HttpResponse response = mHttpClient.execute(setAnnotation);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "ANNOTATION CREATION SUCCEED");
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "ANNOTATION CREATION FAILED");
				return false;
			}
		}
		return false;
	}

	public boolean createAnnotationWithFile(String access_token, String uid,
			String msg, String fid, String annotation_category, String book_id,
			String page_id, int start, int end, String selected)
			throws ClientProtocolException, IOException {

		String postAnnotationURL = "http://readpeer.com/api/annotations/add";
		HttpPost httpPost = new HttpPost(postAnnotationURL);
		HttpContext localContext = new BasicHttpContext();

		// StringEntity attachParams = new StringEntity(attach, "UTF8");

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("uid", uid));
		// parameters.add(new BasicNameValuePair("attach", attachFileInfo));
		// Log.i(TAG,attachFileInfo);
		parameters.add(new BasicNameValuePair("attach[type]", "file"));
		parameters.add(new BasicNameValuePair("attach[id]", fid));
		parameters.add(new BasicNameValuePair("access_token", access_token));
		parameters.add(new BasicNameValuePair("msg", msg));
		parameters.add(new BasicNameValuePair("annotation_category",
				annotation_category));
		parameters.add(new BasicNameValuePair("book_id", book_id));
		parameters.add(new BasicNameValuePair("page_id", page_id));
		parameters
				.add(new BasicNameValuePair("start", Integer.toString(start)));
		parameters.add(new BasicNameValuePair("end", Integer.toString(end)));
		parameters.add(new BasicNameValuePair("selected", selected));

		httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
		// httpPost.setHeader("Content-type", "application/json");
		// httpPost.setEntity(attachParams);
		HttpResponse response = mHttpClient.execute(httpPost, localContext);
		HttpEntity resEntity = response.getEntity();
		String responseStr = EntityUtils.toString(resEntity).trim();
		Log.v(TAG, "Response: " + responseStr);
		response.getEntity().consumeContent();
		if (responseStr.contains(SUCCESS_CODE)) {
			Log.i(TAG, "ANNOTATION CREATION WITH FILE SUCCEED");
			return true;
		}// unauthorized
		else {
			Log.w(TAG, "ANNOTATION CREATION WITH FILE FAILED");
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public String uploadFile(String access_token, String uid, String mediaType,
			File fileToBeUploaded) throws Exception {

		// File fileToBeUploaded = new File(filePath);
		if (fileToBeUploaded.isFile()) {

			String uploadFileURL = "http://readpeer.com/api/annotations/fileupload";
			HttpPost httpPost = new HttpPost(uploadFileURL);
			HttpContext localContext = new BasicHttpContext();
			FileBody fileBody = new FileBody(fileToBeUploaded);
			Log.i(TAG, "file info:" + fileBody.toString());

			MultipartEntity reqEntity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addPart("async-upload", fileBody);
			reqEntity.addPart("uid", new StringBody(uid));
			reqEntity.addPart("mediatype", new StringBody(mediaType));
			reqEntity.addPart("access_token", new StringBody(access_token));
			httpPost.setEntity(reqEntity);
			HttpResponse response = mHttpClient.execute(httpPost, localContext);
			HttpEntity resEntity = response.getEntity();
			String responseStr = EntityUtils.toString(resEntity).trim();
			response.getEntity().consumeContent();
			Log.v(TAG, "Response: " + responseStr);
			return responseStr;
		}
		return "The file indicated is not a legal file";
	}

	public String getAnnotationComments(String access_token,
			String annotationID, int pageNumber) throws Exception {

		String getAnnotationURL = ANNOTATION_URL + "/comment"
				+ "?access_token=" + access_token + "&aid=" + annotationID
				+ "&pageno=" + pageNumber;
		HttpGet getAnnotationComments = new HttpGet(getAnnotationURL);
		HttpResponse response = mHttpClient.execute(getAnnotationComments);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();

			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET ANNOTATION COMMENTS SUCCESSFUL");
				Log.i(TAG, "comments:" + content);
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET ANNOTATION COMMENTS FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	public boolean postCommentWithoutFile(String aid, String access_token,
			String msg, String pid, String subject) throws Exception {

		String postCommentURL = ANNOTATION_URL + "/comment";

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs
				.add(new BasicNameValuePair("access_token", access_token));
		nameValuePairs.add(new BasicNameValuePair("msg", msg));
		nameValuePairs.add(new BasicNameValuePair("pid", pid));
		nameValuePairs.add(new BasicNameValuePair("aid", aid));
		nameValuePairs.add(new BasicNameValuePair("subject", subject));

		HttpPost setAnnotation = new HttpPost(postCommentURL);
		setAnnotation.setEntity(new UrlEncodedFormEntity(nameValuePairs,
				HTTP.UTF_8));
		HttpResponse response = mHttpClient.execute(setAnnotation);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();
			// Log.i(TAG, content);
			// is logged in
			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "COMMENT POST SUCCEED");
				return true;
			}// unauthorized
			else {
				response.getEntity().consumeContent();
				Log.w(TAG, "COMMENT POST FAILED");
				Log.i(TAG, content);
				return false;
			}
		}
		return false;
	}

	public boolean postCommentWithFile(String access_token, String aid,
			String subject, String msg, String fid, String pid)
			throws Exception {

		String postCommentURL = ANNOTATION_URL + "/comment";
		HttpPost httpPost = new HttpPost(postCommentURL);
		HttpContext localContext = new BasicHttpContext();

		// StringEntity attachParams = new StringEntity(attach, "UTF8");

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("aid", aid));
		parameters.add(new BasicNameValuePair("attach[type]", "file"));
		parameters.add(new BasicNameValuePair("attach[id]", fid));
		parameters.add(new BasicNameValuePair("access_token", access_token));
		parameters.add(new BasicNameValuePair("msg", msg));
		parameters.add(new BasicNameValuePair("pid", pid));

		httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
		// httpPost.setHeader("Content-type", "application/json");
		// httpPost.setEntity(attachParams);
		HttpResponse response = mHttpClient.execute(httpPost, localContext);
		HttpEntity resEntity = response.getEntity();
		String responseStr = EntityUtils.toString(resEntity).trim();
		response.getEntity().consumeContent();
		Log.v(TAG, "Response: " + responseStr);
		if (responseStr.contains(SUCCESS_CODE)) {
			Log.i(TAG, "COMMENT WITH FILE POST SUCCEED");
			return true;
		}// unauthorized
		else {
			Log.w(TAG, "COMMENT WITH FILE POST FAILED");
			Log.i(TAG, responseStr);
			return false;
		}
	}

	/* Call server API to like certain annotation */
	public boolean likeAnnotation(String annotationID, String uid,
			String access_token) throws ParseException, IOException {

		String postAnnotationURL = ANNOTATION_URL + "/like";
		HttpPost httpPost = new HttpPost(postAnnotationURL);
		HttpContext localContext = new BasicHttpContext();

		// StringEntity attachParams = new StringEntity(attach, "UTF8");

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("uid", uid));
		parameters.add(new BasicNameValuePair("nid", annotationID));
		parameters.add(new BasicNameValuePair("access_token", access_token));

		httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
		HttpResponse response = mHttpClient.execute(httpPost, localContext);
		HttpEntity resEntity = response.getEntity();
		String responseStr = EntityUtils.toString(resEntity);
		response.getEntity().consumeContent();
		// Log.v(TAG, "Response: " + responseStr);

		if (responseStr.contains(SUCCESS_CODE)) {
			Log.i(TAG, "LIKE POST SUCCEED");
			Log.d(TAG, responseStr);
			return true;
		}// unauthorized
		else {
			Log.w(TAG, "LIKE POST FAILED");
			Log.d(TAG, responseStr);
			return false;
		}
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	/*
	 * SECTION START <Follow related methods>
	 */

	public String getFollowersOrFollowing(String type, String access_token,
			String uid) throws Exception {

		String url;
		HttpResponse response = null;

		if (type.compareTo("follower") == 0) {
			Log.d(TAG, "getting followers");
			url = USER_URL + "/" + uid + "/follower" + "?" + "access_token="
					+ access_token;
			;
			Log.d(TAG, "url:" + url);
			HttpGet getFollower = new HttpGet(url);
			response = mHttpClient.execute(getFollower);
		}

		else if (type.compareTo("following") == 0) {
			Log.d(TAG, "getting following");
			url = USER_URL + "/" + uid + "/following" + "?" + "access_token="
					+ access_token;
			;
			Log.d(TAG, "url:" + url);
			HttpGet getFollowing = new HttpGet(url);
			response = mHttpClient.execute(getFollowing);
		}

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			response.getEntity().consumeContent();

			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET FOLLOWER/FOLLOWING SUCCESSFUL");
				Log.i(TAG, content);
				return content;
			}// unauthorized
			else {
				response.getEntity().consumeContent();
				Log.w(TAG, "GET FOLLOWER/FOLLOWING FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	public boolean followUser(String uid, String targetUserID,
			String access_token) throws Exception {

		String url = USER_URL + "/" + uid + "/following";
		HttpPost followPost = new HttpPost(url);
		HttpContext localContext = new BasicHttpContext();

		// StringEntity attachParams = new StringEntity(attach, "UTF8");

		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("uid", targetUserID));
		parameters.add(new BasicNameValuePair("access_token", access_token));

		followPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
		HttpResponse response = mHttpClient.execute(followPost, localContext);
		HttpEntity resEntity = response.getEntity();
		String responseStr = EntityUtils.toString(resEntity);
		response.getEntity().consumeContent();

		if (responseStr != null) {
			if (responseStr.contains(SUCCESS_CODE)) {
				Log.i(TAG, "FOLLOW SUCCEED");
				Log.d(TAG, responseStr);
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "FOLLOW FAILED");
				Log.d(TAG, responseStr);
				return false;
			}
		}
		return false;
	}

	public boolean unfollowUser(String uid, String targetUserID,
			String access_token) throws Exception {

		URL url = new URL(USER_URL + "/" + uid + "/following"
				+ "?access_token=" + access_token + "&uid=" + targetUserID);
		Log.d(TAG, "uid:" + uid);
		Log.d(TAG, "token:" + access_token);
		Log.d(TAG, "targetUserID:" + targetUserID);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		// httpCon.setDoOutput(true);
		// httpCon.setRequestProperty("uid", targetUserID);
		// httpCon.setRequestProperty("access_token", access_token);
		httpCon.setRequestMethod("DELETE");
		httpCon.connect();
		InputStream inputStream = httpCon.getInputStream();
		String responseStr = inputStreamToString(inputStream);

		if (responseStr != null) {
			if (responseStr.contains(SUCCESS_CODE)) {
				Log.i(TAG, "UNFOLLOW SUCCEED");
				Log.d(TAG, responseStr);
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "UNFOLLOW FAILED");
				Log.d(TAG, responseStr);
				return false;
			}
		}
		return false;
	}

	public boolean blockFollower(String uid, String access_token,
			String targetUserID) throws Exception {

		URL url = new URL(USER_URL + "/" + uid + "/follower" + "?access_token="
				+ access_token + "&uid=" + targetUserID);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		// httpCon.setDoOutput(true);
		// httpCon.setRequestProperty("uid", targetUserID);
		// httpCon.setRequestProperty("access_token", access_token);
		httpCon.setRequestMethod("DELETE");
		httpCon.connect();
		InputStream inputStream = httpCon.getInputStream();
		String responseStr = inputStreamToString(inputStream);

		if (responseStr != null) {
			if (responseStr.contains(SUCCESS_CODE)) {
				Log.i(TAG, "BLOCK SUCCEED");
				Log.d(TAG, responseStr);
				return true;
			}// unauthorized
			else {
				Log.w(TAG, "BLOCK FAILED");
				Log.d(TAG, responseStr);
				return false;
			}
		}
		return false;
	}

	// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

	/*
	 * SECTION START <news feed related methods>
	 */

	public String getUserNewsFeed(String uid, String access_token, String page)
			throws Exception {

		String url = USER_URL + "/" + uid + "/newsfeed" + "?" + "access_token="
				+ access_token + "&" + "page=" + page;
		HttpResponse response = null;
		HttpGet getNewsFeed = new HttpGet(url);
		response = mHttpClient.execute(getNewsFeed);

		if (response != null) {
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);

			if (content.contains(SUCCESS_CODE)) {
				Log.i(TAG, "GET NEWSFEED SUCCESSFUL");
				Log.i(TAG, content);
				return content;
			}// unauthorized
			else {
				Log.w(TAG, "GET NEWSFEED FAILED");
				Log.i(TAG, content);
				return null;
			}
		}
		return null;
	}

	private String inputStreamToString(InputStream inputStream)
			throws Exception {
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				inputStream), 4096);
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		String result = sb.toString();
		return result;
	}

	public void clearCookies() {
		mHttpClient.getCookieStore().clear();
	}

	public void execute(HttpGet httpGet) throws Exception {
		HttpResponse response = mHttpClient.execute(httpGet);
		if (response != null) {
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			String content = inputStreamToString(inputStream);
			entity.consumeContent();
		}
	}
}
