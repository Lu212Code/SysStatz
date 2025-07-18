package lu212.sysStats.SysStats_Web;

public class AuthService {

 private static long CONNECTIONS = 0;
 private static String PASSWORD = "geheim123";

 public static boolean checkPassword(String input) {
	 System.out.println("User Login wird ausgeführt...");
	 PASSWORD = SysStatsWebApplication.webpasswort;
	 CONNECTIONS++;
     return PASSWORD.equals(input);
 }
}
