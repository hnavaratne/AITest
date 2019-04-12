AJS.toInit(function () {

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');
    var connectionSuccess;

    var dialog = new AJS.Dialog({
        width: 600,
        height: 385,
        id: "config-dialog",
        closeOnOutsideClick: true
    });

    dialog.addHeader("Configurations");

    dialog.addPanel("Panel 1", "<div class='jira-field-group'>" +
        "<label>Host</label><br/>" +
        "<input type='text' id='host' class='jira-form-control-lg'>" +
        "<label>Port</label><br/>" +
        "<input type='text' id='port' class='jira-form-control-lg'>" +
        "<label>Base URL</label><br/>" +
        "<input type='text' id='url' class='jira-form-control-lg'>"+
        "<label>Python Home</label><br/>" +
        "<input type='text' id='pythonHome' class='jira-form-control-lg'>"
        + "</div>", "panel-body");

    dialog.addButton("Test Connection", function (dialog) {
        var host = $('#host').val();
        var port = $('#port').val();
        var tbaseUrl = $('#url').val();
        var pythonHome = $('#pythonHome').val();
        var configurationObject = {
            "host": host,
            "port": port,
            "baseUrl": tbaseUrl,
            "pythonHome": pythonHome
        }
        if (isValidate(host, port, tbaseUrl)) {
            testConfigurations(configurationObject);
        } else {
            AJS.flag({
                type: 'error',
                title: 'Please fill all informations',
                close: 'auto'
            });
        }
    });

    dialog.addButton("Save", function (dialog) {
        var host = $('#host').val();
        var port = $('#port').val();
        var tbaseUrl = $('#url').val();
        var pythonHome = $('#pythonHome').val();
        var configurationObject = {
            "host": host,
            "port": port,
            "baseUrl": tbaseUrl,
            "pythonHome": pythonHome
        }
        if (isValidate(host, port, tbaseUrl)) {
            setConfigurations(configurationObject);
        } else {
            AJS.flag({
                type: 'error',
                title: 'Please fill all informations',
                close: 'auto'
            });

        }
    });

    dialog.addButton("Close", function (dialog) {
        dialog.hide();
    }, "#");

    dialog.addPage();

    AJS.$('#config_link').click(function (e) {
        e.preventDefault();
        dialog.gotoPage(0);
        dialog.gotoPanel(0);
        dialog.show();
        return 0;
    });

    function setConfigurations(configurationObject) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-management-util',
            async: false,
            data: {
                user: username,
                action: 'set-configuration',
                data: JSON.stringify(configurationObject)
            },
            success: function (response) {
                $('#host').val(response.host);
                $('#port').val(response.port);
                $('#url').val(response.baseurl);
                $('#pythonHome').val(response.pythonHome);
                if (response.testConnection) {
                    AJS.flag({
                        type: 'success',
                        title: 'Configuration Saved Successfully..',
                        close: 'auto'
                    });
                } else {
                    AJS.flag({
                        type: 'error',
                        title: 'Configuration Saving Error..',
                        body: 'Recheck the connection host,post or base url',
                        close: 'auto'
                    });
                }
                dialog.hide();
            }
        });
    }

    function testConfigurations(configurationObject) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-management-util',
            async: false,
            data: {
                user: username,
                action: 'test-configuration',
                data: JSON.stringify(configurationObject)
            },
            success: function (response) {
                $('#host').val(response.host);
                $('#port').val(response.port);
                $('#url').val(response.baseurl);
                $('#pythonHome').val(response.pythonHome);
                if (response.testConnection) {
                    AJS.flag({
                        type: 'success',
                        title: 'Test Connection Success..',
                        close: 'auto'
                    });
                } else {
                    AJS.flag({
                        type: 'error',
                        title: 'Configuration Error..',
                        body: 'Recheck the connection host,post or base url',
                        close: 'auto'
                    });
                }
            }
        });
    }

    function isValidate(host, port, tbaseUrl) {
        if (host === "" || port === "" || tbaseUrl === "") {
            return false;
        } else {
            return true;
        }
    }

    AJS.bind("show.dialog", function (e, data) {
        setTimeout(function () { loadConfigurations(); }, 100);
    });


    function loadConfigurations() {
     //   if (window.location.href.indexOf('TestExecutionView.jspa') >= 0 || window.location.href.indexOf('TestPlanningView.jspa') >= 0) {
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-management-util',
                async: false,
                data: {
                    user: username,
                    action: 'get-configurations',
                    data: JSON.stringify({ msg: "" })
                },
                success: function (response) {
                    $('#host').val(response.host);
                    $('#port').val(response.port);
                    $('#url').val(response.baseurl);
                    $('#pythonHome').val(response.pythonHome);
                }
            });
      //  }

    }

})(jQuery);