package com.github.malow.malowlib.network;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.github.malow.malowlib.MaloWUtils;
import com.github.malow.malowlib.byteconversion.Byteable;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class NetworkChannelFixture
{
  private static final String IP = "127.0.0.1";
  private static final int PORT = 10000;
  private static final int TIMEOUT_MS = 1000;

  protected static class TestNetworkChannel extends NetworkChannel
  {
    public TestNetworkChannel(Socket socket)
    {
      super(socket);
    }

    public TestNetworkChannel(String ip, int port)
    {
      super(ip, port);
    }

    @Override
    protected Byteable createPacket(ByteBuffer bb) throws Exception
    {
      return Byteable.create(bb, StringPacket.class);
    }
  }

  protected static class TestSocketAcceptor extends SocketAcceptor
  {
    public TestSocketAcceptor(int port, MaloWProcess notifier)
    {
      super(port, notifier);
    }

    @Override
    protected NetworkChannel createNetworkChannel(Socket socket)
    {
      return new TestNetworkChannel(socket);
    }
  }

  protected static class TestServer extends MaloWProcess
  {
    public volatile List<NetworkChannel> clients = new ArrayList<>();
    public volatile Map<NetworkChannel, List<String>> messages = new HashMap<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.peekEvent();
        if (ev != null && ev instanceof ClientConnectedEvent)
        {
          NetworkChannel client = ((ClientConnectedEvent) ev).client;
          this.clients.add(client);
          this.messages.put(client, new ArrayList<>());
        }
        for (NetworkChannel client : this.clients)
        {
          client.getMessage().ifPresent(packet ->
          {
            if (packet instanceof StringPacket)
            {
              List<String> m = this.messages.get(client);
              m.add(((StringPacket) packet).message);
              this.messages.put(client, m);
            }
          });
        }
        MaloWUtils.sleep(10);
      }
    }

    public void sendToAllClients(String msg)
    {
      this.clients.forEach(c -> c.sendMessage(StringPacket.create(msg)));
    }

    @Override
    public void closeSpecific()
    {
      this.clients.forEach(c -> c.close());
    }
  }

  protected static class TestClient extends MaloWProcess
  {
    public volatile NetworkChannel networkChannel;
    public volatile List<String> messages = new ArrayList<>();

    public TestClient()
    {
      this.networkChannel = new TestNetworkChannel(IP, PORT);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        this.networkChannel.getMessage().ifPresent(packet ->
        {
          if (packet instanceof StringPacket)
          {
            this.messages.add(((StringPacket) packet).message);
          }
        });
        MaloWUtils.sleep(1);
      }
    }

    public void sendToServer(String msg)
    {
      this.networkChannel.sendMessage(StringPacket.create(msg));
    }

    @Override
    public void closeSpecific()
    {
      this.networkChannel.close();
    }
  }

  protected TestServer testServer;
  protected SocketAcceptor testSocketAcceptor;
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