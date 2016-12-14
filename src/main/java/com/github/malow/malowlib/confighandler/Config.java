package com.github.malow.malowlib.confighandler;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class Config
{
  Config()
  {

  }

  Map<String, String> configHeader = this.createHeader();

  String getName()
  {
    return this.getClass().getSimpleName();
  }

  abstract String getVersion();

  abstract Class<?> getNextVersionClass();

  abstract Class<?> getPreviousVersionClass();

  private Map<String, String> createHeader()
  {
    Map<String, String> ret = new HashMap<String, String>();
    ret.put("name", this.getName());
    ret.put("version", this.getVersion());
    return ret;
  }

  void upgrade(Config oldVersion)
  {
    this.straightCopy(oldVersion);
    this.upgradeTranslation(oldVersion);
  }

  private void straightCopy(Config oldVersion)
  {
    for (Field field : oldVersion.getClass().getDeclaredFields())
    {
      try
      {
        this.getClass().getDeclaredField(field.getName()).set(this, field.get(oldVersion));
      }
      catch (Exception e)
      {

      }
    }
  }

  abstract void upgradeTranslation(Config oldVersion);
}