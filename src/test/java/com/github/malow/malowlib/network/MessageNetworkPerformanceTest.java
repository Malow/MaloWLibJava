package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessageNetworkPerformanceTest extends MessageNetworkFixture
{
  public static final int MESSAGES = 2000000;

  @Test
  public void test() throws InterruptedException
  {
    Long before = System.nanoTime();
    for (int i = 0; i < MESSAGES; i++)
    {
      this.testServer.sendToAllClients("Welcome " + i);
    }

    long elapsed = System.nanoTime() - before;
    System.out.println(elapsed / 1000000.0 + "ms.");

    Thread.sleep(1000);

    assertEquals(this.testClient.messages.size(), MESSAGES);
    for (int i = 0; i < MESSAGES; i++)
    {
      assertEquals(this.testClient.messages.get(i), "Welcome " + i);
    }
  }
}
