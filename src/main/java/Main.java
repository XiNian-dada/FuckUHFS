import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            String loginUrl = "https://hfs-be.yunxiao.com/v2/users/sessions";
            String examListUrl = "https://hfs-be.yunxiao.com/v3/exam/list";
            String username = "15319770091";
            String password = "337289t";
            int roleType = 1; // 学生登录

            Login login = new Login();
            Map<String, String> cookies = login.loginAndGetCookies(loginUrl, username, password, roleType);

            String response = login.getExamList(examListUrl, cookies);
            parseAndPrintExamData(response, cookies);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseAndPrintExamData(String response, Map<String, String> cookies) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray exams = jsonResponse.getJSONObject("data").getJSONArray("list");

            Scanner scanner = new Scanner(System.in);
            Map<String, Integer> examMap = new HashMap<>();

            for (int i = 0; i < exams.length(); i++) {
                JSONObject exam = exams.getJSONObject(i);
                String name = exam.getString("name");
                int examId = exam.getInt("examId");
                int manfen = exam.getInt("manfen");
                double score = exam.getDouble("score");
                int classRank = exam.getInt("classRank");
                int gradeRank = exam.getInt("gradeRank");

                System.out.printf("考试名称: %s\n满分: %d\n得分: %.1f\n班级排名: %d\n年级排名: %d\n\n",
                        name, manfen, score, classRank, gradeRank);

                examMap.put(name, examId);
            }

            System.out.println("请输入要查看的考试名称：");
            String selectedExamName = scanner.nextLine();

            if (examMap.containsKey(selectedExamName)) {
                int selectedExamId = examMap.get(selectedExamName);
                String overviewUrl = "https://hfs-be.yunxiao.com/v3/exam/" + selectedExamId + "/overview";

                Login login = new Login();
                String overviewResponse = login.getExamOverview(overviewUrl, cookies);
                printExamOverview(overviewResponse);
            } else {
                System.out.println("未找到对应的考试，请检查输入的考试名称是否正确。");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void printExamOverview(String overviewResponse) {
        try {
            JSONObject jsonResponse = new JSONObject(overviewResponse);
            JSONObject data = jsonResponse.getJSONObject("data");

            int manfen = data.getInt("manfen");
            int score = data.getInt("score");
            int manfenBeforeGrading = data.getInt("manfenBeforeGrading");
            int scoreBeforeGrading = data.getInt("scoreBeforeGrading");
            int classRank = data.getInt("classRank");
            int gradeRank = data.getInt("gradeRank");
            int classStuNum = data.getInt("classStuNum");
            int gradeStuNum = data.getInt("gradeStuNum");

            System.out.printf("本科赋分满分: %d\n本科赋分得分: %d\n原始满分: %d\n原始得分: %d\n班级排名: %d\n年级排名: %d\n班级总人数: %d\n年级总人数: %d\n\n",
                    manfen, score, manfenBeforeGrading, scoreBeforeGrading, classRank, gradeRank, classStuNum, gradeStuNum);

            // 解析并输出各科目成绩
            JSONArray papers = data.getJSONArray("papers");
            for (int i = 0; i < papers.length(); i++) {
                JSONObject paper = papers.getJSONObject(i);
                String subject = paper.getString("subject");
                int subjectManfen = paper.getInt("manfen");
                int subjectScore = paper.getInt("score");
                int subjectManfenBeforeGrading = paper.getInt("manfenBeforeGrading");
                int subjectScoreBeforeGrading = paper.getInt("scoreBeforeGrading");

                System.out.printf("科目: %s\n本科赋分满分: %d\n本科赋分得分: %d\n赋分后满分: %d\n赋分后得分: %d\n\n",
                        subject, subjectManfen, subjectScore, subjectManfenBeforeGrading, subjectScoreBeforeGrading);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
