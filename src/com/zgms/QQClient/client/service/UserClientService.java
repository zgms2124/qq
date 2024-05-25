package com.zgms.QQClient.client.service;

import com.zgms.ToView.MainChatWindow;
import com.zgms.common.Message;
import com.zgms.common.MessageType;
import com.zgms.common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：该类完成用户登录验证和用户注册等功能.
 * @version: 1.0
 */
public class UserClientService {

    private User user;
    private MainChatWindow mainChatWindow;
    private ClientConnectServerThread clientConnectServerThread;
    private Map<String, Consumer<String[]>> callbacks = new HashMap<>();
    private Socket socket;

    public UserClientService() {
        this.user = new User();
    }

    public void setMainChatWindow(MainChatWindow mainChatWindow) {
        this.mainChatWindow = mainChatWindow;
        if (this.clientConnectServerThread != null) {
            this.clientConnectServerThread.setMainWindow(mainChatWindow);
        }
    }

    public void getFriendsList() {
        sendMessage(createMessage(user.getUserId(), MessageType.MESSAGE_GET_FRIENDS, null));
    }

    public void getGroups() {
        sendMessage(createMessage(user.getUserId(), MessageType.MESSAGE_GET_GROUPS, null));
    }

    public void getGroupMembers(String selectedGroup) {
        sendMessage(createMessage(user.getUserId(), MessageType.MESSAGE_GET_GROUP_MEMBERS, selectedGroup));
    }

    public void addFriend(String friendId) {
        Message message = createMessage(user.getUserId(), MessageType.MESSAGE_ADD_FRIEND, null);
        message.setGetter(friendId);
        sendMessage(message);
    }

    public String checkUser(String userId, String pwd) {
        this.user.setUserId(userId);
        this.user.setPasswd(pwd);

        Message message = createMessage(userId, MessageType.MESSAGE_LOGIN, userId + "," + pwd);
        return processLoginOrRegister(message);
    }

    public String registerUser(String newUserId, String newPwd) {
        this.user.setUserId(newUserId);
        this.user.setPasswd(newPwd);

        Message message = createMessage(newUserId, MessageType.MESSAGE_REGISTER, newUserId + "," + newPwd);
        return processLoginOrRegister(message);
    }

    public void onlineFriendList() {
        sendMessage(createMessage(user.getUserId(), MessageType.MESSAGE_GET_ONLINE_FRIEND, null));
    }

    public void logout() {
        Message message = createMessage(user.getUserId(), MessageType.MESSAGE_CLIENT_EXIT, null);
        sendMessage(message);

        try {
            clientConnectServerThread.closeResources();
            socket.close();
            ManageClientConnectServerThread.removeClientConnectServerThread(user.getUserId());
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getUserState() {
        return user.isLogin();
    }

    public void onServerResponse(Message message) {
        Consumer<String[]> callback = callbacks.get(message.getMesType());
        if (callback != null) {
            callback.accept(message.getContent().split(" "));
        }
    }

    private Message createMessage(String senderId, String messageType, String content) {
        Message message = new Message();
        message.setMesType(messageType);
        message.setSender(senderId);
        message.setContent(content);
        return message;
    }

    void sendMessage(Message message) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(getSocket().getOutputStream());
            oos.writeObject(message);
            System.out.println("已向服务器发送消息：" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String processLoginOrRegister(Message message) {
        String result = MessageType.MESSAGE_LOGIN_FAIL;

        try {
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            sendMessage(message);

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message responseMessage = (Message) ois.readObject();
            result=responseMessage.getMesType();

            if (MessageType.MESSAGE_LOGIN_SUCCEED.equals(responseMessage.getMesType())) {
                clientConnectServerThread = new ClientConnectServerThread(socket, mainChatWindow);
                clientConnectServerThread.setUserClientService(this);
                clientConnectServerThread.start();
                ManageClientConnectServerThread.addClientConnectServerThread(user.getUserId(), clientConnectServerThread);
            } else if (MessageType.MESSAGE_IS_LOGIN.equals(responseMessage.getMesType())) {
                socket.close();
            } else {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private Socket getSocket() {
        if (socket == null) {
            try {
                socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return socket;
    }
}
