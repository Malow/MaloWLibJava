package com.github.malow.malowlib.malowprocess;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.malowprocess.MaloWProcess.ProcessState;

public class MaloWProcessTest extends MaloWProcessFixture
{

  @Test
  public void testThatPutEventAndWaitEventIsThreadSafe() throws Exception
  {
    final int NR_OF_PRODUCERS = 1000;
    final int RUN_FOR_MS = 2000;

    Consumer consumer = new Consumer();
    consumer.start();
    List<Producer> producers = new ArrayList<>();
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.add(new Producer(consumer));
    }
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).start();
    }

    Thread.sleep(RUN_FOR_MS);

    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).close();
    }
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      producers.get(i).waitUntillDone();
    }
    consumer.putEvent(new CloseEvent());
    consumer.waitUntillDone();

    long totalCount = 0;
    for (int i = 0; i < NR_OF_PRODUCERS; i++)
    {
      totalCount += producers.get(i).getCount();
    }
    assertEquals(totalCount, consumer.getCount());
  }

  @Test
  public void testThatStatusIsSwappedBetweenWaitingAndRunning() throws InterruptedException
  {
    SleepProcess sleepProcess = new SleepProcess(2);
    assertEquals(sleepProcess.getState(), ProcessState.NOT_STARTED);
    sleepProcess.start();
    Thread.sleep(10);
    assertEquals(sleepProcess.getState(), ProcessState.WAITING);
    sleepProcess.putEvent(new ProcessEvent());
    assertEquals(sleepProcess.getState(), ProcessState.WAITING);
    sleepProcess.putEvent(new ProcessEvent());
    Thread.sleep(10);
    assertEquals(sleepProcess.getState(), ProcessState.RUNNING);
    Thread.sleep(110);
    assertEquals(sleepProcess.getState(), ProcessState.WAITING);
    sleepProcess.closeAndWaitForCompletion();
  }

  @Test
  public void testMultiThreadProcess()
  {
    final int THREAD_COUNT = 100;
    final int ITERATIONS_PER_THREAD = 200000;

    MultiThreadProcess process = new MultiThreadProcess(THREAD_COUNT, ITERATIONS_PER_THREAD);
    process.start();
    process.waitUntillDone();
    assertEquals(process.count.get(), THREAD_COUNT * ITERATIONS_PER_THREAD);
  }
}