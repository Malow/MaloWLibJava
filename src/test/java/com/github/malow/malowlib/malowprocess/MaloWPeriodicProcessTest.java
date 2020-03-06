package com.github.malow.malowlib.malowprocess;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void update(float diff, long now)
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
    assertThat(ft.updateCount).isCloseTo(100, Percentage.withPercentage(10.0));
    assertThat(ft.getLoadPercentage()).isCloseTo(5, Percentage.withPercentage(100.0));

    ft.setUpdatesPerSecond(10);

    ft.sleepDuration = 10;
    Thread.sleep(3000);
    assertThat(ft.getLoadPercentage()).isCloseTo(10, Percentage.withPercentage(5.0));

    ft.sleepDuration = 100;
    Thread.sleep(3000);
    assertThat(ft.getLoadPercentage()).isCloseTo(100, Percentage.withPercentage(5.0));

    ft.sleepDuration = 200;
    Thread.sleep(3000);
    assertThat(ft.getLoadPercentage()).isCloseTo(150, Percentage.withPercentage(5.0));

    ft.closeAndWaitForCompletion();
  }
}
