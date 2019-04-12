package com.virtusa.gto.plugins.aitest.entity;

import net.java.ao.Entity;

/**
 * @author RPSPERERA on 6/13/2018
 */
public interface RequirementClarity extends Entity {

    String getUserId();

    void setUserId(String userId);

    String getRating();

    void setRating(String rating);

    String getIssueKey();

    void setIssueKey(String issueKey);

    String getCount();

    void setCount(String count);

    String getTotalRating();

    void setTotalRating(String totalRating);
}
