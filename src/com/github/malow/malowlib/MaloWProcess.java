package com.github.malow.malowlib;

//import java.util.ArrayDeque;
import java.util.LinkedList;

public abstract class MaloWProcess
{
  class ProcThread extends Thread
  {
    @Override
    public void run()
    {
      state = RUNNING;
      life();
      state = FINISHED;
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

  public static final int NOT_STARTED = 0, WAITING = 1, RUNNING = 2, FINISHED = 3;
  private static final int DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL = 250;

  private static long nextID = 0;

  private ProcThread proc;
  private LinkedList<ProcessEvent> eventQueue;
  private int state;
  private int warningThresholdEventQueue = DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL;
  private long id;

  private boolean debug = false;

  protected boolean stayAlive = true;

  public MaloWProcess()
  {
    this.id = MaloWProcess.nextID;
    MaloWProcess.nextID++;
    this.state = NOT_STARTED;
    this.eventQueue = new LinkedList<ProcessEvent>();
    this.proc = new ProcThread();
  }

  public abstract void life();

  public void start()
  {
    if (this.state == NOT_STARTED)
    {
      this.proc.start();
    }
  }

  public void suspend()
  {
    this.proc.suspendThread();
  }

  public void resume()
  {
    // Needed because WaitEvent is not completely synchronized, so if a thread wants to resume it while it's 
    // going to sleep we need to continuously call on it to restart until it responds. Either solve this a better way or start using 
    // PeekEvent() instead of WaitEvent() more and add a sleep between Peeks.
    while (this.state == WAITING)
      this.proc.resumeThread();
  }

  public void close()
  {
    this.stayAlive = false;
    ProcessEvent ev = new ProcessEvent();
    this.putEvent(ev);
    this.closeSpecific();
  }

  public void closeSpecific()
  {

  }

  public void waitUntillDone()
  {
    while (this.state != FINISHED)
      try
      {
        Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
        System.out.println("WaitUntillDone failed");
      }
  }

  public ProcessEvent waitEvent()
  {
    boolean sleep = this.waitEventCheckForSleep();

    if (sleep)
    {
      this.suspend();
      this.state = RUNNING;
    }

    return this.waitEventDequeEvent();
  }

  private synchronized boolean waitEventCheckForSleep()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.id + " Mutex for WaitEvent Failed, multiple procs modifying data.");
    this.debug = true;
    boolean sleep = this.eventQueue.isEmpty();

    if (sleep)
    {
      this.state = WAITING;
    }

    this.debug = false;
    return sleep;
  }

  private synchronized ProcessEvent waitEventDequeEvent()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.id + " Mutex for WaitEvent, second, Failed, multiple procs modifying data.");
    this.debug = true;

    ProcessEvent ev = this.eventQueue.poll();
    this.debug = false;
    return ev;
  }

  public synchronized ProcessEvent peekEvent()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.id + " Mutex for WaitEvent Failed, multiple procs modifying data.");
    this.debug = true;

    ProcessEvent ev = null;
    if (!this.eventQueue.isEmpty())
    {
      ev = this.eventQueue.poll();
    }

    this.debug = false;
    return ev;
  }

  public void putEvent(ProcessEvent ev)
  {
    this.putEvent(ev, true);
  }

  public synchronized void putEvent(ProcessEvent ev, boolean important)
  {
    boolean go = true;
    if (!important)
    {
      if (this.eventQueue.size() > 20)
      {
        go = false;
      }
    }

    if (go)
    {
      if (this.debug) System.out.println("ERROR: Proc: " + this.id + " Mutex for WaitEvent Failed, multiple procs modifying data.");
      this.debug = true;

      int queueSize = this.eventQueue.size();

      this.eventQueue.add(ev);

      if (queueSize > this.warningThresholdEventQueue)
      {
        System.out.println("Warning, EventQueue of process " + this.id + " has " + this.eventQueue.size() + " unread events.");
        this.warningThresholdEventQueue *= 2;
      }

      if (this.state == WAITING)
      {
        this.resume();
      }

      this.debug = false;
    }
  }

  public int getState()
  {
    return this.state;
  }

  public void setState(int state)
  {
    this.state = state;
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
