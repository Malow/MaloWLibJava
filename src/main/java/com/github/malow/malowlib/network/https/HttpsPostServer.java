package com.github.malow.malowlib.network.https;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.github.malow.malowlib.MaloWLogger;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HttpsPostServer
{
  private HttpsServer server = null;
  private ExecutorService executorService = null;

  public HttpsPostServer(HttpsPostServerConfig config)
  {
    this.initHttpsServer(config.port, config.certificateFilePath, config.certificatePassword, config.useMultipleThreads);
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
    if (this.server != null) this.server.stop(0);
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

  public void initHttpsServer(int port, String certificateFilePath, String sslPassword, boolean useMultipleThreads)
  {
    try
    {
      this.server = HttpsServer.create(new InetSocketAddress(port), 0);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      char[] password = sslPassword.toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      FileInputStream fis = new FileInputStream(certificateFilePath);
      ks.load(fis, password);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, password);
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
            SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
            params.setSSLParameters(defaultSSLParameters);
          }
          catch (Exception e)
          {
            MaloWLogger.error("Failed to create HTTPS port", e);
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
    catch (Exception e)
    {
      MaloWLogger.error("Exception while starting HttpsPostServer", e);
    }
  }
}
