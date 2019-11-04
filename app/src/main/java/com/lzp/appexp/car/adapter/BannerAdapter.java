package com.lzp.appexp.car.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.lzp.appexp.R;

/**
 * @describe
 * @author: lixiaopeng
 * @Date: 2019-11-04
 */
public class BannerAdapter extends RecyclerView.Adapter {

    private int size = 10;

    private int img[] = {R.mipmap.ngt_1,R.mipmap.ngt_6,R.mipmap.ngt_10,R.mipmap.ngt_15};


    public BannerAdapter() {
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_car, viewGroup, false);
        return new MyViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

        MyViewHolder holder = (MyViewHolder) viewHolder;

        holder.imageView.setImageResource(img[i]);
    }

    @Override
    public int getItemCount() {
        return img.length;
    }

    class MyViewHolder extends RecyclerView.ViewHolder{
        private ImageView imageView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.car);
        }
    }
}
