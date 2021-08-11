package com.github.malow.malowlib.byteconversion;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import com.github.malow.malowlib.MaloWUtils;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunction;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithReturn;

public class ByteWriter
{
  private ByteArrayOutputStream baos = new ByteArrayOutputStream(512);

  public byte[] toByteArray()
  {
    return this.logException(() -> this.baos.toByteArray());
  }

  public void writeInt(int i)
  {
    this.logException(() -> this.baos.write(ByteConverter.fromInt(i)));
  }

  public void writeDouble(double d)
  {
    this.logException(() -> this.baos.write(ByteConverter.fromDouble(d)));
  }

  public void writeString(String s)
  {
    this.logException(() ->
    {
      if (s == null)
      {
        this.writeInt(0);
        return;
      }
      this.writeInt(s.length());
      this.baos.write(s.getBytes());
    });
  }

  public void writeBoolean(boolean b)
  {
    this.logException(() -> this.baos.write((byte) (b ? 1 : 0)));
  }

  public void writeBytes(byte[] bytes)
  {
    this.logException(() -> this.baos.write(bytes));
  }

  public void writeByteable(Byteable b)
  {
    this.logException(() -> b.writeToBytes(this));
  }

  public void writeEnum(Enum<?> e)
  {
    this.writeInt(e.ordinal());
  }

  public void writeLong(long l)
  {
    this.logException(() -> this.baos.write(ByteConverter.fromLong(l)));
  }

  public void writeUuid(UUID uuid)
  {
    this.writeLong(uuid.getMostSignificantBits());
    this.writeLong(uuid.getLeastSignificantBits());
  }

  public <T extends Byteable> void writeList(List<T> list)
  {
    this.logException(() ->
    {
      this.baos.write(ByteConverter.fromInt(list.size()));
      for (T element : list)
      {
        element.writeToBytes(this);
      }
    });
  }

  private void logException(CheckedFunction f)
  {
    MaloWUtils.logException("Exception in ByteWriter", () ->
    {
      f.apply();
    });
  }

  private <T> T logException(CheckedFunctionWithReturn<T> f)
  {
    return MaloWUtils.logException("Exception in ByteWriter", () ->
    {
      return f.apply();
    });
  }
}