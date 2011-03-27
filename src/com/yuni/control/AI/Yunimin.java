package com.yuni.control.AI;

import com.yuni.control.Packet;
import com.yuni.control.Protocol;

public class Yunimin extends WorldObject
{
    private boolean started;
    private byte node;
    private byte waitEventID;
    private Packet pktEvent;
    private Packet pktMove;
    private static final short TIMER_DISTANCE_CHECK = 300;
    private short timerDistance;
    private byte flags;
    
    private static final routeNode route[]=
    {
        // TODO fill in
        new routeNode(WorldObject.MOVE_FORWARD, 500, (byte) 127, (byte) 0),
        new routeNode(WorldObject.MOVE_LEFT, 150, (byte) 127, (byte) 0),
    };
    
    Yunimin()
    {
        m_type = World.TYPE_YUNIMIN;
        started = false;
        node = 0;
        waitEventID = -1;
        pktEvent = new Packet(Protocol.SMSG_ENCODER_SET_EVENT, null, (byte) 6);
        pktMove = new Packet(Protocol.SMSG_SET_MOVEMENT, null, (byte) 2);
        
        timerDistance = TIMER_DISTANCE_CHECK;
    }
    
    void MoveTo(WorldLoc loc)
    {
        // TODO: implement
    }
    public void Start()
    {
        started = true;
    }
    
    public void EventHappened(byte id)
    {
        if(id == waitEventID)
        {
            ++node;
            waitEventID = -1;
        }
    }
    
    @Override
    public void Update(int diff)
    {
        if(!started)
            return;
        
        // Set new movement
        if(waitEventID == -1)
        {
            waitEventID = node;
            pktEvent.setWritePos((byte) 0);
            pktEvent.writeByte(waitEventID);
            pktEvent.writeUInt16(route[node].encTicks);
            pktEvent.writeUInt16(route[node].encTicks);
            pktEvent.writeByte((byte) 1);
            World.getInstance().SendPacket(pktEvent);
                     
            // Invert flags in case of RED start position
            flags = route[node].moveFlags;
            if(World.getInstance().GetStartPos() == World.STARTPOS_RED && 
                (flags == WorldObject.MOVE_LEFT || flags == WorldObject.MOVE_RIGHT))
            {
                flags = (flags == WorldObject.MOVE_LEFT) ? WorldObject.MOVE_RIGHT : WorldObject.MOVE_LEFT;
            }
            
            pktMove.setWritePos((byte) 0);    
            pktMove.writeByte(route[node].moveSpeed);
            pktMove.writeByte(flags);
            World.getInstance().SendPacket(pktMove);
        }
        
        // crash protection
        if(timerDistance <= diff)
        {
            // TODO implement
            timerDistance = TIMER_DISTANCE_CHECK;
        }else timerDistance -= diff;
    }
}

class routeNode
{
    routeNode(byte flags, int ticks, byte speed, byte action)
    {
        moveFlags = flags;
        encTicks = ticks;
        moveSpeed = speed;
        specialAction = action;
    }
    
    public byte moveFlags;
    public int encTicks;
    public byte moveSpeed;
    public byte specialAction;
}