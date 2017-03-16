package com.github.malow.malowlib.matchmakingengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;

import org.junit.Test;

public class MatchmakingEngineTest extends MatchmakingEngineTestFixture
{
  @Test
  public void testMatchIsFoundAfter500ms() throws InterruptedException
  {
    MatchmakingEngineConfig config = new MatchmakingEngineConfig();
    config.matchFinderInterval = Optional.empty();
    config.initialRatingDifference = 10.0;
    config.ratingDifferenceIncreasePerSecond = 10.0;
    this.matchmakingEngine.updateConfig(config);
    Long startTime = System.currentTimeMillis();
    Long timeElapsed = 0L;
    this.matchmakingEngine.enqueue(0L, 100.0);
    this.matchmakingEngine.enqueue(1L, 115.0);
    Thread.sleep(20);
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(2);
    while (this.testListener.matches.isEmpty() && (timeElapsed < 600))
    {
      Thread.sleep(10);
      timeElapsed = System.currentTimeMillis() - startTime;
    }
    timeElapsed = System.currentTimeMillis() - startTime;
    assertThat(timeElapsed).isCloseTo(500L, within(30L));
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(0);
    assertThat(this.testListener.matches).hasSize(1);
    assertThat(this.testListener.matches.get(0)).isEqualTo(new MatchmakingResult(new MatchmakingPlayer(0L), new MatchmakingPlayer(1L)));
  }

  @Test
  public void testMatchIsNotFoundAfter600ms() throws InterruptedException
  {
    MatchmakingEngineConfig config = new MatchmakingEngineConfig();
    config.matchFinderInterval = Optional.empty();
    config.initialRatingDifference = 10.0;
    config.ratingDifferenceIncreasePerSecond = 10.0;
    this.matchmakingEngine.updateConfig(config);
    Long startTime = System.currentTimeMillis();
    Long timeElapsed = 0L;
    this.matchmakingEngine.enqueue(0L, 100.0);
    this.matchmakingEngine.enqueue(1L, 120.0);
    Thread.sleep(20);
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(2);
    while (this.testListener.matches.isEmpty() && (timeElapsed < 600))
    {
      Thread.sleep(10);
      timeElapsed = System.currentTimeMillis() - startTime;
    }
    timeElapsed = System.currentTimeMillis() - startTime;
    assertThat(timeElapsed).isCloseTo(600L, within(15L));
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(2);
    assertThat(this.testListener.matches).hasSize(0);
  }

  @Test
  public void testMatchIsFoundAfter1000msWith1000msInterval() throws InterruptedException
  {
    MatchmakingEngineConfig config = new MatchmakingEngineConfig();
    config.matchFinderInterval = Optional.of(1000);
    config.initialRatingDifference = 10.0;
    config.ratingDifferenceIncreasePerSecond = 100.0;
    this.matchmakingEngine.updateConfig(config);
    Long startTime = System.currentTimeMillis();
    Long timeElapsed = 0L;
    this.matchmakingEngine.enqueue(0L, 100.0);
    this.matchmakingEngine.enqueue(1L, 120.0);
    Thread.sleep(20);
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(2);
    while (this.testListener.matches.isEmpty() && (timeElapsed < 1100))
    {
      Thread.sleep(10);
      timeElapsed = System.currentTimeMillis() - startTime;
    }
    timeElapsed = System.currentTimeMillis() - startTime;
    assertThat(timeElapsed).isCloseTo(1000L, within(15L));
    assertThat(this.matchmakingEngine.getPlayersInQueue()).isEqualTo(0);
    assertThat(this.testListener.matches).hasSize(1);
    assertThat(this.testListener.matches.get(0)).isEqualTo(new MatchmakingResult(new MatchmakingPlayer(0L), new MatchmakingPlayer(1L)));
  }
}
