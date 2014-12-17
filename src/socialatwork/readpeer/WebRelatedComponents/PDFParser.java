package socialatwork.readpeer.WebRelatedComponents;

public class PDFParser {

	public static String parsePDFString(String inputString) {
		String parsedString = inputString;
		parsedString = parsedString.replaceAll("&#160;", " ");
		return parsedString;
	}
}
