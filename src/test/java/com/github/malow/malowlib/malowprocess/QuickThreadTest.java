package com.github.malow.malowlib.malowprocess;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.github.malow.malowlib.malowprocess.ThreadPool.Future;

public class QuickThreadTest
{
  @Test
  public void testQuickThreadWithParameters() throws Exception
  {
    Future<Integer> f1 = QuickThread.doWork(parameter -> (parameter * 2), 4);
    Future<Integer> f2 = QuickThread.doWork(parameter -> (parameter * 2), 2);
    Future<Integer> f3 = QuickThread.doWork(parameter -> (parameter * 2), 6);

    assertThat(f1.waitForResult()).isEqualTo(8);
    assertThat(f2.waitForResult()).isEqualTo(4);
    assertThat(f3.waitForResult()).isEqualTo(12);
  }

  @Test
  public void testQuickThreadWithoutParameters() throws Exception
  {
    String s = " is cute";
    Future<String> f1 = QuickThread.doWork(() -> "Horse" + s);
    Future<String> f2 = QuickThread.doWork(() -> "Panda" + s);
    Future<String> f3 = QuickThread.doWork(() -> "Cow" + s);

    assertThat(f1.waitForResult()).isEqualTo("Horse is cute");
    assertThat(f2.waitForResult()).isEqualTo("Panda is cute");
    assertThat(f3.waitForResult()).isEqualTo("Cow is cute");
  }
}
