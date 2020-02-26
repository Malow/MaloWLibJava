package com.github.malow.malowlib.network;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class ClientConnectedEvent extends ProcessEvent
{
  public ThreadedNetworkChannel client;

  public ClientConnectedEvent(ThreadedNetworkChannel client)
  {
    this.client = client;
  }
}
