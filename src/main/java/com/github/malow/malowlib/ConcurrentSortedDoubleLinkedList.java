package com.github.malow.malowlib;

import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSortedDoubleLinkedList<T extends Comparable<T>>
{

  public static class Node<T>
  {
    public Node<T> previous;
    public Node<T> next;
    public T item;

    public Node(T item)
    {
      this.item = item;
    }

    @Override
    public String toString()
    {
      String str = this.item.toString();
      if (this.previous != null)
      {
        str += "; Previous: " + this.previous.item.toString();
      }
      if (this.next != null)
      {
        str += "; Next: " + this.next.item.toString();
      }
      return str;
    }
  }

  protected Node<T> first;
  private ReentrantLock lock = new ReentrantLock();
  private int size = 0;

  protected void lock()
  {
    this.lock.lock();
  }

  protected void unlock()
  {
    this.lock.unlock();
  }

  public int getSize()
  {
    return this.size;
  }

  public void add(T item)
  {
    this.lock();
    this.size++;
    if (this.first != null)
    {
      this.insertSorted(item);
    }
    else
    {
      this.first = new Node<T>(item);
    }
    this.unlock();
  }

  public boolean remove(T item)
  {
    this.lock();
    Node<T> result = this.search(item);
    if (result != null)
    {
      if (result.previous != null)
      {
        result.previous.next = result.next;
      }
      if (result.next != null)
      {
        result.next.previous = result.previous;
      }
      this.size--;
      this.unlock();
      return true;
    }
    this.unlock();
    return false;
  }

  protected void remove(Node<T> toRemove)
  {
    if (toRemove.next != null)
    {
      toRemove.next.previous = toRemove.previous;
    }
    if (toRemove.previous != null)
    {
      toRemove.previous.next = toRemove.next;
    }
    else
    {
      this.first = toRemove.next;
    }
    this.size--;
  }

  // Non-recursive is faster, so this has been deprecated.
  @SuppressWarnings("unused")
  private Node<T> searchRecursive(Node<T> current, T item)
  {
    if (current != null)
    {
      if (current.item.equals(item))
      {
        return current;
      }
      return this.searchRecursive(current.next, item);
    }
    return null;
  }

  private Node<T> search(T item)
  {
    Node<T> current = this.first;
    while (current != null)
    {
      if (current.item.equals(item))
      {
        return current;
      }
      current = current.next;
    }
    return null;
  }

  // Non-recursive is faster, so this has been deprecated.
  @SuppressWarnings("unused")
  private void insertSortedRecursive(Node<T> current, T item)
  {
    if (item.compareTo(current.item) > 0)
    {
      if (current.next != null)
      {
        this.insertSortedRecursive(current.next, item);
      }
      else
      {
        Node<T> newNode = new Node<T>(item);
        current.next = newNode;
        newNode.previous = current;
      }
    }
    else
    {
      Node<T> newNode = new Node<T>(item);
      newNode.next = current;
      if (current.previous != null)
      {
        newNode.previous = current.previous;
        current.previous.next = newNode;
      }
      else
      {
        this.first = newNode;
      }
      current.previous = newNode;
    }
  }

  private void insertSorted(T item)
  {
    Node<T> newNode = new Node<T>(item);
    Node<T> current = this.first;
    while (current != null)
    {
      if (item.compareTo(current.item) > 0)
      {
        if (current.next == null)
        {
          current.next = newNode;
          newNode.previous = current;
          return;
        }
      }
      else
      {
        newNode.next = current;
        if (current.previous != null)
        {
          newNode.previous = current.previous;
          current.previous.next = newNode;
        }
        else
        {
          this.first = newNode;
        }
        current.previous = newNode;
        return;
      }
      current = current.next;
    }

  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    Node<T> current = this.first;
    while (current != null)
    {
      builder.append(current.toString() + "\n");
      current = current.next;
    }
    return builder.toString();
  }
}