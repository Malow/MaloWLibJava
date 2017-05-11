package com.github.malow.malowlib.network.https;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

import org.shredzone.acme4j.util.KeyPairUtils;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.network.https.HttpsPostServerConfig.JksFileConfig;
import com.github.malow.malowlib.network.https.HttpsPostServerConfig.LetsEncryptConfig;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HttpsPostServer
{
  private int port;
  private HttpsServer server = null;
  private ExecutorService executorService = null;

  public HttpsPostServer(HttpsPostServerConfig config)
  {
    if (config.sslConfig instanceof JksFileConfig)
    {
      this.initUsingJksFile(config.port, ((JksFileConfig) config.sslConfig).jksFilePath, config.sslPassword, config.useMultipleThreads);
    }
    else if (config.sslConfig instanceof LetsEncryptConfig)
    {
      this.initUsingLetsEncrypt(config.port, ((LetsEncryptConfig) config.sslConfig).letsEncryptFolderPath, config.sslPassword,
          config.useMultipleThreads);
    }
    else
    {
      MaloWLogger.error("Bad ssl config: " + config.sslConfig, new Exception());
    }
  }

  public int getPort()
  {
    return this.port;
  }

  public void createContext(String path, HttpsPostHandler handler)
  {
    this.server.createContext(path, handler);
  }

  public void start()
  {
    this.server.start();
  }

  public void close()
  {
    if (this.server != null)
    {
      this.server.stop(0);
    }
    if (this.executorService != null)
    {
      this.executorService.shutdown();
      try
      {
        if (!this.executorService.awaitTermination(5000, TimeUnit.MILLISECONDS))
        {
          MaloWLogger.error("HttpsPostServer executorService termination timeout reached", new Exception());
        }
      }
      catch (InterruptedException e)
      {
        MaloWLogger.error("HttpsPostServer executorService termination failed", e);
      }
    }
  }

  private void initUsingJksFile(int port, String jksFilePath, String sslPassword, boolean useMultipleThreads)
  {
    try
    {
      this.init(port, this.getKeyStoreForLocalFile(sslPassword, jksFilePath), sslPassword, useMultipleThreads);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to init HTTPS Server", e);
    }
  }

  private void initUsingLetsEncrypt(int port, String letsEncryptFolderPath, String sslPassword, boolean useMultipleThreads)
  {
    try
    {
      this.init(port, this.getKeyStoreForLetsEncrypt(sslPassword, letsEncryptFolderPath), sslPassword, useMultipleThreads);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to init HTTPS Server", e);
    }
  }

  private void init(int port, KeyStore ks, String sslPassword, boolean useMultipleThreads) throws Exception
  {
    this.port = port;
    this.server = HttpsServer.create(new InetSocketAddress(port), 0);
    SSLContext sslContext = SSLContext.getInstance("TLS");

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(ks, sslPassword.toCharArray());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(ks);
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    this.server.setHttpsConfigurator(new HttpsConfigurator(sslContext)
    {
      @Override
      public void configure(HttpsParameters params)
      {
        try
        {
          SSLContext c = SSLContext.getDefault();
          SSLEngine engine = c.createSSLEngine();
          params.setNeedClientAuth(false);
          params.setCipherSuites(engine.getEnabledCipherSuites());
          params.setProtocols(engine.getEnabledProtocols());
          params.setSSLParameters(c.getDefaultSSLParameters());
        }
        catch (Exception e)
        {
          MaloWLogger.error("Failed to configure HTTPS Server", e);
        }
      }
    });
    if (useMultipleThreads)
    {
      this.executorService = Executors.newCachedThreadPool();
      this.server.setExecutor(this.executorService);
    }
    else
    {
      this.server.setExecutor(null); // creates a default executor
    }
  }

  private KeyStore getKeyStoreForLetsEncrypt(String sslPassword, String letsEncryptFolderPath) throws Exception
  {
    KeyPair kp;
    try (InputStreamReader isr = new InputStreamReader(new FileInputStream(letsEncryptFolderPath + "/domain.key"), StandardCharsets.UTF_8))
    {
      kp = KeyPairUtils.readKeyPair(isr);
    }

    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Certificate cert0;
    try (FileInputStream cert0Input = new FileInputStream(letsEncryptFolderPath + "/domain.crt"))
    {
      cert0 = cf.generateCertificate(cert0Input);
    }
    Certificate cert1;
    try (FileInputStream cert1Input = new FileInputStream(letsEncryptFolderPath + "/chain.crt"))
    {
      cert1 = cf.generateCertificate(cert1Input);
    }

    KeyStore ks = KeyStore.getInstance("jks"); // type doesn't really matter since it's in memory only
    ks.load(null);
    ks.setKeyEntry("anyalias", kp.getPrivate(), sslPassword.toCharArray(), new Certificate[] { cert0, cert1 });
    return ks;
  }

  private KeyStore getKeyStoreForLocalFile(String sslPassword, String certificateFilePath) throws Exception
  {
    KeyStore ks = KeyStore.getInstance("JKS");
    try (FileInputStream fis = new FileInputStream(certificateFilePath))
    {
      ks.load(fis, sslPassword.toCharArray());
    }
    return ks;
  }
}
