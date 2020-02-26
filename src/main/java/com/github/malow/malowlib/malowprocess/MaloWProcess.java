package com.github.malow.malowlib.malowprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWProcess
{
  class ProcessThread extends Thread
  {
    public ProcessThread(String processName)
    {
      super(processName);
      this.setDaemon(true);
    }

    @Override
    public void run()
    {
      MaloWProcess.this.runProcess();
    }
  }

  public enum ProcessState
  {
    NOT_STARTED,
    RUNNING,
    WAITING,
    FINISHED,
    FAILED
  }

  private volatile ProcessState state = ProcessState.NOT_STARTED;

  private static volatile long nextID = 0;

  private static synchronized long getAndIncrementId()
  {
    return nextID++;
  }

  private long id;

  public static int DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL = 100;
  public int warningThresholdEventQueue = DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL;
  public int unimportantEventThreshold = 10;

  protected volatile boolean stayAlive = true;
  private volatile List<ProcessThread> threads = new ArrayList<>();
  private BlockingQueue<ProcessEvent> eventQueue = new LinkedBlockingQueue<>();
  private String processName;
  private AtomicInteger waitingThreads = new AtomicInteger();

  public MaloWProcess()
  {
    this.create(this.getClass().getSimpleName(), 1);
  }

  public MaloWProcess(int threadCount)
  {
    this.create(this.getClass().getSimpleName(), threadCount);
  }

  public MaloWProcess(String processName)
  {
    this.create(processName, 1);
  }

  public MaloWProcess(String processName, int threadCount)
  {
    this.create(processName, threadCount);
  }

  private void create(String processName, int threadCount)
  {
    this.id = getAndIncrementId();
    this.processName = "MalowProcess:" + processName + "#" + this.id;
    for (int i = 0; i < threadCount; i++)
    {
      if (threadCount == 1)
      {
        this.threads.add(new ProcessThread(this.processName));
      }
      else
      {
        this.threads.add(new ProcessThread(this.processName + "(" + this.threads.size() + "/" + threadCount + ")"));
      }
    }
  }

  private void runProcess()
  {
    try
    {
      this.life();
      synchronized (this.threads)
      {
        this.threads.remove(Thread.currentThread());
        if (this.threads.size() == 0)
        {
          this.state = ProcessState.FINISHED;
        }
      }
    }
    catch (Exception e)
    {
      MaloWLogger.error("Uncaught unchecked Exception in MaloWProcess:Life method in thread " + Thread.currentThread().getName(), e);
      this.state = ProcessState.FAILED;
    }
  }

  public abstract void life();

  public void start()
  {
    if (this.state == ProcessState.NOT_STARTED)
    {
      this.state = ProcessState.RUNNING;
      synchronized (this.threads)
      {
        this.threads.forEach(t -> t.start());
      }
    }
    else
    {
      MaloWLogger.warning("Tried to start a MaloWProcess that was already running: " + this.processName);
    }
  }

  public void close()
  {
    if (this.stayAlive)
    {
      this.stayAlive = false;
      synchronized (this.threads)
      {
        this.threads.forEach(t -> t.interrupt());
      }
      this.closeSpecific();
    }
  }

  protected void closeSpecific()
  {

  }

  public void closeAndWaitForCompletion()
  {
    this.close();
    this.waitUntillDone();
  }

  public void waitUntillDone()
  {
    ProcessThread t = this.getFirstThread();
    while (t != null)
    {
      try
      {
        t.join();
      }
      catch (InterruptedException e)
      {
        MaloWLogger.error("waitUntillDone failed for thread " + t.getName() + " in MaloWProcess " + this.processName, e);
      }
      t = this.getFirstThread();
    }
  }

  private ProcessThread getFirstThread()
  {
    synchronized (this.threads)
    {
      if (this.threads.size() > 0)
      {
        return this.threads.get(0);
      }
    }
    return null;
  }

  protected ProcessEvent waitEvent()
  {
    try
    {
      this.waitingThreads.incrementAndGet();
      ProcessEvent ev = this.eventQueue.take();
      this.waitingThreads.decrementAndGet();
      return ev;
    }
    catch (InterruptedException e)
    {
      if (this.stayAlive)
      {
        MaloWLogger.error("waitEvent failed for thread " + Thread.currentThread().getName() + " in MaloWProcess " + this.processName, e);
      }
    }
    return null;
  }

  protected ProcessEvent peekEvent()
  {
    return this.eventQueue.poll();
  }

  public void putEvent(ProcessEvent ev)
  {
    this.eventQueue.add(ev);
    if (this.eventQueue.size() > this.warningThresholdEventQueue)
    {
      this.warningThresholdEventQueue *= 2;
      MaloWLogger.warning("eventQueue of process " + this.processName + " has " + this.eventQueue.size() + " unread events.");
    }
  }

  public void putUnimportantEvent(ProcessEvent ev)
  {
    if (this.eventQueue.size() > this.unimportantEventThreshold)
    {
      return;
    }
    this.putEvent(ev);
  }

  public ProcessState getState()
  {
    if (this.state == ProcessState.RUNNING && this.waitingThreads.get() == this.threads.size())
    {
      return ProcessState.WAITING;
    }
    return this.state;
  }

  public long getID()
  {
    return this.id;
  }

  public int getEventQueueSize()
  {
    return this.eventQueue.size();
  }

  public static long getNrOfProcsCreated()
  {
    return nextID;
  }

  public String getProcessName()
  {
    return this.processName;
  }

  public List<String> getStackTracesForAllThreads()
  {
    List<String> list = new ArrayList<>();
    for (ProcessThread thread : this.threads)
    {
      StringBuffer buf = new StringBuffer();
      for (StackTraceElement element : thread.getStackTrace())
      {
        buf.append("\n    at " + element.toString());
      }
      list.add(buf.toString());
    }
    return list;
  }
}