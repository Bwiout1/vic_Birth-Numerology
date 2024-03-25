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
import java.util.Random;

public class Set_Compatibility extends AppCompatActivity {
    private final String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private  static int year;
    private  static  int month;
    private  static  int day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_compatibility);

        TextView txt_check = findViewById(R.id.txt_check);
        ImageView back = findViewById(R.id.pre_com_back);
        //得到匹配值
        TextView txt_you = findViewById(R.id.pre_com_you);
        TextView txt_par = findViewById(R.id.pre_com_par);
        Random random = new Random();
        int you = random.nextInt(10);
        int par = random.nextInt(10);
        txt_you.setText(Integer.toString(you));
        txt_par.setText(Integer.toString(par));

        Calendar calendar = Calendar.getInstance();
        NumberPicker num_day = findViewById(R.id.com_pre_day);
        NumberPicker num_mon = findViewById(R.id.com_pre_month);
        NumberPicker num_yea = findViewById(R.id.com_pre_year);
        setpicker(num_mon,num_yea,num_day);

        SharedPreferences sharedPreferences = getSharedPreferences("precom",MODE_PRIVATE);
        SharedPreferences.Editor editor =sharedPreferences.edit();

        txt_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                year = num_yea.getValue();
                month = num_mon.getValue()+1;
                day = num_day.getValue();
                editor.putString("year", String.valueOf(year));
                editor.putString("month",String.valueOf(month));
                editor.putString("day",String.valueOf(day));
                editor.apply();
                Intent intent = new Intent(Set_Compatibility.this, ComDesActivity.class);
                startActivity(intent);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


    public void setpicker(NumberPicker num_mon, NumberPicker num_yea, NumberPicker num_day){
        Calendar calendar = Calendar.getInstance();

        num_yea.setMinValue(1949);
        num_yea.setMaxValue(calendar.get(Calendar.YEAR));
        num_yea.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_yea, Color.TRANSPARENT);


        num_day.setMaxValue(31);
        num_day.setMinValue(1);
        num_day.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_day, Color.TRANSPARENT);

        num_mon.setMaxValue(monthNames.length-1);
        num_mon.setMinValue(0);
        num_mon.setDisplayedValues(monthNames);
        num_mon.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Change_Number.setDividerColor(num_mon, Color.TRANSPARENT);

    }
}