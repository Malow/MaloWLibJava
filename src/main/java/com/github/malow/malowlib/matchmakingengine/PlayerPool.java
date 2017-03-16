package com.github.malow.malowlib.matchmakingengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.github.malow.malowlib.ConcurrentSortedDoubleLinkedList;

public class PlayerPool extends ConcurrentSortedDoubleLinkedList<MatchmakingPlayer>
{

  public PlayerPool(MatchmakingEngineConfig config)
  {
    this.config = config;
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
    Optional<Node<MatchmakingPlayer>> current = this.first;
    if (current.isPresent())
    {
      current = current.get().next;
    }
    while (current.isPresent())
    {
      if (current.get().previous.isPresent() && this.isSuitableMatch(current.get().item, current.get().previous.get().item))
      {
        Node<MatchmakingPlayer> previous = current.get().previous.get();
        resultList.add(new MatchmakingResult(current.get().item, previous.item));
        this.remove(previous);
        Optional<Node<MatchmakingPlayer>> next = current.get().next;
        this.remove(current.get());
        current = next;
      }
      else
      {
        current = current.get().next;
      }
    }
    this.unlock();
    return resultList;
  }

  private boolean isSuitableMatch(MatchmakingPlayer current, MatchmakingPlayer previous)
  {
    double maxRatingDifference = this.getMaxRatingDifferenceForSearch(System.currentTimeMillis() - Math.min(current.timeAdded, previous.timeAdded));
    if ((previous.rating + maxRatingDifference) > current.rating) return true;
    return false;
  }

  private double getMaxRatingDifferenceForSearch(Long timeSinceSearchStart)
  {
    return this.config.initialRatingDifference + (timeSinceSearchStart * (this.config.ratingDifferenceIncreasePerSecond / 1000.0));
  }
}
