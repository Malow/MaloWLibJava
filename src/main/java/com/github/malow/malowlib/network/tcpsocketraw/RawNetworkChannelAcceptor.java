package com.github.malow.malowlib.network.tcpsocketraw;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.ThreadedNetworkChannel;
import com.github.malow.malowlib.network.SocketAcceptor;

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
