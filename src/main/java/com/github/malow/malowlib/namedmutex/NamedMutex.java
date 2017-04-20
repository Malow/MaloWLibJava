package com.github.malow.malowlib.namedmutex;

import java.util.concurrent.locks.StampedLock;

/**
 * Wraps a StampedLock adding a name property. The inner lock can be access via getLock if more advanced operations than just unlock is needed. Constructor is
 * protected to ensure that this class is only instantiated by the NamedMutexHandler.
 */
public class NamedMutex
{
  private StampedLock lock;
  private String name;
  private long stamp;

  protected NamedMutex(StampedLock lock, String name, long stamp)
  {
    this.lock = lock;
    this.name = name;
    this.stamp = stamp;
  }

  public void unlock()
  {
    this.lock.unlockWrite(this.stamp);
  }

  public String getName()
  {
    return this.name;
  }

  public StampedLock getLock()
  {
    return this.lock;
  }
}