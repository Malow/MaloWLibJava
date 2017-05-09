package com.github.malow.malowlib.matchmakingengine;

import java.util.ArrayList;
import java.util.List;

import com.github.malow.malowlib.ConcurrentSortedDoubleLinkedList;

public class PlayerPool extends ConcurrentSortedDoubleLinkedList<MatchmakingPlayer>
{
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
    this.lock();
    Node<MatchmakingPlayer> current = this.first;
    if (current != null)
    {
      current = current.next;
    }
    while (current != null)
    {
      if (current.previous != null && this.isSuitableMatch(current.item, current.previous.item))
      {
        Node<MatchmakingPlayer> previous = current.previous;
        resultList.add(new MatchmakingResult(current.item, previous.item));
        this.remove(previous);
        Node<MatchmakingPlayer> next = current.next;
        this.remove(current);
        current = next;
      }
      else
      {
        current = current.next;
      }
    }
    this.unlock();
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
