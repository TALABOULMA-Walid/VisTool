/* DisplayDialog.java
 *
 * Revised by Vincent Chan, January 12, 2006
    - DisplayDialog.java no longer needed
 * Revised by Vincent Chan, August 8, 2005
 * Created by Austin Lanham, May 2, 2005
 *
 * This class implements a dialog with radio buttons
 * that select the view of the main panel (drawingArea).
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class DisplayDialog extends JPanel {

    JFrame frame;  // the frame that holds the dialog
    int choice;    // the current selected radio button
    final static int numChoices = 6; // number of radio buttons

    public DisplayDialog(JFrame f, int defaultChoice, final VisTool parent) {
        this.frame = f;
        this.choice = -1;

        // create the panel which holds all the objects
        JPanel choicePanel = new JPanel();
        choicePanel.setPreferredSize(new Dimension(230,200));

        // the labels for the radio buttons
        final String[] choiceString =
                new String[numChoices];
        choiceString[0] = "Introduction";
        choiceString[1] = "Timing Diagram";
        choiceString[2] = "Timing Diagram: Expand Banks";
        choiceString[3] = "Timing Diagram: Expand Utilization";
        choiceString[4] = "Timing Diagram: Expand All";
        choiceString[5] = "Stats Bar Graph";

        // create the radio button objects in an array
        JRadioButton[] radioButton =
                new JRadioButton[numChoices];
        // create a button group for the radio buttons
        final ButtonGroup buttonGroup = new ButtonGroup();

        // the ok button
        JButton okButton = new JButton("Ok");

        // strings hold the value returned by the radio button
        final String[] commandString =
                new String[numChoices];

        // set each radio button's return value and add to button group
        for (int i=0;i<numChoices;i++) {
            commandString[i] = ("command"+i);
            radioButton[i] = new JRadioButton(choiceString[i]);
            radioButton[i].setActionCommand(commandString[i]);
            buttonGroup.add(radioButton[i]);
        }

        // select the default radio button if it's valid
        if (defaultChoice < 0 || defaultChoice >= numChoices) {
            radioButton[0].setSelected(true);
        } else {
            radioButton[defaultChoice].setSelected(true);
        }

        // OK button
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // the ok button is pressed
                // get the return string of the radio button
                String command =
                        buttonGroup.getSelection().getActionCommand();

                // determine which radio button was selected
                for (int j=0;j<numChoices;j++) {
                    if (command.compareTo(commandString[j])==0)
                        choice = j;
                }

                // anything special goes here
                if (command == choiceString[0]) {

                }

                // change the view and close the dialog
                parent.acceptDisplayDialog(choice);
                frame.dispose();
            }
        });

        // Create panel for the buttons and label
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // the label at the top of the dialog
        JLabel label =
                new JLabel("  Please select one of the following:");
        box.add(label);

        // add all the radio buttons
        for (int k=1;k<numChoices;k++) {
            box.add(radioButton[k]);
        }

        // add the label, radio buttons, and ok button to the panel
        choicePanel.setLayout(new BorderLayout());
        choicePanel.add(box, BorderLayout.NORTH);
        choicePanel.add(okButton, BorderLayout.SOUTH);

        // add the panel to the main dialog panel
        setLayout(new BorderLayout());
        add(choicePanel, BorderLayout.CENTER);

    }

    // return the index of the selected radio button
    public int getChoice() {
        return choice;
    }
}
