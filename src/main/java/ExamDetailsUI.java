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

    public void showSubjectDetails(String subject, String paperId, String pid, Map<String, String> cookies) {
        try {
            String detailsUrl = String.format("https://hfs-be.yunxiao.com/v3/exam/%s/papers/%s/answer-picture?pid=%s",
                    examId, paperId, pid);

            Login login = new Login();
            String detailsResponse = login.getExamDetails(detailsUrl, cookies);

            // Print raw JSON response for debugging
            System.out.println("Raw JSON Response:\n" + detailsResponse);

            JFrame detailsFrame = new JFrame("详细得分 - " + subject);
            detailsFrame.setSize(840, 600);
            detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            detailsFrame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout());
            JPanel formattedPanel = formatDetailedScores(detailsResponse, cookies, false);
            JScrollPane formattedScrollPane = new JScrollPane(formattedPanel);

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

            // Add action listeners to buttons
            viewAllButton.addActionListener(e -> {
                panel.removeAll();
                panel.add(formattedScrollPane, BorderLayout.CENTER);
                panel.add(buttonPanel, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();
            });

            viewWrongButton.addActionListener(e -> {
                JPanel wrongPanel = formatDetailedScores(detailsResponse, cookies, true); // Pass a flag to filter wrong questions
                panel.removeAll();
                JScrollPane wrongScrollPane = new JScrollPane(wrongPanel);
                panel.add(wrongScrollPane, BorderLayout.CENTER);
                panel.add(buttonPanel, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();
            });

            backButton.addActionListener(e -> {
                detailsFrame.dispose(); // Close the frame
            });

            panel.add(formattedScrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
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

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return panel;
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

