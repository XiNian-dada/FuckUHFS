import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExamDetailsUI extends JFrame {
    private String examId;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.#");
    private JPanel questionsPanel;
    private boolean showAllQuestions = true; // 默认显示所有题目
    private JProgressBar progressBar;
    public ExamDetailsUI(String overviewResponse, Map<String, String> cookies, String examId) {
        this.examId = examId;
        setTitle("考试详情");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.SOUTH);
        JPanel mainPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane);

        JPanel controlPanel = new JPanel();
        JButton backButton = new JButton("返回");
        JButton showAllButton = new JButton("查看全部题");
        JButton showIncorrectButton = new JButton("仅看错题");

        controlPanel.add(backButton);
        controlPanel.add(showAllButton);
        controlPanel.add(showIncorrectButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        questionsPanel = new JPanel();
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));
        questionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel.add(new JScrollPane(questionsPanel), BorderLayout.CENTER);

        // Set button actions
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close the current window
            }
        });

        showAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllQuestions = true;
                updateQuestionPanel(cookies);
            }
        });

        showIncorrectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllQuestions = false;
                updateQuestionPanel(cookies);
            }
        });

        try {
            JSONObject jsonResponse = new JSONObject(overviewResponse);
            JSONObject data = jsonResponse.getJSONObject("data");

            // Exam overview
            double manfen = data.getDouble("manfen");
            double score = data.getDouble("score");
            double manfenBeforeGrading = data.getDouble("manfenBeforeGrading");
            double scoreBeforeGrading = data.getDouble("scoreBeforeGrading");
            int classRank = data.getInt("classRank");
            int gradeRank = data.getInt("gradeRank");
            int classStuNum = data.getInt("classStuNum");
            int gradeStuNum = data.getInt("gradeStuNum");

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

            mainPanel.add(overviewPanel, BorderLayout.NORTH);

            // Papers
            JSONArray papers = data.getJSONArray("papers");
            for (int i = 0; i < papers.length(); i++) {
                JSONObject paper = papers.getJSONObject(i);
                String subject = paper.getString("subject");
                String paperId = paper.getString("paperId");
                String pid = paper.getString("pid");
                double subjectManfen = paper.getDouble("manfen");
                double subjectScore = paper.getDouble("score");
                double subjectManfenBeforeGrading = paper.getDouble("manfenBeforeGrading");
                double subjectScoreBeforeGrading = paper.getDouble("scoreBeforeGrading");

                addPaperPanel(subject, paperId, pid, subjectManfen, subjectScore, subjectManfenBeforeGrading, subjectScoreBeforeGrading, cookies);
            }

            // Add mouse wheel listener for scroll
            scrollPane.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    JScrollBar vertical = scrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getValue() + e.getWheelRotation() * 10);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }

    private void addPaperPanel(String subject, String paperId, String pid, double subjectManfen, double subjectScore, double subjectManfenBeforeGrading, double subjectScoreBeforeGrading, Map<String, String> cookies) {
        JPanel paperPanel = new JPanel();
        paperPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcPaper = new GridBagConstraints();
        gbcPaper.insets = new Insets(5, 5, 5, 5);

        gbcPaper.gridx = 0;
        gbcPaper.gridy = 0;
        JLabel subjectLabel = new JLabel(subject + ": ");
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 45));
        paperPanel.add(subjectLabel, gbcPaper);

        gbcPaper.gridx = 1;
        gbcPaper.gridy = 0;

        String scoreHtml = String.format(
                "<html><body>" +
                        "<span style='font-size: 20px; font-family: SansSerif; font-weight: bold; color: #000000;'>得分: %s</span> " +
                        "<span style='font-size: 18px; font-family: SansSerif; color: #666666;'> / %s</span>" +
                        "<br/>" +
                        "<span style='font-size: 16px; font-family: SansSerif; color: #999999;'>原始分: %s / %s</span>" +
                        "</body></html>",
                formatNumber(subjectScore), formatNumber(subjectManfen),
                formatNumber(subjectScoreBeforeGrading), formatNumber(subjectManfenBeforeGrading)
        );
        JLabel scoreLabel = new JLabel(scoreHtml);
        paperPanel.add(scoreLabel, gbcPaper);

        gbcPaper.gridx = 2;
        gbcPaper.gridy = 0;
        JButton detailsButton = new JButton("查看详细得分");
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSubjectDetails(subject, paperId, pid, cookies);
            }
        });
        paperPanel.add(detailsButton, gbcPaper);

        questionsPanel.add(paperPanel);
        questionsPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add some space between subjects
    }

    private void updateQuestionPanel(Map<String, String> cookies) {
        questionsPanel.removeAll();
        // Reload the questions based on the current filter
        try {
            String url = String.format("https://hfs-be.yunxiao.com/v3/exam/%s/papers/%s/answer-picture?pid=%s", examId, "paperId", "pid");
            String detailsResponse = getResponse(url, cookies);
            JSONArray questions = new JSONArray(detailsResponse);
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                String subject = question.getString("subject");
                double manfen = question.getDouble("manfen");
                double score = question.getDouble("score");
                boolean isCorrect = question.getBoolean("isCorrect"); // Assuming this key exists

                // Show only incorrect questions if not in "showAllQuestions" mode
                if (showAllQuestions || !isCorrect) {
                    addQuestionPanel(question, cookies);
                }
            }
            questionsPanel.revalidate();
            questionsPanel.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addQuestionPanel(JSONObject question, Map<String, String> cookies) throws IOException, JSONException {
        String questionId = question.getString("id");
        String questionText = question.getString("name");
        String questionType = question.getString("type");
        double manfen = question.getDouble("manfen");
        double score = question.getDouble("score");
        String myAnswer = question.getString("myAnswer");
        String answer = question.getString("answer");
        String url = question.getString("answerUrl");

        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcQuestion = new GridBagConstraints();
        gbcQuestion.insets = new Insets(5, 5, 5, 5);

        gbcQuestion.gridx = 0;
        gbcQuestion.gridy = 0;
        JLabel questionLabel = new JLabel(String.format("题号: %s", questionId));
        questionLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        questionPanel.add(questionLabel, gbcQuestion);

        gbcQuestion.gridy++;
        JLabel scoreLabel = new JLabel(String.format(
                "<html><body>" +
                        "<span style='font-size: 18px; font-family: SansSerif; font-weight: bold; color: #000000;'>得分: %s</span> " +
                        "<span style='font-size: 16px; font-family: SansSerif; color: #666666;'>/ %s</span>" +
                        "</body></html>",
                formatNumber(score), formatNumber(manfen)
        ));
        questionPanel.add(scoreLabel, gbcQuestion);

        gbcQuestion.gridy++;
        JLabel typeLabel = new JLabel(String.format("题型: %s", questionType.equals("2") ? "选择题" : "主观题"));
        questionPanel.add(typeLabel, gbcQuestion);

        gbcQuestion.gridy++;
        JLabel myAnswerLabel = new JLabel(String.format("我的答案: %s", myAnswer));
        questionPanel.add(myAnswerLabel, gbcQuestion);

        gbcQuestion.gridy++;
        JLabel answerLabel = new JLabel(String.format("正确答案: %s", answer));
        questionPanel.add(answerLabel, gbcQuestion);

        gbcQuestion.gridy++;
        if (questionType.equals("1")) { // Subjective question
            BufferedImage image = getAnswerImage(url);
            if (image != null) {
                JLabel imageLabel = new JLabel(new ImageIcon(image));
                questionPanel.add(imageLabel, gbcQuestion);
            }
        }

        questionsPanel.add(questionPanel);
    }

    private BufferedImage getAnswerImage(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getResponse(String url, Map<String, String> cookies) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        // Set cookies
        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            cookieHeader.append(entry.getKey()).append("=").append(entry.getValue()).append("; ");
        }
        if (cookieHeader.length() > 0) {
            con.setRequestProperty("Cookie", cookieHeader.toString());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
    private void showSubjectDetails(String subject, String paperId, String pid, Map<String, String> cookies) {
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
            JScrollPane rawScrollPane = new JScrollPane(rawTextArea);

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

        // 创建进度条对话框
        JDialog progressDialog = new JDialog((java.awt.Frame) null, "下载答题数据中...", true);
        progressDialog.setSize(400, 200);
        progressDialog.setLocationRelativeTo(null);

        // 创建进度条
        JProgressBar progressBar = new JProgressBar(0, totalUrls);
        progressBar.setStringPainted(true);

        // 添加进度条到对话框
        progressDialog.add(progressBar);

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



    private String formatNumber(double number) {
        return DECIMAL_FORMAT.format(number);
    }
}

