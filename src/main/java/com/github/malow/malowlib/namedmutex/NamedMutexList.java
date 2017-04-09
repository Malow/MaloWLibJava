package com.github.malow.malowlib.namedmutex;

import java.util.ArrayList;

/**
 *
 * A wrapper class around an ArrayList of NamedMutex, adds an unlockAll method for convenience. The mutexes stored in this class are publicly available and as
 * such can be modified freely to allow for releasing mutexes individually if needed.
 *
 */
public class NamedMutexList
{
  public ArrayList<NamedMutex> mutexes;

  public NamedMutexList(ArrayList<NamedMutex> mutexes)
  {
    this.mutexes = mutexes;
  }

  /**
   * Unlocks all the mutexes stored in this list.
   */
  public void unlockAll()
  {
    for (NamedMutex lock : this.mutexes)
    {
      lock.unlock();
    }
  }
}
