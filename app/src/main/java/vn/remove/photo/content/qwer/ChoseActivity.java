package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ChoseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chose_kinds);
        SharedPreferences sharedPreferences = getSharedPreferences("birth",MODE_PRIVATE);
        String name = sharedPreferences.getString("name",null);
        String mDay = sharedPreferences.getString("day","0");
        String mMonth = sharedPreferences.getString("month","00");
        String mYear = sharedPreferences.getString("year","0000");

        TextView txt_name = findViewById(R.id.txt_show_name);
        TextView txt_time = findViewById(R.id.txt_show_time);
        txt_name.setText(name);
        txt_time.setText(mDay+"/"+mMonth+"/"+mYear);

        ImageView num = findViewById(R.id.chose_num);
        ImageView com = findViewById(R.id.chose_com);
        num.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoseActivity.this, Get_desActivity.class);
                startActivity(intent);
            }
        });
        com.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChoseActivity.this, Set_Compatibility.class);
                startActivity(intent);
            }
        });
    }
}
