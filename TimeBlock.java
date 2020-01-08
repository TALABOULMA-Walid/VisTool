/*
 * TimeBlock.java
 *
 * Austin Lanham
 *    4/26/05
 *
 * This class holds information about blocks
 * of time in the timing diagram.
 */

import java.awt.*;
import java.util.*;

public class TimeBlock extends Object {
    private int ID;        // Transaction ID
    private String str;    // Display text
    private int busID;     // Which bus is it on?
    private Color col;     // What color
    private double startTime;  // block start time
    private double endTime;    // block end time
    private double transStartTime;
    private double transEndTime;
    private int eventID;
    private int rank;
    private int bank;
    private int row;
    private int column;
    
    public TimeBlock(int id, String s, int bID, Color color,
            double tstart, double tend, int eID,
            int r, int b, int w, int c,
            double transStart, double transEnd) {
        
        
        ID=id;
        str=s;
        busID = bID;
        col=color;
        startTime = tstart;
        endTime = tend;
        eventID = eID;
        rank = r;
        bank = b;
        row = w;
        column = c;
        transStartTime = transStart;
        transEndTime   = transEnd;
    }
    
    public int getID() {
        return ID;
    }
    
    public String getStr() {
        return str;
    }
    
    public int getBusID() {
        return busID;
    }
    
    public Color getColor() {
        return col;
    }
    
    public double getStartTime() {
        return startTime;
    }
    
    public double getEndTime() {
        return endTime;
    }
    
    public double getTransactionStartTime() {
        return transStartTime;
    }
    
    public double getTransactionEndTime() {
        return transEndTime;
    }
    
    public int getType() {
        return eventID;
    }
    
    public int getRank() {
        return rank;
    }
    
    public int getBank() {
        return bank;
    }
    
    public int getRow() {
        return row;
    }
    
    public int getCol() {
        return column;
    }
    
    public boolean isCommand() {
        return (getType()!=0);
    }
};



