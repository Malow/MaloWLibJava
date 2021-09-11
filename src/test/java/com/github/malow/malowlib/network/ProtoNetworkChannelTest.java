package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import java.net.Socket;

import org.junit.Test;

import com.github.malow.malowlib.byteconversion.proto.TestDataProto;
import com.github.malow.malowlib.byteconversion.proto.TestDataProto.InnerTestDataProto;
import com.google.protobuf.Message;

public class ProtoNetworkChannelTest extends ProtoNetworkChannelFixture
{
  protected static enum TestProtoMessageType
  {
    TEST_DATA_PROTO(TestDataProto.class),
    INNER_TEST_DATA_PROTO(InnerTestDataProto.class);

    private final static TestProtoMessageType[] values = values();

    public static TestProtoMessageType fromInt(int i)
    {
      return values[i];
    }

    public int toInt()
    {
      return this.ordinal();
    }

    private final Class<? extends Message> clazz;

    private TestProtoMessageType(Class<? extends Message> clazz)
    {
      this.clazz = clazz;
    }

    public Class<? extends Message> getPacketClass()
    {
      return this.clazz;
    }
  }

  protected static class TestProtoNetworkChannel extends ProtoNetworkChannel
  {
    public TestProtoNetworkChannel(Socket socket)
    {
      super(socket);
    }

    public TestProtoNetworkChannel(String ip, int port, int readTimeoutMs)
    {
      super(ip, port, readTimeoutMs);
    }

    @Override
    protected Class<? extends Message> getMessageClassForType(int type)
    {
      return TestProtoMessageType.fromInt(type).getPacketClass();
    }

    protected void send(TestProtoMessageType messageType, Message message) throws NetworkChannelClosedException
    {
      this.sendWithMessageTypeId(messageType.toInt(), message);
    }
  }

  @Test
  public void test() throws Exception
  {
    TestDataProto data = TestDataProto.newBuilder().setS("TestDataProto").setB(true).setInner(InnerTestDataProto.newBuilder().setS("asd").build())
        .build();
    InnerTestDataProto data2 = InnerTestDataProto.newBuilder().setS("InnerTestDataProto").build();

    for (TestProtoNetworkChannel client : this.testServer.clients)
    {
      client.send(TestProtoMessageType.TEST_DATA_PROTO, data);
    }
    for (TestProtoNetworkChannel client : this.testServer.clients)
    {
      client.send(TestProtoMessageType.INNER_TEST_DATA_PROTO, data2);
    }

    Thread.sleep(100);

    assertEquals(2, this.testClient.messages.size());
    Message m1 = this.testClient.messages.get(0);
    Message m2 = this.testClient.messages.get(1);
    assertEquals(m1.getClass(), TestDataProto.class);
    assertEquals(m2.getClass(), InnerTestDataProto.class);
    assertEquals(data, m1);
    assertEquals(data2, m2);
  }
}
