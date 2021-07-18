package com.github.malow.malowlib.byteconversion;

import java.nio.ByteBuffer;

public class ByteConverter
{
  public static byte[] fromInt(int i)
  {
    byte[] b = new byte[4];
    ByteBuffer.wrap(b).putInt(i);
    return b;
  }

  public static byte[] fromDouble(double d)
  {
    byte[] b = new byte[8];
    ByteBuffer.wrap(b).putDouble(d);
    return b;
  }

  public static byte[] fromLong(long l)
  {
    byte[] b = new byte[8];
    ByteBuffer.wrap(b).putLong(l);
    return b;
  }
}
