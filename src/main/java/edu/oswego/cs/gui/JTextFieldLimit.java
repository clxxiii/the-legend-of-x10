package edu.oswego.cs.gui;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * Lets you limit a text field to a specified number of characters.
 */
//https://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
public class JTextFieldLimit extends PlainDocument {
    private final int limit;

    JTextFieldLimit(int limit) {
        super();
        this.limit = limit;
    }

    public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
        if (str == null) return;

        if ((getLength() + str.length()) <= limit) {
            super.insertString(offset, str, attr);
        }
    }
}