/*
<applet code='ParamApplet' width='200' height='200'>
<param name='param' value='foo'>
</applet>
*/


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.Security;
import java.util.*;
import java.util.List;

import com.hp.ilo2.remcons.remcons;

/*


Set-Cookie: hp-iLO-Login=; Expires=Sun, 01 Jan 1990 12:00:00 GMT
Set-Cookie: hp-iLO-Session=00000005:::LMQJVGLGKQGMIAAEGQHZJUORCOBVQOUZIEXNVTUO; Path=/; Secure



var sessionkey="LMQJVGLGKQGMIAAEGQHZJUORCOBVQOUZIEXNVTUO";
var sessionindex="00000005";


*/

public class Main {
    private static final String USAGE_TEXT = "Usage: \n" +
            "- ILO2RemCon.jar <Hostname or IP> <Username> <Password>\n" +
            "- ILO2RemCon.jar -c <Path to config.properties>";

    private static final String DEFAULT_CONFIG_PATH = "config.properties";
    private static final String COOKIE_FILE = "data.cook";

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko";


    private static String username = "";
    private static String password = "";
    private static String hostname = "";

    public static void setHostname(String hostname) {
        Main.hostname = hostname;
        Main.loginURL = "https://" + hostname + "/login.htm";
    }

    private static String loginURL = "";

    private static String sessionKey = "";
    private static String sessionIndex = "";
    private static String supercookie = "";

    private final static CookieManager cookieManager = new CookieManager();


    private static void Stage1() throws Exception {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        System.setProperty("https.protocols", "TLSv1");
        System.setProperty("javax.net.debug", "all");
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        URL obj = new URL(loginURL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        con.setRequestProperty("Cookie", "hp-iLO-Login=");


        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();
        sessionKey = res.split("var sessionkey=\"")[1].split("\";")[0];
        sessionIndex = res.split("var sessionindex=\"")[1].split("\";")[0];
        System.out.println("Session key: " + sessionKey);
        System.out.println("Session  ID: " + sessionIndex);
    }


    private static void Stage2() throws Exception {
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();

        URL obj = new URL("https://" + hostname + "/index.htm");

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");

        Base64.Encoder enc2 = Base64.getMimeEncoder();
        String cookieVal = String.format("hp-iLO-Login=%s:%s:%s:%s",
                sessionIndex,
                enc2.encodeToString(username.getBytes()),
                enc2.encodeToString(password.getBytes()),
                sessionKey
        );
        con.setRequestProperty("Cookie", cookieVal);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));

        //noinspection StatementWithEmptyBody
        while (in.readLine() != null) {
        } // discard
        in.close();

        // Extract Set-Cookie headers directly from the response
        PrintWriter writer = new PrintWriter(COOKIE_FILE, "UTF-8");
        int i = 1;
        String headerKey;
        while ((headerKey = con.getHeaderFieldKey(i)) != null) {
            if (headerKey.equalsIgnoreCase("Set-Cookie")) {
                String raw = con.getHeaderField(i);
                System.out.println("Set-Cookie: " + raw);
                // raw is like "hp-iLO-Session=VALUE; Path=/; Secure" — take just name=value
                String nameValue = raw.split(";")[0].trim().replace("\"", "");
                writer.println(nameValue);
                if (nameValue.startsWith("hp-iLO-Session=")) {
                    supercookie = nameValue;
                    System.out.println("Using session cookie: " + supercookie);
                }
            }
            i++;
        }

        // Fall back to cookieManager if headers gave us nothing
        if (supercookie.isEmpty()) {
            for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
                System.out.format("CookieManager cookie: %s: %s\n", cookie.getDomain(), cookie);
                String cookieStr = cookie.toString().replace("\"", "");
                writer.println(cookieStr);
                if (cookie.getName().equals("hp-iLO-Session")) {
                    supercookie = cookieStr;
                }
            }
        }
        writer.close();

    }


    private final static HashMap<String, String> hmap = new HashMap<>();

    private static String parseQuoted(String src, String key) {
        return src.split(key + "=\"")[1].split("\"")[0];
    }

    private static String parseUnquoted(String src, String key) {
        return src.split(key + "=")[1].split("[;,\\s]")[0];
    }

    private static void Stage3() throws Exception {
        // https://" + hostname + "/drc2fram.htm?restart=1
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        String url = "https://" + hostname + "/drc2fram.htm?restart=1";
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        if (!supercookie.equals("")) {
            con.setRequestProperty("Cookie", supercookie);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();

        hmap.put("INFO0", parseQuoted(res, "info0"));
        hmap.put("INFO1", parseQuoted(res, "info1"));
        hmap.put("INFO3", parseQuoted(res, "info3"));
        hmap.put("INFO6", parseQuoted(res, "info6"));
        hmap.put("INFO7", parseUnquoted(res, "info7"));
        hmap.put("INFO8", parseQuoted(res, "info8"));

        hmap.put("INFOA", parseQuoted(res, "infoa"));
        hmap.put("INFOB", parseQuoted(res, "infob"));
        hmap.put("INFOC", parseQuoted(res, "infoc"));
        hmap.put("INFOD", parseQuoted(res, "infod"));

        hmap.put("INFOM", parseUnquoted(res, "infom"));
        hmap.put("INFOMM", parseUnquoted(res, "infomm"));

        hmap.put("INFON", parseUnquoted(res, "infon"));
        hmap.put("INFOO", parseQuoted(res, "infoo"));

        // JAR name comes from ARCHIVE= in the document.writeln applet tag
        hmap.put("CABBASE", res.split("ARCHIVE=")[1].split(" ")[0]);

        System.out.println("CABBASE = " + hmap.get("CABBASE"));
    }


    public static boolean isValid(String cookie) throws Exception {
        CookieHandler.setDefault(cookieManager);
        String url = "https://" + hostname + "/ie_index.htm";
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        //con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Referer", loginURL);
        con.setRequestProperty("Host", hostname);
        con.setRequestProperty("Accept-Language", "de-DE");
        con.setRequestProperty("Cookie", cookie);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        String res = response.toString();

        return !(res.contains("Login Delay") || res.contains("Integrated Lights-Out 2 Login"));
    }


    public static void main(String[] args) {
        Optional<String> configPath = Optional.empty();

        switch (args.length) {
            case 0:
                // <no args>
                // try the default config location
                configPath = Optional.of(DEFAULT_CONFIG_PATH);
                break;
            case 2:
                // -c <path>
                if (args[0].equals("-c")) {
                    configPath = Optional.of(args[1]);
                } else {
                    System.out.println(USAGE_TEXT);
                }
                break;
            case 3:
                // <Hostname or IP> <Username> <Password>
                setHostname(args[0]);
                username = args[1];
                password = args[2];
                break;
            default:
                System.out.println(USAGE_TEXT);
                return;
        }

        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();
        CookieHandler.setDefault(cookieManager);

        if (configPath.isPresent()) {
            try (FileInputStream fis = new FileInputStream(configPath.get())) {
                Properties p = new Properties();
                p.load(fis);

                setHostname(p.getProperty("hostname"));
                username = p.getProperty("username");
                password = p.getProperty("password");
            } catch (Exception e) {
                System.err.println("Error in reading/parsing config file!");
                e.printStackTrace();
                return;
            }
        }

        try {
            try (BufferedReader br = new BufferedReader(new FileReader("data.cook"))) {
                System.out.println("Found datastore");
                String line;
                String lastline = "";
                while ((line = br.readLine()) != null) {
                    cookieManager.getCookieStore().add(new URI("https://" + hostname), new HttpCookie(line.split("=")[0], line.split("=")[1]));
                    lastline = line;
                }

                if (!isValid(lastline)) {
                    System.out.println("Datastore not valid, requesting Cookie");
                    Stage1();
                    Stage2();
                } else {
                    supercookie = lastline;
                }
            } catch (FileNotFoundException e) {
                System.out.println("Couldn't find datastore, requesting Cookie");
                Stage1();
                Stage2();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Stage3();
            //hmap.put("IPADDR", hostname);
            //hmap.put("DEBUG", "suckAdIck");

            remcons rmc = new remcons(hmap);
            rmc.SetHost(hostname);

            JFrame jf = new JFrame();
            Container c = jf.getContentPane();
            jf.setBounds(0, 0, 1070, 880);
            jf.setTitle(hostname + " - iLO2");
            jf.setVisible(true);
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            c.add(rmc);
            rmc.init();
            rmc.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
