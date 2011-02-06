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
            if(status == 0 && lenght-dataItr < 3)
            {
                for(byte y = 0; y+dataItr < lenght; ++y, ++bytesToNextItr)
                    bytesToNext[y] = data[dataItr+y];
                break;
            }
            
            if(status == 0 && data[dataItr] == 1 && lenght-dataItr >= 3) // handle new packet
            {
                tmpPacket = new Packet((byte)0, null, (byte)0);
                status = 1;
                byte[] packetData = null;
                if(data[dataItr+2] != 0)
                    packetData = new byte[data[dataItr+2]];
                
                byte y = 0;
                for(; y < data[dataItr+2] && y+dataItr+3 < lenght; ++y)
                    packetData[y] = data[y+dataItr+3];
                tmpPacket.set(data[dataItr+1], packetData, data[dataItr+2]);
                //complete packet
                if(y >= data[dataItr+2])
                {
                    packetBuffer.add(tmpPacket);
                    status = 0;
                    dataItr += 3+data[dataItr+2];
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

class Packet
{
    public Packet(byte opcode, byte[] data, byte lenght)
    {
        //if(data != null)
            set(opcode, data, lenght);
    }
    
    public short readByte()
    {
        if(m_readPos >= m_lenght)
            return 0;
        ++m_readPos;
        return (short)(0xFF & ((int)m_data[m_readPos-1]));
    }
    
    public int readUInt16()
    {
        if(m_readPos+1 >= m_lenght)
            return 0;
        int firstByte = (0xFF & ((int)m_data[m_readPos]));
        int secondByte = (0xFF & ((int)m_data[m_readPos+1]));
        m_readPos += 2;
        return  (firstByte << 8 | secondByte);
    }
    
    public String readString()
    {
        if(m_data[m_readPos] != 2)
            return "";
        String read = "";
        for(++m_readPos; m_readPos < m_lenght; ++m_readPos)
        {
            if(m_data[m_readPos] == 3)
            {
                ++m_readPos;
                break;
            }
            read += (char)m_data[m_readPos];
        }
        return read;
    }
    
    public byte get(byte pos) { return (pos >= m_lenght) ? 0 : m_data[pos]; }
    public void set(byte opcode, byte[] data, byte lenght)
    {
        m_opcode = opcode;
        if(data != null)
            m_data = data.clone();
        else
            m_data = null;
        m_lenght = lenght;
        m_readPos = 0;
    }
    public boolean hasData() { return (m_data != null); }
    public byte getOpcode() { return m_opcode; }
    public byte getLenght() { return m_lenght; }
    
    private byte m_opcode;
    private byte[] m_data;
    private byte m_lenght;
    private byte m_readPos;
}