package com.github.malow.malowlib;

import java.util.concurrent.ThreadLocalRandom;

public class RandomNumberGenerator
{
  public static float getRandomFloat(float min, float max)
  {
    float rnd = ThreadLocalRandom.current().nextFloat();
    float range = max - min;
    return rnd * range + min;
  }

  public static double getRandomDouble(double min, double max)
  {
    return ThreadLocalRandom.current().nextDouble(min, max);
  }

  public static int getRandomInt(int min, int max)
  {
    max++;
    double rnd = ThreadLocalRandom.current().nextDouble();
    int range = max - min;
    return (int) (rnd * range + min);
  }

  public static int rollD(int x)
  {
    return getRandomInt(1, x);
  }
}
