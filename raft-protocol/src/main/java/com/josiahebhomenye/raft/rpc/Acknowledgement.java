package com.josiahebhomenye.raft.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class Acknowledgement {
    private boolean successful;
    private String message;
    private byte[] body;

    public static Acknowledgement successful(){
        return new Acknowledgement(true, "", new byte[0]);
    }

    public static Acknowledgement successful(byte[] body){
        return new Acknowledgement(true, "", body);
    }

    public static Acknowledgement failure(String reason){
        return new Acknowledgement(false, reason,  new byte[0]);
    }
}
