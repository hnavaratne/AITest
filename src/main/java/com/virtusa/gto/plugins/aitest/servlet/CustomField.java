package com.virtusa.gto.plugins.aitest.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.virtusa.gto.plugins.aitest.entity.TestCaseFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Scanned
public class CustomField extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CustomField.class);
    private static final long serialVersionUID = -4472625427993989635L;

    @Inject
    public CustomField(TemplateRenderer templateRenderer, ActiveObjects activeObjects) {
        this.templateRenderer = templateRenderer;
        this.activeObjects = activeObjects;
    }

    @JiraImport
    private TemplateRenderer templateRenderer;

    @JiraImport
    private ActiveObjects activeObjects;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.getWriter().write("<html><body>Hello World</body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/html");
        String customFiled = req.getParameter("custom_field");
        String selectedId = req.getParameter("selected_id");
        if ("parent".equals(customFiled)) {
            generateParentList(selectedId, resp);
        }
    }

    private void generateParentList(String selectedId, HttpServletResponse resp) {
        try {
            UserProjectHistoryManager component = ComponentAccessor.getComponent(UserProjectHistoryManager.class);
            Project currentProject = component.getCurrentProject(10, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser());
            String projectKey = currentProject.getKey();
            int selectedID = Integer.parseInt(selectedId);
            List<TestCaseFolder> ids = Arrays.asList(activeObjects.find(TestCaseFolder.class, "ID = ?", selectedID));
            List<TestCaseFolder> testCaseFolders = Arrays.asList(activeObjects.find(TestCaseFolder.class, "PROJECT_KEY = ? ", projectKey));
            HashMap<String, Object> map = new HashMap<>();
            map.put("selectedParent", ids);
            map.put("parentList", testCaseFolders);
            StringWriter stringWriter = new StringWriter();
            templateRenderer.render("templates/customfields/testcase-parent/project-parent.vm", map, stringWriter);
            String toString = stringWriter.getBuffer().toString();
            resp.getWriter().write(toString);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }
}