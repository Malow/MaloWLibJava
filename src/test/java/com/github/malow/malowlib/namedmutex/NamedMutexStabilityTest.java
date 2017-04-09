package com.github.malow.malowlib.namedmutex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

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

  public static final int THREAD_COUNT = 1000;
  public static final int DATA_COUNT = 1000;
  public static final int NR_OF_DATA_PER_OPERATION = 10;
  public static final int TEST_LENGTH_MS = 5000;

  @Test
  public void stabilityThreadedTest() throws Exception
  {
    SharedData.reset();
    ArrayList<DataOperationThread> runners = new ArrayList<DataOperationThread>();
    ArrayList<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      LockType lockType = i % 5 == 0 ? LockType.NAMED_MUTEX_MULTIPLE : LockType.NAMED_MUTEX;
      DataOperationThread r = new DataOperationThread(lockType);
      Thread t = new Thread(r);
      runners.add(r);
      threads.add(t);
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      threads.get(i).start();
    }
    Thread.sleep(TEST_LENGTH_MS);
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      runners.get(i).stop();
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      threads.get(i).join();
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
}
