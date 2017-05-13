package com.github.malow.malowlib.namedmutex;

import java.util.ArrayList;
import java.util.Arrays;
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

  public static NamedMutex getAndLockByClassAndId(Class<?> clazz, int id)
  {
    return getAndLockByName(classAndIdToString(clazz, id));
  }

  public static NamedMutex getAndLockByClassAndName(Class<?> clazz, String name)
  {
    return getAndLockByName(classAndNameToString(clazz, name));
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
  public static synchronized NamedMutexList getAndLockMultipleByClassAndIds(Class<?> clazz, Integer... ids)
  {
    return getAndLockMultipleByNames(Arrays.stream(ids).map(id -> classAndIdToString(clazz, id)).toArray(String[]::new));
  }

  public static synchronized NamedMutexList getAndLockMultipleByClassAndIds(Class<?> clazz, String... names)
  {
    return getAndLockMultipleByNames(Arrays.stream(names).map(name -> classAndNameToString(clazz, name)).toArray(String[]::new));
  }

  public static synchronized NamedMutexList getAndLockMultipleByNames(String... names)
  {
    ArrayList<NamedMutex> locks = new ArrayList<NamedMutex>();
    for (String name : names)
    {
      locks.add(NamedMutexHandler.getAndLockByName(name));
    }
    return new NamedMutexList(locks);
  }

  private static String classAndIdToString(Class<?> clazz, int id)
  {
    return clazz.getSimpleName() + ":" + id;
  }

  private static String classAndNameToString(Class<?> clazz, String name)
  {
    return clazz.getSimpleName() + ":" + name;
  }
}