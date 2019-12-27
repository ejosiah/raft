package com.josiahebhomenye.raft.server.aspect;

public aspect NodeStateAspect{

    pointcut getNodeState() : execution(* com.josiahebhomenye.raft.server.core.NodeState.FOLLOWER(..));

    Object around() : getNodeState(){
        Object res = proceed();

        try {
            return res.getClass().newInstance();
        }catch (Exception ex){
            return res;
        }
    }
}