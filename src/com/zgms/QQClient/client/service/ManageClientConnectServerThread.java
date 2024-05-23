package com.zgms.QQClient.client.service;

import java.util.HashMap;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：该类管理客户端连接到服务器端的线程的类
 * @version: 1.0
 */
public class ManageClientConnectServerThread {
    //我们把多个线程放入一个HashMap集合，key 就是用户id, value 就是线程
    private static HashMap<String, ClientConnectServerThread> hm = new HashMap<>();

    //将某个线程加入到集合
    public static void addClientConnectServerThread(String userId, ClientConnectServerThread clientConnectServerThread) {
        hm.put(userId, clientConnectServerThread);
    }
    //通过userId 可以得到对应线程
    public static ClientConnectServerThread getClientConnectServerThread(String userId) {
        return hm.get(userId);
    }

    // ManageClientConnectServerThread 类中
    public static void removeClientConnectServerThread(String userId) {
        hm.remove(userId);
        System.out.println("客户端线程 " + userId + " 已从管理器中移除.");
    }

}
