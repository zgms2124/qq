package com.zgms.QQServer.server;


import com.zgms.QQServer.MessageServerUI;
import com.zgms.common.Constants;
import com.zgms.common.Message;
import com.zgms.common.MessageType;
import com.zgms.common.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：这是服务器, 在监听9999，等待客户端的连接，并保持通信
 * @version: 1.0
 */
public class QQServer {

    private ServerSocket ss = null;
    //创建一个集合，存放多个用户，如果是这些用户登录，就认为是合法
    //这里我们也可以使用 ConcurrentHashMap, 可以处理并发的集合，没有线程安全
    //HashMap 没有处理线程安全，因此在多线程情况下是不安全
    //ConcurrentHashMap 处理的线程安全,即线程同步处理, 在多线程情况下是安全
    private static ConcurrentHashMap<String, User> validUsers = new ConcurrentHashMap<>();
    private static String[] groupInfo ;
    private static String[] friendsInfo;

    static { //在静态代码块，初始化 validUsers

        loadUserData();
        loadGroupData();
        loadFriendsData();

    }



    public static void loadUserData(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.USER_DATA_FILE));
            String line = reader.readLine();

            while (line != null) {
                String[] userInfo = line.split(",");
                validUsers.put(userInfo[0], new User(userInfo[0], userInfo[1]));
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadGroupData() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.GROUP_DATA_FILE));
            String line = reader.readLine();
            while (line != null) {
                groupInfo = line.split(",");
                if (groupInfo.length > 1) {
                    // 第一个元素是组ID，后面的都是成员ID
                    String[] members = Arrays.copyOfRange(groupInfo, 1, groupInfo.length);
                    ManageClientThreads.addGroup(groupInfo[0], members);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    private static void loadFriendsData() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.FRIENDS_DATA_FILE)); // 需要替换为实际好友信息文件路径
            String line = reader.readLine();

            while (line != null) {
                friendsInfo = line.split(",");
                if (friendsInfo.length > 0) {
                    // 第一个元素是用户ID，后面的都是朋友ID
                    String[] friends = Arrays.copyOfRange(friendsInfo, 1, friendsInfo.length);
                    ManageClientThreads.addFriends(friendsInfo[0], friends);
                }
                line = reader.readLine();
            }
            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void outlogin(String sender) {
        User user=validUsers.get(sender);
        user.setLogin(Constants.OUT_LOGIN);
    }

    //验证用户是否有效的方法
    private String checkUser(String userId, String passwd) {
        User user = validUsers.get(userId);
        //过关的验证方式
        if(user == null) {//说明userId没有存在validUsers 的key中
            return  MessageType.MESSAGE_LOGIN_FAIL;
        }
        if(!user.getPasswd().equals(passwd)) {//userId正确，但是密码错误
            return MessageType.MESSAGE_LOGIN_FAIL;
        }
        if(user.isLogin()) {
            return MessageType.MESSAGE_IS_LOGIN;
        }
        user.setLogin(Constants.IS_LOGIN);
        return  MessageType.MESSAGE_LOGIN_SUCCEED;
    }

    public QQServer() {
        //注意：端口可以写在配置文件.
        try {
            System.out.println("服务端在9999端口监听...");
            //启动推送新闻的线程
            new Thread(new MessageServerService()).start();
            MessageServerUI serverUi = new MessageServerUI();
            serverUi.setVisible(true);
            ss = new ServerSocket(9999);

            while (true) { //当和某个客户端连接后，会继续监听, 因此while
                Socket socket = ss.accept();
                ObjectInputStream ois =
                        new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos =
                        new ObjectOutputStream(socket.getOutputStream());

                //读取客户端发送过来的 Message 对象
                Message message = (Message) ois.readObject();

                //从Message对象中得到User对象
                String[] userStr = message.getContent().split(",");
                User u = new User(userStr[0], userStr[1]);

                //创建一个Message对象，准备回复客户端
                Message messageToClient = new Message();
                //验证用户 方法
                if (MessageType.MESSAGE_LOGIN.equals(message.getMesType())) {//登录请求
                    String result = checkUser(u.getUserId(), u.getPasswd());
                    if (MessageType.MESSAGE_LOGIN_SUCCEED.equals(result)) {//登录通过
                        message.setMesType(MessageType.MESSAGE_LOGIN_SUCCEED);
                        //将message对象回复客户端
                        oos.writeObject(message);
                        //创建一个线程，和客户端保持通信, 该线程需要持有socket对象
                        ServerConnectClientThread serverConnectClientThread =
                                new ServerConnectClientThread(socket, u.getUserId());
                        //启动该线程
                        serverConnectClientThread.start();
                        //把该线程对象，放入到一个集合中，进行管理.
                        ManageClientThreads.addClientThread(u.getUserId(), serverConnectClientThread);

                    } else if (MessageType.MESSAGE_IS_LOGIN.equals(result)) {
                        System.out.println("用户 id=" + u.getUserId() + " 已经登录，请勿重复登录");
                        message.setMesType(MessageType.MESSAGE_IS_LOGIN);
                        oos.writeObject(message);
                        //关闭socket
                        socket.close();
                    } else { // 登录失败
                        System.out.println("用户 id=" + u.getUserId() + " pwd=" + u.getPasswd() + " 验证失败");
                        message.setMesType(MessageType.MESSAGE_LOGIN_FAIL);
                        oos.writeObject(message);
                        //关闭socket
                        socket.close();
                    }
                }
                else if (MessageType.MESSAGE_REGISTER.equals(message.getMesType())) {//注册请求
                    String result = registerUser(u.getUserId(), u.getPasswd());
                    if (MessageType.MESSAGE_REGISTER_SUCCEED.equals(result)) {//注册成功
                        messageToClient.setMesType(MessageType.MESSAGE_REGISTER_SUCCEED);
                        oos.writeObject(messageToClient);
                    } else { // 注册失败
                        System.out.println("用户 id=" + u.getUserId() + " pwd=" + u.getPasswd() + " 注册失败");
                        messageToClient.setMesType(MessageType.MESSAGE_REGISTER_FAIL);
                        oos.writeObject(messageToClient);
                        //关闭socket
                        socket.close();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            //如果服务器退出了while，说明服务器端不在监听，因此关闭ServerSocket
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String registerUser(String userId, String passwd) {
        if (!validUsers.containsKey(userId)) {
            validUsers.put(userId, new User(userId, passwd));
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.USER_DATA_FILE, true));
                writer.write(userId + "," + passwd + "\n");
                writer.close();
                System.out.println("用户 id=" + userId + " 注册成功");
            } catch(IOException e) {
                e.printStackTrace();
            }
            return MessageType.MESSAGE_REGISTER_SUCCEED;
        } else {
            return MessageType.MESSAGE_REGISTER_FAIL;
        }
    }
}
