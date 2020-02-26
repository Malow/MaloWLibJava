package com.github.malow.malowlib.malowprocess;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameterAndReturn;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithReturn;

public class ThreadPool<T, S> extends MaloWProcess
{
  public ThreadPool(int threadCount)
  {
    super(threadCount);
    this.start();
  }

  private class StartParameterizedWorkEvent extends ProcessEvent
  {
    public CheckedFunctionWithParameterAndReturn<T, S> function;
    public T parameter;
    public Future<S> future;
  }

  private class StartWorkEvent extends ProcessEvent
  {
    public CheckedFunctionWithReturn<S> function;
    public Future<S> future;
  }

  public static class Future<S>
  {
    private S returnValue;
    private Exception exception;

    private volatile boolean finished = false;

    public boolean isDone()
    {
      return this.finished;
    }

    public synchronized S waitForResult() throws Exception
    {
      if (!this.finished)
      {
        this.wait();
      }

      // Sleep the thread
      if (this.exception != null)
      {
        throw this.exception;
      }
      return this.returnValue;
    }

    private synchronized void addResult(S returnValue, Exception exception)
    {
      this.returnValue = returnValue;
      this.exception = exception;
      this.finished = true;
      this.notifyAll();
    }
  }

  public Future<S> StartWork(CheckedFunctionWithReturn<S> function)
  {
    Future<S> future = new Future<>();
    StartWorkEvent ev = new StartWorkEvent();
    ev.function = function;
    ev.future = future;
    this.putEvent(ev);
    return future;
  }

  public Future<S> StartWork(CheckedFunctionWithParameterAndReturn<T, S> function, T parameter)
  {
    Future<S> future = new Future<>();
    StartParameterizedWorkEvent ev = new StartParameterizedWorkEvent();
    ev.function = function;
    ev.parameter = parameter;
    ev.future = future;
    this.putEvent(ev);
    return future;
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      ProcessEvent ev = this.waitEvent();
      if (ev == null)
      {
        continue;
      }
      if (ev instanceof ThreadPool.StartParameterizedWorkEvent)
      {
        @SuppressWarnings("unchecked")
        StartParameterizedWorkEvent swe = StartParameterizedWorkEvent.class.cast(ev);
        try
        {
          S ret = swe.function.apply(swe.parameter);
          swe.future.addResult(ret, null);
        }
        catch (Exception e)
        {
          swe.future.addResult(null, e);
        }
      }
      else if (ev instanceof ThreadPool.StartWorkEvent)
      {
        @SuppressWarnings("unchecked")
        StartWorkEvent swe = StartWorkEvent.class.cast(ev);
        try
        {
          S ret = swe.function.apply();
          swe.future.addResult(ret, null);
        }
        catch (Exception e)
        {
          swe.future.addResult(null, e);
        }
      }
    }
  }
}