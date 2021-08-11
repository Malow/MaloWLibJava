package com.github.malow.malowlib;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.Test;

public class PerformanceMeasureTest
{
  @Test
  public void test() throws Exception
  {
    assertThat(PerformanceMeasure.measureDetailedMs(() -> Thread.sleep(10))).isCloseTo(10.0, Offset.offset(2.0));
    assertThat(PerformanceMeasure.measureMs(() -> Thread.sleep(10))).isCloseTo(10, Offset.offset((long) 2));
  }
}
