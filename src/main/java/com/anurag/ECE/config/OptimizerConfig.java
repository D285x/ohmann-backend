package com.anurag.ECE.config;

import com.anurag.ECE.physics.AscentOptimizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread pool shared by all trajectory optimizations. */
@Configuration
public class OptimizerConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService optimizerPool(@Value("${ohmann.optimizer.threads:4}") int threads) {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "trajectory-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public AscentOptimizer ascentOptimizer(ExecutorService optimizerPool) {
        return new AscentOptimizer(optimizerPool);
    }
}
