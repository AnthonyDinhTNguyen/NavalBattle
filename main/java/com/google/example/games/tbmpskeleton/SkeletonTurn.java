/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.games.tbmpskeleton;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Basic turn data. It's just a blank data string and a turn number counter.
 *
 * @author wolff
 */
public class SkeletonTurn {

  public static final String TAG = "EBTurn";

  public String data = "";
  public int turnCounter;
  public int S1health;
  public int S2health;
  public String S1shoot;
  public String S2shoot;
  public int S1move;
  public int S2move;
  public int S1x;
  public int S2x;
  public int S1y;
  public int S2y;
  public Ship ship1;
  public Ship ship2;
  public SkeletonTurn() {
  }

  // This is the byte array we will write out to the TBMP API.
  public byte[] persist() {
    JSONObject retVal = new JSONObject();

    try {
      retVal.put("S1health",S1health); retVal.put("S2health",S2health);
      retVal.put("S1shoot",S1shoot); retVal.put("S2shoot",S2shoot);
      retVal.put("S1move",S1move); retVal.put("S2move",S2move);
      retVal.put("S1x",S1x); retVal.put("S2x",S2x);
      retVal.put("S1y",S1y); retVal.put("S2y",S2y);
      retVal.put("data", data);
      retVal.put("turnCounter", turnCounter);

    } catch (JSONException e) {
      Log.e("SkeletonTurn", "There was an issue writing JSON!", e);
    }

    String st = retVal.toString();

    Log.d(TAG, "==== PERSISTING\n" + st);

    return st.getBytes(Charset.forName("UTF-8"));
  }

  // Creates a new instance of SkeletonTurn.
  static public SkeletonTurn unpersist(byte[] byteArray) {

    if (byteArray == null) {
      Log.d(TAG, "Empty array---possible bug.");
      return new SkeletonTurn();
    }

    String st = null;
    try {
      st = new String(byteArray, "UTF-8");
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
      return null;
    }

    Log.d(TAG, "====UNPERSIST \n" + st);

    SkeletonTurn retVal = new SkeletonTurn();

    try {
      JSONObject obj = new JSONObject(st);

      if (obj.has("data")) {
        retVal.data = obj.getString("data");
      }
      if (obj.has("turnCounter")) {
        retVal.turnCounter = obj.getInt("turnCounter");
      }
      if(obj.has("S1health")){retVal.S1health = obj.getInt("S1health");}
      if (obj.has("S2health")){retVal.S2health = obj.getInt("S2health");}
      if (obj.has("S1move")){retVal.S1move = obj.getInt("S1move");}
      if (obj.has("S2move")){retVal.S2move = obj.getInt("S2move");}
      if (obj.has("S1shoot")){retVal.S1shoot = obj.getString("S1shoot");}
      if (obj.has("S2shoot")){retVal.S2shoot = obj.getString("S2shoot");}
      if (obj.has("S1x")){retVal.S1x = obj.getInt("S1x");}
      if (obj.has("S2x")){retVal.S2x = obj.getInt("S2x");}
      if (obj.has("S1y")){retVal.S1y = obj.getInt("S1y");}
      if (obj.has("S2y")){retVal.S2y = obj.getInt("S2y");}
      //retVal.ship1 = new Ship(retVal.S1x,retVal.S1y,retVal.S1health,retVal.S1move,retVal.S1shoot);
      //retVal.ship2 = new Ship(retVal.S2x,retVal.S2y,retVal.S2health,retVal.S2move,retVal.S2shoot);
    } catch (JSONException e) {
      Log.e("SkeletonTurn", "There was an issue parsing JSON!", e);
    }

    return retVal;
  }
}
