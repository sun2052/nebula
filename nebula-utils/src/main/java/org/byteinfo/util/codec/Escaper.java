package org.byteinfo.util.codec;

import java.io.IOException;
import java.io.Writer;

/**
 * Escaper
 */
@FunctionalInterface
public interface Escaper {
	/**
	 * Escapes the text and writes to the writer.
	 *
	 * @param text source string
	 * @param writer target writer
	 * @throws IOException if an error occurs
	 */
	void escape(String text, Writer writer) throws IOException;

	/**
	 * Empty Escaper
	 */
	Escaper NONE = (text, writer) -> writer.write(text);

	/**
	 * XHTML Escaper
	 */
	Escaper XHTML = new Escaper() {
		private final char[] LT = "&lt;".toCharArray();
		private final char[] GT = "&gt;".toCharArray();
		private final char[] AMP = "&amp;".toCharArray();
		private final char[] APOS = "&#039;".toCharArray(); // &apos; is NOT supported in HTML 4. https://www.w3.org/TR/xhtml1/#C_16
		private final char[] QUOT = "&quot;".toCharArray();
		private final char[] GRAVE = "&#x60;".toCharArray();
		private final char[] EQUALS = "&#x3D;".toCharArray();

		@Override
		public void escape(String text, Writer writer) throws IOException {
			for (int i = 0; i < text.length(); i++) {
				int c = text.charAt(i);
				switch (c) {
					case '<' -> writer.write(LT);
					case '>' -> writer.write(GT);
					case '&' -> writer.write(AMP);
					case '\'' -> writer.write(APOS);
					case '"' -> writer.write(QUOT);
					case '`' -> writer.write(GRAVE);
					case '=' -> writer.write(EQUALS);
					default -> writer.write(c);
				}
			}
		}
	};
}
