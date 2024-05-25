package com.zgms.ToView;

import com.zgms.QQClient.client.service.MessageClientService;
import com.zgms.QQClient.client.service.UserClientService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import static java.lang.Thread.sleep;

public class MainChatWindow extends JFrame {

    private MessageClientService messageClientService;
    private UserClientService userClientService;
    private String userId;

    private JList<String> userList;
    private JTextArea historyArea;
    private JTextField messageField;
    private String[] groups;
    private String[] groupMenberList;
    private String[] friendsList;

    public MainChatWindow(String userId) {
        this.userId = userId;
        prepareInterface();
    }

    private void prepareInterface() {
        setTitle("主聊天窗口 - 用户 " + userId);
        setSize(800, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        userList = new JList<>();
        JScrollPane userPane = new JScrollPane(userList);
        userPane.setPreferredSize(new Dimension(200, 0));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        JScrollPane historyPane = new JScrollPane(historyArea);

        messageField = new JTextField();
        addMessageFieldListener();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (userClientService != null) {
                    userClientService.logout();
                }
                super.windowClosing(e);
            }
        });

        add(userPane, BorderLayout.WEST);
        add(historyPane, BorderLayout.CENTER);

        setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(constructUserMenu());
        menuBar.add(constructMessageMenu());
        menuBar.add(constructFileMenu());
        return menuBar;
    }

    private JMenu constructUserMenu() {
        JMenu userMenu = new JMenu("Users");
        JMenuItem onlineUsers = new JMenuItem("Display online users");
        onlineUsers.addActionListener(e -> {
            if (userClientService != null) {
                userClientService.onlineFriendList();
            }
        });

        userMenu.add(onlineUsers);
        return userMenu;
    }

    private JMenu constructMessageMenu() {
        JMenu messageMenu = new JMenu("Messages");
        JMenuItem broadcastMessage = new JMenuItem("Broadcast message");
        JMenuItem privateMessage = new JMenuItem("Private message");
        JMenuItem groupMessage = new JMenuItem("Group message");

        broadcastMessage.addActionListener(e -> sendMessageToAll());
        privateMessage.addActionListener(e -> sendMessageToOne());
        groupMessage.addActionListener(this::displayGroupSelectionDialog);


        messageMenu.add(broadcastMessage);
        messageMenu.add(privateMessage);
        messageMenu.add(groupMessage);

        return messageMenu;
    }

    private void displayGroupSelectionDialog(ActionEvent e) {
        userClientService.getGroups();
        try {
            sleep(100);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (groups != null && groups.length > 0) {
            String selectedGroup = (String) JOptionPane.showInputDialog(
                    this,
                    "Select the group:",
                    "Group Message",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    groups,
                    groups[0]
            );
            if (selectedGroup != null && !selectedGroup.isEmpty()) {
                sendMessageToGroup(selectedGroup);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No groups available.");
        }
    }

    public void updateGroupList(String[] groups) {
        // 更新群组列表，仅更新数组内容
        this.groups = groups;
    }

    public void sendMessageToGroup(String selectedGroup) {
        // 检查用户是否是此组的成员
        userClientService.getGroupMembers(selectedGroup); // 使用服务端方法
        try {
            sleep(100);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (groupMenberList != null && Arrays.asList(groupMenberList).contains(userId)) {
            String message = JOptionPane.showInputDialog(
                    this,
                    "Enter the message to send to group " + selectedGroup + ":"
            );
            if (message != null && !message.isBlank()) {
                messageClientService.sendMessageToGroup(selectedGroup, userId, message); // Assumed method signature
                historyArea.append("我（发送给群组：" + selectedGroup + "）: " + message + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(this, "你不是组：" + selectedGroup + " 的成员，无法发送消息。");
        }
    }


    private JMenu constructFileMenu() {
        JMenu fileMenu = new JMenu("Files");
        JMenuItem sendFile = new JMenuItem("Send file");
        sendFile.addActionListener(e -> JOptionPane.showMessageDialog(this, "File sending not implemented yet."));
        fileMenu.add(sendFile);

        return fileMenu;
    }

    private void sendMessageToAll() {
        String message = JOptionPane.showInputDialog(this, "Enter message to broadcast:");
        if (message != null && !message.isEmpty() && messageClientService != null) {
            messageClientService.sendMessageToAll(message, userId);
            historyArea.append("我（广播给大家）: " + message + "\n");
            messageField.setText(""); // Clear the text field after sending the message

        }
    }

    // MainChatWindow 类中添加新方法


    private void sendMessageToOne() {
        userClientService.getFriendsList(); // 使用服务端方法获取好友列表
        try {
            sleep(100); // 暂停一会儿以等待服务器响应
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        String selectedUser = userList.getSelectedValue();
        if (selectedUser != null) {
            if (friendsList != null && Arrays.asList(friendsList).contains(selectedUser)) {
                String message = JOptionPane.showInputDialog(this, "Enter message to send to " + selectedUser + ":");
                if (message != null && !message.isEmpty() && messageClientService != null) {
                    messageClientService.sendMessageToOne(message, userId, selectedUser);
                    historyArea.append("我（发送给" +selectedUser+"）: "+ message + "\n");
                    messageField.setText(""); // Clear the text field after sending the message
                }
            } else {
                // 用户不在好友列表中，询问用户是否添加好友
                int response = JOptionPane.showConfirmDialog(this,
                        selectedUser + "不是你的好友，是否添加为好友？",
                        "添加好友",
                        JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    // 用户点击了“是”，向服务器发送添加好友请求
                    userClientService.addFriend(selectedUser); // 使用服务端方法发送添加好友请求
                }
            }


        } else {
            JOptionPane.showMessageDialog(this, "Please select a user from the user list first!");
        }



    }
    public boolean showFriendRequestDialog(String friendId) {
        // 弹出一个对话框，询问用户是否接受好友请求
        int dialogResult = JOptionPane.showConfirmDialog(
                null,
                friendId + " 想添加你为好友，是否接受？",
                "添加好友请求",
                JOptionPane.YES_NO_OPTION
        );

        return dialogResult == JOptionPane.YES_OPTION;
    }

    private void addMessageFieldListener() {
        messageField.addActionListener(e -> {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    // Check if the messageClientService is not null to ensure there's a connection to the server
                    if (messageClientService != null) {
                        messageClientService.sendMessageToOne(message, userId, selectedUser);
                        historyArea.append("我（发送给" +selectedUser+"）: "+ message + "\n");
                        messageField.setText(""); // Clear the text field after sending the message
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a user from the user list to send a message.");
                }
            }
        });
    }

    public void updateUserList(String[] users) {
        // This method updates the user list in the UI
        if (users != null) {
            userList.setListData(users);
        } else {
            userList.setListData(new String[]{}); // Clear the list if null is received
        }
    }

    public void setMessageClientService(MessageClientService messageClientService) {
        this.messageClientService = messageClientService;
    }

    public MessageClientService getMessageClientService() {
        return messageClientService;
    }

    public void setUserClientService(UserClientService userClientService) {
        this.userClientService = userClientService;
    }

    public UserClientService getUserClientService() {
        return userClientService;
    }

    public void appendMessage(String message) {
        // This method appends a received message to the history area.
        if (message != null && !message.trim().isEmpty()) {
            historyArea.append(message + "\n");
        }
    }

    public void updateGroupMenbersList(String[] groupMenberList) {
        this.groupMenberList=groupMenberList;
    }

    public void updateFriendsList(String[] friends) {
        // 清空所有好友
        this.friendsList=friends;
    }

    public void offLine() {
        // 弹出一个对话框，通知用户被强制下线
        JOptionPane.showMessageDialog(
                this,
                "您被管理员强制下线",
                "下线通知",
                JOptionPane.WARNING_MESSAGE
        );
        // 关闭所有线程并退出程序
        System.exit(0);
    }

    public void dissolveGroup(String groupId) {
        // 弹出一个对话框，通知用户群组被解散
        JOptionPane.showMessageDialog(
                this,
                "您所在的群组 " + groupId + " 已被管理员解散",
                "群组解散通知",
                JOptionPane.WARNING_MESSAGE
        );
        if(groups==null||groups.length==1){
            groups=null;
            updateGroupList(groups);
            return;
        }

        // 从群组列表中移除解散的群组
        groups = Arrays.stream(groups)
                .filter(group -> !group.equals(groupId))
                .toArray(String[]::new);

        // 如果存在更新群组列表的方法，调用此方法更新群组列表
        updateGroupList(groups);
    }
}