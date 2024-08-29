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
    private JPanel examDetailsPanel; // 用于显示考试详细信息的面板
    private Map<String, Integer> examMap;
    private Map<String, String> cookies;

    public ExamListUI(String response, Map<String, String> cookies) {
        this.cookies = cookies; // 存储 cookies 以便后续使用
        setTitle("考试列表");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 创建“退出账号”按钮
        JButton logoutButton = new JButton("退出账号");
        logoutButton.setFont(new Font("SansSerif", Font.BOLD, 14)); // 增加字体加粗，稍微调小字号
        logoutButton.setPreferredSize(new Dimension(120, 40)); // 统一按钮大小
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 退出账号，关闭当前窗口并重新打开登录界面
                dispose(); // 关闭当前窗口
                new LoginUI(); // 重新打开登录界面，LoginUI是登录界面的类名
            }
        });

        // 创建顶部面板用于放置按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(logoutButton, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // 左侧考试列表面板
        JPanel examListPanel = new JPanel();
        examListPanel.setLayout(new GridBagLayout());
        JScrollPane examListScrollPane = new JScrollPane(examListPanel);
        examListScrollPane.setPreferredSize(new Dimension(300, 0));
        add(examListScrollPane, BorderLayout.WEST);

        // 右侧考试详情面板
        examDetailsPanel = new JPanel(new BorderLayout());
        JScrollPane examDetailsScrollPane = new JScrollPane(examDetailsPanel);
        add(examDetailsScrollPane, BorderLayout.CENTER);

        try {
            JSONObject jsonResponse = new JSONObject(response);
            System.out.println("Exam List JSON Response:\n" + jsonResponse.toString(2)); // 打印整个 JSON 响应
            JSONArray exams = jsonResponse.getJSONObject("data").getJSONArray("list");

            examMap = new HashMap<>();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0; // 确保按钮宽度一致

            for (int i = 0; i < exams.length(); i++) {
                JSONObject exam = exams.getJSONObject(i);
                String name = exam.getString("name");
                int examId = exam.getInt("examId");

                JPanel examPanel = new JPanel(new GridBagLayout());
                GridBagConstraints panelGbc = new GridBagConstraints();
                panelGbc.insets = new Insets(10, 10, 10, 10); // 增加间距
                panelGbc.fill = GridBagConstraints.HORIZONTAL;
                panelGbc.weightx = 1.0; // 确保按钮宽度一致

                JLabel examLabel = new JLabel(name);
                examLabel.setFont(new Font("SansSerif", Font.BOLD, 16)); // 字体加粗，调整字号
                panelGbc.gridx = 0;
                panelGbc.gridy = 0;
                examPanel.add(examLabel, panelGbc);

                JButton detailsButton = new JButton("查看详情");
                detailsButton.setFont(new Font("SansSerif", Font.BOLD, 14)); // 字体加粗，稍微调小字号
                detailsButton.setPreferredSize(new Dimension(120, 40)); // 统一按钮大小
                panelGbc.gridy = 1;
                panelGbc.fill = GridBagConstraints.NONE; // 防止按钮扩展填满
                panelGbc.anchor = GridBagConstraints.CENTER; // 按钮居中对齐
                examPanel.add(detailsButton, panelGbc);

                gbc.gridy++;
                examListPanel.add(examPanel, gbc);

                examMap.put(name, examId);

                // 详情按钮事件监听
                detailsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            int selectedExamId = examMap.get(name);
                            String overviewUrl = "https://hfs-be.yunxiao.com/v3/exam/" + selectedExamId + "/overview";
                            Login login = new Login();
                            String overviewResponse = login.getExamOverview(overviewUrl, cookies);
                            displayExamDetails(overviewResponse); // 显示考试详情

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "无法获取考试详情。");
                        }
                    }
                });
            }

            // 添加提示标签到考试列表面板底部
            JLabel infoLabel = new JLabel("120天前的考试无法查看");
            infoLabel.setFont(new Font("SansSerif", Font.ITALIC, 12)); // 斜体字，稍微调小字号
            GridBagConstraints infoGbc = new GridBagConstraints();
            infoGbc.gridx = 0;
            infoGbc.gridy = GridBagConstraints.RELATIVE;
            infoGbc.insets = new Insets(20, 10, 10, 10); // 设置底部间距
            infoGbc.anchor = GridBagConstraints.CENTER;
            examListPanel.add(infoLabel, infoGbc);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }





    // 显示考试详细信息
    private void displayExamDetails(String overviewResponse) {
        examDetailsPanel.removeAll(); // 清空面板上的旧内容

        try {
            JSONObject overviewJson = new JSONObject(overviewResponse);
            System.out.println("Exam Details JSON Response:\n" + overviewJson.toString(2)); // 打印整个 JSON 响应
            JSONObject data = overviewJson.getJSONObject("data");

            double manfen = data.getDouble("manfen");
            double score = data.getDouble("score");
            double manfenBeforeGrading = data.getDouble("manfenBeforeGrading");
            double scoreBeforeGrading = data.getDouble("scoreBeforeGrading");
            int classRank = data.getInt("classRank");
            int gradeRank = data.getInt("gradeRank");
            int classStuNum = data.getInt("classStuNum");
            int gradeStuNum = data.getInt("gradeStuNum");

            // 获取 examId
            String examId = String.valueOf(data.getInt("examId")); // 假设 examId 在 data 对象中

            // 创建考试概况面板
            JPanel overviewPanel = new JPanel();
            overviewPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0;
            gbc.gridy = 0;
            JLabel overviewLabel = new JLabel("本次考试概况");
            overviewLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
            overviewPanel.add(overviewLabel, gbc);

            gbc.gridy++;
            String totalScoreHtml = String.format(
                    "<html><body>" +
                            "<span style='font-size: 20px; font-family: SansSerif; font-weight: bold; color: #000000;'>总得分: %s</span> " +
                            "<span style='font-size: 18px; font-family: SansSerif; color: #666666;'>/ %s</span>" +
                            "<br/>" +
                            "<span style='font-size: 16px; font-family: SansSerif; color: #999999;'>原始得分: %s / %s</span>" +
                            "</body></html>",
                    formatNumber(score), formatNumber(manfen),
                    formatNumber(scoreBeforeGrading), formatNumber(manfenBeforeGrading)
            );
            JLabel totalScoreLabel = new JLabel(totalScoreHtml);
            overviewPanel.add(totalScoreLabel, gbc);

            gbc.gridy++;
            String rankHtml = String.format(
                    "<html><body>" +
                            "<span style='font-size: 22px; font-family: SansSerif; font-weight: bold;'>班级排名: %d</span> / <span style='font-size: 18px; font-family: SansSerif; color: #666666;'>%d</span><br/>" +
                            "<span style='font-size: 22px; font-family: SansSerif; font-weight: bold;'>年级排名: %d</span> / <span style='font-size: 18px; font-family: SansSerif; color: #666666;'>%d</span>" +
                            "</body></html>",
                    classRank, classStuNum, gradeRank, gradeStuNum
            );
            JLabel rankLabel = new JLabel(rankHtml);
            overviewPanel.add(rankLabel, gbc);

            examDetailsPanel.add(overviewPanel, BorderLayout.NORTH);

            // 处理科目信息
            JSONArray subjects = data.getJSONArray("papers"); // 这里应该是 "papers"
            JPanel detailsPanel = new JPanel(new GridBagLayout());
            GridBagConstraints detailsGbc = new GridBagConstraints();
            detailsGbc.insets = new Insets(10, 10, 10, 10);
            detailsGbc.fill = GridBagConstraints.HORIZONTAL;
            detailsGbc.gridx = 0;
            detailsGbc.gridy = 0;

            for (int i = 0; i < subjects.length(); i++) {
                JSONObject subject = subjects.getJSONObject(i);
                String subjectName = subject.getString("subject");
                double subjectScore = subject.getDouble("score");
                double subjectManfen = subject.getDouble("manfen");
                String paperId = subject.getString("paperId"); // 获取 paperId
                String pid = subject.getString("pid"); // 获取 pid

                JPanel subjectPanel = new JPanel(new GridBagLayout());
                GridBagConstraints subjectGbc = new GridBagConstraints();
                subjectGbc.insets = new Insets(5, 5, 5, 5);
                subjectGbc.fill = GridBagConstraints.HORIZONTAL;

                // 使用 HTML 格式化科目信息
                JLabel subjectLabel = new JLabel(String.format(
                        "<html><body>" +
                                "<span style='font-size: 18px; font-family: SansSerif; font-weight: bold;'>科目: %s</span><br/>" +
                                "<span style='font-size: 20px; font-family: SansSerif; font-weight: bold; color: #000000;'>得分: %s</span> " +
                                "<span style='font-size: 18px; font-family: SansSerif; color: #666666;'>/ %s</span>" +
                                "</body></html>",
                        subjectName, formatNumber(subjectScore), formatNumber(subjectManfen)
                ));
                subjectGbc.gridx = 0;
                subjectGbc.gridy = 0;
                subjectPanel.add(subjectLabel, subjectGbc);

                JButton viewDetailsButton = new JButton("查看详细得分");
                // 设置按钮的字体大小
                viewDetailsButton.setFont(new Font("SansSerif", Font.BOLD, 16));
                // 设置按钮的大小
                viewDetailsButton.setPreferredSize(new Dimension(160, 40));

                subjectGbc.gridx = 1;
                subjectGbc.gridy = 0;
                subjectPanel.add(viewDetailsButton, subjectGbc);

                detailsGbc.gridy++;
                detailsPanel.add(subjectPanel, detailsGbc);

                // 查看详细得分按钮事件监听
                viewDetailsButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ExamDetailsUI examDetailsUI = new ExamDetailsUI(examId); // 传递 examId
                            examDetailsUI.showSubjectDetails(subjectName, paperId, pid, cookies);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "无法获取科目详细得分。");
                        }
                    }
                });
            }

            examDetailsPanel.add(detailsPanel, BorderLayout.CENTER);
            examDetailsPanel.revalidate(); // 刷新面板显示
            examDetailsPanel.repaint();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    // 格式化分数的方法
    private String formatNumber(double value) {
        if (value == (int) value) {
            // 如果是整数，则返回整数部分
            return String.format("%d", (int) value);
        } else {
            // 否则，返回一位小数部分
            return String.format("%.1f", value);
        }
    }

    public static void main(String[] args) {
        new ExamListUI("{\"data\": {\"list\": []}}", new HashMap<>()); // 需要传入实际的 response 和 cookies
    }
}
