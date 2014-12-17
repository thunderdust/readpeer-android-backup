package socialatwork.readpeer;

import org.apache.http.impl.client.DefaultHttpClient;


/* This class implements a book mark management for local books */
public class BookmarkManager {

	private static BookmarkManager BookmarkManagerInstance;
	private String bookmarkSavingPath;
	public int bookmarkCount = 0;

	// 'Synchronized' is to ensure there is only one client instance
	public synchronized static BookmarkManager getManagerInstance() {
		if (BookmarkManagerInstance == null) {
			BookmarkManagerInstance = new BookmarkManager();
		}
		return BookmarkManagerInstance;
	}

	public int getBookmarkCount() {
		return 0;
	}
	
	public void saveBookMark(String bid, String bookTitle, String saveTime) {
		
	}

}
