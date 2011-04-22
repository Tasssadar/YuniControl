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
    private int timerEnd;
    private byte flags;
    private boolean end;
    private boolean discIn;
    
    private final static byte SPEC_ACT_OPEN_DOORS  = 1;
    private final static byte SPEC_ACT_CLOSE_DOORS = 2;
    public  final static byte SPEC_ACT_DISC_IN     = 3;
    private final static byte SPEC_ACT_MATCH_END   = 4;
    private final static byte SPEC_ACT_CHECK_FOR_DISC= 5;
    
    private final static byte SERVO_DOORS        = 0x01;                                                                                                                
    private final static byte SERVO_BRUSHES      = 0x02;
    private final static byte SERVO_REEL         = 0x04;
    
    private final static int DOORS_OPEN          = 405;
    private final static int DOORS_CLOSE         = 312;
    private final static int BRUSHES_UP          = 650;
    private final static int BRUSHES_DOWN        = 0;
    
    private final static byte routeNodes = 8;
    
    private static final routeNode route[]=
    {
        // TODO fill in
        new routeNode((byte) 0, (byte) 0, (byte) 0, (byte) 0),
        
        new routeNode(WorldObject.MOVE_FORWARD, 1000, (byte) 127, SPEC_ACT_OPEN_DOORS),
        new routeNode(WorldObject.MOVE_LEFT,    180,  (byte) 127, (byte) 0),
        new routeNode(WorldObject.MOVE_FORWARD, 1200, (byte) 127, (byte) 0),
        new routeNode(WorldObject.MOVE_LEFT,    400,  (byte) 127, SPEC_ACT_CHECK_FOR_DISC), // 4
        new routeNode(WorldObject.MOVE_FORWARD, 4500, (byte) 127, SPEC_ACT_CHECK_FOR_DISC), // 5
        
        new routeNode(WorldObject.MOVE_BACKWARD,100,  (byte) 127, (byte) 0), // this one is calculated
        new routeNode(WorldObject.MOVE_RIGHT,   500,  (byte) 127, (byte) 0),
        new routeNode(WorldObject.MOVE_BACKWARD,1000, (byte) 127, SPEC_ACT_MATCH_END),

        new routeNode((byte) 0, (byte) 0, (byte) 0, (byte) 0),
    };
    
    Yunimin()
    {
    	m_type = World.TYPE_YUNIMIN;
        pktEvent = new Packet(Protocol.SMSG_ENCODER_SET_EVENT, null, (byte) 6);
        pktMove = new Packet(Protocol.SMSG_SET_MOVEMENT, null, (byte) 2);
    }
    
    public void Reset()
    {
        started = false;
        node = 1;
        waitEventID = -1;
        end = false;
        timerEnd = 90000;
        discIn = false;
    }
    
    public void Start()
    {
        started = true;
    }
    
    public void action(byte action)
    {
        switch(action)
        {
            case SPEC_ACT_OPEN_DOORS:
            case SPEC_ACT_CLOSE_DOORS:
            case SPEC_ACT_DISC_IN:
            {
                Packet pkt = new Packet(Protocol.SMSG_SET_SERVO_VAL, null, (byte)4);
                pkt.writeByte(SERVO_DOORS);
                pkt.writeUInt16((SPEC_ACT_OPEN_DOORS == action) ? DOORS_OPEN : DOORS_CLOSE);
                pkt.writeByte((short) ((SPEC_ACT_OPEN_DOORS == action) ? 0 : 1));
                World.getInstance().SendPacket(pkt);
                if(action == SPEC_ACT_DISC_IN)
                    discIn = true;
                break;
            }
            case SPEC_ACT_MATCH_END:
            {
                Packet pkt = new Packet(Protocol.SMSG_SET_SERVO_VAL, null, (byte)4);
                pkt.writeByte((short) (SERVO_BRUSHES | SERVO_REEL));
                pkt.writeUInt16(BRUSHES_UP);
                pkt.writeByte((byte) 0);
                World.getInstance().SendPacket(pkt);
                break;
            }
            case SPEC_ACT_CHECK_FOR_DISC:
            {
                if(!discIn)
                    return;
                
                // stop
                pktMove.setWritePos((byte) 0);    
                pktMove.writeByte((byte) 127);
                pktMove.writeByte((byte) 0);
                World.getInstance().SendPacket(pktMove);
                
                // get value
                Packet getEnc = new Packet(Protocol.SMSG_ENCODER_GET, null, (byte) 0);
                World.getInstance().SendPacket(getEnc);
                waitEventID = 6;
                break;
            }
        }
    }
    
    public void EventHappened(byte id)
    {
    	if(route[node].specialAction != 0)
            action(route[node].specialAction);
        if(id == waitEventID && node < routeNodes)
        {
            waitEventID = -1;
            ++node;
        }
    }
    
    public boolean isEnd() { return end; }
    
    public void encoderValue(int value)
    {
        node = 6;
        waitEventID = node;
        pktEvent.setWritePos((byte) 0);
        pktEvent.writeByte(waitEventID);
        pktEvent.writeUInt16(value + 800);
        pktEvent.writeUInt16(value + 800);
        pktEvent.writeByte((byte) 1);
        World.getInstance().SendPacket(pktEvent);
    }
    
    @Override
    public void Update(int diff)
    {
        if(!started || end)
            return;
        
        if(timerEnd <= diff)
        {
            end = true;
            World.getInstance().end();
        }else timerEnd -= diff;
   
        // Set new movement
        if(waitEventID == -1)
        {
            if(route[node].encTicks == 0)
            {
                action(route[node].specialAction);
                if(node < routeNodes)
                	++node;
                else
                	return;
            }
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