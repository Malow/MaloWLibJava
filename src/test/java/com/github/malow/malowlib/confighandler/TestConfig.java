package com.github.malow.malowlib.confighandler;

public class TestConfig extends Config
{
  public String b = "ewq";
  public String c = "cewdwd";

  @Override
  String getVersion()
  {
    return "2.0";
  }

  @Override
  Class<?> getNextVersionClass()
  {
    return null;
  }

  @Override
  Class<?> getPreviousVersionClass()
  {
    return TestConfigV1_1.class;
  }

  @Override
  void upgradeTranslation(Config oldVersion)
  {
    this.c = ((TestConfigV1_1) oldVersion).a.toUpperCase();
  }

}
