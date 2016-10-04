package com.github.malow.malowlib;

import java.io.IOException;
import java.net.Socket;

public class NetworkChannel extends MaloWProcess
{
  private Socket socket = null;
  private MaloWProcess notifier = null;
  private String buffer = "";

  private static long nextID = 0;
  private long id;

  public NetworkChannel(Socket socket)
  {
    this.id = NetworkChannel.nextID;
    NetworkChannel.nextID++;

    this.socket = socket;
  }

  public NetworkChannel(String ip, int port)
  {
    this.id = NetworkChannel.nextID;
    NetworkChannel.nextID++;

    try
    {
      this.socket = new Socket(ip, port);
    }
    catch (Exception e)
    {
      this.close();
      System.out.println("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id);
    }
  }

  public void sendData(String msg)
  {
    char ten = 10;
    msg += ten;
    byte bufs[] = new byte[1024];
    for (int q = 0; q < 1024; q++)
      bufs[q] = 0;

    for (int i = 0; i < msg.length(); i++)
      bufs[i] = (byte) msg.charAt(i);

    try
    {
      this.socket.getOutputStream().write(bufs);
    }
    catch (IOException e1)
    {
      this.close();
      System.out.println("Error sending data. Channel: " + this.id);
    }
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      String msg = this.receiveData();
      if (msg != "")
      {
        if (this.notifier != null && this.stayAlive)
        {
          NetworkPacket np = new NetworkPacket(msg, this);
          this.notifier.putEvent(np);
        }
      }
    }
  }

  public void setNotifier(MaloWProcess notifier)
  {
    this.notifier = notifier;
  }

  public long GetChannelID()
  {
    return this.id;
  }

  @Override
  public void closeSpecific()
  {
    if (this.socket == null) return;

    try
    {
      this.socket.shutdownInput();
    }
    catch (IOException e1)
    {
      System.out.println("Error trying to perform shutdownInput on socket from a ->Close() call. Channel: " + this.id);
    }
    try
    {
      this.socket.shutdownOutput();
    }
    catch (IOException e1)
    {
      System.out.println("Error trying to perform shutdownOutput on socket from a ->Close() call. Channel: " + this.id);
    }

    try
    {
      this.socket.close();
    }
    catch (IOException e)
    {
      System.out.println("Failed to close socket in channel: " + this.id);
    }
  }

  private String receiveData()
  {
    String msg = "";

    boolean getNewData = true;
    if (!this.buffer.isEmpty())
    {
      int pos = this.buffer.indexOf(10);
      if (pos > 0)
      {
        msg = this.buffer.substring(0, pos);
        this.buffer = this.buffer.substring(pos + 1, this.buffer.length());
        getNewData = false;
      }
    }
    if (getNewData)
    {
      boolean goAgain = true;
      do
      {
        byte[] bufs = new byte[1024];
        for (int q = 0; q < 1024; q++)
          bufs[q] = 0;

        int retCode = 0;
        try
        {
          retCode = this.socket.getInputStream().read(bufs);
        }
        catch (Exception e)
        {
          this.close();
          System.out.println("Channel " + this.id + " exception when receiving, closing. " + e);
        }

        if (retCode == -1)
        {
          this.close();
          System.out.println("Error receiving data by channel: " + this.id + ". Error: " + retCode + ". Probably due to crash/improper disconnect");
        }
        else if (retCode == 0)
        {
          this.close();
          System.out.println("Channel " + this.id + " disconnected, closing.");
        }

        if (retCode > 0)
        {
          for (int i = 0; i < 1024; i++)
          {
            if (bufs[i] == 10) goAgain = false;
            if (bufs[i] != 0) this.buffer += (char) bufs[i];
            else i = 1024;
          }

          if (!goAgain)
          {
            for (int i = 0; i < 1024; i++)
            {
              if (this.buffer.charAt(i) != 10) msg += this.buffer.charAt(i);
              else
              {
                this.buffer = this.buffer.substring(i + 1, this.buffer.length());
                i = 1024;
              }
            }
          }
        }
      }
      while (goAgain && this.stayAlive);
    }

    return msg;
  }
}
