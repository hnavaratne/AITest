package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

public interface Comment extends Entity {

    void setMessage(String message);

    String getMessage();

    void setDateAndTime(String dateandtime);

    String getDateAndTime();

    void setUser(String id);

    String getUser();

    void setQuestionId(String id);

    String getQuestionId();

}
