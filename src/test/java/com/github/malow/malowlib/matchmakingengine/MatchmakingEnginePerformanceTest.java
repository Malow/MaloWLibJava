package com.github.malow.malowlib.matchmakingengine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.Test;


public class MatchmakingEnginePerformanceTest extends MatchmakingEngineTestFixture
{
  private static final Long NR_OF_ENQUEUES = 30000L;
  private static final int EXPECTED_MATCHES = (int) (NR_OF_ENQUEUES / 2);

  @Test
  public void testPerformance() throws InterruptedException
  {
    MatchmakingEngineConfig config = new MatchmakingEngineConfig();
    config.matchFinderInterval = Optional.empty();
    config.initialRatingDifference = 10000.0;
    config.ratingDifferenceIncreasePerSecond = 100.0;
    this.matchmakingEngine.updateConfig(config);
    Long before = System.nanoTime();
    for (Integer l = 0; l < NR_OF_ENQUEUES; l++)
    {
      this.matchmakingEngine.enqueue(l, 1500.0 + generateRandom(1000));
    }
    while (this.matchmakingEngine.getEventQueueSize() > 0 || this.matchmakingEngine.getNumberOfPlayersInQueue() > 0
        || this.testListener.getEventQueueSize() > 0 || this.testListener.matches.size() < EXPECTED_MATCHES)
    {
    }
    long elapsed = System.nanoTime() - before;
    System.out.println(elapsed / 1000000.0 + "ms.");

    assertThat(this.testListener.matches.size()).isEqualTo(EXPECTED_MATCHES);
    Set<Integer> playersWithMatchIds = new HashSet<>();
    for (MatchmakingResult result : this.testListener.matches)
    {
      assertThat(playersWithMatchIds.add(result.player1.playerId)).isTrue();
      assertThat(playersWithMatchIds.add(result.player2.playerId)).isTrue();
    }
    for (Integer l = 0; l < NR_OF_ENQUEUES; l++)
    {
      assertThat(playersWithMatchIds).contains(l);
    }
  }

  private static Random randomGenerator = new Random();

  public static double generateRandom(int maxAndMin)
  {
    return randomGenerator.nextDouble() * maxAndMin - maxAndMin / 2;
  }
}
