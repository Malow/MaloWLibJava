package com.github.malow.malowlib.malowprocess;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.malow.malowlib.RandomNumberGenerator;

public class MaloWProcessFixture
{
  protected static class DataPacket extends ProcessEvent
  {
    public long nr;

    public DataPacket(long nr)
    {
      this.nr = nr;
    }
  }

  protected static class CloseEvent extends ProcessEvent
  {

  }

  protected static class Consumer extends MaloWProcess
  {
    private long myCount;

    public Consumer()
    {
      this.myCount = 0;
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof DataPacket)
        {
          this.myCount += ((DataPacket) ev).nr;
        }
        else if (ev instanceof CloseEvent)
        {
          this.close();
        }
      }
    }

    public long getCount()
    {
      return this.myCount;
    }
  }

  protected static class Producer extends MaloWProcess
  {
    private MaloWProcess target;
    private long myCount;

    public Producer(MaloWProcess target)
    {
      this.target = target;
      this.myCount = 0;
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        long nr = RandomNumberGenerator.getRandomInt(0, 10);
        this.myCount += nr;
        DataPacket dp = new DataPacket(nr);
        this.target.putEvent(dp);
      }
    }

    public long getCount()
    {
      return this.myCount;
    }
  }

  protected static class SleepProcess extends MaloWProcess
  {
    public SleepProcess(int threadCount)
    {
      super(threadCount);
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        this.waitEvent();
        try
        {
          Thread.sleep(100);
        }
        catch (InterruptedException e)
        {
        }
      }
    }
  }

  protected static class MultiThreadProcess extends MaloWProcess
  {
    private int iterationsPerThread;
    public AtomicInteger count = new AtomicInteger();

    public MultiThreadProcess(int threadCount, int iterationsPerThread)
    {
      super(threadCount);
      this.iterationsPerThread = iterationsPerThread;
    }

    @Override
    public void life()
    {
      for (int i = 0; i < this.iterationsPerThread; i++)
      {
        this.count.incrementAndGet();
      }
    }
  }
}