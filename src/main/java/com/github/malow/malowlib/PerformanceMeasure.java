package com.github.malow.malowlib;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunction;

public class PerformanceMeasure
{
  /**
   * @param function
   *          Lambda with code to run
   * @return The number of nano-seconds that the code took to run
   * @throws Exception
   */
  public static long measure(CheckedFunction function) throws Exception
  {
    long startTime = System.nanoTime();
    function.apply();
    return System.nanoTime() - startTime;
  }
}
