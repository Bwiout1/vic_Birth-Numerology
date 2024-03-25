package vn.remove.photo.content.qwer.Adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

public class TabAdapater extends PagerAdapter {//导航栏适配器

    private ArrayList<View> mViewList;

    private ArrayList<String> mtitleList;


    public TabAdapater(ArrayList<View> viewList, ArrayList<String> mtitleList) {
        mViewList = viewList;
        this.mtitleList = mtitleList;
    }



    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        container.addView(mViewList.get(position));
        return  mViewList.get(position);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView(mViewList.get(position));
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
       return mtitleList.get(position);
   }
}

