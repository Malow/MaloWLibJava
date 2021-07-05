package com.github.malow.malowlib.malowprocess;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import com.github.malow.malowlib.EvictingQueue;
import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWPeriodicProcess extends MaloWProcess
{
  private long lastUpdate = System.currentTimeMillis();
  private long expectedUpdateDurationMs = 10;

  Queue<Integer> sleepDurations = new EvictingQueue<Integer>(100);

  public MaloWPeriodicProcess(double updatesPerSecond)
  {
    this.setUpdatesPerSecond(updatesPerSecond);
    this.sleepDurations = new EvictingQueue<Integer>((int) updatesPerSecond);
  }

  public void setUpdatesPerSecond(double updatesPerSecond)
  {
    this.expectedUpdateDurationMs = (long) (1000 / updatesPerSecond);
    this.sleepDurations = new EvictingQueue<Integer>((int) updatesPerSecond);
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      long now = System.currentTimeMillis();
      float diff = (now - this.lastUpdate) / 1000.0f;
      this.lastUpdate = now;

      this.update(diff, now);
      this.throttle();
    }
  }

  private void throttle()
  {
    long sleepDuration = this.lastUpdate + this.expectedUpdateDurationMs - System.currentTimeMillis();
    this.sleepDurations.add((int) sleepDuration);
    if (sleepDuration > 0)
    {
      try
      {
        Thread.sleep(sleepDuration);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  protected abstract void update(float diff, long now);

  public static class LoadData
  {
    public int min = Integer.MAX_VALUE;
    public int max = Integer.MIN_VALUE;
    public int avg = -1;
    public double percentage = -1.0;

    @Override
    public String toString()
    {
      return "min: " + this.min + ", max: " + this.max + ", avg: " + this.avg + ", Load%: " + this.percentage * 100 / 100;
    }
  }

  public LoadData getLoadData()
  {
    LoadData loadData = new LoadData();
    List<Integer> copy = this.getSleepDurationsCopy().stream().filter(i -> i != null).collect(Collectors.toList());
    if (copy == null)
    {
      return loadData;
    }

    int totalRuntime = 0;
    for (Integer sleepDuration : copy)
    {
      int runtime = (int) (this.expectedUpdateDurationMs - sleepDuration);
      totalRuntime += runtime;
      if (runtime > loadData.max)
      {
        loadData.max = runtime;
      }
      if (runtime < loadData.min)
      {
        loadData.min = runtime;
      }
    }
    loadData.avg = totalRuntime / copy.size();
    loadData.percentage = totalRuntime / ((double) copy.size() * this.expectedUpdateDurationMs) * 100.0;
    return loadData;
  }

  private List<Integer> getSleepDurationsCopy()
  {
    for (int i = 0; i < 5; i++)
    {
      try
      {
        List<Integer> ret = new ArrayList<Integer>(this.sleepDurations);
        ret = ret.stream().filter(r -> r != null).collect(Collectors.toList());
        if (ret.size() == 0)
        {
          continue;
        }
        return ret;
      }
      catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException e)
      {
      }
    }
    MaloWLogger.error("Failed to get a copy of the sleepDurations list from " + this.getProcessName());
    return null;
  }
}

