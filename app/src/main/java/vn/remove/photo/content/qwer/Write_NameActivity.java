package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import vn.remove.photo.content.qwer.R;

public class Write_NameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        TextView txt_next = findViewById(R.id.name_next);
        ImageView name_back = findViewById(R.id.name_back);
        EditText ed_name = findViewById(R.id.ed_name);
        SharedPreferences sharedPreferences = getSharedPreferences("birth",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        txt_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name;
                name = ed_name.getText().toString();
                editor.putString("name",name);
                editor.commit();
                Intent intent = new Intent(Write_NameActivity.this, Chose_BirthActivity.class);
                startActivity(intent);
            }
        });
        name_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Write_NameActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
    }
}