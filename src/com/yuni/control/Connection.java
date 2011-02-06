package com.yuni.control;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

public class Connection
{
    static final int REQUEST_ENABLE_BT = 2;
    
    public static final byte CONNECTION_BLUETOOTH = 1;
    public static final byte CONNECTION_USB       = 2;
    
    public Connection(Handler handler)
    {
        con_type = 0;
        bluetoothService = null;
        mBluetoothAdapter = null;
        statusHandler = handler;
        protocol = new Protocol(handler);
    }
    
    public void init()
    {
        con_type = YuniControl.config.getByte(Config.CONF_BYTE_CONNECT_METHOD);
        if(con_type == CONNECTION_BLUETOOTH)
            init_bluetooth();
        else if(con_type == CONNECTION_USB)
            init_usb();
    }
    
    private void init_bluetooth()
    {
        bluetoothService = new BluetoothChatService(bluetoothHandler); 
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    private void init_usb()
    {
        // TODO
    }
    
    public void stopBTService()
    {
        bluetoothService.stop();
    }
    
    public void ConnectBT(BluetoothDevice device)
    {
        bluetoothService.start();
        bluetoothService.connect(device);
    }
    
    public void disconnect()
    {
        if(con_type == CONNECTION_BLUETOOTH)
        {
            bluetoothService.stop();
        }
        else
        {
            //TODO
        }
    }
    
    public byte GetConType() { return con_type; }
    public BluetoothAdapter GetAdapter() { return mBluetoothAdapter; }
    
    public void SendPacket(Packet pkt)
    {
        if(con_type == CONNECTION_BLUETOOTH)
        {
            byte[] data = new byte[pkt.getLenght()+3];
            data[0] = 1;
            data[1] = pkt.getOpcode();
            data[2] = pkt.getLenght();
            for(byte y = 0; y < pkt.getLenght(); ++y)
                data[y+3] = pkt.get(y);
            bluetoothService.write(data);
        }
        else
        {
            //TODO
        }
    }
    
    private final Handler bluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == BluetoothChatService.MESSAGE_READ && msg.obj != null)
            {
                protocol.parseData((byte[])msg.obj, (byte) msg.arg1);
            }
            else
            {
                Message msgNew = new Message();
                msgNew.what = msg.what;
                msgNew.obj = msg.obj;
                msgNew.setData(msg.getData());
                msgNew.arg1 = msg.arg1;
                msgNew.arg2 = msg.arg2;
                statusHandler.sendMessage(msgNew);
            }
        }
    };
    
    private byte con_type;
    private BluetoothChatService bluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    
    private Handler statusHandler;
    private Protocol protocol;
}