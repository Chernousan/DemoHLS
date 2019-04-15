package com.dmytrochernousan.acromaxdemo.helper.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomImageView extends android.support.v7.widget.AppCompatImageView {

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        super.onTouchEvent(event);
        performClick();
        return false;
    }

    @Override
    public boolean performClick(){
        super.performClick();
        return true;
    }
}
