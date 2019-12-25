package com.josiahebhomenye.experiments.profile;

import com.josiahebhomenye.test.support.profile.Profiler;
import com.josiahebhomenye.test.support.profile.Task;

import java.util.Arrays;
import java.util.Random;

public class StreamReduce implements Task {

    Random random = new Random();
    int[] ints = new int[50000];

    boolean setup;

    @Override
    public void setup() {
        if(!setup) {
            for (int i = 0; i < ints.length; i++) {
                ints[i] = random.nextInt();
            }
            setup = true;
        }
    }

    @Override
    public String description() {
        return "Stream Reduce";
    }

    @Override
    public Object run() {
        return Arrays.stream(ints).reduce(Integer.MIN_VALUE, Math::max);

    }

    @Override
    public void tearDown() {

    }

    public static void main(String[] args) throws Exception {
        StreamReduce task = new StreamReduce();
        Profiler profiler = new Profiler(task, 1000000, 10000);
        Profiler.Result result = profiler.run();

        System.out.printf("Stream reduce: %s\n", result);

//        profiler = new Profiler(new ForLoopReduce(), 1000000, 10000);
//
//        result = profiler.run();
//
//        System.out.printf("For loop reduce: %s\n", result);
    }

//    public static class ForLoopReduce extends StreamReduce{
//        @Override
//        public Object run() {
//            int res = Integer.MIN_VALUE;
//            int length = ints.length;
//            for(int i = 0; i < length; i++){
//                res = Math.max(res, ints[i]);
//            }
//            return res;
//        }
//
//        @Override
//        public String description() {
//            return "For loop reduce";
//        }
//    }
}
