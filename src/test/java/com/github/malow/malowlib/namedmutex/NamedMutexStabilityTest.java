package com.github.malow.malowlib.namedmutex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class NamedMutexStabilityTest
{
  public enum LockType
  {
    NO_LOCK,
    SYNCHRONIZED,
    NAMED_MUTEX,
    NAMED_MUTEX_MULTIPLE
  }

  public static final int THREAD_COUNT = 5;
  public static final int DATA_COUNT = 1000;
  public static final int NR_OF_DATA_PER_OPERATION = 10;
  public static final int TEST_LENGTH_MS = 5000;

  @Test
  public void realisticStabilityTest() throws Exception
  {
    SharedData.reset();
    ArrayList<DataOperationThread> runners = new ArrayList<DataOperationThread>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      LockType lockType = i % 5 == 0 ? LockType.NAMED_MUTEX_MULTIPLE : LockType.NAMED_MUTEX;
      DataOperationThread r = new DataOperationThread(lockType);
      runners.add(r);
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      runners.get(i).start();
    }
    Thread.sleep(TEST_LENGTH_MS);
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      runners.get(i).close();
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      runners.get(i).join();
    }
    for (int i = 0; i < DATA_COUNT; i++)
    {
      int result = 0;
      for (int u = 0; u < THREAD_COUNT; u++)
      {
        result += runners.get(u).getData(i);
      }
      assertThat(result).isEqualTo(SharedData.getData(i));
    }
  }

  private static final int RUNNER_COUNT = 10;
  private static final int ITERATIONS = 10;
  private static final int MULTI_RUNS = 100000;
  private static final int SINGLE_RUNS = 2000000;
  private volatile boolean[] singleStuck = new boolean[RUNNER_COUNT];
  private volatile boolean[] multiStuck = new boolean[RUNNER_COUNT];
  private volatile long count1 = 0;
  private volatile long count2 = 0;
  private volatile long count3 = 0;

  public class MultiRunner extends Thread
  {
    private int id;
    public boolean done = false;

    public MultiRunner(int id)
    {
      this.id = id;
    }

    @Override
    public void run()
    {
      for (int u = 0; u < ITERATIONS; u++)
      {
        for (int i = 0; i < MULTI_RUNS; i++)
        {
          NamedMutexList m = NamedMutexHandler.getAndLockMultipleByNames("count1", "count2", "count3");
          NamedMutexStabilityTest.this.count1++;
          NamedMutexStabilityTest.this.count2++;
          NamedMutexStabilityTest.this.count3++;
          m.unlockAll();
        }
        NamedMutexStabilityTest.this.multiStuck[this.id] = false;
        try
        {
          Thread.sleep(1);
        }
        catch (InterruptedException e)
        {
        }
      }
      this.done = true;
      System.out.println("MultiRunner" + this.id + " completed");
    }
  }

  public class SingleRunner extends Thread
  {
    private int id;
    public boolean done = false;

    public SingleRunner(int id)
    {
      this.id = id;
    }

    @Override
    public void run()
    {
      for (int u = 0; u < ITERATIONS; u++)
      {
        for (int i = 0; i < SINGLE_RUNS; i++)
        {
          NamedMutex m1 = NamedMutexHandler.getAndLockByName("count1");
          NamedMutexStabilityTest.this.count1++;
          m1.unlock();
          NamedMutex m2 = NamedMutexHandler.getAndLockByName("count2");
          NamedMutexStabilityTest.this.count2++;
          m2.unlock();
          NamedMutex m3 = NamedMutexHandler.getAndLockByName("count3");
          NamedMutexStabilityTest.this.count3++;
          m3.unlock();
        }
        NamedMutexStabilityTest.this.singleStuck[this.id] = false;
        try
        {
          Thread.sleep(1);
        }
        catch (InterruptedException e)
        {
        }
      }
      this.done = true;
      System.out.println("SingleRunner" + this.id + " completed");
    }
  }

  private List<MultiRunner> multiRunners = new ArrayList<>();
  private List<SingleRunner> singleRunners = new ArrayList<>();

  @Test
  public void maximizedLoadStabilityTest() throws InterruptedException
  {
    for (int i = 0; i < RUNNER_COUNT; i++)
    {
      this.multiRunners.add(new MultiRunner(i));
      this.singleRunners.add(new SingleRunner(i));
    }
    for (int i = 0; i < RUNNER_COUNT; i++)
    {
      this.multiRunners.get(i).start();
      this.singleRunners.get(i).start();
    }
    while (!this.isAllDone())
    {
      for (int i = 0; i < RUNNER_COUNT; i++)
      {
        this.singleStuck[i] = true;
        this.multiStuck[i] = true;
      }
      Thread.sleep(3000);
      System.out.println("");
      System.out.println("Update:");
      for (int i = 0; i < RUNNER_COUNT; i++)
      {
        if (this.singleStuck[i] && !this.singleRunners.get(i).done)
        {
          System.out.println("SingleRunner " + i + " is possibly stuck");
        }
        if (this.multiStuck[i] && !this.multiRunners.get(i).done)
        {
          System.out.println("MultiRunner " + i + " is possibly stuck");
        }
      }
      System.out.println("count1: " + this.count1);
      System.out.println("count2: " + this.count2);
      System.out.println("count3: " + this.count3);
    }
    long expectedCount = RUNNER_COUNT * ITERATIONS * SINGLE_RUNS + RUNNER_COUNT * ITERATIONS * MULTI_RUNS;
    assertThat(this.count1).isEqualTo(expectedCount);
    assertThat(this.count2).isEqualTo(expectedCount);
    assertThat(this.count3).isEqualTo(expectedCount);
    System.out.println("Test completed successfully!");
    System.out.println("count1: " + this.count1);
    System.out.println("count2: " + this.count2);
    System.out.println("count3: " + this.count3);
  }

  private boolean isAllDone()
  {
    for (int i = 0; i < RUNNER_COUNT; i++)
    {
      if (!this.multiRunners.get(i).done)
      {
        return false;
      }
      if (!this.singleRunners.get(i).done)
      {
        return false;
      }
    }
    return true;
  }
}