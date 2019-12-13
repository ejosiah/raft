package com.josiahebhomenye.raft;

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

    public static Acknowledgement successful(){
        return new Acknowledgement(true, "");
    }

    public static Acknowledgement failure(String reason){
        return new Acknowledgement(false, reason);
    }
}
