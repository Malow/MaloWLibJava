package com.github.malow.malowlib.newepisodetabopener;

public class TvShow
{
  public String imdbId;
  public String name;
  public String tvdbId;
  public Episode lastFoundEpisode;

  public TvShow(String imdbId)
  {
    this.imdbId = imdbId;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.imdbId == null ? 0 : this.imdbId.hashCode());
    result = prime * result + (this.tvdbId == null ? 0 : this.tvdbId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (this.getClass() != obj.getClass())
    {
      return false;
    }
    TvShow other = (TvShow) obj;
    if (this.imdbId == null)
    {
      if (this.tvdbId == null)
      {
        return false;
      }
      else
      {
        if (this.tvdbId.equals(other.tvdbId))
        {
          return true;
        }
      }
    }
    else
    {
      if (this.imdbId.equals(other.imdbId))
      {
        return true;
      }
    }
    return false;
  }
}
