package com.josiahebhomenye.raft.server.util;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class DynamicTest {

    private Foo foo = new Foo();

    @Test
    public void dynamically_invoke_method_with_no_arguments(){
        Optional<String> res = Dynamic.invoke(foo, "bar");
        assertEquals(Optional.of("bar"), res);
    }

    @Test
    public void dynamically_invoke_single_argument(){
        Optional<String> res = Dynamic.invoke(foo, "bar", "hello");
        assertEquals( Optional.of("barhello"), res);
    }

    @Test
    public void dynamically_invoke_multiple_argument_method(){
        Optional<String> res = Dynamic.invoke(foo, "bar", "hello", 11);
        assertEquals(Optional.of("bar_hello_11"), res);
    }

    @Test
    public void dynamically_invoking_undefined_method_should_return_nothing(){
        Optional<?> res = Dynamic.invoke(foo, "undefined");

        assertEquals(Optional.empty(), res);
    }

    class Foo {

        public String bar(String param){
            return "bar" + param;
        }

        public String bar(String param0, Integer param1){
            return String.format("%s_%s_%s", "bar", param0, param1);
        }

        public String bar(){
            return "bar";
        }
    }
}