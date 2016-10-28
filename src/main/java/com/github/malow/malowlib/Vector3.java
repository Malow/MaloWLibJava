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

  public Vector3(float[] array)
  {
    if (array.length != 3) throw new RuntimeException("Must create vector with 3 element array");

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
  public boolean equals(Object obj)
  {
    if (obj instanceof Vector3)
    {
      Vector3 rhs = (Vector3) obj;

      return this.x == rhs.x && this.y == rhs.y && this.z == rhs.z;
    }
    else
    {
      return false;
    }

  }

  public float length()
  {
    return (float) Math.sqrt(this.dot(this));
  }

  public void normalize()
  {
    float length = this.length();
    if (length > 0.0f)
    {
      this.x /= length;
      this.y /= length;
      this.z /= length;
    }
  }

  @Override
  public String toString()
  {
    return "( " + this.x + " " + this.y + " " + this.z + " )";
  }
}