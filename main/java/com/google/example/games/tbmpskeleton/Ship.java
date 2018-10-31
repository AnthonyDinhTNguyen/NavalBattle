package com.google.example.games.tbmpskeleton;

import java.util.Random;

/**
 * Created by antho on 2/20/2018.
 */
public class Ship {
    private int health;
    //private coordinate ourCoordinate;
    private int xCoord;
    private int yCoord;
    private String shooting;
    private int moveSpeed;
    private Random generator = new Random();

    public Ship(){

        int i = generator.nextInt(2);
        xCoord = generator.nextInt(10);
        yCoord = generator.nextInt(5);

        if (i == 0)
        {
            health = 3;
            shooting = "line";
            moveSpeed = 2;

        }
        else{
            health = 2;
            shooting = "square";
            moveSpeed = 1;
        }
    }
    public Ship(int x, int y, int h , int ms, String shoot) {
        xCoord = x;
        yCoord = y;
        health = h;
        moveSpeed = ms;
        shooting = shoot;
    }
    public int getHealth()
    {
        return health;
    }
    public int getxCoordinate(){
        return xCoord;
    }
    public int getyCoordinate(){
        return yCoord;
    }
    public String getShooting(){
        return shooting;
    }
    public int getMoveSpeed(){
        return moveSpeed;
    }
    public void setHealth(int newHealth){
        health = newHealth;
    }
    public void setOurCoordinate(int x, int y){
        xCoord = x;
        yCoord = y;
    }
}

