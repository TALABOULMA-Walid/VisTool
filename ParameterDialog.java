/* ParameterDialog.java
 *
 * Revised by Vincent Chan, January 10, 2006
 * Revised by Vincent Chan, August 8, 2005
 * Created by Austin Lanham, May 2, 2005
 *
 * This function creates a panel that holds the various
 * input fields necessary to specify the DRAM parameters.
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class ParameterDialog extends JPanel {
    JFrame frame;     // frame object that holds this panel
    int dramType;     // 0: SDRAM, 1: DDR, 2: DDR2
    double clockPeriod; // period of one DRAM clock cycle (ns)
    int	burstLength;  // length of the bursts in clocks
    double tRC;       // Timing specs (in ns)
    double tRAS;
    double tCAS;
    double tRCD;
    double tFAW;
    double tDQS;
    double tWR;
    double tRP;
    int num_rank;
    int num_bank;

    // number of DRAM types (currently 3 are supported)
    final static int numTypes = 3;

    // constructor creates the dialog panel
    public ParameterDialog(JFrame f, final VisTool parent) {
        this.frame = f;

        // create a panel to place the input objects
        JPanel dramPanel = new JPanel();
        dramPanel.setPreferredSize(new Dimension(300,450));

        // get current values for the timing parameters and
        // use them as the default values
        dramType = parent.getDRAMType();
        clockPeriod = parent.getClockPeriod();
        burstLength = parent.getBurstLength();
        tRC = parent.getTRC();
        tRAS = parent.getTRAS();
        tCAS = parent.getTCAS();
        tRCD = parent.getTRCD();
        tFAW = parent.getTFAW();
        tDQS = parent.getTDQS();
        tWR = parent.getTWR();
        tRP = parent.getTRP();
        num_rank = parent.getNumRank();
        num_bank = parent.getNumBank();

        // Labels for the DRAM types
        final String[] typeString =
                new String[numTypes];
        typeString[0] = "SDRAM";
        typeString[1] = "DDR";
        typeString[2] = "DDR2";

        // create an array of radio buttons for the DRAM type
        JRadioButton[] radioButton =
                new JRadioButton[numTypes];

        // create a button group for the radio buttons
        final ButtonGroup buttonGroup = new ButtonGroup();

        // these strings are returned as the radio button value.
        // Fix ---> Can replace this with typeString???
        final String[] commandString =
                new String[numTypes];

        // create the radio button and put it into the group
        for (int i=0;i<numTypes;i++) {
            commandString[i] = ("command"+i);
            radioButton[i] = new JRadioButton(typeString[i]);
            radioButton[i].setActionCommand(commandString[i]);
            buttonGroup.add(radioButton[i]);
        }

        // set the current type as the default (if it's valid)
        if (dramType < 0 || dramType >= numTypes) {
            radioButton[0].setSelected(true);
        } else {
            radioButton[dramType].setSelected(true);
        }

        // Create the various text field inputs
        final DoubleInput clockPeriodInput =
                new DoubleInput("Clock Period:", clockPeriod);
        final IntInput tBurstLengthInput =
                new IntInput("Burst Length:", burstLength);
        final DoubleInput tRCInput = new DoubleInput("Trc:", tRC);
        final DoubleInput tRASInput = new DoubleInput("Tras:", tRAS);
        final DoubleInput tCASInput = new DoubleInput("Tcas:", tCAS);
        final DoubleInput tRCDInput = new DoubleInput("Trcd:", tRCD);
        final DoubleInput tFAWInput = new DoubleInput("Tfaw:", tFAW);
        final DoubleInput tDQSInput = new DoubleInput("Tdqs:", tDQS);
        final DoubleInput tRPInput = new DoubleInput("Trp:", tRP);
        final DoubleInput tWRInput = new DoubleInput("Twr:", tWR);
        final IntInput tRANKInput = new IntInput("Number of ranks:", num_rank, " ");
        final IntInput tBANKInput = new IntInput("Number of banks:", num_bank, " ");

        // OK button
        JButton okButton = new JButton("Ok");//null;
        okButton.addActionListener(new ActionListener() {
            // this is what is run when the button is hit
            public void actionPerformed(ActionEvent e) {
                String command =
                        buttonGroup.getSelection().getActionCommand();

                // determine which radio button is selected
                for (int j=0;j<numTypes;j++) {
                    if (command.compareTo(commandString[j])==0) {
                        dramType = j;
                    }
                }

                // get the values from the text inputs
                parent.setDRAMType(dramType);
                burstLength= tBurstLengthInput.getValue();
                tRC = tRCInput.getValue();
                tRAS = tRASInput.getValue();
                tCAS = tCASInput.getValue();
                tRCD = tRCDInput.getValue();
                tFAW = tFAWInput.getValue();
                tDQS = tDQSInput.getValue();
                tRP = tRPInput.getValue();
                tWR = tWRInput.getValue();
                clockPeriod = clockPeriodInput.getValue();
                num_rank = tRANKInput.getValue();
                num_bank = tBANKInput.getValue();

                // set the values in the VisTool class
                parent.setClockPeriod(clockPeriod);
                parent.setBurstLength(burstLength);
                parent.setTRC(tRC);
                parent.setTRAS(tRAS);
                parent.setTCAS(tCAS);
                parent.setTRCD(tRCD);
                parent.setTFAW(tFAW);
                parent.setTDQS(tDQS);
                parent.setTRP(tRP);
                parent.setTWR(tWR);
                parent.setNumRank(num_rank);
                parent.setNumBank(num_bank);

                // redraw and validate
                parent.validateDRAMType();
                parent.validateClocks();
                parent.recreateTimeBlocks();
                parent.repaintDrawingArea();

                // close the dialog
                frame.dispose();
            }
        });

        // Cancel button
        JButton cancelButton = new JButton("Cancel");//null;
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // don't do anything just close the dialog
                frame.dispose();
            }
        });

        // Create panel for the inputs as a 'box' which stacks
        // each added element vertically
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        // Label at top of dialog
        JLabel label =
                new JLabel("Please select one of the following");
        box.add(label);

        // add the radio buttons one by one
        for (int k=0;k<numTypes;k++) {
            box.add(radioButton[k]);
        }

        // add the text inputs
        box.add(clockPeriodInput, BorderLayout.CENTER);
        box.add(tBurstLengthInput, BorderLayout.CENTER);
        box.add(tRCInput, BorderLayout.CENTER);
        box.add(tRASInput, BorderLayout.CENTER);
        box.add(tCASInput, BorderLayout.CENTER);
        box.add(tRCDInput, BorderLayout.CENTER);
        box.add(tFAWInput, BorderLayout.CENTER);
        box.add(tDQSInput, BorderLayout.CENTER);
        box.add(tRPInput, BorderLayout.CENTER);
        box.add(tWRInput, BorderLayout.CENTER);
        box.add(tRANKInput, BorderLayout.CENTER);
        box.add(tBANKInput, BorderLayout.CENTER);

        // add the 'box' to the dramPanel panel
        dramPanel.setLayout(new BorderLayout());
        dramPanel.add(box, BorderLayout.NORTH);

        // create a new panel for the buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        // add the buttons to the drawPanel
        dramPanel.add(buttonPanel, BorderLayout.SOUTH);

        // add the dramPanel into the main dialog panel
        setLayout(new BorderLayout());
        add(dramPanel, BorderLayout.CENTER);
    }
}
