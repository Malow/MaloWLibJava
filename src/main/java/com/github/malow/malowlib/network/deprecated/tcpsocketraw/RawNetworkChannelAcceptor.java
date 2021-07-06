package com.github.malow.malowlib.network.deprecated.tcpsocketraw;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.deprecated.SocketAcceptor;
import com.github.malow.malowlib.network.deprecated.ThreadedNetworkChannel;

@Deprecated
public class RawNetworkChannelAcceptor extends SocketAcceptor
{
  public RawNetworkChannelAcceptor(int port, MaloWProcess notifier)
  {
    super(port, notifier);
  }

  @Override
  protected ThreadedNetworkChannel createNetworkChannel(Socket socket)
  {
    return new RawNetworkChannel(socket);
  }
}
