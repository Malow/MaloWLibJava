package com.github.malow.malowlib.matchmakingengine;

import java.util.List;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class MatchmakingEngine extends MaloWProcess
{
  private PlayerPool playerPool;
  private MaloWProcess matchListener;
  private MatchmakingEngineConfig config;

  public MatchmakingEngine(MatchmakingEngineConfig config, MaloWProcess matchListener)
  {
    this.matchListener = matchListener;
    this.config = config;
    this.playerPool = new PlayerPool(config);
  }

  public void enqueue(Integer playerId, Double rating)
  {
    MatchmakingPlayer player = new MatchmakingPlayer();
    player.playerId = playerId;
    player.rating = rating;
    player.timeAdded = System.currentTimeMillis();
    MaloWLogger.info("MatchmakingEngine enqueued player with id " + player.playerId + ".");
    this.playerPool.add(player);
  }

  public boolean dequeue(Integer playerId)
  {
    MatchmakingPlayer player = new MatchmakingPlayer();
    player.playerId = playerId;
    MaloWLogger.info("MatchmakingEngine dequeued player with id " + player.playerId + ".");
    return this.playerPool.remove(player);
  }

  public void updateConfig(MatchmakingEngineConfig config)
  {
    this.config = config;
    this.playerPool.updateConfig(config);
  }

  public MatchmakingEngineConfig getConfig()
  {
    return this.config;
  }

  public int getNumberOfPlayersInQueue()
  {
    return this.playerPool.size();
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      long startTime = System.currentTimeMillis();
      List<MatchmakingResult> results = this.playerPool.createMatches();
      for (MatchmakingResult result : results)
      {
        MaloWLogger.info("MatchmakingEngine found match between player ids: " + result.player1.playerId + " and " + result.player2.playerId + ".");
        this.matchListener.putEvent(new MatchFoundEvent(result));
      }
      if (this.config.matchFinderInterval.isPresent())
      {
        Long timeToSleep = this.config.matchFinderInterval.get() - (System.currentTimeMillis() - startTime);
        if (timeToSleep < 0)
        {
          MaloWLogger.warning("MatchmakingEngine is overloaded and has a negative timeToSleep: " + timeToSleep);
        }
        else
        {
          try
          {
            Thread.sleep(timeToSleep);
          }
          catch (Exception e)
          {
            MaloWLogger.error("Failed to sleep", e);
          }
        }
      }
    }
  }

  @Override
  public void closeSpecific()
  {
  }

  public void clearQueue()
  {
    this.playerPool.clear();
  }
}
