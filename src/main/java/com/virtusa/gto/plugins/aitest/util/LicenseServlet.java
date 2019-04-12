package com.virtusa.gto.plugins.aitest.util;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Scanned
public class LicenseServlet extends HttpServlet {

    private static final long serialVersionUID = -5306703736624767913L;

    private final PluginLicenseManager pluginLicenseManager;

    @Autowired
    public LicenseServlet(@ComponentImport PluginLicenseManager pluginLicenseManager) {
        this.pluginLicenseManager = pluginLicenseManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter w = resp.getWriter();
        if (pluginLicenseManager.getLicense().isDefined()) {
            PluginLicense license = pluginLicenseManager.getLicense().get();
            if (license.getError().isDefined()) {
                w.println("Your evaluation license of " + pluginLicenseManager.getLicense().get().getPluginName() + "expired. Please use the 'Buy' button to purchase a new license.");
            } else {
                w.println(pluginLicenseManager.getLicense().get().getRawLicense());
            }
        } else {
            w.println("License missing!");
        }
        w.close();
    }
}
