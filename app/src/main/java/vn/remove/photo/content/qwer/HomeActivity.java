package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import vn.remove.photo.content.qwer.R;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView txt_start = findViewById(R.id.txt_start);
        txt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, Write_NameActivity.class);
                startActivity(intent);
            }
        });

    }
    public void onBackPressed() {
        Intent intent = new Intent(HomeActivity.this, FinalActivity.class);
        startActivity(intent);
    }
}