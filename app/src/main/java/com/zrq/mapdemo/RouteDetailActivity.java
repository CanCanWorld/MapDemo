package com.zrq.mapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class RouteDetailActivity extends AppCompatActivity {

    private Context context;
    private RecyclerView mRvDetail;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_route_detail);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mRvDetail = findViewById(R.id.rv_detail);
    }

    private void initData() {
        intent = getIntent();
    }

    private void initEvent() {

    }
}