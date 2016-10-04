package com.github.malow.malowlib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
      System.out.println("Invalid socket, failed to create socket in Server.");
    }
  }

  public NetworkChannel listenForNewClients()
  {
    try
    {
      Socket socket = this.serverSocket.accept();
      if (socket != null) return new NetworkChannel(socket);
    }
    catch (IOException e)
    {
      System.out.println("Failed to Listen for new connections.");
    }
    return null;
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      NetworkChannel nc = this.listenForNewClients();
      if (nc != null && this.stayAlive) this.clientConnected(nc);
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
      System.out.println("Failed to close socket in Server.");
    }

    this.waitUntillDone();
  }
}
