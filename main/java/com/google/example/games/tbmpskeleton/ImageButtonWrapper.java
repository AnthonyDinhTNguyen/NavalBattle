package com.google.example.games.tbmpskeleton;

/**
 * Created by antho on 2/26/2018.
 */

import android.content.Context;
import android.widget.ImageButton;

/**
 * Created by nguyennguyen on 2/20/18.
 */

public class ImageButtonWrapper
{
    private ImageButton imageButton;
    private int xCoord;
    private int yCoord;

    private boolean canMove;
    private boolean canShoot;

    public ImageButtonWrapper(Context context, int x, int y){
        canMove = false;
        canShoot = false;
        imageButton = new ImageButton(context);
        xCoord = x;
        yCoord = y;
    }

    public ImageButton getButton(){
        return imageButton;
    }
    public boolean getShoot(){return canShoot;}
    public void setShoot(boolean shoot){canShoot = shoot;}
    public void setMove(boolean value){
        canMove = value;
    }
    public boolean getMove(){
        return canMove;
    }
    public int getxCoord(){return xCoord;}
    public int getyCoord() {return yCoord;}

}
