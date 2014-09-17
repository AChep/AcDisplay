package dreamers.graphics;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;

public class RippleDrawable extends Drawable implements View.OnTouchListener{

    final static Property<RippleDrawable, Float> CREATE_TOUCH_RIPPLE =
            new FloatProperty<RippleDrawable>("createTouchRipple") {
        @Override
        public void setValue(RippleDrawable object, float value) {
            object.createTouchRipple(value);
        }

        @Override
        public Float get(RippleDrawable object) {
            return object.getAnimationState();
        }
    };

    final static Property<RippleDrawable, Float> DESTROY_TOUCH_RIPPLE =
            new FloatProperty<RippleDrawable>("destroyTouchRipple") {
        @Override
        public void setValue(RippleDrawable object, float value) {
            object.destroyTouchRipple(value);
        }

        @Override
        public Float get(RippleDrawable object) {
            return object.getAnimationState();
        }
    };

    final static int DEFAULT_ANIM_DURATION = 250;
    final static float END_RIPPLE_TOUCH_RADIUS = 150f;
    final static float END_SCALE = 1.3f;

    final static int RIPPLE_TOUCH_MIN_ALPHA = 40;
    final static int RIPPLE_TOUCH_MAX_ALPHA = 120;
    final static int RIPPLE_BACKGROUND_ALPHA = 100;

    Paint mRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint mRippleBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    Circle mTouchRipple;
    Circle mBackgroundRipple;

    ObjectAnimator mCurrentAnimator;

    Drawable mOriginalBackground;

    public RippleDrawable() {
        initRippleElements();
    }

    public static void createRipple(View v, int primaryColor){
        RippleDrawable rippleDrawable = new RippleDrawable();
        rippleDrawable.setDrawable(v.getBackground());
        rippleDrawable.setColor(primaryColor);
        rippleDrawable.setBounds(v.getPaddingLeft(), v.getPaddingTop(),
                v.getPaddingRight(), v.getPaddingBottom());

        v.setOnTouchListener(rippleDrawable);
        if(Build.VERSION.SDK_INT >= 16) {
            v.setBackground(rippleDrawable);
        }else{
            v.setBackgroundDrawable(rippleDrawable);
        }
    }

    public static void createRipple(int x, int y, View v, int primaryColor){
        if(!(v.getBackground() instanceof RippleDrawable)) {
            createRipple(v, primaryColor);
        }
        RippleDrawable drawable = (RippleDrawable) v.getBackground();
        drawable.setColor(primaryColor);
        drawable.onFingerDown(v, x, y);
    }

    /**
     * Set colors of ripples
     *
     * @param primaryColor color of ripples
     */
    public void setColor(int primaryColor){
        mRippleBackgroundPaint.setColor(primaryColor);
        mRippleBackgroundPaint.setAlpha(RIPPLE_BACKGROUND_ALPHA);
        mRipplePaint.setColor(primaryColor);

        invalidateSelf();
    }

    /**
     * set first layer you background drawable
     *
     * @param drawable original background
     */
    public void setDrawable(Drawable drawable){
        mOriginalBackground = drawable;

        invalidateSelf();
    }

    void initRippleElements(){
        mTouchRipple = new Circle();
        mBackgroundRipple = new Circle();

        mRipplePaint.setStyle(Paint.Style.FILL);
        mRippleBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        if(mOriginalBackground != null){
            mOriginalBackground.setBounds(getBounds());
            mOriginalBackground.draw(canvas);
        }

        mBackgroundRipple.draw(canvas, mRippleBackgroundPaint);
        mTouchRipple.draw(canvas, mRipplePaint);
    }

    @Override public void setAlpha(int alpha) {}

    @Override public void setColorFilter(ColorFilter cf) {}

    @Override public int getOpacity() {
        return 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                onFingerDown(v, event.getX(), event.getY());
                return v.onTouchEvent(event);
            case MotionEvent.ACTION_MOVE:
                onFingerMove(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                onFingerUp();
                break;
        }
        return false;
    }

    int mViewSize = 0;

    void onFingerDown(View v, float x, float y){
        mTouchRipple.cx = mBackgroundRipple.cx = x;
        mTouchRipple.cy = mBackgroundRipple.cy = y;
        mTouchRipple.radius = mBackgroundRipple.radius = 0f;
        mViewSize = Math.max(v.getWidth(), v.getHeight());

        if(mCurrentAnimator == null){
            mRippleBackgroundPaint.setAlpha(RIPPLE_BACKGROUND_ALPHA);

            mCurrentAnimator = ObjectAnimator.ofFloat(this, CREATE_TOUCH_RIPPLE, 0f, 1f);
            mCurrentAnimator.setDuration(DEFAULT_ANIM_DURATION);
        }

        if(!mCurrentAnimator.isRunning()){
            mCurrentAnimator.start();
        }
    }


    void createTouchRipple(float value){
        mAnimationValue = value;

        mTouchRipple.radius = 40f + (mAnimationValue * (END_RIPPLE_TOUCH_RADIUS - 40f));
        mBackgroundRipple.radius = mAnimationValue * (mViewSize * END_SCALE);

        int min = RIPPLE_TOUCH_MIN_ALPHA;
        int max = RIPPLE_TOUCH_MAX_ALPHA;
        int alpha = min + (int) (mAnimationValue * (max - min));
        mRipplePaint.setAlpha((max + min) - alpha);

        invalidateSelf();
    }

    float mAnimationValue;

    void destroyTouchRipple(float value){
        mAnimationValue = value;

        mTouchRipple.radius = END_RIPPLE_TOUCH_RADIUS + (mAnimationValue * (mViewSize * END_SCALE));

        mRipplePaint.setAlpha((int) (RIPPLE_TOUCH_MIN_ALPHA - (mAnimationValue * RIPPLE_TOUCH_MIN_ALPHA)));
        mRippleBackgroundPaint.setAlpha
                ((int) (RIPPLE_BACKGROUND_ALPHA - (mAnimationValue * RIPPLE_BACKGROUND_ALPHA)));

        invalidateSelf();
    }

    float getAnimationState(){
        return mAnimationValue;
    }

    void onFingerUp(){
        if(mCurrentAnimator != null) {
            mCurrentAnimator.end();
            mCurrentAnimator = null;
            createTouchRipple(END_RIPPLE_TOUCH_RADIUS);
        }

        mCurrentAnimator = ObjectAnimator.ofFloat(this, DESTROY_TOUCH_RIPPLE, 0f, 1f);
        mCurrentAnimator.setDuration(DEFAULT_ANIM_DURATION);
        mCurrentAnimator.addListener(new SimpleAnimationListener(){
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCurrentAnimator = null;
            }
        });
        mCurrentAnimator.start();
    }

    void onFingerMove(float x, float y){
        mTouchRipple.cx = x;
        mTouchRipple.cy = y;

        invalidateSelf();
    }

    @Override
    public boolean setState(int[] stateSet) {
        if(mOriginalBackground != null){
            return mOriginalBackground.setState(stateSet);
        }
        return super.setState(stateSet);
    }

    @Override
    public int[] getState() {
        if(mOriginalBackground != null){
            return mOriginalBackground.getState();
        }
        return super.getState();
    }

    final static class Circle{
        float cx;
        float cy;
        float radius;

        public void draw(Canvas canvas, Paint paint){
            canvas.drawCircle(cx, cy, radius, paint);
        }
    }

}
