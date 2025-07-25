package lu212.sysStats.SysStats_Web;

import jakarta.servlet.http.HttpSession;
import lu212.sysStats.General.Logger;
import lu212.sysStats.General.User;
import lu212.sysStats.General.UserStore;

public class AuthService {
    public static boolean authenticate(String username, String password, HttpSession session) {
        User user = UserStore.getUserByName(username);
        if (user != null && user.getPassword().equals(password)) {
        	Logger.info("Benutzer " + username + "hat sich angemeldet. ");
            session.setAttribute("loggedIn", true);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("isAdmin", user.isAdmin());
            return true;
        }
        return false;
    }
}
