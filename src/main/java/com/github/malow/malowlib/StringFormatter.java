package com.github.malow.malowlib;

public class StringFormatter
{
  public static String withDecimals(double number, int nrOfDecimals)
  {
    int exponential = (int) Math.pow(10, nrOfDecimals);
    int n = (int) (number * exponential);
    return "" + n / (exponential * 1.0);
  }
}
