package com.github.malow.malowlib;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.github.malow.malowlib.ConcurrentSortedDoubleLinkedList.Node;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class ConcurrentSortedDoubleLinkedListTest
{
  private static class TestClass implements Comparable<TestClass>
  {
    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((i == null) ? 0 : i.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TestClass other = (TestClass) obj;
      if (i == null)
      {
        if (other.i != null) return false;
      }
      else if (!i.equals(other.i)) return false;
      return true;
    }

    public Integer i;

    public TestClass(Integer i)
    {
      this.i = i;
    }

    @Override
    public int compareTo(TestClass o)
    {
      return i.compareTo(o.i);
    }

  }

  private static ConcurrentSortedDoubleLinkedList<TestClass> list;

  @Before
  public void init()
  {
    list = new ConcurrentSortedDoubleLinkedList<>();
  }

  @Test
  public void testThatInstertedObjectsAreSorted()
  {
    list.add(new TestClass(3));
    list.add(new TestClass(1));
    list.add(new TestClass(2));
    Node<TestClass> current = list.first.get();
    assertThat(current.item.i).isEqualTo(1);
    current = current.next.get();
    assertThat(current.item.i).isEqualTo(2);
    current = current.next.get();
    assertThat(current.item.i).isEqualTo(3);
  }

  private static class Producer extends MaloWProcess
  {
    private int count;

    public Producer(int count)
    {
      this.count = count;
    }

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        list.add(new TestClass(RandomNumberGenerator.getRandomInt(0, 100000)));
        this.count--;
        int d20 = RandomNumberGenerator.rollD(20);
        if (d20 < 5)
        {
          if (list.remove(new TestClass(RandomNumberGenerator.getRandomInt(0, 100000))))
          {
            this.count++;
          }
        }
        if (this.count == 0)
        {
          this.close();
        }
      }
    }

    @Override
    public void closeSpecific()
    {
    }
  }

  @Test
  public void testConcurrency()
  {
    int threadCount = 10;
    int additionsPerThread = 2000;
    List<Producer> producers = new ArrayList<Producer>();
    for (int i = 0; i < threadCount; i++)
    {
      producers.add(new Producer(additionsPerThread));
    }
    long before = System.nanoTime();
    for (Producer producer : producers)
    {
      producer.start();
    }
    for (Producer producer : producers)
    {
      producer.waitUntillDone();
    }
    long elapsed = System.nanoTime() - before;
    System.out.println((elapsed / 1000000.0) + "ms.");
    assertThat(list.getSize()).isEqualTo(threadCount * additionsPerThread);
    Optional<Node<TestClass>> current = list.first.get().next;
    int i = 1;
    while (current.isPresent())
    {
      assertThat(current.get().item.i).isGreaterThanOrEqualTo(current.get().previous.get().item.i);
      current = current.get().next;
      i++;
    }
    assertThat(i).isEqualTo(threadCount * additionsPerThread);
  }

}