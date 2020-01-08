/* DoubleInput.java
 * Austin Lanham
 * May 7, 2005
 *
 * This class implements a panel which allows the user
 * to enter a double value into a text field.
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

class DoubleInput extends JPanel {
    double val;     // the value of the text field
    JTextArea text; // the text inside the text field
    
    // constructor: l is the left label, v is the default value.
    public DoubleInput(String l, double v) {
        // call the JPanel constructor
        super();
        setLayout(new FlowLayout());
        
        // initialize the left lael
        JLabel label = new JLabel(l);
        
        // create text area
        text = new JTextArea(1,8);
        // insert default value as a string
        text.insert(String.valueOf(v),0);
        // label the right of the text area
        JLabel ns = new JLabel("ns");
        
        // add all three elements to the panel
        add(label);
        add(text);
        add(ns);
    }
    
    // this function retrieves the value in the text area
    public double getValue() {
        return Double.parseDouble(text.getText());
    }
}
