package com.github.malow.malowlib.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class DatabaseTableEntity
{
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Unique
  {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface ForeignKey
  {
    public Class<? extends DatabaseTableEntity> target();
  }

  private Integer id;

  public Integer getId()
  {
    return this.id;
  }

  void setId(Integer id)
  {
    this.id = id;
  }
}
