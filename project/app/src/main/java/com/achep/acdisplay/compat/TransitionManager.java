package com.achep.acdisplay.compat;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.transition.Transition;
import android.view.ViewGroup;

import com.achep.acdisplay.Device;

/**
 * This is a class for easy calling {@link android.transition.Transition}'s methods on
 * all versions of Android.
 */
public class TransitionManager {

    @NonNull
    private static final TransitionManagerBase INSTANCE = Device.hasKitKatApi()
            ? new TransitionManagerKitKat()
            : new TransitionManagerBase();

    /**
     * Convenience method to animate, using the default transition,
     * to a new scene defined by all changes within the given scene root between
     * calling this method and the next rendering frame.
     * Equivalent to calling {@link #beginDelayedTransition(ViewGroup, android.transition.Transition)}
     * with a value of <code>null</code> for the <code>transition</code> parameter.
     *
     * @param sceneRoot The root of the View hierarchy to run the transition on.
     */
    public static void beginDelayedTransition(final ViewGroup vg) {
        beginDelayedTransition(vg, null);
    }

    /**
     * Convenience method to animate to a new scene defined by all changes within
     * the given scene root between calling this method and the next rendering frame.
     * Calling this method causes TransitionManager to capture current values in the
     * scene root and then post a request to run a transition on the next frame.
     * At that time, the new values in the scene root will be captured and changes
     * will be animated. There is no need to create a Scene; it is implied by
     * changes which take place between calling this method and the next frame when
     * the transition begins.
     *
     * <p>Calling this method several times before the next frame (for example, if
     * unrelated code also wants to make dynamic changes and run a transition on
     * the same scene root), only the first call will trigger capturing values
     * and exiting the current scene. Subsequent calls to the method with the
     * same scene root during the same frame will be ignored.</p>
     *
     * <p>Passing in <code>null</code> for the transition parameter will
     * cause the TransitionManager to use its default transition.</p>
     *
     * @param sceneRoot The root of the View hierarchy to run the transition on.
     * @param transition The transition to use for this change. A
     * value of null causes the TransitionManager to use the default transition.
     */
    public static void beginDelayedTransition(
            @NonNull final ViewGroup sceneRoot,
            @Nullable Transition transition) {
        INSTANCE.beginDelayedTransition(sceneRoot, transition);
    }

    /**
     * Empty class.
     */
    public static class TransitionManagerBase {

        public void beginDelayedTransition(
                @NonNull final ViewGroup sceneRoot,
                @Nullable Transition transition) {
        }

    }

    /**
     * Calls {@link android.transition.Transition}'s methods.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static class TransitionManagerKitKat extends TransitionManagerBase {

        /**
         * {@inheritDoc}
         */
        @Override
        public void beginDelayedTransition(
                @NonNull final ViewGroup sceneRoot,
                @Nullable Transition transition) {
            android.transition.TransitionManager.beginDelayedTransition(sceneRoot);
        }

    }

}
