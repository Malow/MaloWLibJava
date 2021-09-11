package com.github.malow.malowlib.id;

public class WithId
{
  public WithId(int id)
  {
    this.id = id;
  }

  private int id;

  protected void setUuid(int id)
  {
    this.id = id;
  }

  public int getId()
  {
    return this.id;
  }
}
