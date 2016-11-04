package com.github.malow.malowlib.network;

import com.github.malow.malowlib.ProcessEvent;

public class NetworkPacket extends ProcessEvent
{
  private String message;
  private NetworkChannel sender;

  public NetworkPacket(String message, NetworkChannel sender)
  {
    this.message = message;
    this.sender = sender;
  }

  public String getMessage()
  {
    return this.message;
  }

  public NetworkChannel getSender()
  {
    return this.sender;
  }
}
