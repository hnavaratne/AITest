package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.virtusa.gto.plugins.aitest.db.CommentDBOperation;
import com.virtusa.gto.plugins.aitest.db.QuestionDBOperation;
import com.virtusa.gto.plugins.aitest.entity.Comment;
import com.virtusa.gto.plugins.aitest.entity.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class IssueQuestionManagement extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(IssueQuestionManagement.class);

    @JiraImport
    private ActiveObjects activeObjects;

    @ComponentImport
    private final UserManager userManager;

    private static final long serialVersionUID = -6956146005160138921L;

    @Inject
    public IssueQuestionManagement(ActiveObjects activeObjects, UserManager userManager) {
        this.activeObjects = activeObjects;
        this.userManager = userManager;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String data = req.getParameter("data");
        String action = req.getParameter("action");
        UserProfile currentUser = userManager.getRemoteUser(req);
        switch (action) {
            case "insert":
                resp.getWriter().write(insert(data));
                break;
            case "update":
                resp.getWriter().write(String.valueOf(update(data)));
                break;
            case "delete":
                resp.getWriter().write(String.valueOf(delete(data)));
                break;
            case "get":
                resp.getWriter().write(getQuestions(data));
                break;
            case "get-count":
                resp.getWriter().write(getQuestionsCount(data));
                break;
            case "insert-comment":
                resp.getWriter().write(insertComment(data, Objects.requireNonNull(currentUser)));
                break;
            case "get-comment":
                resp.getWriter().write(getComments(data));
                break;
            default:
                break;
        }
    }

    private String insert(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject responseObject = new JSONObject();

        String title = jsonData.get("title").getAsString();
        String description = jsonData.get("description").getAsString();
        Long issue = jsonData.get("issue").getAsLong();
        Long user = jsonData.get("user").getAsLong();

        QuestionDBOperation questionDBOperation = new QuestionDBOperation(activeObjects);
        Question insertedQuestion = questionDBOperation.insert(title, description, issue, user);

        try {
            responseObject.put("id", insertedQuestion.getID());
            responseObject.put("title", insertedQuestion.getTitle());
            responseObject.put("description", insertedQuestion.getDescription());
            responseObject.put("status", insertedQuestion.getStatus());

            JSONObject userObject = new JSONObject();
            Optional<ApplicationUser> questionUser = UserUtils.getUserManager().getUserById(insertedQuestion.getAssignTo());
            if (questionUser.isPresent()) {
                userObject.put("id", questionUser.get().getId());
                userObject.put("name", questionUser.get().getDisplayName());
                userObject.put("avatar", ComponentAccessor.getAvatarService().getAvatarAbsoluteURL(questionUser.get(), questionUser.get(), Avatar.Size.SMALL).toString());
            }
            responseObject.put("user", userObject);
            responseObject.put("issue", insertedQuestion.getIssue());
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }

        return responseObject.toString();
    }

    private String update(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();

        Long id = jsonData.get("id").getAsLong();
        String description = jsonData.get("description").getAsString();
        Long issue = jsonData.get("issue").getAsLong();
        Long user = jsonData.get("user").getAsLong();
        String status = jsonData.get("status").getAsString();

        QuestionDBOperation questionDBOperation = new QuestionDBOperation(activeObjects);
        questionDBOperation.update(id, description, status, user);
        JsonObject issueObj = new JsonObject();
        issueObj.addProperty("issue", issue);
        return getQuestions(issueObj.toString());
    }

    private boolean delete(String data) {
        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        Long id = jsonData.get("id").getAsLong();
        QuestionDBOperation questionDBOperation = new QuestionDBOperation(activeObjects);
        CommentDBOperation commentDBOperation = new CommentDBOperation(activeObjects);
        questionDBOperation.delete(id);
        String questionId = Long.toString(id);
        commentDBOperation.delete(questionId);
        return true;
    }

    private String getQuestions(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONArray responseArray = new JSONArray();
        QuestionDBOperation questionDBOperation = new QuestionDBOperation(activeObjects);
        List<Question> questions = questionDBOperation.get(jsonData.get("issue").getAsLong());
        for (Question question : questions) {
            JSONObject returnObject = new JSONObject();
            try {
                returnObject.put("id", question.getID());
                returnObject.put("title", question.getTitle());
                returnObject.put("description", question.getDescription());
                returnObject.put("status", question.getStatus());

                JSONObject userObject = new JSONObject();
                Optional<ApplicationUser> questionUser = UserUtils.getUserManager().getUserById(question.getAssignTo());
                if (questionUser.isPresent()) {
                    userObject.put("id", questionUser.get().getId());
                    userObject.put("name", questionUser.get().getDisplayName());
                    userObject.put("avatar", ComponentAccessor.getAvatarService().getAvatarAbsoluteURL(questionUser.get(), questionUser.get(), Avatar.Size.SMALL).toString());
                }
                returnObject.put("user", userObject);
                returnObject.put("issue", question.getIssue());
            } catch (JSONException e) {
                log.error(e.getMessage());
            }
            responseArray.put(returnObject);
        }

        return responseArray.toString();
    }

    private String getQuestionsCount(String data) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        JSONObject responseObject = new JSONObject();
        QuestionDBOperation questionDBOperation = new QuestionDBOperation(activeObjects);
        int questions = questionDBOperation.getCount(jsonData.get("issue").getAsLong());
        try {
            responseObject.put("count", questions);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return responseObject.toString();
    }

    private String insertComment(String data, UserProfile currentUser) {

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String questionId = jsonData.get("questionid").getAsString();
        String message = jsonData.get("message").getAsString();
        JsonObject respObj = new JsonObject();
        Date originalDate = new Date();

        SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String date = formater.format(originalDate);
        CommentDBOperation comment = new CommentDBOperation(activeObjects);
        String name = currentUser.getUserKey().getStringValue();

        Comment postObj = comment.insert(message, date, name, questionId);
        if (postObj != null) {
            respObj.addProperty("msg", "done");
            return respObj.toString();
        }
        respObj.addProperty("msg", "failed");
        return respObj.toString();
    }

    private String getComments(String data) {
        int startIndex;
        int endIndex;

        JsonElement element = new JsonParser().parse(data);
        JsonObject jsonData = element.getAsJsonObject();
        String questionId = jsonData.get("questionid").getAsString();
        int selectedSection = jsonData.get("selectedSection").getAsInt();

        CommentDBOperation comment = new CommentDBOperation(activeObjects);
        List<Comment> comments = comment.get(questionId);
        Collections.reverse(comments);

        int records_per_section = 7;
        double totalRecordCount = comments.size();
        double tempV = totalRecordCount / (double) records_per_section;
        double sectionCount = Math.ceil(tempV);
        List<Comment> resultcomments = new ArrayList<>();

        startIndex = (selectedSection * records_per_section) - records_per_section;
        endIndex = selectedSection * records_per_section;

        JSONArray responseArray = new JSONArray();
        JsonObject postJsonObj;
        Date originalDate = new Date();

        SimpleDateFormat formater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        for (int i = startIndex; i < comments.size(); i++) {
            if (i < endIndex) {
                resultcomments.add(comments.get(i));
            }
        }
        //Collections.reverse(resultcomments);
        for (int i = 0; i < resultcomments.size(); i++) {
            Comment post = resultcomments.get(i);
            postJsonObj = new JsonObject();
            ApplicationUser applicationuser = UserUtils.getUserManager().getUserByKey(post.getUser());
            postJsonObj.addProperty("size", resultcomments.size());
            postJsonObj.addProperty("postid", post.getID());
            if (!(applicationuser == null) && applicationuser.isActive()) {
                postJsonObj.addProperty("name", applicationuser.getUsername());
            }
            postJsonObj.addProperty("icon", ComponentAccessor.getAvatarService().getAvatarUrlNoPermCheck(applicationuser, Avatar.Size.SMALL).toString());
            postJsonObj.addProperty("message", post.getMessage());
            postJsonObj.addProperty("section", sectionCount);
            try {
                String difference = getDifference(originalDate, formater.parse(post.getDateAndTime()));
                postJsonObj.addProperty("time", difference);
                responseArray.put(postJsonObj);
            } catch (ParseException e) {
                log.error(e.getMessage(), e);
            }
        }

        return responseArray.toString();
    }

    private static String getDifference(Date d1, Date d2) {
        String result;

        long diff = d1.getTime() - d2.getTime();

        diff = diff / 1000;

        long days = diff / (24 * 60 * 60);
        long hours = diff / (60 * 60) % 24;
        long minutes = diff / 60 % 60;
        long seconds = diff % 60;

        if (days > 0) {
            result = days + " days";
            return result;
        } else if (hours > 0) {
            result = hours + " hours";
            return result;
        } else if (minutes > 0) {
            result = minutes + " minutes ";
            return result;
        } else {
            result = seconds + " seconds";
            return result;
        }

    }

}
