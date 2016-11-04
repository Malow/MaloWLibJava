package com.github.malow.malowlib.network.https;

public class HttpsPostServerConfig
{
  public int port;
  public String certificateFilePath;
  public String certificatePassword;
  public String testPath = "/test";
  public String loginPath = "/login";
  public String registerPath = "/register";
  public String sendPwResetTokenPath = "/sendpwresettoken";
  public String resetPwPath = "/resetpw";
  public String clearCachePath = "/clearcache";

  public HttpsPostServerConfig(int port, String certificateFilePath, String certificatePassword)
  {
    this.port = port;
    this.certificateFilePath = certificateFilePath;
    this.certificatePassword = certificatePassword;
  }
}
