package com.github.malow.malowlib.matchmakingengine;

import com.github.malow.malowlib.ProcessEvent;

public class MatchFoundEvent extends ProcessEvent
{

  public MatchmakingResult matchmakingResult;

  public MatchFoundEvent(MatchmakingResult results)
  {
    this.matchmakingResult = results;
  }
}
