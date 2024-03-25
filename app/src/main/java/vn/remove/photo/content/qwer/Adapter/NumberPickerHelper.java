package vn.remove.photo.content.qwer.Adapter;

import android.graphics.drawable.ColorDrawable;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

public class NumberPickerHelper {
    public static void setDividerColor(NumberPicker picker, int color) {
        Field[] fields = NumberPicker.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    field.set(picker, colorDrawable);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
    public static void setTextcolor(NumberPicker picker,int color) {
        Field[] fields = NumberPicker.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("mEditText")) {
                field.setAccessible(true);
                try {
                    ColorDrawable colorDrawable = new ColorDrawable(color);
                    field.set(picker, colorDrawable);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

}
