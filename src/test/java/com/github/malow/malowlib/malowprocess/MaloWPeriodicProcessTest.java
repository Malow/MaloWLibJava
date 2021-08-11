package com.github.malow.malowlib.malowprocess;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.Test;

public class MaloWPeriodicProcessTest
{
  public static class MaloWPeriodicProcessForTestEasy extends MaloWPeriodicProcess
  {
    public int updateCount = 0;
    public float diffSum = 0.0f;
    public int sleepDuration = 0;

    public MaloWPeriodicProcessForTestEasy(double updatesPerSecond)
    {
      super(updatesPerSecond);
    }

    @Override
    protected void update(float diff, long now)
    {
      this.updateCount++;
      this.diffSum += diff;

      if (this.sleepDuration == 0)
      {
        return;
      }

      try
      {
        Thread.sleep(this.sleepDuration);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  @Test
  public void test() throws Exception
  {
    MaloWPeriodicProcessForTestEasy ft = new MaloWPeriodicProcessForTestEasy(100);
    ft.start();
    Thread.sleep(1000);
    assertThat(ft.diffSum).isCloseTo(1.0f, Percentage.withPercentage(5.0));
    assertThat(ft.updateCount).isCloseTo(100, Percentage.withPercentage(15.0));

    ft.setUpdatesPerSecond(10);

    ft.sleepDuration = 10;
    Thread.sleep(2000);
    assertThat(ft.getLoadData().percentage).isCloseTo(10, Offset.offset(2.0));

    ft.sleepDuration = 100;
    Thread.sleep(2000);
    assertThat(ft.getLoadData().percentage).isCloseTo(100, Percentage.withPercentage(5.0));

    ft.sleepDuration = 200;
    Thread.sleep(2000);
    assertThat(ft.getLoadData().percentage).isCloseTo(200, Percentage.withPercentage(5.0));

    ft.sleepDuration = 10;
    Thread.sleep(1000);
    ft.sleepDuration = 5;
    Thread.sleep(50);
    ft.sleepDuration = 20;
    Thread.sleep(100);
    ft.sleepDuration = 10;
    Thread.sleep(100);
    assertThat(ft.getLoadData().min).isCloseTo(5, Offset.offset(2));
    assertThat(ft.getLoadData().max).isCloseTo(20, Offset.offset(2));
    assertThat(ft.getLoadData().avg).isCloseTo(10, Offset.offset(2));

    ft.closeAndWaitForCompletion();
  }

  //@Test
  public void foreverTestThatLoadDataDoesntCrashStuff()
  {
    MaloWPeriodicProcessForTestEasy ft = new MaloWPeriodicProcessForTestEasy(1000);
    ft.start();
    while (true)
    {
      ft.getLoadData();
    }
  }
}
