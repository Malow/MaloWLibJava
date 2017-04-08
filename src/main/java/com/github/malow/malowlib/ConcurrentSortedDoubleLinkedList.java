package com.github.malow.malowlib;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentSortedDoubleLinkedList<T extends Comparable<T>>
{

  public static class Node<T>
  {
    public Optional<Node<T>> previous = Optional.empty();
    public Optional<Node<T>> next = Optional.empty();
    public T item;

    public Node(T item)
    {
      this.item = item;
    }

    @Override
    public String toString()
    {
      String str = this.item.toString();
      if (this.previous.isPresent())
      {
        str += "; Previous: " + this.previous.get().item.toString();
      }
      if (this.next.isPresent())
      {
        str += "; Next: " + this.next.get().item.toString();
      }
      return str;
    }
  }

  protected Optional<Node<T>> first = Optional.empty();
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
    if (this.first.isPresent())
    {
      this.insertSorted(item);
    }
    else
    {
      this.first = Optional.of(new Node<T>(item));
    }
    this.unlock();
  }

  public boolean remove(T item)
  {
    this.lock();
    Optional<Node<T>> result = this.search(item);
    if (result.isPresent())
    {
      Node<T> toRemove = result.get();
      if (toRemove.previous.isPresent())
      {
        toRemove.previous.get().next = toRemove.next;
      }
      if (toRemove.next.isPresent())
      {
        toRemove.next.get().previous = toRemove.previous;
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
    if (toRemove.next.isPresent())
    {
      toRemove.next.get().previous = toRemove.previous;
    }
    if (toRemove.previous.isPresent())
    {
      toRemove.previous.get().next = toRemove.next;
    }
    else
    {
      this.first = toRemove.next;
    }
    this.size--;
  }

  // Non-recursive is faster, so this has been deprecated.
  @SuppressWarnings("unused")
  private Optional<Node<T>> searchRecursive(Optional<Node<T>> current, T item)
  {
    if (current.isPresent())
    {
      if (current.get().item.equals(item))
      {
        return current;
      }
      return this.searchRecursive(current.get().next, item);
    }
    return Optional.empty();
  }

  private Optional<Node<T>> search(T item)
  {
    Optional<Node<T>> current = this.first;
    while (current.isPresent())
    {
      if (current.get().item.equals(item))
      {
        return current;
      }
      current = current.get().next;
    }
    return Optional.empty();
  }

  // Non-recursive is faster, so this has been deprecated.
  @SuppressWarnings("unused")
  private void insertSortedRecursive(Node<T> current, T item)
  {
    if (item.compareTo(current.item) > 0)
    {
      if (current.next.isPresent())
      {
        this.insertSortedRecursive(current.next.get(), item);
      }
      else
      {
        Node<T> newNode = new Node<T>(item);
        current.next = Optional.of(newNode);
        newNode.previous = Optional.of(current);
      }
    }
    else
    {
      Node<T> newNode = new Node<T>(item);
      newNode.next = Optional.of(current);
      if (current.previous.isPresent())
      {
        newNode.previous = current.previous;
        current.previous.get().next = Optional.of(newNode);
      }
      else
      {
        this.first = Optional.of(newNode);
      }
      current.previous = Optional.of(newNode);
    }
  }

  private void insertSorted(T item)
  {
    Node<T> newNode = new Node<T>(item);
    Optional<Node<T>> current = this.first;
    while (current.isPresent())
    {
      if (item.compareTo(current.get().item) > 0)
      {
        if (!current.get().next.isPresent())
        {
          current.get().next = Optional.of(newNode);
          newNode.previous = current;
          return;
        }
      }
      else
      {
        newNode.next = current;
        if (current.get().previous.isPresent())
        {
          newNode.previous = current.get().previous;
          current.get().previous.get().next = Optional.of(newNode);
        }
        else
        {
          this.first = Optional.of(newNode);
        }
        current.get().previous = Optional.of(newNode);
        return;
      }
      current = current.get().next;
    }

  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    Optional<Node<T>> current = this.first;
    while (current.isPresent())
    {
      builder.append(current.get().toString() + "\n");
      current = current.get().next;
    }
    return builder.toString();
  }
}