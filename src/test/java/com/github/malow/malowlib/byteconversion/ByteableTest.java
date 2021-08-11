package com.github.malow.malowlib.byteconversion;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.PerformanceMeasure;
import com.github.malow.malowlib.byteconversion.proto.TestDataProto;
import com.github.malow.malowlib.byteconversion.proto.TestDataProto.InnerTestDataProto;
import com.github.malow.malowlib.byteconversion.proto.TestDataProto.ListOfList;

public class ByteableTest
{
  private static final Random random = new Random();

  public static class InnerTestData extends Byteable implements Serializable
  {
    public InnerTestData()
    {
      this.s = UUID.randomUUID().toString();
      this.i = new ArrayList<>();
      for (int q = 0; q < 10; q++)
      {
        this.i.add(random.nextInt());
      }
    }

    public String s;
    public List<Integer> i;

    @Override
    protected void readFromBytes(ByteReader byteReader) throws Exception
    {
      this.s = byteReader.readString();
      this.i = new ArrayList<>();
      int length = byteReader.readInt();
      for (int i = 0; i < length; i++)
      {
        this.i.add(byteReader.readInt());
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

  public static class TestData extends Byteable implements Serializable
  {
    public String s;
    public List<Integer> i;
    public List<List<Double>> d;
    public boolean b;
    public InnerTestData inner;

    public TestData()
    {
      this.s = UUID.randomUUID().toString();
      this.i = new ArrayList<>();
      for (int q = 0; q < 10; q++)
      {
        this.i.add(random.nextInt());
      }
      this.d = new ArrayList<>();
      for (int q = 0; q < 10; q++)
      {
        List<Double> tmp = new ArrayList<>();
        for (int w = 0; w < 10; w++)
        {
          tmp.add(random.nextDouble());
        }
        this.d.add(tmp);
      }
      this.b = random.nextBoolean();

      this.inner = new InnerTestData();
    }

    @Override
    protected void readFromBytes(ByteReader byteReader) throws Exception
    {
      this.s = byteReader.readString();
      this.i = new ArrayList<>();
      int length = byteReader.readInt();
      for (int i = 0; i < length; i++)
      {
        this.i.add(byteReader.readInt());
      }
      this.d = new ArrayList<>();
      int length2 = byteReader.readInt();
      for (int i = 0; i < length2; i++)
      {
        List<Double> list = new ArrayList<>();
        int length3 = byteReader.readInt();
        for (int u = 0; u < length3; u++)
        {
          list.add(byteReader.readDouble());
        }
        this.d.add(list);
      }
      this.b = byteReader.readBoolean();
      this.inner = byteReader.readByteable(InnerTestData.class);
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

  private static final int RUNS = 10000;

  private List<TestData> expected = new ArrayList<>();
  private List<TestData> result = new ArrayList<>();

  @Before
  public void beforeTest()
  {
    this.expected = new ArrayList<>();
    for (int i = 0; i < RUNS; i++)
    {
      this.expected.add(new TestData());
    }
    this.result = new ArrayList<>();
  }

  @Test
  public void testHomemade() throws Exception
  {
    double ms = PerformanceMeasure.measureDetailedMs(() ->
    {
      for (TestData data : this.expected)
      {
        byte[] bytes = data.toByteArray();
        this.result.add(Byteable.create(ByteBuffer.wrap(bytes), TestData.class));
      }
    });
    System.out.println("Homemade Raw bytes: " + ms + "ms. Object Size: " + this.expected.get(1).toByteArray().length);
  }

  @Test
  public void testSerializeable() throws Exception
  {
    double ms = PerformanceMeasure.measureDetailedMs(() ->
    {
      for (TestData data : this.expected)
      {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(baos))
        {
          out.writeObject(data);
          out.flush();
          byte[] bytes = baos.toByteArray();
          try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bais))
          {
            this.result.add((TestData) in.readObject());
          }
        }
      }
    });
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(baos))
    {
      out.writeObject(this.expected.get(1));
      out.flush();
      System.out.println("Java's built-in Serializable: " + ms + "ms. Object Size: " + baos.toByteArray().length);
    }
  }

  @Test
  public void testJson() throws Exception
  {
    double ms = PerformanceMeasure.measureDetailedMs(() ->
    {
      for (TestData data : this.expected)
      {
        String json = GsonSingleton.toJson(data);
        byte[] bytes = json.getBytes();
        this.result.add(GsonSingleton.fromJson(new String(bytes), TestData.class));
      }
    });
    System.out.println("Json: " + ms + "ms. Object Size: " + GsonSingleton.toJson(this.expected.get(1)).getBytes().length);
  }

  private boolean ignoreAfter = false;

  @After
  public void afterTest()
  {
    if (this.ignoreAfter)
    {
      return;
    }
    for (int i = 0; i < RUNS; i++)
    {
      assertEquals(this.result.get(i).s, this.expected.get(i).s);
      assertEquals(this.result.get(i).i.size(), this.expected.get(i).i.size());
      for (int u = 0; u < this.expected.get(i).i.size(); u++)
      {
        assertEquals(this.result.get(i).i.get(u), this.expected.get(i).i.get(u));
      }
      assertEquals(this.result.get(i).d.size(), this.expected.get(i).d.size());
      for (int u = 0; u < this.expected.get(i).d.size(); u++)
      {
        assertEquals(this.result.get(i).d.get(u).size(), this.expected.get(i).d.get(u).size());
        for (int r = 0; r < this.expected.get(i).d.get(u).size(); r++)
        {
          assertEquals(this.result.get(i).d.get(u).get(r), this.expected.get(i).d.get(u).get(r));
        }
      }
      assertEquals(this.result.get(i).b, this.expected.get(i).b);
      assertEquals(this.result.get(i).inner.s, this.expected.get(i).inner.s);
      assertEquals(this.result.get(i).inner.i.size(), this.expected.get(i).inner.i.size());
      for (int u = 0; u < this.expected.get(i).inner.i.size(); u++)
      {
        assertEquals(this.result.get(i).inner.i.get(u), this.expected.get(i).inner.i.get(u));
      }
    }
  }

  // Protocol Buffers

  @Test
  public void testProtocolBuffers() throws Exception
  {
    List<TestDataProto> expectedProto = new ArrayList<>();
    for (TestData data : this.expected)
    {
      expectedProto.add(this.convertTestDataToProto(data));
    }

    List<TestDataProto> resultProto = new ArrayList<>();
    double ms = PerformanceMeasure.measureDetailedMs(() ->
    {
      for (TestDataProto data : expectedProto)
      {
        byte[] bytes = data.toByteArray();
        resultProto.add(TestDataProto.parseFrom(bytes));
      }
    });
    System.out.println("Google's Protocol Buffers: " + ms + "ms. Object Size: " + expectedProto.get(1).toByteArray().length);

    for (int i = 0; i < RUNS; i++)
    {
      assertEquals(resultProto.get(i).getS(), this.expected.get(i).s);
      assertEquals(resultProto.get(i).getICount(), this.expected.get(i).i.size());
      for (int u = 0; u < this.expected.get(i).i.size(); u++)
      {
        assertEquals((Integer) resultProto.get(i).getI(u), this.expected.get(i).i.get(u));
      }
      assertEquals(resultProto.get(i).getDCount(), this.expected.get(i).d.size());
      for (int u = 0; u < this.expected.get(i).d.size(); u++)
      {
        assertEquals(resultProto.get(i).getD(u).getDCount(), this.expected.get(i).d.get(u).size());
        for (int r = 0; r < this.expected.get(i).d.get(u).size(); r++)
        {
          assertEquals((Double) resultProto.get(i).getD(u).getD(r), this.expected.get(i).d.get(u).get(r));
        }
      }
      assertEquals(resultProto.get(i).getB(), this.expected.get(i).b);
      assertEquals(resultProto.get(i).getInner().getS(), this.expected.get(i).inner.s);
      assertEquals(resultProto.get(i).getInner().getICount(), this.expected.get(i).inner.i.size());
      for (int u = 0; u < this.expected.get(i).inner.i.size(); u++)
      {
        assertEquals((Integer) resultProto.get(i).getInner().getI(u), this.expected.get(i).inner.i.get(u));
      }
    }
    this.ignoreAfter = true;
  }

  private TestDataProto convertTestDataToProto(TestData data)
  {
    TestDataProto proto = TestDataProto.newBuilder()
        .setS(data.s)
        .addAllI(data.i)
        .addAllD(this.convertListToListOfList(data.d))
        .setB(data.b)
        .setInner(this.convertInnerTestDataToProto(data.inner))
        .build();
    return proto;
  }

  private List<ListOfList> convertListToListOfList(List<List<Double>> d)
  {
    List<ListOfList> l = new ArrayList<>();
    for (List<Double> inList : d)
    {
      ListOfList lol = ListOfList.newBuilder()
          .addAllD(inList)
          .build();
      l.add(lol);
    }
    return l;
  }

  private InnerTestDataProto convertInnerTestDataToProto(InnerTestData data)
  {
    InnerTestDataProto proto = InnerTestDataProto.newBuilder()
        .setS(data.s)
        .addAllI(data.i)
        .build();
    return proto;
  }
}
