package com.google.example.games.tbmpskeleton;

/**
 * Created by antho on 2/22/2018.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import com.anthony.skeletontest1.R;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyMap{
    public static boolean pickMovePhase;
    public static boolean doMovePhase;
    public static boolean pickAttackPhase;
    public static boolean doAttackPhase;
    private ArrayList<ArrayList<ImageButtonWrapper>> map;
    private int CLEAR = 1;
    private int COLOR = 0;
    private Context mContext;

    public MyMap(Context context, ArrayList<ArrayList<ImageButtonWrapper>> m){
        pickMovePhase = true;
        doMovePhase = false;
        pickAttackPhase = false;
        doAttackPhase = false;
        map = m;
        mContext = context;
    }

    public void calculateMove(Ship ship, int saturation ){
        int range = ship.getMoveSpeed();
        for(int i = 0; i < range * 2 + 1; i++){
            for(int j = 0; j < range * 2 + 1; j++) {
                int currentX = ship.getxCoordinate() - range + j;
                int currentY = ship.getyCoordinate() - range + i;

                if((currentX >= 0 && currentX < 10) && (currentY >= 0 && currentY < 5)){
                    ImageButtonWrapper imageButtonWrapper = map.get(currentY).get(currentX);
                    if(saturation == COLOR) {
                        imageButtonWrapper.setMove(true);
                        imageButtonWrapper.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
                    }
                    else {
                        imageButtonWrapper.setMove(false);
                        imageButtonWrapper.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DST);
                    }
                }
            }
        }
    }

    public void calculateShoot(Ship ship, int saturation){
        String type = ship.getShooting();
        int xCoord = ship.getxCoordinate();
        int yCoord = ship.getyCoordinate();



        //For square shooting type:
        if (type.equals("square")){
            for(int x = 0; x < 10; x++) {
                for (int y = 0; y < 5; y++) {
                    ImageButtonWrapper currButton = map.get(y).get(x);
                    if(saturation == CLEAR) {
                        currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DST);
                        currButton.setShoot(false);
                    }
                    else {
                        if(x != ship.getxCoordinate() || y != ship.getyCoordinate())
                            currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
                        currButton.setShoot(true);
                    }
                }
            }
        }

        //For line shooting type:
        else {

            for(int x = -9; x <= 9; x++){
                int currentX = xCoord + x;
                int currentY = yCoord;
                if(currentX <= 9 && currentX >= 0) {
                    ImageButtonWrapper currButton = map.get(currentY).get(currentX);
                    if (saturation == COLOR) {
                        if(currentX != ship.getxCoordinate() || currentY != ship.getyCoordinate()) {
                            currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
                            currButton.setShoot(true);
                        }
                    }
                    else {
                        currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DST);
                        currButton.setShoot(false);
                    }
                }
            }

            for(int y = -4; y <= 4; y++){
                int currentX = xCoord;
                int currentY = yCoord + y;
                if(currentY <= 4 && currentY >= 0) {
                    ImageButtonWrapper currButton = map.get(currentY).get(currentX);
                    if (saturation == COLOR) {
                        if(currentX != ship.getxCoordinate() || currentY != ship.getyCoordinate()) {
                            currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DARKEN);
                            currButton.setShoot(true);
                        }
                    }
                    else {
                        currButton.getButton().getBackground().setColorFilter(Color.RED, PorterDuff.Mode.DST);
                        currButton.setShoot(false);
                    }
                }
            }
        }
    }



    public void move(Ship ship, int x, int y) {
        ship.setOurCoordinate(x, y);
    }

    public void attack(int buttonX, int buttonY, Ship currShip, Ship enemyShip){
        String shootingType = currShip.getShooting();
        if(shootingType.equals("square"))
            squareAttack(buttonX, buttonY, currShip, enemyShip);
        else
            lineAttack(buttonX, buttonY, currShip, enemyShip);
    }
    private void lineAttack(int buttonX, int buttonY, Ship currShip, Ship enemyShip){
        int currShipX = currShip.getxCoordinate();
        int currShipY = currShip.getyCoordinate();

        int enemyShipX = enemyShip.getxCoordinate();
        int enemyShipY = enemyShip.getyCoordinate();
        int enemyShipHealth = enemyShip.getHealth();
        if(buttonX == currShipX) {
            if(buttonY > currShipY){
                for(int y = currShipY + 1; y < 5; y++){
                    ImageButton imageButton = map.get(y).get(buttonX).getButton();

                    if(buttonX == enemyShipX && y ==enemyShipY) {
                        enemyShipHealth--;
                        enemyShip.setHealth(enemyShipHealth);
                        Log.d("enemyShipHealth",Integer.toString(enemyShipHealth));
                        String message = new String("Enemy was hit");
                        if(enemyShipHealth == 0)
                            message += "and is dead. You won";
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }

                }
            }
            else {
                for(int y = 0; y < currShipY; y++){
                    ImageButton imageButton = map.get(y).get(buttonX).getButton();

                    if(buttonX == enemyShipX && y ==enemyShipY) {
                        enemyShipHealth--;
                        enemyShip.setHealth(enemyShipHealth);
                        Log.d("enemyShipHealth",Integer.toString(enemyShipHealth));
                        String message = new String("Enemy was hit");
                        if(enemyShipHealth == 0)
                            message += "and is dead. You won";
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
        else if(buttonY == currShipY){
            if(buttonX > currShipX){
                for(int x = currShipX + 1; x < 10; x++){
                    ImageButton imageButton = map.get(buttonY).get(x).getButton();

                    if(buttonY == enemyShipY && x == enemyShipX){
                        enemyShipHealth--;
                        enemyShip.setHealth(enemyShipHealth);
                        Log.d("enemyShipHealth",Integer.toString(enemyShipHealth));
                        String message = new String("Enemy was hit");
                        if(enemyShipHealth == 0)
                            message += "and is dead. You won";
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }

                }
            }
            else{
                for(int x = 0; x < currShipX; x++){

                    final ImageButton imageButton = map.get(buttonY).get(x).getButton();
                    if(buttonY == enemyShipY && x == enemyShipX) {
                        enemyShipHealth--;
                        enemyShip.setHealth(enemyShipHealth);
                        Log.d("enemyShipHealth", Integer.toString(enemyShipHealth));
                        String message = new String("Enemy was hit");
                        if (enemyShipHealth == 0)
                            message += "and is dead. You won";
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }


                }


            }
        }
    }

    private void squareAttack(int buttonX, int buttonY, Ship currShip, Ship enemyShip){

        int enemyShipX = enemyShip.getxCoordinate();
        int enemyShipY = enemyShip.getyCoordinate();
        int enemyShipHealth = enemyShip.getHealth();

        int range = 1;
        for(int i = 0; i < range * 2 + 1; i++){
            for(int j = 0; j < range * 2 + 1; j++) {
                int currentX = buttonX - range + j;
                int currentY = buttonY - range + i;

                if((currentX >= 0 && currentX < 10) && (currentY >= 0 && currentY < 5) && (currentX != currShip.getxCoordinate() || currentY != currShip.getyCoordinate())){
                    final ImageButton imageButton = map.get(currentY).get(currentX).getButton();


                    if(currentX == enemyShipX && currentY == enemyShipY){
                        enemyShipHealth--;
                        enemyShip.setHealth(enemyShipHealth);
                        Log.d("enemyShipHealth",Integer.toString(enemyShipHealth));
                        String message = new String("Enemy was hit");
                        if(enemyShipHealth == 0)
                            message += "and is dead. You won";
                        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                    }


                }
            }
        }


    }


}

