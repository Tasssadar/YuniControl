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
    private boolean hardEnd;
    private boolean discIn;
    
    private final static byte SPEC_ACT_OPEN_DOORS  = 1;
    private final static byte SPEC_ACT_CLOSE_DOORS = 2;
    public  final static byte SPEC_ACT_DISC_IN     = 3;
    private final static byte SPEC_ACT_MATCH_END   = 4;
    private final static byte SPEC_ACT_CHECK_FOR_DISC= 5;
    private final static byte SPEC_ACT_CONTINUE    = 6;
    
    private final static byte SERVO_DOORS        = 0x01;                                                                                                                
    private final static byte SERVO_BRUSHES      = 0x02;
    private final static byte SERVO_REEL         = 0x04;
    
    private final static int DOORS_OPEN          = 405;
    private final static int DOORS_CLOSE         = 420;
    private final static int BRUSHES_UP          = 0;
    private final static int BRUSHES_DOWN        = 650;
    
    private final static byte routeNodes = 17;
    private final static byte escapeNode = 8;
    private final static byte escapeNode2 = 13;
    
    private final static byte leftNode = 17;
    private final static byte rightNode = 19;
    private byte nodeToContinue = -1;
    
    private final static byte BUTTON_START     = 0x01;
    private final static byte BUTTON_PAWN      = 0x02;
    private final static byte BUTTON_FRONT     = 0x04;
    private final static byte BUTTON_RIGHT     = 0x08;
    private final static byte BUTTON_BACK_LEFT = 0x10;
    private final static byte BUTTON_BACK_RIGHT= 0x20;
    
    private static final routeNode route[]=
    {
        new routeNode((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0),
        //                                      red   blue
        new routeNode(WorldObject.MOVE_FORWARD, 1000, 1000,(byte) 127, SPEC_ACT_OPEN_DOORS,     (byte) 0),
        new routeNode(WorldObject.MOVE_LEFT,    300,  200, (byte) 127, (byte) 0,                (byte) 0),
        new routeNode(WorldObject.MOVE_FORWARD, 1390, 1390,(byte) 127, (byte) 0,                (byte) 0),
        new routeNode(WorldObject.MOVE_NONE,    0,    0,   (byte) 127, SPEC_ACT_CLOSE_DOORS,    (byte) 0),
        new routeNode(WorldObject.MOVE_LEFT,    650,  650, (byte) 127, SPEC_ACT_CHECK_FOR_DISC, (byte) 0), // 5
        new routeNode(WorldObject.MOVE_BACKWARD,1000, 950, (byte) 127, SPEC_ACT_OPEN_DOORS,     (byte) (BUTTON_BACK_LEFT| BUTTON_BACK_RIGHT)),
        new routeNode(WorldObject.MOVE_FORWARD, 5000, 5000,(byte) 127, SPEC_ACT_CHECK_FOR_DISC, (byte) 0), // 7
        
        new routeNode(WorldObject.MOVE_BACKWARD,100,  100, (byte) 127, (byte) 0,                (byte) (BUTTON_BACK_LEFT| BUTTON_BACK_RIGHT)), // 8 - this one is calculated
        new routeNode(WorldObject.MOVE_FORWARD, 100,  100, (byte) 127, (byte) 0,                (byte) 0), // 9 - this one is calculated
        new routeNode(WorldObject.MOVE_LEFT,    1600,  1200,(byte) 127, (byte) 0,               (byte) 0), // 10
        new routeNode(WorldObject.MOVE_FORWARD, 2650, 3500,(byte) 127, (byte) 0,                (byte) BUTTON_FRONT), // 11
        new routeNode(WorldObject.MOVE_BACKWARD,200,  200, (byte) 127, SPEC_ACT_MATCH_END,      (byte) 0), // 12
        
        new routeNode(WorldObject.MOVE_LEFT,    670,  670, (byte) 127, (byte) 0,                (byte) 0), // 13
        new routeNode(WorldObject.MOVE_BACKWARD,500,  1000,(byte) 127, (byte) 0,                (byte) (BUTTON_BACK_LEFT| BUTTON_BACK_RIGHT)), // 14
        new routeNode(WorldObject.MOVE_LEFT,    1200, 1200,(byte) 127, (byte) 0,                (byte) 0), // 15
        new routeNode(WorldObject.MOVE_FORWARD, 3500, 3500,(byte) 127, (byte) 0,                (byte) BUTTON_FRONT), // 16
        new routeNode(WorldObject.MOVE_BACKWARD,200,  200, (byte) 127, SPEC_ACT_MATCH_END,      (byte) 0), // 17
        
        
        /*new routeNode(WorldObject.MOVE_FORWARD, 200,  200, (byte) 127, (byte) 0,                (byte) 0), // 18
        new routeNode(WorldObject.MOVE_RIGHT,   100,  100, (byte) 127, SPEC_ACT_CONTINUE,       (byte) 0), // 19
        
        new routeNode(WorldObject.MOVE_FORWARD, 200,  200, (byte) 127, (byte) 0,                (byte) 0), // 20
        new routeNode(WorldObject.MOVE_LEFT,    100,  100, (byte) 127, SPEC_ACT_CONTINUE,       (byte) 0), // 21*/
       
        new routeNode((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0),
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
        hardEnd = false;
    }
    
    public void Start()
    {
        started = true;
    }
    
    public boolean action(byte action)
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
                    
                {
                    discIn = true;
                    
                    pkt = new Packet(Protocol.SMSG_ENCODER_RM_EVENT, null, (byte) 2);
                    pkt.writeByte(waitEventID);
                    pkt.writeByte((byte) 0);
                    World.getInstance().SendPacket(pkt);
                    
                    if(waitEventID <= 5)
                    {
                        if(waitEventID < 5)
                        {
                            waitEventID = escapeNode2;
                            node = escapeNode2;
                        }
                        else
                        {
                            waitEventID = escapeNode2+1;
                            node = escapeNode2+1;
                        }
                        sendMovement();
                        break;
                    }
                    pktMove.setWritePos((byte) 0);    
                    pktMove.writeByte((byte) 127);
                    pktMove.writeByte((byte) 0);
                    World.getInstance().SendPacket(pktMove);

                    // get value
                    Packet getEnc = new Packet(Protocol.SMSG_ENCODER_GET, null, (byte) 0);
                    World.getInstance().SendPacket(getEnc);
                    waitEventID = escapeNode;
                }
                break;
            }
            case SPEC_ACT_MATCH_END:
            {
                Packet pkt = new Packet(Protocol.SMSG_SET_SERVO_VAL, null, (byte)4);
                pkt.writeByte((short) (SERVO_BRUSHES | SERVO_REEL));
                pkt.writeUInt16(BRUSHES_UP);
                pkt.writeByte((byte) 0);
                World.getInstance().SendPacket(pkt);
                
                pktMove.setWritePos((byte) 0);    
                pktMove.writeByte((byte) 127);
                pktMove.writeByte((byte) 0);
                World.getInstance().SendPacket(pktMove);
                end = true;
                World.getInstance().Log("Spec act match end");
                break;
            }
            case SPEC_ACT_CHECK_FOR_DISC:
            {
                if(!discIn)
                    return true;
                break;
            }
            case SPEC_ACT_CONTINUE:
            {
                node = nodeToContinue;
                waitEventID = nodeToContinue;
                nodeToContinue = -1;
                sendMovement();
                return false;
            }
        }
        return true;
    }

    public void EventHappened(byte id)
    {
        if(id != waitEventID || route[node].waitButtons != 0)
            return;
        boolean cont = true;
        if(route[node].specialAction != 0)
            cont = action(route[node].specialAction);
        if(node < routeNodes && cont)
        {
            waitEventID = -1;
            ++node;
        }
    }
    
    public void ButtonStatus(short flags, short status)
    {
        switch(flags)
        {
            case BUTTON_START:
                Start();
                return;
            case BUTTON_PAWN:
                action(Yunimin.SPEC_ACT_DISC_IN);
                return;
        }
        if((route[node].waitButtons & BUTTON_BACK_LEFT) != 0 || (route[node].waitButtons & BUTTON_FRONT) != 0 && status == 1)
        {
            
            if((flags & BUTTON_BACK_LEFT) != 0 || (flags & BUTTON_BACK_RIGHT) != 0 || (flags & BUTTON_FRONT) != 0)
            {
                if(nodeToContinue != -1)
                {
                    waitEventID = nodeToContinue;
                    node = nodeToContinue;
                }
                if(route[node].specialAction != 0)
                    action(route[node].specialAction);
                if(node < routeNodes)
                {
                    waitEventID = -1;
                    ++node;
                }
            }
            /*else if((flags & BUTTON_BACK_LEFT) != 0)
            {
                pktMove.setWritePos((byte) 0);    
                pktMove.writeByte((byte) 127);
                pktMove.writeByte((byte) WorldObject.MOVE_RIGHT_WHEEL);
                World.getInstance().SendPacket(pktMove);
                nodeToContinue = node;
                waitEventID = leftNode;
                node = leftNode;
                sendMovement();
            }
            else if((flags & BUTTON_BACK_RIGHT) != 0)
            {
                ktMove.setWritePos((byte) 0);    
                pktMove.writeByte((byte) 127);
                pktMove.writeByte((byte) WorldObject.MOVE_LEFT_WHEEL);
                World.getInstance().SendPacket(pktMove);
                nodeToContinue = node;
                waitEventID = rightNode;
                node = rightNode;
                sendMovement();
            }*/
        }
    }
    
    public boolean isEnd() { return end; }
    
    public void encoderValue(int value)
    {
        node = escapeNode;
        waitEventID = escapeNode;
        /*pktEvent.setWritePos((byte) 0);
        pktEvent.writeByte(waitEventID);
        if(World.getInstance().GetStartPos() == World.STARTPOS_BLUE)
        {
            pktEvent.writeUInt16(value + 300);
            pktEvent.writeUInt16(value + 300);
        }
        else
        {
            pktEvent.writeUInt16(value + 300);
            pktEvent.writeUInt16(value + 300);
        }
        pktEvent.writeByte((byte) 1);
        World.getInstance().SendPacket(pktEvent); */
        
        pktMove.setWritePos((byte) 0);    
        pktMove.writeByte((byte) 127);
        pktMove.writeByte((byte) WorldObject.MOVE_BACKWARD);
        World.getInstance().SendPacket(pktMove);
    }
    
    public void sendMovement()
    {
        if(waitEventID == 0)
            return;
        
        if(route[node].waitButtons == 0)
        {
            pktEvent.setWritePos((byte) 0);
            pktEvent.writeByte(waitEventID);
            pktEvent.writeUInt16(route[node].getTicks(World.getInstance().GetStartPos()));
            pktEvent.writeUInt16(route[node].getTicks(World.getInstance().GetStartPos()));
            pktEvent.writeByte((byte) 1);
            World.getInstance().SendPacket(pktEvent);
        }
        
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
    
    @Override
    public void Update(int diff)
    {
        
        if(started && !hardEnd)
        {
            if(timerEnd <= diff)
            {
                end = true;
                hardEnd = true;
                World.getInstance().end();
            }else timerEnd -= diff;
        }
        
        if(!started || end || hardEnd)
            return;
   
        // Set new movement
        if(waitEventID == -1)
        {
            if(route[node].encTicksRed == 0)
            {
                action(route[node].specialAction);
                if(node < routeNodes)
                    ++node;
                else
                    return;
            }
            waitEventID = node;
            sendMovement();
        }
    }
}

class routeNode
{
    routeNode(byte flags, int ticksRed, int ticksBlue, byte speed, byte action, byte buttons)
    {
        moveFlags = flags;
        encTicksRed = ticksRed;
        encTicksBlue = ticksBlue;
        moveSpeed = speed;
        specialAction = action;
        waitButtons = buttons;
    }
    
    public int getTicks(byte side)
    {
        if(side == World.STARTPOS_BLUE)
            return encTicksBlue;
        else
            return encTicksRed;
    }
    
    public byte moveFlags;
    public int encTicksRed;
    public int encTicksBlue;
    public byte moveSpeed;
    public byte specialAction;
    public byte waitButtons;
}