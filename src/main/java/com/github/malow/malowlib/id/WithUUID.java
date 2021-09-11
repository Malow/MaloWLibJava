package com.github.malow.malowlib.id;

import java.util.UUID;

/**
 * A simple class to extend to get a UUID (128 bit) included, implements hashCode and equals based on the UUID
 */
public abstract class WithUUID
{
  public WithUUID(UUID uuid)
  {
    this.uuid = uuid;
  }

  public WithUUID()
  {
    this.uuid = UUID.randomUUID();
  }

  private UUID uuid;

  protected void setUuid(UUID uuid)
  {
    this.uuid = uuid;
  }

  public UUID getUuid()
  {
    return this.uuid;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.uuid == null ? 0 : this.uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (this.getClass() != obj.getClass())
    {
      return false;
    }
    WithUUID other = (WithUUID) obj;
    if (this.uuid == null)
    {
      if (other.uuid != null)
      {
        return false;
      }
    }
    else if (!this.uuid.equals(other.uuid))
    {
      return false;
    }
    return true;
  }
}
