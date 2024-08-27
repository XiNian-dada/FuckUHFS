import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleTypeBox;
    private JButton loginButton;

    // Debug mode flag
    private static final boolean DEBUG_MODE = true; // Set to true to enable debug mode

    // Debug credentials
    private static final String DEBUG_USERNAME = "15319770091";
    private static final String DEBUG_PASSWORD = "337289t";

    public LoginUI() {
        setTitle("登录");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("用户名:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("密码:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(passwordField, gbc);

        JLabel roleLabel = new JLabel("登录类型:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(roleLabel, gbc);

        roleTypeBox = new JComboBox<>(new String[]{"学生", "家长"});
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(roleTypeBox, gbc);

        loginButton = new JButton("登录");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        add(panel);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                int roleType = roleTypeBox.getSelectedIndex() + 1; // 1 for student, 2 for parent

                // If debug mode is enabled, use debug credentials
                if (DEBUG_MODE) {
                    username = DEBUG_USERNAME;
                    password = DEBUG_PASSWORD;
                }

                try {
                    Login login = new Login();
                    Map<String, String> cookies = login.loginAndGetCookies("https://hfs-be.yunxiao.com/v2/users/sessions", username, password, roleType);
                    String response = login.getExamList("https://hfs-be.yunxiao.com/v3/exam/list", cookies);
                    new ExamListUI(response, cookies);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "登录失败，请检查你的账号密码。");
                }
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI());
    }
}
