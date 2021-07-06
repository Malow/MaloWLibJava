package com.github.malow.malowlib.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * A simple class to inherit to make something serializeable into and from bytes, using no fluff or padding.
 * Overwrite setValues and getBytes to increase efficiency since those use reflection to be able to do it automatically.
 *
 * int = 4 bytes
 * double = 8 bytes
 * boolean = 1 byte, as a number, 0 = false, 1 = true
 * String = 4 bytes as an int and then bytes for the content
 */
public abstract class Byteable
{
  public static class ByteWriter
  {
    private ByteArrayOutputStream baos = new ByteArrayOutputStream(512);

    protected byte[] toByteArray() throws Exception
    {
      this.baos.flush();
      return this.baos.toByteArray();
    }

    public void writeInt(int i) throws Exception
    {
      this.baos.write(ByteConverter.fromInt(i));
    }

    public void writeDouble(double d) throws Exception
    {
      this.baos.write(ByteConverter.fromDouble(d));
    }

    public void writeString(String s) throws Exception
    {
      this.writeInt(s.length());
      this.baos.write(s.getBytes());
    }

    public void writeBoolean(boolean b) throws Exception
    {
      this.baos.write((byte) (b ? 1 : 0));
    }
  }

  public static <T extends Byteable> T create(ByteBuffer bb, Class<T> targetClass) throws Exception
  {
    T obj = targetClass.getDeclaredConstructor().newInstance();
    obj.readFromBytes(bb);
    return obj;
  }

  protected abstract void readFromBytes(ByteBuffer bb) throws Exception;

  public byte[] toByteArray() throws Exception
  {
    ByteWriter byteWriter = new ByteWriter();
    this.writeToBytes(byteWriter);
    return byteWriter.toByteArray();
  }

  protected abstract void writeToBytes(ByteWriter byteWriter) throws Exception;

  protected String readString(ByteBuffer bb)
  {
    int length = bb.getInt();
    byte[] sb = new byte[length];
    bb.get(sb);
    return new String(sb);
  }

  protected boolean readBoolean(ByteBuffer bb)
  {
    byte b = bb.get();
    return b == 1;
  }
}

