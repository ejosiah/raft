package com.josiahebhomenye.raft.server.util;

import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.IntStream;

public class Dynamic {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> invoke(Object target, String methodName, Object...args){
        Class<T> clazz = (Class<T>) target.getClass();
        Method method;
        try {
            Class[] params = new Class[args.length];
            IntStream.range(0, args.length).forEach(i -> params[i] = args[i].getClass());
            method = clazz.getMethod(methodName, params);
        }catch (NoSuchMethodException ex){
            return Optional.empty();
        }
        return Optional.of((T)method.invoke(target, args));
    }
}
