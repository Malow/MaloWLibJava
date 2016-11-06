package com.github.malow.malowlib.network.https;

public class HttpsPostServerConfig
{
  public int port;
  public String certificateFilePath;
  public String certificatePassword;
  public boolean useMultipleThreads = false;

  public HttpsPostServerConfig(int port, String certificateFilePath, String certificatePassword)
  {
    this.port = port;
    this.certificateFilePath = certificateFilePath;
    this.certificatePassword = certificatePassword;
  }
}
