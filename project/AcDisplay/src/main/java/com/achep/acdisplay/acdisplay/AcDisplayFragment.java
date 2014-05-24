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

package com.achep.acdisplay.acdisplay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import com.achep.acdisplay.Build;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.Device;
import com.achep.acdisplay.R;
import com.achep.acdisplay.Timeout;
import com.achep.acdisplay.acdisplay.components.NotificationUI;
import com.achep.acdisplay.acdisplay.components.Widget;
import com.achep.acdisplay.activities.KeyguardActivity;
import com.achep.acdisplay.compat.SceneCompat;
import com.achep.acdisplay.notifications.NotificationPresenter;
import com.achep.acdisplay.notifications.NotificationUtils;
import com.achep.acdisplay.notifications.OpenStatusBarNotification;
import com.achep.acdisplay.utils.MathUtils;
import com.achep.acdisplay.utils.ViewUtils;
import com.achep.acdisplay.view.ElasticValue;
import com.achep.acdisplay.view.ForwardingLayout;
import com.achep.acdisplay.view.ForwardingListener;
import com.achep.acdisplay.widgets.ProgressBar;
import com.achep.acdisplay.widgets.TimeView;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is main fragment of ActiveDisplay app.
 */
public class AcDisplayFragment extends Fragment implements
View.OnTouchListener {

	private static final String TAG = "AcDisplayFragment";

	public static final int SCENE_UNLOCK = 1;
	public static final int SCENE_NOTIFICATION = 2;
	public static final int SCENE_MUSIC_CONTROLS = 3;

	private Config mConfig;
	private NotificationPresenter mPresenter;
	private NotificationListener mNotificationListener = new NotificationListener();

	private ForwardingLayout mSceneContainer;
	private LinearLayout mCollapsedViewsContainer;
	private HashMap<View, Widget> mWidgetsMap = new HashMap<>();
	private HashMap<String, SceneCompat> mScenesMap = new HashMap<>();
	private boolean mCollapsedViewsNeedsUpdate;

	private Widget mSelectedWidget;

	private SceneCompat mCurrentScene;
	private SceneCompat mSceneMain;
	private Transition mTransition;

	private boolean mTouched;

	private Timeout mTimeout;
	private Timeout.Gui mTimeoutGui;
	private ForwardingListener mForwardingListener;

	private int mMaxFlingVelocity;
	private int mMinFlingVelocity;

	public void unlock(Runnable runnable, boolean finish) {
		Activity activity = getActivity();
		if (activity instanceof KeyguardActivity) {
			KeyguardActivity keyguard = (KeyguardActivity) activity;
			keyguard.unlock(runnable, finish);
		} else {
			if (runnable != null) {
				runnable.run();
			}
		}
	}

	public void dispatchSetBackground(Bitmap bitmap, int mask) {
		Activity activity = getActivity();
		if (activity instanceof AcDisplayActivity) {
			AcDisplayActivity a = (AcDisplayActivity) activity;
			a.dispatchSetBackground(a.isBackground(mask) ? bitmap : null);
		}
	}

	public Config getConfig() {
		return mConfig;
	}

	private SceneCompat findSceneByWidget(Widget fragment) {
		return fragment.hasExpandedView() ? mScenesMap.get(fragment.getClass().getName()) : null;
	}

	private Widget findWidgetByCollapsedView(View view) {
		return mWidgetsMap.get(view);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		ViewConfiguration vc = ViewConfiguration.get(activity);
		mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
		mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
	}

	@Override
	public void onResume() {
		super.onResume();
		mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal());
		ViewGroup clockView = mSceneMain.getView();
		TimeView clock = (TimeView) clockView.findViewById(R.id.time);
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), mConfig.getClockFont());
		clock.setTypeface(tf);
		clock.setTextColor(mConfig.getClockColor());
		clock.setTextSize((float)mConfig.getClockSize());
	}

	@TargetApi(android.os.Build.VERSION_CODES.KITKAT)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.acdisplay_fragment, container, false);
		assert root != null;

		mSceneContainer = (ForwardingLayout) root.findViewById(R.id.container);
		mSceneContainer.setVibrateOnForwardedEventEnabled(true);
		mCollapsedViewsContainer = (LinearLayout) root.findViewById(R.id.list);
		mCollapsedViewsContainer.setOnTouchListener(this);

		mForwardingListener = new ForwardingListener(mCollapsedViewsContainer) {
			@Override
			public ForwardingLayout getForwardingLayout() {
				return mSceneContainer;
			}
		};
		mWidgetTranslatorX = new ElasticValue.TranslationX(mSceneContainer, 500);
		mWidgetTranslatorX.setListener(new ElasticValue.Listener() {
			@Override
			public void onValueChanged(View view, float value) {
				final int width = view.getWidth();
				final float alh = 1f - Math.min(width, Math.abs(value)) / width;
				view.setAlpha(alh);
			}
		});

		ViewGroup sceneMain = (ViewGroup) inflater.inflate(R.layout.acdisplay_scene_clock, mSceneContainer, false);
		if (Device.hasKitKatApi()) {
			mSceneMain = new SceneCompat(mSceneContainer, sceneMain);
			if (getResources().getBoolean(R.bool.config_transition_fade)) {
				mTransition = new TransitionSet()
				.setOrdering(TransitionSet.ORDERING_TOGETHER)
				.addTransition(new Fade())
				.addTransition(new ChangeBounds());
			} else {
				mTransition = new ChangeBounds();
			}
		} else {
			mSceneMain = new SceneCompat(mSceneContainer, sceneMain);
		}
		mCurrentScene = mSceneMain;
		mSceneMain.enter();

		Config config = Config.getInstance();
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), config.getClockFont());
		TimeView clock = (TimeView)sceneMain.findViewById(R.id.time);
		clock.setTypeface(tf);
		clock.setTextColor(config.getClockColor());
		clock.setTextSize((float)config.getClockSize());

		// /////////////////
		// ~~ TIMEOUT GUI ~~
		// /////////////////
		ProgressBar progressBar;
		ViewStub progressBarStub = (ViewStub) root.findViewById(R.id.progress_bar_stub);
		if (config.isMirroredTimeoutProgressBarEnabled()) {
			progressBarStub.setLayoutResource(R.layout.acdisplay_progress_bar_mirrored);
			progressBar = (ProgressBar) progressBarStub.inflate().findViewById(R.id.progress_bar);

			// Redirect all changes from the main progress bar
			// to mirrored one.
			final ProgressBar progressBarMirrored = (ProgressBar)
			root.findViewById(R.id.progress_bar_mirrored);
			progressBar.setOnProgressChangeListener(new ProgressBar.OnProgressChangeListener() {

				@Override
				public void onProgressChanged(ProgressBar progressBar, int progress) {
					progressBarMirrored.setProgress(progress);
				}

				@Override
				public void onMaxChanged(ProgressBar progressBar, int max) {
					progressBarMirrored.setMax(max);
				}
			});
		} else {
			progressBar = (ProgressBar) progressBarStub.inflate().findViewById(R.id.progress_bar);
		}

		Activity activity = getActivity();
		if (activity instanceof AcDisplayActivity) {
			mTimeoutGui = new Timeout.Gui(progressBar);

			AcDisplayActivity a = (AcDisplayActivity) activity;
			mTimeout = a.getTimeout();
			mTimeout.registerListener(mTimeoutGui);
		} else {
			mTimeout = new Timeout(); // fake timeout that does nothing
			progressBar.setProgress(progressBar.getMax());
		}

		return root;
	}

	@Override
	public void onStart() {
		super.onStart();
		Context context = getActivity();
		assert context != null;

		mConfig = Config.getInstance();
		mPresenter = NotificationPresenter.getInstance();
		mPresenter.addOnNotificationListChangedListener(mNotificationListener);
		updateNotificationList();

		getView().setOnTouchListener(this);
	}

	@Override
	public void onStop() {
		mPresenter.removeOnNotificationListChangedListener(mNotificationListener);
		super.onStop();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mTimeout != null) {
			mTimeout.unregisterListener(mTimeoutGui);
		}
	}

	private float[] mStdTouchGen = new float[2];
	private float[] mStdTouchPrev = new float[2];

	private VelocityTracker mVelocityTracker;
	private ElasticValue mWidgetTranslatorX;
	private Handler mTouchHandler = new Handler();

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == mCollapsedViewsContainer) {
			mForwardingListener.onTouch(mCollapsedViewsContainer, event);

			final int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				mTouched = true;

				// Restart timeout and immediately pause it to
				// get full line of timeout.
				mTimeout.setTimeoutDelayed(mConfig.getTimeoutShort(), true);
				mTimeout.pause();

				// Track the velocity of movement, so we
				// can do swipe-to-dismiss.
				mVelocityTracker = VelocityTracker.obtain();
				mStdTouchGen[0] = 0;
				mStdTouchGen[1] = 0;

				// ///////////////
				// ~~ FALL DOWN ~~
				// ///////////////
			case MotionEvent.ACTION_MOVE:

				// Probably best solution would be to use
				// motion forwarding... But it's more complex.
				final float rawX = event.getRawX();
				final float rawY = event.getRawY();

				boolean anythingPressed = false;
				final int length = mCollapsedViewsContainer.getChildCount();
				for (int i = 0; i < length; i++) {
					final View child = mCollapsedViewsContainer.getChildAt(i);
					assert child != null;
					if (child.getVisibility() != View.VISIBLE) continue;

					// Check if current touch is on view, simulate pressing
					// and update its state so view can update background etc.
					final boolean pressedOld = child.isPressed();
					final boolean pressed = ViewUtils.isTouchPointInView(child, rawX, rawY);
					child.setPressed(pressed);

					if (pressed) anythingPressed = true;
					if (pressed != pressedOld && !child.isSelected()) {
						child.refreshDrawableState();
						if (pressed) {
							mTouchHandler.removeCallbacksAndMessages(null);
							mTouchHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									showWidget(findWidgetByCollapsedView(child));
								}
							}, action == MotionEvent.ACTION_DOWN ? 0 : 120);
						}
					}
				}
				if (!anythingPressed) {
					mTouchHandler.removeCallbacksAndMessages(null);
				}

				addSwipeMovement(event, action == MotionEvent.ACTION_DOWN);

				break;
			case MotionEvent.ACTION_UP:

				if (mSelectedWidget != null && mSelectedWidget.isDismissible()) {
					addSwipeMovement(event, false);
					mVelocityTracker.computeCurrentVelocity(1000);
					float velocityX = mVelocityTracker.getXVelocity();
					velocityX = velocityX >= 0
							? Math.max(0, velocityX - mWidgetTranslatorX.getStrength())
									: Math.min(0, velocityX + mWidgetTranslatorX.getStrength());

							boolean dismiss = false;
							final boolean dismissRight;

							int width = mWidgetTranslatorX.getView().getWidth();
							float absVelocityX = Math.abs(velocityX);
							float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
							float deltaX = mWidgetTranslatorX.getValue();
							float absDeltaX = Math.abs(deltaX);
							if (absDeltaX > width / 4) {
								dismiss = true;
								dismissRight = deltaX > 0;
							} else if (mMinFlingVelocity <= absVelocityX
									&& absVelocityX <= mMaxFlingVelocity
									&& absVelocityY * 2 < absVelocityX
									&& absDeltaX > width / 6) {
								// dismiss only if flinging in the same direction as dragging
								dismiss = (velocityX < 0) == (deltaX < 0);
								dismissRight = mVelocityTracker.getXVelocity() > 0;
							} else {
								dismissRight = false;
							}

							if (dismiss) {
								mTouched = false;
								mTouchHandler.removeCallbacksAndMessages(null);

								int duration = Math.round(absDeltaX * 1000f / Math.max(absVelocityX, 500f));

								mWidgetTranslatorX.stop();
								mWidgetTranslatorX.getView().animate()
								.alpha(0)
								.translationX(deltaX + width * MathUtils.charge(deltaX))
								.setDuration(duration)
								.setListener(new AnimatorListenerAdapter() {
									@Override
									public void onAnimationEnd(Animator animation) {
										onAnimationCancel(animation);
										endTouch();
									}

									@Override
									public void onAnimationCancel(Animator animation) {
										mSelectedWidget.onDismissed(!dismissRight);
										mWidgetTranslatorX.setValue(0f);
									}
								});
								break;
							}
				}

				// ///////////////
				// ~~ FALL DOWN ~~
				// ///////////////
			case MotionEvent.ACTION_CANCEL:
				mTouchHandler.removeCallbacksAndMessages(null);
				endTouch();
				break;
			}
			return true;
		}
		return false;
	}

	private void addSwipeMovement(MotionEvent srcEvent, boolean ignoreMovement) {
		final float x = srcEvent.getX();
		final float y = srcEvent.getY();

		if (y > 0 || ignoreMovement) {
			MotionEvent dstEvent = MotionEvent.obtainNoHistory(srcEvent);
			assert dstEvent != null;

			final float deltaX = x - mStdTouchPrev[0];
			final float deltaY = y - mStdTouchPrev[1];
			dstEvent.offsetLocation(
					(mStdTouchGen[0] += deltaX) - x,
					(mStdTouchGen[1] += deltaY) - y);
			mVelocityTracker.addMovement(dstEvent);

			if (mSelectedWidget != null  && mSelectedWidget.isDismissible()) {
				mWidgetTranslatorX.move(deltaX);
			}

			dstEvent.recycle();
		}

		writeCoordinate(mStdTouchPrev, srcEvent);
	}

	private void writeCoordinate(float[] array, MotionEvent event) {
		array[0] = event.getX();
		array[1] = event.getY();
	}

	private void endTouch() {
		int length = mCollapsedViewsContainer.getChildCount();
		for (int i = 0; i < length; i++) {
			View child = mCollapsedViewsContainer.getChildAt(i);
			assert child != null;
			child.setPressed(false);
			child.refreshDrawableState();
		}

		showMainWidget();

		if (mCollapsedViewsNeedsUpdate) updateNotificationList();
		if (mTimeout != null) {
			mTimeout.resume();
		}

		mTouched = false;
		mCollapsedViewsNeedsUpdate = false;
	}

	@SuppressLint("NewApi")
	private void showWidget(Widget widget) {
		if (mSelectedWidget != null) {
			mSelectedWidget.getCollapsedView().setSelected(false);
			mSelectedWidget.onExpandedViewDetached();
		}

		mSelectedWidget = widget;

		mVelocityTracker.clear();
		mWidgetTranslatorX.reset();

		dispatchSetBackground(null, 0);

		if (mSelectedWidget == null) {
			goScene(mSceneMain);
		} else {
			SceneCompat scene = findSceneByWidget(mSelectedWidget);
			if (scene == null) {

				// Widget doesn't have a large view, so
				// display clock.
				goScene(mSceneMain);
			} else if (mCurrentScene != scene) {
				goScene(scene);
			} else if (Device.hasKitKatApi() && mSelectedWidget.hasExpandedView()) {
				TransitionManager.beginDelayedTransition(
						mSelectedWidget.getExpandedView(),
						mTransition);
			}

			mSelectedWidget.onExpandedViewAttached();
			mSelectedWidget.getCollapsedView().setSelected(true);
			mSelectedWidget.getCollapsedView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			dispatchSetBackground(mSelectedWidget.getBackground(), Config.DYNAMIC_BG_NOTIFICATION_MASK);
		}
	}

	public void showMainWidget() {
		showWidget(null);
	}

	/**
	 * Changes current scene to given one.
	 */
	@SuppressLint("NewApi")
	private void goScene(SceneCompat sceneCompat) {
		if (mCurrentScene != sceneCompat) {
			mCurrentScene = sceneCompat;
			if (Device.hasKitKatApi()) {
				TransitionManager.go(sceneCompat.scene, mTransition);
			} else sceneCompat.enter();
		}
	}

	/**
	 * <pre>
	 *     Items sorted by priority:
	 *          [ UNLOCK ]
	 *          [ MUSIC ]
	 *          [ NOTIFICATION ]
	 * </pre>
	 * This method is full of black magic!
	 */
	// TODO: Optimize it
	// Spent hours on optimizing with no result: 0h
	@SuppressLint("NewApi")
	private void updateNotificationList() {
		if (getActivity() == null) {
			return;
		}

		long now = SystemClock.elapsedRealtime();

		ViewGroup container = mCollapsedViewsContainer;
		final int childCount = container.getChildCount();

		if (Device.hasKitKatApi()) {
			TransitionManager.beginDelayedTransition(container);
		}

		// Count the number of non-notification fragments
		// such as unlock or music controls fragments.
		int fragmentsExtraCount = 0;
		for (int i = 0; i < childCount; i++) {
			View child = container.getChildAt(i);
			Widget fragment = findWidgetByCollapsedView(child);
			if (!(fragment instanceof NotificationUI)) {
				fragmentsExtraCount++;
			} else {
				// Those fragments are placed at the begin of layout
				// so no reason to continue searching.
				break;
			}
		}

		final ArrayList<OpenStatusBarNotification> list = mPresenter.getList();
		final int notifyCount = list.size();

		final boolean[] notifyUsed = new boolean[notifyCount];
		final boolean[] childUsed = new boolean[childCount];

		// ///////////////////
		// ~~ NOTIFICATIONS ~~
		// ///////////////////

		// Does not need an update
		for (int i = fragmentsExtraCount; i < childCount; i++) {
			View child = container.getChildAt(i);
			NotificationUI widget = (NotificationUI) findWidgetByCollapsedView(child);
			OpenStatusBarNotification target = widget.getNotification();

			// Try to find the notification with the same
			// id, tag and package name as in present.
			for (int j = 0; j < notifyCount; j++) {
				OpenStatusBarNotification n = list.get(j);
				if (NotificationUtils.equals(target, n)) {

					notifyUsed[j] = true;
					childUsed[i] = true;

					if (target != n) {
						widget.setNotification(n);
					}
					break;
				}
			}
		}

		// Re-use free views and remove redundant views.
		boolean removeAllAfter = false;
		for (int a = fragmentsExtraCount, j = 0, offset = 0; a < childCount; a++) {
			if (childUsed[a]) continue;
			final int i = a + offset;

			View child = container.getChildAt(i);
			removing_all_next_views:
			{
				if (!removeAllAfter) {
					for (; j < notifyCount; j++) {
						if (notifyUsed[j]) continue;

						assert child != null;
						notifyUsed[j] = true;

						NotificationUI fragment = (NotificationUI) findWidgetByCollapsedView(child);
						fragment.setNotification(list.get(j));
						break removing_all_next_views;
					}
				}
				removeAllAfter = true;
				container.removeViewAt(i);
				mWidgetsMap.remove(child);
				offset--;
			}
		}

		assert getActivity() != null;
		LayoutInflater inflater = getActivity().getLayoutInflater();

		for (int i = 0; i < notifyCount; i++) {
			if (notifyUsed[i]) continue;

			NotificationUI fragment = new NotificationUI(this);
			View view = fragment.createCollapsedView(inflater, container);
			container.addView(view);

			fragment.setNotification(list.get(i));
			mWidgetsMap.put(view, fragment);
		}

		// ////////////
		// ~~ EXTRAS ~~
		// ////////////
		/*
        int[] extras = new int[]{
                SCENE_MUSIC_CONTROLS,
        };

        extras[0] = -1;

        // Show unlock widget only if there's no any
        // other views.
        boolean empty = true;
        if (notifyCount > 0) {
            empty = false;
        } else for (int i = 1; i < extras.length; i++)
            if (extras[i] >= 0) {
                empty = false;
                break;
            }


        for (int i = fragmentsExtraCount - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            Widget fragment = findWidgetByCollapsedView(child);

            boolean found = false;
            for (int j = 0; j < extras.length; j++) {
                if (extras[j] == fragment.getType()) {
                    extras[j] = -1;
                    found = true;
                    break;
                }
            }
            if (!found) {
                container.removeViewAt(i);
                mWidgetsMap.remove(child);
            }
        }

        for (int i = 0, j = 0; i < extras.length; i++) {
            if (extras[i] >= 0) {
                Widget fragment;
                switch (extras[i]) {
                    case SCENE_UNLOCK:
                        fragment = new UnlockFragment(this);
                        break;
                    case SCENE_MUSIC_CONTROLS:
                        fragment = new MusicFragment(this);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown acfragment type found!");
                }
                View view = fragment.createCollapsedView(inflater, container);
                container.addView(view, j++);
                mWidgetsMap.put(view, fragment);
            }
        }*/

		// /////////////////////
		// ~~ UPDATE HASH MAP ~~
		// /////////////////////

		HashMap<String, SceneCompat> map = (HashMap<String, SceneCompat>) mScenesMap.clone();

		mScenesMap.clear();
		for (Widget fragment : mWidgetsMap.values()) {
			String type = fragment.getClass().getName();
			SceneCompat scene = map.get(type);
			if (scene != null) {
				fragment.createExpandedView(null, null, scene.getView());
			} else {
				ViewGroup sceneView = fragment.createExpandedView(inflater, mSceneContainer, null);
				if (sceneView != null) {
					scene = new SceneCompat(mSceneContainer, sceneView);
					map.put(type, scene);
				}
			}
			if (scene != null) {
				mScenesMap.put(type, scene);
			}
		}

		//ViewUtils.setVisible(mCollapsedUnlockFake, empty);

		if (Build.DEBUG) {
			long delta = SystemClock.elapsedRealtime() - now;
			Log.d(TAG, "Fragment list updated in " + delta + "ms.");
		}

		if (Device.hasKitKatApi()) {

			// Make sure that container will be updated
			// to end delayed transition.
			container.invalidate();
		}
	}

	// //////////////////////////////////////////
	// ///////////// -- CLASSES -- //////////////
	// //////////////////////////////////////////

	private class NotificationListener implements NotificationPresenter.OnNotificationListChangedListener {

		@Override
		public void onNotificationListChanged(NotificationPresenter nm,
				OpenStatusBarNotification notification,
				final int event) {
			if (mTouched) {
				mCollapsedViewsNeedsUpdate = true;
			} else {
				switch (event) {
				case NotificationPresenter.EVENT_BATH:
				case NotificationPresenter.EVENT_POSTED:
				case NotificationPresenter.EVENT_CHANGED:
				case NotificationPresenter.EVENT_REMOVED:
					mTimeout.setTimeoutDelayed(mConfig.getTimeoutNormal(), true);
					updateNotificationList();
					break;
				}
			}
		}
	}

}
