package com.github.malow.malowlib.nextepisodetabopener;

import com.github.malow.malowlib.confighandler.Config;

public class NextEpisodeTabOpenerConfig extends Config
{
  public String username = "";
  public String PHPSESSID = "";
  public String cookie_show_disclaimer = "";
  public String __utmc = "";
  public String next_ep_id_secure = "";
  public String next_ep_user_secure = "";
  public String next_ep_hash_secure = "";
  public String punbb_cookie = "";
  public String __utma = "";
  public String __utmz = "";
  public String __utmt = "";
  public String __utmb = "";


  @Override
  public String getVersion()
  {
    return "1.0";
  }

  @Override
  public Class<?> getNextVersionClass()
  {
    return null;
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
