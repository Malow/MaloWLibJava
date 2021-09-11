package com.github.malow.malowlib.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.byteconversion.ByteConverter;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameter;
import com.github.malow.malowlib.time.ImpreciseTimer;

/**
 * Extend this and public add send and receive functions where you serialize and deserialize the data and then call the protected send and receive of this
 * class. See {@link com.github.malow.malowlib.network.StringNetworkChannel} for an example.
 */
public abstract class NetworkChannel
{
  public static class NetworkChannelClosedException extends Exception
  {

  }

  private static final int PING_TYPE_ID = Integer.MAX_VALUE;
  @SuppressWarnings("unused") // Not directly used, but the value itself is used (by modifying the incoming buffer)
  private static final int PING_RESPONSE_TYPE_ID = Integer.MAX_VALUE - 1;
  private static final int PACKET_MAX_SIZE = 1000000; // 1 MB

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
  private Integer remainingPacketSize = null;
  private ByteBuffer incomingPacket = null;
  private ImpreciseTimer pingTimer = new ImpreciseTimer();
  private boolean hasOngoingPing = false;
  private long ping = 0;

  public NetworkChannel(Socket socket)
  {
    this.socket = socket;
  }

  public NetworkChannel(String ip, int port, int readTimeoutMs)
  {
    try
    {
      this.socket = new Socket(ip, port);
      this.socket.setSoTimeout(readTimeoutMs);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id, e);
      this.close();
    }
  }

  public boolean isConnected()
  {
    if (this.socket == null || this.socket.isClosed())
    {
      return false;
    }
    return this.socket.isConnected();
  }

  protected void sendBytes(byte[] bytes) throws NetworkChannelClosedException
  {
    this.send(output ->
    {
      output.write(ByteConverter.fromInt(bytes.length));
      output.write(bytes);
    });
  }

  protected void sendBytesWithMessageTypeId(int messageTypeId, byte[] bytes) throws NetworkChannelClosedException
  {
    this.send(output ->
    {
      output.write(ByteConverter.fromInt(bytes.length + 4));
      output.write(ByteConverter.fromInt(messageTypeId));
      output.write(bytes);
    });
  }

  public void sendPingWithInterval(int intervalMs) throws NetworkChannelClosedException
  {
    if (this.hasOngoingPing || !this.pingTimer.hasElapsed(intervalMs))
    {
      return;
    }
    this.pingTimer.reset();
    this.hasOngoingPing = true;
    this.send(output ->
    {
      output.write(ByteConverter.fromInt(PING_TYPE_ID));
    });
  }

  public int getPing()
  {
    if (this.hasOngoingPing)
    {
      return (int) Math.max(this.ping, this.pingTimer.getElapsed());
    }
    return (int) this.ping;
  }

  protected void send(CheckedFunctionWithParameter<OutputStream> function) throws NetworkChannelClosedException
  {
    try
    {
      function.apply(this.socket.getOutputStream());
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error sending in Channel, closing: " + this.id, e);
      this.close();
      throw new NetworkChannelClosedException();
    }
  }

  /**
   * Non-blocking
   */
  protected ByteBuffer receiveBytes() throws NetworkChannelClosedException
  {
    try
    {
      int available = this.socket.getInputStream().available();
      if (this.remainingPacketSize == null)
      {
        if (available < 4)
        {
          return null;
        }
        byte[] buffer = new byte[4];
        this.socket.getInputStream().read(buffer);
        this.remainingPacketSize = ByteBuffer.wrap(buffer).getInt();
        if (this.remainingPacketSize == PING_TYPE_ID)
        {
          this.remainingPacketSize = null;
          buffer[3]--; // Send back PING_RESPONSE_TYPE_ID
          this.send(output -> output.write(buffer));
          return null;
        }
        if (this.remainingPacketSize == PING_RESPONSE_TYPE_ID)
        {
          this.remainingPacketSize = null;
          if (!this.hasOngoingPing)
          {
            MaloWLogger.error("Received a PING_RESPONSE_TYPE_ID but I have no ongoing ping");
            return null;
          }
          this.ping = this.pingTimer.getElapsed();
          this.hasOngoingPing = false;
          return null;
        }
        if (this.remainingPacketSize > PACKET_MAX_SIZE)
        {
          throw new Exception("Received a packet above PACKET_MAX_SIZE, size: " + this.remainingPacketSize);
        }
        this.incomingPacket = ByteBuffer.allocate(this.remainingPacketSize);
        available -= 4;
      }
      if (available > 0)
      {
        int read = this.socket.getInputStream().read(this.incomingPacket.array(), this.incomingPacket.position(),
            Math.min(available, this.remainingPacketSize));
        this.incomingPacket.position(read + this.incomingPacket.position());
        this.remainingPacketSize -= read;
        if (this.remainingPacketSize == 0)
        {
          ByteBuffer message = this.incomingPacket;
          message.position(0);
          this.remainingPacketSize = null;
          this.incomingPacket = null;
          return message;
        }
        else if (this.remainingPacketSize > 0)
        {
          return null;
        }
        else
        {
          MaloWLogger.error("NetworkChannel had a remainingPacketSize of negative value: " + this.remainingPacketSize);
          this.close();
          throw new NetworkChannelClosedException();
        }
      }
    }
    catch (SocketTimeoutException e)
    {
      MaloWLogger.error("Got a SocketTimeoutException in NetworkChannel: " + this.id
          + ", THIS SHOULD NOT HAPPEN, there's a problem with avaialable() returning bad data.", e);
      this.close();
      throw new NetworkChannelClosedException();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error receiving message in Channel, closing: " + this.id, e);
      this.close();
      throw new NetworkChannelClosedException();
    }
    return null;
  }

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
    if (obj == null || this.getClass() != obj.getClass())
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
