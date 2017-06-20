package com.github.malow.malowlib;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;
import com.github.malow.malowlib.network.message.MessageNetworkChannel;
import com.github.malow.malowlib.network.message.NetworkMessage;

public class RequestResponseClient extends MaloWProcess
{
  private String ip;
  private int port;
  private MessageNetworkChannel nc;

  private String response = null;

  public RequestResponseClient(String ip, int port)
  {
    this.ip = ip;
    this.port = port;
    this.start();
  }

  public static class ConnectionBrokenException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }

  public String sendAndReceive(String msg) throws ConnectionBrokenException
  {
    if (!this.isAlive())
    {
      throw new ConnectionBrokenException();
    }

    this.nc.sendMessage(msg);

    while (this.response == null)
    {
      try
      {
        Thread.sleep(10);
      }
      catch (InterruptedException e)
      {
        MaloWLogger.error("Failed to sleep", e);
      }
    }
    String resp = this.response;
    this.response = null;
    return resp;
  }

  public boolean isAlive()
  {
    if (this.nc == null)
    {
      return false;
    }
    return this.nc.getState() == ProcessState.RUNNING;
  }

  @Override
  public void life()
  {
    this.nc = new MessageNetworkChannel(this.ip, this.port);
    this.nc.setNotifier(this);
    this.nc.start();

    while (this.stayAlive)
    {
      ProcessEvent ev = this.waitEvent();
      if (ev instanceof NetworkMessage)
      {
        this.response = ((NetworkMessage) ev).getMessage();
      }
    }

    this.nc.close();
    this.nc.waitUntillDone();
    this.nc = null;
  }

  @Override
  public void closeSpecific()
  {

  }
}
