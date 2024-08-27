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

        // 打印获取到的响应内容
        System.out.println("Response from server: " + content.toString());

        return content.toString();
    }

}