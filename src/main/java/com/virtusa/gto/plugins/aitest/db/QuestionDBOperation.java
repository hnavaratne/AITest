package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.Question;
import com.virtusa.gto.plugins.aitest.entity.QuestionStatus;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionDBOperation {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public QuestionDBOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Question insert(String title, String description, Long issue, Long user) {
        return activeObjects.executeInTransaction(() -> {
            Question question = activeObjects.create(Question.class);
            question.setTitle(title);
            question.setDescription(description);
            question.setIssue(issue);
            question.setAssignTo(user);
            question.setStatus(QuestionStatus.OPEN.value());
            question.save();
            return question;
        });
    }

    public List<Question> get(Long issue) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(Question.class, "ISSUE = ?", issue)));
        } catch (ActiveObjectsException e) {
            return new ArrayList<>();
        }
    }

    public int getCount(Long issue) {
        try {
            Query query = Query.select().where("ISSUE = ?", issue);
            return activeObjects.count(Question.class, query);
//            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(Question.class, "ISSUE = ?", issue)));
        } catch (ActiveObjectsException e) {
            return -1;
        }
    }

    public boolean update(Long id, String description, String status, Long user) {
        return activeObjects.executeInTransaction(() -> {
            try {
                Question[] questions = activeObjects.find(Question.class, "ID = ?", id);
                for (Question question : questions) {
                    question.setAssignTo(user);
                    question.setDescription(description);
                    question.setStatus(status);
                    question.save();
                }
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

    public boolean delete(Long id) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(Question.class, "ID = ?", id);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }
}
