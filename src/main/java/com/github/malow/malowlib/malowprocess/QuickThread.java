package com.github.malow.malowlib.malowprocess;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameterAndReturn;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithReturn;
import com.github.malow.malowlib.malowprocess.ThreadPool.Future;

/**
 * Creates an internal static ThreadPool with a number of threads equal to the available hardware threads in the system. doWork methods simply sends the job to
 * the ThreadPool to execute and returns a Future object that can be waited on to get the results.
 */
@SuppressWarnings("rawtypes")
public class QuickThread
{
  private static ThreadPool threadPool;

  @SuppressWarnings("unchecked")
  public static <T, S> Future<S> doWork(CheckedFunctionWithParameterAndReturn<T, S> f, T parameter)
  {
    if (threadPool == null)
    {
      threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());
    }

    return threadPool.StartWork(f, parameter);
  }

  @SuppressWarnings("unchecked")
  public static <T> Future<T> doWork(CheckedFunctionWithReturn<T> f)
  {
    if (threadPool == null)
    {
      threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());
    }

    return threadPool.StartWork(f);
  }
}
