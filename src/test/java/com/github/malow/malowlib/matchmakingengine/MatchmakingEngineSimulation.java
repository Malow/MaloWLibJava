package com.github.malow.malowlib.matchmakingengine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public class MatchmakingEngineSimulation extends MatchmakingEngineTestFixture
{
  private static final Long NR_OF_ENQUEUES = 10000L;
  private static final int TIMEOUT = 10000;

  @Test
  public void testSimulation() throws InterruptedException
  {
    MatchmakingEngineConfig config = new MatchmakingEngineConfig();
    config.matchFinderInterval = Optional.of(100);
    config.initialRatingDifference = 10.0;
    config.ratingDifferenceIncreasePerSecond = 100.0;
    this.matchmakingEngine.updateConfig(config);
    Long startTime = System.currentTimeMillis();
    Long timeElapsed = 0L;
    for (Long l = 0L; l < NR_OF_ENQUEUES; l++)
    {
      this.matchmakingEngine.enqueue(l, 1500.0 + generateRandom(1000));
    }
    Thread.sleep(100);
    while ((this.matchmakingEngine.getPlayersInQueue() > 0) && (timeElapsed < TIMEOUT))
    {
      Thread.sleep(10);
      timeElapsed = System.currentTimeMillis() - startTime;
    }
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(0);
    assertThat(this.testListener.matches).hasSize((int) (NR_OF_ENQUEUES / 2));
    Set<Long> idsWithMatch = new HashSet<>();
    for (MatchmakingResult result : this.testListener.matches)
    {
      assertThat(idsWithMatch.add(result.player1.playerId)).isTrue();
      assertThat(idsWithMatch.add(result.player2.playerId)).isTrue();
    }
    for (Long l = 0L; l < NR_OF_ENQUEUES; l++)
    {
      assertThat(idsWithMatch).contains(l);
    }
  }

  private static Random randomGenerator = new Random();

  public static double generateRandom(int maxAndMin)
  {
    return (randomGenerator.nextDouble() * maxAndMin) - (maxAndMin / 2);
  }
}
