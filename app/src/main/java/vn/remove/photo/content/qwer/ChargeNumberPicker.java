package vn.remove.photo.content.qwer;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Field;
import java.util.Objects;

import vn.remove.photo.content.qwer.R;

public class ChargeNumberPicker extends NumberPicker {

    public ChargeNumberPicker(Context context) {
        super(context);
    }

    public ChargeNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChargeNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        updateView(child);
    }

    @Override
    public void addView(View child, int index,
                        android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    @Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, params);
        updateView(child);
    }

    public void updateView(View view) {
        if (view instanceof EditText) {
            //这里修改字体的属性
            ((EditText)view).setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
            ((EditText) view).setTextSize(16);
        }
    }

    //动态调整
    public void updateColor(@ColorRes int color){
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof EditText) {
                try {
                    Field selectorWheelPaintField = Objects.requireNonNull(getClass().getSuperclass())
                            .getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(this)).setColor(getResources().getColor(color));
                    ((EditText) child).setTextColor(getResources().getColor(color));
                    this.invalidate();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
