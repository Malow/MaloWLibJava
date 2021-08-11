package com.github.malow.malowlib.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

/**
 * Listens and accepts new TCP connections at specified port and sends ClientConnectedEvents to the specified notifier when a client connects.
 */
public abstract class SocketAcceptor<T extends NetworkChannel> extends MaloWProcess
{
  private MaloWProcess notifier;
  private ServerSocket serverSocket = null;
  private int clientReadTimeoutMs = 0;

  public SocketAcceptor(int port, MaloWProcess notifier, int clientReadTimeoutMs)
  {
    this.notifier = notifier;
    this.clientReadTimeoutMs = clientReadTimeoutMs;
    try
    {
      this.serverSocket = new ServerSocket(port);
    }
    catch (IOException e)
    {
      this.stayAlive = false;
      MaloWLogger.error("Invalid socket, failed to create socket in SocketAcceptor.", e);
    }
  }

  public SocketAcceptor(int port, MaloWProcess notifier)
  {
    this(port, notifier, 0);
  }

  private T listenForNewClient()
  {
    try
    {
      Socket socket = this.serverSocket.accept();
      if (socket != null)
      {
        if (this.clientReadTimeoutMs > 0)
        {
          socket.setSoTimeout(this.clientReadTimeoutMs);
        }
        return this.createNetworkChannel(socket);
      }
    }
    catch (IOException e)
    {
      if (this.stayAlive)
      {
        MaloWLogger.error("Failed to Listen for new connections in SocketAcceptor.", e);
      }
    }
    return null;
  }

  protected abstract T createNetworkChannel(Socket socket);

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      T nc = this.listenForNewClient();
      if (nc != null && this.stayAlive)
      {
        this.notifier.putEvent(new ClientConnectedEvent(nc));
      }
    }
  }

  @Override
  public void closeSpecific()
  {
    try
    {
      this.serverSocket.close();
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to close socket in SocketAcceptor.", e);
    }
  }
}