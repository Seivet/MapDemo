package com.muli.map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.muli.map.overlayUtil.DrivingRouteOverlay;
import com.muli.map.overlayUtil.TransitRouteOverlay;
import com.muli.map.overlayUtil.WalkingRouteOverlay;

public class MainActivity extends Activity implements View.OnClickListener,OnGetRoutePlanResultListener {

    private TextureMapView mMapView;

    // 定位
    public LocationClient mLocationClient = null;
    public BDLocationListener mLocationListener = new MyLocationListener();

    private BaiduMap mBaiduMap;
    private MapStatusUpdate mMsu;
    private BitmapDescriptor mCurrentMarker;
    private boolean isFirst = true;

    private Context context;
    private LatLng mLastLocation;
    private Button mLastLocationButton;

    // 路线
    private EditText mStPlace;
    private EditText mStCity;
    private EditText mEnPlace;
    private EditText mEnCity;
    private Button mDriving;
    private Button mTransit;
    private Button mWalking;
    private PlanNode stNode;
    private PlanNode enNode;

    RoutePlanSearch mRoutePlanSearch = RoutePlanSearch.newInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        this.context = this;

        // 初始化视图
        initView();
        // 初始化定位
        initLocation();
        // 获取定位权限
        verifyStoragePermissions(this);
    }

    private void initView(){
        mMapView = (TextureMapView) findViewById(R.id.bmapView);
        // 刚打开时的默认比例 500米
        if (mMapView != null) {
            mBaiduMap = mMapView.getMap();
        }
        mMsu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(mMsu);

        mLastLocationButton = (Button) findViewById(R.id.back_button);

        mStCity = (EditText) findViewById(R.id.st_city);
        mStPlace = (EditText) findViewById(R.id.st_place);
        mEnCity = (EditText) findViewById(R.id.en_city);
        mEnPlace = (EditText) findViewById(R.id.en_place);
        // 默认搜索
        mStCity.setText("北京");
        mStPlace.setText("西单");
        mEnCity.setText("北京");
        mEnPlace.setText("颐和园");

        mDriving = (Button) findViewById(R.id.driving);
        mTransit = (Button) findViewById(R.id.transit);
        mWalking = (Button) findViewById(R.id.walking);

        mDriving.setOnClickListener(this);
        mTransit.setOnClickListener(this);
        mWalking.setOnClickListener(this);

        mLastLocationButton.setOnClickListener(this);
        mRoutePlanSearch.setOnGetRoutePlanResultListener(this);
    }

    @Override
    public void onClick(View v) {
        mBaiduMap.clear();
        stNode = PlanNode.withCityNameAndPlaceName(mStCity.getText().toString(),mStPlace.getText().toString());
        enNode = PlanNode.withCityNameAndPlaceName(mEnCity.getText().toString(),mEnPlace.getText().toString());

        switch (v.getId()){
            case R.id.back_button:
                if( mLastLocation != null ){
                    MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(mLastLocation);
                    mBaiduMap.animateMapStatus(msu);
                }
                break;
            case R.id.driving:
                mRoutePlanSearch.drivingSearch(new DrivingRoutePlanOption().from(stNode).to(enNode));
                break;
            case R.id.transit:
                // city?
                mRoutePlanSearch.transitSearch(new TransitRoutePlanOption().from(stNode).city(mEnCity.getText().toString()).to(enNode));
                break;
            case R.id.walking:
                mRoutePlanSearch.walkingSearch(new WalkingRoutePlanOption().from(stNode).to(enNode));
                break;
        }
    }


    /*-----------------------查找路线相关-----------------------*/
    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            // result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            DrivingRouteOverlay overlay = new DrivingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            WalkingRouteOverlay overlay = new WalkingRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }

    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult result) {

        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {
            TransitRouteOverlay overlay = new TransitRouteOverlay(mBaiduMap);
            mBaiduMap.setOnMarkerClickListener(overlay);
            overlay.setData(result.getRouteLines().get(0));
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

    }



    /*--------------------------定位相关--------------------------*/

    private void initLocation() {
        mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
        mLocationClient.registerLocationListener(mLocationListener);    //注册监听函数

        LocationClientOption option = new LocationClientOption();

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setScanSpan(1000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的

        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要

        mLocationClient.setLocOption(option);
        mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 开启定位
        mBaiduMap.setMyLocationEnabled(true);
        if( !mLocationClient.isStarted() ) {
            mLocationClient.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 当不需要定位图层时关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            // 开启定位图层
            mBaiduMap.setMyLocationEnabled(true);
            // 构造定位数据
            MyLocationData data = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
//                    .direction(100)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();

            // 设置定位数据
            mBaiduMap.setMyLocationData(data);

            // 设置定位图层的配置（定位模式，是否允许方向信息，用户自定义定位图标）
            MyLocationConfiguration config = new MyLocationConfiguration(
                    MyLocationConfiguration.LocationMode.NORMAL, true, mCurrentMarker);
            mBaiduMap.setMyLocationConfigeration(config);

            LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
            // 记录当前位置
            mLastLocation = latLng;


            // 第一次进入时定位当前位置
            if(isFirst){
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(msu);
                isFirst = false;

                Toast.makeText(context, location.getAddrStr(), Toast.LENGTH_SHORT).show();
            }
        }
    }



    /*-----------Android 6.0版本需要  第一次打开向用户获取定位权限----------*/

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS};

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

}
