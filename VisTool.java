/* VisTool.java
 *
 * Revised by Vincent Chan, January 12, 2006
    - updated data and parameter file selection to remember last directory used
    - updated output file selection to remember last directory used
    - intro page option moved from display to help
    - intro page centers if scroll bar has been moved
    - in view menu, zoom labels have been expanded to include shortcuts
    - cleared side-bar at introduction screen
    - moved display options from popup into the view menu,
        now DisplayDialog.java is no longer needed.

 * Revised by Vincent Chan, August 8, 2005
 * Created by Austin Lanham, May 2, 2005
 *
 * This class implements a frame which holds the
 * sim-DRAM visualization tool.  All of the programs
 * functionality is controled within this class.
 */

// include needed packages
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import java.awt.geom.*;
import java.awt.image.*;
import javax.imageio.*;

public class VisTool extends JPanel {

    private Dimension size; // indicates size taken up by graphics
    private Vector timeBlockVector;   //  holds the time blocks

    private boolean INPUT_READ = false;


    public File saved_directory; //file to save last directory accessed

    // General Program Parameters
    private int displayContent;
    final static int INTRO         = 0;
    final static int TIMING_DIAG   = 1;
    final static int TIMING_DIAG_EXP_BANK = 2; //expanded bank utilization
    final static int TIMING_DIAG_EXP_UTIL = 3; //expanded device utilization
    final static int TIMING_DIAG_EXP_ALL = 4; //expanded both bank and device
    final static int STATS_GRAPH   = 5;

    // Stats/Bar Graph Variables
    private Vector statsVector; // holds command/conflict count
    private boolean[] statsCommandMask;  // mask out unwanted commands
    private String[] statsCommandString; // command labels
    private int barYScale;          // Max value on the Y-axis
    private int barYMajorTick;      // Major tick on the Y-axis
    private int barYMinorTick;      // Minor tick on the Y-axis
    private double barXMajorTick;   // Major tick on the X-axis (time)
    private double barXMinorTick;   // Minor tick on the X-axis
    private double barPixelsPerNS;  // for scaling/zooming

    // Graphics parameters
    private double pixelsPerNanoSecond; // for scaling/zooming
    private double timingStartTime; // start time of data
    private double timingEndTime;   // end time of...
    private int rowHeight = 20;     // for timing diagram spacing
    private int rowSpacing = 5;     // "                        "
    private int colSpacing = 20;    // "                        "
    private int fontSize;           // holds size of current font
    private int busLabelFontSize = 10;   // hold size of bus label font
    private int busLabelAreaWidth = 120; // holds size of the labels
    private int drawLabelAreaHeight = 400; // holds size of the labels
    private double timingXMajorTick; // the X-axis major tick (time)
    private double timingXMinorTick; // the X-axis minor tick
    final static int DEF_AREA_HEIGHT = 400; // default size of graphics window

    // DRAM parameters (in ns)
    private double tRC;
    private double tRAS;
    private double tCAS;
    private double tRCD;
    private double tBURST;
    private double tFAW;    // not used presently
    private double tCWD;
    private double tDQS;
    private double tWR;
    private double tRP;
    private int num_rank;
    private int num_bank;

    // DRAM parameters (# clocks)
    private int tRCclocks;
    private int tRASclocks;
    private int tCASclocks;
    private int tRCDclocks;
    private int tBURSTclocks;
    private int tFAWclocks;
    private int tCWDclocks;
    private int tDQSclocks;
    private int tWRclocks;
    private int tRPclocks;
    private int refreshClocks;
    private int burstLength;
    private double clockPeriod;


    // Specifies the DRAM technology
    private int dramType;
    final static int SDRAM = 0;
    final static int DDR   = 1;
    final static int DDR2  = 2;

    // Command Types
    public static final int NONE = 0;
    public static final int WRITE = 3;
    public static final int PRECHARGE = 4;
    public static final int ROWACT = 1;
    public static final int REFRESH = 5;
    public static final int READ = 2;
    public static final int BANKCONFLICT = 6;
    public static final int TFAWCONFLICT = 7; // not in present simulator
    public static final int NUM_COMMANDS = 6;

    // Timing Diagram Bus locations
    final static int CLOCK     = 0;
    final static int CMD_BUS   = 1;
    final static int BANK_UTIL = 2;
    final static int DEV_UTIL  = 3;
    final static int DATA_BUS  = 4;
    final static int TRANS_ID  = 5;
    final static int CONFLICT  = 6;
    final static int NUM_BUSSES= 7;

    // Type of output to print to
    final static int isJPEG = 0;
    final static int isPNG = 1;

    final String[] busLabel =
            new String[NUM_BUSSES];

    // Various swing components for constructing GUI
    JPanel drawingArea;   // the drawing pane
    JPanel busLabelArea;  // Timing Diagram Bus Labels
    JScrollPane scroller; // scroll pane inside drawingArea

//
//
// class constructor
//
//
    public VisTool() {
        super();

        // initialize
        setOpaque(true);
        size = new Dimension(0,0);
        timeBlockVector = new Vector();

        pixelsPerNanoSecond = 10;
        timingXMajorTick = 1000;
        timingXMinorTick = 100;

        //default values
        displayContent = INTRO;
        dramType = DDR;
        clockPeriod = 1;
        burstLength = 16;
        tRC = 14.0;
        tRAS = 10.0;
        tCAS = 10.0;
        tRCD = 10.0;
        tFAW = 0.0;
        tDQS = 2.0;
        tWR =  2.0;
        tRP = 4.0;
        num_rank = 4;
        num_bank = 4;

        // calculate # of clock cycles for commands
        validateDRAMType();
        validateClocks();

        timingStartTime = 0.0;
        timingEndTime = 40000.0;

        // initialize the stats vector
        statsVector = new Vector();
        statsCommandString = new String[NUM_COMMANDS];
        statsCommandMask = new boolean[NUM_COMMANDS];
        for (int o=0;o<NUM_COMMANDS;o++) {
            statsCommandMask[o] = true;
            switch (o+1) { // 0 = NONE
                case ROWACT:
                    statsCommandString[o] = "RAS"; break;
                case READ:
                    statsCommandString[o] = "CAS READ"; break;
                case WRITE:
                    statsCommandString[o] = "CAS WRITE"; break;
                case PRECHARGE:
                    statsCommandString[o] = "PRECHARGE"; break;
                case REFRESH:
                    statsCommandString[o] = "REFRESH"; break;
                case BANKCONFLICT:
                    statsCommandString[o] = "BANK CONFLICT"; break;
                case TFAWCONFLICT:
                    statsCommandString[o] = "Tfaw CONFLICT"; break;
                default:
                    statsCommandString[o] = "?"; break;
            }
        }

        // Default settings for bar graph
        barYScale = 50;
        barYMajorTick = 5;
        barYMinorTick = 1;
        barXMajorTick = (timingEndTime-timingStartTime)/5;
        barXMinorTick = (timingEndTime-timingStartTime)/25;
        barPixelsPerNS = 1;

        busLabel[0] = "         Clock";
        busLabel[1] = "   Command Bus";
        busLabel[2] = "  Bank Utilization";
        busLabel[3] = "Device Utilization";
        busLabel[4] = "       Data Bus";
        busLabel[5] = "   Transaction ID";
        busLabel[6] = "       Conflicts";


        //Set up the drawing area
        drawingArea = new JPanel() {

            // This function will paint the panel depending on the
            // view the user has selected.
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                switch (displayContent) {
                    case INTRO:
                        paintIntroPanel(g);
                        drawLabelAreaHeight = DEF_AREA_HEIGHT;
                        break;

                    case STATS_GRAPH:
                        paintStatsGraph(g);
                        drawLabelAreaHeight = DEF_AREA_HEIGHT;
                        break;

                    case TIMING_DIAG:
                        paintTimingDiagram(g);
                        drawLabelAreaHeight =
                                (NUM_BUSSES+NUM_COMMANDS)*(rowHeight+rowSpacing)
                                + rowHeight;
                        break;

                    case TIMING_DIAG_EXP_BANK:
                        paintTimingDiagram(g);
                        drawLabelAreaHeight =
                                (NUM_BUSSES+NUM_COMMANDS+num_rank*num_bank)*
                                (rowHeight+rowSpacing)+rowHeight;
                        break;

                    case TIMING_DIAG_EXP_UTIL:
                        paintTimingDiagram(g);
                        drawLabelAreaHeight =
                                (NUM_BUSSES+NUM_COMMANDS+num_rank)*
                                (rowHeight+rowSpacing)+rowHeight;
                        break;

                    case TIMING_DIAG_EXP_ALL:
                        paintTimingDiagram(g);
                        drawLabelAreaHeight = (NUM_BUSSES+NUM_COMMANDS+
                                num_rank*num_bank+num_rank)*
                                (rowHeight+rowSpacing)+rowHeight;
                        break;

                    default:
                        System.out.println("Error in paintComponent");
                        break;
                }
                busLabelArea.setPreferredSize(
                        new Dimension(busLabelAreaWidth, drawLabelAreaHeight));
                busLabelArea.revalidate();
            }
        };

        // This is the side bar where the bus labels and key go
        busLabelArea = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setFont(new Font("Arial", Font.PLAIN,
                        busLabelFontSize));

                g.setColor(Color.black);

                String rank_bank_label;
                switch(displayContent) {

                    case INTRO:
                        // leave empty
/*
                        g.drawString("This is the side bar",
                                colSpacing,
                                rowSpacing+busLabelFontSize);
                        g.drawString("which holds some",
                                colSpacing,
                                rowSpacing*2+busLabelFontSize*2);
                        g.drawString("useful information.",
                                colSpacing,
                                rowSpacing*3+busLabelFontSize*3);
*/
                        break;

                    default:
                        break;

                    case STATS_GRAPH:
                        paintStatsSideBar(g);
                        break;

                    case TIMING_DIAG:
                        for (int i=0;i<NUM_BUSSES;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing,
                                    i*(rowHeight+rowSpacing)+rowHeight);
                        }

                        paintCommandKey(g,10,
                                (NUM_BUSSES+2)*(rowHeight+rowSpacing));
                        break;

                    case TIMING_DIAG_EXP_BANK:
                        // print clock and command & address bus
                        for (int i=0;i<2;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing,
                                    i*(rowHeight+rowSpacing)+rowHeight);
                        }

                        // print expanded banks
                        for (int j=0;j<num_rank;j++) {
                            for (int k = 0;k<num_bank;k++) {
                                rank_bank_label =
                                        "  Rank[" + j + "] Bank[" + k + "]";
                                g.drawString(rank_bank_label,
                                        colSpacing, (2+j*num_rank+k)*
                                        (rowHeight+rowSpacing)+rowHeight);
                            }
                        }

                        // print device util, data bus, trans id, bank conflicts
                        for (int i=3;i<NUM_BUSSES;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing, (i-1+num_rank*num_bank)*
                                    (rowHeight+rowSpacing)+rowHeight);
                        }

                        paintCommandKey(g,10, (NUM_BUSSES+num_rank*num_bank+1)*
                                (rowHeight+rowSpacing));
                        break;

                    case TIMING_DIAG_EXP_UTIL:
                        // print clock, command & address bus, single bank row
                        for (int i=0;i<3;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing,
                                    i*(rowHeight+rowSpacing)+rowHeight);
                        }

                        // print expanded device utilization
                        for (int j=0;j<num_rank;j++) {
                            rank_bank_label = "     Util: Rank[" + j + "]";
                            g.drawString(rank_bank_label,
                                    colSpacing,
                                    (3+j)*(rowHeight+rowSpacing)+rowHeight);
                        }

                        // print data bus, trans id, bank conflicts
                        for (int i=4;i<NUM_BUSSES;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing, (i-1+num_rank)*
                                    (rowHeight+rowSpacing)+rowHeight);
                        }

                        paintCommandKey(g,10,
                                (NUM_BUSSES+num_rank+1)*(rowHeight+rowSpacing));
                        break;

                    case TIMING_DIAG_EXP_ALL:
                        // print clock and command & address bus
                        for (int i=0;i<2;i++) {
                            g.drawString(busLabel[i],
                                    colSpacing,
                                    i*(rowHeight+rowSpacing)+rowHeight);
                        }

                        // print expanded banks
                        for (int j=0;j<num_rank;j++) {
                            for (int k = 0;k<num_bank;k++) {
                                rank_bank_label =
                                        "  Rank[" + j + "] Bank[" + k + "]";
                                g.drawString(rank_bank_label,
                                        colSpacing, (2+j*num_rank+k)*
                                        (rowHeight+rowSpacing)+rowHeight);
                            }
                        }

                        // print expanded device utilization
                        for (int l=0;l<num_rank;l++) {
                            rank_bank_label = "     Util: Rank[" + l + "]";
                            g.drawString(rank_bank_label,
                                    colSpacing, (2+num_rank*num_bank+l)*
                                    (rowHeight+rowSpacing)+rowHeight);
                        }

                        // print data bus, trans id, bank conflicts
                        for (int m=4;m<NUM_BUSSES;m++) {
                            g.drawString(busLabel[m],
                                    colSpacing, (m-2+num_rank*num_bank+num_rank)
                                    *(rowHeight+rowSpacing)+rowHeight);
                        }

                        paintCommandKey(g,10,
                                (NUM_BUSSES+num_rank*num_bank+num_rank)*
                                (rowHeight+rowSpacing));
                        break;
                }
            }
        };

        // Define panel parameters
        busLabelArea.setPreferredSize(
                new Dimension(busLabelAreaWidth, drawLabelAreaHeight));
        busLabelArea.setBackground(Color.white);

        drawingArea.setBackground(Color.white);
        drawingArea.addMouseListener(new MyMouseListener());

        //Put the drawing area in a scroll pane
        scroller = new JScrollPane(drawingArea);
        scroller.setPreferredSize(new Dimension(600,drawLabelAreaHeight));
        setLayout(new BorderLayout());

        // add panel to the left of the main panel
        add(busLabelArea, BorderLayout.WEST);

        // add scroll pane to center (will resize with window)
        add(scroller, BorderLayout.CENTER);
    }

//
//
// menu functions
//
//

    /* Function reads DRAM parameters from a file and
       and sets system parameters to these values */
    public void inputParamFile() throws IOException {

        JFrame frame = new JFrame();

        // Create a file chooser and set directory
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(saved_directory);

        // Show dialog; this method does not return until dialog is closed
        fc.showOpenDialog(frame);

        // Save directory for later
        saved_directory = fc.getCurrentDirectory();

        // Get the selected file
        File file = fc.getSelectedFile();

        // create buffered reader to read file
        File inputFile = fc.getSelectedFile();
        if (inputFile == null) {
            throw new NullPointerException();
        }

        BufferedReader br
                = new BufferedReader(new FileReader(inputFile));

        String line;     // the line being read
        int success = 0; // what was the outcome of parseing the line?
        int errors = 0;  // how many errors were encountered?

        // Prompts the user is they wish to import new data
        int no = JOptionPane.showConfirmDialog
                (this,
                "All current parameters will be replaced.\n"+
                "Proceed anyway?",
                "Import New Parameters?",
                JOptionPane.YES_NO_OPTION);

        if (no == 1) {
            br.close();
            return;
        }

        int p; // position in string
        int s; // position of next space, set to -1 for first character
        int l = 0; // length of string
        String temp_para_name;
        String temp_value;

        // read lines until the file is exhausted
        while ((line=br.readLine())!=null) {
            if (line.length() != 0) {

                // get parameter name
                line = line.trim();
                l = line.length();
                p = 0;
                s = line.indexOf(' ');
                if (s == -1)
                    s = line.indexOf('\t');
                temp_para_name = line.substring(p,s).trim();

                // get parameter value
                p = s+1;
                temp_value = line.substring(p,l).trim();

//                System.out.println(temp_para_name + " = " + temp_value + '.');

                if (temp_para_name.compareTo("type")==0) {
                    if (temp_value.compareTo("SDRAM")==0) {
                        success++;
                        dramType = SDRAM;
                    } else if (temp_value.compareTo("DDR")==0) {
                        dramType = DDR;
                        success++;
                    } else if (temp_value.compareTo("DDR2")==0) {
                        dramType = DDR2;
                        success++;
                    } else
                        errors++;
                } else if (temp_para_name.compareTo("clockPeriod")==0) {
                    clockPeriod = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("burstLength")==0) {
                    burstLength = Integer.parseInt(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tRC")==0) {
                    tRC = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tRAS")==0) {
                    tRAS = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tCAS")==0) {
                    tCAS = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tRCD")==0) {
                    tRCD = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tFAW")==0) {
                    tFAW = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tDQS")==0) {
                    tDQS = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tWR")==0) {
                    tWR = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("tRP")==0) {
                    tRP = Double.parseDouble(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("num_rank")==0) {
                    num_rank = Integer.parseInt(temp_value);
                    success++;
                } else if (temp_para_name.compareTo("num_bank")==0) {
                    num_bank = Integer.parseInt(temp_value);
                    success++;
                } else {
                    errors++;
                }
            }
        }

        // file is empty, report the number of successful commands
        displayInfoMessage("Import completed.", success +
                " parameters were read " + "successfully.\n" +
                "There were " + errors + " errors.");
        recreateStatsVector();
        repaintDrawingArea();
        br.close();
    }

    /* This funtion will take one line of the input data and
       store the values in a TimeBlock found in the TimeBlockVector.
       The function works by looking for spaces (' ') in the input
       line and parsing the characters it find between spaces. */

    public int parseInputLine(String line, int lineNum) {
        int p = 0; // position in string
        int s = 0; // position of next space
        int l = line.length(); // length of the line

        // TimeBlock values
        double time;
        int type;
        int transID;
        int rank;
        int bank;
        int row;
        int col;

        // find first space
        s = line.indexOf(' ');
        // verify the first space is valid
        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        // store the first word as a double into the time variable
        // FIX: catch NumberFormatException
        time = Double.parseDouble(line.substring(p,s));

        // See if the command is in the time window of interest
        // if not, ignore.
        if (time < timingStartTime || time > timingEndTime) {
            return 2; // fix this
        }

        // reposition line pointers to find second word
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');

        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }

        // Match string with command types
        if (line.substring(p,s).compareTo("ROWACT")==0) {
            type = ROWACT;
        } else if (line.substring(p,s).compareTo("READ")==0) {
            type = READ;
        } else if (line.substring(p,s).compareTo("WRITE")==0) {
            type = WRITE;
        } else if (line.substring(p,s).compareTo("PRECHARGE")==0) {
            type = PRECHARGE;
        } else if (line.substring(p,s).compareTo("REFRESH")==0) {
            type = REFRESH;
        } else if (line.substring(p,s).compareTo("BANKCONFLICT")==0) {
            type = BANKCONFLICT;
        }
        /*
        else if (line.substring(p,s).compareTo("ROWCONFLICT")==0) {
            type = ROWCONFLICT;
            }*/
        else if (line.substring(p,s).compareTo("TFAWCONFLICT")==0) {
            type = TFAWCONFLICT;
        } else {
            displayErrorMessage("Input Error",
                    "Command '"+line.substring(p,s)+
                    "' not supported.");
            return 0;
        }

        // find transaction id
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');

        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        transID = Integer.parseInt(line.substring(p,s));

        // find rank
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');

        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        rank = Integer.parseInt(line.substring(p,s));

        // find bank
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');

        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        bank = Integer.parseInt(line.substring(p,s));

        // find row
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');

        if (s <= p || s >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        row = Integer.parseInt(line.substring(p,s));

        // find column
        p=s+1;
        s = p+(line.substring(p,l)).indexOf(' ');
        if (s < p) s = l;

        if (p >= l) {
            displayErrorMessage("Input Error",
                    "Error parsing input on line "+
                    lineNum + ".");
            return 0;
        }
        col = Integer.parseInt(line.substring(p,s));

        // determine if vector must resize or not
        if (timeBlockVector.size() == timeBlockVector.capacity()) {
            timeBlockVector.ensureCapacity(timeBlockVector.size()*2);
        }

        // create the time blocks from associated command
        createTimeBlocks(time,type,transID,rank,bank,row,col);

        return 1;
    }

    /* This function prompts the user for an input file, reads lines from the
       file and sends them to parseInputLine for processing.  */
    public void inputFile() throws IOException {

        JFrame frame = new JFrame();

        // Create a file chooser and set directory
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(saved_directory);

        // Show dialog; this method does not return until dialog is closed
        fc.showOpenDialog(frame);

        // Save directory for later
        saved_directory = fc.getCurrentDirectory();

        // Get the selected file
        File file = fc.getSelectedFile();

        // create buffered reader to read file
        File inputFile = fc.getSelectedFile();
        if (inputFile == null) {
            throw new NullPointerException();
        }
        //File inputFile = new File(inputFileName);
        BufferedReader br
                = new BufferedReader(new FileReader(inputFile));
        String line;     // the line being read
        int success = 0; // what was the outcome of parseing the line?
        int num = 1;     // how many commands were read?
        int errors = 0;  // how many errors were encountered?

        // Prompts the user is they wish to import new data
        int no = JOptionPane.showConfirmDialog
                (this,
                "All current data will be removed.\n"+
                "Proceed anyway?",
                "Import New Data?",
                JOptionPane.YES_NO_OPTION);

        if (no == 1) {
            br.close();
            return;
        }

        // clear the timeBlockVector for the new data
        timeBlockVector.removeAllElements();

        // read lines until the file is exhausted
        while ((line=br.readLine())!=null) {
            // valid line found, parse it
            success = parseInputLine(line,num);
            if (success == 1) {
                num++;
            }
            // If the command is out of the time range
            else if (success == 2) { // fix this
                // Out of time range
            }
            // input error was found
            else {
                errors++;
                if (errors > 5) {
                    displayErrorMessage("Too Many Errors",
                            "Aborting file input.");
                    br.close();
                    return;
                }
            }
        }

        INPUT_READ = true;

        // file is empty, report the number of successful commands
        displayInfoMessage("Data Input Success",
                "You have imported "+
                (num-1) +
                " command(s) successfully!");

        recreateStatsVector();
        repaintDrawingArea();
        br.close();
    }

    /* Function prints the current frame to a graphics file based on the type */
    public void printToFile(int type, JFrame inFrame) {
        try {
            JFrame f = new JFrame();
            FileDialog fd = new FileDialog(f, "Save as ", FileDialog.SAVE);
            fd.setDirectory(saved_directory.getAbsolutePath());

            switch (type) {
                case isJPEG:
                    fd.setFile("output.jpeg");
                    break;
                case isPNG:
                    fd.setFile("output.png");
                    break;
            }
            fd.show();
            String name = fd.getDirectory() + fd.getFile();
            Rectangle r = inFrame.getBounds();
            Image image = inFrame.createImage(r.width, r.height);
            Graphics g = image.getGraphics();
            inFrame.paint(g);
            switch (type) {
                case isJPEG:
                    ImageIO.write((RenderedImage)image, "jpeg", new File(name));
                    break;
                case isPNG:
                    ImageIO.write((RenderedImage)image, "png", new File(name));
                    break;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    /* The funtion display the timing diagrams and stats graph */
    public void displayView(int x) {

        if (!INPUT_READ) {
            displayErrorMessage("ERROR!",
                "No data input yet. Use File->Input Data.");
        } else {
            // print the selected view
            displayContent = x;
            repaintDrawingArea();
        }
    }


    /* The funtion changes the pixels per nanosecond to zoom in */
    public int zoomIn(int x, int y) {
        // determine which view is current
        switch (displayContent) {
            case TIMING_DIAG:
            case TIMING_DIAG_EXP_BANK:
            case TIMING_DIAG_EXP_UTIL:
            case TIMING_DIAG_EXP_ALL:
                // zoom in by a factor of 2
                pixelsPerNanoSecond = pixelsPerNanoSecond*2;
                break;
            case STATS_GRAPH:
                // zoom in by a factor of 2
                barPixelsPerNS = barPixelsPerNS*2;
                break;
            default:
                System.out.println("Error in Zoom in");
                break;
        }
        // to change the perferred size of scroller
        size.width = size.width*2;

        // center the screen at the clicked point
        if (2*x < size.width-getWidth()/2) {
            centerDisplay(2*x,y);
        } else {
            centerDisplay(size.width-getWidth()/2,y);
        }

        // re-establish the drawing area
        drawingArea.setPreferredSize(size);
        drawingArea.revalidate();
        drawingArea.repaint();
        return 1;
    }

    /* The funtion changes the pixels per nanosecond to zoom out */
    public int zoomOut(int x, int y) {
        // determine which view is current
        switch (displayContent) {
            case TIMING_DIAG:
            case TIMING_DIAG_EXP_BANK:
            case TIMING_DIAG_EXP_UTIL:
            case TIMING_DIAG_EXP_ALL:
                // is it zoomed out too far?
                if (pixelsPerNanoSecond >= clockPeriod) {
                    // zoom out by factor of 2
                    pixelsPerNanoSecond = pixelsPerNanoSecond/2;
                    // set scroller size
                    size.width=size.width/2;
                    // center the screen around click
                    centerDisplay(x/2,y);
                    // re-establist the drawing area
                    drawingArea.setPreferredSize(size);
                    drawingArea.revalidate();
                    drawingArea.repaint();
                    return 1;
                }
                break;
            case STATS_GRAPH:
                // zoom out by factor of 2
                barPixelsPerNS = barPixelsPerNS/2;
                // set scroller size
                size.width=size.width/2;
                // center the screen around click
                centerDisplay(x/2,y);
                // re-establist the drawing area
                drawingArea.setPreferredSize(size);
                drawingArea.revalidate();
                drawingArea.repaint();
                return 1;
        }

        centerDisplay(x,y);
        return 0;
    }

    /* This function creates a frame and inserts a DisplayDialog object
       which allows the user to select what is displayed on the screen */

/*
    public void openDisplayDialog() {
        JFrame frame =
                new JFrame("DISPLAY");
        // set the layout as a simple 1x1 grid
        frame.getContentPane().setLayout(new GridLayout(1,1));
        frame.setLocation(150,150);

        // instantiate the DisplayDialog
        DisplayDialog displayDialog =
                new DisplayDialog(frame, displayContent, this);

        // add the panel into the frame
        frame.getContentPane().add(displayDialog);

        // initialize the frame
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
*/


    /* This function prints the introduction screen */

    public void displayIntro() {

        displayContent = INTRO;
        repaintDrawingArea();

        // recenter view to the beginning in case it has been moved
        Point p = scroller.getViewport().getViewPosition();
        p.setLocation(0,(int)p.getY());
        scroller.getViewport().setViewPosition(p);

    }

    /* This function creates a frame and inserts a
       TimeDialog object which allows the user
       to specify the start and end times */

    public void openTimeDialog() {

        JFrame frame =
                new JFrame("Start/End Time Input");
        // set the layout as a simple 1x1 grid
        frame.getContentPane().setLayout(new GridLayout(1,1));
        frame.setLocation(150,150);

        // instantiate the dialog
        TimeDialog timeDialog =
                new TimeDialog(frame, this);
        // add it into the frame
        frame.getContentPane().add(timeDialog);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);
    }


    /* This function creates a frame and inserts a
       DRAMParametersDialog object which allows the user
       to specify the various DRAM parameters */

    public void openDRAMParametersDialog() {
        JFrame frame =
                new JFrame("DRAM Parameters Selection");
        frame.getContentPane().setLayout(new GridLayout(1,1));
        frame.setLocation(150,150);

        // instantiate the ParameterDialog
        ParameterDialog paramDialog =
                new ParameterDialog(frame, this);
        // add it into the frame
        frame.getContentPane().add(paramDialog);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    /* This function creates a frame and inserts a
       BarGraphParametersDialog object which allows the user
       to specify the various options with displaying the bar graph */
    public void openBarGraphParameterDialog() {
        JFrame frame =
                new JFrame("Bar Graph Parameters");
        frame.getContentPane().setLayout(new GridLayout(1,1));
        frame.setLocation(150,150);

        // instaniate the dialog
        BarGraphParameterDialog barDialog =
                new BarGraphParameterDialog(frame, this);

        // add it into the frame
        frame.getContentPane().add(barDialog);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);
    }

//
//
// helper functions
//
//

    /* find the center of the viewable screen */
    public int getCenterX() {
        Point p = scroller.getViewport().getViewPosition();
        return (int)p.getX()+getWidth()/2;
    }

    /* find the center of the viewable screen */
    public int getCenterY() {
        Point p = scroller.getViewport().getViewPosition();
        return (int)p.getY()+getHeight()/2;
    }

    /* centers the viewable screen around x (not y currently) */
    public void centerDisplay(int x, int y) {
        Point p = scroller.getViewport().getViewPosition();
        int currentX = (int)p.getX();
        int newX = x - getWidth()/2;
        if (newX < 0) newX = 0;
        if (newX >= size.width) newX=size.width-1;
        p.setLocation(newX,(int)p.getY());
        scroller.getViewport().setViewPosition(p);
    }

    /* this function repaints the drawing area based on the
       display mode selected by the user. */

    public void repaintDrawingArea() {
        int w = busLabelArea.getPreferredSize().width;
        double t = timingEndTime-timingStartTime;

        switch (displayContent) {
            case STATS_GRAPH:
                // set the scroller size to match the time scale
                size.width = (int)(t*barPixelsPerNS);

                // revaildate the drawing area
                drawingArea.setPreferredSize(size);
                drawingArea.revalidate();
                drawingArea.repaint();
                break;
            case TIMING_DIAG:
            case TIMING_DIAG_EXP_BANK:
            case TIMING_DIAG_EXP_UTIL:
            case TIMING_DIAG_EXP_ALL:
                // determine the viewable area
                size.width = (int)(t*pixelsPerNanoSecond);
                drawingArea.setPreferredSize(size);
                drawingArea.revalidate();
                drawingArea.repaint();
                break;
            case INTRO:
                drawingArea.repaint();
                break;
            default:
                drawingArea.repaint();
                System.out.println("Error in repaintDrawingArea");
                break;
        }
        busLabelArea.repaint();
    }

    // Changes linked DRAM parameters if the DRAM type changes
    public void validateDRAMType() {
        switch (dramType) {
            case SDRAM:
                tCWD = 0;
                tBURST = burstLength*clockPeriod;
                break;
            case DDR:
                //tCWD = clockPeriod; Lecture 6 slide 6 say 1 clock
                // tCWD = 0;  // simulator uses 0
                tBURST = burstLength*clockPeriod/2;
                break;
            case DDR2:
                tCWD = tCAS-clockPeriod;
                tBURST = burstLength*clockPeriod/4;
                break;
        }
    }

    // Sets the start time of the data if it is valid
    public void setStartTime(double s) {
        if (s > timingEndTime) {
            displayErrorMessage("Time Input Error",
                    "Start time must be < end time.");
        } else {
            timingStartTime = s;
            recreateStatsVector();
            repaintDrawingArea();
        }
    }

    // returns the start time
    public double getStartTime() {
        return timingStartTime;
    }

    // sets the end time if it is valid
    public void setEndTime(double s) {
        if (s < timingStartTime) {
            displayErrorMessage("Time Input Error",
                    "End time must be > start time.");
        } else {
            timingEndTime = s;
            recreateStatsVector();
            repaintDrawingArea();
        }
    }

    // returns the end time
    public double getEndTime() {
        return timingEndTime;
    }

//
//
// simple functions to return values
//
//

    public int getYScale() {
        return barYScale;
    }

    public void setYScale(int s) {
        barYScale=s;
    }

    public int getYMajorTick() {
        return barYMajorTick;
    }

    public void setYMajorTick(int s) {
        barYMajorTick=s;
    }

    public int getYMinorTick() {
        return barYMinorTick;
    }

    public void setYMinorTick(int s) {
        barYMinorTick=s;
    }

    public double getXMajorTick() {
        return barXMajorTick;
    }

    public void setXMajorTick(double s) {
        barXMajorTick=s;
    }

    public double getXMinorTick() {
        return barXMinorTick;
    }

    public void setXMinorTick(double s) {
        barXMinorTick=s;
    }

    public int getNumCommands() {
        return NUM_COMMANDS;
    }

    public boolean getCommandMask(int i) {
        return statsCommandMask[i];
    }

    public void setCommandMask(int i, boolean b) {
        statsCommandMask[i]=b;
    }

    public String getCommandString(int i) {
        return statsCommandString[i];
    }

//
//
// Helper functions for ParameterDialog class
//
//

    public int getDRAMType() {
        return dramType;
    }

    public void setDRAMType(int t) {
        dramType = t;
    }

    public double getClockPeriod() {
        return clockPeriod;
    }

    public void setClockPeriod(double t) {
        clockPeriod=t;
    }

    public int getBurstLength() {
        return burstLength;
    }

    public void setBurstLength(int t) {
        burstLength = t;
    }

    public double getTRC() {
        return tRC;
    }

    public void setTRC(double t) {
        tRC=t;
    }

    public void setTRAS(double t) {
        tRAS = t;
    }

    public double getTRAS() {
        return tRAS;
    }

    public double getTCAS() {
        return tCAS;
    }

    public void setTCAS(double t) {
        tCAS=t;
    }

    public double getTRCD() {
        return tRCD;
    }

    public void setTRCD(double t) {
        tRCD=t;
    }

    public double getTFAW() {
        return tFAW;
    }

    public void setTFAW(double t) {
        tFAW=t;
    }

    public double getTDQS() {
        return tDQS;
    }

    public void setTDQS(double t) {
        tDQS=t;
    }

    public double getTWR() {
        return tWR;
    }

    public void setTWR(double t) {
        tWR=t;
    }

    public double getTRP() {
        return tRP;
    }

    public void setTRP(double t) {
        tRP=t;
    }

    public int getNumRank() {
        return num_rank;
    }

    public void setNumRank(int t) {
        num_rank = t;
    }

    public int getNumBank() {
        return num_bank;
    }

    public void setNumBank(int t) {
        num_bank = t;
    }

//
//
// more helper functions
//
//

    /* calculates how many clocks a time equates to (rounded up) */
    public void validateClocks() {
        tRCclocks = (int)(Math.ceil(tRC/clockPeriod));
        tRASclocks = (int)(Math.ceil(tRAS/clockPeriod));
        tCASclocks = (int)(Math.ceil(tCAS/clockPeriod));
        tRCDclocks = (int)(Math.ceil(tRCD/clockPeriod));
        tBURSTclocks = (int)(Math.ceil(tBURST/clockPeriod));
        tFAWclocks = (int)(Math.ceil(tFAW/clockPeriod));
        tCWDclocks = (int)(Math.ceil(tCWD/clockPeriod));
        tDQSclocks = (int)(Math.ceil(tDQS/clockPeriod));
        tWRclocks = (int)(Math.ceil(tWR/clockPeriod));
        tRPclocks = (int)(Math.ceil(tRP/clockPeriod));
        // Refresh time (Lecture 6, slide 8)
        refreshClocks = Math.max(tRASclocks+tRPclocks,tRCclocks);
    }

    /* This function sets the option selected by the
       user in the DisplayDialog. */
    public void acceptDisplayDialog(int choice) {
        displayContent = choice;
        repaintDrawingArea();
    }

    /* This function assigns colors to the various command types */
    public Color getCommandColor(int command) {
        switch (command) {
            case ROWACT:
                return Color.red;
            case READ:
                return Color.blue;
            case WRITE:
                return Color.green;
            case PRECHARGE:
                return Color.magenta;
            case REFRESH:
                return Color.orange;
            case BANKCONFLICT:
                return Color.cyan;
            /*
        case ROWCONFLICT:
            return Color.yellow;
             */
            case TFAWCONFLICT:
                return Color.pink;
            default:
                return Color.black;
        }
    }

    /* This function will draw a block of time for Timing Diagram mode.
       It takes into account time (width), color, and bus */
    public void drawTimeBlock(Graphics g, TimeBlock temp) {
        int c = (int)(pixelsPerNanoSecond*clockPeriod);

        fontSize = g.getFont().getSize();
        double t = temp.getStartTime()-timingStartTime;
        int type = temp.getType();

        // determine where to put timeblocks
        int x_coord = 0;
        int y_coord = 0;

        // calculate the width of the block in pixels
        int w = (int)((temp.getEndTime()-temp.getStartTime())
        *pixelsPerNanoSecond)-1;
        g.setColor(temp.getColor());

        // determine x and y coordinates
        switch (displayContent) {
            case TIMING_DIAG:
                x_coord = colSpacing+(int)(pixelsPerNanoSecond*t);
                y_coord = temp.getBusID()*(rowHeight+rowSpacing)+rowSpacing;
                break;

            case TIMING_DIAG_EXP_BANK:
                x_coord = colSpacing+(int)(pixelsPerNanoSecond*t);
                switch (temp.getBusID()) {
                    case CMD_BUS:
                        y_coord = temp.getBusID()*
                                (rowHeight+rowSpacing)+rowSpacing;
                        break;
                    case BANK_UTIL:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (temp.getRank()*num_rank + temp.getBank())
                                *(rowHeight+rowSpacing) + rowSpacing;
                        break;
                    default:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (num_rank*num_bank-1)* (rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                }
                break;

            case TIMING_DIAG_EXP_UTIL:
                x_coord = colSpacing+(int)(pixelsPerNanoSecond*t);
                switch (temp.getBusID()) {
                    case CMD_BUS:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                    case BANK_UTIL:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                    case DEV_UTIL:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (int) (temp.getRank())*(rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                    default:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (int) (num_rank-1)*(rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                }
                break;

            case TIMING_DIAG_EXP_ALL:
                x_coord = colSpacing+(int)(pixelsPerNanoSecond*t);
                switch (temp.getBusID()) {
                    case CMD_BUS:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                rowSpacing;
                        break;
                    case BANK_UTIL:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (temp.getRank()*num_rank + temp.getBank())*
                                (rowHeight+rowSpacing) + rowSpacing;
                        break;
                    case DEV_UTIL:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (num_rank*num_bank-1+temp.getRank())*
                                (rowHeight+rowSpacing) + rowSpacing;
                        break;
                    default:
                        y_coord = temp.getBusID()*(rowHeight+rowSpacing) +
                                (num_rank*num_bank+num_rank-2)*
                                (rowHeight+rowSpacing) + rowSpacing;
                        break;
                }
                break;
            default:
                System.out.println("Error in paintTimeBlock");
                break;
        }

        // determine if the size of the text will be larger than the box
        if (temp.getStr().length()*fontSize < w) {
            // the box is big enough, draw the text
            g.drawRect
                    (x_coord,
                    y_coord,
                    w, rowHeight);
            g.setColor(Color.black);
            g.drawString
                    (temp.getStr(),
                    x_coord + 5,
                    y_coord + rowHeight/2);
        } else {
            // the box is not big enough, fill solid
            g.fillRect(x_coord,
                    y_coord,
                    w, rowHeight);
        }

        // Label the commands with the transaction ID
        if (temp.isCommand()) {
            switch (displayContent) {
                case TIMING_DIAG:
                    g.drawString(String.valueOf(temp.getID()),
                            x_coord, TRANS_ID*(rowHeight+rowSpacing) +
                            rowSpacing+fontSize);
                    break;
                case TIMING_DIAG_EXP_BANK:
                    g.drawString(String.valueOf(temp.getID()),
                            x_coord, (TRANS_ID+num_rank*num_bank-1)*
                            (rowHeight+rowSpacing)+rowSpacing+fontSize);
                    break;
                case TIMING_DIAG_EXP_UTIL:
                    g.drawString(String.valueOf(temp.getID()),
                            x_coord, (TRANS_ID+num_rank-1)*
                            (rowHeight+rowSpacing)+rowSpacing+fontSize);
                    break;
                case TIMING_DIAG_EXP_ALL:
                    g.drawString(String.valueOf(temp.getID()),
                            x_coord, (TRANS_ID+num_rank*num_bank+num_rank-1)*
                            (rowHeight+rowSpacing)+rowSpacing+fontSize);
                    break;
                default:
                    break;
            }
        }

        // What is this for??? I think this should be deleted
        //size.width = (int)(temp.getEndTime()*pixelsPerNanoSecond)+colSpacing;
        //drawingArea.setPreferredSize(size);

        drawingArea.revalidate();

    }

    /* Creates a new TimeBlock vector based on the new start and end times */
    public void recreateTimeBlocks() {

        /* This doubles the memory requirement
           May need to fix this! */

        Vector temp = (Vector)timeBlockVector.clone();
        timeBlockVector.removeAllElements();

        for (int p=0; p < temp.size(); p++) {
            TimeBlock b = ((TimeBlock)temp.elementAt(p));
            // Note: non-commands are ignored
            if (b.getEndTime() > timingStartTime &&
                    b.getStartTime() < timingEndTime) {
                createTimeBlocks(b.getStartTime(),
                        b.getType(),
                        b.getID(),
                        b.getRank(),
                        b.getBank(),
                        b.getRow(),
                        b.getCol());
            }
        }
    }

    /* Creates the various time blocks based on the command type */
    public void createTimeBlocks(double time, int type, int transID,
            int rank, int bank, int row, int col) {
        // Draw TimeBlocks
        double t = time;
        double endTime;
        double temp = 0.0;
        Color c = getCommandColor(type);

        // identify the command type
        switch(type) {
            case ROWACT:
                // add row activation command block
                endTime = t+tRCDclocks*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "row act", CMD_BUS, c,
                        t, t+clockPeriod, ROWACT,
                        rank,bank,row,col,
                        time,endTime));

                // CMD delay of one clock period
                t = t+clockPeriod;

                // add data sense block on bank utilization bus
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "data sense", BANK_UTIL, c,
                        t,t+tRCDclocks*clockPeriod,NONE,
                        rank,bank,row,col,
                        time,endTime));

                //t = t+tRCDclocks*clockPeriod;
                break;
            case READ:
                // Column Read
                endTime = time+
                        tCASclocks*clockPeriod+
                        tBURSTclocks*clockPeriod;

                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "col read", CMD_BUS, c,
                        t,t+clockPeriod, READ,
                        rank,bank,row,col,
                        time,endTime));

                // Add bank access block
                t = t+clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "bank access", BANK_UTIL, c,
                        t,t+(tCASclocks-1)*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));

                // Add I/O Gating block
                t = time+(tCASclocks)*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "I/O gating", DEV_UTIL, c,
                        t,t+tBURSTclocks*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));

                // Add Data burst
                //t = time+tCASclocks*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "data burst", DATA_BUS, c,
                        t,t+tBURSTclocks*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));
                break;
            case WRITE:
                // Column Write
                endTime = time+tCWDclocks*clockPeriod+
                        clockPeriod+
                        (tCWDclocks+tBURSTclocks-1)*clockPeriod+
                        (1+tWRclocks)*clockPeriod;

                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "col write", CMD_BUS, c,
                        t,t+clockPeriod, WRITE,
                        rank,bank,row,col,
                        time,endTime));

                // Add Data burst
                t = t+tCWDclocks*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "data burst", DATA_BUS, c,
                        t,t+tBURSTclocks*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));

                // Add I/O gating block
                //t = t+clockPeriod;
                t = time+(tCWDclocks+tBURSTclocks-1)*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "I/O gating", DEV_UTIL, c,
                        //t,t+tBURSTclocks*clockPeriod, NONE,
                        t,t+(1+tWRclocks)*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));

                // Add bank access block
                t = time+(tCWDclocks+tBURSTclocks-1)*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "bank access", BANK_UTIL, c,
                        t,t+(1+tWRclocks)*clockPeriod, NONE,
                        rank,bank,row,col,
                        time,endTime));
                break;
            case PRECHARGE:
                // Pre-charge
                endTime = time+clockPeriod+
                        tRPclocks*clockPeriod;

                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "precharge", CMD_BUS, c,
                        t,t+clockPeriod, PRECHARGE,
                        rank,bank,row,col,
                        time,endTime));

                // Add bank access block
                t = t+clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "bank precharge", BANK_UTIL,c,
                        t,t+tRPclocks*clockPeriod,NONE,
                        rank,bank,row,col,
                        time,endTime));
                break;
            case REFRESH:
                // refresh
                endTime = time+clockPeriod+tRASclocks*clockPeriod
                        +refreshClocks*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "refresh", CMD_BUS, c,
                        t,t+clockPeriod, REFRESH,
                        rank,bank,row,col,
                        time,endTime));

                // Add bank access block
                t = t+clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "row access (all banks)", BANK_UTIL,c,
                        t,t+tRASclocks*clockPeriod,NONE,
                        rank,bank,row,col,
                        time,endTime));

                // Add bank access block
                t = t+tRASclocks*clockPeriod;
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "precharge (all banks)", BANK_UTIL,c,
                        t,t+refreshClocks*clockPeriod,NONE,
                        rank,bank,row,col,
                        time,endTime));
                break;
            case BANKCONFLICT:
                // bank conflict on the "conflict bus"
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "bank conflict", CONFLICT, c,
                        t,t+clockPeriod, BANKCONFLICT,
                        rank,bank,row,col,
                        time,time));
                break;
            /*
        case ROWCONFLICT:
            // refresh
            timeBlockVector.addElement
                (new TimeBlock(transID,
                               "row conflict", CONFLICT, c,
                               t,t+clockPeriod, ROWCONFLICT,
                               rank,bank,row,col,
                               time,time));
            break;
             */
            case TFAWCONFLICT:
                // refresh
                timeBlockVector.addElement
                        (new TimeBlock(transID,
                        "Tfaw conflict", CONFLICT, c,
                        t,t+clockPeriod, TFAWCONFLICT,
                        rank,bank,row,col,
                        time,time));
                break;
            default:
                break;
        }
    }

    /* Searches the TimeBlock vector and counts the number of
       commands per minor tick. */
    public void collectStats() {
        double t = timingStartTime;
        TimeBlock b;
        int l, v, q;

        for (int p=0;p<timeBlockVector.size();p++) {
            // retreive the TimeBlock
            b = (TimeBlock)
            timeBlockVector.elementAt(p);
            // check to make sure it is a valid block and a command
            if (b.getStartTime() > timingStartTime &&
                    b.getStartTime() < timingEndTime &&
                    b.getType() != NONE) {

                // determine which tick the command fits into
                l = (int)Math.floor(b.getStartTime()/barXMinorTick);

                // determine what bar the command fits
                q = l*NUM_COMMANDS+b.getType()-1;

                // retrieve the old value (height) of the bar and increment
                v = ((Integer)statsVector.elementAt(q)).intValue()+1;

                // place the new value into the vector
                statsVector.setElementAt(new Integer(v),q);
            }
        }

    }

    /************************
     * FIX THESE TWO TO TAKE DISPLAY CONTENT INTO ACCOUNT
     ************************/

    // determine the number of major ticks
    public int getNumXMajorTicks() {
        double t = timingEndTime-timingStartTime;
        return (int)Math.ceil(t/barXMajorTick);
    }

    // determine the number of minor ticks
    public int getNumXMinorTicks() {
        double t = timingEndTime-timingStartTime;
        return (int)Math.ceil((getNumXMajorTicks()
        *barXMajorTick)/barXMinorTick);
    }
    /***************************/

    // zeros the vector and recollect the stats
    public void recreateStatsVector() {
        double t = timingEndTime-timingStartTime;
        statsVector.removeAllElements();
        statsVector.ensureCapacity(NUM_COMMANDS*
                getNumXMinorTicks());

        for (int i=0;i<NUM_COMMANDS*getNumXMinorTicks();i++) {
            statsVector.add(i,new Integer(0));
        }

        collectStats();

    }

    // test function
    public void displayStatsVector() {
        for (int i=0;i<getNumXMinorTicks();i++) {
            for (int j=0;j<NUM_COMMANDS;j++) {
                displayInfoMessage
                        ("Stats Vector",
                        "element("+(i*NUM_COMMANDS+j)+")="+
                        ((Integer)statsVector.
                        elementAt(i*NUM_COMMANDS+j)));
            }
        }
    }

    // displays an error emssage dialog
    public void displayErrorMessage(String title, String message) {
        JOptionPane.showMessageDialog
                (this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);
    }

    // displays an information dialog message
    public void displayInfoMessage(String title, String message) {
        JOptionPane.showMessageDialog
                (this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    // displays the text as start-up
    public void paintIntroPanel(Graphics g) {

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        fontSize = g.getFont().getSize();
        int textX = 10;
        int textY = 10+fontSize;

        g.drawString("Welcome to the DRAM Simulator Visualization Tool!",
                textX,textY);
        textY = textY+fontSize;

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        fontSize = g.getFont().getSize();

        g.drawString("File",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Input data:  loads input data"+
                " from the specified start time"+
                " to the end time from a file.", textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Load DRAM parameters:  loads paramaters of DRAM"+
                " from a file.", textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Output to Jpeg:  prints screen to a jpeg file.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Output to Png:  prints screen to a png file.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Exit:  close the program.", textX,textY);
        textY=textY+2*fontSize;

        g.drawString("Data",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Start Time/End Time:  Specify the time the"+
                " data should be collected/displayed.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - DRAM Parameters: Manually specify the"+
                " parameters associated with the DRAM technology.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Bar Graph Parameters:  Specify the"+
                " bar graph settings.",
                textX,textY);
        textY=textY+2*fontSize;

        g.drawString("View",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Display:  Choose what to display"+
                "on the screen.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Zoom In:  Zoom in display by a factor of 2.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Zoom Out:  Zoom out display by a factor of 2.",
                textX,textY);
        textY=textY+2*fontSize;

        g.drawString("Help",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Help:  Gives shortcut key definitions.",
                textX,textY);
        textY=textY+fontSize;

        g.drawString(" - Introduction: Display this introduction screen.",
                textX,textY);
        textY=textY+4*fontSize;

        g.drawString("Choose Help->Introduction to return to this introduction page.",
                textX,textY);
        textY=textY+fontSize;

    }

    /* Draw the graphics for the timing diagrams */
    public void paintTimingDiagram(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();

        double t = timingStartTime;

        // paint the TimeBlocks in the timeBlockVector
        for (int p=0; p < timeBlockVector.size(); p++) {
            TimeBlock b = ((TimeBlock)
            timeBlockVector.elementAt(p));
            if (b.getEndTime() > timingStartTime &&
                    b.getStartTime() < timingEndTime) {

                drawTimeBlock(g,(TimeBlock)
                timeBlockVector.elementAt(p));
            }
        }

        // Draw Clock
        g.setColor(Color.black);
        t = timingEndTime-timingStartTime;
        int c = (int)(pixelsPerNanoSecond*clockPeriod);

        int p = 0;
        double d = timingStartTime;
        int clocks = 0;
        while (d<timingEndTime) {
            g.drawLine(colSpacing+p,rowSpacing,
                    colSpacing+p,rowSpacing+rowHeight);
            g.drawLine(colSpacing+p,rowSpacing,
                    colSpacing+p+c/2, rowSpacing);
            g.drawLine(colSpacing+p+c/2, rowSpacing,
                    colSpacing+p+c/2, rowSpacing+rowHeight);
            g.drawLine(colSpacing+p+c/2, rowSpacing+rowHeight,
                    colSpacing+p+c, rowSpacing+rowHeight);
            d = d+clockPeriod;
            p = (int)(d*pixelsPerNanoSecond);
        }

        // draw the axis
        switch (displayContent) {
            case TIMING_DIAG:
                paintXAxis(g,colSpacing,(NUM_BUSSES+1)*
                        (rowSpacing+rowHeight),true);
                break;
            case TIMING_DIAG_EXP_BANK:
                paintXAxis(g,colSpacing, (int)(NUM_BUSSES+num_bank*num_rank)*
                        (rowSpacing+rowHeight),true);
                break;
            case TIMING_DIAG_EXP_UTIL:
                paintXAxis(g,colSpacing,
                        (int)(NUM_BUSSES+num_rank)*(rowSpacing+rowHeight),true);
                break;
            case TIMING_DIAG_EXP_ALL:
                paintXAxis(g,colSpacing, (int)(NUM_BUSSES+num_bank*
                        num_rank+num_rank-1)*(rowSpacing+rowHeight),true);
                break;
            default:
                System.out.println("Error in paintTimeBlock");
                break;
        }
    }

    /* draws the side bar for the bar graphs */
    public void paintStatsSideBar(Graphics g) {
        // make variables global
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();
        int yBase = getHeight()-200;
        int yTop  = 100;
        int xOffset = 70;
        int yMajorTickSize = 20;
        int yMinorTickSize = 10;
        int numYMajorTicks = (int)Math.ceil(barYScale/barYMajorTick);
        int numYMinorTicks = (int)Math.ceil(barYScale/barYMinorTick);
        int yHeight = yBase-yTop;

        // Draw and label major ticks
        for (int i=0;i<numYMajorTicks;i++) {
            g.drawLine(xOffset,
                    yBase-(int)(i*Math.ceil(yHeight/numYMajorTicks)),
                    xOffset+yMajorTickSize,
                    yBase-(int)(i*Math.ceil(yHeight/numYMajorTicks)));
            g.drawString(String.valueOf(i*barYMajorTick),
                    xOffset-40,
                    yBase-(int)(i*Math.ceil(yHeight/numYMajorTicks)));

        }

        // Draw minor ticks
        for (int i=0;i<numYMinorTicks;i++) {
            g.drawLine(xOffset,
                    yBase-(int)(i*Math.ceil(yHeight/numYMinorTicks)),
                    xOffset+yMinorTickSize,
                    yBase-(int)(i*Math.ceil(yHeight/numYMinorTicks)));
        }

        g.drawLine(xOffset,yBase,xOffset,yTop);

        // paints the command color key
        paintCommandKey(g,xOffset-40,yBase+fontSize*2);

    }

    /* draws the x-axis */
    /* FIX FOR  TIMING DIAGRAM AXIS */
    public void paintXAxis(Graphics g, int xOffset,
            int yBase, boolean ticksUp) {
        int xMajorTickSize = 20;
        int xMinorTickSize = 10;
        double xMajorTick;
        double xMinorTick;
        double pixelsPerNS;
        int xTickPixels;// = (int)(barXMinorTick*pixelsPerNanoSecond);
        double t = timingEndTime-timingStartTime;
        int sign;

        // do the ticks go up or down from the axis?
        if (ticksUp)
            sign = -1;
        else
            sign = 1;

        // choose which scale to use
        switch (displayContent) {
            case TIMING_DIAG:
            case TIMING_DIAG_EXP_BANK:
            case TIMING_DIAG_EXP_UTIL:
            case TIMING_DIAG_EXP_ALL:
                xMajorTick = timingXMajorTick;
                xMinorTick = timingXMinorTick;
                pixelsPerNS = pixelsPerNanoSecond;
                break;
            default:
            case STATS_GRAPH:
                xMajorTick = barXMajorTick;
                xMinorTick = barXMinorTick;
                pixelsPerNS = barPixelsPerNS;
                break;
        }
        // determine the pixels in one tick
        xTickPixels = (int)(xMinorTick*pixelsPerNS);

        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();

        g.drawLine(xOffset,
                yBase,
                (int)(xOffset+t*pixelsPerNanoSecond),
                yBase);

        // draw and label major ticks
        for (int i=0;i<getNumXMajorTicks();i++) {
            g.drawLine(xOffset+(int)(i*xMajorTick*pixelsPerNS),
                    yBase,
                    xOffset+(int)(i*xMajorTick*pixelsPerNS),
                    yBase+sign*xMajorTickSize);
            g.drawString(String.valueOf(i*xMajorTick),
                    xOffset+(int)(i*xMajorTick*pixelsPerNS),
                    yBase+fontSize);

        }

        // draw and label minor ticks
        for (int i=0;i<getNumXMinorTicks();i++) {
            int tickOffset = xOffset+
                    (int)(i*xMinorTick*pixelsPerNS);

            g.setColor(Color.black);
            g.drawLine(tickOffset,yBase,
                    tickOffset,yBase+sign*xMinorTickSize);
            // is the tick size small enough to label the minor ticks?
            if (String.valueOf(i*xMinorTick).length()*fontSize <
                    xTickPixels) {
                g.drawString(String.valueOf(i*xMinorTick),
                        xOffset+
                        (int)(i*xMinorTick*pixelsPerNS),
                        yBase-sign*fontSize);
            }
        }
    }

    /* draws the color coded command key */
    public void paintCommandKey(Graphics g, int x, int y) {
        String text;
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();

        for (int i=0;i<NUM_COMMANDS;i++) {
            text = getCommandString(i);
            g.setColor(getCommandColor(i+1));
            g.fillRect(x,y+i*fontSize*2-fontSize,
                    fontSize,fontSize);
            g.drawString(text,x+2*fontSize, y+i*fontSize*2);
        }
    }

    /* draws the bar graphs */
    public void paintStatsGraph(Graphics g) {
        // make variables global??
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();
        int yBase = getHeight()-200;
        int yTop  = 100;
        int xOffset = 10;
        int xMajorTickSize = 20;
        int xMinorTickSize = 10;
        int yMajorTickSize = 20;
        int yMinorTickSize = 10;
        int numYMajorTicks = (int)Math.ceil(barYScale/barYMajorTick);
        int numYMinorTicks = (int)Math.ceil(barYScale/barYMinorTick);
        int yHeight = yBase-yTop;
        int xTickPixels = (int)(barXMinorTick*barPixelsPerNS);
        int numBars = 0;

        // dtermine the number of bars appearing on the graph
        for (int q=0;q<NUM_COMMANDS;q++) {
            if (statsCommandMask[q])
                numBars++;
        }

        g.setFont(new Font("Arial", Font.PLAIN, 10));
        fontSize = g.getFont().getSize();

        // evenly divide the bars in the allotted space
        int barWidth = xTickPixels/numBars;
        if (barWidth<1)
            return;

        double t = timingEndTime-timingStartTime;

        g.drawLine(xOffset,
                yBase,
                (int)(xOffset+t*barPixelsPerNS),
                yBase);

        //draws axis ---> Fix to use drawXAxis function
        for (int i=0;i<getNumXMajorTicks();i++) {
            g.drawLine(xOffset+(int)(i*barXMajorTick*barPixelsPerNS),
                    yBase,
                    xOffset+(int)(i*barXMajorTick*barPixelsPerNS),
                    yBase-xMajorTickSize);
            g.drawString(String.valueOf(i*barXMajorTick),
                    xOffset+(int)(i*barXMajorTick*barPixelsPerNS),
                    yBase+fontSize);

        }

        // draw minor ticks and bars
        for (int i=0;i<getNumXMinorTicks();i++) {
            int tickOffset = xOffset+
                    (int)(i*barXMinorTick*barPixelsPerNS);

            // draw bars
            int n = 0;
            if (INPUT_READ) {
                for (int j=0;j<NUM_COMMANDS;j++) {
                    if (statsCommandMask[j]) {
                        g.setColor(getCommandColor(j+1));
                        // assuming command 0 is NONE (j+1)

                        int s = ((Integer)
                        (statsVector.elementAt(i*NUM_COMMANDS+j))).intValue();

                        double f =
                                Math.min(barYScale,s)/((double)(barYScale));

                        int h = (int)(f*yHeight);

                        g.fillRect(tickOffset+n*xTickPixels/numBars+1,
                                yBase-h,
                                xTickPixels/numBars-1,
                                h);
                        n++;
                    }
                }
            }

            // draw and label minor ticks
            g.setColor(Color.black);
            g.drawLine(tickOffset,yBase,
                    tickOffset,yBase-xMinorTickSize);
            if (String.valueOf(i*barXMinorTick).length()*fontSize <
                    xTickPixels) {
                g.drawString(String.valueOf(i*barXMinorTick),
                        xOffset+
                        (int)(i*barXMinorTick*barPixelsPerNS),
                        yBase+fontSize);
            }

        }
    }

    // converts a mouse click to a time
    public double clickToTime(double x) {
        double t = timingEndTime-timingStartTime;
        double f = (x-colSpacing)/((double)size.width);
        return f*t;
    }

    // searchs timeBlockVector and returns the transaction ID
    public int findTransactionAtTime(double t) {
        // Fix this to be binary search!
        TimeBlock b;
        for (int i=0;i<timeBlockVector.size();i++) {
            b=(TimeBlock)timeBlockVector.elementAt(i);
            if (b.getStartTime() <= t &&
                    b.getEndTime()   >= t) {
                return i;
            }
        }
        return -1;
    }

    // Listen for mouse clicks and take appropriate action
    class MyMouseListener extends MouseAdapter implements MouseListener {
        // Do nothing while button is down
        public void mousePressed(MouseEvent e) { }

        // Action when mouse is released
        public void mouseReleased(MouseEvent e) {

            // determine the keys for zooming
            String modText = e.getMouseModifiersText(e.getModifiers());
            boolean shift, alt, ctrl;

            // determine if a key was pressed during click
            ctrl  = (modText.indexOf("Ctrl")  != -1);
            shift = (modText.indexOf("Shift") != -1);
            alt   = (modText.indexOf("Alt")   != -1);

            if (ctrl) {
                //displayErrorMessage("Mouse","Ctrl");
                centerDisplay(e.getX(),e.getY());
            } else if (shift) {
                /*
                displayErrorMessage("Mouse","Shift ("+
                                    e.getX()+","+e.getY()+")");
                 */
                zoomIn(e.getX(),e.getY());
            } else if (alt) {
                /*
                displayErrorMessage("Mouse","Alt ("+
                                    e.getX()+","+e.getY()+")");
                 */
                zoomOut(e.getX(),e.getY());
            } else {
                // display transaction info
                double t = clickToTime(e.getX());
                int p = findTransactionAtTime(t);
                String toolTipText;
                if (p != -1) {
                    TimeBlock b = (TimeBlock)timeBlockVector.elementAt(p);
                    int tid = b.getID();
                    int rank = b.getRank();
                    int bank = b.getBank();
                    int row  = b.getRow();
                    int col = b.getCol();
                    double start = b.getStartTime();
                    double end = b.getEndTime();
                    double tStart = b.getTransactionStartTime();
                    double tEnd = b.getTransactionEndTime();
                    toolTipText
                            = new String("Transaction "+tid+":\n"+
                            "Trans. Start time = "+ (int)tStart+" ns\n"+
                            "Trans. End time = "+ (int)tEnd+" ns\n"+
                            "Block Start time = "+ (int)start +" ns\n"+
                            "Block End time = "+ (int)end+" ns\n"+
                            "Rank   = "+rank+"\n"+
                            "Bank   = "+bank+"\n"+
                            "Row    = "+row+"\n"+
                            "Column = "+col+"\n");

                    displayInfoMessage
                            ("Transaction Info",
                            toolTipText);


                } else {
                    toolTipText
                            = new String("No Transaction"+
                            "at time "+(int)t+" ns.");
                    /*
                      displayErrorMessage("Nothing!",
                      "Nothing here!");
                     */
                }
                //showToolTip(toolTipText,e.getX(), e.getY());
                //}
            }
        }
    }

//
//
// main function - create GUI and run
//
//

    public static void main(String args[]) {
        final VisTool sv = new VisTool();

        final JFrame frame = new JFrame("DRAM Simulator Visualization Tool");
        /* {
                public void setSize() {
                    super.setSize();
                    sv.displayInfoMessage("RESIZE",
                                          "Window resized!");
                }
            };
         */
        frame.setLocation(100,100);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });

        JScrollPane scrollerV = new JScrollPane(sv);
        frame.getContentPane().add(scrollerV);
        scrollerV.setPreferredSize(new Dimension(800,400));

        // Menu bar object for VisTool frame
        JMenuBar bar = new JMenuBar();
        frame.setJMenuBar(bar);
        bar.setPreferredSize(new Dimension(200,20));
        //frame.add(bar,BorderLayout.NORTH);

        //
        // Menu bar selections
        //
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenu dataMenu = new JMenu("Data");
        dataMenu.setMnemonic('D');

        JMenu zoomMenu = new JMenu("View");
        zoomMenu.setMnemonic('V');

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        //
        // File subselections
        //

        // Input
        JMenuItem inputItem = new JMenuItem("Input data");
        inputItem.setMnemonic('i');
        inputItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sv.inputFile();
                } catch (IOException ioex) {
                    JOptionPane.showMessageDialog
                            (sv,
                            "File Input Error.",
                            " ",
                            JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException NPex) {
                    // Cancel selected, do nothing
                }
            }
        }
        );
        fileMenu.add(inputItem);

        // Input parameters
        JMenuItem inputParamItem = new JMenuItem("Load DRAM parameters");
        inputParamItem.setMnemonic('L');
        inputParamItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sv.inputParamFile();
                } catch (IOException ioex) {
                    JOptionPane.showMessageDialog
                            (sv,
                            "File Load Error.",
                            " ",
                            JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException NPex) {
                    // Cancel selected, do nothing
                }
            }
        }
        );
        fileMenu.add(inputParamItem);

        // Print to JPEG
        JMenuItem jpegItem = new JMenuItem("Output to Jpeg");
        jpegItem.setMnemonic('J');
        jpegItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.printToFile(isJPEG, frame);
            }
        }
        );
        fileMenu.add(jpegItem);

        // Print to png
        JMenuItem pngItem = new JMenuItem("Output to Png");
        pngItem.setMnemonic('N');
        pngItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.printToFile(isPNG, frame);
            }
        }
        );
        fileMenu.add(pngItem);

        // Exit
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }
        );
        fileMenu.add(exitItem);

        //
        // Data subselections
        //

        // Start/End Time
        final JMenuItem timeItem = new JMenuItem("Start/End Time");
        timeItem.setMnemonic('S');
        timeItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.openTimeDialog();
            }
        }
        );
        dataMenu.add(timeItem);

        // DRAM Parameters
        final JMenuItem paramItem = new JMenuItem("DRAM Parameters");
        paramItem.setMnemonic('D');
        paramItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.openDRAMParametersDialog();
            }
        }
        );
        dataMenu.add(paramItem);

        // Bar Graph Parameter
        final JMenuItem barItem = new JMenuItem("Bar Graph Parameters");
        barItem.setMnemonic('B');
        barItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.openBarGraphParameterDialog();
            }
        }
        );
        dataMenu.add(barItem);

        //
        // View subselections
        //


        // Display Timing Diagram
        final JMenuItem t1Item = new JMenuItem("Basic Timing Diagram");
        t1Item.setMnemonic('T');
        t1Item.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.displayView (TIMING_DIAG);
            }
        }
        );
        zoomMenu.add(t1Item);


        // Display Timing Diagram with expanded bank utilization
        final JMenuItem t2Item = new JMenuItem("Expand bank utilization");
        t2Item.setMnemonic('B');
        t2Item.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.displayView(TIMING_DIAG_EXP_BANK);
            }
        }
        );
        zoomMenu.add(t2Item);


        // Display Timing Diagram with expanded device utilization
        final JMenuItem t3Item = new JMenuItem("Expand device utilization");
        t3Item.setMnemonic('D');
        t3Item.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.displayView(TIMING_DIAG_EXP_UTIL);
            }
        }
        );
        zoomMenu.add(t3Item);


        // Display Timing Diagram with expanded bank and device
        final JMenuItem t4Item = new JMenuItem("Expand all: bank and device");
        t4Item.setMnemonic('A');
        t4Item.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.displayView(TIMING_DIAG_EXP_ALL);
            }
        }
        );
        zoomMenu.add(t4Item);


        // Display Statistics Graph
        final JMenuItem statItem = new JMenuItem("Statistics Bar Graph");
        statItem.setMnemonic('S');
        statItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sv.displayView(STATS_GRAPH);
            }
        }
        );
        zoomMenu.add(statItem);


        // Zoom In
        final JMenuItem zoomInItem = new JMenuItem("Zoom In:     (shift + mouse click)");
        zoomInItem.setMnemonic('I');
        zoomInItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = sv.zoomIn(sv.getCenterX(),
                        sv.getCenterY());
                if (i == 1)
                    zoomInItem.setEnabled(true);
                else
                    zoomInItem.setEnabled(false);
            }
        }
        );
        zoomMenu.add(zoomInItem);

        // Zoom Out
        final JMenuItem zoomOutItem = new JMenuItem("Zoom Out:  (alt + mouse click)");
        zoomOutItem.setMnemonic('O');
        zoomOutItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = sv.zoomOut(sv.getCenterX(),
                        sv.getCenterY());
                zoomInItem.setEnabled(true);
            }
        }
        );
        zoomMenu.add(zoomOutItem);

        //
        // Help subselections
        //

        // Help
        final JMenuItem helpItem = new JMenuItem("Help");
        helpItem.setMnemonic('H');
        helpItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog
                sv.displayInfoMessage
                        ("Help",
                        "Shortcut Keys:\n"+
                        "Ctrl+Mouse Click - Center Scroll Pane\n"+
                        "Shift+Mouse Click - Zoom In\n"+
                        "Alt+Mouse Click - Zoom Out"//,
                        /*JOptionPane.INFORMATION_MESSAGE*/);
            }
        }
        );
        helpMenu.add(helpItem);

        // Help
        final JMenuItem introItem = new JMenuItem("Introduction");
        introItem.setMnemonic('I');
        introItem.addActionListener(
                new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog
                sv.displayIntro();
            }
        }
        );
        helpMenu.add(introItem);

        //
        // add menus to menu bar
        //
        bar.add(fileMenu);
        bar.add(dataMenu);
        bar.add(zoomMenu);
        bar.add(helpMenu);

        frame.pack();
        frame.setVisible(true);
    }

}