package com.github.malow.malowlib.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.github.malow.malowlib.MaloWUtils;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;
import com.github.malow.malowlib.network.NetworkChannel.NetworkChannelClosedException;
import com.github.malow.malowlib.network.ProtoNetworkChannelTest.TestProtoNetworkChannel;
import com.google.protobuf.Message;

public class ProtoNetworkChannelFixture
{
  private static final String IP = "127.0.0.1";
  private static final int PORT = 10000;
  private static final int TIMEOUT_MS = 1000;

  protected static class TestSocketAcceptor extends SocketAcceptor<TestProtoNetworkChannel>
  {
    public TestSocketAcceptor(int port, MaloWProcess notifier)
    {
      super(port, notifier);
    }

    @Override
    protected TestProtoNetworkChannel createNetworkChannel(Socket socket)
    {
      return new TestProtoNetworkChannel(socket);
    }
  }

  protected static class TestServer extends MaloWProcess
  {
    public volatile List<TestProtoNetworkChannel> clients = new ArrayList<>();
    public volatile Map<TestProtoNetworkChannel, List<Message>> messages = new HashMap<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.peekEvent();
        if (ev != null && ev instanceof ClientConnectedEvent cce)
        {
          TestProtoNetworkChannel client = cce.getClient();
          this.clients.add(client);
          this.messages.put(client, new ArrayList<>());
        }
        for (TestProtoNetworkChannel client : this.clients)
        {
          try
          {
            Optional<Message> msg = client.receive();
            msg.ifPresent(message ->
            {
              List<Message> m = this.messages.get(client);
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

    @Override
    public void closeSpecific()
    {
      this.clients.forEach(c -> c.close());
    }
  }

  protected static class TestClient extends MaloWProcess
  {
    public volatile TestProtoNetworkChannel networkChannel;
    public volatile List<Message> messages = new ArrayList<>();

    public TestClient()
    {
      this.networkChannel = new TestProtoNetworkChannel(IP, PORT, 0);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        try
        {
          Optional<Message> msg = this.networkChannel.receive();
          msg.ifPresent(message ->
          {
            this.messages.add(message);
          });
          MaloWUtils.ignoreException(() -> Thread.sleep(1));
        }
        catch (NetworkChannelClosedException e)
        {
        }
      }
    }

    @Override
    public void closeSpecific()
    {
      this.networkChannel.close();
    }
  }

  protected TestServer testServer;
  protected SocketAcceptor<TestProtoNetworkChannel> testSocketAcceptor;
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