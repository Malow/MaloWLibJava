package com.github.malow.malowlib.network.tcpproxy;

import java.util.Scanner;

import com.github.malow.malowlib.MaloWLogger;

/*
TODO:
Offload writing to syso/file to a separate thread to allow for minimum ms overhead on packets.
 */
public class TcpProxy
{
  public static void main(String[] args)
  {
    MaloWLogger.setLoggingThresholdToInfo();
    TcpProxy tcpProxy = new TcpProxy();
    tcpProxy.run(3724, "127.0.0.1", 3725);
  }

  public void run(int localPort, String remoteIp, int remotePort)
  {
    LocalProxySocketListener sl = new LocalProxySocketListener(localPort, remoteIp, remotePort);
    sl.start();

    String input = "";
    Scanner in = new Scanner(System.in);
    while (!input.equals("exit"))
    {
      System.out.print("> ");
      input = in.next();
    }
    in.close();

    sl.close();
    sl.waitUntillDone();
  }
}
