package com.github.malow.malowlib.matchmakingengine;

import java.util.List;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class MatchmakingEngine extends MaloWProcess
{

  public static class QueuePlayerEvent extends ProcessEvent
  {
    public boolean enqueue;
    public MatchmakingPlayer player;

    public QueuePlayerEvent(boolean enqueue, MatchmakingPlayer player)
    {
      this.enqueue = enqueue;
      this.player = player;
    }
  }

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
    this.putEvent(new QueuePlayerEvent(true, player));
  }

  public void dequeue(Integer playerId)
  {
    MatchmakingPlayer player = new MatchmakingPlayer();
    player.playerId = playerId;
    this.putEvent(new QueuePlayerEvent(false, player));
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
    return this.playerPool.getSize();
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      long startTime = System.currentTimeMillis();
      this.handleAllPendingEvents();
      List<MatchmakingResult> results = this.playerPool.createMatches();
      for (MatchmakingResult result : results)
      {
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
            // Handle events while sleeping between match-iterations.
            Long lastIteration = System.currentTimeMillis();
            while (timeToSleep > 0)
            {
              this.handleAllPendingEvents();
              Thread.sleep(5);
              timeToSleep -= System.currentTimeMillis() - lastIteration;
              lastIteration = System.currentTimeMillis();
            }
          }
          catch (Exception e)
          {
          }
        }
      }
    }
  }

  private void handleAllPendingEvents()
  {
    ProcessEvent processEvent = this.peekEvent();
    while (processEvent != null)
    {
      if (processEvent instanceof QueuePlayerEvent)
      {
        QueuePlayerEvent queuePlayerEvent = (QueuePlayerEvent) processEvent;
        if (queuePlayerEvent.enqueue)
        {
          this.playerPool.add(queuePlayerEvent.player);
        }
        else
        {
          this.playerPool.remove(queuePlayerEvent.player);
        }
      }
      processEvent = this.peekEvent();
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
