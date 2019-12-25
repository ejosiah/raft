package com.josiahebhomenye.test.support.profile;

public interface Task {

    String description();

    void setup();

    Object run();

    void tearDown();
}
