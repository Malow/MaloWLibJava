package com.github.malow.malowlib;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

import com.github.malow.malowlib.id.WithUUID;

public class WithUUIDTest
{
  public static class UUIDTest extends WithUUID
  {
    public UUIDTest(UUID uuid)
    {
      super(uuid);
    }

    public int asd;
  }

  @Test
  public void test()
  {
    UUIDTest a = new UUIDTest(UUID.randomUUID());
    a.asd = 5;
    UUIDTest b = new UUIDTest(UUID.fromString(a.getUuid().toString()));
    b.asd = 7;
    assertEquals(b, a);
  }
}
