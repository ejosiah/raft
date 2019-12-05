package com.josiahebhomenye.raft.server.core;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionHelper {

    @SneakyThrows
    public static <T> void replaceStaticField(String fieldName, Class<T> clazz, Object value){
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        int mod = field.getModifiers();
        if(Modifier.isFinal(mod)){
            Field modField = field.getClass().getDeclaredField("modifiers");
            modField.setAccessible(true);
            modField.set(field, mod & ~ Modifier.FINAL);
        }

        field.set(clazz, value);
    }
}
