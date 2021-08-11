package com.github.malow.malowlib.network;

import java.nio.ByteBuffer;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.byteconversion.ByteReader;

public abstract class NetworkPacket
{
  protected abstract void readFromBytes(ByteReader byteReader) throws Exception;

  protected static <T extends NetworkPacket> T create(ByteReader byteReader, Class<T> targetClass)
  {
    try
    {
      T obj = targetClass.getDeclaredConstructor().newInstance();
      obj.readFromBytes(byteReader);
      return obj;
    }
    catch (NoSuchMethodException e)
    {
      MaloWLogger.error("Missing a public default constructor for class '" + targetClass.getName() + "', it is required.");
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error while trying to create an object of '" + targetClass.getName(), e);
    }
    return null;
  }

  public static <T extends NetworkPacket> T create(ByteBuffer bb, Class<T> targetClass)
  {
    ByteReader byteReader = new ByteReader(bb);
    return create(byteReader, targetClass);
  }
}
