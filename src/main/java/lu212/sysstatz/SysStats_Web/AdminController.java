package lu212.sysstatz.SysStats_Web;

import jakarta.servlet.http.HttpSession;
import lu212.sysstatz.General.User;
import lu212.sysstatz.General.UserStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/manageUsers")
    public String manageUsers(Model model, HttpSession session) {
		Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
		if (loggedIn != null && loggedIn) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        model.addAttribute("activePage", "manageUsers");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
   	 	String theme = SysStatsWebApplication.theme;
   	 	model.addAttribute("theme", theme);
        
        model.addAttribute("users", UserStore.getAll());
        return "manageUsers";  // Name der Thymeleaf Vorlage (manageUsers.html)
		} else {
			return "redirect:/login?error=sessionExpired";
		}
    }


    // POST: Neuen Benutzer anlegen
    @PostMapping("/createUser")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false) boolean isAdmin,
                             HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/dashboard";
        }

        if (UserStore.getUserByName(username) == null) {
            UserStore.addUser(new User(username, password, isAdmin));
        }
        return "redirect:/admin/manageUsers";
    }

    // POST: Benutzer löschen
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam String username, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/dashboard";
        }

        // Admin darf sich selbst nicht löschen
        String currentUsername = (String) session.getAttribute("username");
        if (!username.equals(currentUsername)) {
            UserStore.deleteUser(username);
        }
        return "redirect:/admin/manageUsers";
    }

    // POST: Rolle (Admin/Normal) umschalten
    @PostMapping("/toggleRole")
    public String toggleUserRole(@RequestParam String username, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/dashboard";
        }

        User user = UserStore.getUserByName(username);
        if (user != null) {
            user.setAdmin(!user.isAdmin());
            UserStore.saveUsers();  // Speichern nicht vergessen!
        }
        return "redirect:/admin/manageUsers";
    }
    
    @PostMapping("/changePass")
    public String changeUserPass(@RequestParam String username,
                                 @RequestParam String password,
                                 HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/dashboard";
        }

        User user = UserStore.getUserByName(username);
        if (user != null) {
            user.setPassword(password);
            UserStore.saveUsers();
        }
        return "redirect:/admin/manageUsers";
    }
    
    @PostMapping("/reset2fa")
    public String reset2FA(@RequestParam String username, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/dashboard";
        }

        User user = UserStore.getUserByName(username);
        if (user != null) {
            user.setTwoFactorSecret(null);
            user.setTwoFactorEnabled(false);
            UserStore.saveUsers();
        }
        return "redirect:/admin/manageUsers";
    }
}
