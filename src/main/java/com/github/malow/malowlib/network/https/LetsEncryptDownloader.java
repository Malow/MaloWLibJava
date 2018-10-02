package com.github.malow.malowlib.network.https;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Registration;
import org.shredzone.acme4j.RegistrationBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeConflictException;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.CertificateUtils;
import org.shredzone.acme4j.util.KeyPairUtils;

import com.github.malow.malowlib.MaloWLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class LetsEncryptDownloader
{
  private static final String FOLDER = "LetsEncryptCerts/";
  private static final File USER_KEY_FILE = new File(FOLDER + "user.key");
  private static final File DOMAIN_KEY_FILE = new File(FOLDER + "domain.key");
  private static final File DOMAIN_CERT_FILE = new File(FOLDER + "domain.crt");
  private static final File CERT_CHAIN_FILE = new File(FOLDER + "chain.crt");
  private static final File DOMAIN_CSR_FILE = new File(FOLDER + "domain.csr");

  private static final int KEY_SIZE = 2048;

  private String domain;
  private int port;
  private HttpServer httpServer;

  public static void main(String[] args)
  {
    String domain = "malow.duckdns.org";
    int port = 7777;

    LetsEncryptDownloader ct = new LetsEncryptDownloader(domain, port);
    try
    {
      ct.downloadCertificates();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to download certificates", e);
    }
  }

  /**
   *
   * @param domain
   *          Example: "malow.mooo.com"
   * @param port
   *          The port which port 80 is forwarded to.
   */
  public LetsEncryptDownloader(String domain, int port)
  {
    this.domain = domain;
    this.port = port;
  }

  private static class ChallangeHandler implements HttpHandler
  {
    private String fileContent;

    public ChallangeHandler(String fileContent)
    {
      this.fileContent = fileContent;
    }

    @Override
    public void handle(HttpExchange t) throws IOException
    {
      t.sendResponseHeaders(200, this.fileContent.length());
      OutputStream os = t.getResponseBody();
      os.write(this.fileContent.getBytes(StandardCharsets.UTF_8));
      os.close();
    }
  }

  public void downloadCertificates() throws Exception
  {
    USER_KEY_FILE.getParentFile().mkdirs();
    Security.addProvider(new BouncyCastleProvider());
    // Use "acme://letsencrypt.org" for production server
    //Session session = new Session("acme://letsencrypt.org/staging", this.getOrCreateKeyPair(USER_KEY_FILE));
    Session session = new Session("acme://letsencrypt.org", this.getOrCreateKeyPair(USER_KEY_FILE));

    Registration reg = this.getOrCreateRegistration(session);

    URI agreement = reg.getAgreement();
    reg.modify().setAgreement(agreement).commit();

    Authorization auth = this.createAuthorization(this.domain, reg, agreement);

    Http01Challenge challenge = this.createChallange(this.domain, auth);
    if (challenge == null)
    {
      return;
    }

    // Setup HTTP server for the challange
    String fileName = challenge.getToken();
    String fileContent = challenge.getAuthorization();
    this.startHttpServerForChallange(fileName, fileContent);

    challenge.trigger();
    int attempts = 10;
    while (challenge.getStatus() != Status.VALID && attempts-- > 0)
    {
      if (challenge.getStatus() == Status.INVALID)
      {
        MaloWLogger.error("Challenge failed... Giving up.", new Exception());
        return;
      }
      try
      {
        Thread.sleep(3000L);
      }
      catch (InterruptedException ex)
      {
        MaloWLogger.error("Interrupted", ex);
      }
      challenge.update();
    }
    if (challenge.getStatus() != Status.VALID)
    {
      MaloWLogger.error("Failed to pass the challenge... Giving up.", new Exception());
      return;
    }

    KeyPair domainKeyPair = this.getOrCreateKeyPair(DOMAIN_KEY_FILE);

    CSRBuilder csrb = new CSRBuilder();
    csrb.addDomain(this.domain);
    csrb.sign(domainKeyPair);

    try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(DOMAIN_CSR_FILE), StandardCharsets.UTF_8))
    {
      csrb.write(out);
    }

    Certificate certificate = reg.requestCertificate(csrb.getEncoded());
    X509Certificate cert = certificate.download();

    try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(DOMAIN_CERT_FILE), StandardCharsets.UTF_8))
    {
      CertificateUtils.writeX509Certificate(cert, out);
    }

    X509Certificate[] chain = certificate.downloadChain();
    try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(CERT_CHAIN_FILE), StandardCharsets.UTF_8))
    {
      CertificateUtils.writeX509CertificateChain(chain, out);
    }

    // Revoke the certificate (uncomment if needed...)
    // certificate.revoke();

    this.closeHttpServer();
  }

  private KeyPair getOrCreateKeyPair(File file) throws Exception
  {
    KeyPair keyPair;
    if (file.exists())
    {
      try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
      {
        keyPair = KeyPairUtils.readKeyPair(isr);
      }
    }
    else
    {
      keyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
      try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
      {
        KeyPairUtils.writeKeyPair(keyPair, out);
      }
    }
    return keyPair;
  }

  private Registration getOrCreateRegistration(Session session) throws AcmeException
  {
    Registration reg = null;
    try
    {
      reg = new RegistrationBuilder().create(session);
    }
    catch (AcmeConflictException ex)
    {
      reg = Registration.bind(session, ex.getLocation());
    }
    return reg;
  }

  private Authorization createAuthorization(String domain, Registration reg, URI agreement) throws AcmeException
  {
    Authorization auth = null;
    auth = reg.authorizeDomain(domain);
    return auth;
  }

  private Http01Challenge createChallange(String domain, Authorization auth)
  {
    Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
    if (challenge == null)
    {
      MaloWLogger.error("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...", new Exception());
      return null;
    }
    return challenge;
  }

  private void startHttpServerForChallange(String filename, String fileContent) throws Exception
  {
    if (this.httpServer != null)
    {
      this.closeHttpServer();
    }

    this.httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
    this.httpServer.createContext("/.well-known/acme-challenge/" + filename, new ChallangeHandler(fileContent));
    this.httpServer.setExecutor(null); // creates a default executor
    this.httpServer.start();
  }

  private void closeHttpServer()
  {
    this.httpServer.stop(0);
    this.httpServer = null;
  }
}