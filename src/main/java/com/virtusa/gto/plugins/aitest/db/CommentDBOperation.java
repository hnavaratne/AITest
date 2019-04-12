package com.virtusa.gto.plugins.aitest.db;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.virtusa.gto.plugins.aitest.entity.Comment;
import net.java.ao.ActiveObjectsException;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommentDBOperation {

    @JiraImport
    private final ActiveObjects activeObjects;

    @Inject
    public CommentDBOperation(ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public Comment insert(String message, String dateandtime, String userid, String questionid) {
        return activeObjects.executeInTransaction(() -> {
            Comment comment = activeObjects.create(Comment.class);
            comment.setMessage(message);
            comment.setDateAndTime(dateandtime);
            comment.setUser(userid);
            comment.setQuestionId(questionid);
            comment.save();
            return comment;
        });
    }

    public List<Comment> get(String questionId) {
        try {
            return activeObjects.executeInTransaction(() -> Arrays.asList(activeObjects.find(Comment.class, "QUESTION_ID = ?", questionId)));
        } catch (ActiveObjectsException ex) {
            return new ArrayList<>();
        }
    }

    public boolean delete(String questionId) {
        return activeObjects.executeInTransaction(() -> {
            try {
                activeObjects.deleteWithSQL(Comment.class, "QUESTION_ID = ?", questionId);
            } catch (ActiveObjectsException e) {
                return false;
            }
            return true;
        });
    }

}
