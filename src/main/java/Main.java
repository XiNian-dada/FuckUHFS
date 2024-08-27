import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            String loginUrl = "https://hfs-be.yunxiao.com/v2/users/sessions";
            String examListUrl = "https://hfs-be.yunxiao.com/v3/exam/list";
            String username = "15309260876";
            String password = "8iggg8y8";
            int roleType = 1; // 学生登录

            Login login = new Login();
            Map<String, String> cookies = login.loginAndGetCookies(loginUrl, username, password, roleType);

            String response = login.getExamList(examListUrl, cookies);
            parseAndPrintExamData(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseAndPrintExamData(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray exams = jsonResponse.getJSONObject("data").getJSONArray("list");

            for (int i = 0; i < exams.length(); i++) {
                JSONObject exam = exams.getJSONObject(i);
                String name = exam.getString("name");
                int manfen = exam.getInt("manfen");
                double score = exam.getDouble("score");
                int classRank = exam.getInt("classRank");
                int gradeRank = exam.getInt("gradeRank");

                System.out.printf("考试名称: %s\n满分: %d\n得分: %.1f\n班级排名: %d\n年级排名: %d\n\n",
                        name, manfen, score, classRank, gradeRank);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}