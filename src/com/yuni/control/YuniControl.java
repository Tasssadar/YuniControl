package com.yuni.control;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.yuni.control.AI.World;
import com.yuni.control.Packet;


public class YuniControl extends Activity {
    
    public static final byte STATE_CONNECTED = 0x01;
    public static final byte STATE_TERMINAL  = 0x02;
    public static final byte STATE_SCROLL    = 0x04;
    public static final byte STATE_RESTARTING= 0x08;
    
    public final static Config config = new Config();
    public static int state;
    
    private ArrayAdapter<String> mArrayAdapter = null;
    private ArrayAdapter<String> mPairedDevices;
    
    private Context context;
    
    private byte btTurnOn = 0;
    private View connectView = null;
    public File curFolder = null; 
    
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final byte MESSAGE_DATA = 6;
    public static final byte MESSAGE_LOG = 7;
    public static final byte MESSAGE_STOP = 8;
    public ProgressDialog dialog;
    
    private WakeLock lock = null;
    private Thread autoScrollThread = null;
    private WorldThread worldThread = null;
    private LogFile log = new LogFile();

    private final Handler conStatusHandler = new Handler() {
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case BluetoothChatService.MESSAGE_STATE_CHANGE:
                    if(msg.arg1 != BluetoothChatService.STATE_CONNECTED)
                        break;
                    log("Connection succesful!");
                    dialog.dismiss();
                    state |= STATE_CONNECTED;
                    InitTerminal();
                    break;
                case MESSAGE_LOG:
                    log((String)msg.obj);
                    break;
                case BluetoothChatService.MESSAGE_TOAST:
                    final String text = msg.getData().getString(BluetoothChatService.TOAST);
                    log("Toast: " + text);
                    if(text == null)
                        break;
                    Toast.makeText(context, text,
                            Toast.LENGTH_SHORT).show();
                    if(msg.arg1 == BluetoothChatService.CONNECTION_LOST)
                        Disconnect(true);
                    else if(msg.arg1 == BluetoothChatService.CONNECTION_FAILED)
                        EnableConnect(true);
                    break;
                case MESSAGE_DATA:
                {
                    if(msg.obj == null)
                        return;
                    if(msg.arg1 == 0)
                    {
                        Packet pkt = (Packet)msg.obj; 
                        log("Sending packet " + Protocol.opcodeToString(pkt.getOpcode()) + " lenght " + pkt.getLenght());
                        if(pkt.getOpcode() == Protocol.SMSG_ENCODER_SET_EVENT)
                            log("Encoder event " + pkt.get((byte) 0) + " setted");
                        con.SendPacket(pkt);
                    }
                    else
                        con.SendBytes((byte[])msg.obj);
                    break;
                }
                case MESSAGE_STOP:
                {
                    pingThread.pause(true);
                    log("Match end, stopping...");
                    Packet pkt = new Packet(Protocol.SMSG_STOP, null, (byte)0);
                    con.SendPacket(pkt);
                    break;
                }
            }
        }
    };
    
    private void log(String text)
    {
        log.writeString(text);
        if((state & STATE_TERMINAL) != 0)
        {
            TextView text_view = (TextView) findViewById(R.id.output);
            text_view.append(text + "\r\n");
            state |= STATE_SCROLL;
        }
    }
    public final PacketHandler packetHandler = new PacketHandler(conStatusHandler);
    
    private final Connection con = new Connection(conStatusHandler);
    
    private class WorldThread extends Thread {
        boolean stop;

        public WorldThread() {
            stop = false;
        }

        public void run()
        {
             long lastTime = Calendar.getInstance().getTimeInMillis();
             while(!stop)
             {
                 long thisTime = Calendar.getInstance().getTimeInMillis();
                 World.getInstance().Update((int)(thisTime - lastTime));
                 try
                 {
                     Thread.sleep(50);
                 } catch (InterruptedException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
                 lastTime = thisTime;
             }
        }
      
        public void cancel()
        {
            stop = true;
        }    
    }
    Packet pingPacket = new Packet(Protocol.SMSG_PING, null, (byte)0);
    
    private class PingThread extends Thread {
        boolean stop;
        boolean pause;

        public PingThread() {
            stop = false;
            pause = false;
        }

        public void run()
        {
             while(!stop)
             {
                 try
                 {
                     Thread.sleep(500);
                 } catch (InterruptedException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
                 if(!pause)
                     con.SendPacket(pingPacket);
             }
        }
      
        public void cancel()
        {
            stop = true;
        }
        
        public void pause(boolean val)
        {
            pause = val;
        }
    }
    PingThread pingThread = null;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;
        try {
            config.load();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if(config.getBool(Config.CONF_BOOL_START_PROGRAM_AFTER_START))
        {
            // TODO
        }  
        if(config.getBool(Config.CONF_BOOL_SCREEN_ALWAYS_ON))
        {
            PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
            lock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK), "YuniControl lock");
            lock.acquire();
        }
        InitMain();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }
    public void onDestroy() 
    {
        super.onDestroy();       
        if(lock != null)
            lock.release();
        Disconnect(false);
        unregisterReceiver(mReceiver);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
              if((state & STATE_TERMINAL) != 0)
                  Disconnect(true);
              else
                  finish();
              return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_MENU)
        {
            if((state & STATE_TERMINAL) != 0)
                return super.onKeyDown(keyCode, event);
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        prepareMenu(menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        prepareMenu(menu);
        return true;
    }
    
    void prepareMenu(Menu menu)
    {
        if((state & STATE_TERMINAL) != 0 && menu.findItem(R.id.restart_yuni) == null)
        {
            MenuInflater inflater = getMenuInflater();
            menu.clear();
            inflater.inflate(R.menu.menu, menu);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.restart_yuni:
                state |= STATE_RESTARTING;
                pingThread.pause(true);
                
                byte[] stop = {0x74, 0x7E, 0x7A, 0x33};
                con.SendBytes(stop);
                log("Stopping robot...");
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                con.SendBytes(stop);
                try {
                    Thread.sleep(70);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                byte[] start = {0x11};
                con.SendBytes(start);
                log("Sending start command");
                pingThread.pause(false);
                state &= ~(STATE_RESTARTING);
                World.getInstance().GetYunimin().Reset();
                log("Yunimin script restarted");
                return true;
            case R.id.restart_program:
                World.getInstance().GetYunimin().Reset();
                log("Yunimin script restarted");
                return true;
            case R.id.test:
                Packet pkt = new Packet(Protocol.SMSG_TEST, null, (byte) 0);
                con.SendPacket(pkt);
                log("Test started");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    void ShowAlert(CharSequence text)
    {
        AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
        builder2.setMessage(text)
               .setTitle("Error")
               .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       dialog.dismiss();
                   }
               });
        AlertDialog alert = builder2.create();
        alert.show();
    }
    public void Disconnect(boolean resetUI)
    {
        state = 0; 
        mArrayAdapter = null;
        mPairedDevices = null;
        con.disconnect();
        dialog = null;
        log.close();
        if(worldThread != null)
        {
            worldThread.cancel();
            pingThread.cancel();
            pingThread = null;
            worldThread = null;
        }
        World.destroy();
        
        if(resetUI)
        {
            setContentView(R.layout.main);
            InitMain();
        }
        else
        {
            context = null;  
        }
    }
    
    private void InitMain()
    {
        Button button = (Button) findViewById(R.id.Connect_b);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               con.init();
               if(con.GetConType() == Connection.CONNECTION_BLUETOOTH)
                   InitBluetooth();
               else
               {
                   //TODO
               }
            }
        });
        button = (Button) findViewById(R.id.Reload_b);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = null;
                try {
                    if(config.load())
                        text = "Config loaded.";
                    else
                        text = "Error loading config file.";
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                SetStartPosText();
                 Toast.makeText(context, text,
                            Toast.LENGTH_SHORT).show();
            }
           
        });
        button = (Button) findViewById(R.id.Side_b);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int cur = getPreferences(0).getInt("startSide", World.STARTPOS_BLUE);
                getPreferences(0).edit().putInt("startSide", 
                        cur == World.STARTPOS_RED ? World.STARTPOS_BLUE : World.STARTPOS_RED).commit();
                SetStartPosText();
            }
           
        });
        SetStartPosText();
        
    }
    
    private void SetStartPosText()
    {
        TextView textConf = (TextView)findViewById(R.id.config_status_t);
        if(textConf == null)
            return;
        textConf.setText("Start Position: ");
        textConf.append(getPreferences(0).getInt("startSide", World.STARTPOS_BLUE) == World.STARTPOS_RED ? "red" : "blue");
    }
    
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action) && mArrayAdapter != null) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };
    
    private void InitBluetooth()
    {
        setContentView(R.layout.device_list);
        if (con.GetAdapter() == null)
            ShowAlert("This device does not have bluetooth adapter");
        else if (!con.GetAdapter().isEnabled())
                EnableBT();
        
        mPairedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevices);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        
        Set<BluetoothDevice> pairedDevices = con.GetAdapter().getBondedDevices(); 
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        
        final Button button = (Button) findViewById(R.id.button_scan);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               FindDevices();
            }
        });
    }
    
    void EnableBT()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, Connection.REQUEST_ENABLE_BT);
    }
    
    private final OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            if(!con.GetAdapter().isEnabled())
            {
                btTurnOn = 2;
                connectView = v;
                EnableBT();
                return;
            }
            Connect(v);
        }
    }; 
    
    public void FindDevices()
    {
        if(!con.GetAdapter().isEnabled())
        {
            btTurnOn = 1;
            EnableBT();
            return;
        }
        if (con.GetAdapter().isDiscovering())
            con.GetAdapter().cancelDiscovery();
        
        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mPairedDevices = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevices);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        
        Set<BluetoothDevice> pairedDevices = con.GetAdapter().getBondedDevices(); 
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        }
        
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        con.GetAdapter().startDiscovery();
    }
    
    private void Connect(View v)
    {
        log.init(config.getBool(Config.CONF_BOOL_LOG_TIME));
        
        EnableConnect(false);
        // Cancel discovery because it's costly and we're about to connect
        con.GetAdapter().cancelDiscovery();

        // Get the device MAC address, which is the last 17 chars in the View
        String info = ((TextView) v).getText().toString();
        String address = info.substring(info.length() - 17);
        log("Attempting to connect to " + address + " ...");

        // Create the result Intent and include the MAC address
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        BluetoothDevice device = con.GetAdapter().getRemoteDevice(address);
        con.ConnectBT(device);
    }
    
    public void EnableConnect(boolean enable)
    {
        if(!enable)
        {
            dialog= new ProgressDialog(this);
            dialog.setCancelable(true);
            dialog.setMessage("Connecting...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);    
            dialog.setMax(0);
            dialog.setProgress(0);
            dialog.setOnCancelListener(new Dialog.OnCancelListener()
            {
                public void onCancel(DialogInterface dia)
                {
                    con.stopBTService();
                    EnableConnect(true);
                }
            });
            dialog.show();
        }
        else
            dialog.dismiss();
        Button button = (Button) findViewById(R.id.button_scan);
        button.setEnabled(enable);
        ListView listView = (ListView) findViewById(R.id.new_devices);
        listView.setEnabled(enable);
        listView = (ListView) findViewById(R.id.paired_devices);
        listView.setEnabled(enable);

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != Connection.REQUEST_ENABLE_BT)
            return;

        if (resultCode == Activity.RESULT_OK)
        {
            switch(btTurnOn)
            {
                case 1:
                    FindDevices();
                    break;
                case 2:
                    Connect(connectView);
                    break;
                case 0:
                    Set<BluetoothDevice> pairedDevices = con.GetAdapter().getBondedDevices(); 
                    if (pairedDevices.size() > 0) {
                        mPairedDevices.clear();
                        findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                        for (BluetoothDevice device : pairedDevices) {
                            mPairedDevices.add(device.getName() + "\n" + device.getAddress());
                        }
                    }
                    break;
            }
            btTurnOn = 0;
            connectView = null;
        }
        else if(btTurnOn != 0)
           ShowAlert("Bluetooth is disabled!");
    }
    
    private void InitTerminal()
    {
        state |= STATE_TERMINAL;
        setContentView(R.layout.terminal);

        autoScrollThread = new Thread (new Runnable()
        {
            public void run()
            {
                TextView out = (TextView) findViewById(R.id.output);
                ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
                while(true)
                {
                    if((state & STATE_TERMINAL) == 0)
                        break;

                    if((state & STATE_SCROLL) != 0 && scroll.getScrollY() != out.getHeight())
                    {
                        scrollHandler.sendEmptyMessage(0);
                        state &= ~(STATE_SCROLL);
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        autoScrollThread.setPriority(1);
        autoScrollThread.start();
        World.destroy();
        World.CreateInstance(conStatusHandler, (byte) getPreferences(0).getInt("startSide", World.STARTPOS_BLUE));
        World.getInstance().Initiate();
        pingThread = new PingThread();
        pingThread.start();
        worldThread = new WorldThread();
        worldThread.start();
    }
    private final Handler scrollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final TextView out = (TextView) findViewById(R.id.output);
            final ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
            if(scroll == null || out == null)
                return;
            scroll.scrollTo(0, out.getHeight());
        }
    };
}