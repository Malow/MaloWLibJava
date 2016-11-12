package com.github.malow.malowlib.network.https;

import org.junit.Test;

public class LetsEncryptTest
{
  @Test
  public void test() throws Exception
  {
    String domain = "malow.mooo.com";
    int port = 7777;

    LetsEncrypt ct = new LetsEncrypt(domain, port);
    ct.downloadCertificates();
  }
}
