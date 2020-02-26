package com.github.malow.malowlib.network.tpcsocketmessage;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.ThreadedNetworkChannel;
import com.github.malow.malowlib.network.SocketAcceptor;

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
