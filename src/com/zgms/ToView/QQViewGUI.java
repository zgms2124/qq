package com.zgms.ToView;

import com.zgms.QQClient.client.service.MessageClientService;
import com.zgms.QQClient.client.service.UserClientService;
import com.zgms.common.MessageType;

import javax.swing.*;

public class QQViewGUI {

    private static UserClientService userClientService=new UserClientService();
    private static MessageClientService messageClientService=new MessageClientService();

    public static void main(String[] args) {
        JFrame frame = new JFrame("网络通信系统");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        frame.add(panel);

        placeComponents(panel,frame);

        frame.setVisible(true);
    }

    private static void placeComponents(JPanel panel,JFrame frame) {

        panel.setLayout(null);

        JLabel userLabel = new JLabel("用户名:");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        JTextField userText = new JTextField(20);
        userText.setBounds(100, 20, 160, 25);
        panel.add(userText);

        JLabel passwordLabel = new JLabel("密 码:");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100, 50, 160, 25);
        panel.add(passwordText);

        JButton loginButton = new JButton("登录系统");
        loginButton.setBounds(10, 80, 120, 25);
        panel.add(loginButton);

        JButton registerButton = new JButton("注册系统");
        registerButton.setBounds(140, 80, 120, 25);
        panel.add(registerButton);
        
        // 添加按键监听器
        loginButton.addActionListener(e -> {
            String userId = userText.getText();  //从用户名文本框获取用户名
            String password = new String(passwordText.getPassword());  //从密码文本框获取密码

            // 使用UserClientService的checkUser方法验证登录信息，该方法返回登录结果字符串
            String result = userClientService.checkUser(userId, password);

            if (MessageType.MESSAGE_LOGIN_SUCCEED.equals(result)) {
                JOptionPane.showMessageDialog(frame, "欢迎用户 " + userId);
                frame.dispose();  // 关闭登录窗口

                // 创建主聊天界面，并传入UserClientService对象和当前用户ID进行初始化
                MainChatWindow mainChatWindow=new MainChatWindow( userId);
                mainChatWindow.setVisible(true);
                System.out.println("新建窗口"+mainChatWindow);
                userClientService.setMainChatWindow(mainChatWindow);

                mainChatWindow.setUserClientService(userClientService);
                mainChatWindow.setMessageClientService(messageClientService);
            } else if(MessageType.MESSAGE_IS_LOGIN.equals(result)){
                JOptionPane.showMessageDialog(frame, "已登录，请不要重复登录");
            }
            else { //登录失败
                JOptionPane.showMessageDialog(frame, "登录失败，请检查用户名或密码是否正确");
            }
        });

        registerButton.addActionListener(e -> {
            // 从文本框获取用户名和密码
            String newUserId = userText.getText();
            String newPassword = new String(passwordText.getPassword());

            // 使用UserClientService的registerUser方法进行注册，该方法返回注册结果字符串
            String registerResult = userClientService.registerUser(newUserId, newPassword);

            if (MessageType.MESSAGE_REGISTER_SUCCEED.equals(registerResult)) {
                // 注册成功
                JOptionPane.showMessageDialog(frame, "注册成功！");
            } else {
                // 注册失败
                JOptionPane.showMessageDialog(frame, "注册失败，用户名可能已经存在");
            }
        });
    }
}