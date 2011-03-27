package com.yuni.control;

import com.yuni.control.AI.World;

import android.os.Handler;
import android.os.Message;

class PacketHandler
{
    public PacketHandler(Handler handler)
    {
        m_handler = handler;
    }
    void HandlePacket(Packet packet)
    {
        Message msg = new Message();
        String log = null;
        boolean sendLog = true;
        switch(packet.getOpcode())
        {
            case Protocol.CMSG_PONG:
                log = "CMSG_PONG received";
                break;
            case Protocol.CMSG_GET_RANGE_VAL:
                log = "CMSG_GET_RANGE_VAL received, adress was " + packet.readByte() + " and range is " + packet.readUInt16();
                break;
            case Protocol.CMSG_EMERGENCY_START:
                log = "CMSG_EMERGENCY_START received";
                break;
            case Protocol.CMSG_EMERGENCY_END:
                log = "CMSG_EMERGENCY_END received";
                break;
            case Protocol.CMSG_ENCODER_SEND:
                log = "Encoders left: " + packet.readUInt16() + " right: " + packet.readUInt16();
                break;
            case Protocol.CMSG_BUTTON_STATUS:
            	short adr = packet.readByte();
            	short status = packet.readByte();
                log = "Button adr " + adr + " status " + status;
                if(adr == 0x01 && status == 0x01)
                	World.getInstance().GetYunimin().Start();
                break;
            case Protocol.CMSG_ENCODER_EVENT_DONE:
            	byte eventID = packet.get((byte) 0);
            	log = "Encoder event " + eventID + " passed";
            	World.getInstance().GetYunimin().EventHappened(eventID);
            	break;
            default:
                log = "Packet with opcode " + packet.getOpcode() + " and lenght " + packet.getLenght() + " recieved";
                break;
        }
        if(sendLog && log != null)
        {
            msg.what = YuniControl.MESSAGE_LOG;
            msg.obj = log;
            m_handler.sendMessage(msg);
        }
    }
    
    private Handler m_handler;
}