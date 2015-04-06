package socialatwork.readpeer.Util;

/* This class will formalize an annotation to the length of 256*/

public class StringFormalizer {

	private final int FORMAL_LENGTH = 256;

	public StringFormalizer() {

	}

	public String formalize(String s) {
		StringBuilder buf = new StringBuilder(s);
		buf.setLength(FORMAL_LENGTH);
		return buf.toString();
	}
}
