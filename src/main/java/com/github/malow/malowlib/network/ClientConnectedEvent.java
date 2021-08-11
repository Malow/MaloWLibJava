package com.github.malow.malowlib.network;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class ClientConnectedEvent extends ProcessEvent
{
  private Object client;

  public ClientConnectedEvent(Object client)
  {
    this.client = client;
  }

  @SuppressWarnings("unchecked")
  public <S extends NetworkChannel> S getClient()
  {
    return (S) this.client;
  }
}