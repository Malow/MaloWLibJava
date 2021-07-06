package com.github.malow.malowlib.network.deprecated.tcpsocketmessage;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.deprecated.SocketAcceptor;
import com.github.malow.malowlib.network.deprecated.ThreadedNetworkChannel;

@Deprecated
public class MessageNetworkChannelAcceptor extends SocketAcceptor
{
  public MessageNetworkChannelAcceptor(int port, MaloWProcess notifier)
  {
    super(port, notifier);
  }

  @Override
  protected ThreadedNetworkChannel createNetworkChannel(Socket socket)
  {
    return new MessageNetworkChannel(socket);
  }
}
