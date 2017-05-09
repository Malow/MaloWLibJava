package com.github.malow.malowlib.matchmakingengine;

import java.util.Optional;

public class MatchmakingEngineConfig
{

  /**
   * If specified MatchmakingEngine will sleep between searches for matches if it finishes a search before the interval for the next one, and it will warn if it
   * can't finish the execution within its interval window. Number is specified in milliseconds.
   */
  public Optional<Integer> matchFinderInterval = Optional.of(1000);
  /**
   * Each player has a rating difference range that it uses to match against other players. This value sets the initial range window. IE. with this value set to
   * 10 a player with 1000 rating will be matched with one with 1009 rating instantly.
   */
  public double initialRatingDifference = 10;
  /**
   * Each player has a rating difference range that it uses to match against other players. This range will increase with each second by the amount set here.
   */
  public double ratingDifferenceIncreasePerSecond = 1;
  /**
   * If specified, it will determine the maximum amount of rating difference between two players for a match to be found.
   */
  public Optional<Double> maxRatingDifference = Optional.empty();
}
