package com.github.malow.malowlib.network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Optional;

public class StringNetworkChannel extends NetworkChannel
{
  public StringNetworkChannel(String ip, int port, int readTimeoutMs)
  {
    super(ip, port, readTimeoutMs);
  }

  public StringNetworkChannel(Socket socket)
  {
    super(socket);
  }

  public void send(String message) throws NetworkChannelClosedException
  {
    this.sendBytes(message.getBytes());
  }

  public Optional<String> receive() throws NetworkChannelClosedException
  {
    ByteBuffer bb = this.receiveBytes();
    if (bb == null)
    {
      return Optional.empty();
    }
    return Optional.of(new String(bb.array()));
  }
}
