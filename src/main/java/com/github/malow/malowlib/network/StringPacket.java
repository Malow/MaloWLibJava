package com.github.malow.malowlib.network;

import com.github.malow.malowlib.byteconversion.ByteReader;
import com.github.malow.malowlib.byteconversion.ByteWriter;
import com.github.malow.malowlib.byteconversion.Byteable;

public class StringPacket extends Byteable
{
  public static StringPacket create(String message)
  {
    StringPacket packet = new StringPacket();
    packet.message = message;
    return packet;
  }

  public StringPacket()
  {
  }

  public String message = "";

  @Override
  protected void readFromBytes(ByteReader byteReader) throws Exception
  {
    this.message = new String(byteReader.getRaw());
  }

  @Override
  protected void writeToBytes(ByteWriter byteWriter) throws Exception
  {
    byteWriter.writeBytes(this.message.getBytes());
  }

}
