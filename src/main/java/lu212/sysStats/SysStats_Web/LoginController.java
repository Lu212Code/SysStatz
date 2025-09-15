package lu212.sysStats.SysStats_Web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.QRCodeUtil;
import lu212.sysStats.General.User;
import lu212.sysStats.General.UserStore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String showLoginPage(@RequestParam(value = "error", required = false) String error, Model model) {

		if (!UserStore.hasUsers()) {
			return "redirect:/createadminuser";
		}

		if (error != null) {
			model.addAttribute("error", "Falsches Passwort");
		}
		String theme = SysStatsWebApplication.theme;
		model.addAttribute("theme", theme);
		return "login";
	}

	@PostMapping("/login")
	public String handleLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {
	    if (AuthService.verifyUsernamePassword(username, password, session)) {
	        User user = UserStore.getUserByName(username);

	        String require2fa = SysStatsWebApplication.twoFactorRequired;

	        if (require2fa.equalsIgnoreCase("true")) {
	            // Wenn 2FA global erzwungen wird, muss der User 2FA aktiviert haben
	            if (user.isTwoFactorEnabled()) {
	                return "redirect:/2fa";
	            } else {
	                // User muss 2FA jetzt einrichten
	                String secret = AuthService.generateNewSecret();
	                session.setAttribute("tempSecret", secret);
	                session.setAttribute("username", username);
	                session.setAttribute("passwordVerified", true);
	                return "redirect:/2fa-setup";
	            }
	        } else {
	            // 2FA ist nicht global erzwungen - nur User mit 2FA werden abgefragt
	            if (user.isTwoFactorEnabled()) {
	                return "redirect:/2fa";
	            } else {
	                // Direkter Login ohne 2FA
	                session.setAttribute("loggedIn", true);
	                session.setAttribute("isAdmin", user.isAdmin());
	                return "redirect:/dashboard";
	            }
	        }
	    } else {
	        return "redirect:/login?error=true";
	    }
	}
	
	@GetMapping("/2fa-setup")
	public String show2faSetup(HttpSession session, Model model) {
	    String secret = (String) session.getAttribute("tempSecret");
	    String username = (String) session.getAttribute("username");
	    Boolean pwVerified = (Boolean) session.getAttribute("passwordVerified");

	    if (secret == null || username == null || pwVerified == null || !pwVerified) {
	        return "redirect:/login?error=true";
	    }

	    try {
	        String rawOtpAuthUrl = String.format("otpauth://totp/SysStatz:%s?secret=%s&issuer=SysStatz", username, secret);

	        // Realer Pfad im Webserver (/static/qr)
	        ServletContext context = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
	                .getRequest().getServletContext();
	        String realPath = context.getRealPath("/qr/");

	        // Sicherstellen, dass Ordner existiert
	        Files.createDirectories(Paths.get(realPath));

	        // Dateiname und Speicherpfad
	        String fileName = UUID.randomUUID() + ".png";
	        Path qrPath = Paths.get(realPath, fileName);

	        QRCodeUtil.generateQRCode(rawOtpAuthUrl, qrPath, 200, 200);

	        // Webzugänglicher Pfad
	        model.addAttribute("qrImagePath", "/qr/" + fileName);
	    } catch (Exception e) {
	        e.printStackTrace();
	        model.addAttribute("qrError", "Fehler beim Generieren des QR-Codes: " + e.getMessage());
	    }

	    model.addAttribute("secret", secret);
	    model.addAttribute("theme", SysStatsWebApplication.theme);
	    return "2fa-setup";
	}

	@PostMapping("/2fa-setup")
	public String handle2faSetup(@RequestParam String code, HttpSession session) {
	    String secret = (String) session.getAttribute("tempSecret");
	    String username = (String) session.getAttribute("username");

	    if (secret == null || username == null) {
	        return "redirect:/login?error=true";
	    }

	    if (AuthService.verifyTwoFactorCodeWithSecret(secret, code)) {
	        // 2FA für User aktivieren
	        User user = UserStore.getUserByName(username);
	        user.setTwoFactorSecret(secret);
	        user.setTwoFactorEnabled(true);
	        UserStore.saveUsers();

	        // Session als eingeloggt markieren
	        session.setAttribute("loggedIn", true);
	        session.setAttribute("isAdmin", user.isAdmin());
	        session.removeAttribute("tempSecret");
	        session.removeAttribute("passwordVerified");

	        return "redirect:/dashboard";
	    } else {
	        return "redirect:/2fa-setup?error=true";
	    }
	}

	
	@GetMapping("/2fa")
	public String show2faPage(HttpSession session, Model model) {
	    Boolean pwVerified = (Boolean) session.getAttribute("passwordVerified");
	    if (pwVerified == null || !pwVerified) {
	        return "redirect:/login?error=true";
	    }
	    String theme = SysStatsWebApplication.theme;
	    model.addAttribute("theme", theme);
	    return "2fa"; // Thymeleaf Seite mit Code Eingabe
	}

	@PostMapping("/2fa")
	public String handle2faCode(@RequestParam String code, HttpSession session) {
	    String username = (String) session.getAttribute("username");
	    if (username == null) {
	        return "redirect:/login?error=true";
	    }

	    if (AuthService.verifyTwoFactorCode(username, code)) {
	        session.setAttribute("loggedIn", true);
	        User user = UserStore.getUserByName(username);
	        session.setAttribute("isAdmin", user.isAdmin());
	        session.removeAttribute("passwordVerified"); // aufräumen
	        return "redirect:/dashboard";
	    } else {
	        return "redirect:/2fa?error=true";
	    }
	}

	@GetMapping("/dashboard")
	public String showDashboard(HttpSession session, Model model) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {

			String theme = SysStatsWebApplication.theme;
			model.addAttribute("theme", theme);
	        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
	        model.addAttribute("activePage", "dashboard");
	        model.addAttribute("apiKey", SysStatsWebApplication.apiKey);

			return "dashboard";
		} else {
			return "redirect:/login?error=sessionExpired";
		}
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login?logout=true";
	}

	@GetMapping("/createadminuser")
	public String showCreateAdminPage(Model model) {
		model.addAttribute("theme", SysStatsWebApplication.theme);
		return "createadminuser";
	}

	@PostMapping("/createadminuser")
	public String createAdminUser(@RequestParam String username, @RequestParam String password, HttpSession session) {
	    User newUser = new User(username, password, true);
	    newUser.setTwoFactorEnabled(false);
	    newUser.setTwoFactorSecret(null);
	    UserStore.addUser(newUser);
	    session.setAttribute("loggedIn", true);
	    session.setAttribute("username", username);
	    session.setAttribute("isAdmin", true);
	    return "redirect:/dashboard";
	}


}
