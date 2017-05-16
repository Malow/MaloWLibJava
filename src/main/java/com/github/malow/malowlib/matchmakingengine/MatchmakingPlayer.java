package com.github.malow.malowlib.matchmakingengine;

public class MatchmakingPlayer implements Comparable<MatchmakingPlayer>
{
  public Integer playerId;
  public Double rating;
  public Long timeAdded;

  public MatchmakingPlayer()
  {
  }

  public MatchmakingPlayer(Integer playerId)
  {
    this.playerId = playerId;
  }

  public MatchmakingPlayer(Integer playerId, Double rating, Long timeAdded)
  {
    this.playerId = playerId;
    this.rating = rating;
    this.timeAdded = timeAdded;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.playerId == null ? 0 : this.playerId.hashCode());
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
    MatchmakingPlayer other = (MatchmakingPlayer) obj;
    if (this.playerId == null)
    {
      if (other.playerId != null)
      {
        return false;
      }
    }
    else if (!this.playerId.equals(other.playerId))
    {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(MatchmakingPlayer other)
  {
    if (this.playerId.equals(other.playerId))
    {
      return 0;
    }
    int ratingComparison = this.rating.compareTo(other.rating);
    return ratingComparison != 0 ? ratingComparison : this.playerId.compareTo(other.playerId);
  }

  @Override
  public String toString()
  {
    return "MatchmakingPlayer [playerId=" + this.playerId + ", rating=" + this.rating + ", timeAdded=" + this.timeAdded + "]";
  }
}
