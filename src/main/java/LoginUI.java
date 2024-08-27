import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleTypeBox;
    private JButton loginButton;

    public LoginUI() {
        setTitle("Login");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        add(panel);
        placeComponents(panel);

        setVisible(true);
    }

    private void placeComponents(JPanel panel) {
        panel.setLayout(null);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        usernameField = new JTextField(20);
        usernameField.setBounds(100, 20, 165, 25);
        panel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        passwordField = new JPasswordField(20);
        passwordField.setBounds(100, 50, 165, 25);
        panel.add(passwordField);

        JLabel roleLabel = new JLabel("Role Type:");
        roleLabel.setBounds(10, 80, 80, 25);
        panel.add(roleLabel);

        roleTypeBox = new JComboBox<>(new String[]{"Student", "Parent"});
        roleTypeBox.setBounds(100, 80, 165, 25);
        panel.add(roleTypeBox);

        loginButton = new JButton("Login");
        loginButton.setBounds(10, 110, 255, 25);
        panel.add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                int roleType = roleTypeBox.getSelectedIndex() + 1; // 1 for student, 2 for parent

                try {
                    Login login = new Login();
                    Map<String, String> cookies = login.loginAndGetCookies("https://hfs-be.yunxiao.com/v2/users/sessions", username, password, roleType);
                    String response = login.getExamList("https://hfs-be.yunxiao.com/v3/exam/list", cookies);
                    new ExamListUI(response, cookies);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Login failed. Please check your credentials.");
                }
            }
        });
    }

    public static void main(String[] args) {
        new LoginUI();
    }
}
