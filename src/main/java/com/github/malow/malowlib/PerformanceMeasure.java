package com.github.malow.malowlib;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunction;

// TODO: Start using this everywhere, search for nanoTime, maybe add an ms-version too
public class PerformanceMeasure
{
  /**
   * @param function
   *          Lambda with code to run
   * @return The number of milliseconds that the code took to run
   * @throws Exception
   */
  public static double measureDetailedMs(CheckedFunction function) throws Exception
  {
    long startTime = System.nanoTime();
    function.apply();
    return (System.nanoTime() - startTime) / 1000000.0;
  }

  /**
   * @param function
   *          Lambda with code to run
   * @return The number of milliseconds that the code took to run
   * @throws Exception
   */
  public static long measureMs(CheckedFunction function) throws Exception
  {
    long startTime = System.currentTimeMillis();
    function.apply();
    return System.currentTimeMillis() - startTime;
  }
}
