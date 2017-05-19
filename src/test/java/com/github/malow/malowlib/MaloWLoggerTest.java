package com.github.malow.malowlib;

import org.junit.Test;

import com.google.gson.Gson;

public class MaloWLoggerTest
{
  public class TestException extends Exception
  {
    private static final long serialVersionUID = 599966510288241529L;

    public TestException(String msg, Exception cause)
    {
      super(msg, cause);
    }
  }

  @Test
  public void generateLogEntries()
  {
    MaloWLogger.info("this is info");
    MaloWLogger.warning("this is warning");
    try
    {
      this.throwError();
    }
    catch (TestException e)
    {
      MaloWLogger.error("this is error", e);
    }
  }

  private void throwError() throws TestException
  {
    try
    {
      new Gson().fromJson("bad", TestException.class);
    }
    catch (Exception e)
    {
      throw new TestException("Test exception", e);
    }
  }
}
