package com.github.malow.malowlib;

public class Vector3
{
  public float x = 0.0f;
  public float y = 0.0f;
  public float z = 0.0f;

  public Vector3()
  {
    this.x = 0;
    this.y = 0;
    this.z = 0;
  }

  public Vector3(float x, float y, float z)
  {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3(Vector3 old)
  {
    this.x = old.x;
    this.y = old.y;
    this.z = old.z;
  }

  public Vector3(float[] array)
  {
    if (array.length != 3)
    {
      throw new RuntimeException("Must create vector with 3 element array");
    }

    this.x = array[0];
    this.y = array[1];
    this.z = array[2];
  }

  public Vector3 add(Vector3 rhs)
  {
    return new Vector3(this.x + rhs.x, this.y + rhs.y, this.z + rhs.z);
  }

  public Vector3 sub(Vector3 rhs)
  {
    return new Vector3(this.x - rhs.x, this.y - rhs.y, this.z - rhs.z);
  }

  public Vector3 neg()
  {
    return new Vector3(-this.x, -this.y, -this.z);
  }

  public Vector3 mul(float c)
  {
    return new Vector3(c * this.x, c * this.y, c * this.z);
  }

  public Vector3 div(float c)
  {
    return new Vector3(this.x / c, this.y / c, this.z / c);
  }

  public float dot(Vector3 rhs)
  {
    return this.x * rhs.x + this.y * rhs.y + this.z * rhs.z;
  }

  public Vector3 cross(Vector3 rhs)
  {
    return new Vector3(this.y * rhs.z - this.z * rhs.y, this.x * rhs.z - this.z * rhs.x, this.x * rhs.y - this.y * rhs.x);
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(this.x);
    result = prime * result + Float.floatToIntBits(this.y);
    result = prime * result + Float.floatToIntBits(this.z);
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
    Vector3 other = (Vector3) obj;
    if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x))
    {
      return false;
    }
    if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y))
    {
      return false;
    }
    if (Float.floatToIntBits(this.z) != Float.floatToIntBits(other.z))
    {
      return false;
    }
    return true;
  }

  public float getDistance(Vector3 otherPos)
  {
    return this.sub(otherPos).length();
  }

  public float length()
  {
    return (float) Math.sqrt(this.dot(this));
  }

  public void setLength(float desiredLength)
  {
    if (desiredLength < 0.0f)
    {
      MaloWLogger.error("Tried to set a Vector's length to " + desiredLength);
      return;
    }
    if (desiredLength == 0.0f)
    {
      this.x = 0.0f;
      this.y = 0.0f;
      this.z = 0.0f;
      return;
    }
    float currentLength = this.length();
    if (currentLength == 0.0f)
    {
      MaloWLogger.error("Tried to set a 0-length Vector's length to " + desiredLength);
      return;
    }

    float multiplier = desiredLength / currentLength;
    this.x *= multiplier;
    this.y *= multiplier;
    this.z *= multiplier;
  }

  public void normalize()
  {
    this.setLength(1.0f);
  }

  @Override
  public String toString()
  {
    return "( " + this.x + " " + this.y + " " + this.z + " )";
  }
}