package com.github.malow.malowlib.malowprocess;

public abstract class MaloWPeriodicProcess extends MaloWProcess
{
  private long lastUpdate = System.currentTimeMillis();
  private long expectedUpdateDurationMs = 10;

  private int loadPercentageLastSecond = 0;
  private long sleepDurationThisSecond = 0;
  private float timeSinceLastSecond = 0.0f;

  public MaloWPeriodicProcess(double updatesPerSecond)
  {
    this.setUpdatesPerSecond(updatesPerSecond);
  }

  public void setUpdatesPerSecond(double updatesPerSecond)
  {
    this.expectedUpdateDurationMs = (long) (1000 / updatesPerSecond);
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

      this.timeSinceLastSecond += diff;
      if (this.timeSinceLastSecond > 1.0f)
      {
        this.loadPercentageLastSecond = (int) ((1000 - this.sleepDurationThisSecond) / 10);
        this.sleepDurationThisSecond = 0;
        this.timeSinceLastSecond = 0.0f;
      }
      this.throttle();
    }
  }

  private void throttle()
  {
    long sleepDuration = this.lastUpdate + this.expectedUpdateDurationMs - System.currentTimeMillis();
    this.sleepDurationThisSecond += sleepDuration;
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

  public abstract void update(float diff, long now);

  public int getLoadPercentage()
  {
    return this.loadPercentageLastSecond;
  }
}
