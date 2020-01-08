/* IntInput.java
 * Austin Lanham
 * May 7, 2005
 *
 * This class implements a panel which allows the user
 * to enter a integer value into a text field.
 * The panel consists of a left label, a text field, and
 * a right label.  The initial value of the text field
 * is passed as an arguement in the constructor (v).
 * The left label is specified using the string l and
 * the right label is set to "ns" by default.
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

class IntInput extends JPanel {
    int val;         // int value of text area
    JTextArea text;  // text value in text area
    
    /* constructor which allows the programmer to
       specify both labels */
    public IntInput(String l, int v, String e) {
        super();
        createInput(l,v,e);
    }
    
    /* constructor which allows the programmer to specify
       the left input and uses "clocks" as the right one */
    public IntInput(String l, int v) {
        super();
        createInput(l,v,"clocks");
    }
    
    /* This function creates the objects and places them into
       the panel */
    public void createInput(String l, int v, String e) {
        setLayout(new FlowLayout());
        // create left label
        JLabel label = new JLabel(l);
        // create text field
        text = new JTextArea(1,8);
        // set default value specified by v
        text.insert(String.valueOf(v),0);
        // create right label
        JLabel ns = new JLabel(e);
        
        // add the elements to the panel
        add(label);
        add(text);
        add(ns);
    }
    
    /* this function retrieves the text from the text field */
    public int getValue() {
        return Integer.parseInt(text.getText());
    }
}
