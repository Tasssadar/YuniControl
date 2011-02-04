package com.yuni.control;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Config
{
	public final static byte CONF_BYTE_CONNECT_METHOD = 0;
	public final static byte CONF_BOOL_SCREEN_ALWAYS_ON = 1;
	public final static byte CONF_BOOL_START_PROGRAM_AFTER_START = 2;
	public final static byte CONF_STRING_BLUETOOTH_MAC = 3;
	public final static byte CONF_BOOL_LOG_TIME = 4;
	
	public final static byte MAX_CONFIG = 5;

	public Config()
	{
		loaded = false;	
		boolVals = new boolean[50];
		byteVals = new byte[50];
		stringVals = new String[50];
	}
	
	public boolean load() throws IOException
	{
		setDefaults();
		File conf = new File("/sdcard/YuniControl/config.ini");
		if(conf.length() >= Integer.MAX_VALUE || !conf.exists() || !conf.canRead())
			return false;
		final FileInputStream file = new FileInputStream(conf);
		final byte[] fileBuff = new byte[(int) conf.length()];
        file.read(fileBuff);
        file.close();
        
		int pos = 0;
		String index;
		String val;
		boolean nextLine;
		char c;
		while(pos < conf.length())
		{
			nextLine = fileBuff[pos] == (byte)';';
			if(!nextLine)
			{
				// Get index
				index = "";
			    while(pos < conf.length())
			    {
			    	c = (char)fileBuff[pos];
			    	++pos;
			    	if(c == ' ') continue;
			    	else if(c == '=') break;
			    	index += c;
			    }
			    // Get value
			    val = "";
			    while(pos < conf.length())
			    {
			    	c = (char)fileBuff[pos];
			    	++pos;
			    	if(c == ' ') continue;
			    	else if(c == '\r' || c == '\n')
			    	{
			    		if(pos < conf.length() && (char)fileBuff[pos] == '\n')
			    			++pos;
			    		break;
			    	}
			    	val += c;
			    }
			}
			else
			{
				while(pos < conf.length())
			    {
			    	c = (char)fileBuff[pos];
			    	++pos;
			    	if(c == '\r' || c == '\n')
			    	{
			    		if(pos < conf.length() && (char)fileBuff[pos] == '\n')
			    			++pos;
			    		break;
			    	}
			    }
				continue;
			}
			// Parse index and value
			if(index.contentEquals("ConnectMethod"))
				addByteVal(CONF_BYTE_CONNECT_METHOD, val);
			else if(index.contentEquals("KeepScreenOn"))
				addBoolVal(CONF_BOOL_SCREEN_ALWAYS_ON, val);
			else if(index.contentEquals("ImmediatelyStartProgram"))
				addBoolVal(CONF_BOOL_START_PROGRAM_AFTER_START, val);
		    else if(index.contentEquals("BluetoothMacAdress"))
		    	addStringVal(CONF_STRING_BLUETOOTH_MAC, val);
		    else if(index.contentEquals("LogTime"))
		    	addBoolVal(CONF_BOOL_LOG_TIME, val);
		}
		loaded = true;
		return true;
	}
	public boolean getBool(byte index)
	{
		return boolVals[index];
	}
	
	public byte getByte(byte index)
	{
		return byteVals[index];
	}
	public boolean isLoaded()
	{
		return loaded;
	}
	
	private void setDefaults()
	{
		addByteVal(CONF_BYTE_CONNECT_METHOD, "1"); // Bluetooth
		
		addBoolVal(CONF_BOOL_SCREEN_ALWAYS_ON, "false");
		addBoolVal(CONF_BOOL_START_PROGRAM_AFTER_START, "false");
		addStringVal(CONF_STRING_BLUETOOTH_MAC, "");
		addBoolVal(CONF_BOOL_LOG_TIME, "true");
	}
	
	public void addBoolVal(byte index, String val)
	{
		boolVals[index] = Boolean.parseBoolean(val);
	}
	public void addByteVal(byte index, String val)
	{
		byteVals[index] = Byte.valueOf(val);
	}
	public void addStringVal(byte index, String val)
	{
		stringVals[index] = val;
	}
	
	private boolean[] boolVals;
	private byte[] byteVals;
	private String[] stringVals;
	private boolean loaded;
}