package com.yuni.control.AI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.yuni.control.Packet;
import com.yuni.control.YuniControl;

public class World
{
    public static final byte TYPE_YUNIMIN = 1;
    
    public static final byte STARTPOS_BLUE = 1;
    public static final byte STARTPOS_RED = 2;
    
    
    public World(Handler handler, byte startPos)
    {
        objectList = new ArrayList<WorldObject>();
        mHandler = handler;
        mStartPos = startPos;
    }
    
    public static void CreateInstance(Handler handler, byte startPos)
    {
        instance = new World(handler, startPos);
    }
    
    public static World getInstance()
    {
        return instance;
    }
    public static void destroy()
    {
        instance = null;
    }
    
    public void AddObject(WorldObject object) { objectList.add(object); }
    public void RemoveObject(WorldObject object) { objectList.remove(object); }
    public Yunimin GetYunimin()
    {
        return yunimin;
    }
    
    public void Initiate()
    {
        //Spawn Yunimin
        yunimin = new Yunimin();
        AddObject(yunimin);    
    }
    
    public void Update(int diff)
    {
        for(Iterator<WorldObject> i = objectList.iterator(); i.hasNext();)
        {
            i.next().Update(diff);
        }
    }
    
    public void SendPacket(Packet pkt)
    {    
        Message msg = new Message();
        msg.what = YuniControl.MESSAGE_DATA;
        msg.obj = pkt;
        mHandler.sendMessage(msg);
    }
    
    public void end()
    {
    	 Message msg = new Message();
         msg.what = YuniControl.MESSAGE_STOP;
         mHandler.sendMessage(msg);
    }
    
    public byte GetStartPos()
    {
        return mStartPos;
    }
    
    List<WorldObject> objectList;
    private static World instance = null;
    private Yunimin yunimin = null;
    private Handler mHandler = null;
    private byte mStartPos;
}