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
        try {

            Optional<Method> method = getMethod(target, methodName, args);
            log.debug("invoking {}.{}({})", target, methodName, Arrays.toString(args));
            return  method.map(m -> invoke(m, target, args));
        }catch (Exception ex){
            log.warn("exception [{}] encountered trying to invoke {}.{}({}}", ex.getCause(), target, methodName, Arrays.toString(args));
            throw ex;
        }
    }

    private static Optional<Method> getMethod(Object target, String methodName, Object...args){
        Class<?> clazz = target.getClass();
        Class<?>[] params = new Class[args.length];
        IntStream.range(0, args.length).forEach(i -> params[i] = args[i].getClass());

        try {
            return  Optional.of(clazz.getMethod(methodName, params));
        } catch (NoSuchMethodException e) {

            Optional<Method> res = Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .filter(m -> m.getParameterCount() == params.length)
                .filter( m -> {
                    Class<?>[] mParams = m.getParameterTypes();
                    for(int i = 0; i < params.length; i++){
                        if(!mParams[i].isAssignableFrom(params[i])) return false;
                    }
                    return true;
                }).findFirst();

            if(!res.isPresent()){
                log.debug("method {}({}) not found on target {}", methodName, Arrays.toString(args), target);
            }

            return  res;
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object target, Object...args){
        Object res =  method.invoke(target, args);
        return res != null ? (T)res : (T)Void.instance;
    }
}
