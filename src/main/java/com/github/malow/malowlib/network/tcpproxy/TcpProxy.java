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
    tcpProxy.run(8085, "140.89.160.249", 80);
  }

  public void run(int localPort, String remoteIp, int remotePort)
  {
    LocalProxySocketListener sl = new LocalProxySocketListener(localPort, remoteIp, remotePort);
    sl.start();

    String input = "";
    Scanner in = new Scanner(System.in);
    while (!input.toLowerCase().equals("exit"))
    {
      System.out.print("> ");
      input = in.next();
    }
    in.close();

    sl.close();
    sl.waitUntillDone();
  }

  /*
  private byte[] toByteArray(String s) throws DecoderException
  {
    s = s.replaceAll(" ", "");
    return Hex.decodeHex(s.toCharArray());
  }
  */
}
