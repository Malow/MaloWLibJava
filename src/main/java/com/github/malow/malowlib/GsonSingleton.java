package com.github.malow.malowlib;

import com.google.gson.Gson;

public class GsonSingleton
{
  private static Gson gson = new Gson();

  private GsonSingleton()
  {

  }

  public static Gson get()
  {
    return gson;
  }
}
