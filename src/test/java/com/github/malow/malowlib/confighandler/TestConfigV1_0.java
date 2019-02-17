package com.github.malow.malowlib.confighandler;

public class TestConfigV1_0 extends Config
{
  public String a = "asd";

  @Override
  public String getVersion()
  {
    return "1.0";
  }

  @Override
  public Class<?> getNextVersionClass()
  {
    return TestConfigV1_1.class;
  }

  @Override
  public Class<?> getPreviousVersionClass()
  {
    return null;
  }

  @Override
  public void upgradeTranslation(Config oldVersion)
  {
  }
}
