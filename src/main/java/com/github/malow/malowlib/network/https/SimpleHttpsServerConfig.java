package com.github.malow.malowlib.network.https;

public class SimpleHttpsServerConfig
{
  public static interface SslConfig
  {

  }

  public static class JksFileConfig implements SslConfig
  {
    public String jksFilePath;

    public JksFileConfig(String jksFilePath)
    {
      this.jksFilePath = jksFilePath;
    }
  }

  public static class LetsEncryptConfig implements SslConfig
  {
    public String letsEncryptFolderPath;

    public LetsEncryptConfig(String letsEncryptFolderPath)
    {
      this.letsEncryptFolderPath = letsEncryptFolderPath;
    }
  }

  public int port;
  public String sslPassword;
  public SslConfig sslConfig;
  public boolean useMultipleThreads = false;

  public SimpleHttpsServerConfig(int port, SslConfig sslConfig, String sslPassword)
  {
    this.port = port;
    this.sslConfig = sslConfig;
    this.sslPassword = sslPassword;
  }
}
