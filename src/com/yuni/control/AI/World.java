package com.yuni.control.AI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class World
{
    public World()
    {
        objectList = new ArrayList<WorldObject>();
    }
    public static World getInstance()
    {
        if(instance == null)
            instance = new World();
        return instance;
    }
    public static void destroy()
    {
        instance = null;
    }
    
    public void AddObject(WorldObject object) { objectList.add(object); }
    public void RemoveObject(WorldObject object) { objectList.remove(object); }
    
    public void Initiate()
    {
        //TODO implement
    }
    
    public void Update(int diff)
    {
        for(Iterator<WorldObject> i = objectList.iterator(); i.hasNext();)
        {
            i.next().Update(diff);
        }
    }
    
    List<WorldObject> objectList;
    private static World instance = null;
}