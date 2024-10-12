import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExamDetailsUI extends JFrame {
    private String examId;
    public ExamDetailsUI(String examId) {
        this.examId = examId;
    }
    private Double totalScore = 0.0;
    public void showSubjectDetails(String subject, String paperId, String pid, Map<String, String> cookies, double score) {
        try {
            // 获取详细得分
            String detailsUrl = String.format("https://hfs-be.yunxiao.com/v3/exam/%s/papers/%s/answer-picture?pid=%s",
                    examId, paperId, pid);

            Login login = new Login();
            String detailsResponse = login.getExamDetails(detailsUrl, cookies);
            System.out.println("原始 JSON 响应:\n" + detailsResponse);

            // 获取排名信息
            String rankInfoResponse = fetchRankInfo(examId, paperId, cookies);
            System.out.println("排名信息响应:\n" + rankInfoResponse);

            JFrame detailsFrame = new JFrame("详细得分 - " + subject);
            detailsFrame.setSize(840, 600);
            detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            detailsFrame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout());
            JPanel formattedPanel = formatDetailedScores(detailsResponse, cookies, false);
            JScrollPane formattedScrollPane = new JScrollPane(formattedPanel);

            // 创建排名信息面板
            JPanel rankInfoPanel = formatRankInfo(rankInfoResponse, detailsResponse, score,totalScore);
            rankInfoPanel.setVisible(true); // 初始状态设置为可见

            // 创建切换按钮
            JButton toggleButton = new JButton("隐藏排名信息");
            toggleButton.addActionListener(e -> {
                boolean isVisible = rankInfoPanel.isVisible();
                rankInfoPanel.setVisible(!isVisible);
                toggleButton.setText(isVisible ? "显示排名信息" : "隐藏排名信息");
                panel.revalidate(); // 刷新面板
                panel.repaint();    // 重绘面板
            });

            // 创建一个面板用于显示排名信息和切换按钮
            JPanel rankPanel = new JPanel();
            rankPanel.setLayout(new BorderLayout());
            rankPanel.add(toggleButton, BorderLayout.NORTH);
            rankPanel.add(rankInfoPanel, BorderLayout.CENTER);

            // 创建原始 JSON 响应文本区域
            JTextArea rawTextArea = new JTextArea();
            rawTextArea.setEditable(false);
            rawTextArea.setText("原始 JSON 响应:\n" + detailsResponse);

            JPanel buttonPanel = new JPanel();
            JButton viewAllButton = new JButton("查看全部题");
            JButton viewWrongButton = new JButton("仅看错题");
            JButton backButton = new JButton("返回");

            buttonPanel.add(viewAllButton);
            buttonPanel.add(viewWrongButton);
            buttonPanel.add(backButton);

            // 添加按钮的事件监听器
            viewAllButton.addActionListener(e -> {
                panel.removeAll();
                panel.add(rankPanel, BorderLayout.NORTH);
                panel.add(formattedScrollPane, BorderLayout.CENTER);
                panel.add(buttonPanel, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();
            });

            viewWrongButton.addActionListener(e -> {
                JPanel wrongPanel = formatDetailedScores(detailsResponse, cookies, true);
                panel.removeAll();
                JScrollPane wrongScrollPane = new JScrollPane(wrongPanel);
                panel.add(rankPanel, BorderLayout.NORTH);
                panel.add(wrongScrollPane, BorderLayout.CENTER);
                panel.add(buttonPanel, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();
            });

            backButton.addActionListener(e -> {
                detailsFrame.dispose(); // 关闭窗口
            });

            // 初始化面板
            panel.add(rankPanel, BorderLayout.NORTH); // 添加排名信息面板
            panel.add(formattedScrollPane, BorderLayout.CENTER); // 添加详细得分面板
            panel.add(buttonPanel, BorderLayout.SOUTH); // 添加按钮
            detailsFrame.add(panel);
            detailsFrame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "无法获取详细得分。");
        }
    }




    private JPanel formatDetailedScores(String detailsResponse, Map<String, String> cookies, boolean onlyWrong) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10)); // 添加内边距

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

            // 总得分和总满分
            int totalMaxScore = 0;

            // 计算所有主观题的 URL
            List<String> subjectiveUrls = new ArrayList<>();
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                int type = question.getInt("type");
                if (type == 1) { // 主观题
                    JSONArray urlsArray = question.optJSONArray("url");
                    if (urlsArray != null) {
                        for (int j = 0; j < urlsArray.length(); j++) {
                            subjectiveUrls.add(urlsArray.getString(j));
                        }
                    }
                }
            }

            // 下载主观题的图片
            List<ImageIcon> images = downloadSubjectiveAnswers(subjectiveUrls, cookies);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                String name = question.getString("name");
                int mafen = question.getInt("manfen");
                int score = question.getInt("score");
                int type = question.getInt("type");
                String myAnswer = question.optString("myAnswer", "无");
                String answer = question.optString("answer", "无");

                // 累加总得分和总满分
                totalScore += score;
                totalMaxScore += mafen;

                // Skip questions if we are only displaying wrong ones and the score equals the maximum
                if (onlyWrong && score == mafen) {
                    continue;
                }

                // Create a panel for each question
                JPanel questionPanel = new JPanel();
                questionPanel.setLayout(new BoxLayout(questionPanel, BoxLayout.Y_AXIS));
                questionPanel.setBorder(new EmptyBorder(5, 10, 5, 10)); // 添加内边距

                // 创建题号和分数标签
                JLabel questionLabel = new JLabel();
                questionLabel.setText(String.format("<html><span style='font-size:26px; font-weight:bold;'>题号: %s</span><br>" +
                        "<span style='font-size:24px; font-weight:bold; color: #000;'>得分: %d</span>" +
                        "<span style='font-size:20px; font-weight:bold; color: #666666;'> / </span>" +
                        "<span style='font-size:20px; color: #666666;'> %d</span><br></html>", name, score, mafen));
                questionPanel.add(questionLabel);

                if (type == 2) { // 选择题
                    JLabel answerLabel = new JLabel(String.format("<html><span style='font-size:20px; color: #000;'>我的答案: %s<br>" +
                            "<span style='font-size:18px; color: #666666;'>正确答案: %s</span></span><br><br></html>", myAnswer, answer));
                    questionPanel.add(answerLabel);
                } else if (type == 1) { // 主观题
                    JSONArray urlsArray = question.optJSONArray("url");
                    if (urlsArray != null) {
                        for (int j = 0; j < urlsArray.length(); j++) {
                            String url = urlsArray.getString(j);
                            int imageIndex = subjectiveUrls.indexOf(url);
                            if (imageIndex >= 0 && imageIndex < images.size()) {
                                ImageIcon subjectiveAnswer = images.get(imageIndex);
                                if (subjectiveAnswer != null) {
                                    questionPanel.add(new JLabel("<html><span style='font-size:20px;'>我的答题情况: </span></html>", JLabel.LEFT));
                                    JLabel imageLabel = new JLabel();
                                    Image image = subjectiveAnswer.getImage();
                                    int width = 760; // 设置宽度为面板宽度
                                    int height = (image.getHeight(null) * width) / image.getWidth(null); // 计算高度
                                    image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                                    imageLabel.setIcon(new ImageIcon(image));
                                    questionPanel.add(imageLabel);
                                } else {
                                    questionPanel.add(new JLabel("<html><span style='font-size:20px;'>我的答题情况: 无</span></html>"));
                                }
                            } else {
                                questionPanel.add(new JLabel("<html><span style='font-size:20px;'>我的答题情况: 无</span></html>"));
                            }
                        }
                    }
                }

                panel.add(questionPanel);
            }

            // 在面板底部添加总得分
            JLabel totalScoreLabel = new JLabel(String.format("<html><span style='font-size:28px; font-weight:bold;'>总得分: .0f</span>" +
                    "<span style='font-size:24px; font-weight:bold; color: #666666;'> / %.0f</span></html>", totalScore, totalMaxScore));
            panel.add(totalScoreLabel);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return panel;
    }

    private String fetchRankInfo(String examId, String paperId, Map<String, String> cookies) {
        String url = String.format("https://hfs-be.yunxiao.com/v3/exam/%s/papers/%s/rank-info", examId, paperId);
        StringBuilder response = new StringBuilder();

        try {
            URL rankUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) rankUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Cookie", String.join("; ", cookies.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toArray(String[]::new)));

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response.toString();
    }
    private JPanel formatRankInfo(String rankInfoResponse, String detailsResponse, Double score, Double totalScore) {
        JPanel rankInfoPanel = new JPanel();
        rankInfoPanel.setLayout(new GridLayout(6, 1)); // 调整以适应所有信息，包括总分
        rankInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // 添加内边距

        try {
            JSONObject jsonResponse = new JSONObject(rankInfoResponse);
            JSONObject data = jsonResponse.getJSONObject("data");
            JSONObject highest = data.getJSONObject("highest");
            JSONObject avg = data.getJSONObject("avg");
            JSONObject rank = data.getJSONObject("rank");
            JSONObject number = data.getJSONObject("number");

            // 格式化输出字符串，使用HTML以获得更好的字体大小和样式
            //String totalScoreString = String.format("<html><span style='font-size: 20px; font-weight: bold;'>总得分: %.0f / %d</span></html>", totalScore);
            String myScoreString = String.format("<html><span style='font-size: 20px; font-weight: bold;'>我的分数: %s </span></html>", formatScore(totalScore));
            String highestScores = String.format("<html><span style='font-size: 14px;'>年级最高分<span style='color: gray;'>/</span>班级最高分: %s<span style='color: gray;'>/</span>%s</span></html>",
                    formatScore(highest.getDouble("grade")), formatScore(highest.getDouble("class")));
            String avgScores = String.format("<html><span style='font-size: 14px;'>班级平均分<span style='color: gray;'>/</span>年级平均分: %s<span style='color: gray;'>/</span>%s</span></html>",
                    formatScore(avg.getDouble("class")), formatScore(avg.getDouble("grade")));
            String classRanking = String.format("<html><span style='font-size: 14px;'>班级排名: %d<span style='color: gray;'>/</span>%d</span></html>",
                    rank.getInt("class"), number.getInt("class"));
            String gradeRanking = String.format("<html><span style='font-size: 14px;'>年级排名: %d<span style='color: gray;'>/</span>%d</span></html>",
                    rank.getInt("grade"), number.getInt("grade"));

            // 添加总分和其他信息到面板
            //rankInfoPanel.add(new JLabel(totalScoreString));  // 显示总得分
            rankInfoPanel.add(new JLabel(myScoreString));
            rankInfoPanel.add(new JLabel(highestScores));
            rankInfoPanel.add(new JLabel(avgScores));
            rankInfoPanel.add(new JLabel(classRanking));
            rankInfoPanel.add(new JLabel(gradeRanking));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rankInfoPanel;
    }

    // 格式化得分的方法，去掉小数部分如果是0
    private String formatScore(double score) {
        return (score % 1 == 0) ? String.format("%.0f", score) : String.format("%.2f", score);
    }





    private List<ImageIcon> downloadSubjectiveAnswers(List<String> urls, Map<String, String> cookies) {
        List<ImageIcon> images = new ArrayList<>();
        int totalUrls = urls.size(); // URL 数量
        AtomicInteger urlCounter = new AtomicInteger(0); // 已下载的 URL 数量

        // 设置进度条的颜色和样式
        UIManager.put("ProgressBar.background", Color.LIGHT_GRAY);
        UIManager.put("ProgressBar.foreground", new Color(70, 130, 180)); // 设置前景色为钢蓝色
        UIManager.put("ProgressBar.border", BorderFactory.createLineBorder(Color.GRAY, 2)); // 设置边框
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK); // 选中文本的背景色
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE); // 选中文本的前景色

        // 创建进度条对话框
        JDialog progressDialog = new JDialog((java.awt.Frame) null, "下载答题数据中...", true);
        progressDialog.setSize(400, 200);
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setLayout(new BorderLayout()); // 使用 BorderLayout 布局

        // 创建进度条
        JProgressBar progressBar = new JProgressBar(0, totalUrls);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("SansSerif", Font.BOLD, 16)); // 更改进度条字体

        // 创建提示标签
        JLabel messageLabel = new JLabel("正在下载，请稍候...");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER); // 居中对齐
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14)); // 设置字体

        // 添加提示标签和进度条到对话框
        progressDialog.add(messageLabel, BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);

        // 调用 pack() 以调整对话框大小并布局组件
        progressDialog.pack();

        // 创建 SwingWorker 来处理图片下载
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (String url : urls) {
                    // 处理多个 URL
                    String[] urlArray = url.split(",\\s*"); // 用逗号和可选空格分隔 URL

                    for (String singleUrl : urlArray) {
                        // 清理 URL 中的意外符号
                        singleUrl = singleUrl.replace("[\"", "").replace("\"]", "").trim();
                        singleUrl = singleUrl.replace('"', ' ').replace("”", "").trim();

                        try {
                            URL imageUrl = new URL(singleUrl);
                            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                            connection.setRequestMethod("GET");
                            connection.setRequestProperty("Cookie", String.join("; ", cookies.entrySet().stream()
                                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                                    .toArray(String[]::new)));

                            InputStream inputStream = connection.getInputStream();
                            ImageIcon imageIcon = new ImageIcon(ImageIO.read(inputStream));
                            images.add(imageIcon);

                            // 更新进度条
                            int progress = urlCounter.incrementAndGet();
                            setProgress(progress);
                            publish(progress);

                            inputStream.close();
                            connection.disconnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int progress = chunks.get(chunks.size() - 1);
                progressBar.setValue(progress);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
            }
        };

        worker.execute();
        progressDialog.setVisible(true);

        return images;
    }


}