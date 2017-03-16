package com.github.malow.malowlib.confighandler;

public class TestConfigV1_1 extends Config
{

  public String a = "aaasd";
  public String b = "ewq";

  @Override
  public String getVersion()
  {
    return "1.1";
  }

  @Override
  public Class<?> getNextVersionClass()
  {
    return TestConfig.class;
  }

  @Override
  public Class<?> getPreviousVersionClass()
  {
    return TestConfigV1_0.class;
  }

  @Override
  public void upgradeTranslation(Config oldVersion)
  {
  }
}
