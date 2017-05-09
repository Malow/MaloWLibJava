package com.github.malow.malowlib.malowprocess;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWProcess
{
  private class ProcThread extends Thread
  {
    public ProcThread(String processName)
    {
      super(processName);
    }

    @Override
    public void run()
    {
      MaloWProcess.this.state = ProcessState.RUNNING;
      try
      {
        MaloWProcess.this.life();
      }
      catch (Exception e)
      {
        MaloWLogger.error("Uncaught unchecked Exception in MaloWProcess:Life method in thread " + this.getName(), e);
      }
      MaloWProcess.this.state = ProcessState.FINISHED;
    }

    public synchronized void resumeThread()
    {
      try
      {
        this.notifyAll();
      }
      catch (Exception e)
      {
        MaloWLogger.error("resumeThread failed", e);
      }
    }

    public synchronized void suspendThread()
    {
      try
      {
        this.wait();
      }
      catch (Exception e)
      {
        MaloWLogger.error("suspendThread failed", e);
      }
    }
  }

  public enum ProcessState
  {
    NOT_STARTED,
    RUNNING,
    WAITING,
    FINISHED
  }

  private static long nextID = 0;

  private static synchronized long getAndIncrementId()
  {
    return nextID++;
  }

  public static int DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL = 100;
  public int warningThresholdEventQueue = DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL;
  public int unimportantEventThreshold = 10;

  protected boolean stayAlive = true;
  private ProcThread thread;
  private BlockingQueue<ProcessEvent> eventQueue;
  private ProcessState state;
  private long id;
  private String processName;

  public MaloWProcess()
  {
    this.create(this.getClass().getSimpleName());
  }

  public MaloWProcess(String processName)
  {
    this.create(processName);
  }

  private void create(String processName)
  {
    this.id = getAndIncrementId();
    this.processName = "MalowProcess:" + processName + "#" + this.id;
    this.state = ProcessState.NOT_STARTED;
    this.eventQueue = new LinkedBlockingQueue<ProcessEvent>();
    this.thread = new ProcThread(this.processName);
  }

  public abstract void life();

  public void start()
  {
    if (this.state == ProcessState.NOT_STARTED)
    {
      this.thread.start();
    }
  }

  public void suspend()
  {
    this.thread.suspendThread();
  }

  public void resume()
  {
    this.thread.resumeThread();
  }

  public void close()
  {
    this.stayAlive = false;
    ProcessEvent ev = new ProcessEvent();
    this.putEvent(ev);
    this.closeSpecific();
  }

  public abstract void closeSpecific();

  public void waitUntillDone()
  {
    try
    {
      this.thread.join();
    }
    catch (InterruptedException e)
    {
      MaloWLogger.error("waitUntillDone failed for process " + this.processName, e);
    }
  }

  public ProcessEvent waitEvent()
  {
    try
    {
      this.state = ProcessState.WAITING;
      ProcessEvent ev = this.eventQueue.take();
      this.state = ProcessState.RUNNING;
      return ev;
    }
    catch (InterruptedException e)
    {
      MaloWLogger.error("waitEvent failed for process " + this.processName, e);
    }
    return null;
  }

  public ProcessEvent peekEvent()
  {
    return this.eventQueue.poll();
  }

  public void putEvent(ProcessEvent ev)
  {
    this.eventQueue.add(ev);
    if (this.eventQueue.size() > this.warningThresholdEventQueue)
    {
      this.warningThresholdEventQueue *= 2;
      MaloWLogger.warning("Warning, EventQueue of process " + this.processName + " has " + this.eventQueue.size() + " unread events.");
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
}