package com.github.malow.malowlib.newepisodetabopener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.github.malow.malowlib.confighandler.Config;

public class NewEpisodeTabOpenerConfig extends Config
{
  public String apiKey = "";
  public String userKey = "";
  public String userName = "";

  public String apiToken = "";
  public LocalDateTime lastApiTokenRefresh = LocalDateTime.now();

  public List<TvShow> watchedTvShows = new ArrayList<>();

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