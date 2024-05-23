package com.zgms.QQServer.server;



import com.zgms.common.Message;
import com.zgms.common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：该类的一个对象和某个客户端保持通信
 * @version: 1.0
 */
public class ServerConnectClientThread extends Thread {

    private Socket socket;
    private String userId;//连接到服务端的用户id

    public ServerConnectClientThread(Socket socket, String userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() { //这里线程处于run的状态，可以发送/接收消息

        while (true) {
            try {
                System.out.println("服务端和客户端" + userId + " 保持通信，读取数据...");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) ois.readObject();
                //后面会使用message, 根据message的类型，做相应的业务处理
                if (message.getMesType().equals(MessageType.MESSAGE_GET_ONLINE_FRIEND)) {
                    //客户端要在线用户列表
                    /*
                    在线用户列表形式 100  200  紫霞仙子
                     */
                    System.out.println(message.getSender() + " 要在线用户列表");
                    String onlineUser = ManageClientThreads.getOnlineUser();
                    //返回message
                    //构建一个Message 对象，返回给客户端
                    Message message2 = new Message();
                    message2.setMesType(MessageType.MESSAGE_RET_ONLINE_FRIEND);
                    message2.setContent(onlineUser);
                    message2.setGetter(message.getSender());
                    //返回给客户端
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(message2);

                } else if (message.getMesType().equals(MessageType.MESSAGE_COMM_MES)) {
                    //根据message获取getter id, 然后在得到对应先线程
                    ServerConnectClientThread serverConnectClientThread =
                            ManageClientThreads.getServerConnectClientThread(message.getGetter());
                    //得到对应socket的对象输出流，将message对象转发给指定的客户端
                    ObjectOutputStream oos =
                            new ObjectOutputStream(serverConnectClientThread.getSocket().getOutputStream());
                    oos.writeObject(message);//转发，提示如果客户不在线，可以保存到数据库，这样就可以实现离线留言

                } else if (message.getMesType().equals(MessageType.MESSAGE_TO_ALL_MES)) {
                    //需要遍历 管理线程的集合，把所有的线程的socket得到，然后把message进行转发即可
                    HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();

                    Iterator<String> iterator = hm.keySet().iterator();
                    while (iterator.hasNext()) {

                        //取出在线用户id
                        String onLineUserId = iterator.next().toString();

                        if (!onLineUserId.equals(message.getSender())) {//排除群发消息的这个用户

                            //进行转发message
                            ObjectOutputStream oos =
                                    new ObjectOutputStream(hm.get(onLineUserId).getSocket().getOutputStream());
                            oos.writeObject(message);
                        }

                    }

                }
                else if (message.getMesType().equals(MessageType.MESSAGE_GET_GROUPS)) {
                    // 客户端要求获取分组列表
                    System.out.println("客户端 " + userId + " 请求分组列表");
                    // 假设我们已经有一个方法来获取所有分组信息
                    String groupsInfo = ManageClientThreads.getGroups();
                    // 构建返回给客户端的消息对象
                    Message responseMessage = new Message();
                    responseMessage.setMesType(MessageType.MESSAGE_RET_GROUPS);
                    responseMessage.setContent(groupsInfo);
                    responseMessage.setGetter(message.getSender());
                    // 发送分组信息回客户端
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(responseMessage);

                } else if (message.getMesType().equals(MessageType.MESSAGE_GROUP_MES)) {
                    // 客户端发送的是一个组消息，格式为 "groupId:messageContent"
                    String[] parts = message.getContent().split(":", 2);
                    if (parts.length == 2) {
                        String groupId = parts[0]; // 获取组ID
                        String messageContent = parts[1]; // 获取消息内容

                        System.out.println("客户端 " + userId + " 发送消息到分组 " + groupId);
                        // 获取特定分组所有成员的ID，您需要在ManageClientThreads中实现getGroupMembers方法
                        String[] groupMembers = ManageClientThreads.getGroupMembers(groupId);
                        if (groupMembers != null) {
                            for (String memberId : groupMembers) {
                                // 确保不向消息的发送者自己发送消息
                                if (!memberId.equals(userId)) {
                                    ServerConnectClientThread memberThread = ManageClientThreads.getServerConnectClientThread(memberId);
                                    // 检查成员是否在线
                                    if (memberThread != null) {
                                        // 使用成员的ID和原始消息内容创建一个新的 Message 对象，以便个别发送给每一个组成员
                                        Message groupMessage = new Message();
                                        groupMessage.setMesType(MessageType.MESSAGE_GROUP_MES); // 组消息类型
                                        groupMessage.setSender(userId); // 发送者ID
                                        groupMessage.setContent(messageContent); // 真实消息内容
                                        groupMessage.setGetter(memberId); // 接收者ID，重要用于客户端逻辑处理
                                        groupMessage.setSendTime(new Date().toString()); // 发送时间

                                        // 发送消息
                                        ObjectOutputStream oos = new ObjectOutputStream(memberThread.getSocket().getOutputStream());
                                        oos.writeObject(groupMessage);
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println("组消息格式错误");
                    }
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_FILE_MES)) {
                    //根据getter id 获取到对应的线程，将message对象转发
                    ObjectOutputStream oos =
                            new ObjectOutputStream(ManageClientThreads.getServerConnectClientThread(message.getGetter()).getSocket().getOutputStream());
                    //转发
                    oos.writeObject(message);
                } else if (message.getMesType().equals(MessageType.MESSAGE_CLIENT_EXIT)) {//客户端退出

                    System.out.println(message.getSender() + " 退出");
                    //将这个客户端对应线程，从集合删除.
                    ManageClientThreads.removeServerConnectClientThread(message.getSender());
                    QQServer.outlogin(message.getSender());
                    socket.close();//关闭连接
                    //退出线程
                    break;

                } else {
                    System.out.println("其他类型的message , 暂时不处理");
                }
            } catch (SocketException e) {
                System.out.println("客户端" + userId + " 异常断开连接。");
                closeAndLogout();
                break;
            } catch (IOException e) {
                System.out.println("IO异常，客户端" + userId + " 可能关闭了连接。");
                closeAndLogout();
                break;
            } catch (Exception e) {
                System.out.println("发生异常");
                e.printStackTrace();
            } finally {
                // 在这里不进行最终的资源清理，因为closeAndLogout已经处理
            }
        }
    }
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                // 检查socket是否打开后再尝试关闭输入输出流
                try {
                    socket.shutdownInput(); // 关闭输入流
                } catch (IOException e) {
                    System.out.println("关闭输入流时发生错误: " + e.getMessage());
                }
                try {
                    socket.shutdownOutput(); // 关闭输出流
                } catch (IOException e) {
                    System.out.println("关闭输出流时发生错误: " + e.getMessage());
                }
                socket.close(); // 最后关闭Socket本身
            }
            System.out.println("与用户: " + userId + " 的连接已经关闭。");
        } catch (Exception e) {
            System.out.println("关闭与用户: " + userId + " 的连接时发生异常");
            e.printStackTrace();
        }
    }
    public void closeAndLogout() {
        try {
            // 首先关闭资源
            close();
            // 然后尝试重置用户状态
            QQServer.outlogin(userId); // 假定这个方法会重置用户的登录状态
            // 从在线用户列表移除用户
            ManageClientThreads.removeServerConnectClientThread(userId);
        } catch (Exception e) {
            System.out.println("在尝试关闭连接和登出时发生异常");
            e.printStackTrace();
        }
    }

}
