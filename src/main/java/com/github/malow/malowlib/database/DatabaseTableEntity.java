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
  public @interface Optional
  {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface NotPersisted
  {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface ForeignKey
  {
    public Class<? extends DatabaseTableEntity> target();
  }

  private Integer id;
  private Integer version = 1;

  public Integer getId()
  {
    return this.id;
  }

  void setId(Integer id)
  {
    this.id = id;
  }

  Integer getVersion()
  {
    return this.version;
  }

  void setVersion(Integer version)
  {
    this.version = version;
  }

  void incrementVersion()
  {
    this.version++;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + ":" + this.getId() + " v" + this.getVersion();
  }
}
