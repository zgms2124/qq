package com.zgms.ToView;

import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Random random = new Random();
        for (int i = 2; i <= 100000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("group").append(i);
            int randomNumCount = random.nextInt(3) + 1; // 生成1到3的随机数
            for (int j = 0; j < randomNumCount; j++) {
                sb.append(",");
                sb.append(random.nextInt(200000)); // 生成随机数
            }
            System.out.println(sb.toString());
        }
    }
}