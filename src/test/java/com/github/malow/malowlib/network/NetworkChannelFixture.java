package com.github.malow.malowlib.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.github.malow.malowlib.MaloWUtils;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;
import com.github.malow.malowlib.network.NetworkChannel.NetworkChannelClosedException;

public class NetworkChannelFixture
{
  private static final String IP = "127.0.0.1";
  private static final int PORT = 10000;
  private static final int TIMEOUT_MS = 1000;

  protected static class TestSocketAcceptor extends SocketAcceptor<StringNetworkChannel>
  {
    public TestSocketAcceptor(int port, MaloWProcess notifier)
    {
      super(port, notifier);
    }

    @Override
    protected StringNetworkChannel createNetworkChannel(Socket socket)
    {
      return new StringNetworkChannel(socket);
    }
  }

  protected static class TestServer extends MaloWProcess
  {
    public volatile List<StringNetworkChannel> clients = new ArrayList<>();
    public volatile Map<StringNetworkChannel, List<String>> messages = new HashMap<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.peekEvent();
        if (ev != null && ev instanceof ClientConnectedEvent cce)
        {
          StringNetworkChannel client = cce.getClient();
          this.clients.add(client);
          this.messages.put(client, new ArrayList<>());
        }
        for (StringNetworkChannel client : this.clients)
        {
          try
          {
            client.receive().ifPresent(message ->
            {
              List<String> m = this.messages.get(client);
              m.add(message);
              this.messages.put(client, m);
            });
          }
          catch (NetworkChannelClosedException e)
          {
          }
        }
        MaloWUtils.ignoreException(() -> Thread.sleep(10));
      }
    }

    public void sendToAllClients(String msg) throws NetworkChannelClosedException
    {
      for (StringNetworkChannel client : this.clients)
      {
        client.send(msg);
      }
    }

    @Override
    public void closeSpecific()
    {
      this.clients.forEach(c -> c.close());
    }
  }

  protected static class TestClient extends MaloWProcess
  {
    public volatile StringNetworkChannel networkChannel;
    public volatile List<String> messages = new ArrayList<>();

    public TestClient()
    {
      this.networkChannel = new StringNetworkChannel(IP, PORT, 0);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        try
        {
          this.networkChannel.receive().ifPresent(message ->
          {
            this.messages.add(message);
          });
        }
        catch (NetworkChannelClosedException e)
        {
        }
        MaloWUtils.ignoreException(() -> Thread.sleep(1));
      }
    }

    public void sendToServer(String msg) throws NetworkChannelClosedException
    {
      this.networkChannel.send(msg);
    }

    @Override
    public void closeSpecific()
    {
      this.networkChannel.close();
    }
  }

  protected TestServer testServer;
  protected SocketAcceptor<StringNetworkChannel> testSocketAcceptor;
  protected TestClient testClient;

  @Before
  public void before()
  {
    this.testServer = new TestServer();
    this.testServer.start();

    this.testSocketAcceptor = new TestSocketAcceptor(PORT, this.testServer);
    this.testSocketAcceptor.start();

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
        Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  @After
  public void after()
  {
    this.testSocketAcceptor.close();
    this.testSocketAcceptor.waitUntillDone();

    this.testClient.close();
    this.testClient.waitUntillDone();

    this.testServer.close();
    this.testServer.waitUntillDone();
  }
}