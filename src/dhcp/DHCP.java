package dhcp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author Fabian Tamas
 */
public class DHCP {
    
    static final int MERET = 100;
    static final int KEZDO = 100;
    static String[] iptabla = new String[MERET];
    static HashMap<String, String> reserved = new HashMap<>();
    static HashMap<String, String> dhcp = new HashMap<>();
    
    /**
     * index meghatározása - az IP cím pontok mentén való eldarabolásával
     * a hármas indexű elemet (0.1.2.3) egész számmá alakítjuk és kivonjuk 
     * belőle a 100-at (KEZDŐ érték)
     **/
    static int index (String cim) {
        return Integer.parseInt(cim.split("\\.")[3]) - KEZDO;
    }
    
    /**
     * Az IP cím "összerakása"
     * @param index
     * @return 
     */
    static String cim(int index) {
        return "192.168.10." + (index+KEZDO);
    }

    public static void main(String[] args) {
        // iptábla inicializálása
        for (int i = 0; i < MERET; i++)
            iptabla[i] = "";
        // excluded beolvasása (kizárt címek)
        try (Scanner be = new Scanner(new File("excluded.csv"))) {
            while (be.hasNextLine()) {
                iptabla[index(be.nextLine())] = "excluded";
            }
        } catch (IOException e) {
            System.out.println("Hiba az excluded.csv olvasásakor!");
        }
        // reserved beolvasása (foglalt címek)
        try (Scanner be = new Scanner(new File("reserved.csv"))) {
            while (be.hasNextLine()) {
                String sor[] = be.nextLine().split(";");
                iptabla[index(sor[1])] = "reserved";
                reserved.put (sor[0], sor[1]);
            }
        } catch (IOException e) {
            System.out.println("Hiba az reserved.csv olvasásakor!");
        }
        // dhcp beolvasása
        try (Scanner be = new Scanner(new File("dhcp.csv"))) {
            while (be.hasNextLine()) {
                String sor[] = be.nextLine().split(";");
                iptabla[index(sor[1])] = sor [0];
                dhcp.put (sor[0], sor[1]);
            }
        } catch (IOException e) {
            System.out.println("Hiba az dhcp.csv olvasásakor!");
        }
        
        // teszt
        try (Scanner be = new Scanner(new File("test.csv"))) {
            while (be.hasNextLine()) {
                String sor[] = be.nextLine().split(";");
                if (sor[0].equals("request"))
                    try {
                    request(sor[1]);
                    } catch (Kivetel ex) {
                        System.out.println(ex.getMessage());
                    }
                else if (sor[0].equals("release"))
                    release(sor[1]);
            }
        } catch (IOException e) {
            System.out.println("Hiba az test.csv olvasásakor!");
        }
        
        // kiírás fájlba
        try (PrintWriter ki = new PrintWriter("dhcp_kesz.csv")) {
            for (int i=0; i<MERET; i++) {
                String s = iptabla[i];
                if (!s.equals("")
                        && !s.equals("reserved")
                        && !s.equals("excluded"))
                    ki.println(iptabla[i] + ";" + cim(i));
            }
        } catch (IOException e) {
            System.out.println("Hiba a dhcp_kesz.csv írásakor!");
        }
    }    
    
    // az iptabla-ban üresre kell állítani az IP címhez tartozó elemet
    // a foglalt IP címeket foglaltra visszaállítani
    static private void release(String ip) {
        String mac = iptabla[index(ip)];
        dhcp.remove(mac);
        if (reserved.get(mac) != null)
            iptabla[index(ip)] = "reserved";
        else
            iptabla[index(ip)] = "";
    }
    
    static private void kioszt(String mac, String ip) {
        iptabla[index(ip)] = mac;
        dhcp.put(mac, ip);
    }
    
    static private void request (String mac) throws Kivetel {
        if (dhcp.get(mac) != null)
            return;
        String ip = reserved.get(mac);
        if (ip != null) {
            kioszt(mac, ip);
            return;
        }
        for (int i = 0; i < MERET; i++) {
            if (iptabla[i].equals("")) {
                kioszt(mac, cim(i));
                return;
            }
        }
        throw new Kivetel ("Nem sikerült az IP cím kiosztása!");
    }
}

// kivételkezelés saját osztályból
class Kivetel extends Exception {
    public Kivetel (String uzenet) {
        super (uzenet);
    }
}

