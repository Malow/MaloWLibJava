package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.RandomNumberGenerator;

public class NetworkChannelTest extends NetworkChannelFixture
{
  @Test
  public void testClientAndServerSendingMessagesBackAndForth() throws Exception
  {
    this.testClient.sendToServer("Hello");
    this.testServer.sendToAllClients("Welcome");

    Thread.sleep(100);

    assertEquals(1, this.testClient.messages.size());
    assertEquals("Welcome", this.testClient.messages.get(0));
    assertEquals(1, this.testServer.clients.size());
    assertEquals(1, this.testServer.messages.size());
    for (NetworkChannel nc : this.testServer.messages.keySet())
    {
      List<String> messages = this.testServer.messages.get(nc);
      assertEquals(1, messages.size());
      assertEquals("Hello", messages.get(0));
    }
  }

  @Test
  public void testClientAndServerSendingMessagesBackAndForth2() throws Exception
  {
    this.testClient.sendToServer("Hello");
    this.testServer.sendToAllClients("Welcome");

    Thread.sleep(100);

    assertEquals(this.testClient.messages.size(), 1);
    assertEquals(this.testClient.messages.get(0), "Welcome");
    assertEquals(this.testServer.clients.size(), 1);
    assertEquals(this.testServer.messages.size(), 1);
    for (NetworkChannel nc : this.testServer.messages.keySet())
    {
      List<String> messages = this.testServer.messages.get(nc);
      assertEquals(1, messages.size());
      assertEquals("Hello", messages.get(0));
    }
  }

  @Test
  public void testBigMessageCanBeSent() throws Exception
  {
    MaloWLogger.init();
    MaloWLogger.setLoggingThresholdToInfo();
    StringBuffer message = new StringBuffer();
    for (int i = 0; i < 900000; i++)
    {
      message.append((char) RandomNumberGenerator.getRandomInt(33, 125));
      if (i % 200 == 0)
      {
        message.append("\n");
      }
    }
    this.testServer.sendToAllClients(message.toString());

    Thread.sleep(100);

    assertEquals(1, this.testClient.messages.size());
    assertEquals(message.toString(), this.testClient.messages.get(0));
  }

  @Test
  public void testMessageContainingLinebreakCanBeSent() throws Exception
  {
    this.testServer.sendToAllClients("Wel\ncome");

    Thread.sleep(100);

    assertEquals(1, this.testClient.messages.size());
    assertEquals("Wel\ncome", this.testClient.messages.get(0));
  }
}