package com.github.malow.malowlib.matchmakingengine;

public class MatchmakingResult
{
  public MatchmakingPlayer player1;
  public MatchmakingPlayer player2;

  public MatchmakingResult(MatchmakingPlayer player1, MatchmakingPlayer player2)
  {
    this.player1 = player1;
    this.player2 = player2;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.player1 == null ? 0 : this.player1.hashCode());
    result = prime * result + (this.player2 == null ? 0 : this.player2.hashCode());
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
    MatchmakingResult other = (MatchmakingResult) obj;
    return this.player1.equals(other.player1) && this.player2.equals(other.player2)
        || this.player1.equals(other.player2) && this.player2.equals(other.player1);
  }
}
