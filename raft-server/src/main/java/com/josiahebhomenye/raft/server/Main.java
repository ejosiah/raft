package com.josiahebhomenye.raft.server;

import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

    public static void main(String[] args) {
       ServerConfig config = new ServerConfig(ConfigFactory.load());
       Node node = new Node(config);
       node.start();

       Runtime.getRuntime().addShutdownHook(new Thread(() -> {
           try {
               node.stop().get();
           }catch (Exception ex){
               log.error(String.format("Error encountered trying to stop %s", node), ex);
           }
       }));
    }
}
