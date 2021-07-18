package com.github.malow.malowlib.network;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
  private long lastActivity = 0;

  public NetworkChannel(Socket socket)
  {
    this.socket = socket;
    this.lastActivity = System.currentTimeMillis();
  }

  public NetworkChannel(String ip, int port)
  {
    try
    {
      this.socket = new Socket(ip, port);
      this.lastActivity = System.currentTimeMillis();
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

  public long getTimeSinceLastActivityMs()
  {
    return System.currentTimeMillis() - this.lastActivity;
  }

  public void sendMessage(Byteable message)
  {
    this.lastActivity = System.currentTimeMillis();
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

  private Integer remainingPacketSize = null;
  private ByteBuffer incomingPacket = null;

  public Optional<Byteable> getMessage()
  {
    try
    {
      int available = this.socket.getInputStream().available();
      if (this.remainingPacketSize == null)
      {
        if (available < 4)
        {
          return Optional.empty();
        }
        this.lastActivity = System.currentTimeMillis();
        byte[] buffer = new byte[4];
        this.socket.getInputStream().read(buffer);
        this.remainingPacketSize = ByteBuffer.wrap(buffer).getInt();
        this.incomingPacket = ByteBuffer.allocate(this.remainingPacketSize);
        available -= 4;
      }
      if (available >= 0)
      {
        this.lastActivity = System.currentTimeMillis();
        int read = this.socket.getInputStream().read(this.incomingPacket.array(), this.incomingPacket.position(),
            Math.min(available, this.remainingPacketSize));
        this.incomingPacket.position(read + this.incomingPacket.position());
        this.remainingPacketSize -= read;
        if (this.remainingPacketSize == 0)
        {
          Optional<Byteable> message = Optional.of(this.createPacket(this.incomingPacket));
          this.remainingPacketSize = null;
          return message;
        }
        else if (this.remainingPacketSize > 0)
        {
          return Optional.empty();
        }
        else
        {
          MaloWLogger.error("NetworkChannel had a remainingPacketSize of negative value: " + this.remainingPacketSize);
        }
      }
    }
    catch (SocketTimeoutException e)
    {
      MaloWLogger.error("Got a SocketTimeoutException in NetworkChannel: " + this.id
          + ", THIS SHOULD NOT HAPPEN, there's a problem with avaialable() returning bad data.", e);
      this.close();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error receiving message in Channel, closing: " + this.id, e);
      this.close();
    }
    return Optional.empty();
  }

  protected abstract Byteable createPacket(ByteBuffer bb) throws Exception;

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

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (this.id ^ this.id >>> 32);
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (this.getClass() != obj.getClass())
    {
      return false;
    }
    NetworkChannel other = (NetworkChannel) obj;
    if (this.id != other.id)
    {
      return false;
    }
    return true;
  }
}
