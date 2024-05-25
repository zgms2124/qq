package com.zgms.common;

/**
 * Java Project
 *
 * @Author: 子庚木上
 * @Date: 2024/5/6
 * @Description: 表示消息类型的常量接口
 * @version: 1.0
 */
public interface MessageType {

    // 登录相关消息类型
    String MESSAGE_LOGIN = "13"; // 登录请求
    String MESSAGE_LOGIN_SUCCEED = "1"; // 表示登录成功
    String MESSAGE_LOGIN_FAIL = "2"; // 表示登录失败
    String MESSAGE_IS_LOGIN = "9"; // 是否登录

    // 注册相关消息类型
    String MESSAGE_REGISTER = "12"; // 注册请求
    String MESSAGE_REGISTER_SUCCEED = "10"; // 表示注册成功
    String MESSAGE_REGISTER_FAIL = "11"; // 表示注册失败

    // 用户相关消息类型
    String MESSAGE_GET_ONLINE_FRIEND = "4"; // 要求返回在线用户列表
    String MESSAGE_RET_ONLINE_FRIEND = "5"; // 返回在线用户列表
    String MESSAGE_CLIENT_EXIT = "6"; // 客户端请求退出
    String MESSAGE_FORCE_OFFLINE = "25"; // 强制下线

    // 好友相关消息类型
    String MESSAGE_GET_FRIENDS = "19"; // 获取好友列表
    String MESSAGE_RET_FRIENDS = "20"; // 返回好友列表
    String MESSAGE_ADD_FRIEND = "21"; // 添加好友请求
    String MESSAGE_FRIEND_REQUEST = "22"; // 好友请求
    String MESSAGE_CONFIRM_FRIEND_REQUEST = "23"; // 确认好友请求
    String MESSAGE_CONFIRM_FRIEND_REQUEST_RESULT = "24"; // 确认好友请求结果

    // 群组相关消息类型
    String MESSAGE_GROUP_MES = "14"; // 群消息
    String MESSAGE_GET_GROUPS = "15"; // 获取群组列表
    String MESSAGE_RET_GROUPS = "16"; // 返回群组列表
    String MESSAGE_GET_GROUP_MEMBERS = "17"; // 获取群成员
    String MESSAGE_RET_GROUP_MEMBERS = "18"; // 返回群成员
    String MESSAGE_FORCE_DISSOLVE_GROUP = "26"; // 强制解散群组

    // 普通和文件消息类型
    String MESSAGE_COMM_MES = "3"; // 普通信息包
    String MESSAGE_TO_ALL_MES = "7"; // 广播消息
    String MESSAGE_FILE_MES = "8"; // 文件消息（发送文件）

    String TEST = "-1"; // 文件消息（发送文件）
}
