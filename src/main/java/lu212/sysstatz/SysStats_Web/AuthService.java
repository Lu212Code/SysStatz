package lu212.sysstatz.SysStats_Web;

import jakarta.servlet.http.HttpSession;
import lu212.sysstatz.General.User;
import lu212.sysstatz.General.UserStore;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class AuthService {

    // 1. Login Username/Passwort prüfen
	public static boolean verifyUsernamePassword(String username, String password, HttpSession session) {
	    User user = UserStore.getUserByName(username);
	    if (user != null && user.getPassword().equals(password)) {
	        session.setAttribute("username", user.getUsername());
	        session.setAttribute("isAdmin", user.isAdmin());
	        session.setAttribute("passwordVerified", true);

	        // Wenn 2FA nicht aktiviert: direkt loggedIn = true
	        if (!user.isTwoFactorEnabled()) {
	            session.setAttribute("loggedIn", true);
	        }
	        return true;
	    }
	    return false;
	}

    // 2. 2FA Code prüfen
    public static boolean verifyTwoFactorCode(String username, String code) {
        User user = UserStore.getUserByName(username);
        if (user == null || !user.isTwoFactorEnabled()) return false;

        String secretBase32 = user.getTwoFactorSecret();
        if (secretBase32 == null) return false;

        Totp totp = new Totp(secretBase32);
        return totp.verify(code);
    }

    // 3. Neues Secret generieren (Base32!)
    public static String generateNewSecret() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA1");
            keyGenerator.init(160); // 160 bit für SHA1
            SecretKey key = keyGenerator.generateKey();

            // Secret im Base32-Format (für Authenticator-Apps)
            return Base32.encode(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean verifyTwoFactorCodeWithSecret(String secret, String code) {
        if (secret == null) return false;
        Totp totp = new Totp(secret);
        return totp.verify(code);
    }
}
