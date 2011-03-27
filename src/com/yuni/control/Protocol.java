package com.yuni.control;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public class Protocol
{
    public final static byte SMSG_PING                = 0x01;
    public final static byte CMSG_PONG                = 0x02;
    public final static byte SMSG_SET_MOVEMENT        = 0x03;
    public final static byte SMSG_SET_CORRECTION_VAL  = 0x04;
    public final static byte SMSG_GET_RANGE_VAL       = 0x05;
    public final static byte CMSG_GET_RANGE_VAL       = 0x06;
    public final static byte CMSG_EMERGENCY_START     = 0x07;
    public final static byte CMSG_EMERGENCY_END       = 0x08;
    public final static byte SMSG_SET_EMERGENCY_INFO  = 0x09;
    public final static byte SMSG_SET_SERVO_VAL       = 0x10;
    public final static byte SMSG_ENCODER_START       = 0x11;
    public final static byte SMSG_ENCODER_GET         = 0x12;
    public final static byte CMSG_ENCODER_SEND        = 0x13;
    public final static byte SMSG_ENCODER_STOP        = 0x14;
    public final static byte SMSG_ENCODER_SET_EVENT   = 0x15;
    public final static byte CMSG_ENCODER_EVENT_DONE  = 0x16;
    public final static byte CMSG_LASER_GATE_STAT     = 0x17;
    public final static byte SMSG_LASER_GATE_SET      = 0x18;
    public final static byte CMSG_BUTTON_STATUS       = 0x19;
    public final static byte SMSG_ADD_STATE           = 0x20;
    public final static byte SMSG_REMOVE_STATE        = 0x21;
    public final static byte SMSG_STOP                = 0x22;
    public final static byte CMSG_LOCKED              = 0x23;
    public final static byte SMSG_UNLOCK              = 0x24;
    
    public Protocol(Handler handler)
    {
        bytesToNext = new byte[2];    
        packetBuffer = new ArrayList<Packet>();
        bytesToNextItr = 0;
        packetHandler = new PacketHandler(handler);
        tmpPacket = new Packet((byte)0, null, (byte)0);
    }
    
    public void parseData(byte[] data, byte lenght)
    {
        byte dataItr = 0;
        if(bytesToNextItr != 0)
        {
            byte[] tmp = new byte[lenght+bytesToNextItr];
            byte itr = 0;
            for(; itr < bytesToNextItr; ++itr)
                tmp[itr] = bytesToNext[itr];
            for(; itr-bytesToNextItr < lenght; ++itr)
                tmp[itr] = data[itr-bytesToNextItr];
            lenght += bytesToNextItr;
            bytesToNextItr = 0;
        }
        
        while(dataItr < lenght)
        {
            if(status == 0 && lenght-dataItr < 4)
            {
                for(byte y = 0; y+dataItr < lenght; ++y, ++bytesToNextItr)
                    bytesToNext[y] = data[dataItr+y];
                break;
            }
            
            if(status == 0 && data[dataItr] == (byte)0xFF && lenght-dataItr >= 4) // handle new packet
            {
                tmpPacket = new Packet((byte)0, null, (byte)0);
                status = 1;
                byte[] packetData = null;
                if(data[dataItr+2] != 0)
                    packetData = new byte[data[dataItr+2]];
                
                byte y = 0;
                for(; y < data[dataItr+2] && y+dataItr+4 < lenght; ++y)
                    packetData[y] = data[y+dataItr+4];
                tmpPacket.set(data[dataItr+3], packetData, data[dataItr+2]);
                //complete packet
                if(y >= data[dataItr+2])
                {
                    packetBuffer.add(tmpPacket);
                    status = 0;
                    dataItr += 4+data[dataItr+2];
                    continue;
                }
                else
                {
                    tmpPacketRead = y;
                    break;
                }
            }
            // fill incomplete packet
            else if(status == 1)
            {
                // TODO
                //byte y = 0;
                //for(; y < data[dataItr+2] && y+dataItr+3 < lenght; ++y)
                    //packetData[y] = data[y+dataItr+3];
            }
        }
        while(!packetBuffer.isEmpty())
        {
            packetHandler.HandlePacket(packetBuffer.get(0));
            packetBuffer.remove(0);
        }
    }
    
    private byte status; // 0 waiting for packet, 1 receiving packet
    private Packet tmpPacket;
    private List<Packet> packetBuffer;
    private byte tmpPacketRead;
    private byte[] bytesToNext;
    private byte bytesToNextItr;
    private PacketHandler packetHandler;
};