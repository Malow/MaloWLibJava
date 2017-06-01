package com.github.malow.malowlib.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class RawNetworkFixture
{
  private static final String IP = "127.0.0.1";
  private static final int PORT = 10000;
  private static final int TIMEOUT_MS = 1000;

  protected static class TestServer extends MaloWProcess
  {
    public volatile List<RawNetworkChannel> clients = new ArrayList<>();
    public volatile Map<RawNetworkChannel, List<byte[]>> messages = new HashMap<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof ClientConnectedEvent)
        {
          RawNetworkChannel client = (RawNetworkChannel) ((ClientConnectedEvent) ev).client;
          client.setNotifier(this);
          this.clients.add(client);
          this.messages.put(client, new ArrayList<>());
        }
        else if (ev instanceof RawNetworkPacket)
        {
          RawNetworkPacket np = (RawNetworkPacket) ev;
          List<byte[]> m = this.messages.get(np.from);
          m.add(np.bytes);
          this.messages.put(np.from, m);
        }
      }
    }

    public void sendToAllClients(byte[] bytes)
    {
      this.clients.forEach(c -> c.sendRawData(bytes));
    }

    @Override
    public void closeSpecific()
    {
      this.clients.forEach(c -> c.close());
      this.clients.forEach(c -> c.waitUntillDone());
    }
  }

  protected static class TestClient extends MaloWProcess
  {
    public volatile RawNetworkChannel networkChannel;
    public volatile List<byte[]> messages = new ArrayList<>();

    public TestClient()
    {
      this.networkChannel = new RawNetworkChannel(IP, PORT);
      this.networkChannel.setNotifier(this);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof RawNetworkPacket)
        {
          RawNetworkPacket np = (RawNetworkPacket) ev;
          this.messages.add(np.bytes);
        }
      }
    }

    public void sendToServer(byte[] bytes)
    {
      this.networkChannel.sendRawData(bytes);
    }

    @Override
    public void closeSpecific()
    {
      this.networkChannel.close();
      this.networkChannel.waitUntillDone();
    }
  }

  protected TestServer testServer;
  protected SocketListener testSocketlistener;
  protected TestClient testClient;

  @Before
  public void before()
  {
    this.testServer = new TestServer();
    this.testServer.start();

    this.testSocketlistener = new RawSocketListener(PORT, this.testServer);
    this.testSocketlistener.start();

    this.testClient = new TestClient();
    this.testClient.start();

    this.waitForClientToBeConnected();
  }

  private void waitForClientToBeConnected()
  {
    long before = System.currentTimeMillis();
    while (this.testServer.clients.size() == 0)
    {
      if (System.currentTimeMillis() > before + TIMEOUT_MS)
      {
        Assert.fail("waitForClientToBeConnected timed out");
        return;
      }
      try
      {
        Thread.sleep(10);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  @After
  public void after()
  {
    this.testClient.close();
    this.testClient.waitUntillDone();

    this.testSocketlistener.close();
    this.testSocketlistener.waitUntillDone();

    this.testServer.close();
    this.testServer.waitUntillDone();
  }
}
