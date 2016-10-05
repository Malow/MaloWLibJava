package com.github.malow.malowlib;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class MaloWProcess
{

  private class ProcThread extends Thread
  {

    @Override
    public void run()
    {
      state = ProcessState.RUNNING;
      life();
      state = ProcessState.FINISHED;
    }

    public synchronized void resumeThread()
    {
      try
      {
        notify();
      }
      catch (Exception E)
      {
        System.out.println("resumeThread failed");
      }
    }

    public synchronized void suspendThread()
    {
      try
      {
        wait();
      }
      catch (Exception E)
      {
        System.out.println("suspendThread failed");
      }
    }
  }

  public enum ProcessState
  {
    NOT_STARTED, WAITING, RUNNING, FINISHED
  }

  public static final int DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL = 250;
  public static final long WAIT_TIMEOUT = 0;
  private int warningThresholdEventQueue = DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL;
  private static long nextID = 0;
  private ProcThread thread;
  private ConcurrentLinkedQueue<ProcessEvent> eventQueue;
  private ProcessState state;
  private long id;
  protected boolean stayAlive = true;

  public MaloWProcess()
  {
    this.id = MaloWProcess.nextID;
    MaloWProcess.nextID++;
    this.state = ProcessState.NOT_STARTED;
    this.eventQueue = new ConcurrentLinkedQueue<ProcessEvent>();
    this.thread = new ProcThread();
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
    catch (InterruptedException e1)
    {
      System.out.println("waitUntillDone failed");
    }
  }

  public ProcessEvent waitEvent()
  {
    if (this.eventQueue.isEmpty())
    {
      try
      {
        synchronized (this)
        {
          this.state = ProcessState.WAITING;
          this.wait();
          this.state = ProcessState.RUNNING;
        }
      }
      catch (InterruptedException e)
      {
        System.out.println("waitEvent failed");
      }
    }
    return this.peekEvent();
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
      System.out.println("Warning, EventQueue of process " + this.id + " has " + this.eventQueue.size() + " unread events.");
    }
    synchronized (this)
    {
      this.notifyAll();
    }
  }

  public void putUnimportantEvent(ProcessEvent ev)
  {
    int queueSize = this.eventQueue.size();
    if (queueSize > 20) { return; }
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