package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.RandomNumberGenerator;
import com.github.malow.malowlib.network.deprecated.tcpsocketmessage.MessageNetworkChannel;

public class MessageNetworkTest extends MessageNetworkFixture
{
  @Test
  public void testClientAndServerSendingMessagesBackAndForth() throws InterruptedException
  {
    this.testClient.sendToServer("Hello");
    this.testServer.sendToAllClients("Welcome");

    Thread.sleep(10);

    assertEquals(this.testClient.messages.size(), 1);
    assertEquals(this.testClient.messages.get(0), "Welcome");
    assertEquals(this.testServer.clients.size(), 1);
    assertEquals(this.testServer.messages.size(), 1);
    for (MessageNetworkChannel nc : this.testServer.messages.keySet())
    {
      List<String> messages = this.testServer.messages.get(nc);
      assertEquals(messages.size(), 1);
      assertEquals(messages.get(0), "Hello");
    }
  }

  @Test
  public void testClientAndServerSendingMessagesBackAndForth2() throws InterruptedException
  {
    this.testClient.sendToServer("Hello");
    this.testServer.sendToAllClients("Welcome");

    Thread.sleep(10);

    assertEquals(this.testClient.messages.size(), 1);
    assertEquals(this.testClient.messages.get(0), "Welcome");
    assertEquals(this.testServer.clients.size(), 1);
    assertEquals(this.testServer.messages.size(), 1);
    for (MessageNetworkChannel nc : this.testServer.messages.keySet())
    {
      List<String> messages = this.testServer.messages.get(nc);
      assertEquals(messages.size(), 1);
      assertEquals(messages.get(0), "Hello");
    }
  }

  @Test
  public void testHugeMessageCanBeSent() throws InterruptedException
  {
    StringBuffer message = new StringBuffer();
    for (int i = 0; i < 1000000; i++)
    {
      message.append((char) RandomNumberGenerator.getRandomInt(33, 125));
      if (i % 200 == 0)
      {
        message.append("\n");
      }
    }
    this.testServer.sendToAllClients(message.toString());

    Thread.sleep(100);

    assertEquals(this.testClient.messages.size(), 1);
    assertEquals(this.testClient.messages.get(0), message.toString());
  }

  @Test
  public void testMessageContainingLinebreakCanBeSent() throws InterruptedException
  {
    this.testServer.sendToAllClients("Wel\ncome");

    Thread.sleep(10);

    assertEquals(this.testClient.messages.size(), 1);
    assertEquals(this.testClient.messages.get(0), "Wel\ncome");
  }
}
