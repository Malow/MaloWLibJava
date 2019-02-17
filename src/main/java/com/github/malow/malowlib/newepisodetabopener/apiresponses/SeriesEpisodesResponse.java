package com.github.malow.malowlib.newepisodetabopener.apiresponses;

import java.util.List;

public class SeriesEpisodesResponse
{
  public static class Data
  {
    public int airedSeason;
    public int airedEpisodeNumber;
    public String firstAired;
  }

  public static class Links
  {
    public Integer next;
  }

  public List<Data> data;
  public Links links;
}
