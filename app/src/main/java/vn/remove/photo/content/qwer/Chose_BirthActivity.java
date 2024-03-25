package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;

public class Chose_BirthActivity extends AppCompatActivity {
    private final String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private  static int year;
    private  static  int month;
    private  static  int day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_birth);

        NumberPicker num_mon = findViewById(R.id.bir_pic_month);
        NumberPicker num_day = findViewById(R.id.bir_pic_day);
        NumberPicker num_yea = findViewById(R.id.bir_pic_year);
        TextView txt_next2 = findViewById(R.id.start_next_2);
        ImageView back2 = findViewById(R.id.start_back_2);

        back2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent(Chose_BirthActivity.this, Write_NameActivity.class);
                startActivity(intent);
            }
        });
        setpicker(num_mon,num_yea,num_day);


        SharedPreferences sharedPreferences = getSharedPreferences("birth",MODE_PRIVATE);
        SharedPreferences.Editor editor =sharedPreferences.edit();

        txt_next2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                year =num_yea.getValue();
                month = num_mon.getValue()+1;
                day = num_day.getValue();
                editor.putString("year", String.valueOf(year));
                editor.putString("month",String.valueOf(month));
                editor.putString("day",String.valueOf(day));
                editor.apply();
                Intent intent = new Intent(Chose_BirthActivity.this,ChoseActivity.class);
                startActivity(intent);
            }
        });
    }

    public void setpicker(NumberPicker num_mon, NumberPicker num_yea, NumberPicker num_day){
        Calendar calendar = Calendar.getInstance();


        num_yea.setMinValue(1949);
        num_yea.setMaxValue(calendar.get(Calendar.YEAR));
        num_yea.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_yea, Color.TRANSPARENT);
        Change_Number.setTextcolor(num_yea,Color.WHITE);

        num_day.setMaxValue(31);
        num_day.setMinValue(1);
        num_day.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_day, Color.TRANSPARENT);
        Change_Number.setTextcolor(num_day,Color.WHITE);

        num_mon.setMaxValue(monthNames.length-1);
        num_mon.setMinValue(0);
        num_mon.setDisplayedValues(monthNames);
        num_mon.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_mon, Color.TRANSPARENT);
        Change_Number.setTextcolor(num_mon,Color.WHITE);

    }
}