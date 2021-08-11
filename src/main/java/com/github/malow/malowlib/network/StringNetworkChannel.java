package com.github.malow.malowlib.network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Optional;

public class StringNetworkChannel extends NetworkChannel
{
  public StringNetworkChannel(String ip, int port)
  {
    super(ip, port);
  }

  public StringNetworkChannel(Socket socket)
  {
    super(socket);
  }

  public void send(String message)
  {
    this.sendBytes(message.getBytes());
  }

  public Optional<String> receive()
  {
    ByteBuffer bb = this.receiveBytes();
    if (bb == null)
    {
      return Optional.empty();
    }
    return Optional.of(new String(bb.array()));
  }
}
