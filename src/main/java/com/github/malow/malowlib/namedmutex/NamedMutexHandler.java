package com.github.malow.malowlib.namedmutex;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * Handles the creation of NamedMutex. Stores all mutexes statically meaning that memory will never be released for them. <br>
 * Be warned: All the mutexes are stored statically by the handler, so memory usage will continue to grow over usage.<br>
 * <br>
 * Usage example: <br>
 * Single Mutex: <br>
 * {@code NamedMutex mutex = NamedMutexHandler.getAndLockByName("test");} <br>
 * {@code exclusiveData += 54;} <br>
 * {@code TestClass.exclusiveMethodCall(32);} <br>
 * {@code mutex.unlock();} <br>
 * <br>
 * Multiple Mutexes: <br>
 * {@code String[] mutexNames = {"testMutex1", "testMutex2", "testMutex3"};} <br>
 * {@code NamedMutexList mutexes = NamedMutexHandler.getAndLockMultipleLocksByNames(mutexNames);} <br>
 * {@code SharedData.data += x;} <br>
 * {@code mutexes.unlockAll();} <br>
 */
public class NamedMutexHandler
{
  private NamedMutexHandler()
  {

  }

  private static ConcurrentHashMap<String, StampedLock> locks = new ConcurrentHashMap<String, StampedLock>();

  public static void resetAllMutexes()
  {
    NamedMutexHandler.locks = new ConcurrentHashMap<String, StampedLock>();
  }

  public static NamedMutex getAndLockByName(String name)
  {
    StampedLock lock = locks.get(name);
    if (lock == null)
    {
      lock = new StampedLock();
      StampedLock existingLock = locks.putIfAbsent(name, lock);
      if (existingLock != null)
      {
        lock = existingLock;
      }
    }
    long stamp = lock.writeLock();
    return new NamedMutex(lock, name, stamp);
  }

  /**
   * Locks and returns multiple Mutexes at once. This method is to be used when multiple locks are needed at the same time for something. The method is
   * synchronized meaning that performance wont be great if you have multiple threads calling this often.
   */
  public static synchronized NamedMutexList getAndLockMultipleByNames(String... names)
  {
    System.out.println("Getting multiple");
    ArrayList<NamedMutex> locks = new ArrayList<NamedMutex>();
    for (String name : names)
    {
      locks.add(NamedMutexHandler.getAndLockByName(name));
    }
    System.out.println("Got em");
    return new NamedMutexList(locks);
  }
}