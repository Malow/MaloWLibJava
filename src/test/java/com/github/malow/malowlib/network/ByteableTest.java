package com.github.malow.malowlib.network;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.Test;

import com.github.malow.malowlib.GsonSingleton;

public class ByteableTest
{
  private static final Random random = new Random();

  @SuppressWarnings("serial")
  private static class InnerTestData extends Byteable implements Serializable
  {
    public InnerTestData()
    {
      this.s = UUID.randomUUID().toString();
      this.i = new ArrayList<>();
      int qwe = random.nextInt(50) + 10;
      for (int q = 0; q < qwe; q++)
      {
        this.i.add(random.nextInt());
      }
    }

    public String s;
    public List<Integer> i;

    @Override
    protected void readFromBytes(ByteBuffer bb) throws Exception
    {
      this.s = this.readString(bb);
      this.i = new ArrayList<>();
      int length = bb.getInt();
      for (int i = 0; i < length; i++)
      {
        this.i.add(bb.getInt());
      }
    }

    @Override
    protected void writeToBytes(ByteWriter byteWriter) throws Exception
    {
      byteWriter.writeString(this.s);
      byteWriter.writeInt(this.i.size());
      for (int q : this.i)
      {
        byteWriter.writeInt(q);
      }
    }
  }

  @SuppressWarnings("serial")
  private static class TestData extends Byteable implements Serializable
  {
    public String s;
    public List<Integer> i;
    public List<List<Double>> d;
    public boolean b;
    public InnerTestData inner;

    public TestData()
    {
      this.s = UUID.randomUUID().toString();
      int c = random.nextInt(50) + 10;
      this.i = new ArrayList<>();
      for (int q = 0; q < c; q++)
      {
        this.i.add(random.nextInt());
      }
      int cd = random.nextInt(50) + 10;
      this.d = new ArrayList<>();
      for (int q = 0; q < cd; q++)
      {
        int e = random.nextInt(50) + 10;
        List<Double> tmp = new ArrayList<>();
        for (int w = 0; w < e; w++)
        {
          tmp.add(random.nextDouble());
        }
        this.d.add(tmp);
      }
      this.b = random.nextBoolean();

      this.inner = new InnerTestData();
    }

    @Override
    protected void readFromBytes(ByteBuffer bb) throws Exception
    {
      this.s = this.readString(bb);
      this.i = new ArrayList<>();
      int length = bb.getInt();
      for (int i = 0; i < length; i++)
      {
        this.i.add(bb.getInt());
      }
      this.d = new ArrayList<>();
      int length2 = bb.getInt();
      for (int i = 0; i < length2; i++)
      {
        List<Double> list = new ArrayList<>();
        int length3 = bb.getInt();
        for (int u = 0; u < length3; u++)
        {
          list.add(bb.getDouble());
        }
        this.d.add(list);
      }
      this.b = this.readBoolean(bb);
      this.inner = Byteable.create(bb, InnerTestData.class);
    }

    @Override
    protected void writeToBytes(ByteWriter byteWriter) throws Exception
    {
      byteWriter.writeString(this.s);
      byteWriter.writeInt(this.i.size());
      for (int q : this.i)
      {
        byteWriter.writeInt(q);
      }
      byteWriter.writeInt(this.d.size());
      for (List<Double> q : this.d)
      {
        byteWriter.writeInt(q.size());
        for (Double du : q)
        {
          byteWriter.writeDouble(du);
        }
      }
      byteWriter.writeBoolean(this.b);
      this.inner.writeToBytes(byteWriter);
    }
  }

  private static final int RUNS = 1000;

  @Test
  public void testHomemade() throws Exception
  {
    List<TestData> expected = new ArrayList<>();
    for (int i = 0; i < RUNS; i++)
    {
      expected.add(new TestData());
    }

    List<TestData> result = new ArrayList<>();
    long before = System.nanoTime();
    for (TestData data : expected)
    {
      byte[] bytes = data.toByteArray();
      result.add(Byteable.create(ByteBuffer.wrap(bytes), TestData.class));
    }
    System.out.println("HomeMade: " + (System.nanoTime() - before) / 1000000.0 + "ms.");

    this.assertArrays(result, expected);
  }

  @Test
  public void testSerializeable() throws Exception
  {
    List<TestData> expected = new ArrayList<>();
    for (int i = 0; i < RUNS; i++)
    {
      expected.add(new TestData());
    }

    List<TestData> result = new ArrayList<>();
    long before = System.nanoTime();
    for (TestData data : expected)
    {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(baos))
      {
        out.writeObject(data);
        out.flush();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray()); ObjectInput in = new ObjectInputStream(bais))
        {
          result.add((TestData) in.readObject());
        }
      }
    }
    System.out.println("Serializable: " + (System.nanoTime() - before) / 1000000.0 + "ms.");

    this.assertArrays(result, expected);
  }

  @Test
  public void testJson() throws Exception
  {
    List<TestData> expected = new ArrayList<>();
    for (int i = 0; i < RUNS; i++)
    {
      expected.add(new TestData());
    }

    List<TestData> result = new ArrayList<>();
    long before = System.nanoTime();
    for (TestData data : expected)
    {
      String json = GsonSingleton.toJson(data);
      result.add(GsonSingleton.fromJson(json, TestData.class));
    }
    System.out.println("Json: " + (System.nanoTime() - before) / 1000000.0 + "ms.");

    this.assertArrays(result, expected);
  }

  private void assertArrays(List<TestData> result, List<TestData> expected)
  {
    for (int i = 0; i < RUNS; i++)
    {
      assertEquals(result.get(i).s, expected.get(i).s);
      assertEquals(result.get(i).i.size(), expected.get(i).i.size());
      for (int u = 0; u < expected.get(i).i.size(); u++)
      {
        assertEquals(result.get(i).i.get(u), expected.get(i).i.get(u));
      }
      assertEquals(result.get(i).d.size(), expected.get(i).d.size());
      for (int u = 0; u < expected.get(i).d.size(); u++)
      {
        assertEquals(result.get(i).d.get(u).size(), expected.get(i).d.get(u).size());
        for (int r = 0; r < expected.get(i).d.get(u).size(); r++)
        {
          assertEquals(result.get(i).d.get(u).get(r), expected.get(i).d.get(u).get(r));
        }
      }
      assertEquals(result.get(i).b, expected.get(i).b);
      assertEquals(result.get(i).inner.s, expected.get(i).inner.s);
      assertEquals(result.get(i).inner.i.size(), expected.get(i).inner.i.size());
      for (int u = 0; u < expected.get(i).inner.i.size(); u++)
      {
        assertEquals(result.get(i).inner.i.get(u), expected.get(i).inner.i.get(u));
      }
    }
  }
}
