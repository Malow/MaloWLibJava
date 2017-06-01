package com.github.malow.malowlib.network;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class RawNetworkPacket extends ProcessEvent
{
  public byte[] bytes;
  public RawNetworkChannel from;

  public RawNetworkPacket(byte[] bytes, RawNetworkChannel from)
  {
    this.bytes = bytes;
    this.from = from;
  }

  @Override
  public String toString()
  {
    try
    {
      return new String(this.bytes, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      MaloWLogger.error("Failed to convert bytes to string", e);
      return Arrays.toString(this.bytes);
    }
  }
}
