package com.zgms.QQServer.server;



import com.zgms.common.Message;
import com.zgms.common.MessageType;
import com.zgms.common.Utility;

import java.io.*;
import java.util.*;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：
 * @version: 1.0
 */
public class MessageServerService implements Runnable {


    @Override
    public void run() {
        // 循环以便多次使用菜单
        while (true) {
            System.out.println("请选择服务器功能：\n1.对所有用户发送消息\n2.展示当前在线用户\n" +
                    "3.展示当前群组\n4.强制某个用户下线\n5.强制某个群组解散\n6.对某个用户发消息\n" +
                    "7.对某个群组发消息\n输入exit表示退出");

            String input = Utility.readString(100);

            if("exit".equals(input)) {
                break;
            }

            switch (input) {
                case "1":
                    // 对所有用户发送消息
                    broadcastMessage();
                    break;
                case "2":
                    // 展示当前在线用户
                    showOnlineUsers();
                    break;
                case "3":
                    // 展示当前群组
                    showCurrentGroups();
                    break;
                case "4":
                    // 强制某个用户下线
                    forceUserOffline();
                    break;
                case "5":
                    // 强制某个群组解散
                    forceGroupDissolve();
                    break;
                case "6":
                    // 对某个用户发消息
                    sendMessageToUser();
                    break;
                case "7":
                    // 对某个群组发消息
                    sendMessageToGroup();
                    break;
                default:
                    System.out.println("输入无效，请重新选择！");
                    break;
            }
        }
    }

    private void sendMessageToUser() {
        System.out.println("请输入接收者 ID：");
        String getter = Utility.readString(30);

        System.out.println("请输入消息内容：");
        String content = Utility.readString(500);

        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_COMM_MES);
        message.setSender("服务器");
        message.setGetter(getter);
        message.setContent(content);
        message.setSendTime(new Date().toString());

        try {
            ServerConnectClientThread thread = ManageClientThreads.getServerConnectClientThread(getter);
            if (thread != null) {
                ObjectOutputStream oos = new ObjectOutputStream(thread.getSocket().getOutputStream());
                oos.writeObject(message);
                System.out.println("消息已发送。");
            } else {
                System.out.println("用户 " + getter + " 当前不在线，消息未发送。");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToGroup() {
        System.out.println("请输入群 ID：");
        String groupId = Utility.readString(30);

        System.out.println("请输入消息内容：");
        String messageContent = Utility.readString(500);

        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_GROUP_MES);
        message.setSender("服务器");
        message.setGetter(groupId);
        message.setContent(messageContent);
        message.setSendTime(new Date().toString());

        // 获取群内所有在线用户
        String[] onlineGroupMembers = ManageClientThreads.getGroupMembers(groupId);

        // 对每个在线用户发送消息
        for (String userId : onlineGroupMembers) {
            try {
                ServerConnectClientThread clientThread = ManageClientThreads.getServerConnectClientThread(userId);
                if(clientThread!=null){
                    ObjectOutputStream oos = new ObjectOutputStream(clientThread.getSocket().getOutputStream());
                    oos.writeObject(message);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("群组消息已发送。");
    }

    private void forceUserOffline() {
        // 提示服务器管理员输入要强制下线的用户ID
        System.out.println("请输入要强制下线的用户ID:");
        String userId = Utility.readString(100);

        // 构造一个强制下线的消息，并发送给该用户
        Message offlineMsg = new Message();
        offlineMsg.setMesType(MessageType.MESSAGE_FORCE_OFFLINE);
        offlineMsg.setGetter(userId);

        try {
            ServerConnectClientThread clientThread = ManageClientThreads.getServerConnectClientThread(userId);
            if(clientThread!=null){
                ObjectOutputStream oos =
                        new ObjectOutputStream(clientThread.getSocket().getOutputStream());
                oos.writeObject(offlineMsg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("已向用户 " + userId + " 发送强制下线消息");
    }

    private void forceGroupDissolve() {
        // 提示服务器管理员输入要解散的群组ID
        System.out.println("请输入要解散的群组ID:");
        String groupId = Utility.readString(100);

        // 构造一个群解散的消息，并发送给每一个群组成员
        Message dissolveMsg = new Message();
        dissolveMsg.setMesType(MessageType.MESSAGE_FORCE_DISSOLVE_GROUP);
        dissolveMsg.setContent(groupId);

        // 你可能需要一个持有群组成员列表的类或者结构
        String[] groupMembers = ManageClientThreads.getGroupMembers(groupId);
        if(groupMembers==null){
            System.out.println(groupId+"群组不存在！");
            return;
        }
        for (String memberId : groupMembers) {
            try {
                ServerConnectClientThread clientThread = ManageClientThreads.getServerConnectClientThread(memberId);
                if(clientThread!=null){
                    dissolveMsg.setGetter(memberId);
                    ObjectOutputStream oos =
                            new ObjectOutputStream(clientThread.getSocket().getOutputStream());
                    oos.writeObject(dissolveMsg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        removeGroupFromFile(groupId);
        System.out.println("已向群组 " + groupId + " 的所有成员发送解散群组消息");
    }

    // 从文件中删除特定群组的方法
    private void removeGroupFromFile(String groupId) {
        File inputFile = new File("E:\\java\\projectpractice\\qq源码\\qq\\src\\com\\zgms\\QQServer\\server\\groups.txt");
        File tempFile = new File("E:\\java\\projectpractice\\qq源码\\qq\\src\\com\\zgms\\QQServer\\server\\tempGroups.txt");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                String trimmedLine = currentLine.trim();
                if (trimmedLine.startsWith(groupId + ",")) continue;
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();

            // 删除原始文件
            if (inputFile.delete()) {
                // 在原始文件被删除后重命名临时文件
                boolean isRenamed = tempFile.renameTo(inputFile);
                System.out.println(isRenamed ? "已成功删除群组 " + groupId : "删除群组失败");
                if(isRenamed){
                    ManageClientThreads.removeGroup(groupId);
                }
            } else {
                System.out.println("删除原始文件失败，无法重命名临时文件。");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void showOnlineUsers() {
        HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();
        System.out.println("\n=======当前在线用户列表========");
        for (String userID : hm.keySet()) {
            System.out.println("用户: " + userID);
        }
    }

    private void showCurrentGroups() {
        HashMap<String, String[]> hm = ManageClientThreads.getGroups();
        System.out.println("\n=======当前群组列表========");
        for (String groupID : hm.keySet()) {
            System.out.println("群组" + groupID+"中的成员:");
            for(String userName:hm.get(groupID)){
                System.out.println(userName);
            }
        }
    }

    private void broadcastMessage(){
        System.out.println("请输入服务器要推送的新闻/消息");
        String news = Utility.readString(100);
        //构建一个消息 , 群发消息
        Message message = new Message();
        message.setSender("服务器");
        message.setMesType(MessageType.MESSAGE_TO_ALL_MES);
        message.setContent(news);
        message.setSendTime(new Date().toString());
        System.out.println("服务器推送消息给所有人 说: " + news);

        //遍历当前所有的通信线程，得到socket,并发送message

        HashMap<String, ServerConnectClientThread> hm = ManageClientThreads.getHm();

        Iterator<String> iterator = hm.keySet().iterator();
        while (iterator.hasNext()) {
            String onLineUserId = iterator.next().toString();
            try {
                ObjectOutputStream oos =
                        new ObjectOutputStream(hm.get(onLineUserId).getSocket().getOutputStream());
                oos.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
