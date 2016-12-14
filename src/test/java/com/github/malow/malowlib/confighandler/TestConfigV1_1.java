package com.github.malow.malowlib.confighandler;

public class TestConfigV1_1 extends Config
{
  public String a = "aaasd";
  public String b = "ewq";

  @Override
  String getVersion()
  {
    return "1.1";
  }

  @Override
  Class<?> getNextVersionClass()
  {
    return TestConfig.class;
  }

  @Override
  Class<?> getPreviousVersionClass()
  {
    return TestConfigV1_0.class;
  }

  @Override
  void upgradeTranslation(Config oldVersion)
  {

  }
}