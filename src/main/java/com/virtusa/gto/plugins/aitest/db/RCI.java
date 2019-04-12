package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.RequirementClarity;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author RPSPERERA on 6/13/2018
 */
@Scanned
public class RCI {
    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public RCI(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public RequirementClarity upsert(String issueId, String rating) {
        return activeObjects.executeInTransaction(() -> {
            List<RequirementClarity> requirementClarityList = Arrays.asList(activeObjects.find(RequirementClarity.class, "ISSUE_KEY = ?", String.valueOf(issueId)));
            if (requirementClarityList.size() > 0) {
                RequirementClarity requirementClarity = requirementClarityList.get(0);
                int count = Integer.parseInt(requirementClarity.getCount());
                int finalCount = count + 1;
                int totalRating = Integer.parseInt(requirementClarity.getTotalRating());
                int finalTotalRating = totalRating + Integer.parseInt(rating);
                int newRating = finalTotalRating / finalCount;
                requirementClarity.setCount(String.valueOf(finalCount));
                requirementClarity.setTotalRating(String.valueOf(finalTotalRating));
                requirementClarity.setRating(String.valueOf(newRating));
                requirementClarity.save();
                return requirementClarity;
            } else {
                RequirementClarity requirementClarity = activeObjects.create(RequirementClarity.class);
                requirementClarity.setIssueKey(issueId);
                requirementClarity.setRating(rating);
                requirementClarity.setCount("1");
                requirementClarity.setTotalRating(rating);
                requirementClarity.save();
                return requirementClarity;
            }
        });
    }


    public String getAverage(String issueId) {
        return activeObjects.executeInTransaction(() -> {
            List<RequirementClarity> requirementClarityList = Arrays.asList(activeObjects.find(RequirementClarity.class, "ISSUE_KEY = ?", String.valueOf(issueId)));
            if (requirementClarityList.size() > 0) {
                return requirementClarityList.get(0).getRating();
            } else {
                return "0";
            }

        });
    }
}
