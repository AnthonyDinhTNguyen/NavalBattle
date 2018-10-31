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

import com.anthony.skeletontest1.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;


/**
 * TBMPSkeleton: A minimalistic "game" that shows turn-based
 * multiplayer features for Play Games Services.  In this game, you
 * can invite a variable number of players and take turns editing a
 * shared state, which consists of single string.  You can also select
 * automatch players; all known players play before automatch slots
 * are filled.
 * <p>
 * INSTRUCTIONS: To run this sample, please set up
 * a project in the Developer Console. Then, place your app ID on
 * res/values/ids.xml. Also, change the package name to the package name you
 * used to create the client ID in Developer Console. Make sure you sign the
 * APK with the certificate whose fingerprint you entered in Developer Console
 * when creating your Client Id.
 *
 * @author Wolff (wolff@google.com), 2013
 */
public class SkeletonActivity extends Activity implements
    View.OnClickListener {
  private boolean gameState = false;
  private MyMap currentMap;
  private int COLOR = 0;
  private int CLEAR = 1;
  public static final String TAG = "SkeletonActivity";

  // Client used to sign in with Google APIs
  private GoogleSignInClient mGoogleSignInClient = null;

  public TextView mDataView;
  public TextView mTurnTextView;
  public MyMap mMap;

  private AlertDialog mAlertDialog;

  // Client used to interact with the TurnBasedMultiplayer system.
  private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;

  // Client used to interact with the Invitation system.
  private InvitationsClient mInvitationsClient = null;
  private Ship currShip;
  private Ship enemyShip;
  // Local convenience pointers
  // For our intents
  private static final int RC_SIGN_IN = 9001;
  final static int RC_SELECT_PLAYERS = 10000;
  final static int RC_LOOK_AT_MATCHES = 10001;

  // Should I be showing the turn API?
  public boolean isDoingTurn = false;

  // This is the current match we're in; null if not loaded
  public TurnBasedMatch mMatch;

  // This is the current match data after being unpersisted.
  // Do not retain references to match data once you have
  // taken an action on the match, such as takeTurn()
  public SkeletonTurn mTurnData;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Create the Google API Client with access to Games
    // Create the client used to sign in.
    mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

    // Setup signin and signout buttons
    findViewById(R.id.sign_out_button).setOnClickListener(this);
    findViewById(R.id.sign_in_button).setOnClickListener(this);

    checkPlaceholderIds();
  }

  // Check the sample to ensure all placeholder ids are are updated with real-world values.
  // This is strictly for the purpose of the samples; you don't need this in a production
  // application.
  private void checkPlaceholderIds() {
    StringBuilder problems = new StringBuilder();

    if (getPackageName().startsWith("com.google.")) {
      problems.append("- Package name start with com.google.*\n");
    }

    for (Integer id : new Integer[]{R.string.app_id}) {

      String value = getString(id);

      if (value.startsWith("YOUR_")) {
        // needs replacing
        problems.append("- Placeholders(YOUR_*) in ids.xml need updating\n");
        break;
      }
    }

    if (problems.length() > 0) {
      problems.insert(0, "The following problems were found:\n\n");

      problems.append("\nThese problems may prevent the app from working properly.");
      problems.append("\n\nSee the TODO window in Android Studio for more information");
      (new AlertDialog.Builder(this)).setMessage(problems.toString())
          .setNeutralButton(android.R.string.ok, null).create().show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    // Since the state of the signed in user can change when the activity is not active
    // it is recommended to try and sign in silently from when the app resumes.
    signInSilently();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister the invitation callbacks; they will be re-registered via
    // onResume->signInSilently->onConnected.
    if (mInvitationsClient != null) {
      mInvitationsClient.unregisterInvitationCallback(mInvitationCallback);
    }

    if (mTurnBasedMultiplayerClient != null) {
      mTurnBasedMultiplayerClient.unregisterTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
    }
  }

  private String mDisplayName;
  private String mPlayerId;

  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");

    mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(this, googleSignInAccount);
    mInvitationsClient = Games.getInvitationsClient(this, googleSignInAccount);

    Games.getPlayersClient(this, googleSignInAccount)
        .getCurrentPlayer()
        .addOnSuccessListener(
            new OnSuccessListener<Player>() {
              @Override
              public void onSuccess(Player player) {
                mDisplayName = player.getDisplayName();
                mPlayerId = player.getPlayerId();

                setViewVisibility();
              }
            }
        )
        .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

    Log.d(TAG, "onConnected(): Connection successful");

    // Retrieve the TurnBasedMatch from the connectionHint
    GamesClient gamesClient = Games.getGamesClient(this, googleSignInAccount);
    gamesClient.getActivationHint()
        .addOnSuccessListener(new OnSuccessListener<Bundle>() {
          @Override
          public void onSuccess(Bundle hint) {
            if (hint != null) {
              TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

              if (match != null) {
                updateMatch(match);
                Log.d( "updatematch","onconnected");
              }
            }
          }
        })
        .addOnFailureListener(createFailureListener(
            "There was a problem getting the activation hint!"));

    setViewVisibility();

    // As a demonstration, we are registering this activity as a handler for
    // invitation and match events.

    // This is *NOT* required; if you do not register a handler for
    // invitation events, you will get standard notifications instead.
    // Standard notifications may be preferable behavior in many cases.
    mInvitationsClient.registerInvitationCallback(mInvitationCallback);

    // Likewise, we are registering the optional MatchUpdateListener, which
    // will replace notifications you would get otherwise. You do *NOT* have
    // to register a MatchUpdateListener.
    mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
  }

  private void onDisconnected() {

    Log.d(TAG, "onDisconnected()");

    mTurnBasedMultiplayerClient = null;
    mInvitationsClient = null;

    setViewVisibility();
  }

  // This is a helper functio that will do all the setup to create a simple failure message.
  // Add it to any task and in the case of an failure, it will report the string in an alert
  // dialog.
  private OnFailureListener createFailureListener(final String string) {
    return new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        handleException(e, string);
      }
    };
  }

  // Displays your inbox. You will get back onActivityResult where
  // you will need to figure out what you clicked on.
  public void onCheckGamesClicked(View view) {
    mTurnBasedMultiplayerClient.getInboxIntent()
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            startActivityForResult(intent, RC_LOOK_AT_MATCHES);
          }
        })
        .addOnFailureListener(createFailureListener(getString(R.string.error_get_inbox_intent)));
  }

  // Open the create-game UI. You will get back an onActivityResult
  // and figure out what to do.
  public void onStartMatchClicked(View view) {
    mTurnBasedMultiplayerClient.getSelectOpponentsIntent(1, 7, true)
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            startActivityForResult(intent, RC_SELECT_PLAYERS);
          }
        })
        .addOnFailureListener(createFailureListener(
            getString(R.string.error_get_select_opponents)));
  }

  // Create a one-on-one automatch game.
  public void onQuickMatchClicked(View view) {

    Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(1, 1, 0);

    TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
        .setAutoMatchCriteria(autoMatchCriteria).build();

    showSpinner();

    // Start the match
    mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            onInitiateMatch(turnBasedMatch);
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
  }

  // In-game controls

  // Cancel the game. Should possibly wait until the game is canceled before
  // giving up on the view.
  public void onCancelClicked(View view) {
    showSpinner();

    mTurnBasedMultiplayerClient.cancelMatch(mMatch.getMatchId())
        .addOnSuccessListener(new OnSuccessListener<String>() {
          @Override
          public void onSuccess(String matchId) {
            onCancelMatch(matchId);
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem cancelling the match!"));

    isDoingTurn = false;
    setViewVisibility();
  }

  // Leave the game during your turn. Note that there is a separate
  // mTurnBasedMultiplayerClient.leaveMatch() if you want to leave NOT on your turn.
  public void onLeaveClicked(View view) {
    showSpinner();
    String nextParticipantId = getNextParticipantId();

    mTurnBasedMultiplayerClient.leaveMatchDuringTurn(mMatch.getMatchId(), nextParticipantId)
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            onLeaveMatch();
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem leaving the match!"));

    setViewVisibility();
  }

  // Finish the game. Sometimes, this is your only choice.
  public void onFinishClicked() {
    showSpinner();
    mTurnBasedMultiplayerClient.finishMatch(mMatch.getMatchId())
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            onUpdateMatch(turnBasedMatch);
            Log.d( "onupdatematch","onfinishedclick");
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem finishing the match!"));

    isDoingTurn = false;
    setViewVisibility();
  }


  // Upload your new gamestate, then take a turn, and pass it on to the next
  // player.
  public void onDoneClicked() {

    if(currShip.getHealth() == 0){
      Toast.makeText(getApplicationContext(), "You lost", Toast.LENGTH_SHORT).show();
      onFinishClicked();
    }
    else if(enemyShip.getHealth() == 0){
      Toast.makeText(getApplicationContext(), "You won", Toast.LENGTH_SHORT).show();
      onFinishClicked();
    }
    else {
      showSpinner();

      String nextParticipantId = getNextParticipantId();
      // Create the next turn
      mTurnData.turnCounter += 1;
      mTurnData.S1health = enemyShip.getHealth();
      Log.d("dS", "S1health: " + Integer.toString(mTurnData.S1health));
      mTurnData.S1shoot = enemyShip.getShooting();
      Log.d("dS", "S1shoot: " + mTurnData.S1shoot);
      mTurnData.S1x = enemyShip.getxCoordinate();
      Log.d("dS", "S1x: " + Integer.toString(mTurnData.S1x));
      mTurnData.S1y = enemyShip.getyCoordinate();
      Log.d("dS", "S1y: " + Integer.toString(mTurnData.S1y));
      mTurnData.S1move = enemyShip.getMoveSpeed();
      Log.d("dS", "S1move: " + Integer.toString(mTurnData.S1move));

      mTurnData.S2health = currShip.getHealth();
      mTurnData.S2move = currShip.getMoveSpeed();
      mTurnData.S2x = currShip.getxCoordinate();
      mTurnData.S2y = currShip.getyCoordinate();
      mTurnData.S2shoot = currShip.getShooting();

      Log.d("dS", "S2health: " + Integer.toString(mTurnData.S2health));

      Log.d("dS", "S2shoot: " + mTurnData.S2shoot);

      Log.d("dS", "S2x: " + Integer.toString(mTurnData.S2x));

      Log.d("dS", "S2y: " + Integer.toString(mTurnData.S2y));

      Log.d("dS", "S2move: " + Integer.toString(mTurnData.S2move));

      mTurnBasedMultiplayerClient.takeTurn(mMatch.getMatchId(),
              mTurnData.persist(), nextParticipantId)
              .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                @Override
                public void onSuccess(TurnBasedMatch turnBasedMatch) {
                  onUpdateMatch(turnBasedMatch);
                  Log.d("onupdatematch", "ondoneclick");
                }
              })
              .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
    }
      mTurnData = null;

  }

  // Sign-in, Sign out behavior

  // Update the visibility based on what state we're in.
  public void setViewVisibility() {
    boolean isSignedIn = mTurnBasedMultiplayerClient != null;

    if (!isSignedIn) {
      findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
      findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
      findViewById(R.id.matchup_layout).setVisibility(View.GONE);
      findViewById(R.id.gameplay_layout).setVisibility(View.GONE);

      if (mAlertDialog != null) {
        mAlertDialog.dismiss();
      }
      return;
    }


    ((TextView) findViewById(R.id.name_field)).setText(mDisplayName);
    findViewById(R.id.login_layout).setVisibility(View.GONE);

    if (isDoingTurn) {
      findViewById(R.id.matchup_layout).setVisibility(View.GONE);
      findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
      dismissSpinner();
    } else {
      findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
      findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
    }
  }

  // Switch to gameplay view.
  public void setGameplayUI() {
    Log.d("setGamplay","hi");
    isDoingTurn = true;
    setViewVisibility();
    if(!gameState) {
      gameState = true;

      final ArrayList<ArrayList<ImageButtonWrapper>> skeletonMap = new ArrayList<>();


      for (int j = 0; j < 5; j++) {
        final LinearLayout currLayout;
        switch (j) {
          case 0:
            currLayout = (LinearLayout) findViewById(R.id.first_row);
            break;
          case 1:
            currLayout = (LinearLayout) findViewById(R.id.second_row);
            break;
          case 2:
            currLayout = (LinearLayout) findViewById(R.id.third_row);
            break;
          case 3:
            currLayout = (LinearLayout) findViewById(R.id.fourth_row);
            break;
          case 4:
            currLayout = (LinearLayout) findViewById(R.id.fifth_row);
            break;
          default:
            currLayout = (LinearLayout) findViewById(R.id.fifth_row);
        }


        ArrayList<ImageButtonWrapper> currRow = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
          final ImageButtonWrapper imageButtonWrapper = new ImageButtonWrapper(getApplicationContext(), i, j);
          ImageButton btn = imageButtonWrapper.getButton();
          btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
          if (i == currShip.getxCoordinate() && j == currShip.getyCoordinate()) {
            if (currShip.getMoveSpeed() == 1)
              btn.setBackgroundResource(R.drawable.ship1);
            else
              btn.setBackgroundResource(R.drawable.ship2);
          } else
            btn.setBackgroundResource(R.drawable.water);

          btn.setId(j * 10 + i);
          btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              if (currentMap.pickMovePhase && imageButtonWrapper.getyCoord() == currShip.getyCoordinate() && imageButtonWrapper.getxCoord() == currShip.getxCoordinate()) {
                currentMap.calculateMove(currShip, COLOR);
                currentMap.pickMovePhase = false;
                currentMap.doMovePhase = true;
                Toast.makeText(getApplicationContext(), "Your health is: " + currShip.getHealth(), Toast.LENGTH_SHORT).show();
              } else if (currentMap.doMovePhase) {
                if (imageButtonWrapper.getMove()) {
                  currentMap.calculateMove(currShip, CLEAR);
                  skeletonMap.get(currShip.getyCoordinate()).get(currShip.getxCoordinate()).getButton().setBackgroundResource(R.drawable.water);

                  currentMap.move(currShip, imageButtonWrapper.getxCoord(), imageButtonWrapper.getyCoord());
                  ImageButton currButton = (ImageButton) view;
                  if (currShip.getMoveSpeed() == 1)
                    currButton.setBackgroundResource(R.drawable.ship1);
                  else
                    currButton.setBackgroundResource(R.drawable.ship2);
                  currentMap.doMovePhase = false;
                  currentMap.pickAttackPhase = true;
                } else {
                  Toast.makeText(getApplicationContext(), "Invalid Move", Toast.LENGTH_SHORT).show();
                  currentMap.calculateMove(currShip, CLEAR);
                  currentMap.doMovePhase = false;
                  currentMap.pickMovePhase = true;
                }
              } else if (currentMap.pickAttackPhase) {
                currentMap.calculateShoot(currShip, COLOR);
                currentMap.pickAttackPhase = false;
                currentMap.doAttackPhase = true;
              } else if (currentMap.doAttackPhase) {
                if (imageButtonWrapper.getShoot()) {
                  currentMap.calculateShoot(currShip, CLEAR);
                  currentMap.attack(imageButtonWrapper.getxCoord(), imageButtonWrapper.getyCoord(), currShip, enemyShip);
                  currentMap.doAttackPhase = false;
                  currentMap.pickMovePhase =true;
                  onDoneClicked();

                } else {
                  Toast.makeText(getApplicationContext(), "Invalid Attack", Toast.LENGTH_SHORT).show();
                  currentMap.calculateShoot(currShip, CLEAR);
                  currentMap.pickAttackPhase = true;
                  currentMap.doAttackPhase = false;

                }

              }
            }
          });
          currRow.add(imageButtonWrapper);
          currLayout.addView(btn);
        }
        skeletonMap.add(currRow);

      }

      currentMap = new MyMap(getApplicationContext(), skeletonMap);
    }

  }

  // Helpful dialogs

  public void showSpinner() {
    findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
  }

  public void dismissSpinner() {
    findViewById(R.id.progressLayout).setVisibility(View.GONE);
  }

  // Generic warning/info dialog
  public void showWarning(String title, String message) {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

    // set title
    alertDialogBuilder.setTitle(title).setMessage(message);

    // set dialog message
    alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int id) {
            // if this button is clicked, close
            // current activity
          }
        });

    // create alert dialog
    mAlertDialog = alertDialogBuilder.create();

    // show it
    mAlertDialog.show();
  }

  public void showEndingWarning(String title, String message) {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

    // set title
    alertDialogBuilder.setTitle(title).setMessage(message);

    // set dialog message
    alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                onFinishClicked();
              }
            });

    // create alert dialog
    mAlertDialog = alertDialogBuilder.create();

    // show it
    mAlertDialog.show();
  }

  // Rematch dialog
  public void askForRematch() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

    alertDialogBuilder.setMessage("Do you want a rematch?");

    alertDialogBuilder
        .setCancelable(false)
        .setPositiveButton("Sure, rematch!",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
                rematch();
              }
            })
        .setNegativeButton("No.",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int id) {
              }
            });

    alertDialogBuilder.show();
  }

  /**
   * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
   * your Activity's onActivityResult function
   */
  public void startSignInIntent() {
    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
  }

  /**
   * Try to sign in without displaying dialogs to the user.
   * <p>
   * If the user has already signed in previously, it will not show dialog.
   */
  public void signInSilently() {
    Log.d(TAG, "signInSilently()");

    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
        new OnCompleteListener<GoogleSignInAccount>() {
          @Override
          public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
            if (task.isSuccessful()) {
              Log.d(TAG, "signInSilently(): success");
              onConnected(task.getResult());
            } else {
              Log.d(TAG, "signInSilently(): failure", task.getException());
              onDisconnected();
            }
          }
        });
  }

  public void signOut() {
    Log.d(TAG, "signOut()");

    mGoogleSignInClient.signOut().addOnCompleteListener(this,
        new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {

            if (task.isSuccessful()) {
              Log.d(TAG, "signOut(): success");
            } else {
              handleException(task.getException(), "signOut() failed!");
            }

            onDisconnected();
          }
        });
  }

  /**
   * Since a lot of the operations use tasks, we can use a common handler for whenever one fails.
   *
   * @param exception The exception to evaluate.  Will try to display a more descriptive reason for
   *                  the exception.
   * @param details   Will display alongside the exception if you wish to provide more details for
   *                  why the exception happened
   */
  private void handleException(Exception exception, String details) {
    int status = 0;

    if (exception instanceof TurnBasedMultiplayerClient.MatchOutOfDateApiException) {
      TurnBasedMultiplayerClient.MatchOutOfDateApiException matchOutOfDateApiException =
          (TurnBasedMultiplayerClient.MatchOutOfDateApiException) exception;

      new AlertDialog.Builder(this)
          .setMessage("Match was out of date, updating with latest match data...")
          .setNeutralButton(android.R.string.ok, null)
          .show();

      TurnBasedMatch match = matchOutOfDateApiException.getMatch();
      updateMatch(match);
      Log.d("updatematch","jhandle");

      return;
    }

    if (exception instanceof ApiException) {
      ApiException apiException = (ApiException) exception;
      status = apiException.getStatusCode();
    }

    if (!checkStatusCode(status)) {
      return;
    }

    String message = getString(R.string.status_exception_error, details, status, exception);

    new AlertDialog.Builder(this)
        .setMessage(message)
        .setNeutralButton(android.R.string.ok, null)
        .show();
  }

  private void logBadActivityResult(int requestCode, int resultCode, String message) {
    Log.i(TAG, "Bad activity result(" + resultCode + ") for request (" + requestCode + "): "
        + message);
  }

  // This function is what gets called when you return from either the Play
  // Games built-in inbox, or else the create game built-in interface.
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

    if (requestCode == RC_SIGN_IN) {

      Task<GoogleSignInAccount> task =
          GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        onConnected(account);
      } catch (ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null || message.isEmpty()) {
          message = getString(R.string.signin_other_error);
        }

        onDisconnected();

        new AlertDialog.Builder(this)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .show();
      }
    } else if (requestCode == RC_LOOK_AT_MATCHES) {
      // Returning from the 'Select Match' dialog

      if (resultCode != Activity.RESULT_OK) {
        logBadActivityResult(requestCode, resultCode,
            "User cancelled returning from the 'Select Match' dialog.");
        return;
      }

      TurnBasedMatch match = intent
          .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

      if (match != null) {
        updateMatch(match);
        Log.d( "updatematch","onactivityresult");
      }

      Log.d(TAG, "Match = " + match);
    } else if (requestCode == RC_SELECT_PLAYERS) {
      // Returning from 'Select players to Invite' dialog

      if (resultCode != Activity.RESULT_OK) {
        // user canceled
        logBadActivityResult(requestCode, resultCode,
            "User cancelled returning from 'Select players to Invite' dialog");
        return;
      }

      // get the invitee list
      ArrayList<String> invitees = intent
          .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

      // get automatch criteria
      Bundle autoMatchCriteria;

      int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
      int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

      if (minAutoMatchPlayers > 0) {
        autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers,
            maxAutoMatchPlayers, 0);
      } else {
        autoMatchCriteria = null;
      }

      TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
          .addInvitedPlayers(invitees)
          .setAutoMatchCriteria(autoMatchCriteria).build();

      // Start the match
      mTurnBasedMultiplayerClient.createMatch(tbmc)
          .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
            @Override
            public void onSuccess(TurnBasedMatch turnBasedMatch) {
              onInitiateMatch(turnBasedMatch);
            }
          })
          .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
      showSpinner();
    }
  }

  // startMatch() happens in response to the createTurnBasedMatch()
  // above. This is only called on success, so we should have a
  // valid match object. We're taking this opportunity to setup the
  // game, saving our initial state. Calling takeTurn() will
  // callback to OnTurnBasedMatchUpdated(), which will show the game
  // UI.
  public void startMatch(TurnBasedMatch match) {
    mTurnData = new SkeletonTurn();
    // Some basic turn data
    currShip = new Ship();
    enemyShip = new Ship();

    mTurnData.S1health = enemyShip.getHealth();
    mTurnData.S1shoot = enemyShip.getShooting();
    mTurnData.S1x = enemyShip.getxCoordinate();
    mTurnData.S1y = enemyShip.getyCoordinate();
    mTurnData.S1move = enemyShip.getMoveSpeed();

    mTurnData.S2health = currShip.getHealth();
    mTurnData.S2move = currShip.getMoveSpeed();
    mTurnData.S2x = currShip.getxCoordinate();
    mTurnData.S2y = currShip.getyCoordinate();
    mTurnData.S2shoot= currShip.getShooting();

    mMatch = match;

    String myParticipantId = mMatch.getParticipantId(mPlayerId);

    showSpinner();

    mTurnBasedMultiplayerClient.takeTurn(match.getMatchId(),
        mTurnData.persist(), myParticipantId)
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            updateMatch(turnBasedMatch);
            Log.d( "updatematch","startmatch");
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
  }

  // If you choose to rematch, then call it and wait for a response.
  public void rematch() {
    showSpinner();
    mTurnBasedMultiplayerClient.rematch(mMatch.getMatchId())
        .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
          @Override
          public void onSuccess(TurnBasedMatch turnBasedMatch) {
            onInitiateMatch(turnBasedMatch);
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem starting a rematch!"));
    mMatch = null;
    isDoingTurn = false;
  }

  /**
   * Get the next participant. In this function, we assume that we are
   * round-robin, with all known players going before all automatch players.
   * This is not a requirement; players can go in any order. However, you can
   * take turns in any order.
   *
   * @return participantId of next player, or null if automatching
   */
  public String getNextParticipantId() {

    String myParticipantId = mMatch.getParticipantId(mPlayerId);

    ArrayList<String> participantIds = mMatch.getParticipantIds();

    int desiredIndex = -1;

    for (int i = 0; i < participantIds.size(); i++) {
      if (participantIds.get(i).equals(myParticipantId)) {
        desiredIndex = i + 1;
      }
    }

    if (desiredIndex < participantIds.size()) {
      return participantIds.get(desiredIndex);
    }

    if (mMatch.getAvailableAutoMatchSlots() <= 0) {
      // You've run out of automatch slots, so we start over.
      return participantIds.get(0);
    } else {
      // You have not yet fully automatched, so null will find a new
      // person to play against.
      return null;
    }
  }

  // This is the main function that gets called when players choose a match
  // from the inbox, or else create a match and want to start it.
  public void updateMatch(TurnBasedMatch match) {
    mMatch = match;

    int status = match.getStatus();
    int turnStatus = match.getTurnStatus();
    boolean finished = false;
    switch (status) {
      case TurnBasedMatch.MATCH_STATUS_CANCELED:
        showWarning("Canceled!", "This game was canceled!");
        return;
      case TurnBasedMatch.MATCH_STATUS_EXPIRED:
        showWarning("Expired!", "This game is expired.  So sad!");
        return;
      case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
        showWarning("Waiting for auto-match...",
            "We're still waiting for an automatch partner.");
        return;
      case TurnBasedMatch.MATCH_STATUS_COMPLETE:
        if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
          showWarning("Complete!",
              "This game is over; someone finished it, and so did you!  " +
                  "There is nothing to be done.");
          break;
        }

        // Note that in this state, you must still call "Finish" yourself,
        // so we allow this to continue.
        showEndingWarning("Complete!",
            "This game is over. You lost!");

    }

    // OK, it's active. Check on turn status.
    switch (turnStatus) {
      case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
        mTurnData = SkeletonTurn.unpersist(mMatch.getData());
        if (mTurnData!=null) {
          currShip = new Ship(mTurnData.S1x, mTurnData.S1y, mTurnData.S1health, mTurnData.S1move, mTurnData.S1shoot);
          enemyShip = new Ship(mTurnData.S2x, mTurnData.S2y, mTurnData.S2health, mTurnData.S2move, mTurnData.S2shoot);

        }
        setGameplayUI();

        Log.d("dR","S1health: "+Integer.toString(mTurnData.S1health));

        Log.d("dR","S1shoot: "+mTurnData.S1shoot);

        Log.d("dR","S1x: "+Integer.toString(mTurnData.S1x));

        Log.d("dR","S1y: "+Integer.toString(mTurnData.S1y));

        Log.d("dR","S1move: "+Integer.toString(mTurnData.S1move));

        Log.d("dR","S2health: "+Integer.toString(mTurnData.S2health));

        Log.d("dR","S2shoot: "+mTurnData.S2shoot);

        Log.d("dR","S2x: "+Integer.toString(mTurnData.S2x));

        Log.d("dR","S2y: "+Integer.toString(mTurnData.S2y));

        Log.d("dR","S2move: "+Integer.toString(mTurnData.S2move));
        return;
      case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
        // Should return results.
        showWarning("Alas...", "It's not your turn.");
        break;
      case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
        showWarning("Good inititative!",
            "Still waiting for invitations.\n\nBe patient!");
    }

    mTurnData = null;

    setViewVisibility();
  }

  private void onCancelMatch(String matchId) {
    dismissSpinner();

    isDoingTurn = false;

    showWarning("Match", "This match (" + matchId + ") was canceled.  " +
        "All other players will have their game ended.");
  }

  private void onInitiateMatch(TurnBasedMatch match) {
    dismissSpinner();

    if (match.getData() != null) {
      // This is a game that has already started, so I'll just start
      updateMatch(match);
      Log.d( "updatematch","oninitmatch");
      return;
    }

    startMatch(match);
  }

  private void onLeaveMatch() {
    dismissSpinner();

    isDoingTurn = false;
    showWarning("Left", "You've left this match.");
  }


  public void onUpdateMatch(TurnBasedMatch match) {
    dismissSpinner();

    if (match.canRematch()) {
      askForRematch();
    }

    isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

    if (isDoingTurn) {
      updateMatch(match);
      Log.d( "updatematch","onupdatematch");
      return;
    }

    setViewVisibility();
  }

  private InvitationCallback mInvitationCallback = new InvitationCallback() {
    // Handle notification events.
    @Override
    public void onInvitationReceived(@NonNull Invitation invitation) {
      Toast.makeText(
          SkeletonActivity.this,
          "An invitation has arrived from "
              + invitation.getInviter().getDisplayName(), Toast.LENGTH_SHORT)
          .show();
    }

    @Override
    public void onInvitationRemoved(@NonNull String invitationId) {
      Toast.makeText(SkeletonActivity.this, "An invitation was removed.", Toast.LENGTH_SHORT)
          .show();
    }
  };

  private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
    @Override
    public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {
      Toast.makeText(SkeletonActivity.this, "A match was updated.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTurnBasedMatchRemoved(@NonNull String matchId) {
      Toast.makeText(SkeletonActivity.this, "A match was removed.", Toast.LENGTH_SHORT).show();
    }
  };

  public void showErrorMessage(int stringId) {
    showWarning("Warning", getResources().getString(stringId));
  }

  // Returns false if something went wrong, probably. This should handle
  // more cases, and probably report more accurate results.
  private boolean checkStatusCode(int statusCode) {
    switch (statusCode) {
      case GamesCallbackStatusCodes.OK:
        return true;

      case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
        showErrorMessage(R.string.status_multiplayer_error_not_trusted_tester);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
        showErrorMessage(R.string.match_error_already_rematched);
        break;
      case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
        showErrorMessage(R.string.network_error_operation_failed);
        break;
      case GamesClientStatusCodes.INTERNAL_ERROR:
        showErrorMessage(R.string.internal_error);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
        showErrorMessage(R.string.match_error_inactive_match);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
        showErrorMessage(R.string.match_error_locally_modified);
        break;
      default:
        showErrorMessage(R.string.unexpected_status);
        Log.d(TAG, "Did not have warning or string to deal with: "
            + statusCode);
    }

    return false;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.sign_in_button:
        mMatch = null;
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        startSignInIntent();
        break;
      case R.id.sign_out_button:
        signOut();
        setViewVisibility();
        break;
    }
  }
}
