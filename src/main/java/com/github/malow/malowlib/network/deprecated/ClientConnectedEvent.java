package com.github.malow.malowlib.network.deprecated;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

@Deprecated
public class ClientConnectedEvent extends ProcessEvent
{
  public ThreadedNetworkChannel client;

  public ClientConnectedEvent(ThreadedNetworkChannel client)
  {
    this.client = client;
  }
}
