package com.github.malow.malowlib.malowprocess;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.RandomNumberGenerator;

public class MaloWProcessTest
{
  private static class DataPacket extends ProcessEvent
  {
    public long nr;

    public DataPacket(long nr)
    {
      this.nr = nr;
    }
  }

  private static class CloseEvent extends ProcessEvent
  {

  }

  private static class Consumer extends MaloWProcess
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

    @Override
    public void closeSpecific()
    {
    }

    public long getCount()
    {
      return this.myCount;
    }
  }

  private static class Producer extends MaloWProcess
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

    @Override
    public void closeSpecific()
    {
    }

    public long getCount()
    {
      return this.myCount;
    }
  }

  private static final int NR_OF_PRODUCERS = 1000;
  private static final int RUN_FOR_MS = 2000;

  @Test
  public void testThatPutEventAndWaitEventIsThreadSafe() throws Exception
  {
    Consumer consumer = new Consumer();
    consumer.start();
    List<Producer> producers = new ArrayList<Producer>();
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.add(new Producer(consumer));
    }
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).start();
    }

    Thread.sleep(RUN_FOR_MS);

    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).close();
    }
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).waitUntillDone();
    }
    consumer.putEvent(new CloseEvent());
    consumer.waitUntillDone();

    long totalCount = 0;
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      totalCount += producers.get(i).getCount();
    }
    assertEquals(totalCount, consumer.getCount());
  }
}
