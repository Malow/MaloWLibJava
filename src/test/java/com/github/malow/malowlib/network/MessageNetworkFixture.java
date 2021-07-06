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
import com.github.malow.malowlib.network.deprecated.ClientConnectedEvent;
import com.github.malow.malowlib.network.deprecated.SocketAcceptor;
import com.github.malow.malowlib.network.deprecated.tcpsocketmessage.MessageNetworkChannel;
import com.github.malow.malowlib.network.deprecated.tcpsocketmessage.MessageNetworkChannelAcceptor;
import com.github.malow.malowlib.network.deprecated.tcpsocketmessage.NetworkMessage;

public class MessageNetworkFixture
{
  private static final String IP = "127.0.0.1";
  private static final int PORT = 10000;
  private static final int TIMEOUT_MS = 1000;

  protected static class TestServer extends MaloWProcess
  {
    public volatile List<MessageNetworkChannel> clients = new ArrayList<>();
    public volatile Map<MessageNetworkChannel, List<String>> messages = new HashMap<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof ClientConnectedEvent)
        {
          MessageNetworkChannel client = (MessageNetworkChannel) ((ClientConnectedEvent) ev).client;
          client.setNotifier(this);
          this.clients.add(client);
          this.messages.put(client, new ArrayList<>());
        }
        else if (ev instanceof NetworkMessage)
        {
          NetworkMessage np = (NetworkMessage) ev;
          List<String> m = this.messages.get(np.getSender());
          m.add(np.getMessage());
          this.messages.put(np.getSender(), m);
        }
      }
    }

    public void sendToAllClients(String msg)
    {
      this.clients.forEach(c -> c.sendMessage(msg));
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
    public volatile MessageNetworkChannel networkChannel;
    public volatile List<String> messages = new ArrayList<>();

    public TestClient()
    {
      this.networkChannel = new MessageNetworkChannel(IP, PORT);
      this.networkChannel.setNotifier(this);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof NetworkMessage)
        {
          NetworkMessage np = (NetworkMessage) ev;
          this.messages.add(np.getMessage());
        }
      }
    }

    public void sendToServer(String msg)
    {
      this.networkChannel.sendMessage(msg);
    }

    @Override
    public void closeSpecific()
    {
      this.networkChannel.close();
      this.networkChannel.waitUntillDone();
    }
  }

  protected TestServer testServer;
  protected SocketAcceptor testSocketlistener;
  protected TestClient testClient;

  @Before
  public void before()
  {
    this.testServer = new TestServer();
    this.testServer.start();

    this.testSocketlistener = new MessageNetworkChannelAcceptor(PORT, this.testServer);
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
