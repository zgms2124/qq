package com.zgms.QQClient.client.service;


import com.zgms.ToView.MainChatWindow;
import com.zgms.common.Constant;
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

    //因为我们可能在其他地方用使用user信息, 因此作出成员属性
    private User u = new User();
    private MainChatWindow mainChatWindow;
    private ClientConnectServerThread clientConnectServerThread;
    // 储存回调的 Map，以消息类型作为键
    private Map<String, Consumer<String[]>> callbacks = new HashMap<>();
    //因为Socket在其它地方也可能使用，因此作出属性
    private Socket socket;

    public void setMainChatWindow(MainChatWindow mainChatWindow) {
        this.mainChatWindow = mainChatWindow;
        this.clientConnectServerThread.setMainWindow(mainChatWindow);

    }

    public UserClientService() {
    }

    public void getGroups() {

        // 发送一个 Message 请求分组信息
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_GET_GROUPS);
        message.setSender(u.getUserId());

        try {
            //通过管理线程的集合，通过userId得到这个线程对象
            ClientConnectServerThread clientConnectServerThread =
                    ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId());
            //通过这个线程得到关联的socket
            Socket socket = clientConnectServerThread.getSocket();
            //得到当前线程的Socket 对应的 ObjectOutputStream 对象
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);

            // 在这里添加处理服务器响应的代码
            // 当从服务器收到的数据可用，调用 consumer.accept 方法
            // consumer.accept(data);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onServerResponse(Message message) {
        // 查找储存的回调函数
        Consumer<String[]> callback = callbacks.get(message.getMesType());
        if (callback != null) {
            callback.accept(message.getContent().split(" "));
        }
    }

    //根据userId 和 pwd 到服务器验证该用户是否合法
    public String checkUser(String userId, String pwd) {
        String result=MessageType.MESSAGE_LOGIN_FAIL;
        // 创建新的 User 对象
        u = new User();
        u.setUserId(userId);
        u.setPasswd(pwd);

        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_LOGIN);
        message.setSender(userId);
        // Manually serialize User as a String
        String userString = userId + "," + pwd;
        message.setContent(userString);


        try {
            //连接到服务端，发送u对象
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            //得到ObjectOutputStream对象
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message);//发送User对象

            //读取从服务器回复的Message对象
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message ms = (Message) ois.readObject();

            if (ms.getMesType().equals(MessageType.MESSAGE_LOGIN_SUCCEED)) {//登录OK


                //创建一个和服务器端保持通信的线程-> 创建一个类 ClientConnectServerThread
                clientConnectServerThread =
                        new ClientConnectServerThread(socket,mainChatWindow);
//                        new ClientConnectServerThread();
                //启动客户端的线程

                clientConnectServerThread.setUserClientService(this);
                clientConnectServerThread.start();

                //这里为了后面客户端的扩展，我们将线程放入到集合管理
                ManageClientConnectServerThread.addClientConnectServerThread(userId, clientConnectServerThread);
                result=MessageType.MESSAGE_LOGIN_SUCCEED;
            }
            else if (ms.getMesType().equals(MessageType.MESSAGE_IS_LOGIN)) {
                socket.close();
                result=MessageType.MESSAGE_IS_LOGIN;
            }
            else {
                //如果登录失败, 我们就不能启动和服务器通信的线程, 关闭socket
                socket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }

    //向服务器端请求在线用户列表
    public void onlineFriendList() {

        //发送一个Message , 类型MESSAGE_GET_ONLINE_FRIEND
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_GET_ONLINE_FRIEND);
        message.setSender(u.getUserId());

        //发送给服务器

        try {
            //从管理线程的集合中，通过userId, 得到这个线程对象
            ClientConnectServerThread clientConnectServerThread =
                    ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId());
            //通过这个线程得到关联的socket
            Socket socket = clientConnectServerThread.getSocket();
            //得到当前线程的Socket 对应的 ObjectOutputStream对象
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(message); //发送一个Message对象，向服务端要求在线用户列表
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // UserClientService 类中
    public void logout() {
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_CLIENT_EXIT);
        message.setSender(u.getUserId()); //指定是哪个客户端id

        //发送message
        try {
            // 获取当前用户的连接线程所持有的socket，并通过它获取输出流发送退出消息
            ObjectOutputStream oos =
                    new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId()).getSocket().getOutputStream());
            oos.writeObject(message);
            System.out.println(u.getUserId() + " 退出系统 ");
            u.setLogin(Constant.OUT_LOGIN);

            // 关闭资源
            ManageClientConnectServerThread.getClientConnectServerThread(u.getUserId()).closeResources();
            socket.close();
            // 移除客户端的连接线程管理
            ManageClientConnectServerThread.removeClientConnectServerThread(u.getUserId());
            System.exit(0); //结束进程
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getUserState(){
        return u.isLogin();
    }
    public String registerUser(String newUserId, String newPwd) {
        String result = MessageType.MESSAGE_REGISTER_FAIL;

        // 创建新的 User 对象
        u = new User();
        u.setUserId(newUserId);
        u.setPasswd(newPwd);

        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_REGISTER);
        message.setSender(newUserId);
        // Manually serialize User as a String
        String userString = newUserId + "," + newPwd;
        message.setContent(userString);

        try {
            // 连接到服务器，发送新的 User 对象
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            // 发送 Message 对象
            oos.writeObject(message);

            // 读取来自服务器的注册结果
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Message ms = (Message) ois.readObject();

            if (ms.getMesType().equals(MessageType.MESSAGE_REGISTER_SUCCEED)) {
                result = MessageType.MESSAGE_REGISTER_SUCCEED;
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }
}
