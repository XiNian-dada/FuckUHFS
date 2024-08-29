import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleTypeBox;
    private JButton loginButton;
    private JCheckBox showPasswordCheckBox;
    private JCheckBox savePasswordCheckBox;
    private JComboBox<String> accountSelectionBox;
    private JButton deleteAccountButton;

    private static final boolean DEBUG_MODE = false; // Set to true to enable debug mode
    private static final String DEBUG_USERNAME = "15319770091";
    private static final String DEBUG_PASSWORD = "337289t";

    private Map<String, String[]> savedAccounts; // Updated to store password and roleType
    private static final String ACCOUNT_FILE = "saved_accounts.properties";

    public LoginUI() {
        setTitle("登录你的好分数账号");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        savedAccounts = new HashMap<>();
        loadSavedAccounts();

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel accountLabel = new JLabel("选择账号:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(accountLabel, gbc);

        accountSelectionBox = new JComboBox<>();
        accountSelectionBox.addItem("选择或输入新账号");
        for (String account : savedAccounts.keySet()) {
            accountSelectionBox.addItem(account);
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(accountSelectionBox, gbc);

        JLabel userLabel = new JLabel("用户名:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(userLabel, gbc);

        usernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("密码:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(passwordField, gbc);

        showPasswordCheckBox = new JCheckBox("显示密码");
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(showPasswordCheckBox, gbc);

        savePasswordCheckBox = new JCheckBox("保存账号和密码");
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(savePasswordCheckBox, gbc);

        showPasswordCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showPasswordCheckBox.isSelected()) {
                    passwordField.setEchoChar((char) 0); // Show password
                } else {
                    passwordField.setEchoChar('*'); // Hide password
                }
            }
        });

        JLabel roleLabel = new JLabel("登录类型:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(roleLabel, gbc);

        roleTypeBox = new JComboBox<>(new String[]{"学生", "家长"});
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(roleTypeBox, gbc);

        loginButton = new JButton("登录");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(loginButton, gbc);

        deleteAccountButton = new JButton("删除账号");
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(deleteAccountButton, gbc);

        add(panel);

        accountSelectionBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selectedAccount = (String) accountSelectionBox.getSelectedItem();
                    if (savedAccounts.containsKey(selectedAccount)) {
                        String[] accountInfo = savedAccounts.get(selectedAccount);
                        usernameField.setText(selectedAccount);
                        passwordField.setText(new String(Base64.getDecoder().decode(accountInfo[0]))); // Decode password
                        roleTypeBox.setSelectedIndex(Integer.parseInt(accountInfo[1]) - 1); // Set roleType
                    } else {
                        usernameField.setText("");
                        passwordField.setText("");
                    }
                }
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                int roleType = roleTypeBox.getSelectedIndex() + 1; // 1 for student, 2 for parent

                if (DEBUG_MODE) {
                    username = DEBUG_USERNAME;
                    password = DEBUG_PASSWORD;
                }

                try {
                    Login login = new Login();
                    Map<String, String> cookies = login.loginAndGetCookies("https://hfs-be.yunxiao.com/v2/users/sessions", username, password, roleType);
                    String response = login.getExamList("https://hfs-be.yunxiao.com/v3/exam/list", cookies);

                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.isNull("data") || jsonResponse.get("data").equals(JSONObject.NULL)) {
                        JOptionPane.showMessageDialog(null, "登录失败，请检查你的账号密码。");
                    } else {
                        if (savePasswordCheckBox.isSelected()) {
                            savedAccounts.put(username, new String[]{Base64.getEncoder().encodeToString(password.getBytes()), String.valueOf(roleType)}); // Encode and save password with roleType
                            if (accountSelectionBox.getItemCount() > 1 && !isAccountAlreadyInComboBox(username)) {
                                accountSelectionBox.addItem(username); // Add account to selection box if it's new
                            }
                            saveAccountsToFile(); // Save the accounts to file
                        }
                        // 隐藏或关闭当前窗口
                        setVisible(false); // 或者使用 dispose() 来完全关闭窗口
                        new ExamListUI(response, cookies); // 打开 ExamListUI 窗口
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "登录失败，请检查你的账号密码。");
                }
            }
        });



        deleteAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedAccount = (String) accountSelectionBox.getSelectedItem();
                if (savedAccounts.containsKey(selectedAccount)) {
                    savedAccounts.remove(selectedAccount);
                    accountSelectionBox.removeItem(selectedAccount);
                    saveAccountsToFile();

                    // Automatically select the first available account after deletion
                    if (accountSelectionBox.getItemCount() > 1) {
                        String firstAccount = (String) accountSelectionBox.getItemAt(1); // Skip "选择或输入新账号"
                        accountSelectionBox.setSelectedItem(firstAccount);
                    } else {
                        // If no accounts left, clear fields
                        usernameField.setText("");
                        passwordField.setText("");
                        roleTypeBox.setSelectedIndex(0);
                    }

                    JOptionPane.showMessageDialog(null, "账号已删除。");
                }
            }
        });

        // Auto-fill the first saved account if available
        if (accountSelectionBox.getItemCount() > 1) {
            accountSelectionBox.setSelectedIndex(1);
        }

        setVisible(true);
    }

    private void loadSavedAccounts() {
        File file = new File(ACCOUNT_FILE);
        if (!file.exists()) {
            return;
        }

        try (InputStream input = new FileInputStream(file)) {
            Properties prop = new Properties();
            prop.load(input);

            for (String key : prop.stringPropertyNames()) {
                savedAccounts.put(key, prop.getProperty(key).split(",")); // Split to get password and roleType
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveAccountsToFile() {
        try (OutputStream output = new FileOutputStream(ACCOUNT_FILE)) {
            Properties prop = new Properties();

            for (Map.Entry<String, String[]> entry : savedAccounts.entrySet()) {
                prop.setProperty(entry.getKey(), String.join(",", entry.getValue())); // Join password and roleType
            }

            prop.store(output, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isAccountAlreadyInComboBox(String account) {
        for (int i = 0; i < accountSelectionBox.getItemCount(); i++) {
            if (accountSelectionBox.getItemAt(i).equals(account)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginUI::new);
    }
}
