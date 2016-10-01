package com.github.malow.malowlib;

public class RandomNumberGenerator
{
  public static float GetRandomFloat(float min, float max)
  {
    double rnd = Math.random();
    float range = max - min;
    return (float) ((rnd * range) + min);
  }

  public static int GetRandomInt(int min, int max)
  {
    max++;
    double rnd = Math.random();
    int range = max - min;
    return (int) ((rnd * range) + min);
  }

  public static int RollD(int x)
  {
    return GetRandomInt(1, x);
  }
}
