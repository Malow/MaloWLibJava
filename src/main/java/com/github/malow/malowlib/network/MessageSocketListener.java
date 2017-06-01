package com.github.malow.malowlib.network;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class MessageSocketListener extends SocketListener
{
  public MessageSocketListener(int port, MaloWProcess notifier)
  {
    super(port, notifier);
  }

  @Override
  protected NetworkChannel createNetworkChannel(Socket socket)
  {
    return new MessageNetworkChannel(socket);
  }
}
