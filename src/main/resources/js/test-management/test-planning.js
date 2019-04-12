AJS.toInit(function ($) {

    'use strict';

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');

    var tesPlanFolderSelector = 'li[type="test-plan-folder"]';

    var host = 'cmlvdhhewapathi';
    var port = '8080';

    var centroidHost;
    var centroidUrl;
    var centroidPort;

    var selectedFolderTestplan;
    var createdTestPlanList = new Array();

    function initTestPlan(parent) {
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
                    'redirectView': '/secure/TestPlanningView.jspa'
                })
            },
            success: function (response) {
                var returnTreeItems = JSON.parse(response);
                if (parent === 'null') {
                    $('ul[parent="test-plan-root-node"]').empty();
                } else {
                    $('ul[parent="' + parent + '-test-plan-test-case-folder"]').empty();
                }
                for (var i = 0; i < returnTreeItems.folders.length; i++) {
                    var folderObject = returnTreeItems.folders[i];
                    if (folderObject.parent === 'null') {
                        appendTPChild($('ul[parent="test-plan-root-node"]'), folderObject);
                    } else {
                        appendTPChild($('ul[parent="' + folderObject.parent + '-test-plan-test-case-folder"]'), folderObject);
                    }
                }
                var testcases = JSON.parse(returnTreeItems.testcases);
                $('#selected-folder-ul').empty();
                for (var i = 0; i < testcases.length; i++) {
                    appendTestCaseNodes($('#selected-folder-ul'), testcases[i]);
                }
            }, error: function (jqXHR, exception) {
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

    function appendTPChild(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-test-plan-test-case-folder',
            'type': 'test-plan-folder',
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
            $(tesPlanFolderSelector).removeClass('active');
            this.classList.toggle('active');
            selectedFolderTestplan = $(event.currentTarget).attr('id');
            var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
            if (childUL.length !== 0) {
                if (childUL.css('display') === 'block') {
                    childUL.css('display', 'none');
                } else {
                    initTestPlan($(event.currentTarget).attr('id').split('-')[0]);
                    childUL.css('display', 'block');
                }
            }
        });

        var ulObject = $('<ul>', {
            'class': 'aui-nav',
            'parent': child.id + '-test-plan-test-case-folder',
            'style': 'display: none'
        });

        parent.append(liObject);
        parent.append(ulObject);

    }

    function appendTestCaseNodes(parent, child) {

        var testCaseLi = $('<li>', {
            'id': child.id + '-test-plan-test-case'
        });

        var testCaseA = $('<a>', {
            'class': 'aui-nav-item'
        });
        var testCaseSpanIcon = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-bullet-list'
        });
        var testCaseSpanName = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        testCaseSpanName.text(child.name);

        testCaseA.append(testCaseSpanIcon);
        testCaseA.append(testCaseSpanName);

        testCaseLi.append(testCaseA);

        parent.append(testCaseLi);

    }

    function appendSelectedTCtoTable(parent, child) {
        var isTestCaseSelected = true;
        var count = 0;
        var id = child.attr('id').split('-')[0];
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                entity: 'TestCase',
                action: 'get',
                data: JSON.stringify({
                    'id': id
                })
            },
            success: function (response) {
                var response = JSON.parse(response);
                loadSelectedTC(parent, response, isTestCaseSelected, count, false, 0);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Cases..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    };

    function loadSelectedTC(parent, response, isRun, count, planExecuted, planCount) {

        var trObject = $('<tr>', {
            'id': response.id + '-test-plan-row'
        });

        var tdNameObject = $('<td>', {
            'style': 'vertical-align: middle;'
        });
        tdNameObject.text(response.name);

        var tdAutomatedObject = $('<td>', {
            'style': 'vertical-align: middle; text-align: center;'
        });
        var tdAutomatedInput = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': response.id + '-test-plan-automated-check',
            'id': response.id + '-test-plan-automated-check',
            'style': 'margin: auto;',
            'disabled': ''
        });
        tdAutomatedObject.append(tdAutomatedInput);

        var tdManualObject = $('<td>', {
            'style': 'vertical-align: middle; text-align: center;'
        });
        var tdManualInput = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': response.id + '-test-plan-manual-check',
            'id': response.id + '-test-plan-manual-check',
            'style': 'margin: auto;',
            'disabled': ''
        });
        tdManualObject.append(tdManualInput);

        var tdAgentObject = $('<td>');
        var tdAgentSelect = $('<select>', {
            'class': 'select form-control',
            'id': response.id + '-test-plan-agent-select',
            'name': response.id + '-test-plan-agent-select'
        });

        var testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option.text('Select');
        tdAgentSelect.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option);

        // Agents
        if (response.automated == 'Yes') {
            var isExecuted = false;
            count += 1;

            if (planExecuted) {
                var isExecuted = true;
            } else {
                if (isRun) {
                    if (count == 1) {
                        isExecuted = true;
                    }
                } else {
                    if (count == 1) {
                        isExecuted = true;
                    }
                }
            }

            var tdAgentSelect_Pool_Optgroup = $('<optgroup>', {
                'label': 'Pools'
            });

            var isEmptyPool = setPools(tdAgentSelect_Pool_Optgroup, isExecuted);
            tdAgentSelect.append(tdAgentSelect_Pool_Optgroup)

            var tdAgentSelect_Agent_Optgroup = $('<optgroup>', {
                'label': 'Agents'
            });

            var isEmptyAgents = setAgents(tdAgentSelect_Agent_Optgroup, isExecuted);
            tdAgentSelect.append(tdAgentSelect_Agent_Optgroup);

            if (planExecuted && planCount == 1 && typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                AJS.flag({
                    type: 'error',
                    title: 'Agent Unavailable',
                    close: 'auto'
                });
            }

            if (isRun && typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                AJS.flag({
                    type: 'error',
                    title: 'Agent Unavailable',
                    close: 'auto'
                });
            }
        }

        if (response.agent) {
            tdAgentSelect.val(response.agent);
        }

        var tdUserObject = $('<td>');
        var tdUserSelect = $('<select>', {
            'class': 'select form-control',
            'id': response.id + '-test-plan-user-select',
            'name': response.id + '-test-plan-user-select'
        })
        var tdUserSelectOption = $('<option>');
        tdUserSelectOption.text('Select');
        tdUserSelect.append(tdUserSelectOption);

        // found
        setUsers(tdUserSelect, response.user);

        var tdRemoveObject = $('<td>');

        var tdRemoveButton = $('<button>', {
            'class': 'aui-button'
        });
        var tdRemoveButtonSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-trash'
        });
        tdRemoveButton.append(tdRemoveButtonSpan);
        tdRemoveButton.click(function (event) {
            event.preventDefault();
            $(event.currentTarget).parent().parent().remove();
            // appendTestCaseNodes($('ul#selected-folder-ul'), response);
        });
        tdRemoveObject.append(tdRemoveButton);

        if (response.automated === 'Yes') {
            tdAutomatedInput.prop('checked', true);
            tdAgentObject.append(tdAgentSelect);
        } else {
            tdAutomatedInput.prop('checked', false);
        }

        if (response.manual === 'Yes') {
            tdManualInput.prop('checked', true);
            tdUserObject.append(tdUserSelect);
        } else {
            tdManualInput.prop('checked', false);
        }

        trObject.append(tdNameObject);
        trObject.append(tdAutomatedObject);
        trObject.append(tdManualObject);
        trObject.append(tdAgentObject);
        trObject.append(tdUserObject);
        trObject.append(tdRemoveObject);

        parent.append(trObject);
    }

    $(tesPlanFolderSelector).click(function (event) {
        $(tesPlanFolderSelector).removeClass('active');
        this.classList.toggle('active');
        selectedFolderTestplan = $(event.currentTarget).attr('id');
        var childUL = $('ul[parent=' + 'test-plan-' + $(event.currentTarget).attr('id') + ']');
        if (childUL.length !== 0) {
            if (childUL.css('display') === 'block') {
                childUL.css('display', 'none');
            } else {
                if ($(event.currentTarget).attr('id') === 'root-node') {
                    initTestPlan('null');
                } else {
                    initTestPlan($(event.currentTarget).attr('id').split('-')[0]);
                }
                childUL.css('display', 'block');
            }
        }
    });

    $('#new-test-plan-btn').click(function (event) {
        event.preventDefault();
        $('span#test-plan-modal-title').text('Add New Test Plan');
        $('#create-test-plan-btn').attr('style', 'visibility: visible;');
        $('#edit-test-plan-btn').attr('style', 'display: none;');
        AJS.dialog2('#add-test-plan-dialog').show();
    });

    $('#create-test-plan-btn').click(function (event) {
        event.preventDefault();
        if ($('#selected-tc-table-tbody').children().length != 0) {
            var testPlanName = $('#new-test-plan-name').val();
            getAllTestPlans();
            if (!validateInput($('#new-test-plan-name').val())) {
                if (!validateCreation(testPlanName, createdTestPlanList)) {
                    if (!testPlanName && testPlanName === '') {
                        $('#test-plan-name-error').attr('style', 'display: block;');
                        AJS.flag({
                            type: 'error',
                            title: 'Test Plan Name Should Not Be Empty..',
                            close: 'auto'
                        });
                        return;
                    } else {
                        $('#test-plan-name-error').attr('style', 'display: none;');
                    }
                    $.ajax({
                        type: 'POST',
                        url: baseUrl + 'plugins/servlet/test-plan-management',
                        async: false,
                        data: {
                            user: username,
                            entity: 'TestPlan',
                            action: 'insert',
                            data: JSON.stringify({
                                'name': $('#new-test-plan-name').val()
                            })
                        },
                        success: function (response) {
                            if ($('#selected-tc-table-tbody').children().length != 0) {
                                var ajexInputObject = {
                                    'testPlanId': JSON.parse(response).id,
                                    'testCases': []
                                }
                                $('#selected-tc-table-tbody').children().each(function () {
                                    var agentTD = $(this).children()[3];
                                    var selectedAgent = $(agentTD).children().val() !== 'Select' ? $(agentTD).children().val() : '';
                                    var userTD = $(this).children()[4];
                                    var selectedUser = $(userTD).children().val() !== 'Select' ? $(userTD).children().val() : '';
                                    ajexInputObject.testCases.push({
                                        'id': $(this).attr('id').split('-')[0],
                                        'agent': selectedAgent ? selectedAgent : '',
                                        'user': selectedUser ? selectedUser : '',
                                        'testAgentStatus': $('option:selected', agentTD).attr('agentstatus')

                                    });
                                });
                                $.ajax({
                                    type: 'POST',
                                    url: baseUrl + 'plugins/servlet/test-plan-tc',
                                    async: false,
                                    data: {
                                        user: username,
                                        entity: 'TestPlanTestCase',
                                        action: 'insert',
                                        data: JSON.stringify(ajexInputObject)
                                    },
                                    success: function (testPlanTestCaseresponse) {
                                        var testPlanTestCaseresponseObject = JSON.parse(testPlanTestCaseresponse)
                                        var responseObject = JSON.parse(response);
                                        responseObject.count = testPlanTestCaseresponseObject.length;
                                        responseObject.lastExecution = 'N/A';
                                        appendTestPlanChild(responseObject);
                                        $('#new-test-plan-name').val('');
                                        $('#selected-folder-ul').empty();
                                        $('#selected-tc-table-tbody').empty();
                                        AJS.flag({
                                            type: 'success',
                                            title: 'New Test Plan created..',
                                            close: 'auto'
                                        });
                                        AJS.dialog2('#add-test-plan-dialog').hide();
                                    }, error: function (jqXHR, textStatus, errorThrown) {
                                        AJS.flag({
                                            type: 'error',
                                            title: 'An Error Occurred Saving The Test Plan Test Case..',
                                            body: 'Contact Developer',
                                            close: 'auto'
                                        });
                                    }
                                });
                            }
                        }, error: function (jqXHR, textStatus, errorThrown) {
                            AJS.flag({
                                type: 'error',
                                title: 'An Error Occurred Saving The Test Plan..',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        }
                    });
                }
            }
        } else {
            AJS.flag({
                type: 'warning',
                title: 'Test Plan Creation',
                body: 'Atleast One Test Case Should Be Selected To Create A Test Plan.',
                close: 'auto'
            });
        }
    });

    $('#edit-test-plan-btn').click(function (event) {
        event.preventDefault();

        if ($('#selected-tc-table-tbody').children().length != 0) {
            var id = $('span#test-plan-modal-title').text().split('-')[$('span#test-plan-modal-title').text().split('-').length - 1].trim();

            var testPlanName = $('#new-test-plan-name').val();
            if (!testPlanName && testPlanName === '') {
                $('#test-plan-name-error').attr('style', 'display: block;');
                AJS.flag({
                    type: 'error',
                    title: 'Test Plan Name Should Not Be Empty..',
                    close: 'auto'
                });
                return;
            } else {
                $('#test-plan-name-error').attr('style', 'display: none;');
            }
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-plan-management',
                async: false,
                data: {
                    user: username,
                    entity: 'TestPlan',
                    action: 'update',
                    data: JSON.stringify({
                        'id': id,
                        'name': $('#new-test-plan-name').val()
                    })
                },
                success: function (response) {
                    if ($('#selected-tc-table-tbody').children().length != 0) {
                        var ajexInputObject = {
                            'testPlanId': id,
                            'testCases': []
                        }
                        $('#selected-tc-table-tbody').children().each(function () {
                            var agentTD = $(this).children()[3];
                            var selectedAgent = $(agentTD).children().val() !== 'Select' ? $(agentTD).children().val() : '';
                            var userTD = $(this).children()[4];
                            var selectedUser = $(userTD).children().val() !== 'Select' ? $(userTD).children().val() : '';
                            ajexInputObject.testCases.push({
                                'id': $(this).attr('id').split('-')[0],
                                'agent': selectedAgent ? selectedAgent : '',
                                'user': selectedUser ? selectedUser : '',
                                'testAgentStatus': $('option:selected', agentTD).attr('agentstatus')

                            });
                        });
                        $.ajax({
                            type: 'POST',
                            url: baseUrl + 'plugins/servlet/test-plan-tc',
                            async: false,
                            data: {
                                user: username,
                                entity: 'TestPlanTestCase',
                                action: 'update',
                                data: JSON.stringify(ajexInputObject)
                            },
                            success: function (testPlanTestCaseresponse) {
                                if (testPlanTestCaseresponse !== '') {
                                    var testPlanTestCaseresponseObject = JSON.parse(testPlanTestCaseresponse)
                                    var responseObject = JSON.parse(response);
                                    responseObject.count = testPlanTestCaseresponseObject.length;
                                    responseObject.lastExecution = 'N/A';
                                    $('table#test-plan-list-table-content').find('tr#' + responseObject.id).remove();
                                    appendTestPlanChild(responseObject);
                                    $('#new-test-plan-name').val('');
                                    $('#selected-folder-ul').empty();
                                    $('#selected-tc-table-tbody').empty();
                                    AJS.flag({
                                        type: 'success',
                                        title: 'Test Plan updated..',
                                        close: 'auto'
                                    });
                                    AJS.dialog2('#add-test-plan-dialog').hide();
                                }
                            }, error: function (jqXHR, textStatus, errorThrown) {
                                AJS.flag({
                                    type: 'error',
                                    title: 'An Error Occurred Updating The Test Plan Test Case..',
                                    body: 'Contact Developer',
                                    close: 'auto'
                                });
                            }
                        });
                    }
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Updating The Test Plan..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        } else {
            AJS.flag({
                type: 'warning',
                title: 'Test Plan Creation',
                body: 'Atleast One Test Case Should Be Selected To Create A Test Plan.',
                close: 'auto'
            });
        }
    });

    $('#cancel-test-plan-btn').click(function (event) {
        event.preventDefault();
        // $('#selected-testcases-ul').empty();
        $('#new-test-plan-name').val('');
        $('#selected-tc-table-tbody').empty();
        $('#selected-folder-ul').empty();
        AJS.dialog2('#add-test-plan-dialog').hide();
    });

    $('body').on('click', 'ul#selected-folder-ul li', function (event) {
        $(event.currentTarget).remove();
        appendSelectedTCtoTable($('#selected-tc-table-tbody'), $(event.currentTarget));
    });

    function init() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-plan-management',
            async: false,
            data: {
                user: username,
                entity: 'TestPlan',
                action: 'get',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                var availablePlan = JSON.parse(response);
                for (var i = 0; i < availablePlan.plans.length; i++) {
                    var planObject = availablePlan.plans[i];
                    appendTestPlanChild(planObject);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Plans..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function appendTestPlanChild(plan) {

        var trObject = $('<tr>', {
            'id': plan.id
        });

        var idTDObject = $('<td>', {
            'width': '5%'
        });
        idTDObject.text(plan.id);

        var nameTDObject = $('<td>', {
            'width': '30%'
        });

        var numTDAObject = $('<a>', {
            'style': 'cursor: pointer'
        });
        numTDAObject.text(plan.name);

        var numTCTDObject = $('<td>', {
            'width': '30%'
        });

        numTCTDObject.text(plan.count + ' Test Case(s)');
        numTDAObject.click(function (event) {
            event.preventDefault();
            var isRun = false;
            var count = 0;
            var planExecuted = true;
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-plan-management',
                async: false,
                data: {
                    user: username,
                    entity: 'TestPlan',
                    action: 'getPlan',
                    data: JSON.stringify({
                        'id': plan.id
                    })
                },
                success: function (response) {
                    var planCount = 1;
                    var responseObject = JSON.parse(response);
                    $('span#test-plan-modal-title').text('Edit Test Plan - ' + responseObject.id);
                    $('#create-test-plan-btn').attr('style', 'display: none;');
                    $('#edit-test-plan-btn').attr('style', 'visibility: visible;');
                    AJS.dialog2('#add-test-plan-dialog').show();
                    testPlanEdit(responseObject, isRun, count, planExecuted, planCount);
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Test Plan..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });

        })

        nameTDObject.append(numTDAObject);

        var lastExecTDObject = $('<td>', {
            'width': '25%'
        });
        lastExecTDObject.text(plan.lastExecution);

        var actionsTDObject = $('<td>', {
            'width': '10%'
        });
        var editButton = $('<button>', {
            'class': 'aui-button',
            'style': 'margin-right: 10px;'
        });
        var editButtonSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-edit-filled'
        });
        editButton.append(editButtonSpan);
        editButton.click(function (event) {
            event.preventDefault();
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-plan-management',
                async: false,
                data: {
                    user: username,
                    entity: 'TestPlan',
                    action: 'getPlan',
                    data: JSON.stringify({
                        'id': plan.id
                    })
                },
                success: function (response) {
                    var planExecuted = true;
                    var responseObject = JSON.parse(response);
                    $('span#test-plan-modal-title').text('Edit Test Plan - ' + responseObject.id);
                    $('#create-test-plan-btn').attr('style', 'display: none;');
                    $('#edit-test-plan-btn').attr('style', 'visibility: visible;');
                    AJS.dialog2('#add-test-plan-dialog').show();
                    testPlanEdit(responseObject, false, 0, planExecuted, 1);
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Edit Test Plan..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        });

        var deleteButton = $('<button>', {
            'class': 'aui-button'
        });
        var deleteButtonSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-trash'
        });
        deleteButton.append(deleteButtonSpan);
        deleteButton.click(function (event) {
            event.preventDefault();
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/test-plan-management',
                async: false,
                data: {
                    user: username,
                    entity: 'TestPlan',
                    action: 'delete',
                    data: JSON.stringify({
                        'id': plan.id
                    })
                },
                success: function (response) {
                    if (response === 'true') {
                        $('tr#' + plan.id).remove();
                        AJS.flag({
                            type: 'success',
                            title: 'Test Plan deleted..',
                            close: 'auto'
                        });
                    }
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Deleting The Test Plan..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        });

        actionsTDObject.append(editButton);
        actionsTDObject.append(deleteButton);

        trObject.append(idTDObject);
        trObject.append(nameTDObject);
        trObject.append(numTCTDObject);
        trObject.append(lastExecTDObject);
        trObject.append(actionsTDObject);

        $('table#test-plan-list-table-content').append(trObject);
    }

    function testPlanEdit(plan, isRun, count, planExecuted, planCount) {
        $('#new-test-plan-name').val(plan.name);
        for (var i = 0; i < plan.testCases.length; i++) {
            var testCase = plan.testCases[i].testCase;
            testCase.agent = plan.testCases[i].agent;
            testCase.user = plan.testCases[i].user;
            if (i == plan.testCases.length - 1) {
                loadSelectedTC($('#selected-tc-table-tbody'), testCase, isRun, count, planExecuted, planCount);
            } else {
                loadSelectedTC($('#selected-tc-table-tbody'), testCase, isRun, count, planExecuted, 0);
            }
        }
    }

    function setUsers(selectElement, user) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-management-util',
            async: false,
            data: {
                user: username,
                action: 'get-users',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                for (var i = 0; i < response.length; i++) {
                    var responseObject = response[i];
                    var tdUserSelectOption = $('<option>', {
                        'id': responseObject.id,
                        'value': responseObject.username
                    });
                    tdUserSelectOption.text(responseObject.displayName + '( ' + responseObject.emailAddress + ' )');
                    selectElement.append(tdUserSelectOption);
                }
                if (user) {
                    selectElement.val(user);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Users..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function setPools(selectElement, isExecuted) {
        var isEmptyPool = false;
        $.ajax({
            type: 'GET',
            url: 'http://' + centroidHost + ':' + centroidPort + '/' + centroidUrl + "/browseagentpool",
            async: false,
            success: function (response) {
                var responseAgentPool = JSON.parse(response);
                if (responseAgentPool.length > 0) {
                    for (var i = 0; i < responseAgentPool.length; i++) {
                        var poolObject = $('<option>', {
                            id: responseAgentPool[i].agent_pool_id,
                            value: responseAgentPool[i].agent_pool_id,
                            agentstatus: true
                        });
                        poolObject.text(responseAgentPool[i].name);
                        selectElement.append(poolObject);
                    }
                } else {
                    if (isExecuted) {
                        isEmptyPool = true;
                    }
                    var poolObject = $('<option>');
                    poolObject.attr("disabled", "disabled");
                    poolObject.text("Unavailable");
                    selectElement.append(poolObject);
                }
            },
            error: function () {
                if (isExecuted) {
                    isEmptyPool = true;
                }
                var poolObject = $('<option>');
                poolObject.attr("disabled", "disabled");
                poolObject.text("Unavailable");
                selectElement.append(poolObject);
            }
        });
        return isEmptyPool;
    }

    function setAgents(selectElement, isExecuted) {
        var isEmptyAgents = false;
        $.ajax({
            type: 'GET',
            url: 'http://' + centroidHost + ':' + centroidPort + '/' + centroidUrl + "/browseagent",
            async: false,
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (responseObject.length > 0) {
                    for (var i = 0; i < responseObject.length; i++) {
                        if (responseObject[i].active) {
                            var agentObject = $('<option>', {
                                id: responseObject[i].agentid,
                                value: responseObject[i].agentid,
                                agentstatus: false
                            });
                            agentObject.text(responseObject[i].name);
                            selectElement.append(agentObject);
                        }
                    }
                } else {
                    if (isExecuted) {
                        isEmptyAgents = true;
                    }
                    var poolObject = $('<option>');
                    poolObject.attr("disabled", "disabled");
                    poolObject.text("Unavailable");
                    selectElement.append(poolObject);
                }
            },
            error: function () {
                isEmptyAgents = true;
                var poolObject = $('<option>');
                poolObject.attr("disabled", "disabled");
                poolObject.text("Unavailable");
                selectElement.append(poolObject);
            }
        });
        return isEmptyAgents;
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
        } else {
            AJS.flag({
                type: 'error',
                title: 'Folder Name Should Not Be Empty..',
                close: 'auto'
            });
            return true;
        }
    }

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

    function getAllTestPlans() {
        createdTestPlanList = [];
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-plan-management',
            async: false,
            data: {
                user: username,
                entity: 'TestPlan',
                action: 'get',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                var availablePlan = JSON.parse(response);
                for (var i = 0; i < availablePlan.plans.length; i++) {
                    createdTestPlanList.push(availablePlan.plans[i].name);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Plan List..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    if (window.location.href.indexOf('TestPlanningView.jspa') >= 0) {
        $(function () {
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
                    centroidHost = response.host
                    centroidUrl = response.baseurl
                    centroidPort = response.port
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Configurations..',
                        close: 'auto'
                    });
                }
            });
        });
        initTestPlan('null');
        init();
    }

    $('#new-test-plan-name').keyup(function (e) {
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


})(jQuery);