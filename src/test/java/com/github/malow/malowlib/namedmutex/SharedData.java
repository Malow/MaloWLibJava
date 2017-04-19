package com.github.malow.malowlib.namedmutex;

public class SharedData
{

  private static int data[] = new int[NamedMutexStabilityTest.DATA_COUNT];

  public static void reset()
  {
    SharedData.data = new int[NamedMutexStabilityTest.DATA_COUNT];
  }

  public static void increment(String accId, int x)
  {
    SharedData.data[Integer.parseInt(accId)] += x;
  }

  public static synchronized void incrementSynchronized(String accId, int x)
  {
    SharedData.data[Integer.parseInt(accId)] += x;
  }

  public static void incrementNamedMutex(String accId, int x)
  {
    NamedMutex mutex = NamedMutexHandler.getAndLockByName(accId);
    SharedData.data[Integer.parseInt(accId)] += x;
    mutex.unlock();
  }

  public static void incrementMultipleNamedMutexes(int x, String... accIds)
  {
    NamedMutexList mutexes = NamedMutexHandler.getAndLockMultipleByNames(accIds);
    for (String accId : accIds)
    {
      SharedData.data[Integer.parseInt(accId)] += x;
    }
    System.out.println("releasing all");
    mutexes.unlockAll();
  }

  public static int getData(int i)
  {
    return SharedData.data[i];
  }
}