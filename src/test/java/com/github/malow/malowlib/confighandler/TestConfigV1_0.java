package com.github.malow.malowlib.confighandler;

public class TestConfigV1_0 extends Config
{
  public String a = "asd";

  @Override
  String getVersion()
  {
    return "1.0";
  }

  @Override
  Class<?> getNextVersionClass()
  {
    return TestConfigV1_1.class;
  }

  @Override
  Class<?> getPreviousVersionClass()
  {
    return null;
  }

  @Override
  void upgradeTranslation(Config oldVersion)
  {

  }
}