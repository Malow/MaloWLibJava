package com.github.malow.malowlib.database;

import java.util.HashMap;
import java.util.Map;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.database.DatabaseExceptions.SimultaneousModificationException;
import com.github.malow.malowlib.database.DatabaseExceptions.TimeoutException;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunction;

public class Database
{
  public static class AccessorsSingleton
  {
    private static Map<Class<?>, Accessor<?>> accessors = new HashMap<>();

    public static <T extends DatabaseTableEntity> void register(Accessor<T> accessor) throws ClassNotFoundException
    {
      accessors.put(accessor.getEntityClass(), accessor);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DatabaseTableEntity> Accessor<T> getForClass(Class<T> clazz)
    {
      Accessor<T> accessor = (Accessor<T>) accessors.get(clazz);
      if (accessor == null)
      {
        MaloWLogger.error("Accessor for class " + clazz.getSimpleName() + " has not been registered.", new Exception());
      }
      return accessor;
    }
  }

  private static final int TIMEOUT_MS = 100;

  public static <T> void executeWithRetries(CheckedFunction f) throws Exception
  {
    long millies = System.currentTimeMillis();
    while (true)
    {
      try
      {
        if (System.currentTimeMillis() > millies + TIMEOUT_MS)
        {
          throw new TimeoutException();
        }
        f.apply();
        return;
      }
      catch (SimultaneousModificationException e)
      {
      }
    }
  }
}
