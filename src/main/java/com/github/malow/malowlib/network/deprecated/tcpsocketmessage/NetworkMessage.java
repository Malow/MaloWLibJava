package com.github.malow.malowlib.network.deprecated.tcpsocketmessage;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

@Deprecated
public class NetworkMessage extends ProcessEvent
{
  private String message;
  private MessageNetworkChannel sender;

  public NetworkMessage(String message, MessageNetworkChannel sender)
  {
    this.message = message;
    this.sender = sender;
  }

  public String getMessage()
  {
    return this.message;
  }

  public MessageNetworkChannel getSender()
  {
    return this.sender;
  }
}
