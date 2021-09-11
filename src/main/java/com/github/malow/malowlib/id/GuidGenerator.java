package com.github.malow.malowlib.id;

public class GuidGenerator
{
  private long counter = 0;

  public long getNext()
  {
    return counter++;
  }
}
