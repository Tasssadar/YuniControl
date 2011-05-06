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
                World.getInstance().GetYunimin().ButtonStatus(adr, status);
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
                World.getInstance().GetYunimin().sendMovement();
                break;
            }
            case Protocol.CMSG_TEST_RESULT:
            {
            	log = "Test result:\r\n  Button: " + packet.readByte() + "\r\n  LeftEnc: " + packet.readUInt16() +
            	    "\r\n RightEnc: " + packet.readUInt16();
            	break;
            }
            case Protocol.CMSG_RANGE_BLOCK:
            	log = "range block!";
            	World.getInstance().GetYunimin().RangeBlock(true);
            	break;
            case Protocol.CMSG_RANGE_BLOCK_GONE:
            	log = "range block gone.";
            	World.getInstance().GetYunimin().RangeBlock(false);
            	break;
            case Protocol.CMSG_RANGE_VALUE:
            	log = "range " + Integer.toHexString(packet.readByte()) + " " + packet.readUInt16();
            	break;
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