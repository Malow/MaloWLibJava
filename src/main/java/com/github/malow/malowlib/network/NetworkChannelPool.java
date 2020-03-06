package com.github.malow.malowlib.network;

import java.util.ArrayList;
import java.util.List;

import com.github.malow.malowlib.Pair;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

@Deprecated
// Not yet implemented
public class NetworkChannelPool extends MaloWProcess
{
  private List<NetworkChannel> channels = new ArrayList<>();
  private Integer nextSliceStart = 0;
  private int threadCount = 0;
  private long lastFinishedIteration = 0;
  private int targetUpdatesPerSecond;

  public NetworkChannelPool(int threadCount, int targetUpdatesPerSecond)
  {
    super(threadCount);
    this.threadCount = threadCount;
    this.targetUpdatesPerSecond = targetUpdatesPerSecond;
  }

  public void addChannel(NetworkChannel channel)
  {
    this.channels.add(channel);
  }

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      long now = System.currentTimeMillis();
      long sleepDuration = this.lastFinishedIteration + 1000 / this.targetUpdatesPerSecond - now;
      if (sleepDuration > 0)
      {
        try
        {
          Thread.sleep(sleepDuration);
        }
        catch (InterruptedException e)
        {
        }
      }
      Pair<Integer, Integer> workSlice = this.getWorkSlice();
      for (int i = workSlice.first; i < workSlice.second; i++)
      {
        try
        {
          this.channels.get(i).update();
        }
        catch (Exception e)
        {
        }
      }
    }
  }

  private synchronized Pair<Integer, Integer> getWorkSlice()
  {
    int channelsSize = this.channels.size();
    int sliceSize = channelsSize / this.threadCount + 1;
    int start = this.nextSliceStart;
    int end = start + sliceSize;
    if (end > channelsSize)
    {
      end = channelsSize;
    }
    this.nextSliceStart = end + 1;
    if (this.nextSliceStart > channelsSize)
    {
      this.nextSliceStart = 0;
      this.lastFinishedIteration = System.currentTimeMillis();
    }
    return new Pair<Integer, Integer>(start, end);
  }
}
