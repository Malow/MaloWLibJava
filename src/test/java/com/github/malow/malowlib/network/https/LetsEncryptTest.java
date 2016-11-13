package com.github.malow.malowlib.network.https;

import org.junit.Test;

public class LetsEncryptTest
{
  @Test
  public void testDownloadCertificates() throws Exception
  {
    String domain = "malow.duckdns.org";
    int port = 7777;

    LetsEncrypt ct = new LetsEncrypt(domain, port);
    ct.downloadCertificates();
  }
}
