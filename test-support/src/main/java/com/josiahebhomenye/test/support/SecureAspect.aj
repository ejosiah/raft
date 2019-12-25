package com.josiahebhomenye.test.support;

public aspect SecureAspect {
    private Authenticator authenticator = new Authenticator();

    pointcut secureAccess() : execution(* MessageCommunicator.deliver(..));

    before() : secureAccess(){
        System.out.println("Checking user authentication");
        authenticator.authenticate();
    }
}
