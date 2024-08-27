import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ExamListUI extends JFrame {
    public ExamListUI(String response, Map<String, String> cookies) {
        setTitle("Exam List");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray exams = jsonResponse.getJSONObject("data").getJSONArray("list");

            Map<String, Integer> examMap = new HashMap<>();

            for (int i = 0; i < exams.length(); i++) {
                JSONObject exam = exams.getJSONObject(i);
                String name = exam.getString("name");
                int examId = exam.getInt("examId");
                int manfen = exam.getInt("manfen");
                double score = exam.getDouble("score");
                int classRank = exam.getInt("classRank");
                int gradeRank = exam.getInt("gradeRank");

                JPanel examPanel = new JPanel();
                examPanel.setLayout(new BoxLayout(examPanel, BoxLayout.Y_AXIS));
                JLabel examLabel = new JLabel(String.format("考试名称: %s\n满分: %d\n得分: %.1f\n班级排名: %d\n年级排名: %d\n",
                        name, manfen, score, classRank, gradeRank));
                JButton detailsButton = new JButton("Details");
                examPanel.add(examLabel);
                examPanel.add(detailsButton);
                panel.add(examPanel);

                examMap.put(name, examId);

                detailsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            int selectedExamId = examMap.get(name);
                            String overviewUrl = "https://hfs-be.yunxiao.com/v3/exam/" + selectedExamId + "/overview";
                            Login login = new Login();
                            String overviewResponse = login.getExamOverview(overviewUrl, cookies);

                            // Create and show ExamDetailsUI
                            new ExamDetailsUI(overviewResponse, cookies,String.valueOf(selectedExamId));

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Failed to retrieve exam details.");
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }
}
