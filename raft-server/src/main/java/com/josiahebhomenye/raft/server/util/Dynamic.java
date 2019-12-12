package com.josiahebhomenye.raft.server.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
public class Dynamic {

    private static class Void{
        public static Void instance = new Void();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> invoke(Object target, String methodName, Object...args){
        Class<T> clazz = (Class<T>) target.getClass();
        Method method;
        try {
            Class<?>[] params = new Class[args.length];
            IntStream.range(0, args.length).forEach(i -> params[i] = args[i].getClass());
            method = clazz.getMethod(methodName, params);
            log.debug("invoking {}.{}({})", target.getClass().getSimpleName(), methodName, Arrays.toString(args));
            Object resp = method.invoke(target, args);
            return (resp == null ? Optional.of((T)Void.instance) : Optional.of((T)resp));
        }catch (NoSuchMethodException ex){
            log.debug("method {}({}) not found on target {}", methodName, Arrays.toString(args), target.getClass().getSimpleName());
            return Optional.empty();
        }catch (Exception ex){
            log.warn("exception encountered trying to invoke {}.{}({}}", target.getClass().getSimpleName(), methodName, Arrays.toString(args));
            throw ex;
        }
    }
}
