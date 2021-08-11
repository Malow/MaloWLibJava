package com.github.malow.malowlib.matchmakingengine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class PlayerPool extends ConcurrentSkipListSet<MatchmakingPlayer>
{
  private static final long serialVersionUID = 1L;
  private Double maxRatingDifference;

  public PlayerPool(MatchmakingEngineConfig config)
  {
    this.config = config;
    if (config.maxRatingDifference.isPresent())
    {
      this.maxRatingDifference = config.maxRatingDifference.get();
    }
  }

  private MatchmakingEngineConfig config;

  public void updateConfig(MatchmakingEngineConfig config)
  {
    this.config = config;
  }

  public List<MatchmakingResult> createMatches()
  {
    List<MatchmakingResult> resultList = new ArrayList<>();
    MatchmakingPlayer previous = null;
    for (MatchmakingPlayer player : this)
    {
      if (previous != null && this.isSuitableMatch(player, previous))
      {
        if (this.remove(player))
        {
          if (this.remove(previous))
          {
            resultList.add(new MatchmakingResult(player, previous));
            previous = null;
            continue;
          }
          this.add(player);
        }
      }
      previous = player;
    }
    return resultList;
  }

  private boolean isSuitableMatch(MatchmakingPlayer current, MatchmakingPlayer previous)
  {
    double maxRatingDifference = this.getMaxRatingDifferenceForSearch(System.currentTimeMillis() - Math.min(current.timeAdded, previous.timeAdded));
    if (previous.rating + maxRatingDifference > current.rating)
    {
      return true;
    }
    return false;
  }

  private double getMaxRatingDifferenceForSearch(Long timeSinceSearchStart)
  {
    if (this.maxRatingDifference != null)
    {
      return Math.min(this.config.initialRatingDifference + timeSinceSearchStart * (this.config.ratingDifferenceIncreasePerSecond / 1000.0),
          this.maxRatingDifference);
    }
    return this.config.initialRatingDifference + timeSinceSearchStart * (this.config.ratingDifferenceIncreasePerSecond / 1000.0);
  }
}
