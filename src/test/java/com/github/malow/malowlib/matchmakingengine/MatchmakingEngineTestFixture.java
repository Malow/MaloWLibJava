package com.github.malow.malowlib.matchmakingengine;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.malowprocess.ProcessEvent;

public class MatchmakingEngineTestFixture
{
  public static class TestListener extends MaloWProcess
  {
    public List<MatchmakingResult> matches = new ArrayList<>();

    @Override
    public void life()
    {
      while (this.stayAlive)
      {
        ProcessEvent ev = this.waitEvent();
        if (ev instanceof MatchFoundEvent)
        {
          MatchFoundEvent mfe = (MatchFoundEvent) ev;
          this.matches.add(mfe.matchmakingResult);
        }
      }
    }

    @Override
    public void closeSpecific()
    {
    }
  }

  TestListener testListener;
  MatchmakingEngine matchmakingEngine;

  @Before
  public void before()
  {
    this.testListener = new TestListener();
    this.testListener.start();
    this.matchmakingEngine = new MatchmakingEngine(new MatchmakingEngineConfig(), this.testListener);
    this.matchmakingEngine.start();
  }

  @After
  public void after()
  {
    this.matchmakingEngine.close();
    this.matchmakingEngine.waitUntillDone();
    this.testListener.close();
    this.testListener.waitUntillDone();
  }
}
