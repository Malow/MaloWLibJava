package com.github.malow.malowlib;

public class RandomNumberGenerator
{
  public static float getRandomFloat(float min, float max)
  {
    double rnd = Math.random();
    float range = max - min;
    return (float) ((rnd * range) + min);
  }

  public static int getRandomInt(int min, int max)
  {
    max++;
    double rnd = Math.random();
    int range = max - min;
    return (int) ((rnd * range) + min);
  }

  public static int rollD(int x)
  {
    return getRandomInt(1, x);
  }
}
