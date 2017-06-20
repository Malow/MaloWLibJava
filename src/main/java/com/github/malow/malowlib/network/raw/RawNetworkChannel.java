package com.github.malow.malowlib.network.raw;

import java.io.IOException;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.ProcessEvent;
import com.github.malow.malowlib.network.NetworkChannel;

public class RawNetworkChannel extends NetworkChannel
{
  public RawNetworkChannel(String ip, int port)
  {
    super(ip, port);
  }

  public RawNetworkChannel(Socket socket)
  {
    super(socket);
  }

  public void sendRawData(byte[] bytes)
  {
    try
    {
      this.socket.getOutputStream().write(bytes);
    }
    catch (IOException e)
    {
      this.close();
      MaloWLogger.error("Error sending data. Channel: " + this.getChannelID(), e);
    }
  }

  @Override
  protected ProcessEvent receiveMessage()
  {
    byte[] bufs = new byte[1024];
    int retCode = 0;
    try
    {
      retCode = this.socket.getInputStream().read(bufs);
    }
    catch (Exception e)
    {
      this.close();
      return null;
    }
    if (retCode < 1)
    {
      this.close();
      return null;
    }
    return this.createEvent(bufs);
  }

  protected ProcessEvent createEvent(byte[] bytes)
  {
    return new RawNetworkPacket(bytes, this);
  }
}
