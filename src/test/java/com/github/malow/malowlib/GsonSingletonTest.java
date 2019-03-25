package com.github.malow.malowlib;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;

import org.junit.Test;

public class GsonSingletonTest
{
  @Test
  public void testZonedDateTime()
  {
    ZonedDateTime time = ZonedDateTime.now();
    String json = GsonSingleton.toJson(time);
    assertThat(GsonSingleton.fromJson(json, ZonedDateTime.class)).isEqualTo(time);
  }
}
