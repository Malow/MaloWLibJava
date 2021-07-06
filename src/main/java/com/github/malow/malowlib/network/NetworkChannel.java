package com.github.malow.malowlib.network;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Optional;

import com.github.malow.malowlib.MaloWLogger;

public abstract class NetworkChannel
{
  private static long nextID = 0;

  private static synchronized long getAndIncrementId()
  {
    return nextID++;
  }

  private long id = getAndIncrementId();

  public long getChannelID()
  {
    return this.id;
  }

  private Socket socket = null;

  public NetworkChannel(Socket socket)
  {
    this.socket = socket;
  }

  public NetworkChannel(String ip, int port)
  {
    try
    {
      this.socket = new Socket(ip, port);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id, e);
      this.close();
    }
  }

  public boolean isConnected()
  {
    if (this.socket == null)
    {
      return false;
    }
    if (this.socket.isClosed())
    {
      return false;
    }
    return true;
  }

  public void sendMessage(Byteable message)
  {
    try
    {
      byte[] bytes = message.toByteArray();
      this.socket.getOutputStream().write(ByteConverter.fromInt(bytes.length));
      this.socket.getOutputStream().write(bytes);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error sending message in Channel, closing: " + this.id, e);
      this.close();
    }
  }

  private Integer incomingPacketSize = null;

  public Optional<Byteable> getMessage()
  {
    try
    {
      if (this.incomingPacketSize == null)
      {
        if (this.socket.getInputStream().available() < 4)
        {
          return Optional.empty();
        }
        byte[] buffer = new byte[4];
        this.socket.getInputStream().read(buffer);
        this.incomingPacketSize = ByteBuffer.wrap(buffer).getInt();
      }
      if (this.socket.getInputStream().available() >= this.incomingPacketSize)
      {
        byte[] buffer = new byte[this.incomingPacketSize];
        this.socket.getInputStream().read(buffer);
        Optional<Byteable> message = Optional.of(this.createPacket(ByteBuffer.wrap(buffer)));
        this.incomingPacketSize = null;
        return message;
      }
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error receiving message in Channel, closing: " + this.id, e);
      this.close();
    }
    return Optional.empty();
  }

  @Deprecated /* NYI */
  public Object waitMessage()
  {
    return null;
  }

  protected abstract Byteable createPacket(ByteBuffer bb);

  public void close()
  {
    try
    {
      if (this.socket != null)
      {
        this.socket.close();
        this.socket = null;
      }
    }
    catch (IOException e)
    {
      MaloWLogger.error("Error closing Channel: " + this.id, e);
    }
  }
}
