package com.github.malow.malowlib;

import java.util.concurrent.ThreadLocalRandom;

public class Dice
{
  public Dice(int count, int size)
  {
    this.count = count;
    this.size = size;
  }

  private int count;
  private int size;

  public int roll()
  {
    int total = 0;
    for (int i = 0; i < this.count; i++)
    {
      total += ThreadLocalRandom.current().nextInt(this.size - 1) + 1;
    }
    return total;
  }
}
