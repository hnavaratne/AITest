AJS.toInit(function ($) {
    'use strict';
    
    window.onbeforeunload = null;

    var businessSection = $('#bc-section');
    var testcaseSection = $('#test-case-details-page-panel');

    var treeNodeATagSelector = 'ul.aui-nav li > a.aui-nav-item';
    var treeNodeLiTagSelector = 'ul.aui-nav li';
    var $folderContext = $("#folderContext");
    var $testCaseContext = $("#testCaseContext");

    var $bcFolderContext = $("#bc-folderContext");
    var $bcScriptContext = $("#bc-scriptContext");

    var folderNodeSelector = 'li[type="folder"]';
    var testCaseNodeSelector = 'li[type="test-case"]';
    var businessFolderNodeSelector = 'li[type="bc-folder"]';
    var businessScriptNodeSelector = 'li[type="business-script"]';
    var childULNodeSelector = 'ul.aui-nav';

    var baseUrl = window.location.href.split("secure")[0];
    var globalParamArray = [];
    var newparams = [];
    var globalResponse;
    var tableEdited;
    var newDataCount = 0;
    var isNew = false;
    var storeRow;
    var newDataStore = [];
    var section = '';
    var selectedFolderNameToRename = '';
    var selectedTestCaseToRename = '';
    var selectedBusinessScriptToRename = '';

    var oscarRedirectedFolderID;
    var oscarRedirectedTestCaseID;
    var selectedTestCaseID;
    var selectedBusinessFunctionID;
    var selectedFolderID;
    var selectedFolderName;
    var parentName;
    var tempSelectedTestCaseID;
    var createdFolderList = new Array();
    var createdTestCasesList = new Array();
    var createdBCFolderList = new Array();
    var createdBusinessScriptList = new Array();
    var folderHierarchy = [];

    var testCaseDesigning = $('#aui-progress-tracker-test-case-designing');
    var testCasePlaning = $('#aui-progress-tracker-test-case-planning');
    var testCaseExecution = $('#aui-progress-tracker-test-case-execution');
    var testCaseResults = $('#aui-progress-tracker-test-case-results');

    var linkIssueTable;
    var scriptDataTable;

    testCaseDesigning.on('click', function () {
        window.open(baseUrl + 'secure/TestCaseDesigningView.jspa', '_self');
    });

    testCasePlaning.on('click', function () {
        window.open(baseUrl + 'secure/TestPlanningView.jspa', '_self');
    });

    testCaseExecution.on('click', function () {
        window.open(baseUrl + 'secure/TestExecutionView.jspa', '_self');
    });

    testCaseResults.on('click', function () {
        window.open(baseUrl + 'secure/TestReportsView.jspa', '_self');
    });
    testCaseResults.on('click', function () {
        window.open(baseUrl + 'secure/TestReportsView.jspa', '_self');
    });


    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    if (window.location.href.indexOf('TestCaseDesigningView.jspa') != -1) {
        setTimeout(() => {
            var testCaseId = getParameterByName('testcase');
            if (testCaseId != null) {
                oscarRedirectedTestCaseID = getParameterByName('testcase').split('-')[0];
                oscarRedirectedFolderID = getParameterByName('folderid').split('-')[0];
                loadDataFromOscarRoute(oscarRedirectedFolderID, oscarRedirectedTestCaseID);
            }
        }, 1);
    }

    //found
    $('body').on('contextmenu', treeNodeATagSelector, function (event) {
        $folderContext.hide();
        $testCaseContext.hide();
        $bcFolderContext.hide();
        $bcScriptContext.hide();
        if ($(event.currentTarget).parent().attr('type') === 'folder') {
            selectedFolderName = $(event.currentTarget).children().find('.aui-nav-item-label').prevObject.context.text;
            var parent_UL = $(event.currentTarget).parent().parent().attr('parent');
            var folder_ID = $(event.currentTarget).parent().attr('id');
            selectedFolderNameToRename = $(event.currentTarget).children("span[class='aui-nav-item-label']")[0].innerHTML;
            var testCaseIDS = [];
            for (var i = 0; i < $('[parent*=' + folder_ID + '] li').length; i++) {
                // console.log($('[parent*='+folder_ID+'] li')[i].id.split('-').length);
                if ($('[parent*=' + folder_ID + '] li')[i].id.split('-').length < 4) {
                    testCaseIDS.push($('[parent*=' + folder_ID + '] li')[i].id.split('-')[0]);
                }

            }
            //remove Testcase by folder
            tempSelectedTestCaseID = testCaseIDS;
            selectedFolderID = folder_ID.split('-')[0];
            if ($(event.currentTarget).parent().parent().attr('parent') === 'root-node') {
                parentName = 'null';
            } else {
                if ($(event.currentTarget).parent().attr('id') !== 'root-node') {
                    parentName = $(event.currentTarget).parent().parent().attr('parent').split('-')[0];
                }
            }
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            $(event.currentTarget).parent().addClass('active');
            selectedFolderID = $(event.currentTarget).parent().attr('id');
            if (folder_ID == 'root-node') {
                $folderContext.css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('.divider').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#renameFolderLI').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#deleteFolderLI').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
            } else {
                $folderContext.css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('.divider').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#renameFolderLI').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#deleteFolderLI').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
            }
        } else if ($(event.currentTarget).parent().attr('type') === 'test-case') {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            $(event.currentTarget).parent().addClass('active');
            selectedTestCaseID = $(event.currentTarget).parent().attr('id');
            selectedTestCaseToRename = $(event.currentTarget).children("span[class='aui-nav-item-label']")[0].innerHTML;
            $testCaseContext.css({
                display: "block",
                left: event.pageX,
                top: 'calc(' + event.pageY + 'px - 40px)'
            });
        } else if ($(event.currentTarget).parent().attr('type') === 'bc-folder') {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            $(event.currentTarget).parent().addClass('active');
            selectedFolderNameToRename = $(event.currentTarget).children("span[class='aui-nav-item-label']")[0].innerHTML;
            var folder_ID = $(event.currentTarget).parent().attr('id');
            selectedFolderID = folder_ID.split('-')[0];

            if ($(event.currentTarget).parent().parent().attr('parent') === 'bcroot-node') {
                parentName = 'null';
            } else {
                if ($(event.currentTarget).parent().attr('id') !== 'bcroot-node') {
                    parentName = $(event.currentTarget).parent().parent().attr('parent').split('-')[0];
                }
            }

            if (folder_ID == 'bcroot-node') {
                $bcFolderContext.css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('.divider').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#bc-renameFolderLI').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#bc-deleteFolderLI').css({
                    display: "none",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
            } else {
                $bcFolderContext.css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('.divider').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#bc-renameFolderLI').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
                $('#bc-deleteFolderLI').css({
                    display: "block",
                    left: event.pageX,
                    top: 'calc(' + event.pageY + 'px - 40px)'
                });
            }

        } else if ($(event.currentTarget).parent().attr('type') === 'business-script') {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            $(event.currentTarget).parent().addClass('active');
            selectedBusinessFunctionID = $(event.currentTarget).parent().attr('id');
            selectedBusinessScriptToRename = $(event.currentTarget).children("span[class='aui-nav-item-label']")[0].innerHTML;
            $bcScriptContext.css({
                display: "block",
                left: event.pageX,
                top: 'calc(' + event.pageY + 'px - 40px)'
            });

        }
        return false;
    });

    $('html').click(function () {
        $folderContext.hide();
        $testCaseContext.hide();
        $bcFolderContext.hide();
        $bcScriptContext.hide();
    });

    $(folderNodeSelector).click(function (event) {
        $(folderNodeSelector).removeClass('active');
        $(testCaseNodeSelector).removeClass('active');
        $(businessFolderNodeSelector).removeClass('active');
        $(businessScriptNodeSelector).removeClass('active');
        this.classList.toggle('active');
        selectedFolderID = $(event.currentTarget).attr('id');
        var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
        if (childUL.length !== 0) {
            if (childUL.css('display') === 'block') {
                childUL.css('display', 'none');
            } else {
                if ($(event.currentTarget).attr('id') === 'root-node') {
                    init('null');
                } else {
                    init($(event.currentTarget).attr('id').split('-')[0]);
                }
                childUL.css('display', 'block');
            }
        }
        $('#test-case-details-page-panel').attr('style', 'display: none;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
    });
    $(businessFolderNodeSelector).click(function (event) {
        $(folderNodeSelector).removeClass('active');
        $(testCaseNodeSelector).removeClass('active');
        $(businessFolderNodeSelector).removeClass('active');
        $(businessScriptNodeSelector).removeClass('active');
        this.classList.toggle('active');
        selectedFolderID = $(event.currentTarget).attr('id');
        var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
        if (childUL.length !== 0) {
            if (childUL.css('display') === 'block') {
                childUL.css('display', 'none');
            } else {
                if ($(event.currentTarget).attr('id') === 'bcroot-node') {
                    initBC('null');
                } else {
                    initBC($(event.currentTarget).attr('id').split('-')[0]);
                }
                childUL.css('display', 'block');
            }
        }
        $('#test-case-details-page-panel').attr('style', 'display: none;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
    });

    $(businessScriptNodeSelector).click(function (event) {
        $(folderNodeSelector).removeClass('active');
        $(testCaseNodeSelector).removeClass('active');
        $(businessFolderNodeSelector).removeClass('active');
        $(businessScriptNodeSelector).removeClass('active');
        this.classList.toggle("active");
        $('#test-case-details-page-panel').attr('style', 'display: block;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: none;');
    });

    $(testCaseNodeSelector).click(function (event) {
        $(folderNodeSelector).removeClass('active');
        $(testCaseNodeSelector).removeClass('active');
        $(businessFolderNodeSelector).removeClass('active');
        this.classList.toggle("active");
        selectedTestCaseID = $(event.currentTarget).attr('id');

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                entity: 'TestCase',
                action: 'get',
                data: JSON.stringify({
                    'id': selectedTestCaseID.split('-')[0]
                })
            },
            success: function (response) {
                var response = JSON.parse(response);
                $('span#selected-test-case-titile').text(response.name);
                $('input#test-case-name').val(response.name);
                $('textarea#test-case-description').val(response.description);
                $('textarea#test-case-overall-expected-result').val(response.overallExpectedResult);
                if (response.manual == 'Yes') {
                    $('#manualYes').prop('checked', true)
                } else {
                    $('#manualNo').prop('checked', true)
                }
                if (response.automated == 'Yes') {
                    $('#automatedYes').prop('checked', true);
                } else {
                    $('#automatedNo').prop('checked', true);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        $('#test-case-details-page-panel').attr('style', 'display: block;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: none;');
    });

    var $user_name = AJS.$('#header-details-user-fullname');
    var username = $user_name.attr('data-username');
    var $project = AJS.$('#browse_link');
    var projectanchor = $project.attr('href');

    function init(parent) {
        AJS.$("#select2-issueid").auiSelect2();
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-management',
            async: false,
            data: {
                user: username,
                entity: 'TestCaseFolder',
                action: 'get-folders',
                data: JSON.stringify({
                    'parent': parent,
                    'redirectView': '/secure/TestCaseDesigningView.jspa'
                })
            },
            success: function (response) {
                console.log(response);
                var returnTreeItems = JSON.parse(response);
                if (parent === 'null') {
                    $('ul[parent="root-node"]').empty();
                } else {
                    $('ul[parent="' + parent + '-test-case-folder"]').empty();
                }
                for (var i = 0; i < returnTreeItems.folders.length; i++) {
                    var folderObject = returnTreeItems.folders[i];
                    if (folderObject.parent === 'null') {
                        appendChild($('ul[parent="root-node"]'), folderObject);
                    } else {
                        appendChild($('ul[parent="' + folderObject.parent + '-test-case-folder"]'), folderObject);
                    }
                }
                var testcases = JSON.parse(returnTreeItems.testcases);
                for (var i = 0; i < testcases.length; i++) {
                    if (testcases[i].parent === '0' || testcases[i].parent == "null") {
                        appendTestCaseChild($('ul[parent="root-node"]'), testcases[i]);
                    } else {
                        appendTestCaseChild($('ul[parent="' + testcases[i].parent + '-test-case-folder"]'), testcases[i]);
                    }
                }
            },
            error: function (jqXHR, exception) {
                var responseObject = JSON.parse(jqXHR.responseText);
                if (jqXHR.status === 302) {
                    if (responseObject.redirect) {
                        window.location.replace(responseObject.redirect);
                        return;
                    }
                }
            }
        });

    }

    $('#addFolderLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-folder-dialog').show();
    });

    $('#addTestCaseLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-test-case-dialog').show();
    });

    $('#renameFolderLI').click(function (event) {
        event.preventDefault();
        $("#newfolder-name").val(selectedFolderNameToRename);
        AJS.dialog2('#rename-folder-dialog').show();
    });

    AJS.$('#folder-newname-save-button').click(function (e) {
        event.preventDefault();
        var new_FolderName = $('#newfolder-name').val();
        getAllData();
        if (!validateInput($('#newfolder-name').val())) {
            if (!validateCreation(new_FolderName, createdFolderList)) {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-case-management',
                    async: false,
                    data: {
                        user: username,
                        entity: 'TestCaseFolder',
                        action: 'update',
                        data: JSON.stringify({
                            'parent': parentName,
                            'oldname': selectedFolderName,
                            'newname': new_FolderName
                        })
                    },
                    success: function (response) {
                        var response = JSON.parse(response);
                        if (response) {
                            $('#newfolder-name').val('')
                            AJS.dialog2('#rename-folder-dialog').hide();
                            $('li#' + selectedFolderID + ' .aui-nav-item-label').text(new_FolderName);
                            AJS.flag({
                                type: 'success',
                                title: 'Folder Updated..',
                                close: 'auto'
                            });
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Updating The Folder..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        } else {
            if ($('#newfolder-name').val() == ''){
                AJS.flag({
                    type: 'error',
                    title: 'Folder Name Should Not Be Empty..',
                    close: 'auto'
                });
            }
        }
    });

    $('#deleteFolderLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#warning-dialog-folder').show();
    });

    $('#folder-delete-confirm').on('click', function () {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-management',
            async: false,
            data: {
                user: username,
                entity: 'TestCaseFolder',
                action: 'full-delete',
                data: JSON.stringify({
                    'id': selectedFolderID.split('-')[0]
                })
            },
            success: function (response) {
                var responseObj = JSON.parse(response);
                if (responseObj) {
                    $('li#' + selectedFolderID).remove();
                    $('ul[parent="' + selectedFolderID + '"]').remove();
                    $('#test-case-details-page-panel').attr('style', 'display: none;');
                    $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
                    AJS.dialog2('#warning-dialog-folder').hide();
                    AJS.flag({
                        type: 'success',
                        title: 'Folder deleted..',
                        close: 'auto'
                    });
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Folder..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('#folder-delete-cancel').on('click', function () {
        AJS.dialog2('#warning-dialog-folder').hide();
    });

    $('#test-case-delete-cancel').on('click', function () {
        AJS.dialog2('#warning-dialog-test-case').hide();
    });

    $('#renameTestCaseLi').click(function (event) {
        event.preventDefault();
        $('#new-testcase-name').val(selectedTestCaseToRename);
        AJS.dialog2("#rename-testcase-dialog").show();
    });

    AJS.$("#testcase-newname-save-button").click(function (e) {
        e.preventDefault();
        getAllData();
        var new_Testcase_Name = $('#new-testcase-name').val();
        if (!validateInput($('#new-testcase-name').val())) {
            if (!validateCreation(new_Testcase_Name, createdTestCasesList)) {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-case',
                    async: false,
                    data: {
                        user: username,
                        entity: 'TestCase',
                        action: 'rename',
                        data: JSON.stringify({
                            'id': selectedTestCaseID.split('-')[0],
                            'newname': new_Testcase_Name
                        })
                    },
                    success: function (response) {
                        var response = JSON.parse(response);
                        if (response) {
                            $('#new-testcase-name').val('');
                            AJS.dialog2("#rename-testcase-dialog").hide();
                            $('li#' + selectedTestCaseID + ' .aui-nav-item-label').text(new_Testcase_Name);
                            $('#selected-test-case-titile').text(new_Testcase_Name);
                            AJS.flag({
                                type: 'success',
                                title: 'Test Case Updated..',
                                close: 'auto'
                            });
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Updating The Test Case..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        }
    });

    $('#deleteTestCaseLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#warning-dialog-test-case').show();
    });

    $('#test-case-delete-confirm').click(() => {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                entity: 'TestCase',
                action: 'delete',
                data: JSON.stringify({
                    'id': selectedTestCaseID.split('-')[0]
                })
            },
            success: function (response) {
                var response = JSON.parse(response);
                if (response) {
                    $('li#' + selectedTestCaseID).remove();
                    removeFromTestPlan(selectedTestCaseID.split('-')[0]);
                    AJS.flag({
                        type: 'success',
                        title: 'Test Case deleted..',
                        close: 'auto'
                    });
                    AJS.dialog2('#warning-dialog-test-case').hide();
                    testcaseSection.css({ display: "none" });
                    $('#test-case-details-page-panel').attr('style', 'display: none;');
                    $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Test Case..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    function removeFromTestPlan(selectedTID) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-plan-tc',
            async: false,
            data: {
                user: username,
                action: 'deleteFTP',
                data: JSON.stringify({
                    'testPlanId': selectedTID
                })
            },
            success: function (response) {

            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Test Plan..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#cancel-folder-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-folder-dialog').hide();
    });

    $('#cancel-test-case-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-test-case-dialog').hide();
    });

    $('#create-folder-btn').click(function (event) {
        event.preventDefault();
        getAllData();
        var ajaxData;
        var validateSuccess = false;
        var new_FolderName = $('#new-folder-name').val();
        if (selectedFolderID === 'root-node') {
            if (!validateInput($('#new-folder-name').val())) {
                if (!validateCreation($('#new-folder-name').val(), createdFolderList)) {
                    ajaxData = JSON.stringify({
                        'name': $('#new-folder-name').val(),
                        'description': $('#new-folder-description').val(),
                        'parent': 'null'
                    });
                    validateSuccess = true;
                }
            }
        } else {
            if (!validateInput($('#new-folder-name').val())) {
                if (!validateCreation($('#new-folder-name').val(), createdFolderList)) {
                    ajaxData = JSON.stringify({
                        'name': $('#new-folder-name').val(),
                        'description': $('#new-folder-description').val(),
                        'parent': selectedFolderID.split('-')[0]
                    });
                    validateSuccess = true;
                }
            }
        }

        if (validateSuccess) {
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-case-management',
                async: false,
                data: {
                    user: username,
                    entity: 'TestCaseFolder',
                    action: 'insert',
                    data: ajaxData
                },
                success: function (response) {
                    $('#new-folder-name').val('');
                    $('#new-folder-description').val('');
                    var folderObject = JSON.parse(response);
                    if (folderObject.parent === 'null') {
                        appendChild($('ul[parent="root-node"]'), folderObject);
                    } else {
                        appendChild($('ul[parent="' + folderObject.parent + '-test-case-folder"]'), folderObject);
                    }
                    AJS.flag({
                        type: 'success',
                        title: 'New Folder created..',
                        close: 'auto'
                    });
                    AJS.dialog2('#add-folder-dialog').hide();
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Inserting The Folder..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        }
    });

    $('#create-test-case-btn').click(function (event) {
        event.preventDefault();
        getAllData();
        var ajaxData;
        var validateSuccess = false;
        var manualString = $('input[name="new-test-case-manual"]:checked').val();
        var automatedString = $('input[name="new-test-case-automated"]:checked').val();
        if (!validateInput($('#new-test-case-name').val())) {
            if (!validateCreation($('#new-test-case-name').val(), createdTestCasesList)) {
                ajaxData = {
                    'name': $('#new-test-case-name').val(),
                    'description': $('#new-test-case-description').val(),
                    'overall_expected_result': $('#new-test-case-overall-expected-result').val(),
                    'automated': automatedString,
                    'manual': manualString
                };
                validateSuccess = true;
            }
        }
        if (validateSuccess) {
            if (selectedFolderID === 'root-node') {
                ajaxData.parent = 0;
            } else {
                var id = parseInt(selectedFolderID.split('-')[0]);
                if (typeof id === 'undefined') {
                    ajaxData.parent = 'err';
                } else {
                    ajaxData.parent = id;
                }
            }
            if (ajaxData.parent !== 'err') {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-case',
                    async: false,
                    data: {
                        user: username,
                        action: 'insert',
                        data: JSON.stringify(ajaxData)
                    },
                    success: function (response) {
                        var responseObj = JSON.parse(response);
                        if (responseObj.id) {
                            $('#new-test-case-name').val('');
                            $('#new-test-case-description').val('');
                            $('#new-test-case-overall-expected-result').val('');
                            $('input[name="new-test-case-manual"]:checked').val('Yes');
                            $('input[name="new-test-case-atomated"]:checked').val('No');
                            if (responseObj.parent === '0') {
                                appendTestCaseChild($('ul[parent="root-node"]'), responseObj);
                            } else {
                                appendTestCaseChild($('ul[parent="' + responseObj.parent + '-test-case-folder"]'), responseObj);
                            }
                            AJS.flag({
                                type: 'success',
                                title: 'New Test Case created..',
                                close: 'auto'
                            });
                            AJS.dialog2('#add-test-case-dialog').hide();
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Inserting The Test Case..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        }


    });

    function appendChild(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-test-case-folder',
            'type': 'folder',
        });
        var aObject = $('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-folder'
        });
        var nameSpan = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        nameSpan.text(child.name);

        aObject.append(iconSpan);
        aObject.append(nameSpan);

        liObject.append(aObject);

        liObject.click(function (event) {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            this.classList.toggle('active');
            selectedFolderID = $(event.currentTarget).attr('id');
            var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
            if (childUL.length !== 0) {
                if (childUL.css('display') === 'block') {
                    childUL.css('display', 'none');
                } else {
                    init($(event.currentTarget).attr('id').split('-')[0]);
                    childUL.css('display', 'block');
                }
            }
         //   $('#test-case-details-page-panel').attr('style', 'display: block;');
        });

        var ulObject = $('<ul>', {
            'class': 'aui-nav',
            'parent': child.id + '-test-case-folder',
            'style': 'display: none'
        });

        parent.append(liObject);
        parent.append(ulObject);

    }

    function appendTestCaseChild(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-test-case',
            'type': 'test-case',
        });

        var aObject = $('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-bullet-list'
        });
        var nameSpan = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        nameSpan.text(child.name);

        aObject.append(iconSpan);
        aObject.append(nameSpan);

        liObject.append(aObject);

        liObject.click(function (event) {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            this.classList.toggle("active");
            selectedTestCaseID = $(event.currentTarget).attr('id');
            window.location.hash = '#!' + selectedTestCaseID;
            selectedFolderID = $(event.currentTarget).parent().attr('parent');
            testcaseSection.css({ display: "block" });
            businessSection.css({ display: "none" });
            LoadTestDataInfo();
            loadBDTValues();
            loadScriptDetails(selectedTestCaseID.split('-')[0]);
            $('#test-step-tb > tr > td > textarea').css({height:"30px"});
            // Load test case into view..
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-case',
                async: false,
                data: {
                    user: username,
                    entity: 'TestCase',
                    action: 'get',
                    data: JSON.stringify({
                        'id': selectedTestCaseID.split('-')[0]
                    })
                },
                success: function (response) {
                    var response = JSON.parse(response);
                    $('span#selected-test-case-titile').text(response.name);
                    $('input#test-case-name').val(response.name);
                    $('textarea#test-case-description').val(response.description);
                    $('textarea#test-case-overall-expected-result').val(response.overallExpectedResult);

                    if (response.manual == 'Yes') {
                        $('#manualYes').prop('checked', true);
                    } else {
                        $('#manualNo').prop('checked', true);
                    }
                    if (response.automated == 'Yes') {
                        $('#automatedYes').prop('checked', true);
                    } else {
                        $('#automatedNo').prop('checked', true);
                    }
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Test Case..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
            $('#test-case-details-page-panel').attr('style', 'display: block;');
            $('#test-case-details-page-panel-empty').attr('style', 'display: none;');
        });

        parent.append(liObject);
    }

    function readBdtNotes() {
        var tempUpdatedDataArray = [];
        var table = $('#sticker-table-body')[0];

        for (var i = 0; i < table.rows.length; i++) {

            for (var j = 0; j < table.rows[i].cells.length; j++) {
                if (j != 0) {
                    var updateDataObject = {
                        "value": table.rows[i].cells[j].getElementsByTagName('input')[0].value
                    }
                    tempUpdatedDataArray.push(updateDataObject);
                }

            }

        }
        return tempUpdatedDataArray;
    }

    function loadBDTValues() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/business-script-steps-management',
            async: false,
            data: {
                user: username,
                action: 'get_bdts',
                data: JSON.stringify({
                    'testCaseId': selectedTestCaseID.split('-')[0]
                })
            },
            success: (resp) => {
                $("#sticker-table-body").html("");
                var resp = JSON.parse(resp);
                if (resp.length > 0) {
                    for (var i = 0; i < resp.length; i++) {
                        var TR = $('<tr>');
                        var spanIcon = $('<span>', {
                            'class': 'aui-icon aui-icon-small aui-iconfont-cross-circle'
                        });
                        var remove_Bdt_Btn = $('<button>', {
                            'class': 'remove-bdt'
                        });
                        var remove_Row_TD = $('<td>', {
                            'style': 'width:2%;padding-top: 1.5%;'
                        });
                        var input_TD = $('<td>');
                        var inputInfo = $('<input>', {
                            'class': 'jira-bdt-form-ctrl'
                        });
                        remove_Bdt_Btn.append(spanIcon);
                        remove_Row_TD.append(remove_Bdt_Btn);
                        inputInfo.val(resp[i].description);
                        input_TD.append(inputInfo);
                        TR.append(remove_Row_TD);
                        TR.append(input_TD);
                        $('#sticker-table-body').append(TR);

                        $('.remove-bdt').live('click', function (e) {
                            e.preventDefault();
                            $(this).closest('tr').remove();
                        }
                        );
                    }
                }
            },
            error: (jqXHR, textStatus, errorThrown) => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The BDT Values..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#addLinkBtn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-link-dialog').show();
    });

    $('#cancel-link-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-link-dialog').hide();
    });

    $('#create-link-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-link-dialog').hide();
    });

    $('#test-case-planning').on('click', function () {
        window.open(baseUrl + 'secure/TestPlanningView.jspa', '_self');
    });

    $('#test-case-execution').on('click', function () {
        window.open(baseUrl + 'secure/TestExecutionView.jspa', '_self');
    });

    $('#aui-uid-1').click(function (event) {
        LoadTestDataInfo();
    });

    $('#aui-uid-2').click(function (event) {
        loadScriptDetails(selectedTestCaseID.split('-')[0]);
    });

    $('#aui-uid-3').click(function (event) {
        LoadTestDataInfo();
    });

    /*=========================================Test Data part===========================================*/
    var oldResponse;
    function LoadTestDataInfo() {
        if (typeof selectedTestCaseID != "undefined") {
            var testCaseId = selectedTestCaseID.split('-')[0];
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-case-data-management',
                async: false,
                data: {
                    user: username,
                    action: 'get_test_case_params',//edited
                    data: JSON.stringify({
                        test_case_id: testCaseId
                    })
                },
                success: function (response) {
                    oldResponse = response;
                    decryptResponse(response);
                    loadHeaderAndInputs(response);
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Test Data..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
            $("#add-Row-Button").click(function () {
                var newRowData = [];
                var count = 1;
                tableEdited = false;
                newDataCount++;
                $('body #newDataRow').find('td').each(function () {
                    if (count !== 1) {
                        var param = {
                            'name': $(this).find("input").attr('id'),
                            'value': $(this).find("input").val()
                        }
                        $(this).find("input").val('');
                        newRowData.push(param);
                    }
                    count++;
                });

                generateTable(newRowData);
                $('body .btn-remove-temp').click(function (evt) {
                    newDataCount--;
                    var $item = $(this).closest("tr")
                        .find("td").each(function () {
                            if ($(this).find("input").val() != '' & $(this).find("input").val() != null) {
                                var name = $(this).find("input").attr('id');
                                var value = $(this).find("input").val();
                                for (var i = 0; i < newparams.length; i++) {
                                    for (var j = 0; j < newparams.length; j++) {
                                        if (newparams[i][j].name == name && newparams[i][j].value == value) {
                                            newparams.splice(i, 1);
                                            break;
                                        }
                                    }

                                }

                            }
                        })
                        .remove();
                });
            })
        }
    }
    var globalResponseDataArray;
    var removeData = [];
    function decryptResponse(response) {
        var responseData = []
        for (var i = 0; i < response.length; i++) {
            var param = {
                'paramName': response[i].paramName,
                'values': []
            }
            for (var j = 0; j < response[i].values.length; j++) {
                param.values.push(response[i].values[j]);
            }
            responseData.push(param);
        }
        globalResponseDataArray = responseData;
        addRowToDataTable(responseData);

        $('.btn-remove').click(function (evt) {
            var $item = $(this).closest("tr")   // Finds the closest row <tr>
                .find("td").each(function () {
                    if ($(this).find("input").val() != '' || $(this).find("input").val() != null || typeof $(this).find("input").val() == 'undefined') {
                        var removeObject = {
                            'name': $(this).find("input").attr('id'),
                            'value': $(this).find("input").val()
                        }
                        removeData.push(removeObject);

                    }
                }).remove();
        });
    }



    function loadHeaderAndInputs(response) {
        globalResponse = response;

        $('#auithead th').remove();
        $('#auitbody td').remove();
        var table_data = $('#test-data-table');
        var table_head = $('#auithead');
        var table_body = $('#auitbody');
        var table_head_row = $('<tr>');

        var table_data_add_TD = $('<td>', {
            'style': 'width:20px;border:none;'
        });
        table_head_row.append('<th style="width:20px;border:none;"></th>')
        var table_data_row = $('<tr id="newDataRow">');
        if (response.length > 0) {

            var add_Data_Button = $('<button>', {
                'id': "add-Row-Button",
                'class': 'jira-btn-sm'

            });
            var add_Data_Span = $('<span>', {
                'id': 'add-Row',
                'class': 'aui-icon aui-icon-small aui-iconfont-add-circle',
                'style': 'display:"block";margin-left:"auto";margin-right:"auto";'
            });
            add_Data_Button.append(add_Data_Span);
            table_data_add_TD.append(add_Data_Button);
            table_data_row.append(table_data_add_TD);
        }
        for (var i = 0; i < response.length; i++) {
            var paramName = response[i]['paramName'];
            table_head_row.append('<th style="text-align:center;">' + paramName + '</th>');
            var tbody_data = $('<td>');

            var inputValue = $('<input>', {
                'id': paramName,
                'type': 'text',
                'class': 'jira-form-control',
                'style': 'min-width:150px',
                'autocomplete': 'off'
            });
            tbody_data.append(inputValue);
            table_data_row.append(tbody_data);

            var valueArray = response[i]['values'];
        }
        table_head.append(table_head_row.append('</tr>')).append('</thead>');
        table_data.append(table_head);
        table_body.append(table_data_row);
    }

    function generateTable(newRowData) {

        if (typeof newRowData === 'object') {
            globalParamArray.push(newRowData);
            if (checkIfFieldEmpty(newRowData)) {//new added
                newparams.unshift(newRowData);
                addNewRowToTable(newparams);
                tableEdited = true;
            } else {
                AJS.flag({
                    type: 'error',
                    title: 'At Least One Field Is Required..',
                    close: 'auto'
                });
                tableEdited = false;
            }
        } else {

        }
    }

    //new Added
    function checkIfFieldEmpty(rowData) {

        var dataCount = rowData.length;
        var count = 0;
        var emptyCount = 0;
        for (var i = 0; i < dataCount; i++) {
            if (rowData[i].value == '') {
                emptyCount++;
                count++;
            } else {
                count++;
            }
        }
        if (dataCount === count && emptyCount < dataCount) {
            return true;
        }
        return false;
    }

    function addNewRowToTable(newParams) {

        $("#auitbody-data").html("");
        for (var j = 0; j < newParams.length; j++) {
            var newRowData = newParams[j];
            var dataRowTRObject = $('<tr>');

            var table_data_remove_TD = $('<td>', {
                'style': 'width:20px;border:none;'
            });

            var remove_Data_Button = $('<button>', {
                'id': 'btnremove',
                'class': 'jira-btn-sm btn-remove-temp'

            });
            var remove_Data_Span = $('<span>', {
                'class': 'aui-icon aui-icon-small aui-iconfont-list-remove',
                'style': 'display:"block";margin-left:"auto";margin-right:"auto";'
            });
            remove_Data_Button.append(remove_Data_Span);
            table_data_remove_TD.append(remove_Data_Button);
            dataRowTRObject.append(table_data_remove_TD);
            for (var i = 0; i < newRowData.length; i++) {

                var param = newRowData[i];
                var tdParamObject = $('<td>', {
                    'id': param.name + '-data-param-row'
                });

                var tdParamInputObject = $('<input>', {
                    'id': param.name,
                    'class': 'jira-form-control',
                    'style': 'min-width:150px',
                    'autocomplete': 'off'
                });
                tdParamInputObject.val(param.value);
                tdParamObject.append(tdParamInputObject);
                dataRowTRObject.append(tdParamObject)
            }
            $('tbody#auitbody-data').append(dataRowTRObject);
        }

        isNew = true;
        addRowToDataTable(globalResponseDataArray);
    }

    function addRowToDataTable(params) {
        if (!isNew) {
            $("#auitbody-data").html("");
        }
        var max = 0;
        for (var j = 0; j < params.length; j++) {
            if (max < params[j].values.length) {
                max = params[j].values.length;
            }
        }

        for (var k = 0; k < max; k++) {
            var dataRowTRObject = $('<tr>');

            var table_data_remove_TD = $('<td>', {
                'style': 'width:20px;border:none;'
            });

            var remove_Data_Button = $('<button>', {
                'id': 'btnremove',
                'class': 'jira-btn-sm btn-remove'

            });
            var remove_Data_Span = $('<span>', {
                'class': 'aui-icon aui-icon-small aui-iconfont-list-remove',
                'style': 'display:"block";margin-left:"auto";margin-right:"auto";'
            });

            remove_Data_Button.append(remove_Data_Span);
            table_data_remove_TD.append(remove_Data_Button);
            dataRowTRObject.append(table_data_remove_TD);
            for (var i = 0; i < params.length; i++) {
                var param = params[i]
                var tdParamObject = $('<td>', {
                    'id': param.paramName + '-data-param-row'
                });

                var tdParamInputObject = $('<input>', {
                    'id': param.paramName,
                    'class': 'jira-form-control',
                    'style': 'min-width:150px',
                    'autocomplete': 'off'
                });
                tdParamInputObject.val(param.values[k]);
                tdParamObject.append(tdParamInputObject);
                dataRowTRObject.append(tdParamObject);
            }
            $('tbody#auitbody-data').append(dataRowTRObject);

        }

    }
    //updated section======================================
    var updatedArray;
    $('#save-data').click(function (event) {
        $.when(paramValueLoad()).done(function () {
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-case-data-management',
                async: false,
                data: {
                    user: username,
                    testCaseId: selectedTestCaseID.split('-')[0],
                    action: 'save_and_update',
                    data: JSON.stringify(updatedArray)
                },
                success: function (response) {
                    isNew = false;
                    newparams = [];
                    LoadTestDataInfo();
                    AJS.flag({
                        type: 'success',
                        title: 'Saved Successfully..',
                        close: 'auto'
                    });
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Saving The Test Data..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        });
    });

    function paramValueLoad() {

        updatedArray = getAllTableData();
        if (updatedArray) {
            return true;
        }
        return false;

    }

    function getAllTableData() {
        var tempUpdatedDataArray = [];
        var table = $('#auitbody-data')[0];

        for (var i = 0; i < table.rows.length; i++) {

            for (var j = 0; j < table.rows[i].cells.length; j++) {
                if (j != 0) {
                    var updateDataObject = {
                        "name": table.rows[i].cells[j].getElementsByTagName('input')[0].id,
                        "value": table.rows[i].cells[j].getElementsByTagName('input')[0].value,
                        "row": i,
                        "col": j
                    }
                    tempUpdatedDataArray.push(updateDataObject);
                }

            }

        }

        return generateUpdateArray(tempUpdatedDataArray);

    }


    function generateUpdateArray(updatedDataArray) {

        var cloneArray = [];
        for (var j = 0; j < updatedDataArray.length; j++) {
            if (cloneArray.length > 0) {
                var isExist = false;
                for (var k = 0; k < cloneArray.length; k++) {
                    if (cloneArray[k].name == updatedDataArray[j].name) {
                        cloneArray[k].values.unshift(updatedDataArray[j].value);
                        isExist = true;
                    }
                }
                if (!isExist) {
                    var object = {
                        "name": updatedDataArray[j].name,
                        "values": []
                    }
                    object.values.unshift(updatedDataArray[j].value);
                    cloneArray.push(object);
                }
            } else {
                var object = {
                    "name": updatedDataArray[j].name,
                    "values": []
                }
                object.values.unshift(updatedDataArray[j].value);//edit
                cloneArray.push(object);
            }
        }
        return cloneArray;
    }

    /*=========================================Linked Issue part===========================================*/

    var firstLoadResponse;
    AJS.$("#btnaddlinkissue").click(function (e) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-management-view-project-issues',
            async: false,
            data: {
                action: 'add',
                user: username
            },
            success: function (response) {
                var issueInfo = JSON.parse(response);
                appendToIssueDialog(issueInfo);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Adding The Linked Issue..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        e.preventDefault();
        AJS.dialog2("#demo-dialog").show();
    });

    function appendToIssueDialog(issueobj) {
        $('#select2-issueid')
            .find('option')
            .remove()
            .end();
        $('#select2-issueid').select2('data', null);
        var selectChoice = $('#select2-issueid');
        Object.keys(issueobj).forEach(function (key) {
            console.log(issueobj);
            var issuegroup = issueobj[key];
            var optGroupSummary = $('<optgroup>', {
                'label': key
            });
            for (var i = 0; i < issuegroup.length; i++) {
                for (var j = 0; j < issuegroup[i].children.length; j++) {
                    var option = $('<option>', {
                        'id': issuegroup[i].children[j],
                        'value': j,
                        'text': issuegroup[i].text
                    });
                    optGroupSummary.append(option);
                }

            }
            selectChoice.append(optGroupSummary);
        });

    }

    $('#select2-issueid').on('change', function () {
        $('#' + this.value).remove();

    });

    var fullIssueArray = null;
    var pageCount = 0;
    var noOfIssues = 0;

    function regenerateUniqueArray(issueobj) {
        var uniqueissues = [];
        $.each(issueobj, function (i, e) {
            var matchingItems = $.grep(uniqueissues, function (item) {
                return item.issue_id === e.issue_id && item.issue_name == e.issue_name;
            });
            if (matchingItems.length === 0) {
                uniqueissues.push(e);
            }
        });
        fullIssueArray = uniqueissues;

        return uniqueissues;

    }

    function appendToLinkIssueTable() {
        var issuetablebody = $('#issuetblbody');
        $("#issuetblbody tr").remove();

        for (var i = 0; i < fullIssueArray.length; i++) {
            if (fullIssueArray.length == 1) {
                $('#btnaddlinkissue').css('display', 'none');
            }
            var issue = fullIssueArray[i];
            var issueTablebody_TR = $('<tr>');
            var issueTablebody_Issueid_TR_TD = $('<td>', {
                'style': 'vertical-align: middle; text-align: center;'
            });
            var issueTablebody_Issueid_TR_A = $('<a>', {
                'href': baseUrl + 'browse/' + issue.issue_name

            });
            var nameSpan = $('<span>');
            nameSpan.text(issue.issue_id);
            issueTablebody_Issueid_TR_A.append(nameSpan);

            var issueTablebody_Issuename_TR_TD = $('<td>', {
                'style': 'vertical-align: middle; text-align: center;'
            });

            var issueTablebody_summary_TR_TD = $('<td>', {
                'style': 'vertical-align: middle; text-align: center;'
            });
            var summarySpan = $('<span>');
            summarySpan.text(issue.summary);

            issueTablebody_summary_TR_TD.append(summarySpan);
            issueTablebody_Issueid_TR_TD.append(issueTablebody_Issueid_TR_A);
            issueTablebody_Issuename_TR_TD.text(issue.issue_name);
            issueTablebody_TR.append(issueTablebody_Issueid_TR_TD);
            issueTablebody_TR.append(issueTablebody_Issuename_TR_TD);
            issueTablebody_TR.append(issueTablebody_summary_TR_TD);
            issuetablebody.append(issueTablebody_TR);
        }
    }

    function initializeDataTable() {

        linkIssueTable = $('#tbli').dataTable({
            "bPaginate": false,
            "bLengthChange": false,
            "bFilter": false,
            "bInfo": false,
            "bDestroy": true,
            "pageLength": 10,
            "bRetrieve": true,
            "bSortCellsTop": false,
            "displayLength": 10,
            "bAutoWidth": false
        });

    };

    $('#importDataFromOscarBtn').click(function (event) {
        event.preventDefault();
        folderHierarchy = [];
        var folderId = $('li[id=' + selectedTestCaseID + ']').parent().attr('parent')
        folderHierarchy.push(folderId);
        while (folderId != 'root-node') {
            folderId = $('li[id=' + folderId + ']').parent().attr('parent');
            folderHierarchy.push(folderId);
        }
        localStorage.folderHierarchy = folderHierarchy;
        if (typeof selectedFolderID == "undefined" || typeof selectedFolderID == "root") {
            selectedFolderID = "root-node";
            window.location.replace(baseUrl + 'secure/OscarIntegrationView.jspa?testcase=' + selectedTestCaseID + '-test-case' + '?folderid=' + selectedFolderID + '-folder-id');
        } else {
            window.location.replace(baseUrl + 'secure/OscarIntegrationView.jspa?testcase=' + selectedTestCaseID + '-test-case' + '?folderid=' + selectedFolderID + '-folder-id');
        }
    });

    $('#generate-testdata-ai-btn').click(function (event) {
        event.preventDefault();
    });


    function loadScriptDetails(testCaseId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-data-management',
            async: false,
            data: {
                user: username,
                action: 'get_test_case_scripts',
                data: JSON.stringify({
                    test_case_id: testCaseId
                })
            },
            success: function (response) {
                loadScriptDataToTable(response);

                if (response.length > 0) {
                    if (typeof scriptDataTable == "undefined") {
                        initializeScriptsDataTable();
                    }

                } else {
                    if (typeof scriptDataTable == "undefined") {
                        initializeScriptsDataTable();
                    }
                    scriptDataTable.fnClearTable();
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Scripts..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function loadScriptDataToTable(response) {

        $("#script-table-body tr").remove();
        var scriptTableBody = $('#script-table-body');

        for (var k = 0; k < response.length; k++) {

            var scriptObj = response[k];
            var scriptId = scriptObj.scriptId;

            var scriptTableBody_TR = $('<tr>', {
                id: scriptId
            });

            var scriptName = scriptObj.scriptName;
            var scriptData = scriptObj.scriptData;

            var scriptTableBody_ScriptId_TR_TD = $('<td>', {
                'id': 'scriptId',
            });

            scriptTableBody_ScriptId_TR_TD.text(scriptId);

            var scriptTableBody_ScriptName_TR_TD = $('<td>', {
                'id': 'scriptName'
            });

            scriptTableBody_ScriptName_TR_TD.text(scriptName);
            scriptTableBody_TR.append(scriptTableBody_ScriptId_TR_TD);
            scriptTableBody_TR.append(scriptTableBody_ScriptName_TR_TD);
            scriptTableBody.append(scriptTableBody_TR);
        }
    }

    function initializeScriptsDataTable() {

        scriptDataTable = $('#script-table').dataTable({
            "bPaginate": false,
            "bLengthChange": false,
            "bFilter": false,
            "bInfo": false,
            "bDestroy": true,
            "bRetrieve": true,
            "bAutoWidth": true,
            "aoColumns": [
                { "sWidth": "40%" },
                { "sWidth": "60%" }
            ]
        });
    }

    if (window.location.href.indexOf('TestCaseDesigningView.jspa') >= 0) {
        init('null');
        initBC('null');
    }

    function validateInput(inputValue) {
        if (inputValue != '') {
            var specialChars = "<>@!#$%^&*()+[]{}?:;|'\"\\,./~`-=";
            for (var i = 0; i < specialChars.length; i++) {
                if (inputValue.indexOf(specialChars[i]) > -1) {
                    AJS.flag({
                        type: 'error',
                        title: 'Special Characters Are Not Allowed..',
                        close: 'auto'
                    });
                    return true;
                }
            }
            return false;
        }
    }

    $('#newfolder-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed In..',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });
    $('#new-folder-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });
    $('#new-test-case-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });
    $('#new-bc-folder-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed In..',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });

    $('#bc-newfolder-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed In..',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });
    $('#new-bc-businessfunction-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed In..',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });
    $('#bc-new-businessfunction-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });

    $('#new-testcase-name').keyup(function (e) {
        if (e.which === 32) {
            AJS.flag({
                type: 'error',
                title: 'No Spaces Are Allowed',
                close: 'auto'
            });
            var str = $(this).val();
            str = str.replace(/\s/g, '');
            $(this).val(str);
        }
    }).blur(function () {
        var str = $(this).val();
        str = str.replace(/\s/g, '');
        $(this).val(str);
    });

    function validateCreation(inputValue, dataList) {
        if (typeof dataList != "undefined") {
            for (var j = 0; j < dataList.length; j++) {
                if (dataList[j] == inputValue) {
                    AJS.flag({
                        type: 'error',
                        title: 'Already Exist..',
                        close: 'auto'
                    });
                    return true;
                }
            }
            return false;
        }
    }

    function getAllData() {

        createdFolderList = [];
        createdTestCasesList = [];
        createdBCFolderList = [];
        createdBusinessScriptList = [];

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-management',
            async: false,
            data: {
                user: username,
                action: 'get-all',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                var getAllContent = JSON.parse(response);
                var folderList = getAllContent.folders;
                var testCasesList = getAllContent.testcases;
                var bcfolderList = getAllContent.bcfolders;
                var bcscriptList = getAllContent.bcscript;

                for (var i = 0; i < folderList.length; i++) {
                    createdFolderList.push(folderList[i]);
                }
                for (var j = 0; j < testCasesList.length; j++) {
                    createdTestCasesList.push(testCasesList[j]);
                }
                for (var k = 0; k < bcfolderList.length; k++) {
                    createdBCFolderList.push(bcfolderList[k]);
                }
                for (var l = 0; l < bcscriptList.length; l++) {
                    createdBusinessScriptList.push(bcscriptList[l]);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Validating The Exist Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function loadDataFromOscarRoute(oscarRedirectedFolderID, oscarRedirectedTestCaseID) {
        if (oscarRedirectedFolderID == "root") {
            oscarRedirectedFolderID = "root-node"
        }

        var childUL = $('ul[parent=' + oscarRedirectedFolderID + ']');
        if (childUL.length !== 0) {
            if (childUL.css('display') === 'block') {
                if (oscarRedirectedFolderID == "root-node") {
                    childUL.css('display', 'block');
                } else {
                    childUL.css('display', 'none');
                }
            } else {
                if (oscarRedirectedFolderID === 'root-node') {
                    init('null');
                } else {
                    init(oscarRedirectedFolderID);
                }
                childUL.css('display', 'block');
            }
        }
        $('#test-case-details-page-panel').attr('style', 'display: none;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: block;');

        selectedTestCaseID = oscarRedirectedTestCaseID;

        // Load test case into view..
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                entity: 'TestCase',
                action: 'get',
                data: JSON.stringify({
                    'id': selectedTestCaseID
                })
            },
            success: function (response) {
                var response = JSON.parse(response);
                $('span#selected-test-case-titile').text(response.name);
                $('input#test-case-name').val(response.name);
                $('textarea#test-case-description').val(response.description);
                $('textarea#test-case-overall-expected-result').val(response.overallExpectedResult);
                if (response.manual == 'Yes') {
                    $('#manualYes').prop('checked', true)
                } else {
                    $('#manualNo').prop('checked', true)
                }
                if (response.automated == 'Yes') {
                    $('#automatedYes').prop('checked', true);
                } else {
                    $('#automatedNo').prop('checked', true);
                }
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-step',
                    async: false,
                    data: {
                        user: username,
                        action: 'get',
                        data: JSON.stringify({
                            'testCaseId': selectedTestCaseID
                        })
                    },
                    success: function (stepsResponse) {
                        var steps = JSON.parse(stepsResponse);
                        var stepsArray = [];
                        if (steps.steps.length !== 0) {
                            for (var i = 0; i < steps.steps.length; i++) {
                                var stepObject = [];
                                stepObject[0] = steps.steps[i].step;
                                stepObject[1] = steps.steps[i].expectedResult;
                                stepObject[2] = steps.steps[i].data;

                                stepsArray.push(stepObject);
                            }
                        } else {
                            stepsArray.push(
                                [
                                    "",
                                    "",
                                    ""
                                ]
                            )
                        }

                        $('#steps-excel-view').jexcel('setData', stepsArray);
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Loading The Steps..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        $('#test-case-details-page-panel').attr('style', 'display: block;');
        $('#test-case-details-page-panel-empty').attr('style', 'display: none;');

        if (oscarRedirectedFolderID != 'root-node') {
            oscarRedirectedFolderID = oscarRedirectedFolderID + '-test-case-folder';
        } else {
            oscarRedirectedFolderID = oscarRedirectedFolderID;
        }

        var folderHierarchyList = new Array();

        if (oscarRedirectedFolderID != 'root-node') {
            var folderHierarchyLength = localStorage.folderHierarchy.split(',').length;
            for (var i = 0; i < folderHierarchyLength; i++) {
                folderHierarchyList[i] = localStorage.folderHierarchy.split(',')[i];
            }

            for (var i = folderHierarchyLength - 2; i >= 0; i--) {
                var folderName = folderHierarchyList[i];
                $('li[id=' + folderName + ']').click();
            }

            $('li[id=' + oscarRedirectedTestCaseID + '-test-case]').click();

        } else {
            $('li[id=' + oscarRedirectedTestCaseID + '-test-case]').click()
        }
    }

    /**=================Business component section====================== */
    $('#bc-addFolderLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-bc-folder-dialog').show();
    });
    $('#cancel-bc-folder-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-bc-folder-dialog').hide();
    });
    //add-bc-businessfunction-dialog
    $('#bc-addBusinessFunctionLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-bc-businessfunction-dialog').show();
    });
    $('#cancel-bc-businessfunction-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#add-bc-businessfunction-dialog').hide();
    });

    $('#create-bc-folder-btn').click((event) => {
        event.preventDefault();
        getAllData();
        var validateSuccess = false;
        var ajaxData;
        if (selectedFolderID === 'bcroot') {
            if (!validateInput($('#new-bc-folder-name').val())) {
                if (!validateCreation($('#new-bc-folder-name').val(), createdBCFolderList)) {
                    ajaxData = JSON.stringify({
                        'name': $('#new-bc-folder-name').val(),
                        'description': $('#new-bc-folder-description').val(),
                        'parent': 'null'
                    });
                    validateSuccess = true;
                }
            }
        } else {
            if (!validateInput($('#new-bc-folder-name').val())) {
                if (!validateCreation($('#new-bc-folder-name').val(), createdBCFolderList)) {
                    ajaxData = JSON.stringify({
                        'name': $('#new-bc-folder-name').val(),
                        'description': $('#new-bc-folder-description').val(),
                        'parent': selectedFolderID.split('-')[0]
                    });
                    validateSuccess = true;
                }
            }
        }
        if (validateSuccess) {
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/bcomponent',
                async: false,
                data: {
                    user: username,
                    entity: 'BusComFolder',
                    action: 'insert_business_folder',
                    data: ajaxData
                },
                success: function (response) {
                    $('#new-bc-folder-name').val('');
                    $('#new-bc-folder-description').val('');
                    var folderObject = JSON.parse(response);
                    if (folderObject.parent === 'null') {
                        appendBusinessComponentChild($('ul[parent="bcroot-node"]'), folderObject);
                    } else {
                        appendBusinessComponentChild($('ul[parent="' + folderObject.parent + '-business-component-folder"]'), folderObject);
                    }
                    AJS.dialog2('#add-bc-folder-dialog').hide();
                    AJS.flag({
                        type: 'success',
                        title: 'New Folder created..',
                        close: 'auto'
                    });
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Inserting The Business Folder..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        }
    });
    $('#create-bc-businessfunction-btn').click(() => {
        var businessScriptName = $('#new-bc-businessfunction-name').val();
        event.preventDefault();
        getAllData();
        var validateSuccess = false;
        var ajaxData;
        if (!validateInput($('#new-bc-businessfunction-name').val())) {
            if (!validateCreation($('#new-bc-businessfunction-name').val(), createdBusinessScriptList)) {
                ajaxData = {
                    'name': businessScriptName
                };
                validateSuccess = true;
            }
        }
        if (validateSuccess) {
            if (selectedFolderID === 'bcroot') {
                ajaxData.parent = 0;
            } else {
                var id = parseInt(selectedFolderID.split('-')[0]);
                if (typeof id === 'undefined') {
                    ajaxData.parent = 'err';
                } else {
                    ajaxData.parent = id;
                }

            }
            if (ajaxData.parent !== 'err') {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/bcomponent',
                    async: false,
                    data: {
                        user: username,
                        entity: 'BusComScript',
                        action: 'insert_business_script',
                        data: JSON.stringify(ajaxData)
                    },
                    success: function (response) {
                        var response = JSON.parse(response);
                        if (response.id) {
                            $('#new-bc-businessfunction-name').val('');
                            if (response.parent === '0') {
                                appendBusinessScriptChild($('ul[parent="bcroot-node"]'), response);
                            } else {
                                appendBusinessScriptChild($('ul[parent="' + response.parent + '-business-component-folder"]'), response);
                            }
                            AJS.flag({
                                type: 'success',
                                title: 'New Business Script created..',
                                close: 'auto'
                            });
                            AJS.dialog2('#add-bc-businessfunction-dialog').hide();
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Inserting The Business Script..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        }

    })
    function initBC(parent) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/bcomponent',
            async: false,
            data: {
                user: username,
                entity: 'BusComFolder',
                action: 'get_business_folders',
                data: JSON.stringify({
                    'parent': parent,
                    'redirectView': '/secure/TestCaseDesigningView.jspa'
                })
            },
            success: function (response) {
                var returnTreeItems = JSON.parse(response);
                if (parent === 'null') {
                    $('ul[parent="bcroot-node"]').empty();
                } else {
                    $('ul[parent="' + parent + '-business-component-folder"]').empty();
                }
                for (var i = 0; i < returnTreeItems.folders.length; i++) {
                    var folderObject = returnTreeItems.folders[i];
                    if (folderObject.parent === 'null') {
                        appendBusinessComponentChild($('ul[parent="bcroot-node"]'), folderObject);
                    } else {
                        appendBusinessComponentChild($('ul[parent="' + folderObject.parent + '-business-component-folder"]'), folderObject);
                    }
                }
                var businessScripts = JSON.parse(returnTreeItems.businessScripts);
                for (var i = 0; i < businessScripts.length; i++) {
                    if (businessScripts[i].parent === '0' || businessScripts[i].parent == "null") {
                        appendBusinessScriptChild($('ul[parent="bcroot-node"]'), businessScripts[i]);
                    } else {
                        appendBusinessScriptChild($('ul[parent="' + businessScripts[i].parent + '-business-component-folder"]'), businessScripts[i]);
                    }
                }
            },
            error: function (jqXHR, exception) {
                var responseObject = JSON.parse(jqXHR.responseText);
                if (jqXHR.status === 302) {
                    if (responseObject.redirect) {
                        window.location.replace(responseObject.redirect);
                        return;
                    }
                }
            }
        });

    }
    $('#bc-steps-excel-view').jexcel({
        colHeaders: [
            'Step Description',
            'Expected Result',
            'Data'
        ],
        colAlignments: [
            'left',
            'left',
            'left'
        ],
        minSpareRows: 5,
        allowInsertColumn: false,
        data: []
    });
    function appendBusinessComponentChild(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-business-component-folder',
            'type': 'bc-folder',
        });
        var aObject = $('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-folder'
        });
        var nameSpan = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        nameSpan.text(child.name);

        aObject.append(iconSpan);
        aObject.append(nameSpan);

        liObject.append(aObject);

        liObject.click(function (event) {
            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            this.classList.toggle('active');
            selectedFolderID = $(event.currentTarget).attr('id');
            var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
            if (childUL.length !== 0) {
                if (childUL.css('display') === 'block') {
                    childUL.css('display', 'none');
                } else {
                    initBC($(event.currentTarget).attr('id').split('-')[0]);
                    childUL.css('display', 'block');
                }
            }
            $('#test-case-details-page-panel').attr('style', 'display: none;');
            $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
        });

        var ulObject = $('<ul>', {
            'class': 'aui-nav',
            'parent': child.id + '-business-component-folder',
            'style': 'display: none'
        });

        parent.append(liObject);
        parent.append(ulObject);

    }

    function appendBusinessScriptChild(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-business-script',
            'type': 'business-script',
        });

        var aObject = $('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-bullet-list'
        });
        var nameSpan = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        nameSpan.text(child.name);

        aObject.append(iconSpan);
        aObject.append(nameSpan);

        liObject.append(aObject);

        liObject.click(function (event) {

            $(folderNodeSelector).removeClass('active');
            $(testCaseNodeSelector).removeClass('active');
            $(businessFolderNodeSelector).removeClass('active');
            $(businessScriptNodeSelector).removeClass('active');
            this.classList.toggle("active");
            selectedTestCaseID = $(event.currentTarget).attr('id');
            window.location.hash = '#!' + selectedTestCaseID;
            $('span#selected-business-Script-titile').text($(event.currentTarget).children().children("span[class='aui-nav-item-label']")[0].innerHTML);
            selectedFolderID = $(event.currentTarget).parent().attr('parent');
            $('#test-case-details-page-panel').attr('style', 'display: block;');
            $('#test-case-details-page-panel-empty').attr('style', 'display: none;');          
            $('#bc-steps-table-content > div > div.table-content > table > tbody > tr > td > textarea').css({height:"30px"});
            testcaseSection.css({ display: "none" });
            businessSection.css({ display: "block" });
        });

        parent.append(liObject);
    }

    $('#bc-renameFolderLI').click(() => {
        $('#bc-newfolder-name').val(selectedFolderNameToRename);
        AJS.dialog2('#bc-rename-folder-dialog').show();
    })
    $('#bc-folder-newname-save-button').click(() => {
        var new_FolderName = $('#bc-newfolder-name').val();
        getAllData();
        if (!validateInput($('#bc-newfolder-name').val())) {
            if (!validateCreation(new_FolderName, createdBCFolderList)) {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/bcomponent',
                    async: false,
                    data: {
                        user: username,
                        entity: 'BusComFolder',
                        action: 'update_business_folder',
                        data: JSON.stringify({
                            'parent': parentName,
                            'oldname': selectedFolderNameToRename,
                            'newname': new_FolderName
                        })
                    },
                    success: function (response) {
                        var response = JSON.parse(response);
                        if (response === 'true') {
                            $('#bc-newfolder-name').val('')
                            AJS.dialog2('#bc-rename-folder-dialog').hide();
                            $('li#' + selectedFolderID + '-business-component-folder .aui-nav-item-label').text(new_FolderName);
                            AJS.flag({
                                type: 'success',
                                title: 'Folder updated..',
                                close: 'auto'
                            });
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Updating The Business Folder..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        } else {
            AJS.flag({
                type: 'error',
                title: 'Folder Name Should Not Be Empty..',
                close: 'auto'
            });
        }
    });
    $('#renameBusinessScriptLi').click((e) => {
        e.preventDefault();
        $('#bc-new-businessfunction-name').val(selectedBusinessScriptToRename);
        AJS.dialog2('#bc-rename-businessfunction-dialog').show();
    })

    $('#bc-businessfunction-newname-save-button').click((e) => {
        e.preventDefault();
        var new_Businessfunction_Name = $('#bc-new-businessfunction-name').val();
        getAllData();
        if (!validateInput($('#bc-new-businessfunction-name').val())) {
            if (!validateCreation(new_Businessfunction_Name, createdBusinessScriptList)) {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/bcomponent',
                    async: false,
                    data: {
                        user: username,
                        entity: 'BusComScript',
                        action: 'update_business_script',
                        data: JSON.stringify({
                            'id': selectedBusinessFunctionID.split('-')[0],
                            'newname': new_Businessfunction_Name,
                            'oldname': selectedBusinessScriptToRename
                        })
                    },
                    success: function (response) {
                        $('#bc-new-businessfunction-name').val('');
                        AJS.dialog2("#bc-rename-businessfunction-dialog").hide();
                        $('li#' + selectedBusinessFunctionID + ' .aui-nav-item-label').text(new_Businessfunction_Name);
                        $('#selected-business-Script-titile').text(new_Businessfunction_Name);
                        AJS.flag({
                            type: 'success',
                            title: 'Business Script Updated..',
                            close: 'auto'
                        });
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Updating The Business Script..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        }
    });

    $('#bc-deleteFolderLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#warning-dialog-bcfolder').show();
    });

    $('#bcfolder-delete-cancel').on('click', function () {
        AJS.dialog2('#warning-dialog-bcfolder').hide();
    });

    $('#bcfolder-delete-confirm').on('click', (e) => {
        e.preventDefault();
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/bcomponent',
            async: false,
            data: {
                user: username,
                action: 'delete_business_folder',
                data: JSON.stringify({
                    parent: parentName,
                    parentfor: selectedFolderID
                })
            },
            success: (resp) => {
                $('li#' + selectedFolderID + '-business-component-folder').remove();
                $('ul[parent="' + selectedFolderID + '-business-component-folder"]').remove();
                $('#bc-section').attr('style', 'display: none;');
                $('#test-case-details-page-panel').attr('style', 'display: none;');
                $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
                AJS.dialog2('#warning-dialog-bcfolder').hide();
                AJS.flag({
                    type: 'success',
                    title: 'Folder deleted..',
                    close: 'auto'
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Business Folder..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('#deleteBusinessScriptLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#warning-dialog-business-script').show();
    });

    $('#business-script-delete-cancel').on('click', function () {
        AJS.dialog2('#warning-dialog-business-script').hide();
    });

    $('#business-script-delete-confirm').click(() => {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/bcomponent',
            async: false,
            data: {
                user: username,
                action: 'delete_business_script',
                data: JSON.stringify({
                    'id': selectedBusinessFunctionID.split('-')[0]
                })
            }
            , success: (resp) => {
                $('li#' + selectedBusinessFunctionID).remove();
                AJS.flag({
                    type: 'success',
                    title: 'Business Script deleted..',
                    close: 'auto'
                });
                AJS.dialog2('#warning-dialog-business-script').hide();
                businessSection.css({ display: "none" });
                $('#test-case-details-page-panel').attr('style', 'display: none;');
                $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
            }, error: (jqXHR, textStatus, errorThrown) => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Business Script..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $("#sortable").sortable();

    $("#droppable").droppable({
        drop: function (event, ui) {
            var $sortEl = $('<div>').html(ui.draggable.html());
            var TR = $('<tr>');
            var spanIcon = $('<span>', {
                'class': 'aui-icon aui-icon-small aui-iconfont-cross-circle'
            });
            var remove_Bdt_Btn = $('<button>', {
                'class': 'remove-bdt'
            });
            var remove_Row_TD = $('<td>', {
                'style': 'width:2%;padding-top: 1.5%;'
            });
            var input_TD = $('<td>');
            var inputInfo = $('<input>', {
                'class': 'jira-bdt-form-ctrl'
            });
            remove_Bdt_Btn.append(spanIcon);
            remove_Row_TD.append(remove_Bdt_Btn);
            inputInfo.val($sortEl[0].innerText);
            input_TD.append(inputInfo);
            TR.append(remove_Row_TD);
            TR.append(input_TD);
            $(this).children('tbody').append(TR);


            $('.remove-bdt').live('click', function (e) {
                e.preventDefault();
                $(this).closest('tr').remove();
            }
            );
        }
    });

    $("li.menu-item a").click(function (event) {
        if ($(this).attr("href") == "#business-component-tab") {
            $('#test-case-details-page-panel').attr('style', 'display: none;');
            $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
        }
        else if ($(this).attr("href") == "#testcase-tab") {
            $('#test-case-details-page-panel').attr('style', 'display: none;');
            $('#test-case-details-page-panel-empty').attr('style', 'display: block;');
        }
    });

})(jQuery);
