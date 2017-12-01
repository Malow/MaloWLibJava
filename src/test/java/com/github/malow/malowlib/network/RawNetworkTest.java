package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.network.tcpsocketraw.RawNetworkChannel;

public class RawNetworkTest extends RawNetworkFixture
{
  @Test
  public void testClientAndServerSendingMessagesBackAndForth() throws InterruptedException
  {
    this.testClient.sendToServer(new byte[] { 0x00, 0x02 });
    this.testServer.sendToAllClients(new byte[] { 0x00, 0x03 });

    Thread.sleep(10);

    assertEquals(this.testClient.messages.size(), 1);
    this.assertByteArray(this.testClient.messages.get(0), new byte[] { 0x00, 0x03 });
    assertEquals(this.testServer.clients.size(), 1);
    assertEquals(this.testServer.messages.size(), 1);
    for (RawNetworkChannel nc : this.testServer.messages.keySet())
    {
      List<byte[]> messages = this.testServer.messages.get(nc);
      assertEquals(messages.size(), 1);
      this.assertByteArray(messages.get(0), new byte[] { 0x00, 0x02 });
    }
  }

  private void assertByteArray(byte[] actual, byte[] expected)
  {
    for (int i = 0; i < expected.length; i++)
    {
      assertEquals(expected[i], actual[i]);
    }
  }
}
