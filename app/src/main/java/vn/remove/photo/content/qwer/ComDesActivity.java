package vn.remove.photo.content.qwer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import vn.remove.photo.content.qwer.Descripition.*;

import java.util.Random;

public class ComDesActivity extends AppCompatActivity {
    private static int count1=0;
    private static int count2=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com_des);

        ImageView fold1 = findViewById(R.id.com_fold_1);
        ImageView fold2 = findViewById(R.id.com_fold_2);
        ImageView back = findViewById(R.id.com_back);
        TextView txt_com = findViewById(R.id.com_des_com);
        TextView txt_you = findViewById(R.id.com_des_you);
        TextView ran_you = findViewById(R.id.com_you);
        TextView ran_par = findViewById(R.id.com_par);
        Random random = new Random();
        int you1 = random.nextInt(40);
        int par1 = random.nextInt(40);
        int you = you1/10;
        int par = par1/10;
        ran_you.setText(Integer.toString(you));
        ran_par.setText(Integer.toString(par));
        Comdes comdes = new Comdes();

        fold1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //txt_com.setText();
                count1++;
                for(int i=0;i<40;i++){
                    if(i==you1){
                        txt_com.setText(comdes.des[i]);
                    }

                }
                txt_com.setVisibility(View.VISIBLE);
                if(count1%2==0){
                    txt_com.setVisibility(View.GONE);
                    fold1.setImageResource(R.drawable.fold);
                }else {
                    txt_com.setVisibility(View.VISIBLE);
                    fold1.setImageResource(R.drawable.unfold);
                }
            }
        });

        fold2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count2++;
                for(int i=0;i<40;i++){
                    if(i==par1){
                        txt_you.setText(comdes.des[i]);
                    }

                }
                //txt_you.setText();
                txt_you.setVisibility(View.VISIBLE);
                if(count2%2==0){
                    txt_you.setVisibility(View.GONE);
                    fold2.setImageResource(R.drawable.fold);
                }else {
                    txt_you.setVisibility(View.VISIBLE);
                    fold2.setImageResource(R.drawable.unfold);
                }


            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}