package com.github.malow.malowlib.network.message;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.NetworkChannel;
import com.github.malow.malowlib.network.SocketAcceptor;

public class MessageNetworkChannelAcceptor extends SocketAcceptor
{
  public MessageNetworkChannelAcceptor(int port, MaloWProcess notifier)
  {
    super(port, notifier);
  }

  @Override
  protected NetworkChannel createNetworkChannel(Socket socket)
  {
    return new MessageNetworkChannel(socket);
  }
}
