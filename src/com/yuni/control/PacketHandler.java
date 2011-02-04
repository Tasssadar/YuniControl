package com.yuni.control;

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
		String log = "";
		switch(packet.getOpcode())
		{
		    case Protocol.CMSG_PONG:
		    	msg.what = YuniControl.MESSAGE_LOG;
				log = "CMSG_PONG received";
				msg.obj = log;
				m_handler.sendMessage(msg);
			    break;
		    case Protocol.CMSG_GET_RANGE_VAL:
		    	msg.what = YuniControl.MESSAGE_LOG;
				log = "CMSG_GET_RANGE_VAL received, adress was " + packet.readByte() + " and range is " + packet.readUInt16();
				msg.obj = log;
				m_handler.sendMessage(msg);
			    break;
			default:
				msg.what = YuniControl.MESSAGE_LOG;
				log = "Packet with opcode " + packet.getOpcode() + " and lenght " + packet.getLenght() + " recieved";
				msg.obj = log;
				m_handler.sendMessage(msg);
				break;
		}
	}
	
	private Handler m_handler;
}