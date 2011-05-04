package com.yuni.control.AI;

class WorldObject
{
    public static final byte MOVE_NONE     = 0x00;
    public static final byte MOVE_FORWARD  = 0x01; 
    public static final byte MOVE_BACKWARD = 0x02; 
    public static final byte MOVE_LEFT     = 0x04; 
    public static final byte MOVE_RIGHT    = 0x08; 
    public static final byte MOVE_LEFT_WHEEL= 0x10; 
    public static final byte MOVE_RIGHT_WHEEL= 0x20; 
    
    public WorldObject()
    {
        m_X = m_Y = m_Z = m_o = 0.0f;    
    }
    
    public void AddToWorld()
    {
        World.getInstance().AddObject(this);
    }
    
    public void RemoveFromWorld()
    {
        World.getInstance().RemoveObject(this);
    }
    
    public void Relocate(float x, float y, float z, float o)
    {
        m_X = x;
        m_Y = y;
        m_Z = z;
        m_o = o;
    }
    
    public float GetPositionX() { return m_X; }
    public float GetPositionY() { return m_Y; }
    public float GetPositionZ() { return m_Z; }
    public float GetOrientation() { return m_o; }
    public void GetPosition(WorldLoc loc)
    {
        if(loc == null)
            return;
        loc.Set(m_X, m_Y, m_Z, m_o);
    }
    
    public byte GetType() { return m_type; }
    
    public void Update(int diff)
    {
        // TODO implement
    }
    
    
    private float m_X;
    private float m_Y;
    private float m_Z;
    private float m_o;
    protected byte m_type;
}

class WorldLoc
{
    public void Set(float x, float y, float z, float ori) { X = x; Y = y; Z = z; o = ori; }

    public float X;
    public float Y;
    public float Z;
    public float o;
}
