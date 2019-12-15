package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RedirectCommand {
    private InetSocketAddress leaderId;
    private byte[] command;
}
