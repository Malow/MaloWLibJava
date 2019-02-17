package com.github.malow.malowlib.newepisodetabopener;

import java.time.LocalDate;

public class Episode
{
  public int season;
  public int episode;
  public LocalDate aired;

  public Episode(int season, int episode, LocalDate aired)
  {
    this.season = season;
    this.episode = episode;
    this.aired = aired;
  }

  @Override
  public String toString()
  {
    String s = "S";
    if (this.season > 9)
    {
      s += this.season;
    }
    else
    {
      s += "0" + this.season;
    }
    if (this.episode > 9)
    {
      s += "E" + this.episode;
    }
    else
    {
      s += "E0" + this.episode;
    }
    return s;
  }

  public boolean isNewerThan(Episode other)
  {
    if (this.season > other.season)
    {
      return true;
    }
    if (this.season == other.season && this.episode > other.episode)
    {
      return true;
    }
    return false;
  }
}
