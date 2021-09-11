package com.github.malow.malowlib.time;

//TODO: Start using this everywhere, search for "nanoTime" and "currentTimeMillis"
/**
 * A timer class that measures things with millisecond-precision
 */
public class ImpreciseTimer
{
  public ImpreciseTimer()
  {
    this.lastTime = System.currentTimeMillis();
  }

  private long lastTime;

  /**
   * Returns the number of milliseconds since last reset
   */
  public int getElapsed()
  {
    return (int) (System.currentTimeMillis() - this.lastTime);
  }

  /**
   * Returns true/false depending on if ms time has passed since the last reset
   */
  public boolean hasElapsed(int ms)
  {
    return this.getElapsed() >= ms;
  }

  /**
   * Resets the timer and starts counting time from now
   */
  public void reset()
  {
    this.lastTime = System.currentTimeMillis();
  }
}
