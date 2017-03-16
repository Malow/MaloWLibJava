package com.github.malow.malowlib.confighandler;

public class TestConfig extends Config
{

  public String b = "ewq";
  public String c = "cewdwd";

  @Override
  public String getVersion()
  {
    return "2.0";
  }

  @Override
  public Class<?> getNextVersionClass()
  {
    return null;
  }

  @Override
  public Class<?> getPreviousVersionClass()
  {
    return TestConfigV1_1.class;
  }

  @Override
  public void upgradeTranslation(Config oldVersion)
  {
    this.c = ((TestConfigV1_1) oldVersion).a.toUpperCase();
  }
}
