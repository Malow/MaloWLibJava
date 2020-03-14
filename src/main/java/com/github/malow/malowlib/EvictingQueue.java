package com.github.malow.malowlib;

import java.util.LinkedList;

/**
 * A queue with a fixed size that removes the oldest element when full and a new element is added
 */
public class EvictingQueue<E> extends LinkedList<E>
{
  private static final long serialVersionUID = -3241469702499987437L;

  private int limit;

  public EvictingQueue(int limit)
  {
    this.limit = limit;
  }

  @Override
  public boolean add(E o)
  {
    boolean added = super.add(o);
    while (added && this.size() > this.limit)
    {
      super.remove();
    }
    return added;
  }
}
