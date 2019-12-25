package com.josiahebhomenye.test.support;

public class MessageCommunicator {

    public void deliver(String message){
        System.out.println(message);
    }

    public void deliver(String person, String message){
        System.out.printf("%s, %s", person, message);
    }
}
