package com.github.malow.malowlib.network;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.malow.malowlib.MaloWLogger;

/**
 * A simple class to inherit to make something serializeable into and from bytes, using no fluff or padding.
 * Overwrite setValues and getBytes to increase efficiency since those use reflection to be able to do it automatically.
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
      if (s == null)
      {
        this.writeInt(0);
        return;
      }
      this.writeInt(s.length());
      this.baos.write(s.getBytes());
    }

    public void writeBoolean(boolean b) throws Exception
    {
      this.baos.write((byte) (b ? 1 : 0));
    }

    public void writeBytes(byte[] bytes) throws Exception
    {
      this.baos.write(bytes);
    }

    public void writeByteable(Byteable b) throws Exception
    {
      b.writeToBytes(this);
    }

    public void writeEnum(Enum<?> e) throws Exception
    {
      this.writeInt(e.ordinal());
    }

    public void writeLong(long l) throws Exception
    {
      this.baos.write(ByteConverter.fromLong(l));
    }

    public void writeUuid(UUID uuid) throws Exception
    {
      this.writeLong(uuid.getMostSignificantBits());
      this.writeLong(uuid.getLeastSignificantBits());
    }

    public <T extends Byteable> void writeList(List<T> list) throws Exception
    {
      this.baos.write(ByteConverter.fromInt(list.size()));
      for (T element : list)
      {
        element.writeToBytes(this);
      }
    }
  }

  public static class ByteReader
  {
    private ByteBuffer bb;

    protected ByteReader(ByteBuffer bb)
    {
      this.bb = bb;
    }

    public byte[] getRaw()
    {
      return this.bb.array();
    }

    public String readString()
    {
      int length = this.bb.getInt();
      if (length == 0)
      {
        return null;
      }
      byte[] sb = new byte[length];
      this.bb.get(sb);
      return new String(sb);
    }

    public boolean readBoolean()
    {
      byte b = this.bb.get();
      return b == 1;
    }

    public <T extends Enum<?>> T readEnum(Class<T> enumClass)
    {
      return enumClass.getEnumConstants()[this.bb.getInt()];
    }

    public <T extends Byteable> List<T> readList(Class<T> targetClass) throws Exception
    {
      List<T> list = new ArrayList<>();
      int count = this.bb.getInt();
      for (int i = 0; i < count; i++)
      {
        list.add(create(this.bb, targetClass));
      }
      return list;
    }

    public int readInt()
    {
      return this.bb.getInt();
    }

    public double readDouble()
    {
      return this.bb.getDouble();
    }

    public <T extends Byteable> T readByteable(Class<T> targetClass) throws Exception
    {
      return create(this, targetClass);
    }

    public UUID readUuid()
    {
      return new UUID(this.bb.getLong(), this.bb.getLong());
    }
  }

  public Byteable()
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

