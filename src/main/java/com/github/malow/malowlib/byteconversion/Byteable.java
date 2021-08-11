package com.github.malow.malowlib.byteconversion;

import java.nio.ByteBuffer;

import com.github.malow.malowlib.MaloWLogger;

/**
 * A simple class to inherit to make something serializeable into and from bytes, using no fluff or padding.
 *
 * int = 4 bytes
 * double = 8 bytes
 * boolean = 1 byte, as a number, 0 = false, 1 = true
 * String = 4 bytes as an Int for length and then bytes for the content
 * List = 4 bytes as an Int for length and then the elements one by one
 * UUID = 2 8-byte Longs, with the most significant bytes first
 */
public abstract class Byteable
{
  protected Byteable()
  {

  }

  protected static <T extends Byteable> T create(ByteReader byteReader, Class<T> targetClass) throws Exception
  {
    try
    {
      T obj = targetClass.getDeclaredConstructor().newInstance();
      obj.readFromBytes(byteReader);
      return obj;
    }
    catch (NoSuchMethodException e)
    {
      MaloWLogger.error("Missing a default constructor for class '" + targetClass.getName() + "', it is required.");
      return null;
    }
  }

  public static <T extends Byteable> T create(ByteBuffer bb, Class<T> targetClass) throws Exception
  {
    ByteReader byteReader = new ByteReader(bb);
    return create(byteReader, targetClass);
  }

  protected abstract void readFromBytes(ByteReader byteReader) throws Exception;

  public byte[] toByteArray() throws Exception
  {
    ByteWriter byteWriter = new ByteWriter();
    this.writeToBytes(byteWriter);
    return byteWriter.toByteArray();
  }

  protected abstract void writeToBytes(ByteWriter byteWriter) throws Exception;
}

