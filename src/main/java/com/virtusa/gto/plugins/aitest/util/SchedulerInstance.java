package com.virtusa.gto.plugins.aitest.util;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author RPSPERERA on 10/25/2017
 */
public class SchedulerInstance {

    private Scheduler scheduler;

    private static final Logger log = LoggerFactory.getLogger(SchedulerInstance.class);

    private SchedulerInstance() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            Runtime r1 = Runtime.getRuntime();
            r1.addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down scheduler.....");
                    scheduler.shutdown();
                    System.out.println("Scheduler terminated successfully.....");
                    resetInstance();
                } catch (SchedulerException e) {
                    log.error(e.getMessage(), e);
                }
            }));
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static SchedulerInstance getInstance() {
        return Holder.INSTANCE;
    }

    public Scheduler getSchedulerInstance() {
        return scheduler;
    }

    private static class Holder {
        private static final SchedulerInstance INSTANCE = new SchedulerInstance();
    }

    private void resetInstance() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
