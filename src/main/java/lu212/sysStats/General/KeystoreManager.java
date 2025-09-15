package lu212.sysStats.General;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class KeystoreManager {

    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final String PASS_FILE = "keystore.pass.enc";
    private static final String AES_KEY_FILE = "aes.key";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyStore loadOrCreateKeystore() throws Exception {
        File ksFile = new File(KEYSTORE_FILE);

        if (ksFile.exists()) {
            String password = decryptPassword();
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                ks.load(fis, password.toCharArray());
            }
            System.out.println("[OK] Keystore geladen.");

            // ----- Hier Zertifikat prüfen und ggf. verlängern -----
            String alias = ks.aliases().nextElement();
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            if (cert.getNotAfter().before(new Date())) {
                System.out.println("[WARN] Zertifikat abgelaufen. Erstelle neues Zertifikat...");
                createKeystore(password);  // überschreibt alten Keystore
                // Keystore neu laden
                try (FileInputStream fis = new FileInputStream(ksFile)) {
                    ks.load(fis, password.toCharArray());
                }
                System.out.println("[OK] Neuer Keystore geladen.");
            }
            // ------------------------------------------------------

            return ks;
        } else {
            String password = generateRandomPassword();
            saveEncryptedPassword(password);
            createKeystore(password);
            System.out.println("[OK] Neuer Keystore erstellt.");
            return loadOrCreateKeystore();
        }
    }

    private static void createKeystore(String password) throws Exception {
        // RSA KeyPair erzeugen
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Selbstsigniertes Zertifikat mit BouncyCastle erzeugen
        X509Certificate cert = generateSelfSignedCertificate(keyPair);

        // Keystore anlegen und speichern
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("server", keyPair.getPrivate(), password.toCharArray(), new X509Certificate[]{cert});
        try (FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE)) {
            ks.store(fos, password.toCharArray());
        }
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + TimeUnit.DAYS.toMillis(3650)); // 10 Jahre gültig

        X500Name dnName = new X500Name("CN=SysStatsServer, O=MyOrg, L=MyCity, C=DE");
        BigInteger serial = BigInteger.valueOf(now);

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, serial, startDate, endDate, dnName, keyPair.getPublic());

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(contentSigner));
    }

    private static String generateRandomPassword() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[16];
        sr.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static void saveEncryptedPassword(String password) throws Exception {
        SecretKey aesKey;
        File aesFile = new File(AES_KEY_FILE);

        if (aesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(aesFile)) {
                byte[] keyBytes = fis.readAllBytes();
                aesKey = new SecretKeySpec(keyBytes, "AES");
            }
        } else {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            aesKey = keyGen.generateKey();
            try (FileOutputStream fos = new FileOutputStream(aesFile)) {
                fos.write(aesKey.getEncoded());
            }
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(password.getBytes());

        try (FileOutputStream fos = new FileOutputStream(PASS_FILE)) {
            fos.write(encrypted);
        }
    }

    private static String decryptPassword() throws Exception {
        byte[] keyBytes;
        try (FileInputStream fis = new FileInputStream(AES_KEY_FILE)) {
            keyBytes = fis.readAllBytes();
        }
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        byte[] encPass;
        try (FileInputStream fis = new FileInputStream(PASS_FILE)) {
            encPass = fis.readAllBytes();
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return new String(cipher.doFinal(encPass));
    }
    
    public static String loadKeystorePassword() throws Exception {
        return decryptPassword();
    }

    public static void main(String[] args) {
        try {
            KeyStore ks = loadOrCreateKeystore();
            System.out.println("Alias im Keystore: " + ks.aliases().nextElement());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}