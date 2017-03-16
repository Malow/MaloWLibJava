package com.github.malow.malowlib.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.MaloWProcess;

public abstract class NetworkServer extends MaloWProcess
{
  private ServerSocket serverSocket = null;

  public NetworkServer(int port)
  {
    try
    {
      this.serverSocket = new ServerSocket(port);
    }
    catch (IOException e)
    {
      this.stayAlive = false;
      MaloWLogger.error("Invalid socket, failed to create socket in Server.", e);
    }
  }

  public NetworkChannel listenForNewClients()
  {
    try
    {
      Socket socket = this.serverSocket.accept();
      if (socket != null) return this.createNetworkChannel(socket);
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to Listen for new connections.", e);
    }
    return null;
  }

  protected NetworkChannel createNetworkChannel(Socket socket)
  {
    return new NetworkChannel(socket);
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      NetworkChannel nc = this.listenForNewClients();
      if ((nc != null) && this.stayAlive)
      {
        this.clientConnected(nc);
      }
    }
  }

  public abstract void clientConnected(NetworkChannel nc);

  @Override
  public void closeSpecific()
  {
    this.stayAlive = false;
    try
    {
      this.serverSocket.close();
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to close socket in Server.", e);
    }

    this.waitUntillDone();
  }
}
