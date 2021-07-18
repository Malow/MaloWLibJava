package com.github.malow.malowlib.byteconversion;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

public class ByteWriter
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