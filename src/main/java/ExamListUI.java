import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ExamListUI extends JFrame {
    public ExamListUI(String response, Map<String, String> cookies) {
        setTitle("考试列表");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane);

        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray exams = jsonResponse.getJSONObject("data").getJSONArray("list");

            Map<String, Integer> examMap = new HashMap<>();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = GridBagConstraints.REMAINDER;

            for (int i = 0; i < exams.length(); i++) {
                JSONObject exam = exams.getJSONObject(i);
                String name = exam.getString("name");
                int examId = exam.getInt("examId");
                int manfen = exam.getInt("manfen");
                double score = exam.getDouble("score");
                int classRank = exam.getInt("classRank");
                int gradeRank = exam.getInt("gradeRank");

                JPanel examPanel = new JPanel();
                examPanel.setLayout(new GridBagLayout());
                GridBagConstraints panelGbc = new GridBagConstraints();
                panelGbc.insets = new Insets(5, 5, 5, 5);
                panelGbc.fill = GridBagConstraints.HORIZONTAL;

                JLabel examLabel = new JLabel(String.format("考试名称: %s", name));
                panelGbc.gridx = 0;
                panelGbc.gridy = 0;
                examPanel.add(examLabel, panelGbc);

                JLabel manfenLabel = new JLabel(String.format("满分: %d", manfen));
                panelGbc.gridy = 1;
                examPanel.add(manfenLabel, panelGbc);

                JLabel scoreLabel = new JLabel(String.format("得分: %.1f", score));
                panelGbc.gridy = 2;
                examPanel.add(scoreLabel, panelGbc);

                JLabel classRankLabel = new JLabel(String.format("班级排名: %d", classRank));
                panelGbc.gridy = 3;
                examPanel.add(classRankLabel, panelGbc);

                JLabel gradeRankLabel = new JLabel(String.format("年级排名: %d", gradeRank));
                panelGbc.gridy = 4;
                examPanel.add(gradeRankLabel, panelGbc);

                JButton detailsButton = new JButton("查看详情");
                panelGbc.gridy = 5;
                panelGbc.gridwidth = GridBagConstraints.REMAINDER;
                examPanel.add(detailsButton, panelGbc);

                gbc.gridy++;
                panel.add(examPanel, gbc);

                examMap.put(name, examId);

                // Add a separator
                if (i < exams.length() - 1) {
                    JSeparator separator = new JSeparator();
                    separator.setPreferredSize(new Dimension(550, 10));
                    gbc.gridy++;
                    panel.add(separator, gbc);
                }

                detailsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            int selectedExamId = examMap.get(name);
                            String overviewUrl = "https://hfs-be.yunxiao.com/v3/exam/" + selectedExamId + "/overview";
                            Login login = new Login();
                            String overviewResponse = login.getExamOverview(overviewUrl, cookies);

                            // Create and show ExamDetailsUI
                            new ExamDetailsUI(overviewResponse, cookies, String.valueOf(selectedExamId));

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "无法获取考试详情。");
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
