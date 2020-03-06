package com.github.malow.malowlib.network;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

@Deprecated
// Old version was moved to ThreadedNetworkChannel, this one is a new version but Not yet implemented
public abstract class NetworkChannel
{
  private static long nextID = 0;

  private static synchronized long getAndIncrementId()
  {
    return nextID++;
  }

  private long id = getAndIncrementId();

  public long getChannelID()
  {
    return this.id;
  }

  protected Socket socket = null;
  protected MaloWProcess notifier = null;
  private boolean isAlive = false;

  private BlockingQueue<ProcessEvent> bufferQueue = new LinkedBlockingQueue<>();

  public NetworkChannel(Socket socket)
  {
    this.socket = socket;
    this.isAlive = true;
  }

  public NetworkChannel(String ip, int port) throws Exception
  {
    this.socket = new Socket(ip, port);
    this.isAlive = true;
  }

  public synchronized void clearBufferQueue()
  {
    this.bufferQueue.clear();
  }

  private synchronized void sendQueuedEvents()
  {
    while (!this.bufferQueue.isEmpty())
    {
      this.notifier.putEvent(this.bufferQueue.poll());
    }
  }

  public void setNotifier(MaloWProcess notifier)
  {
    this.notifier = notifier;
    this.sendQueuedEvents();
  }

  public void update() throws Exception
  {
    if (!this.isAlive)
    {
      throw new Exception("NetworkChannel isn't alive");
    }

    ProcessEvent msg = this.receiveMessage();
    if (msg != null)
    {
      if (this.notifier != null)
      {
        this.sendQueuedEvents();
        this.notifier.putEvent(msg);
      }
      else
      {
        this.bufferQueue.add(msg);
      }
    }
  }

  public void close()
  {
    try
    {
      if (this.socket != null)
      {
        this.socket.close();
      }
    }
    catch (Exception e)
    {
    }
    this.socket = null;
  }

  protected abstract ProcessEvent receiveMessage();
}
