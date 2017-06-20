package com.github.malow.malowlib.network.tcpproxy;

import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class ProxyInstance extends MaloWProcess
{
  private volatile Socket localSocket;
  private volatile Socket remoteSocket;

  private volatile AtomicInteger threadSplitter = new AtomicInteger();
  private static final int MAX_PACKAGE_SIZE = 1024;

  public ProxyInstance(Socket localSocket, Socket remoteSocket)
  {
    super(2);
    this.localSocket = localSocket;
    this.remoteSocket = remoteSocket;
    this.start();
  }

  private int read(byte[] bytes, Socket socket) throws Exception
  {
    int retCode = socket.getInputStream().read(bytes);
    if (retCode > 0)
    {
      return retCode;
    }
    else
    {
      throw new Exception("Read failed, retCode: " + retCode);
    }
  }

  protected void send(byte[] bytes, Socket socket) throws Exception
  {
    socket.getOutputStream().write(bytes);
  }


  private void doLocalSocket() throws Exception
  {
    byte[] bytes = new byte[MAX_PACKAGE_SIZE];
    while (this.stayAlive)
    {
      int bytesRead = this.read(bytes, this.localSocket);
      byte[] bytesToSend = Arrays.copyOf(bytes, bytesRead);
      this.send(bytesToSend, this.remoteSocket);
      this.onReceivedFromLocalSocket(bytesToSend);
    }
  }

  protected void onReceivedFromLocalSocket(byte[] bytes)
  {
    MaloWLogger.info("Recived from Local: " + Arrays.toString(bytes));
    //new String(bytesToSend, "UTF-8"));
  }

  private void doRemoteSocket() throws Exception
  {
    byte[] bytes = new byte[MAX_PACKAGE_SIZE];
    while (this.stayAlive)
    {
      int bytesRead = this.read(bytes, this.remoteSocket);
      byte[] bytesToSend = Arrays.copyOf(bytes, bytesRead);
      this.send(bytesToSend, this.localSocket);
      this.onReceivedFromRemoteSocket(bytesToSend);
    }
  }

  protected void onReceivedFromRemoteSocket(byte[] bytes)
  {
    MaloWLogger.info("Recived from Remote: " + Arrays.toString(bytes));
    //new String(bytesToSend, "UTF-8"));
  }

  @Override
  public void life()
  {
    if (this.threadSplitter.compareAndSet(0, 1))
    {
      try
      {
        this.doLocalSocket();
      }
      catch (Exception e)
      {
        MaloWLogger.info("Local closed connection.");
        this.close();
      }
    }
    else
    {
      try
      {
        this.doRemoteSocket();
      }
      catch (Exception e)
      {
        MaloWLogger.info("Remote closed connection.");
        this.close();
      }
    }
  }
}
