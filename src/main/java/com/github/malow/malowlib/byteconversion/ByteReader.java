package com.github.malow.malowlib.byteconversion;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ByteReader
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
      list.add(Byteable.create(this.bb, targetClass));
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
    return Byteable.create(this, targetClass);
  }

  public UUID readUuid()
  {
    return new UUID(this.bb.getLong(), this.bb.getLong());
  }
}
