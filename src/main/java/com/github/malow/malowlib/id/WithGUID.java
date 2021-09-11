package com.github.malow.malowlib.id;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A simple class to extend to get a GUID (64 bit) included, implements hashCode and equals based on the UUID
 */
public abstract class WithGUID
{
  public WithGUID(long guid)
  {
    this.guid = guid;
  }

  public WithGUID()
  {
    this.guid = ThreadLocalRandom.current().nextLong();
  }

  private long guid;

  protected void setGuid(long guid)
  {
    this.guid = guid;
  }

  public long getGuid()
  {
    return this.guid;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (guid ^ (guid >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WithGUID other = (WithGUID) obj;
    if (guid != other.guid)
      return false;
    return true;
  }
}
