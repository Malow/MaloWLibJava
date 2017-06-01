package com.github.malow.malowlib.network;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class ClientConnectedEvent extends ProcessEvent
{
  public NetworkChannel client;

  public ClientConnectedEvent(NetworkChannel client)
  {
    this.client = client;
  }
}
