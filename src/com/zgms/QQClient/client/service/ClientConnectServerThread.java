package com.zgms.QQClient.client.service;



import com.zgms.QQServer.server.ManageClientThreads;
import com.zgms.ToView.MainChatWindow;
import com.zgms.common.Message;
import com.zgms.common.MessageType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：
 * @version: 1.0
 */
public class ClientConnectServerThread extends Thread {
    private MainChatWindow mainWindow;
    //该线程需要持有Socket
    private Socket socket;
    private UserClientService userClientService;

    public UserClientService getUserClientService() {
        return userClientService;
    }

    public void setUserClientService(UserClientService userClientService) {
        this.userClientService = userClientService;
    }

    public MainChatWindow getMainWindow() {
        return mainWindow;
    }

    public void setMainWindow(MainChatWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    //构造器可以接受一个Socket对象
    public ClientConnectServerThread(Socket socket, MainChatWindow window) {
        this.socket = socket;
        this.mainWindow = window;
        System.out.println("初始化"+mainWindow);
    }

    public ClientConnectServerThread(Socket socket) {
        this.socket = socket;
    }



    volatile boolean running = true;

    // ClientConnectServerThread 类中
    public void closeResources() {
        try {
            if(socket != null) {
                socket.close();
            }
            System.out.println("与服务器的连接已经关闭.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //
    @Override
    public void run() {
        //因为Thread需要在后台和服务器通信，因此我们while循环
        while (true) {

            try {
                if (socket.isClosed()) {
                    System.out.println("Socket has been closed. Exiting read thread.");
                    break;
                }
                System.out.println("客户端线程，等待从读取从服务器端发送的消息");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                //如果服务器没有发送Message对象,线程会阻塞在这里
                Message message = (Message) ois.readObject();
                userClientService.onServerResponse(message);
                //注意，后面我们需要去使用message
                //判断这个message类型，然后做相应的业务处理
                //如果是读取到的是 服务端返回的在线用户列表
                if (message.getMesType().equals(MessageType.MESSAGE_RET_ONLINE_FRIEND)) {
                    //取出在线列表信息，并显示
                    //规定
                    String[] onlineUsers = message.getContent().split(" ");
                    System.out.println(mainWindow);
                    mainWindow.updateUserList(onlineUsers);
                    System.out.println("\n=======当前在线用户列表========");
                    for (int i = 0; i < onlineUsers.length; i++) {
                        System.out.println("用户: " + onlineUsers[i]);
                    }

                }else if (message.getMesType().equals(MessageType.MESSAGE_FRIEND_REQUEST)) {
                    // 如果收到一个添加好友的请求
                    System.out.println("收到用户 " + message.getSender() + " 的好友请求");

                    // 提示用户是否接受好友请求，您可以使用一个弹窗
                    boolean acceptRequest = mainWindow.showFriendRequestDialog(message.getSender());

                    // 创建一个消息对象，保存是否接受好友请求的决定，并发送给服务器
                    Message addFriendConfirm = new Message();
                    addFriendConfirm.setMesType(MessageType.MESSAGE_CONFIRM_FRIEND_REQUEST);
                    addFriendConfirm.setSender(message.getGetter());
                    addFriendConfirm.setGetter(message.getSender());
                    if(acceptRequest){
                        addFriendConfirm.setContent("true");
                        System.out.println("您接受了" + message.getSender() + "的好友请求");
                        // 在UI界面显示对方接受了您的好友请求
                        mainWindow.appendMessage("您接受了" + message.getSender() + "的好友请求");

                    }
                    else{
                        addFriendConfirm.setContent("false");
                        System.out.println("您拒绝了" + message.getSender() + "的好友请求");
                        // 在UI界面显示对方接受了您的好友请求
                        mainWindow.appendMessage("您拒绝了" + message.getSender() + "的好友请求");
                    }

                    userClientService.sendMessage(addFriendConfirm);
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_CONFIRM_FRIEND_REQUEST_RESULT)) {
                    // 如果收到服务器发送的添加好友请求的结果
                    String result = message.getContent(); // 结果，"true" 或 "false"

                    if ("true".equals(result)) {
                        System.out.println("用户 " + message.getSender() + " 接受了您的好友请求");

                        // 在UI界面显示对方接受了您的好友请求
                        mainWindow.appendMessage("用户 " + message.getSender() + "接受了您的好友请求");


                    } else {
                        System.out.println("用户 " + message.getSender() + " 拒绝了您的好友请求");

                        // 在UI界面显示对方拒绝了您的好友请求
                        mainWindow.appendMessage("用户 " + message.getSender() + " 拒绝了您的好友请求");
                    }
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_RET_FRIENDS)) {
                    // 取出朋友列表信息，并更新到主聊天窗口
                    String[] friendsList = message.getContent().split(",");
                    // 假设 MainWindow 有一个方法 updateFriendsList 更新朋友列表
                    mainWindow.updateFriendsList(friendsList);
                    System.out.println("\n=======当前好友列表========");
                    for (String friend : friendsList) {
                        System.out.println("好友: " + friend);
                    }
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_COMM_MES)) {//普通的聊天消息
                    //把从服务器转发的消息，显示到控制台即可
                    // 当接收到普通聊天消息时
                    // 构建要显示的消息内容，格式为"发送者: 信息"
                    String msgToShow = message.getSender() + ": " + message.getContent();
                    // 调用 MainChatWindow 中的 appendMessage 方法将消息显示在聊天历史区域
                    mainWindow.appendMessage(msgToShow);
                    System.out.println("\n" + message.getSender()
                            + " 对 " + message.getGetter() + " 说: " + message.getContent());
                } else if (message.getMesType().equals(MessageType.MESSAGE_TO_ALL_MES)) {
                    //显示在客户端的控制台
                    // 当接收到普通聊天消息时
                    // 构建要显示的消息内容，格式为"发送者: 信息"
                    String msgToShow = message.getSender() + "（来自广播）: " + message.getContent();
                    // 调用 MainChatWindow 中的 appendMessage 方法将消息显示在聊天历史区域
                    mainWindow.appendMessage(msgToShow);
                    System.out.println("\n" + message.getSender() + " 对大家说: " + message.getContent());
                } else if (message.getMesType().equals(MessageType.MESSAGE_GROUP_MES)) {
                    // 接收到分组消息时
                    String msgToShow = message.getSender() + "（来自组 " + message.getGetter() + "）: " + message.getContent();
                    mainWindow.appendMessage(msgToShow);
                    System.out.println("\n" + message.getSender() + " 对组 " + message.getGetter() + " 说: " + message.getContent());
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_FILE_MES)) {//如果是文件消息
                    //让用户指定保存路径。。。
                    System.out.println("\n" + message.getSender() + " 给 " + message.getGetter()
                            + " 发文件: " + message.getSrc() + " 到我的电脑的目录 " + message.getDest());

                    //取出message的文件字节数组，通过文件输出流写出到磁盘
                    FileOutputStream fileOutputStream = new FileOutputStream(message.getDest(), true);
                    fileOutputStream.write(message.getFileBytes());
                    fileOutputStream.close();
                    System.out.println("\n 保存文件成功~");

                }else if (message.getMesType().equals(MessageType.MESSAGE_RET_GROUPS)) {
                    // 取出组列表信息，并更新到主聊天窗口
                    String[] groupList = message.getContent().split(" ");
                    // 假设 MainWindow 有一个方法 updateGroupList 更新组列表
                    mainWindow.updateGroupList(groupList);
                    System.out.println("\n=======当前群组列表========");
                    for (String group : groupList) {
                        System.out.println("群组: " + group);
                    }
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_RET_GROUP_MEMBERS)) {
                    // 取出组列表信息，并更新到主聊天窗口
                    String[] groupMenberList = message.getContent().split(",");
                    // 假设 MainWindow 有一个方法 updateGroupList 更新组列表
                    mainWindow.updateGroupMenbersList(groupMenberList);
                    System.out.println("\n=======当前群组成员列表========");
                    for (String group : groupMenberList) {
                        System.out.println("成员: " + group);
                    }
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_FORCE_OFFLINE)) {
                    // 如果收到强制下线的消息
                    System.out.println("您被强制下线");

                    // 在UI界面弹出一个提示框
                    mainWindow.offLine();

                    // 关闭相关资源并结束进程
                    this.closeResources();
                    ManageClientThreads.removeServerConnectClientThread(message.getGetter());
                    System.exit(0);
                }
                else if (message.getMesType().equals(MessageType.MESSAGE_FORCE_DISSOLVE_GROUP)) {
                    // 如果收到群组解散的消息
                    String groupId = message.getContent();

                    System.out.println("您所在的群组 " + groupId + " 被解散");

                    // 从UI中移除该群组
                    mainWindow.dissolveGroup(groupId);
                }
                else {
                    System.out.println("是其他类型的message, 暂时不处理....");
                }

            } catch (Exception e) {
                running = false; // 将循环控制标志设置为false
                if (!(e instanceof IOException)) {
                    e.printStackTrace();
                }
                // 在异常情况下关闭线程，并释放资源
                closeResources();
            }
        }
    }

    //为了更方便的得到Socket
    public Socket getSocket() {
        return socket;
    }
}
