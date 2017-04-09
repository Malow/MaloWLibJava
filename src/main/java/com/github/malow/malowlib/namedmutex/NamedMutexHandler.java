package com.github.malow.malowlib.namedmutex;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles the creation of NamedMutex. Stores all mutexes statically meaning that memory will never be released for them. <br>
 * <br>
 * Usage example: <br>
 * {@code NamedMutex mutex = NamedMutexHandler.getAndLockByName("test");} <br>
 * {@code exclusiveData += 54;} <br>
 * {@code TestClass.exclusiveMethodCall(32);} <br>
 * {@code mutex.unlock();}
 *
 */
public class NamedMutexHandler
{
  private NamedMutexHandler()
  {

  }

  private static ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

  public static void resetAllMutexes()
  {
    NamedMutexHandler.locks = new ConcurrentHashMap<String, ReentrantLock>();
  }

  public static NamedMutex getAndLockByName(String name)
  {
    ReentrantLock lock = locks.get(name);
    if (lock == null)
    {
      lock = new ReentrantLock();
      ReentrantLock existingLock = locks.putIfAbsent(name, lock);
      if (existingLock != null)
      {
        lock = existingLock;
      }
    }
    lock.lock();
    return new NamedMutex(lock, name);
  }

  /**
   * Locks and returns multiple Mutexes at once. This method is to be used when multiple locks are needed at the same time for something. The method is
   * synchronized meaning that performance wont be great if you have multiple threads calling this often.
   */
  public static synchronized NamedMutexList getAndLockMultipleLocksByNames(String... names)
  {
    ArrayList<NamedMutex> locks = new ArrayList<NamedMutex>();
    for (String name : names)
    {
      locks.add(NamedMutexHandler.getAndLockByName(name));
    }
    return new NamedMutexList(locks);
  }
}