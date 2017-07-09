package com.github.malow.malowlib.malowcliapplication;

import com.github.malow.malowlib.MaloWLogger;

public class MaloWCliApplicationTest extends MaloWCliApplication
{
  public static void main(String[] args)
  {
    MaloWLogger.setLoggingThresholdToInfo();
    MaloWCliApplicationTest test = new MaloWCliApplicationTest();
    test.run();
  }

  @Command
  public void apa()
  {
    System.out.println("appa");
  }

  @Command
  public void monkey()
  {
    System.out.println("monkkeeh");
  }

  @Command
  @Override
  public void exit()
  {
    System.out.println("exit override");
    super.exit();
  }
}
