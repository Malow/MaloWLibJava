package com.github.malow.malowlib.network.tpcsocketmessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.ProcessEvent;
import com.github.malow.malowlib.network.NetworkChannel;

public class MessageNetworkChannel extends NetworkChannel
{
  private PrintWriter out;
  private BufferedReader in;

  public MessageNetworkChannel(Socket socket)
  {
    super(socket);
  }

  public MessageNetworkChannel(String ip, int port)
  {
    super(ip, port);
  }

  @Override
  protected void init()
  {
    try
    {
      this.out = new PrintWriter(this.socket.getOutputStream(), true);
      this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }
    catch (IOException e)
    {
      this.close();
      MaloWLogger.error("Error initializing MessageNetworkChannel: " + this.getChannelID(), e);
    }
  }

  public void sendMessage(String msg)
  {
    this.out.print(msg + (char) 13 + (char) 10);
    this.out.flush();
  }

  @Override
  protected ProcessEvent receiveMessage()
  {
    try
    {
      StringBuffer msg = new StringBuffer();
      int i = this.in.read();
      while (i != -1)
      {
        if (i == 13)
        {
          int b = this.in.read();
          if (b == 10)
          {
            return this.createEvent(msg.toString());
          }
          char c = (char) i;
          msg.append(c);
          i = b;
        }
        else
        {
          char c = (char) i;
          msg.append(c);
          i = this.in.read();
        }
      }
    }
    catch (IOException e)
    {
      if (this.stayAlive)
      {
        this.close();
      }
    }
    return null;
  }

  protected ProcessEvent createEvent(String msg)
  {
    return new NetworkMessage(msg.toString(), this);
  }
}
