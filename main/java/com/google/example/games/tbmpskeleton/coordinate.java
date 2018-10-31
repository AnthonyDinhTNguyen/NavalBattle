package com.google.example.games.tbmpskeleton;

import java.util.Random;

/**
 * Created by antho on 2/20/2018.
 */

public class coordinate {
    private int xCoord;
    private int yCoord;

    public int getx()
    {
        return xCoord;
    }
    public int gety(){
        return yCoord;
    }
    public void setCoord(int x, int y){
        xCoord = x;
        yCoord = y;
    }
    public coordinate(){
        Random generator = new Random();
        xCoord=generator.nextInt(10);
        yCoord=generator.nextInt(5);
    }
}
