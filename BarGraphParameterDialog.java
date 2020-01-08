/* BarGraphParameterDialog.java
 *
 *
 * Revised by Vincent Chan, January 10, 2006
 * Created by Austin Lanham, May 7, 2005
 *
 * This class constructs and implements the
 * bar graph parameter dialog which allows a
 * user to input the various bar graph parameters.
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class BarGraphParameterDialog extends JPanel {

    JFrame frame;       // the frame the dialog is contained in
    int	yScale;         // input values
    int yMajorTick;
    int yMinorTick;
    double xMajorTick;
    double xMinorTick;

    boolean[] commandMask;  // for check box group
    int numCommands;        // number of check boxes

    public BarGraphParameterDialog(JFrame f, final VisTool parent) {
        this.frame = f;

        // panel for the dialog's elements
        JPanel barPanel = new JPanel();
        barPanel.setPreferredSize(new Dimension(300,450));

        // set the number of commands and initialize check boxes
        numCommands = parent.getNumCommands();
        commandMask = new boolean[numCommands];

        for (int i=0;i<numCommands;i++)
            commandMask[i]=parent.getCommandMask(i);

        final String[] commandString =
                new String[numCommands];

        // get the names of the commands (check boxes)
        final JCheckBox checkBox[] = new JCheckBox[numCommands];
        for (int i=0;i<numCommands;i++) {
            commandString[i] = parent.getCommandString(i);
            checkBox[i] = new JCheckBox(commandString[i],
                    commandMask[i]);
        }

        // get default values for the text inputs
        yScale = parent.getYScale();
        yMajorTick = parent.getYMajorTick();
        yMinorTick = parent.getYMinorTick();
        xMajorTick = parent.getXMajorTick();
        xMinorTick = parent.getXMinorTick();

        // create the text area input fields
        final IntInput yScaleInput
                = new IntInput("Max Y value:", yScale," ");
        final IntInput yMajorInput
                = new IntInput("Y Major Tick:", yMajorTick," ");
        final IntInput yMinorInput
                = new IntInput("Y Minor Tick:", yMinorTick," ");
        final DoubleInput xMajorInput
                = new DoubleInput("X Major Tick:", xMajorTick);
        final DoubleInput xMinorInput
                = new DoubleInput("X Minor Tick:", xMinorTick);

        // OK button
        JButton okButton = new JButton("Ok");//null;
        okButton.addActionListener(new ActionListener() {
            // what to do if the button is pressed:
            public void actionPerformed(ActionEvent e) {
                int numSelected = 0;
                // see which check boxes are checked
                for (int i=0;i<numCommands;i++) {
                    if (checkBox[i].isSelected()) {
                        commandMask[i]=true;
                        numSelected++;
                    } else
                        commandMask[i]=false;
                }
                // make sure at least one box is checked
                if (numSelected == 0) {
                    parent.
                            displayErrorMessage("Command Mask Error",
                            "Must select a command.");
                    frame.dispose();
                    return;
                }

                // Commit Command Mask
                for (int i=0;i<numCommands;i++) {
                    parent.setCommandMask(i,commandMask[i]);
                }

                // get the values entered into the check boxes
                yScale = yScaleInput.getValue();
                yMajorTick = yMajorInput.getValue();
                yMinorTick = yMinorInput.getValue();
                xMajorTick = xMajorInput.getValue();
                xMinorTick = xMinorInput.getValue();

                // make sure it's valid
                if (yScale < yMajorTick) {
                    parent.displayErrorMessage("Input Error",
                            "Max Y value must be > Y Major Tick.");
                    frame.dispose();
                    return;
                }

                // make sure the major and minor ticks are multiples
                double multTest = (double)(yMajorTick)/(double)(yMinorTick);
                if (Math.floor(multTest) != Math.ceil(multTest)) {
                    parent.displayErrorMessage("Input Error",
                            "Y Major Tick must be a \n"+
                            "a multiple of Y Minor Tick.");
                    frame.dispose();
                    return;
                }

                multTest = xMajorTick/xMinorTick;
                if (Math.floor(multTest) != Math.ceil(multTest)) {
                    parent.displayErrorMessage("Input Error",
                            "X Major Tick must be a \n"+
                            "a multiple of X Minor Tick.");
                    frame.dispose();
                    return;
                }

                // commit the values
                parent.setYScale(yScale);
                parent.setYMajorTick(yMajorTick);
                parent.setYMinorTick(yMinorTick);
                parent.setXMajorTick(xMajorTick);
                parent.setXMinorTick(xMinorTick);

                // revalidate the tool and close the frame
                parent.recreateStatsVector();
                parent.repaintDrawingArea();
                frame.dispose();
            }
        });

        // Cancel button
        JButton cancelButton = new JButton("Cancel");//null;
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        // Create panel to place inputs
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // add label to the top
        JLabel label =
                new JLabel("Please select which bars to show:");
        box.add(label);

        // add check boxes
        for (int k=0;k<numCommands;k++) {
            box.add(checkBox[k], BorderLayout.CENTER);
        }

        // add text inputs
        box.add(yScaleInput, BorderLayout.CENTER);
        box.add(yMajorInput, BorderLayout.CENTER);
        box.add(yMinorInput, BorderLayout.CENTER);
        box.add(xMajorInput, BorderLayout.CENTER);
        box.add(xMinorInput, BorderLayout.CENTER);

        barPanel.setLayout(new BorderLayout());
        barPanel.add(box, BorderLayout.NORTH);

        // create panel for the ok and cancel buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        barPanel.add(buttonPanel, BorderLayout.SOUTH);

        // add all the elements to this panel
        setLayout(new BorderLayout());
        add(barPanel, BorderLayout.CENTER);

    }
}
