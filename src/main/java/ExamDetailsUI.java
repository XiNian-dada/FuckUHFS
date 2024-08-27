import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class ExamDetailsUI extends JFrame {
    private String examId; // Add examId field

    public ExamDetailsUI(String overviewResponse, Map<String, String> cookies, String examId) {
        this.examId = examId; // Initialize examId
        setTitle("Exam Details");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        try {
            JSONObject jsonResponse = new JSONObject(overviewResponse);
            JSONObject data = jsonResponse.getJSONObject("data");

            // Exam overview
            int manfen = data.getInt("manfen");
            int score = data.getInt("score");
            int manfenBeforeGrading = data.getInt("manfenBeforeGrading");
            int scoreBeforeGrading = data.getInt("scoreBeforeGrading");
            int classRank = data.getInt("classRank");
            int gradeRank = data.getInt("gradeRank");
            int classStuNum = data.getInt("classStuNum");
            int gradeStuNum = data.getInt("gradeStuNum");

            JPanel overviewPanel = new JPanel();
            overviewPanel.setLayout(new BoxLayout(overviewPanel, BoxLayout.Y_AXIS));
            JLabel overviewLabel = new JLabel(String.format(
                    "本科赋分满分: %d\n本科赋分得分: %d\n原始满分: %d\n原始得分: %d\n班级排名: %d\n年级排名: %d\n班级总人数: %d\n年级总人数: %d\n\n",
                    manfen, score, manfenBeforeGrading, scoreBeforeGrading, classRank, gradeRank, classStuNum, gradeStuNum
            ));
            overviewPanel.add(overviewLabel);
            panel.add(overviewPanel);

            // Papers
            JSONArray papers = data.getJSONArray("papers");
            for (int i = 0; i < papers.length(); i++) {
                JSONObject paper = papers.getJSONObject(i);
                String subject = paper.getString("subject");
                String paperId = paper.getString("paperId");
                String pid = paper.getString("pid");
                int subjectManfen = paper.getInt("manfen");
                int subjectScore = paper.getInt("score");
                int subjectManfenBeforeGrading = paper.getInt("manfenBeforeGrading");
                int subjectScoreBeforeGrading = paper.getInt("scoreBeforeGrading");

                JPanel paperPanel = new JPanel();
                paperPanel.setLayout(new BoxLayout(paperPanel, BoxLayout.Y_AXIS));
                JLabel paperLabel = new JLabel(String.format(
                        "科目: %s\n本科赋分满分: %d\n本科赋分得分: %d\n赋分后满分: %d\n赋分后得分: %d\n",
                        subject, subjectManfen, subjectScore, subjectManfenBeforeGrading, subjectScoreBeforeGrading
                ));
                JButton detailsButton = new JButton("查看详细得分");
                paperPanel.add(paperLabel);
                paperPanel.add(detailsButton);
                panel.add(paperPanel);

                // Handle details button click
                detailsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Call a method to show detailed scores for the subject
                        showSubjectDetails(subject, paperId, pid, cookies);
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }

    private void showSubjectDetails(String subject, String paperId, String pid, Map<String, String> cookies) {
        try {
            String detailsUrl = String.format("https://hfs-be.yunxiao.com/v3/exam/%s/papers/%s/answer-picture?pid=%s",
                    examId, paperId, pid);

            Login login = new Login();
            String detailsResponse = login.getExamDetails(detailsUrl, cookies);

            // 打印原始 JSON 响应以调试
            System.out.println("Raw JSON Response:\n" + detailsResponse);

            JFrame detailsFrame = new JFrame("Detailed Scores for " + subject);
            detailsFrame.setSize(600, 400);
            detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            detailsFrame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout());
            JPanel formattedPanel = formatDetailedScores(detailsResponse, cookies);
            JScrollPane formattedScrollPane = new JScrollPane(formattedPanel);

            JTextArea rawTextArea = new JTextArea();
            rawTextArea.setEditable(false);
            rawTextArea.setText("Raw JSON Response:\n" + detailsResponse);
            JScrollPane rawScrollPane = new JScrollPane(rawTextArea);

            panel.add(formattedScrollPane, BorderLayout.CENTER);
            panel.add(rawScrollPane, BorderLayout.SOUTH);
            detailsFrame.add(panel);

            detailsFrame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to retrieve detailed scores.");
        }
    }

    private JPanel formatDetailedScores(String detailsResponse, Map<String, String> cookies) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        try {
            JSONObject jsonResponse = new JSONObject(detailsResponse);
            JSONObject data = jsonResponse.optJSONObject("data");
            if (data == null) {
                throw new JSONException("No data field in response.");
            }

            JSONArray questions = data.optJSONArray("questions");
            if (questions == null) {
                throw new JSONException("No questions field in data.");
            }

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                String name = question.getString("name");
                int mafen = question.getInt("manfen");
                int score = question.getInt("score");
                int type = question.getInt("type");
                String myAnswer = question.optString("myAnswer", "无");
                String answer = question.optString("answer", "无");

                // Create a panel for each question
                JPanel questionPanel = new JPanel();
                questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));

                questionPanel.add(new JLabel(String.format("题号: %s\n满分: %d\n得分: %d\n", name, mafen, score)));

                if (type == 2) { // 选择题
                    questionPanel.add(new JLabel(String.format("我的答案: %s\n正确答案: %s\n\n", myAnswer, answer)));
                } else if (type == 1) { // 主观题
                    String url = question.optString("url", "");
                    if (!url.isEmpty()) {
                        ImageIcon subjectiveAnswer = downloadSubjectiveAnswer(url, cookies);
                        if (subjectiveAnswer != null) {
                            questionPanel.add(new JLabel("我的答题情况:"));
                            JLabel imageLabel = new JLabel(subjectiveAnswer);
                            questionPanel.add(imageLabel);
                        } else {
                            questionPanel.add(new JLabel("我的答题情况: 无\n"));
                        }
                    } else {
                        questionPanel.add(new JLabel("我的答题情况: 无\n"));
                    }
                }

                panel.add(questionPanel);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return panel;
    }



    private ImageIcon downloadSubjectiveAnswer(String urlString, Map<String, String> cookies) {
        try {
            String[] urls = urlString.split(",");

            for (String url : urls) {
                // 清理 URL 中的意外符号
                url = url.replace("[\"", "").replace("\"]", "").trim();
                url = url.replace('"', ' ').replace("”", "").trim(); // 去除意外的“符号
                System.out.println("Processed URL: " + url);

                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("GET");

                // 设置 Cookie 头信息
                StringBuilder cookieString = new StringBuilder();
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    cookieString.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
                }
                conn.setRequestProperty("Cookie", cookieString.toString());

                // 获取输入流
                InputStream in = conn.getInputStream();

                // 读取流中的数据到 ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                in.close();

                // 将 ByteArrayOutputStream 转换为 ByteArrayInputStream
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(baos.toByteArray());
                Image image = ImageIO.read(byteArrayInputStream);

                return new ImageIcon(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
