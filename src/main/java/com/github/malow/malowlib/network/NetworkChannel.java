package com.github.malow.malowlib.network;

import java.io.IOException;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class NetworkChannel extends MaloWProcess
{
  private static long nextID = 0;

  private static synchronized long getAndIncrementId()
  {
    return nextID++;
  }

  private Socket socket = null;
  private MaloWProcess notifier = null;
  private String buffer = "";

  private long id;

  public NetworkChannel(Socket socket)
  {
    this.id = getAndIncrementId();
    this.socket = socket;
  }

  public NetworkChannel(String ip, int port)
  {
    this.id = getAndIncrementId();

    try
    {
      this.socket = new Socket(ip, port);
    }
    catch (Exception e)
    {
      this.close();
      MaloWLogger.error("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id, e);
    }
  }

  public void sendData(String msg)
  {
    char ten = 10;
    msg += ten;
    byte bufs[] = new byte[1024];
    for (int q = 0; q < 1024; q++)
    {
      bufs[q] = 0;
    }

    for (int i = 0; i < msg.length(); i++)
    {
      bufs[i] = (byte) msg.charAt(i);
    }

    try
    {
      this.socket.getOutputStream().write(bufs);
    }
    catch (IOException e)
    {
      this.close();
      MaloWLogger.error("Error sending data. Channel: " + this.id, e);
    }
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      String msg = this.receiveData();
      if (!msg.equals(""))
      {
        if (this.notifier != null && this.stayAlive)
        {
          this.notifier.putEvent(this.createEvent(msg));
        }
      }
    }
  }

  protected ProcessEvent createEvent(String msg)
  {
    return new NetworkPacket(msg, this);
  }

  public void setNotifier(MaloWProcess notifier)
  {
    this.notifier = notifier;
  }

  public long getChannelID()
  {
    return this.id;
  }

  @Override
  public void closeSpecific()
  {
    if (this.socket == null)
    {
      return;
    }

    try
    {
      this.socket.close();
      this.socket = null;
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to close socket in channel: " + this.id, e);
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
        {
          bufs[q] = 0;
        }

        int retCode = 0;
        try
        {
          retCode = this.socket.getInputStream().read(bufs);
        }
        catch (Exception e)
        {
          this.close();
          if (this.stayAlive)
          {
            MaloWLogger.error("Channel " + this.id + " exception when receiving, closing. " + e, e);
          }
        }

        if (retCode == -1)
        {
          this.close();
          if (this.stayAlive)
          {
            MaloWLogger
                .warning("Error receiving data by channel: " + this.id + ". Error: " + retCode + ". Probably due to crash/improper disconnect");
          }

        }
        else if (retCode == 0)
        {
          this.close();
          if (this.stayAlive)
          {
            MaloWLogger.warning("Channel " + this.id + " disconnected, closing.");
          }
        }

        if (retCode > 0)
        {
          for (int i = 0; i < 1024; i++)
          {
            if (bufs[i] == 10)
            {
              goAgain = false;
            }
            if (bufs[i] != 0)
            {
              this.buffer += (char) bufs[i];
            }
            else
            {
              i = 1024;
            }
          }

          if (!goAgain)
          {
            for (int i = 0; i < 1024; i++)
            {
              if (this.buffer.charAt(i) != 10)
              {
                msg += this.buffer.charAt(i);
              }
              else
              {
                this.buffer = this.buffer.substring(i + 1, this.buffer.length());
                i = 1024;
              }
            }
          }
        }
      } while (goAgain && this.stayAlive);
    }
    return msg;
  }
}
