package com.yuni.control;

import com.yuni.control.AI.World;
import com.yuni.control.AI.Yunimin;

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
                //log = "CMSG_PONG received";
                break;
            case Protocol.CMSG_EMERGENCY_START:
                log = "CMSG_EMERGENCY_START received";
                break;
            case Protocol.CMSG_EMERGENCY_END:
                log = "CMSG_EMERGENCY_END received";
                break;
            case Protocol.CMSG_BUTTON_STATUS:
                short adr = packet.readByte();
                short status = packet.readByte();
                log = "Button adr " + adr + " status " + status;
                if(adr == 0x01 && status == 0x01)
                    World.getInstance().GetYunimin().Start();
                else if(adr == 0x02 && status == 0x01)
                    World.getInstance().GetYunimin().action(Yunimin.SPEC_ACT_DISC_IN);
                break;
            case Protocol.CMSG_ENCODER_EVENT_DONE:
                byte eventID = packet.get((byte) 0);
                log = "Encoder event " + eventID + " passed";
                World.getInstance().GetYunimin().EventHappened(eventID);
                break;
            case Protocol.CMSG_ENCODER_SEND:
                int enc = (packet.readUInt16() + packet.readUInt16())/2;
                log = "Encoder value " + enc;
                World.getInstance().GetYunimin().encoderValue(enc);
                break;
            case Protocol.CMSG_LOCKED:
            {
                if(World.getInstance().GetYunimin().isEnd())
                    break;
                Packet req = new Packet(Protocol.SMSG_CONNECT_REQ, null, (byte) 0);
                Message req_msg = new Message();
                req_msg.what = YuniControl.MESSAGE_DATA;
                req_msg.obj = req;
                m_handler.sendMessage(req_msg);
                log = "Yunimin locked, sending connect req";
                break;
            }
            case Protocol.CMSG_CONNECT_RES:
            {
                short locked = packet.readByte();
                log = "Connect result, locked: " + locked;
                if(locked == 1)
                {
                    log += " - unlocking...";
                    Packet unlock = new Packet(Protocol.SMSG_UNLOCK, null, (byte ) 0);
                    Message unlock_msg = new Message();
                    unlock_msg.what = YuniControl.MESSAGE_DATA;
                    unlock_msg.obj = unlock;
                    m_handler.sendMessage(unlock_msg);
                }
                break;
            }
            default:
                log = "Packet with opcode " + Protocol.opcodeToString(packet.getOpcode()) + " and lenght " + packet.getLenght() + " recieved";
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