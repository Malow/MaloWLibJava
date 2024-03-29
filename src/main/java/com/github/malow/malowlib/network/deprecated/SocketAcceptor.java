package com.github.malow.malowlib.network.deprecated;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

/**
 * Listens and accepts new TCP connections at specified port and sends ClientConnectedEvents to the specified notifier when a client connects.
 *
 */
@Deprecated
public abstract class SocketAcceptor extends MaloWProcess
{
  private MaloWProcess notifier;
  private ServerSocket serverSocket = null;

  public SocketAcceptor(int port, MaloWProcess notifier)
  {
    this.notifier = notifier;
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

  private ThreadedNetworkChannel listenForNewClient()
  {
    try
    {
      Socket socket = this.serverSocket.accept();
      if (socket != null)
      {
        return this.createNetworkChannel(socket);
      }
    }
    catch (IOException e)
    {
      if (this.stayAlive)
      {
        MaloWLogger.error("Failed to Listen for new connections.", e);
      }
    }
    return null;
  }

  protected abstract ThreadedNetworkChannel createNetworkChannel(Socket socket);

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      ThreadedNetworkChannel nc = this.listenForNewClient();
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
      MaloWLogger.error("Failed to close socket in Server.", e);
    }
  }
}
