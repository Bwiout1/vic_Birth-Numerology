package vn.remove.photo.content.qwer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import vn.remove.photo.content.qwer.Adapter.*;
import vn.remove.photo.content.qwer.Descripition.*;

public class Get_YearPredit extends AppCompatActivity {

    private View view1, view2, view3;//布局
    private ArrayList<View> views;
    private ArrayList<String> tab_name;//tab栏名字
    private TabAdapater tabAdapater;//tab适配器
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private static int count_1 = 0;
    private static int count_2 = 0;
    private static int count_3 = 0;
    public  static  String weekday;
    public static  String  month;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_year_predit);
        //返回图标
        ImageView num_back = findViewById(R.id.num_year_back);
        num_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        tabLayout = findViewById(R.id.num_year_tab);
        viewPager = findViewById(R.id.num_year_viewpager);
        ProgressBar pro1 = findViewById(R.id.pro4);
        ProgressBar pro2 = findViewById(R.id.pro5);
        ProgressBar pro3 = findViewById(R.id.pro6);
        //获取viewpager中的子布局
        LayoutInflater inflater = getLayoutInflater();
        view1 = inflater.inflate(R.layout.com_num_1, null);
        view2 = inflater.inflate(R.layout.com_num_2, null);
        view3 = inflater.inflate(R.layout.com_num_3, null);
        //view1中显示的数据
        TextView txt_des_1_detail = view1.findViewById(R.id.num_des_detail_1);
        ImageView fold_1 = view1.findViewById(R.id.fold_1);
        fold_1.setImageResource(R.drawable.fold);
        ImageView fold_2 = view1.findViewById(R.id.fold_2);
        fold_2.setImageResource(R.drawable.fold);
        TextView txt_num_day_detail_1 = view1.findViewById(R.id.num_day_detail_1);
        TextView per_day = view1.findViewById(R.id.num_txt_day_1);
        //显示出生日期
        TextView num_time = findViewById(R.id.num_year_time);
        SharedPreferences sharedPreferences = getSharedPreferences("birth",MODE_PRIVATE);
        SharedPreferences.Editor editor =sharedPreferences.edit();
        String Day = sharedPreferences.getString("day","0");
        String Month = sharedPreferences.getString("month","00");
        String Year = sharedPreferences.getString("year","0000");
        num_time.setText(Day+"/"+Month+"/"+Year);

        TextView num_day = findViewById(R.id.num_year_day);
        TextView num_month = findViewById(R.id.num_year_mon);
        TextView num_year = findViewById(R.id.num_year_year);
        Random random = new Random();
        int ran_day = random.nextInt(10);
        per_day.setText("Personal Day"+" "+ran_day);
        int ran_year = random.nextInt(10);
        int ran_mon = random.nextInt(10);
        num_day.setText(Integer.toString(ran_day));
        num_month.setText(Integer.toString(ran_mon));
        num_year.setText(Integer.toString(ran_year));
        pro1.setProgress(ran_day);
        pro2.setProgress(ran_mon);
        pro3.setProgress(ran_year);
        DayDescrip dayDescrip = new DayDescrip();
        //获取数据
        //折叠与否
        fold_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_1++;
                for(int i = 0;i<=9;i++){
                    if(i==ran_day){
                        txt_des_1_detail.setText(dayDescrip.des_day[i]);
                    }
                }
                if(count_1%2==0){
                    txt_des_1_detail.setVisibility(View.GONE);
                    fold_1.setImageResource(R.drawable.fold);
                }else {
                    txt_des_1_detail.setVisibility(View.VISIBLE);
                    fold_1.setImageResource(R.drawable.unfold);
                }

            }
        });
        fold_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_1=count_1+1;
                for(int i = 0;i<=9;i++){
                    if(i==ran_day){
                        txt_num_day_detail_1.setText(dayDescrip.des_day[i]);
                    }
                }
                if(count_1%2==0){
                    txt_num_day_detail_1.setVisibility(View.GONE);
                    fold_2.setImageResource(R.drawable.fold);
                }else {
                    txt_num_day_detail_1.setVisibility(View.VISIBLE);
                    fold_2.setImageResource(R.drawable.unfold);
                }
            }
        });
        //view2中显示的数据
        TextView num_des_detail_2 = view2.findViewById(R.id.num_des_detail_2);
        ImageView fold_3 = view2.findViewById(R.id.fold_3);
        fold_3.setImageResource(R.drawable.fold);
        TextView num_day_detail_2 = view2.findViewById(R.id.num_day_detail_2);
        ImageView fold_4 = view2.findViewById(R.id.fold_4);
        fold_4.setImageResource(R.drawable.fold);
        MonthDescip monthDescip = new MonthDescip();
        TextView num_txt_day_2 = view2.findViewById(R.id.num_txt_day_2);
        num_txt_day_2.setText("Personal Month"+" "+Integer.toString(ran_mon));
        fold_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_2++;
                for(int i = 0;i<=9;i++){
                    if(i==ran_mon){
                        num_des_detail_2.setText(monthDescip.mon_des[i]);
                    }
                }
                if(count_2%2==0){
                    num_des_detail_2.setVisibility(View.GONE);
                    fold_3.setImageResource(R.drawable.fold);
                }else {
                    num_des_detail_2.setVisibility(View.VISIBLE);
                    fold_3.setImageResource(R.drawable.unfold);
                }
            }
        });
        fold_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_2++;
                for(int i = 0;i<=9;i++){
                    if(i==ran_mon){
                        num_day_detail_2.setText(monthDescip.mon_des[i]);
                    }
                }
                if(count_2%2==0){
                    num_day_detail_2.setVisibility(View.GONE);
                    fold_4.setImageResource(R.drawable.fold);
                }else {
                    num_day_detail_2.setVisibility(View.VISIBLE);
                    fold_4.setImageResource(R.drawable.unfold);
                }
            }
        });
        //view3中的数据
        TextView num_des_detail_3 = view3.findViewById(R.id.num_des_detail_3);
        TextView num_day_detail_3 = view3.findViewById(R.id.num_day_detail_3);
        ImageView fold_5 = view3.findViewById(R.id.fold_5);
        fold_5.setImageResource(R.drawable.fold);
        ImageView fold_6 = view3.findViewById(R.id.fold_6);
        fold_6.setImageResource(R.drawable.fold);
        TextView num_txt_day_3 = view3.findViewById(R.id.num_txt_day_3);
        num_txt_day_3.setText("Personal Year"+" "+Integer.toString(ran_year));
        YearDescrip yearDescrip = new YearDescrip();
        fold_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_3++;
                for(int i = 0;i<=9;i++){
                    if(i==ran_year){
                        num_des_detail_3.setText(yearDescrip.year_des[i]);
                    }
                }
                if(count_3%2==0){
                    num_des_detail_3.setVisibility(View.GONE);
                    fold_5.setImageResource(R.drawable.fold);
                }else {
                    num_des_detail_3.setVisibility(View.VISIBLE);
                    fold_5.setImageResource(R.drawable.unfold);
                }
            }
        });
        fold_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count_3++;
                for(int i = 0;i<=9;i++){
                    if(i==ran_year){
                        num_day_detail_3.setText(yearDescrip.year_des[i]);
                    }
                }
                if(count_3%2==0){
                    num_day_detail_3.setVisibility(View.GONE);
                    fold_6.setImageResource(R.drawable.fold);
                }else {
                    num_day_detail_3.setVisibility(View.VISIBLE);
                    fold_6.setImageResource(R.drawable.unfold);
                }
            }
        });
        //添加布局
        views = new ArrayList<View>();
        views.add(view1);
        views.add(view2);
        views.add(view3);
        //获取当前时间
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String mYear = String.valueOf(c.get(Calendar.YEAR)); // 获取当前年份
        String mMonth = String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份
        month = getmonth(mMonth);
        tabLayout = findViewById(R.id.num_year_tab);
        tab_name = new ArrayList<String>();
        tab_name.add(mYear);
        tab_name.add(Integer.toString(Integer.parseInt(mYear)+1));
        tab_name.add(Integer.toString(Integer.parseInt(mYear)+2));
        //显示tab栏和viewpager
        for (int i = 0; i < tab_name.size(); i++) {
            tabLayout.addTab(tabLayout.newTab().setText(tab_name.get(i)));
        }
        tabAdapater = new TabAdapater(views, tab_name);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setAdapter(tabAdapater);
    }
    public String getmonth(String num){
        switch (num)
        {
            case "1":
                month="Jan";
                break;
            case "2":
                month="Feb";
                break;
            case "3":
                month="Mar";
                break;
            case "4":
                month="Apr";
                break;
            case "5":
                month="May";
                break;
            case "6":
                month="Jun";
                break;
            case "7":
                month="Jul";
                break;
            case "8":
                month="Aug";
                break;
            case "9":
                month="Sep";
                break;
            case "10":
                month="Oct";
                break;
            case "11":
                month="Nov";
                break;
            case "12":
                month="Dec";
                break;
        }
        return month;
    }
}