package com.josiahebhomenye.raft;

public enum  Environment {
    TEST, DEV, PROD;

    public static boolean isTest(){
        return CURRENT == TEST;
    }

    public static Environment get(){
        String env = System.getProperty("env", "PROD").toUpperCase();
        return valueOf(env);
    }

    public static final Environment CURRENT = get();

    public static void main(String[] args) {
        System.out.println(Environment.CURRENT);
    }
}
