/* TimeDialog.java
 *
 * Revised by Vincent Chan, January 10, 2006
 * Created by Austin Lanham, May 8, 2005
 *
 * This class creates a dialog that allows
 * the user to specify the start and end
 * time of the data using two text area
 * input fields.
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class TimeDialog extends JPanel {

    JFrame frame;     // frame that holds the dialog
    double startTime;
    double endTime;

    public TimeDialog(JFrame f, final VisTool parent) {
        this.frame = f;

        // create the panel that holds the inputs and buttons
        JPanel timePanel = new JPanel();
        timePanel.setPreferredSize(new Dimension(300,200));

        // set the default values as the current ones
        startTime = parent.getStartTime();
        endTime   = parent.getEndTime();

        // create the inputs
        final DoubleInput startTimeInput
                = new DoubleInput("Start Time:", startTime);
        final DoubleInput endTimeInput
                = new DoubleInput("End Time:", endTime);

        // OK button
        JButton okButton = new JButton("Ok");//null;
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // retrieve the values from the text inputs
                startTime = startTimeInput.getValue();
                endTime = endTimeInput.getValue();
                // make sure the values are valid
                if (startTime != parent.getStartTime()) {
                    parent.setStartTime(startTime);
                }
                if (endTime != parent.getEndTime()) {
                    parent.setEndTime(endTime);
                }
                // revalidate the data and close the frame
                parent.recreateTimeBlocks();
                parent.repaintDrawingArea();
                frame.dispose();
            }
        });

        // Cancel button
        JButton cancelButton = new JButton("Cancel");//null;
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // do nothing and close the frame
                frame.dispose();
            }
        });

        // Create panel for the inputs
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // label at top of dialog
        JLabel label =
                new JLabel("Please choose the Start and End times (ns).");
        box.add(label);

        // add the text inputs
        box.add(startTimeInput, BorderLayout.CENTER);
        box.add(endTimeInput, BorderLayout.CENTER);

        // add the label and text inputs into the panel
        timePanel.setLayout(new BorderLayout());
        timePanel.add(box, BorderLayout.NORTH);

        // create a 1x2 grid for the buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));

        // add the buttons to the panel
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        // add the button panel into the rest of the dialog
        timePanel.add(buttonPanel, BorderLayout.SOUTH);

        // add the panel into the dialog's main panel
        setLayout(new BorderLayout());
        add(timePanel, BorderLayout.CENTER);

    }
}
