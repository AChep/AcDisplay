/*
 * Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.achep.snake;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.achep.acdisplay.R;
import com.achep.base.Device;
import com.achep.base.ui.widgets.TextView;
import com.achep.snake.snake.Animal;
import com.achep.snake.snake.ColorScheme;
import com.achep.snake.snake.GameObject;


public class MainActivity extends AppCompatActivity implements
        GameObject.OnGameObjectChangedListener {

    private Logic mLogic;

    private TextView mScoreTextView;
    private Animator mScorePulseAnimation;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            int visibilityUi = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;

            if (Device.hasKitKatApi()) {
                // Hide navigation bar and flag sticky.
                visibilityUi = visibilityUi
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            getWindow().getDecorView().setSystemUiVisibility(visibilityUi);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snake_fragment);

        GameView gameView = (GameView) findViewById(R.id.game_view);
        mLogic = new Logic(gameView, 12, 16,
                new ColorScheme(0xFF03A9F4, 0xFF0277BD), // Snake
                new ColorScheme(0xFF8BC34A, 0xFF558B2F), // Apple
                new ColorScheme(0xFFF44336, 0xFFC62828) // Bricks
        );
        gameView.setLogic(mLogic);

        ViewGroup scorePanel = (ViewGroup) findViewById(R.id.score_panel);
        mScoreTextView = (TextView) scorePanel.findViewById(R.id.score);
        ControllerView controller = (ControllerView) findViewById(R.id.game_controller);
        controller.setCallback(mLogic);

        mScorePulseAnimation = AnimatorInflater.loadAnimator(
                this, R.animator.pulse);
        mScorePulseAnimation.setTarget(scorePanel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLogic.getSnake().registerListener(this);
        updateScoreTextView();
    }

    @Override
    protected void onPause() {
        mLogic.getSnake().unregisterListener(this);
        mScorePulseAnimation.end();
        super.onPause();
    }

    private void updateScoreTextView() {
        int score = mLogic.getScore();
        mScoreTextView.setText(String.valueOf(score));
        mScorePulseAnimation.start();
    }

    @Override
    public void onGameObjectChanged(GameObject object, int action) {
        if (object == mLogic.getSnake()) {
            switch (action) {
                case GameObject.ACTION_SIZE_CHANGED:
                    if (mLogic.getScore() == 0) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Game over.")
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        finish();
                                    }
                                })
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .create()
                                .show();
                    }
                    updateScoreTextView();
                    break;
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        byte direction;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_NUMPAD_2:
                direction = Animal.DIRECTION_DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_NUMPAD_8:
                direction = Animal.DIRECTION_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_NUMPAD_6:
                direction = Animal.DIRECTION_RIGHT;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_NUMPAD_4:
                direction = Animal.DIRECTION_LEFT;
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }

        mLogic.onDirectionChange(direction);
        return true;
    }

}
