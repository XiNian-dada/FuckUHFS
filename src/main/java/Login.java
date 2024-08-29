import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

class Login {

    public Map<String, String> loginAndGetCookies(String loginUrl, String username, String password, int roleType) throws Exception {
        URL url = new URL(loginUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        // 将密码进行Base64编码
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes("UTF-8"));
        System.out.println(roleType);
        System.out.println(password);
        System.out.println(encodedPassword);
        System.out.println(username);
        String jsonInputString = String.format(
                "{\"loginName\":\"%s\",\"password\":\"%s\",\"roleType\":%d,\"loginType\":1,\"rememberMe\":2}",
                username, encodedPassword, roleType
        );

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        Map<String, String> cookies = new HashMap<>();
        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase("Set-Cookie")) {
                String cookie = conn.getHeaderField(i);
                String cookieName = cookie.substring(0, cookie.indexOf("="));
                String cookieValue = cookie.substring(cookie.indexOf("=") + 1, cookie.indexOf(";"));
                cookies.put(cookieName, cookieValue);
            }
        }

        conn.disconnect();
        return cookies;
    }
    public String getExamDetails(String urlStr, Map<String, String> cookies) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Set cookies in the request header
        if (cookies != null && !cookies.isEmpty()) {
            StringBuilder cookieHeader = new StringBuilder();
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                cookieHeader.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
            }
            connection.setRequestProperty("Cookie", cookieHeader.toString());
        }

        // Read the response
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
    public String getExamList(String examListUrl, Map<String, String> cookies) throws Exception {
        URL url = new URL(examListUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            conn.addRequestProperty("Cookie", cookie.getKey() + "=" + cookie.getValue());
        }

        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }

        conn.disconnect();
        return content.toString();
    }

    public String getExamOverview(String overviewUrl, Map<String, String> cookies) throws Exception {
        URL url = new URL(overviewUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            conn.addRequestProperty("Cookie", cookie.getKey() + "=" + cookie.getValue());
        }

        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }

        conn.disconnect();
        return content.toString();
    }
}
