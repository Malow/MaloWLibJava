package com.github.malow.malowlib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class NetworkServer extends Process
{
  private ServerSocket sock = null;

  public NetworkServer(int port)
  {
    try
    {
      this.sock = new ServerSocket(port);
    }
    catch (IOException e)
    {
      this.stayAlive = false;
      System.out.println("Invalid socket, failed to create socket in Server.");
    }
  }

  public NetworkChannel ListenForNewClients()
  {
    NetworkChannel nc = null;

    Socket s = null;
    try
    {
      s = this.sock.accept();
    }
    catch (IOException e)
    {
      System.out.println("Failed to Listen for new connections.");
    }

    if (!this.stayAlive) return nc;

    if (s != null) nc = new NetworkChannel(s);

    return nc;
  }

  @Override
  public void Life()
  {
    while (this.stayAlive)
    {
      NetworkChannel nc = this.ListenForNewClients();
      if (nc != null && this.stayAlive) this.ClientConnected(nc);
    }
  }

  public abstract void ClientConnected(NetworkChannel nc);

  @Override
  public void CloseSpecific()
  {
    this.stayAlive = false;
    try
    {
      this.sock.close();
    }
    catch (IOException e)
    {
      System.out.println("Failed to close socket in Server.");
    }

    this.WaitUntillDone();
  }
}
