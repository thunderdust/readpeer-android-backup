package socialatwork.readpeer.Util;

public class FileManager {

	public static String getSaveFilePath() {
		if (CommonUtil.hasSDCard()) {
			return CommonUtil.getRootFilePath() + "ReadPeer/Cache/Pictures";
		} else {
			return CommonUtil.getRootFilePath() + "ReadPeer/Cache/Pictures";
		}
	}
}