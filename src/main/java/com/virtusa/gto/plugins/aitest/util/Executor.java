package com.virtusa.gto.plugins.aitest.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author rpsperera on 9/3/2018
 */
public class Executor {

    public static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
}
