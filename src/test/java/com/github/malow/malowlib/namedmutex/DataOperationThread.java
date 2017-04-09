package com.github.malow.malowlib.namedmutex;

import java.util.ArrayList;
import java.util.Random;

import com.github.malow.malowlib.namedmutex.NamedMutexStabilityTest.LockType;

public class DataOperationThread implements Runnable
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
      ArrayList<String> dataIds = new ArrayList<String>();
      for (int i = 0; i < NamedMutexStabilityTest.NR_OF_DATA_PER_OPERATION; i++)
      {
        dataIds.add("" + rand(NamedMutexStabilityTest.DATA_COUNT));
      }
      int x = DataOperationThread.rand(10);
      if (this.lockType == LockType.NAMED_MUTEX_MULTIPLE)
      {
        String[] stockArr = new String[dataIds.size()];
        dataIds.toArray(stockArr);
        SharedData.incrementMultipleNamedMutexes(x, stockArr);
      }
      for (String accId : dataIds)
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
    }
  }

  public void stop()
  {
    this.go = false;
  }

  public double getData(int count)
  {
    return this.myData[count];
  }
}