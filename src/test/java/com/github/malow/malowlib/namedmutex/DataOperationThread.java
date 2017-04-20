package com.github.malow.malowlib.namedmutex;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.github.malow.malowlib.namedmutex.NamedMutexStabilityTest.LockType;

public class DataOperationThread extends Thread
{

  public static int rand(int x)
  {
    Random rand = new Random();
    return rand.nextInt(x);
  }

  private int myData[] = new int[NamedMutexStabilityTest.DATA_COUNT];
  private LockType lockType;
  private boolean go;

  public DataOperationThread(LockType lockType)
  {
    this.lockType = lockType;
    this.go = true;
  }

  @Override
  public void run()
  {
    while (this.go)
    {
      Set<String> accIds = new HashSet<String>();
      for (int i = 0; i < NamedMutexStabilityTest.NR_OF_DATA_PER_OPERATION; i++)
      {
        int x = rand(NamedMutexStabilityTest.DATA_COUNT);
        while (accIds.contains("" + x))
        {
          x = rand(NamedMutexStabilityTest.DATA_COUNT);
        }
        accIds.add("" + x);
      }
      int x = DataOperationThread.rand(10);
      if (this.lockType == LockType.NAMED_MUTEX_MULTIPLE)
      {
        String[] stockArr = new String[accIds.size()];
        accIds.toArray(stockArr);
        SharedData.incrementMultipleNamedMutexes(x, stockArr);
      }
      for (String accId : accIds)
      {
        this.myData[Integer.parseInt(accId)] += x;
        if (this.lockType == LockType.NO_LOCK)
        {
          SharedData.increment(accId, x);
        }
        else if (this.lockType == LockType.SYNCHRONIZED)
        {
          SharedData.incrementSynchronized(accId, x);
        }
        else if (this.lockType == LockType.NAMED_MUTEX)
        {
          SharedData.incrementNamedMutex(accId, x);
        }
      }
      try
      {
        Thread.sleep(1);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  public void close()
  {
    this.go = false;
  }

  public double getData(int count)
  {
    return this.myData[count];
  }
}