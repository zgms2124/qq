package com.zgms.QQServer.server;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：该类用于管理和客户端通信的线程
 * @version: 1.0
 */
public class ManageClientThreads {
    private static HashMap<String, ServerConnectClientThread> hm = new HashMap<>();

    //返回 hm
    public static HashMap<String, ServerConnectClientThread> getHm() {
        return hm;
    }
    // 我们假设有一个静态的 HashMap 来存储组信息，键是组ID，值是组成员列表
    private static HashMap<String, String[]> groups = new HashMap<String, String[]>();

    // 我们假设有一个静态的 HashMap 来存储朋友信息，键是用户ID，值是朋友列表
    private static HashMap<String, String[]> friends = new HashMap<String, String[]>();

    public static HashMap<String, String[]> getGroups() {
        return groups;
    }

    public static void removeGroup(String groupId) {
        if (groups.containsKey(groupId)) {
            groups.remove(groupId);
            System.out.println("群组 " + groupId + " 已被成功移除。");
        } else {
            System.out.println("不存在群组 " + groupId + "，无法移除。");
        }
    }

    // 添加朋友及其列表到集合中
    public static void addFriends(String userId, String[] friendList) {
        friends.put(userId, friendList);
    }



    // 获取某个用户的所有朋友
    public static String[] getFriends(String userId) {
        return friends.get(userId); // 这将返回朋友的字符串数组，如果用户ID不存在，则会返回null
    }

    //添加线程对象到 hm 集合
    public static void addClientThread(String userId, ServerConnectClientThread serverConnectClientThread) {

        hm.put(userId, serverConnectClientThread);

    }

    //根据userId 返回ServerConnectClientThread线程
    public static ServerConnectClientThread getServerConnectClientThread(String userId) {

        return hm.get(userId);
    }

    //增加一个方法，从集合中，移除某个线程对象
    public static void removeServerConnectClientThread(String userId) {
        ServerConnectClientThread thread = hm.get(userId);
        if (thread != null) {
            // 调用线程的关闭方法
            thread.close();
            // 从HashMap中移除
            hm.remove(userId);
            // 可选：打印日志或通知有线程被移除和关闭
            System.out.println("用户 " + userId + " 对应的线程已经被移除并关闭。");
        }
    }

    //这里编写方法，可以返回在线用户列表
    public static String getOnlineUser() {
        //集合遍历 ，遍历 hashmap的key
        Iterator<String> iterator = hm.keySet().iterator();
        String onlineUserList = "";
        while (iterator.hasNext()) {
            onlineUserList += iterator.next().toString() + " ";
        }
        return  onlineUserList;
    }

    // 这个方法用来获取全部分组的ID列表
    public static String getGroupsName() {
        return String.join(" ", groups.keySet());
    }

    // 添加组及其成员到集合中
    public static void addGroup(String groupId, String[] members) {
        groups.put(groupId, members);
    }

    // 获取某个组的所有成员
    public static String[] getGroupMembers(String groupId) {
        return groups.get(groupId); // 这将返回组成员的字符串数组，如果组ID不存在，则会返回null
    }

    //用于更新好友列表
    public static void updateFriends(String userId, String newFriendId) {
        // 更新 userId 的好友列表
        updateFriendList(userId, newFriendId);
        // 更新 newFriendId 的好友列表
        updateFriendList(newFriendId, userId);
    }

    // 更新单个用户的好友列表的方法
    public static void updateFriendList(String userId, String newFriendId) {
        // 从内存中获取已有的好友列表
        String[] currentFriends = friends.get(userId);

        //创建一个新列表用于存储和更新朋友ID
        List<String> updatedFriends = new ArrayList<>();
        if (currentFriends != null) {
            for (String friendId : currentFriends) {
                updatedFriends.add(friendId);
            }
        }
        // 在列表中添加新朋友的ID
        updatedFriends.add(newFriendId);

        // 将更新后的朋友列表保存到内存中
        friends.put(userId, updatedFriends.toArray(new String[0]));

        // 现在更新 friends.txt 文件
        try {
            //打开文件并添加新的朋友信息
            FileWriter writer = new FileWriter("E:\\java\\projectpractice\\qq源码\\qq\\src\\com\\zgms\\QQServer\\server\\friends.txt", true);
            writer.write(userId + "," + newFriendId + "\n"); //假设每行包含两个ID，空格分隔
            writer.close();
        } catch (IOException e) {
            System.err.println("在更新 friends.txt 文件时发生错误");
            e.printStackTrace();
        }
    }
}
