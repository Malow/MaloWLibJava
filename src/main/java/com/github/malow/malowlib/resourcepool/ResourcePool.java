package com.github.malow.malowlib.resourcepool;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameter;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameterAndReturn;

public abstract class ResourcePool<T>
{
  private BlockingDeque<T> resources = new LinkedBlockingDeque<>();

  private T getOrCreate()
  {
    T resource = this.resources.pollFirst();
    if (resource != null)
    {
      return resource;
    }
    return this.createNew();
  }

  protected abstract T createNew();

  protected abstract void closeResource(T resource);

  /*
   * This shit is awkward as fuck
   */
  protected abstract void handleException(Exception e) throws Exception;

  public <S> S useResource(CheckedFunctionWithParameterAndReturn<T, S> f) throws Exception
  {
    T resource = this.getOrCreate();
    try
    {
      S ret = f.apply(resource);
      this.resources.add(resource);
      return ret;
    }
    catch (Exception e)
    {
      this.handleException(e);
    }
    return null; // Nope :(
  }

  public void useResource(CheckedFunctionWithParameter<T> f) throws Exception
  {
    T resource = this.getOrCreate();
    try
    {
      f.apply(resource);
      this.resources.add(resource);
    }
    catch (Exception e)
    {
      this.handleException(e);
    }
  }
}
