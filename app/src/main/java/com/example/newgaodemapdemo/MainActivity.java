package com.example.newgaodemapdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.Manifest;

import androidx.annotation.RequiresApi;
import androidx.databinding.DataBindingUtil;

import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapException;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.RotateAnimation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.newgaodemapdemo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, LocationSource, PoiSearch.OnPoiSearchListener, AMap.OnMapClickListener, AMap.OnMapLongClickListener, GeocodeSearch.OnGeocodeSearchListener, AMap.OnMarkerDragListener, AMap.OnMarkerClickListener {
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;
    //POI查询对象
    private PoiSearch.Query query;
    //POI搜索对象
    private PoiSearch poiSearch;
    //城市
    private String city;
    //城市码
    private String cityCode = null;
    //地理编码搜索
    private GeocodeSearch geocodeSearch;
    //解析成功标识码
    private static final int PARSE_SUCCESS_CODE = 1000;
    private ActivityMainBinding binding;
    // 声明地图控制器
    private AMap aMap = null;
    // 声明地图定位监听
    private LocationSource.OnLocationChangedListener mListener = null;
    //标点列表
    private final List<Marker> markerList = new ArrayList<>();

    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<String> requestPermission;
    private TextView text_fouse;
    private Switch btn_test;
    // 声明 listener 变量
    private CompoundButton.OnCheckedChangeListener listener;

    private boolean isRebound = false;
    private boolean switchState;
    private  boolean testmsg = false;
    private void showMsg(CharSequence llw) {
        Toast.makeText(this, llw, Toast.LENGTH_SHORT).show();
    }

    private void uilts(){
        text_fouse = findViewById(R.id.text_fouse);

    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            //初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());
            //设置定位回调监听
            mLocationClient.setLocationListener(this);
            //初始化AMapLocationClientOption对象
            mLocationOption = new AMapLocationClientOption();
            //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //获取最近3s内精度最高的一次定位结果
            mLocationOption.setOnceLocationLatest(true);
            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            //设置定位超时时间，单位是毫秒
            mLocationOption.setHttpTimeOut(6000);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 开始定位
     */
    private void startLocation() {
        if (mLocationClient != null) mLocationClient.startLocation();
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        if (mLocationClient != null) mLocationClient.stopLocation();
    }

    private void updateSwitchState(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (btn_test.isChecked() != state) {
                    btn_test.setChecked(state);
                }
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        uilts();
        requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            // 权限申请结果
            Log.d(TAG, "权限申请结果: " + result);
            showMsg(result ? "已获取到权限":"权限申请失败");
        });
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        CompoundButton btn_test = findViewById(R.id.btn_test);
        switchState = btn_test.isChecked();
        //测试回弹逻辑按钮
        listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!testmsg) {
                    if (!isRebound) { // 如果不是回弹操作
                        Toast.makeText(MainActivity.this, "条件不满足，将在 3 秒后回弹", Toast.LENGTH_SHORT).show();
                        // 延迟 3 秒后将状态设回原来的状态
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 移除监听器以避免触发回调
                                btn_test.setOnCheckedChangeListener(null);
                                // 在这里恢复开关状态
                                btn_test.setChecked(switchState);
                                // 重新添加监听器
                                btn_test.setOnCheckedChangeListener(listener);
                                // 恢复标志位
                                isRebound = false;
                            }
                        }, 3000); // 延迟 3 秒
                    }
                } else {
                    switchState = isChecked; // 满足条件，更新状态
                }
            }
        };
            btn_test.setOnCheckedChangeListener(listener);


        //悬浮窗监听事件
        Button floating = findViewById(R.id.FloatingWindow);
        floating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FloatingViewService.class);
                startService(intent);
                Log.d("MainActivity", "启动悬浮窗");
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initLocation();
        //绑定生命周期 onCreate
        binding.mapView.onCreate(savedInstanceState);
        //初始化地图
        initMap();
        //初始化搜索
        initSearch();
        //初始化控件
        initView();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        } else {
            // 权限已授予，直接启动服务
            startFloatingService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // 权限已授予，启动服务
                startFloatingService();
            } else {
                Toast.makeText(this,"权限未授予", Toast.LENGTH_SHORT).show();
                Log.e("MainActivity", "Overlay permission not granted");
                // 权限未授予，可以给用户一个提示，如 Toast 或 Dialog
            }
        }
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingViewService.class);
        startService(intent);
        Log.d("MainActivity", "启动悬浮窗服务");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //绑定生命周期
        binding.mapView.onResume();
        // 检查是否已经获取到定位权限
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 获取到权限
            Log.d(TAG, "onResume: 已获取到权限");
            showMsg("已获取到权限");
            startLocation();
        } else {
            // 请求定位权限
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * 定位回调结果
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null) {
            showMsg("定位失败，aMapLocation 为空");
            return;
        }
        // 获取定位结果
        if (aMapLocation.getErrorCode() == 0) {
            // 定位成功
            showMsg("定位成功");
//            aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
//            aMapLocation.getLatitude();//获取纬度
//            aMapLocation.getLongitude();//获取经度
//            aMapLocation.getAccuracy();//获取精度信息
//            aMapLocation.getAddress();//详细地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//            aMapLocation.getCountry();//国家信息
//            aMapLocation.getProvince();//省信息
//            aMapLocation.getCity();//城市信息
            String result = aMapLocation.getDistrict();//城区信息
//            aMapLocation.getStreet();//街道信息
//            aMapLocation.getStreetNum();//街道门牌号信息
//            aMapLocation.getCityCode();//城市编码
//            aMapLocation.getAdCode();//地区编码
//            aMapLocation.getAoiName();//获取当前定位点的AOI信息
//            aMapLocation.getBuildingId();//获取当前室内定位的建筑物Id
//            aMapLocation.getFloor();//获取当前室内定位的楼层
//            aMapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
            // 停止定位
            stopLocation();
            // 显示地图定位结果
            if (mListener != null) {
                mListener.onLocationChanged(aMapLocation);
            }
            // 显示浮动按钮
            binding.fabPoi.show();
            // 城市编码赋值
            cityCode = aMapLocation.getCityCode();
            city = aMapLocation.getCity();
        } else {
            // 定位失败
            showMsg("定位失败，错误：" + aMapLocation.getErrorInfo());
            Log.e(TAG,"location Error, ErrCode:"
                    + aMapLocation.getErrorCode() + ", errInfo:"
                    + aMapLocation.getErrorInfo());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 绑定生命周期 onPause
        binding.mapView.onPause();
        Log.e(TAG, "onPause: 开始" );
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 绑定生命周期 onSaveInstanceState
        binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 绑定生命周期 onDestroy
        binding.mapView.onDestroy();
    }
    /**
     * 初始化控件
     */
    private void initView() {
        // Poi搜索按钮点击事件
        binding.fabPoi.setOnClickListener(v -> {
            //构造query对象
            query = new PoiSearch.Query("购物", "", cityCode);
            // 设置每页最多返回多少条poiItem
            query.setPageSize(10);
            //设置查询页码
            query.setPageNum(1);
            //构造 PoiSearch 对象
            try {
                poiSearch = new PoiSearch(this, query);
                //设置搜索回调监听
                poiSearch.setOnPoiSearchListener(this);
                //发起搜索附近POI异步请求
                poiSearch.searchPOIAsyn();
            } catch (com.amap.api.services.core.AMapException e) {
                throw new RuntimeException(e);
            }
        });
        //全屏按钮点击事件
        binding.FullScreen.setOnClickListener(v -> Fullscreen());
        // 清除标点按钮点击事件
        binding.fabClearMarker.setOnClickListener(v -> clearAllMarker());
        // 路线按钮点击事件
        binding.fabRoute.setOnClickListener(v -> startActivity(new Intent(this,RouteActivity.class)));

        // 键盘按键监听
        binding.etAddress.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                //获取输入框的值
                String address = binding.etAddress.getText().toString().trim();
                if (address.isEmpty()) {
                    showMsg("请输入地址");
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    //隐藏软键盘
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

                    // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
                    GeocodeQuery query = new GeocodeQuery(address, city);
                    geocodeSearch.getFromLocationNameAsyn(query);
                }
                return true;
            }
            return false;
        });

    }
    private void Fullscreen(){
        uilts();
        // 在这里添加你的点击事件逻辑
        boolean isFullScreen;
        isFullScreen = (getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN;
        // 例如，切换全屏模式
        if (isFullScreen) {
            // 退出全屏模式
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullScreen = false;
            testmsg = false;
            text_fouse.setText("进入全屏");
            Log.d(TAG, "Fullscreen: 退出全屏");
        } else {
            // 进入全屏模式
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            Log.d(TAG, "Fullscreen: 进入全屏");
            testmsg = true;
            text_fouse.setText("退出全屏");
            isFullScreen = true;
        }
    }

    /**
     * 初始化地图
     */
    private void initMap() {
        if (aMap == null) {

            aMap = binding.mapView.getMap();
            // 创建定位蓝点的样式
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            // 自定义定位蓝点图标
            myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_foreground));
            // 自定义精度范围的圆形边框颜色  都为0则透明
            myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
            // 自定义精度范围的圆形边框宽度  0 无宽度
            myLocationStyle.strokeWidth(0);
            // 设置圆形的填充颜色  都为0则透明
            myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
            // 设置定位蓝点的样式
            aMap.setMyLocationStyle(myLocationStyle);
            // 设置定位监听
            aMap.setLocationSource(this);
            // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
            aMap.setMyLocationEnabled(true);

            //设置最小缩放等级为12 ，缩放级别范围为[3, 20]
            aMap.setMinZoomLevel(12);
            // 开启室内地图
            aMap.showIndoorMap(true);
            // 设置地图点击事件
            aMap.setOnMapClickListener(this);
            // 设置地图长按事件
            aMap.setOnMapLongClickListener(this);
            // 设置地图标点点击事件
            aMap.setOnMarkerClickListener(this);
            // 设置地图标点拖拽事件
            aMap.setOnMarkerDragListener(this);
            // 地图控件设置
            UiSettings uiSettings = aMap.getUiSettings();
            // 隐藏缩放按钮
            uiSettings.setZoomControlsEnabled(false);
            // 显示比例尺，默认不显示
            uiSettings.setScaleControlsEnabled(true);
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (mListener == null) {
            mListener = onLocationChangedListener;
        }
        startLocation();
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        //解析result获取POI信息

        //获取POI组数列表
        ArrayList<PoiItem> poiItems = poiResult.getPois();
        for (PoiItem poiItem : poiItems) {
            Log.d("MainActivity", " Title：" + poiItem.getTitle() + " Snippet：" + poiItem.getSnippet());
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    /**
     * 地图点击事件
     * @param latLng
     */
    public void onMapClick(LatLng latLng) {
        latLonToAddress(latLng);
        addMarker(latLng);
        //改变地图中心点
        updateMapCenter(latLng);
     }

    /**
     * 地图长按事件
     * @param latLng
     */
    public void onMapLongClick(LatLng latLng) {
        showMsg("长按了地图，经度："+latLng.longitude+"，纬度："+latLng.latitude);
        latLonToAddress(latLng);
    }
    /**
     * 初始化搜索
     */
    private void initSearch() {
        // 构造 GeocodeSearch 对象
        try {
            geocodeSearch = new GeocodeSearch(this);
            // 设置监听
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (com.amap.api.services.core.AMapException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 通过经纬度获取地址
     * @param latLng
     */
    private void latLonToAddress(LatLng latLng) {
        //位置点  通过经纬度进行构建
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        //逆编码查询  第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 20, GeocodeSearch.AMAP);
        //异步获取地址信息
        geocodeSearch.getFromLocationAsyn(query);
    }


    /**
     * 坐标转地址
     * @param regeocodeResult
     * @param rCode
     */
    /**
     * 地址转坐标
     * @param geocodeResult
     * @param rCode
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
        if (rCode != PARSE_SUCCESS_CODE) {
            showMsg("获取坐标失败");
            return;
        }
        List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
        if (geocodeAddressList != null && !geocodeAddressList.isEmpty()) {
            LatLonPoint latLonPoint = geocodeAddressList.get(0).getLatLonPoint();
            //显示解析后的坐标
            showMsg("坐标：" + latLonPoint.getLongitude() + "，" + latLonPoint.getLatitude());
        }

    }
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        //解析result获取地址描述信息
        if(rCode == PARSE_SUCCESS_CODE){
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
            //显示解析后的地址
            showMsg("地址："+regeocodeAddress.getFormatAddress());
        }else {
            showMsg("获取地址失败");
        }
    }

    /**
     * 添加地图标点
     *
     * @param latLng
     */
    private void addMarker(LatLng latLng) {
        //显示浮动按钮
        binding.fabClearMarker.show();
        //添加标点
        Marker marker = aMap.addMarker(new MarkerOptions().
                draggable(true)
                .position(latLng)
                .title("标题")
                .snippet("详细内容"));
        markerList.add(marker);
        //设置标点的绘制动画效果
        Animation animation = new RotateAnimation(marker.getRotateAngle(),marker.getRotateAngle()+180,0,0,0);
        long duration = 1000L;
        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());

        marker.setAnimation(animation);
        marker.startAnimation();
    }

    /**
     * 清空地图Marker
     */
    public void clearAllMarker() {
        if (markerList != null && !markerList.isEmpty()) {
            for (Marker markerItem : markerList) {
                markerItem.remove();
            }
        }
        binding.fabClearMarker.hide();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (!marker.isInfoWindowShown()) { // 显示
            marker.showInfoWindow();
        } else { // 隐藏
            marker.hideInfoWindow();
        }
        return true;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d(TAG, "开始拖拽");
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d(TAG, "拖拽中...");
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        showMsg("拖拽完成");
    }
    /**
     * 改变地图中心位置
     * @param latLng 位置
     */
    private void updateMapCenter(LatLng latLng) {
        // CameraPosition 第一个参数： 目标位置的屏幕中心点经纬度坐标。
        // CameraPosition 第二个参数： 目标可视区域的缩放级别
        // CameraPosition 第三个参数： 目标可视区域的倾斜度，以角度为单位。
        // CameraPosition 第四个参数： 可视区域指向的方向，以角度为单位，从正北向顺时针方向计算，从0度到360度
        CameraPosition cameraPosition = new CameraPosition(latLng, 16, 30, 0);
        //位置变更
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        //改变位置（使用动画）
        aMap.animateCamera(cameraUpdate);
    }

}