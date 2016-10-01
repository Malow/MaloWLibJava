package com.github.malow.malowlib;

import java.io.IOException;
import java.net.Socket;

public class NetworkChannel extends Process
{
  private Socket sock = null;
  private Process notifier = null;
  private String buffer = "";

  private static long nextCID = 0;
  private long id;

  public NetworkChannel(Socket socket)
  {
    this.id = NetworkChannel.nextCID;
    NetworkChannel.nextCID++;

    this.sock = socket;
  }

  public NetworkChannel(String ip, int port)
  {
    this.id = NetworkChannel.nextCID;
    NetworkChannel.nextCID++;

    try
    {
      this.sock = new Socket(ip, port);
    }
    catch (Exception e)
    {
      this.Close();
      System.out.println("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id);
    }
  }

  public void SendData(String msg)
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
      this.sock.getOutputStream().write(bufs);
    }
    catch (IOException e1)
    {
      this.Close();
      System.out.println("Error sending data. Channel: " + this.id);
    }
  }

  @Override
  public void Life()
  {
    while (this.stayAlive)
    {
      String msg = this.ReceiveData();
      if (msg != "")
      {
        if (this.notifier != null && this.stayAlive)
        {
          NetworkPacket np = new NetworkPacket(msg, this.id);
          this.notifier.PutEvent(np);
        }
      }
    }
  }

  public void SetNotifier(Process notifier)
  {
    this.notifier = notifier;
  }

  public long GetChannelID()
  {
    return this.id;
  }

  @Override
  public void CloseSpecific()
  {
    if (this.sock == null) return;

    try
    {
      this.sock.shutdownInput();
    }
    catch (IOException e1)
    {
      System.out.println("Error trying to perform shutdown on socket from a ->Close() call. Channel: " + this.id);
    }
    try
    {
      this.sock.shutdownOutput();
    }
    catch (IOException e1)
    {
      System.out.println("Error trying to perform shutdown on socket from a ->Close() call. Channel: " + this.id);
    }

    try
    {
      this.sock.close();
    }
    catch (IOException e)
    {
      System.out.println("Failed to close socket in channel: " + this.id);
    }
  }

  private String ReceiveData()
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
          retCode = this.sock.getInputStream().read(bufs);
        }
        catch (Exception e)
        {
          this.Close();
          System.out.println("Channel " + this.id + " exception when receiving, closing. " + e);
        }

        if (retCode == -1)
        {
          this.Close();
          System.out.println("Error receiving data by channel: " + this.id + ". Error: " + retCode + ". Probably due to crash/improper disconnect");
        }
        else if (retCode == 0)
        {
          this.Close();
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
