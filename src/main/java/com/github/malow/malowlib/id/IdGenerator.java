package com.github.malow.malowlib.id;

public class IdGenerator
{
  private int counter = 0;

  public int getNext()
  {
    return counter++;
  }
}
