package com.github.malow.malowlib.matchmakingengine;

import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class MatchFoundEvent extends ProcessEvent
{

  public MatchmakingResult matchmakingResult;

  public MatchFoundEvent(MatchmakingResult results)
  {
    this.matchmakingResult = results;
  }
}
