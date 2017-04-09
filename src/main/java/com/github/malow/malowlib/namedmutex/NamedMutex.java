package com.github.malow.malowlib.namedmutex;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * Wraps a ReentrantLock adding a name property. The inner lock can be access via getLock if more advanced operations than just unlock is needed. Constructor is
 * protected to ensure that this class is only instantiated by the NamedMutexHandler.
 *
 */
public class NamedMutex
{

  private ReentrantLock lock;
  private String name;

  protected NamedMutex(ReentrantLock lock, String name)
  {
    this.lock = lock;
    this.name = name;
  }

  public void unlock()
  {
    this.lock.unlock();
  }

  public String getName()
  {
    return this.name;
  }

  public ReentrantLock getLock()
  {
    return this.lock;
  }
}
