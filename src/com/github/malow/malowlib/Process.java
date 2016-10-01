package com.github.malow.malowlib;

//import java.util.ArrayDeque;
import java.util.LinkedList;

public abstract class Process
{
  class ProcThread extends Thread
  {
    @Override
    public void run()
    {
      state = RUNNING;
      Life();
      state = FINISHED;
    }

    public synchronized void Resume()
    {
      try
      {
        notify();
      }
      catch (Exception E)
      {
        System.out.println("ThreadResume failed");
      }
    }

    public synchronized void Suspend()
    {
      try
      {
        wait();
      }
      catch (Exception E)
      {
        System.out.println("ThreadSuspend failed");
      }
    }
  }

  public static final int NOT_STARTED = 0, WAITING = 1, RUNNING = 2, FINISHED = 3;
  private static final int DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL = 250;

  private static long nextPID = 0;

  private ProcThread proc;
  private LinkedList<ProcessEvent> eventQueue;
  private int state;
  private int warningThresholdEventQueue = DEFAULT_WARNING_THRESHOLD_EVENTQUEUE_FULL;
  private long ID;

  private boolean debug = false;

  protected boolean stayAlive = true;

  public Process()
  {
    this.ID = Process.nextPID;
    Process.nextPID++;
    this.state = NOT_STARTED;
    this.eventQueue = new LinkedList<ProcessEvent>();
    this.proc = new ProcThread();
  }

  public abstract void Life();

  public void Start()
  {
    if (this.state == NOT_STARTED)
    {
      this.proc.start();
    }
  }

  public void Suspend()
  {
    this.proc.Suspend();
  }

  public void Resume()
  {
    // Needed because WaitEvent is not completely synchronized, so if a thread wants to resume it while it's 
    // going to sleep we need to continuously call on it to restart until it responds. Either solve this a better way or start using 
    // PeekEvent() instead of WaitEvent() more and add a sleep between Peeks.
    while (this.state == WAITING)
      this.proc.Resume();
  }

  public void Close()
  {
    this.stayAlive = false;
    ProcessEvent ev = new ProcessEvent();
    this.PutEvent(ev);
    this.CloseSpecific();
  }

  public void CloseSpecific()
  {

  }

  public void WaitUntillDone()
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

  public ProcessEvent WaitEvent()
  {
    boolean sleep = this.WaitEventCheckForSleep();

    if (sleep)
    {
      this.Suspend();
      this.state = RUNNING;
    }

    return this.WaitEventDequeEvent();
  }

  private synchronized boolean WaitEventCheckForSleep()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.ID + " Mutex for WaitEvent Failed, multiple procs modifying data.");
    this.debug = true;
    boolean sleep = this.eventQueue.isEmpty();

    if (sleep)
    {
      this.state = WAITING;
    }

    this.debug = false;
    return sleep;
  }

  private synchronized ProcessEvent WaitEventDequeEvent()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.ID + " Mutex for WaitEvent, second, Failed, multiple procs modifying data.");
    this.debug = true;

    ProcessEvent ev = this.eventQueue.poll();
    this.debug = false;
    return ev;
  }

  public synchronized ProcessEvent PeekEvent()
  {
    if (this.debug) System.out.println("ERROR: Proc: " + this.ID + " Mutex for WaitEvent Failed, multiple procs modifying data.");
    this.debug = true;

    ProcessEvent ev = null;
    if (!this.eventQueue.isEmpty())
    {
      ev = this.eventQueue.poll();
    }

    this.debug = false;
    return ev;
  }

  public void PutEvent(ProcessEvent ev)
  {
    this.PutEvent(ev, true);
  }

  public synchronized void PutEvent(ProcessEvent ev, boolean important)
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
      if (this.debug) System.out.println("ERROR: Proc: " + this.ID + " Mutex for WaitEvent Failed, multiple procs modifying data.");
      this.debug = true;

      int queueSize = this.eventQueue.size();

      this.eventQueue.add(ev);

      if (queueSize > this.warningThresholdEventQueue)
      {
        System.out.println("Warning, EventQueue of process " + this.ID + " has " + this.eventQueue.size() + " unread events.");
        this.warningThresholdEventQueue *= 2;
      }

      if (this.state == WAITING)
      {
        this.Resume();
      }

      this.debug = false;
    }
  }

  public int GetState()
  {
    return this.state;
  }

  public void SetState(int state)
  {
    this.state = state;
  }

  public long GetID()
  {
    return this.ID;
  }

  public long GetNrOfProcs()
  {
    return Process.nextPID;
  }

  public int GetEventQueueSize()
  {
    return this.eventQueue.size();
  }
}
