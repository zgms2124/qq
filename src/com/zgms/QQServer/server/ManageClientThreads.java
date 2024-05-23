package com.zgms.QQServer.server;

import java.util.HashMap;
import java.util.Iterator;

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
    public static String getGroups() {
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
}
