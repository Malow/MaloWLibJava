package com.github.malow.malowlib.network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Optional;

import com.github.malow.malowlib.MaloWLogger;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public abstract class ProtoNetworkChannel extends NetworkChannel
{
  public ProtoNetworkChannel(Socket socket)
  {
    super(socket);
  }

  public ProtoNetworkChannel(String ip, int port)
  {
    super(ip, port);
  }

  protected void sendWithMessageTypeId(Message message, int messageTypeId)
  {
    this.sendBytesWithMessageTypeId(message.toByteArray(), messageTypeId);
  }

  protected abstract Class<? extends Message> getMessageClassForType(int type);

  public Optional<Message> recieve()
  {
    ByteBuffer bb = this.receiveBytes();
    if (bb == null)
    {
      return Optional.empty();
    }

    try
    {
      int type = bb.getInt();
      Class<? extends Message> clazz = this.getMessageClassForType(type);
      Parser<?> parser = (Parser<?>) clazz.getMethod("parser").invoke(clazz);
      Message obj = (Message) parser.parseFrom(bb);
      return Optional.of(obj);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to create Message", e);
    }
    return Optional.empty();
  }
}
