package com.github.malow.malowlib.network;

import com.github.malow.malowlib.byteconversion.ByteWriter;

public class NetworkData
{
  private byte[] bytes;

  public NetworkData(ByteWriter byteWriter)
  {
    this.bytes = byteWriter.toByteArray();
  }

  public NetworkData(byte[] bytes)
  {
    this.bytes = bytes;
  }

  public byte[] getBytes()
  {
    return this.bytes;
  }
}
