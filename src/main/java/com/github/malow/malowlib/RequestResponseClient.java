package com.github.malow.malowlib;

import com.github.malow.malowlib.network.NetworkChannel;
import com.github.malow.malowlib.network.NetworkPacket;

public class RequestResponseClient extends MaloWProcess
{
  private String ip;
  private int port;
  private NetworkChannel nc;

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
    if (!this.isAlive()) { throw new ConnectionBrokenException(); }

    this.nc.sendData(msg);

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
    if (this.nc == null) return false;
    return this.nc.getState() == ProcessState.RUNNING;
  }

  @Override
  public void life()
  {
    nc = new NetworkChannel(ip, port);
    nc.setNotifier(this);
    nc.start();

    while (this.stayAlive)
    {
      ProcessEvent ev = this.waitEvent();
      if (ev instanceof NetworkPacket)
      {
        this.response = ((NetworkPacket) ev).getMessage();
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
