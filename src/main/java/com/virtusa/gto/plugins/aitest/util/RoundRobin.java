package com.virtusa.gto.plugins.aitest.util;

import java.util.Iterator;
import java.util.List;

public class RoundRobin {
    private Iterator<String> it;
    private List<String> list;

    public RoundRobin(List<String> list) {
        this.list = list;
        it = list.iterator();
    }

    public String next() {
        if (!it.hasNext()) {
            it = list.iterator();
        }
        return it.next();
    }

}
