package com.github.malow.malowlib.network.deprecated;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

@Deprecated
public abstract class ThreadedNetworkChannel extends MaloWProcess
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

  private BlockingQueue<ProcessEvent> bufferQueue = new LinkedBlockingQueue<>();

  public ThreadedNetworkChannel(Socket socket)
  {
    this.socket = socket;
    this.init();
    this.start();
  }

  public ThreadedNetworkChannel(String ip, int port)
  {
    try
    {
      this.socket = new Socket(ip, port);
      this.init();
      this.start();
    }
    catch (Exception e)
    {
      this.close();
      MaloWLogger.error("Error creating socket: " + ip + ":" + port + ". Channel: " + this.id, e);
    }
  }

  protected void init()
  {
  };

  public synchronized void clearBufferQueue()
  {
    this.bufferQueue.clear();
  }

  public void setNotifier(MaloWProcess notifier)
  {
    this.notifier = notifier;
    this.sendQueuedEvents();
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      ProcessEvent msg = this.receiveMessage();
      if (msg != null && this.stayAlive)
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
  }

  private synchronized void sendQueuedEvents()
  {
    while (!this.bufferQueue.isEmpty())
    {
      this.notifier.putEvent(this.bufferQueue.poll());
    }
  }

  protected abstract ProcessEvent receiveMessage();

  @Override
  public void closeSpecific()
  {
    if (this.socket == null)
    {
      return;
    }

    try
    {
      this.socket.close();
      this.socket = null;
    }
    catch (IOException e)
    {
      MaloWLogger.error("Failed to close socket in channel: " + this.id, e);
    }
  }
}
