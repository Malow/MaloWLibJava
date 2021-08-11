package com.github.malow.malowlib.network.tcpproxy;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

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
    throw new Exception("Read failed, retCode: " + retCode);
  }

  protected void send(byte[] bytes, Socket socket) throws Exception
  {
    if (socket != null)
    {
      socket.getOutputStream().write(bytes);
    }
  }

  public void sendToRemote(byte[] bytes) throws Exception
  {
    this.send(bytes, this.remoteSocket);
    MaloWLogger.info("Sent manually to Remote: " + getHexStringFromByteArray(bytes));
  }

  private void doLocalSocket() throws Exception
  {
    byte[] bytes = new byte[MAX_PACKAGE_SIZE];
    while (this.stayAlive)
    {
      int bytesRead = this.read(bytes, this.localSocket);
      byte[] bytesToSend = Arrays.copyOf(bytes, bytesRead);
      bytesToSend = this.modifyBytes(bytesToSend);
      this.send(bytesToSend, this.remoteSocket);
      this.onReceivedFromLocalSocket(bytesToSend);
    }
  }

  private byte[] modifyBytes(byte[] bytesToSend) throws DecoderException
  {
    String s = getHexStringFromByteArray(bytesToSend);
    //s = s.replace("07 00 00 00 07 00 00 00 4D 61 6C 6F 77 00 41 41 42 42 43 43 44 44 00 ",
    //    "07 00 00 00 07 00 00 00 4D 61 6C 6F 77 00 44 44 42 42 43 43 44 44 00 ");
    //"0D 1B 04 03 33 02 10 F1 41 73 64 64 73 61 00 61 00 00 29 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ");
    if (s.contains(
        "0D 1B 04 03 33 02 10 F1 41 73 64 64 73 61 00 61 00 00 29 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 "))
    {
      s = s.replace(
          "0D 1B 04 03 33 02 10 F1 41 73 64 64 73 61 00 61 00 00 29 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ",
          "07 00 00 00 07 00 00 00 4D 61 6C 6F 77 00 44 44 42 42 43 43 44 44 00 ");
      MaloWLogger.info("replaced");
    }
    return this.toByteArray(s);
  }

  protected void onReceivedFromLocalSocket(byte[] bytes)
  {
    MaloWLogger.info("Recived from Local: " + getHexStringFromByteArray(bytes));
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
    MaloWLogger.info("Recived from Remote: " + getHexStringFromByteArray(bytes));
  }

  private static String getHexStringFromByteArray(byte[] bytes)
  {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes)
    {
      sb.append(String.format("%02X ", b));
    }
    return sb.toString();
  }

  private byte[] toByteArray(String s) throws DecoderException
  {
    s = s.replaceAll(" ", "");
    return Hex.decodeHex(s.toCharArray());
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
      }
      this.closeSocket(this.localSocket);
      this.localSocket = null;
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
      }
      this.closeSocket(this.remoteSocket);
      this.remoteSocket = null;
    }
    if (this.remoteSocket == null && this.localSocket == null)
    {
      this.close();
    }
  }

  public void closeSocket(Socket socket)
  {
    if (socket == null)
    {
      return;
    }
    try
    {
      socket.close();
      socket = null;
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to close socket", e);
    }
  }
}
