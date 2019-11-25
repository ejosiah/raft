package com.josiahebhomenye.raft.comand;

import lombok.Getter;

public enum MessageType {

    JSON(1);

    MessageType(int value){
        this.value = value;
    }

    @Getter
    int value;
}
