package com.github.malow.malowlib.network.tcpproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class LocalProxySocketListener extends MaloWProcess
{
  private List<ProxyInstance> clients = new ArrayList<>();
  private String remoteUrl;
  private int remotePort;
  private ServerSocket serverSocket;

  public LocalProxySocketListener(int localPort, String remoteUrl, int remotePort)
  {
    this.remoteUrl = remoteUrl;
    this.remotePort = remotePort;
    try
    {
      this.serverSocket = new ServerSocket(localPort);
    }
    catch (IOException e)
    {
      this.stayAlive = false;
      MaloWLogger.error("Invalid socket, failed to create socket.", e);
    }
  }

  private Socket listenForNewClient()
  {
    try
    {
      return this.serverSocket.accept();
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

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      Socket localSocket = this.listenForNewClient();
      if (localSocket != null && this.stayAlive)
      {
        try
        {
          Socket remoteSocket = new Socket(this.remoteUrl, this.remotePort);
          ProxyInstance handler = new ProxyInstance(localSocket, remoteSocket);
          this.clients.add(handler);
        }
        catch (Exception e)
        {
          MaloWLogger.error("Failed to connect to remoteSocket.", e);
        }
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
      MaloWLogger.error("Failed to close socket.", e);
    }
    this.clients.forEach(h -> h.closeAndWaitForCompletion());
  }
}
