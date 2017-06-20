package com.github.malow.malowlib.network.raw;

import java.net.Socket;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.network.NetworkChannel;
import com.github.malow.malowlib.network.SocketAcceptor;

public class RawNetworkChannelAcceptor extends SocketAcceptor
{
  public RawNetworkChannelAcceptor(int port, MaloWProcess notifier)
  {
    super(port, notifier);
  }

  @Override
  protected NetworkChannel createNetworkChannel(Socket socket)
  {
    return new RawNetworkChannel(socket);
  }
}
