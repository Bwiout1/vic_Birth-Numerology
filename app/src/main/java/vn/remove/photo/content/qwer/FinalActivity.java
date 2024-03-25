package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class FinalActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);

        TextView txt_yes = findViewById(R.id.txt_yes);
        TextView txt_no = findViewById(R.id.txt_no);
        txt_yes.setOnClickListener(this::onClick);
        txt_no.setOnClickListener(this::onClick);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.txt_yes){
            System.exit(0);
        } else if (v.getId()==R.id.txt_no) {
            Intent intent = new Intent(FinalActivity.this,HomeActivity.class);
            startActivity(intent);
        }
    }
}