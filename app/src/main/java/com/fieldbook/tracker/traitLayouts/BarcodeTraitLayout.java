package com.fieldbook.tracker.traitLayouts;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

public class BarcodeTraitLayout extends TraitLayout {

    public BarcodeTraitLayout(Context context) {
        super(context);
    }

    public BarcodeTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarcodeTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init(){

    }

    @Override
    public void loadLayout(){
        getEtCurVal().setVisibility(EditText.VISIBLE);
    }
    @Override
    public void deleteTraitListener() {

    }
}