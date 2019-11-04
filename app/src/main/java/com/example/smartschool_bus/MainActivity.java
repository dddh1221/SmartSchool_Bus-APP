package com.example.smartschool_bus;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private TextView tv_nowBusStop, tv_nextBusStop, tv_seat;
    private ImageView im_stop1, im_stop2, im_stop3, im_stop4, im_loading1, im_loading2, im_loading3;
    private ListView list_distance;

    private BeaconManager beaconManager;
    private List<Beacon> beaconList = new ArrayList<>();
    private ArrayList<BusDistance> busDistanceArrayList;

    private ListviewAdapter listviewAdapter;

    private final String BEACON_ID = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private final String BUS_STATION_1 = "30101111-1111-1111-8f0c-720eaf059935";    // 통신과
    private final String BUS_STATION_2 = "30101222-2222-2222-8f0c-720eaf059935";    // 제어과
    private final String BUS_STATION_3 = "30101333-3333-3333-8f0c-720eaf059935";    // 회로과

    private String BUS_STOP1 = "통신과";
    private String BUS_STOP2 = "제어과";
    private String BUS_STOP3 = "회로과";

    private SocketManager m_SocketManager;

    private final static int SOCKET_CREATE_SUCCESS = 0;
    private final static int DATA_RECV_SUCCESS = 1;

    private final static String IP = "192.168.0.4";
    private final static int PORT = 8301;

    private int SOCKET_FLAG;
    private boolean THREAD = true;
    private int BUS_ID = 1;
    private final double BUS_LIMIT_DISTANCE = 1.0;

    private String bus_now = "";
    private String now_station = "";
    private String next_station = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        tv_nowBusStop = (TextView)findViewById(R.id.tv_nowBusStop);
        tv_nextBusStop = (TextView)findViewById(R.id.tv_nextBusStop);
        tv_seat = (TextView)findViewById(R.id.tv_seat);

        im_stop1 = (ImageView)findViewById(R.id.im_stop1);
        im_stop2 = (ImageView)findViewById(R.id.im_stop2);
        im_stop3 = (ImageView)findViewById(R.id.im_stop3);
        im_stop4 = (ImageView)findViewById(R.id.im_stop4);

        im_loading1 = (ImageView)findViewById(R.id.im_loading1);
        im_loading2 = (ImageView)findViewById(R.id.im_loading2);
        im_loading3 = (ImageView)findViewById(R.id.im_loading3);

        list_distance = (ListView)findViewById(R.id.list_distance);

        m_SocketManager = new SocketManager(IP, PORT, socket_Handler);
        busDistanceArrayList = new ArrayList<BusDistance>();
        listviewAdapter = new ListviewAdapter(getApplicationContext(), busDistanceArrayList);
        list_distance.setAdapter(listviewAdapter);

        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));

        // 비콘을 탐지하는 티콘매니저 객체 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);
        // 비콘 스캔 객체에 비콘의 식별번호를 입력해준다.
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_ID));
        // 비콘 스캔 시작
        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                if(collection.size() > 0) {
                    Log.d("SCAN", "beacon SCAN");
                    beaconList.clear();

                    for(Beacon beacon : collection) {
                        beaconList.add(beacon);
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {

        }
    }

    private Handler beacon_Handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                for (Beacon beacon : beaconList) {
                    m_SocketManager.sendData("bus distance " + beacon.getId1() + " " + Double.parseDouble(String.format("%.3f", beacon.getDistance())));

                    Log.d("next_station", next_station);
                    Log.d("ID", String.valueOf(beacon.getId1()));

                    if (next_station.equals(BUS_STOP1) && String.valueOf(beacon.getId1()).equals(BUS_STATION_1) && beacon.getDistance() <= BUS_LIMIT_DISTANCE) {
                        m_SocketManager.sendData("bus check " + BUS_ID);

                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                    } else if (next_station.equals(BUS_STOP2) && String.valueOf(beacon.getId1()).equals(BUS_STATION_2) && beacon.getDistance() <= BUS_LIMIT_DISTANCE) {
                        m_SocketManager.sendData("bus check " + BUS_ID);

                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                    } else if (next_station.equals(BUS_STOP3) && String.valueOf(beacon.getId1()).equals(BUS_STATION_3) && beacon.getDistance() <= BUS_LIMIT_DISTANCE) {
                        m_SocketManager.sendData("bus check " + BUS_ID);

                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                    } else if (next_station.equals(BUS_STOP1) && String.valueOf(beacon.getId1()).equals(BUS_STATION_1) && beacon.getDistance() > BUS_LIMIT_DISTANCE) {
                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                    } else if (next_station.equals(BUS_STOP2) && String.valueOf(beacon.getId1()).equals(BUS_STATION_2) && beacon.getDistance() > BUS_LIMIT_DISTANCE) {
                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                    } else if (next_station.equals(BUS_STOP3) && String.valueOf(beacon.getId1()).equals(BUS_STATION_3) && beacon.getDistance() > BUS_LIMIT_DISTANCE) {
                        im_stop1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading1.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading2.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop3.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_loading3.setImageDrawable(getDrawable(R.drawable.bus_route_wait2));
                        im_stop4.setImageDrawable(getDrawable(R.drawable.bus_route_wait));
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {

            }

            beacon_Handler.sendEmptyMessageDelayed(0, 1000);
        }
    };

    private Handler socket_Handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SOCKET_CREATE_SUCCESS:
                    // 비콘 리더 핸들러 동작
                    beacon_Handler.sendEmptyMessage(0);

                    ReadThread thread = new ReadThread();
                    thread.start();
                    break;

                case DATA_RECV_SUCCESS:
                    switch(SOCKET_FLAG) {
                        case 1:     // bus get bus_info 1 명령 처리
                            try {
                                JSONArray info_array = new JSONArray(msg.obj.toString());
                                JSONObject info_object = info_array.getJSONObject(0);

                                now_station = info_object.getString("now_station");
                                next_station = info_object.getString("next_station");
                                String rest_seat = info_object.getString("rest_seat");

                                tv_nowBusStop.setText(now_station);
                                tv_nextBusStop.setText(next_station);
                                tv_seat.setText(rest_seat);



                                m_SocketManager.sendData("bus get distance"); SOCKET_FLAG = 2;
                            } catch (JSONException e) {

                            }
                            break;

                        case 2:
                            try {
                                busDistanceArrayList.clear();
                                JSONArray distance_array = new JSONArray(msg.obj.toString());

                                for(int i = 0; i < distance_array.length() ; i++) {
                                    JSONObject distance_object = distance_array.getJSONObject(i);

                                    String station = distance_object.getString("station");
                                    String distance = distance_object.getString("distance") + "m";

                                    BusDistance e = new BusDistance(station, distance);
                                    busDistanceArrayList.add(e);
                                }

                                list_distance.setAdapter(listviewAdapter);
                            } catch (JSONException e) {

                            }
                            break;
                    }
                    break;
            }
        }
    };

    private class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                while (THREAD) {
                    if(m_SocketManager != null) {
                        m_SocketManager.sendData("bus get bus_info " + BUS_ID); SOCKET_FLAG = 1;
                    } else {
                        Toast.makeText(MainActivity.this, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        THREAD = false;
                    }

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Toast.makeText(MainActivity.this, "서버와 연결하는데 문제가 발생했습니다.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private class ListviewAdapter extends BaseAdapter {
        Context context;
        ArrayList<BusDistance> busDistanceArrayList = new ArrayList<>();

        public ListviewAdapter(Context context, ArrayList<BusDistance> busDistanceArrayList) {
            this.context = context;
            this.busDistanceArrayList = busDistanceArrayList;
        }

        @Override
        public int getCount() {
            return busDistanceArrayList.size();
        }

        @Override
        public Object getItem(int i) {
            return busDistanceArrayList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) convertView = new ListviewItem(context);
            ((ListviewItem)convertView).setData(busDistanceArrayList.get(position));
            return convertView;
        }
    }

    private class ListviewItem extends LinearLayout {
        TextView list_tv_bus_station_name, list_tv_bus_distance;

        public ListviewItem(Context context) {
            super(context);
            init(context);
        }

        private void init(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_bus_item, this);
            list_tv_bus_station_name = (TextView)findViewById(R.id.list_tv_bus_station_name);
            list_tv_bus_distance = (TextView)findViewById(R.id.list_tv_bus_distance);
        }

        public void setData(BusDistance one) {
            list_tv_bus_station_name.setText(one.getName());
            list_tv_bus_distance.setText(one.getDistance());
        }
    }

    private class BusDistance {
        private String name, distance;

        public BusDistance(String name, String distance) {
            this.name = name;
            this.distance = distance;
        }

        public String getName() {
            return name;
        }

        public String getDistance() {
            return distance;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDistance(String distance) {
            this.distance = distance;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        m_SocketManager.closeSocket();
    }
}
