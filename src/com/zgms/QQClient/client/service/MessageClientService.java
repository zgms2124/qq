package com.zgms.QQClient.client.service;



import com.zgms.common.Message;
import com.zgms.common.MessageType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;

/**
 * Java Project
 *
 * @Author： 子庚木上
 * @Date： 2024/5/6 - 05 - 06 - 2024
 * @Description：该类/对象，提供和消息相关的服务方法
 * @version: 1.0
 */
public class MessageClientService {


    /**
     * @param content  内容
     * @param senderId 发送者
     */
    public void sendMessageToAll(String content, String senderId) {
        //构建message
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_TO_ALL_MES);//群发消息这种类型
        message.setSender(senderId);
        message.setContent(content);
        message.setSendTime(new Date().toString());//发送时间设置到message对象
        System.out.println(senderId + " 对大家说 " + content);
        //发送给服务端

        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(senderId).getSocket().getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param content  内容
     * @param senderId 发送用户id
     * @param getterId 接收用户id
     */
    public void sendMessageToOne(String content, String senderId, String getterId) {
        //构建message
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_COMM_MES);//普通的聊天消息这种类型
        message.setSender(senderId);
        message.setGetter(getterId);
        message.setContent(content);
        message.setSendTime(new Date().toString());//发送时间设置到message对象
        System.out.println(senderId + " 对 " + getterId + " 说 " + content);
        //发送给服务端

        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(senderId).getSocket().getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * 使用特定的字符串格式发送组消息。
     * 消息格式为 "groupId:messageContent"，服务端需按此格式解析。
     * @param content  消息内容
     * @param userId   发送用户ID
     * @param groupId  组ID
     */
    public void sendGroupMessage(String content, String userId, String groupId) {
        // 将组ID和消息内容合并为一个字符串，格式为 "groupId:messageContent"
        String groupMessageContent = groupId + ":" + content;

        // 创建一个新的 Message 实例
        Message message = new Message();
        message.setMesType(MessageType.MESSAGE_GROUP_MES); // 设置消息类型为组消息
        message.setSender(userId); // 设置发送者ID
        message.setContent(groupMessageContent); // 设置整合后的消息内容
        message.setSendTime(new Date().toString()); // 设置发送时间

        // 在控制台打印出发送的消息（可选）
        System.out.println(userId +  "说: " + content+"(来自组："+groupId+")");

        // 发送消息给服务端
        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(ManageClientConnectServerThread.getClientConnectServerThread(userId).getSocket().getOutputStream());
            oos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
