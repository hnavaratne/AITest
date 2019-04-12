AJS.toInit(function ($) {

    'use strict';

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');

    var tableHEADBGColor = '#f4f5f7';
    var tableHEADColor = '#000';

    var testPlanId;
    var selectTestExecTFId;
    var selectedBrowserForTestPlan;
    var isValidated = false;
    var parallelExecutionStatus = true;
    var selectedTestCasesCount = 0;
    var selectorElementVersion = "";
    var selectedEditEntityInstanceId = "";
    var selectedEditScheduleName = "";
    var $scheduleContext = $("#scheduleContext");
    var scheduleNodeSelector = 'ul[type="schedule"]';
    var treeNodeATagSelector = 'ul.aui-nav li > a.aui-nav-item';
    var selectedScheduleID = '';
    var status = '';

    var toggle = $('#schedule-parallel-execution');
    toggle.click('change', function (e) {
        var isChecked = toggle.prop("checked");
        if (isChecked) {
            parallelExecutionStatus = true;
        } else {
            parallelExecutionStatus = false;
        }
    });

    $('html').click(function () {
        $scheduleContext.hide();
    });

    if (window.location.href.indexOf('ScheduleView.jspa') >= 0) {
        initScheduleList();
        $('#schedule-view-header').text("SCHEDULED EXECUTIONS");
    }

    function initScheduleList() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            data: {
                user: username,
                action: 'get-all-schedules',
                data: JSON.stringify({
                    'redirectView': '/secure/ScheduleView.jspa'
                })
            },
            success: function (response) {
                var scheduleList = JSON.parse(response);
                for (var i = 0; i < scheduleList.length; i++) {
                    var scheduleObj = scheduleList[i];
                    appendScheduleNodes($('#test-schedule-ul'), scheduleObj);
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

    function appendScheduleNodes(parent, schedule) {
        var scheduleLi = $('<li>', {
            'id': schedule.ExecEntityInstanceId + '-test-schedule-test-plan',
            'style': 'border-bottom: 1px solid #6b778c40;',
            'status': schedule.Status
        });

        var scheduleA = $('<a>', {
            'class': 'aui-nav-item',
            'style': 'display: flex; padding: 12px;'
        });

        var scheduleIconSpan = $('<span>', {
            'id': schedule.ExecEntityInstanceId + '-test-schedule-test-plan',
            'class': 'aui-icon aui-icon-large aui-iconfont-schedule-filled',
            'style': 'margin-left: 10px;'
        });

        var scheduleNameSpan = $('<span>', {
            'id': schedule.ExecEntityInstanceId + '-test-schedule-test-plan',
            'class': 'aui-nav-item-label',
            'style': 'margin-left: 5px;'
        });
        scheduleNameSpan.text(schedule.ScheduleName);

        var scheduleLabel = $('<label>', {
            'class': 'jtoggler-wrapper',
            'id': schedule.ExecEntityInstanceId + '-test-schedule-test-plan',
            'style': 'position: absolute; right: 4%; margin-top: 5px;'
        });

        if (schedule.Status) {
            var scheduleInput = $('<input>', {
                'type': "checkbox",
                'class': 'jtoggler',
                'id': schedule.ExecEntityInstanceId + '-test-schedule-input-button',
                'checked': 'checked'
            }).click('change', function (event) {
                var isChecked = scheduleInput.prop("checked");
                var eEinstanceId = $(event.currentTarget).attr('id').split('-')[0];
                if (isChecked) {
                    startSchedule(eEinstanceId);
                } else {
                    removeSchedule(eEinstanceId);
                }
            });
        } else {
            var scheduleInput = $('<input>', {
                'type': "checkbox",
                'class': 'jtoggler',
                'id': schedule.ExecEntityInstanceId + '-test-schedule-input-button'
            }).click('change', function (event) {
                var isChecked = scheduleInput.prop("checked");
                var eEinstanceId = $(event.currentTarget).attr('id').split('-')[0];
                if (isChecked) {
                    startSchedule(eEinstanceId);
                } else {
                    removeSchedule(eEinstanceId);
                }
            });
        }

        var scheduleControllerDiv = $('<div>', {
            'class': 'jtoggler-control'
        });

        var scheduleControllerInnerDiv = $('<div>', {
            'class': 'jtoggler-handle'
        });

        scheduleControllerDiv.append(scheduleControllerInnerDiv);
        scheduleLabel.append(scheduleInput);
        scheduleLabel.append(scheduleControllerDiv);
        scheduleA.append(scheduleIconSpan);
        scheduleA.append(scheduleNameSpan);
        scheduleA.append(scheduleLabel);

        scheduleLi.append(scheduleA);

        parent.append(scheduleLi);
    }

    //SCHEDULE
    $('body').on('click', 'ul#test-schedule-ul li a span', function (event) {
        $(event.currentTarget).parent().addClass('active');
        appendSchedulePlanList(false, event, $('#' + event.currentTarget.id).text());
    });

    //EDIT VIEW
    $('body').on('click', 'ul#test-schedule-test-plan-ul li', function (event) {
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
                    'id': $(event.currentTarget).attr('id').split('-')[0]
                })
            },
            success: function (response) {
                var planCount = 1;
                var responseObject = JSON.parse(response);
                addSelectedPlanToList($('tbody#selected-schedule-table-tbody'), responseObject, isRun, count, planExecuted, planCount);
                $(event.currentTarget).remove();
                $('div.selected-schedule-table-content').animate({
                    scrollTop: $('div.selected-schedule-table-content').prop('scrollHeight')
                }, 500);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Test Plans..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('body').on('click', 'li[type="test-exec-test-case"]', function (event) {
        var isRun = false;
        var count = 0;
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                entity: 'TestCase',
                action: 'get',
                data: JSON.stringify({
                    'id': $(event.currentTarget).attr('id').split('-')[0]
                })
            },
            success: function (response) {
                var response = JSON.parse(response);
                if ($('tbody#selected-schedule-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').length == 0) {
                    addDefaultTestPlan($('tbody#selected-schedule-table-tbody'), isRun, count);
                }
                addSelectedTestCaseToList($('tbody#selected-schedule-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').find('table.aui tbody'), response, isRun, count);
                $(event.currentTarget).remove();
                $('div.selected-schedule-table-content').animate({
                    scrollTop: $('div.selected-schedule-table-content').prop('scrollHeight')
                }, 500);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    function addDefaultTestPlan(parent, isRun, count) {
        var plan = {
            'id': 'TPDefaultPlan',
            'name': 'Ad Hoc Test Plan',
            'testCases': []
        }
        addSelectedPlanToList(parent, plan, isRun, count);
    }

    function addSchedulePlanToList(parent, plan, isRun, count, planExecuted, planCount) {

        var testPlanTRObject = $('<tr>', {
            'id': plan.id + '-test-exec-test-plan-tr'
        });

        var testPlanNameTDObject = $('<td>', {
            'width': '15%',
            'style': 'text-align: center;'
        });
        testPlanNameTDObject.text(plan.planName);

        var testPlanConfigTDObject = $('<td>', {
            'width': '80%'
        });

        // Heading..
        var testPlanConfigHeadingDIV = $('<div>', {
            'class': 'aui-nav-heading'
        });
        var testPlanConfigHeadingStrong = $('<strong>');
        testPlanConfigHeadingStrong.text('Test Cases');

        testPlanConfigHeadingDIV.append(testPlanConfigHeadingStrong);

        // Test Cases Table..
        var testPlanConfigTCsTable = $('<table>', {
            'class': 'aui'
        })
        var testPlanConfigTCsTHEAD = $('<thead>', {
            'style': 'background-color: ' + tableHEADBGColor + ';'
        });
        var testPlanConfigTCsTHEAD_TR = $('<tr>');

        var testPlanConfigTCsTHEAD_TR_TH_Name = $('<th>', {
            'style': 'color: #000; width: 25%; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Name.text('Name')

        var testPlanConfigTCsTHEAD_TR_TH_Automated = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Automated.text('Automated');

        var testPlanConfigTCsTHEAD_TR_TH_Manual = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Manual.text('Manual');

        var testPlanConfigTCsTHEAD_TR_TH_Agent = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Agent.text('Agent');

        var testPlanConfigTCsTHEAD_TR_TH_User = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_User.text('User');

        // Browser header
        var testPlanConfigTCsTHEAD_TR_TH_Browser = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Browser.text('Browser');

        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Name);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Automated);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Manual);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Agent);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_User);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Browser);

        testPlanConfigTCsTHEAD.append(testPlanConfigTCsTHEAD_TR);
        testPlanConfigTCsTable.append(testPlanConfigTCsTHEAD);

        var testPlanConfigTCsTBODY = $('<tbody>');
        var errorAgentCount = 0;

        for (var i = 0; i < plan.testCases.length; i++) {
            var testCase = plan.testCases[i];

            var testPlanConfigTCsTBODY_TR = $('<tr>', {
                'id': testCase.id + '-test-exec-test-plan-tr-tcs',
                'test-case-id': testCase.testCaseId
            });

            var testPlanConfigTCsTBODY_TR_TD_Name = $('<td>', {
                'style': 'vertical-align: middle; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_Name.text(testCase.testCaseName);

            var testPlanConfigTCsTBODY_TR_TD_Automated = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Automated_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.id + '-test-exec-test-plan-automated-check',
                'id': testCase.id + '-test-exec-test-plan-automated-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Automated.append(testPlanConfigTCsTBODY_TR_TD_Automated_Input);

            var testPlanConfigTCsTBODY_TR_TD_Manual = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Manual_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.id + '-test-exec-test-plan-manual-check',
                'id': testCase.id + '-test-exec-test-plan-manual-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Manual.append(testPlanConfigTCsTBODY_TR_TD_Manual_Input);

            var testPlanConfigTCsTBODY_TR_TD_Agent = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.id + '-test-exec-test-plan-agent-select',
                'name': testCase.id + '-test-exec-test-plan-agent-select',
                'disabled': 'disabled'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option);

            // Agents
            if (testCase.automated == 'Yes') {
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

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Pool_Optgroup);

                var tdAgentSelect_Agent_Optgroup = $('<optgroup>', {
                    'label': 'Agents'
                });

                var isEmptyAgents = setAgents(tdAgentSelect_Agent_Optgroup, isExecuted);

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Agent_Optgroup);

                if (planExecuted && planCount == 1 && typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                    errorAgentCount += 1;
                }

                if (errorAgentCount == 1) {
                    AJS.flag({
                        type: 'error',
                        title: 'Agent Unavailable',
                        close: 'auto'
                    });
                }
            }

            if (testCase.agent) {
                testPlanConfigTCsTBODY_TR_TD_Agent_Select.val(testCase.agent);
            }

            var testPlanConfigTCsTBODY_TR_TD_User = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_User_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.id + '-test-exec-test-plan-user-select',
                'name': testCase.id + '-test-exec-test-plan-user-select',
                'style': 'vertical-align: middle;',
                'disabled': 'disabled'
            })
            var testPlanConfigTCsTBODY_TR_TD_User_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_User_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_User_Select.append(testPlanConfigTCsTBODY_TR_TD_User_Select_Option);

            // Users
            setUsers(testPlanConfigTCsTBODY_TR_TD_User_Select, testCase.user);

            // Browser selection
            var testPlanConfigTCsTBODY_TR_TD_Browser = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Browser_Select = $('<select>', {
                'class': 'select form-control',
                'disabled': 'disabled'
            });

            var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

            testPlanConfigTCsTBODY_TR_TD_Browser.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            setBrowser(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            if (testCase.testCaseBrowser) {
                testPlanConfigTCsTBODY_TR_TD_Browser_Select.val(testCase.testCaseBrowser);
            }

            if (testCase.automated === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_Agent.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', false);
            }

            if (testCase.manual === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_User.append(testPlanConfigTCsTBODY_TR_TD_User_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', false);
            }

            var testPlanConfigTCsTBODY_TR_TD_ExpectedResult = $('<td>', {
                'style': 'vertical-align: middle; display: none; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_ExpectedResult.text(testCase.overallExpectedResult);

            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Name);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Automated);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Manual);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Agent);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_User);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Browser);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_ExpectedResult);

            testPlanConfigTCsTBODY.append(testPlanConfigTCsTBODY_TR);

        }
        testPlanConfigTCsTable.append(testPlanConfigTCsTBODY);

        testPlanConfigTDObject.append(testPlanConfigHeadingDIV);
        testPlanConfigTDObject.append(testPlanConfigTCsTable);

        testPlanTRObject.append(testPlanNameTDObject);
        testPlanTRObject.append(testPlanConfigTDObject);

        parent.append(testPlanTRObject);
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
                if (isExecuted) {
                    isEmptyAgents = true;
                }
                var agentObject = $('<option>');
                agentObject.attr("disabled", "disabled");
                agentObject.text("Unavailable");
                selectElement.append(agentObject);
            }
        });
        return isEmptyAgents;
    }

    function setBrowser(selectElement, selectedBrowserForTestPlan) {

        var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('IE');
        selectElement.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

        var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Firefox');
        selectElement.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

        var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Chrome');
        selectElement.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

    }

    function editViewInit(eEntityInstanceId) {
        var isRun = false;
        var count = 0;
        var planExecuted = true;
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            data: {
                user: username,
                action: 'get-schedule-data',
                data: JSON.stringify({
                    'id': eEntityInstanceId
                })
            },
            success: function (response) {
                var planCount = 1;
                var responseObject = JSON.parse(response);
                var planList = responseObject.plans;
                $('tbody#selected-schedule-table-tbody').empty();
                planList.forEach(planObj => {
                    appendEditViewList($('tbody#selected-schedule-table-tbody'), planObj, isRun, count, planExecuted, planCount);
                });
                $('div.selected-schedule-table-content').animate({
                    scrollTop: $('div.selected-schedule-table-content').prop('scrollHeight')
                }, 500);

            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Plan..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function appendEditViewList(parent, plan, isRun, count, planExecuted, planCount) {
        var testPlanTRObject = $('<tr>', {
            'id': plan.id + '-test-exec-test-plan-tr'
        });

        var testPlanNameTDObject = $('<td>', {
            'width': '15%'
        });
        testPlanNameTDObject.text(plan.planName);

        var testPlanConfigTDObject = $('<td>', {
            'width': '80%'
        });

        // Heading..
        var testPlanConfigHeadingDIV = $('<div>', {
            'class': 'aui-nav-heading'
        });
        var testPlanConfigHeadingStrong = $('<strong>');
        testPlanConfigHeadingStrong.text('Test Cases');

        testPlanConfigHeadingDIV.append(testPlanConfigHeadingStrong);

        // Test Cases Table..
        var testPlanConfigTCsTable = $('<table>', {
            'class': 'aui'
        })
        var testPlanConfigTCsTHEAD = $('<thead>', {
            'style': 'background-color: ' + tableHEADBGColor + ';'
        });
        var testPlanConfigTCsTHEAD_TR = $('<tr>');

        var testPlanConfigTCsTHEAD_TR_TH_Name = $('<th>', {
            'style': 'color: #000; width: 25%; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Name.text('Name')

        var testPlanConfigTCsTHEAD_TR_TH_Automated = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Automated.text('Automated');

        var testPlanConfigTCsTHEAD_TR_TH_Manual = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Manual.text('Manual');

        var testPlanConfigTCsTHEAD_TR_TH_Agent = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Agent.text('Agent');

        var testPlanConfigTCsTHEAD_TR_TH_User = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_User.text('User');

        // Browser header
        var testPlanConfigTCsTHEAD_TR_TH_Browser = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Browser.text('Browser');

        //Browser button header
        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header = $('<th>', {
            'style': 'width: 5%; color:' + tableHEADColor + ';'
        });

        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button = $('<button>', {
            'class': 'aui-button',
            'planId': plan.id,
            'testPlanName': plan.name,
            'title': 'Browser Selector For Test Plan',
            'style': 'outline: none;'
        }).click(openBrowserDialog);;

        testPlanConfigTCsTHEAD_TR_TH_Browser_Button.tooltip({
            gravity: 'w'
        });

        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Span = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-world',
            'style': 'height: 20px; width: 20px; vertical-align: middle;'
        });

        testPlanConfigTCsTHEAD_TR_TH_Browser_Button.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Span);
        testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button);

        var testPlanConfigTCsTHEAD_TR_TH_AutoGenerateAI = $('<th>', {
            'style': 'width: 5%; color: ' + tableHEADColor + ';'
        });

        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Name);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Automated);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Manual);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Agent);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_User);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Browser);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_AutoGenerateAI);

        testPlanConfigTCsTHEAD.append(testPlanConfigTCsTHEAD_TR);
        testPlanConfigTCsTable.append(testPlanConfigTCsTHEAD);

        var testPlanConfigTCsTBODY = $('<tbody>');
        var errorAgentCount = 0;

        for (var i = 0; i < plan.testCases.length; i++) {
            var testCase = plan.testCases[i];

            var testPlanConfigTCsTBODY_TR = $('<tr>', {
                'id': testCase.id + '-test-exec-test-plan-tr-tcs',
                'test-case-id': testCase.testCaseId,
                'test-plan-id': testCase.planId
            });

            var testPlanConfigTCsTBODY_TR_TD_Name = $('<td>', {
                'style': 'vertical-align: middle; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_Name.text(testCase.testCaseName);

            var testPlanConfigTCsTBODY_TR_TD_Automated = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Automated_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.id + '-test-exec-test-plan-automated-check',
                'id': testCase.id + '-test-exec-test-plan-automated-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Automated.append(testPlanConfigTCsTBODY_TR_TD_Automated_Input);

            var testPlanConfigTCsTBODY_TR_TD_Manual = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Manual_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.id + '-test-exec-test-plan-manual-check',
                'id': testCase.id + '-test-exec-test-plan-manual-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Manual.append(testPlanConfigTCsTBODY_TR_TD_Manual_Input);

            var testPlanConfigTCsTBODY_TR_TD_Agent = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.id + '-test-exec-test-plan-agent-select',
                'name': testCase.id + '-test-exec-test-plan-agent-select'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option);

            // Agents
            if (testCase.automated == 'Yes') {
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

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Pool_Optgroup);

                var tdAgentSelect_Agent_Optgroup = $('<optgroup>', {
                    'label': 'Agents'
                });

                var isEmptyAgents = setAgents(tdAgentSelect_Agent_Optgroup, isExecuted);

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Agent_Optgroup);

                if (planExecuted && planCount == 1 && typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                    errorAgentCount += 1;
                }

                if (errorAgentCount == 1) {
                    AJS.flag({
                        type: 'error',
                        title: 'Agent Unavailable',
                        close: 'auto'
                    });
                }
            }

            if (testCase.agent) {
                testPlanConfigTCsTBODY_TR_TD_Agent_Select.val(testCase.agent);
            }

            var testPlanConfigTCsTBODY_TR_TD_User = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_User_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.id + '-test-exec-test-plan-user-select',
                'name': testCase.id + '-test-exec-test-plan-user-select',
                'style': 'vertical-align: middle;'
            })
            var testPlanConfigTCsTBODY_TR_TD_User_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_User_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_User_Select.append(testPlanConfigTCsTBODY_TR_TD_User_Select_Option);

            // Users
            setUsers(testPlanConfigTCsTBODY_TR_TD_User_Select, testCase.user);

            // Browser selection
            var testPlanConfigTCsTBODY_TR_TD_Browser = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Browser_Select = $('<select>', {
                'class': 'select form-control',
            });

            var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

            testPlanConfigTCsTBODY_TR_TD_Browser.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            setBrowser(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            if (testCase.testCaseBrowser) {
                testPlanConfigTCsTBODY_TR_TD_Browser_Select.val(testCase.testCaseBrowser);
            }

            if (testCase.automated === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_Agent.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', false);
            }

            if (testCase.manual === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_User.append(testPlanConfigTCsTBODY_TR_TD_User_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', false);
            }

            var testPlanConfigTCsTBODY_TR_TD_ExpectedResult = $('<td>', {
                'style': 'vertical-align: middle; display: none; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_ExpectedResult.text(testCase.overallExpectedResult);

            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI = $('<td>', {
                'style': 'vertical-align: middle; border: none;'
            });
            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton = $('<button>', {
                'id': testCase.id + '-auto-gen-ai-button',
                'class': 'aui-button',
                'title': 'Auto Generate Configuration from AI',
                'style': 'outline: none;'
            }).on('click', function (event) {
                var testCaseId = $(this).attr('id').split('-')[0];
                $(this).attr('test-case-id')
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/execution-entity-data-management',
                    async: false,
                    data: {
                        user: username,
                        action: 'get-executed-test-case-data',
                        data: JSON.stringify({
                            'test_case_id': testCaseId
                        })
                    },
                    success: function (response) {
                        var responseObject = JSON.parse(response);
                        if (responseObject.id == '') {
                            AJS.flag({
                                type: 'error',
                                title: 'Not Executed Previously',
                                body: 'No Such Record To Display',
                                close: 'auto'
                            });
                        } else {
                            autoGenerateFromAI(parent, responseObject, testCaseId);
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'Not Executed Previously..',
                            close: 'auto'
                        });
                    }
                });
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.tooltip({
                gravity: 'w'
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.click(function (event) {
                console.log($(event.currentTarget).attr('id'));
            });

            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan = $('<span>', {
                'class': 'aui-icon aui-icon-small aui-iconfont-vid-raised-hand',
                'style': 'height: 20px; width: 20px;'
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan);
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton);

            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Name);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Automated);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Manual);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Agent);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_User);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Browser);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_ExpectedResult);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI);

            testPlanConfigTCsTBODY.append(testPlanConfigTCsTBODY_TR);

        }
        testPlanConfigTCsTable.append(testPlanConfigTCsTBODY);

        testPlanConfigTDObject.append(testPlanConfigHeadingDIV);
        testPlanConfigTDObject.append(testPlanConfigTCsTable);

        var testPlanRemoveTDObject = $('<td>', {
            'width': '5%'
        });

        var testPlanRemoveBtn = $('<button>', {
            'class': 'aui-button'
        });

        var testPlanRemoveBtnSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-trash'
        });

        testPlanRemoveBtn.append(testPlanRemoveBtnSpan);
        testPlanRemoveBtn.click(function (event) {
            $(event.currentTarget).parent().parent().remove();
            if ($(event.currentTarget).parent().parent().attr('id') !== 'TPDefaultPlan-test-exec-test-plan-tr') {
                var planObject = {
                    'id': plan.id,
                    'name': plan.name,
                    'count': plan.testCases.length
                }
                if (plan.name.length > 0) {
                    appendTestExecTestPlanNodes($('#test-schedule-test-plan-ul'), planObject);
                    $('div.test-schedule-test-plans-grouping').animate({
                        scrollTop: $('div.test-schedule-test-plans-grouping').prop('scrollHeight')
                    }, 500);
                }
                event.preventDefault();
            }
        });
        testPlanRemoveTDObject.append(testPlanRemoveBtn);

        testPlanTRObject.append(testPlanNameTDObject);
        testPlanTRObject.append(testPlanConfigTDObject);
        testPlanTRObject.append(testPlanRemoveTDObject);

        parent.append(testPlanTRObject);
    }

    function initTestExecTestPlan() {
        $('#test-schedule-test-plan-ul').empty();
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
                var availablePlans = JSON.parse(response);
                for (var i = 0; i < availablePlans.plans.length; i++) {
                    var planObject = availablePlans.plans[i];
                    appendTestExecTestPlanNodes($('#test-schedule-test-plan-ul'), planObject);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Initializing The Test Plan..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function appendTestExecTestPlanNodes(parent, plan) {
        var testPlanLi = $('<li>', {
            'id': plan.id + '-test-exec-test-plan',
            'style': 'border-bottom: 1px solid #6b778c40;'
        });

        var testPlanA = $('<a>', {
            'class': 'aui-nav-item',
            'style': 'display: flex;'
        });
        var testPlanIconSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-calendar-filled',
        });
        var testPlanNameSpan = $('<span>', {
            'class': 'aui-nav-item-label'
        });
        testPlanNameSpan.text(plan.name);

        var testPlanFlexSpan = $('<span>', {
            'style': 'flex: 1;'
        });

        var testPlanTCCountSpan = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-current',
            'style': 'line-height: 16px;'
        });
        testPlanTCCountSpan.text(plan.count);

        testPlanA.append(testPlanIconSpan);
        testPlanA.append(testPlanNameSpan);
        testPlanA.append(testPlanFlexSpan);
        testPlanA.append(testPlanTCCountSpan);

        testPlanLi.append(testPlanA);

        parent.append(testPlanLi);
    }

    function initTestExecTC(parent) {
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
                    'redirectView': '/secure/ScheduleView.jspa'
                })
            },
            success: function (response) {
                var returnTreeItems = JSON.parse(response);
                if (parent === 'null') {
                    $('ul[parent="test-exec-tc-root-node"]').empty();
                } else {
                    $('ul[parent="' + parent + '-test-exec-tc-folder"]').empty();
                }
                for (var i = 0; i < returnTreeItems.folders.length; i++) {
                    var folderObject = returnTreeItems.folders[i];
                    if (folderObject.parent === 'null') {
                        appendTestExecTF($('ul[parent="test-exec-tc-root-node"]'), folderObject);
                    } else {
                        appendTestExecTF($('ul[parent="' + folderObject.parent + '-test-exec-tc-folder"]'), folderObject);
                    }
                }
                var testcases = JSON.parse(returnTreeItems.testcases);
                for (var i = 0; i < testcases.length; i++) {
                    if (testcases[i].parent == 0) {
                        appendTestExecTC($('ul[parent="test-exec-tc-root-node"]'), testcases[i]);
                    } else {
                        appendTestExecTC($('ul[parent="' + testcases[i].parent + '-test-exec-tc-folder"]'), testcases[i]);
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

    function appendTestExecTF(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-test-exec-tc-folder',
            'type': 'test-exec-tc-folder',
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
            $('li[type="test-exec-tc-folder"]').removeClass('active');
            $('li[type="test-exec-test-case"]').removeClass('active');
            this.classList.toggle('active');
            selectTestExecTFId = $(event.currentTarget).attr('id');
            var childUL = $('ul[parent=' + $(event.currentTarget).attr('id') + ']');
            if (childUL.length !== 0) {
                if (childUL.css('display') === 'block') {
                    childUL.css('display', 'none');
                } else {
                    initTestExecTC($(event.currentTarget).attr('id').split('-')[0]);
                    childUL.css('display', 'block');
                }
            }
        });

        var ulObject = $('<ul>', {
            'class': 'aui-nav',
            'parent': child.id + '-test-exec-tc-folder',
            'style': 'display: none'
        });

        parent.append(liObject);
        parent.append(ulObject);

    }

    function appendTestExecTC(parent, child) {
        var liObject = $('<li>', {
            'id': child.id + '-test-exec-test-case',
            'type': 'test-exec-test-case',
            'style': 'border-bottom: 1px solid #6b778c40;'
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
        });

        parent.append(liObject);
    }

    function openBrowserDialog() {
        var planId = $(this).attr('planId');
        var testPlanName = $(this).attr('testPlanName');
        testPlanId = planId;
        var browserHeader = $('.aui-dialog2-header-main');
        browserHeader.text(testPlanName);
        AJS.dialog2("#edit-schedule-browser-selector-dialog").show();

        // Apply browser from test plan dialog
        $('#edit-schedule-apply-browser-btn').click(function () {
            selectedBrowserForTestPlan = $("#browser-select-test-plan").val();
            var selectedTestPlans = $('#selected-schedule-table-tbody').children();
            for (var i = 0; i < selectedTestPlans.length; i++) {
                selectedTestPlans.each(function () {
                    var testPlanIds = selectedTestPlans[i];
                    var tpId = jQuery(testPlanIds.closest('tr')).attr('id')
                    var selectedTestPlanId = tpId.split("-")[0];
                    if (selectedTestPlanId == testPlanId) {
                        var planTestCases = $($($(selectedTestPlans[i]).children()[1]).children()[1]).find('tbody tr');
                        planTestCases.each(function () {
                            var testCaseTR = $(this);
                            var testCaseBrowserTD = $(testCaseTR.children()[5]);
                            if (selectedBrowserForTestPlan != '') {
                                $(testCaseBrowserTD.find('select')).val(selectedBrowserForTestPlan);
                            }
                        });
                    }
                });
            }
            AJS.dialog2("#edit-schedule-browser-selector-dialog").hide();
        });
    }

    // Hide browser from test plan dialog
    $('#edit-schedule-cancel-browser-btn').click(function () {
        AJS.dialog2('#edit-schedule-browser-selector-dialog').hide();
    });

    $('#edit-execute-test-plan-btn').on('click', function () {
        AJS.$("#edit-schedule-entity-release").auiSelect2();
        getVersion("#edit-schedule-entity-release");
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            data: {
                user: username,
                action: 'edit-schedule-data',
                data: JSON.stringify({
                    'id': selectedEditEntityInstanceId,
                })
            }, success: function (response) {
                var responseObj = JSON.parse(response);
                $('#edit-schedule-entity-release').auiSelect2('val', responseObj.release);
                $('#edit-schedule-entity-release-description').val(responseObj.description);
                $('#edit-schedule-name').val(responseObj.schedulerName);
                $('#edit-cron-value').val(responseObj.cronValue);

            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Scheduler Details..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        AJS.dialog2('#edit-schedule-dialog').show();
    });

    $('#edit-save-schedule-button').on('click', function () {
        var agentValidationCounter = 0;
        var userValidationCounter = 0;
        var browserValidationCounter = 0;
        var selectedRelease = $('#edit-schedule-entity-release').val();
        var versionDesc = $('#edit-schedule-entity-release-description').val();
        var scheduleName = $('#edit-schedule-name').val();
        var cronValue = $('#edit-cron-value').val();
        var isChecked = toggle.prop("checked");
        if (isChecked) {
            parallelExecutionStatus = true;
        }
        if (!validateInput(scheduleName)) {
            if (selectedRelease != '' && scheduleName != '') {
                var validateFields = true;
                var execEntityObject = {
                    'newReleaseVersion': selectedRelease,
                    'newReleaseDescription': versionDesc,
                    'plans': [],
                    'parallelExecution': parallelExecutionStatus,
                    'newScheduleName': scheduleName,
                    'newCronValue': cronValue,
                    'eEntityInstanceId': selectedEditEntityInstanceId,
                    'isFromSchedule': false,
                    'status': false
                }
                var selectedTestPlans = $('#selected-schedule-table-tbody').children();
                if (selectedTestPlans.length < 0) {
                    removeSchedule(selectedEditEntityInstanceId);
                } else {
                    selectedTestPlans.each(function () {
                        var planName = $($(this).children()[0]).text();
                        var planTestCases = $($($(this).children()[1]).children()[1]).find('tbody tr');
                        var planObject = {
                            'planId': $(this).attr('id').split('-')[0],
                            'planName': planName,
                            'testCases': []
                        };

                        planTestCases.each(function () {
                            var agentCounter = 0;
                            var userCounter = 0;
                            var browserCounter = 0;
                            var testCaseTR = $(this);
                            var testCaseNameTD = $(testCaseTR.children()[0]);
                            var testCaseAutomatedTD = $(testCaseTR.children()[1]);
                            var testCaseManualTD = $(testCaseTR.children()[2]);
                            var testCaseAgentTD = $(testCaseTR.children()[3]);
                            var testCaseUserTD = $(testCaseTR.children()[4]);
                            var testCaseBrowserTD = $(testCaseTR.children()[5]);
                            var testCaseExpectedResultTD = $(testCaseTR.children()[6]);
                            var testAgentStatus = $('option:selected', testCaseAgentTD).attr('agentstatus');
                            var testCaseId = '';
                            if (typeof testAgentStatus == 'undefined') {
                                testAgentStatus = false;
                            } else {
                                testAgentStatus = $('option:selected', testCaseAgentTD).attr('agentstatus');
                            }
                            if (typeof testCaseTR.attr('test-case-id') != 'undefined') {
                                testCaseId = testCaseTR.attr('test-case-id');
                            } else {
                                testCaseId = testCaseTR.attr('id').split('-')[0];
                            }
                            var testCaseObject = {
                                'id': testCaseTR.attr('id').split('-')[0],
                                'testCaseId': testCaseId,
                                'testCaseName': testCaseNameTD.text(),
                                'overallExpectedResult': testCaseExpectedResultTD.text() || '',
                                'testAgentStatus': testAgentStatus,
                                'testCaseBrowser': $(testCaseBrowserTD.find('select')).val()
                            }

                            if ($(testCaseAutomatedTD.find('input[type="checkbox"]')).prop('checked')) {
                                testCaseObject.automated = 'Yes';
                            } else {
                                testCaseObject.automated = 'No';
                            }
                            if ($(testCaseManualTD.find('input[type="checkbox"]')).prop('checked')) {
                                testCaseObject.manual = 'Yes';
                            } else {
                                testCaseObject.manual = 'No';
                            }

                            if (testCaseObject.automated == 'Yes' && testCaseObject.manual == 'Yes') {

                                if (!($(testCaseAgentTD.find('select')).val() == 'Select') || !($(testCaseUserTD.find('select')).val() == 'Select')) {
                                    testCaseObject.agent = $(testCaseAgentTD.find('select')).val();
                                    testCaseObject.user = $(testCaseUserTD.find('select')).val();
                                    planObject.testCases.push(testCaseObject);

                                } else {
                                    if (agentCounter < 1) {
                                        AJS.flag({
                                            type: 'error',
                                            body: 'Agent not selected select agent and rerun',
                                            title: 'Agent Not Selected',
                                            close: 'auto'
                                        });
                                    }
                                    agentCounter += 1;
                                    agentValidationCounter = agentCounter;

                                    if (userCounter < 1) {
                                        AJS.flag({
                                            type: 'error',
                                            body: 'User not selected select user and rerun',
                                            title: 'User Not Selected',
                                            close: 'auto'
                                        });
                                    }
                                    userCounter += 1;
                                    userValidationCounter = userCounter;
                                }
                            }
                            else if (testCaseObject.automated == 'Yes' || testCaseObject.manual == 'Yes') {
                                if (testCaseObject.automated == 'Yes') {
                                    if (!($(testCaseAgentTD.find('select')).val() == 'Select')) {
                                        testCaseObject.agent = $(testCaseAgentTD.find('select')).val();
                                    } else {
                                        if (agentCounter < 1) {
                                            AJS.flag({
                                                type: 'error',
                                                body: 'Agent not selected select agent and rerun',
                                                title: 'Agent Not Selected',
                                                close: 'auto'
                                            });
                                        }
                                        agentCounter += 1;
                                        agentValidationCounter = agentCounter;
                                    }
                                }

                                if (testCaseObject.manual == 'Yes') {
                                    if (!($(testCaseUserTD.find('select')).val() == 'Select')) {
                                        testCaseObject.user = $(testCaseUserTD.find('select')).val();
                                    } else {
                                        if (userCounter < 1) {
                                            AJS.flag({
                                                type: 'error',
                                                body: 'User not selected select user and rerun',
                                                title: 'User Not Selected',
                                                close: 'auto'
                                            });
                                        }
                                        userCounter += 1;
                                        userValidationCounter = userCounter;
                                    }
                                }

                                if (!($(testCaseBrowserTD.find('select')).val() == 'Select')) {
                                    testCaseObject.testCaseBrowser = $(testCaseBrowserTD.find('select')).val();
                                } else {
                                    if (browserCounter < 1) {
                                        AJS.flag({
                                            type: 'error',
                                            body: 'Browser not selected select browser and rerun',
                                            title: 'Browser Not Selected',
                                            close: 'auto'
                                        });
                                    }
                                    browserCounter += 1;
                                    browserValidationCounter = browserCounter;
                                }

                                planObject.testCases.push(testCaseObject);
                            }
                        });
                        execEntityObject.plans.push(planObject);
                    });
                }
                if (userValidationCounter == 0 && agentValidationCounter == 0 && browserValidationCounter == 0) {
                    $.ajax({
                        type: 'POST',
                        url: baseUrl + 'plugins/servlet/exe-entity',
                        async: false,
                        data: {
                            user: username,
                            action: 'validate',
                            data: JSON.stringify(execEntityObject)
                        }, success: function (validatedResponse) {
                            var parsedValidatedResponse = JSON.parse(validatedResponse);
                            if (!(parsedValidatedResponse.length > 0)) {
                                $.ajax({
                                    type: 'POST',
                                    url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
                                    async: false,
                                    data: {
                                        user: username,
                                        action: 'update-schedule',
                                        data: JSON.stringify(execEntityObject)
                                    },
                                    success: function (response) {
                                        var updated = JSON.parse(response);
                                        if (updated.isUpdated) {
                                            AJS.flag({
                                                type: 'success',
                                                title: 'Schedule Updated..',
                                                close: 'auto'
                                            });
                                            $('#test-schedule-div').css("display", "block");
                                            $('#test-schedule-test-plan-ul').empty();
                                            $('#schedule-hr').css("display", "none");
                                            $('#schedule-test-case-header').css("display", "none");
                                            $('#scheduler-test-case-div').css("display", "none");
                                            $('#test-exec-actions-div').css("display", "none");
                                            $('#test-schedule-test-plans-div').css("display", "none");
                                            $('#schedule-view-header').text("SCHEDULED EXECUTIONS");
                                            $('#test-schedule-ul').empty();
                                            initScheduleList();
                                            appendSchedulePlanList(true, selectedEditEntityInstanceId, scheduleName);
                                            AJS.dialog2('#edit-schedule-dialog').hide();
                                        } else {
                                            AJS.flag({
                                                type: 'error',
                                                title: 'An Error Occured While Updating Schedule..',
                                                body: 'Contact Developer',
                                                close: 'auto'
                                            });
                                        }
                                    }, error: function (jqXHR, textStatus, errorThrown) {
                                        AJS.flag({
                                            type: 'error',
                                            title: 'An Error Occured While Updating Schedule..',
                                            body: 'Contact Developer',
                                            close: 'auto'
                                        });
                                    }
                                });
                            } else {
                                for (var index = 0; index < parsedValidatedResponse.length; index++) {
                                    AJS.flag({
                                        type: 'error',
                                        body: parsedValidatedResponse[index] + ' has no attached centriod scripts. Attach a script and rerun .',
                                        title: 'Missing Scripts',
                                        close: 'auto'
                                    });
                                }
                            }
                        }, error: function (jqXHR, textStatus, errorThrown) {
                            AJS.flag({
                                type: 'error',
                                title: 'An Error Occurred Validating The Data..',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        }
                    });
                }
            }
        }
    });

    $('#edit-schedule-cancel-button').on('click', function () {
        AJS.dialog2('#edit-schedule-dialog').hide();
        $('#test-schedule-div').css("display", "block");
        $('#test-schedule-test-plan-ul').empty();
        $('#schedule-hr').css("display", "none");
        $('#schedule-test-case-header').css("display", "none");
        $('#scheduler-test-case-div').css("display", "none");
        $('#test-exec-actions-div').css("display", "none");
        $('#test-schedule-test-plans-div').css("display", "none");
        $('#schedule-view-header').text("SCHEDULED EXECUTIONS");
        $('#test-schedule-ul').empty();
        initScheduleList();
        appendSchedulePlanList(true, selectedEditEntityInstanceId, selectedEditScheduleName);
    });

    function startSchedule(eEntityInstanceId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            timeout: 5000,
            data: {
                user: username,
                action: 'start-schedule',
                data: JSON.stringify({
                    'id': eEntityInstanceId,
                    'status': true
                })
            },
            success: function (response) {
                var returnObj = JSON.parse(response);
                if (returnObj) {
                    AJS.flag({
                        type: 'success',
                        title: 'Schedule Started..',
                        close: 'auto'
                    });
                }
            },
            error: function (jqXHR, exception) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occured Starting The Schedule..',
                    close: 'auto'
                });
            }
        });
    }

    function removeSchedule(eEntityInstanceId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            timeout: 5000,
            data: {
                user: username,
                action: 'remove-schedule',
                data: JSON.stringify({
                    'id': eEntityInstanceId,
                    'status': false
                })
            },
            success: function (response) {
                var returnObj = JSON.parse(response);
                if (returnObj) {
                    AJS.flag({
                        type: 'success',
                        title: 'Schedule Removed..',
                        close: 'auto'
                    });
                }
            },
            error: function (jqXHR, exception) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occured Removing The Schedule..',
                    close: 'auto'
                });
            }
        });
    }

    function getVersion(releaseSelector) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-management-util',
            async: false,
            data: {
                user: username,
                action: 'get-versions',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                $(releaseSelector).children().remove();
                addReleaseSelectOptions($(releaseSelector), {
                    'id': 'select-version',
                    'value': 'Select Version',
                    'disabled': ''
                });
                for (var i = 0; i < response.length; i++) {
                    addReleaseSelectOptions($(releaseSelector), response[i]);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Versions..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function addReleaseSelectOptions(parent, option) {
        var optionObject = $('<option>', {
            'id': option.id,
            'value': option.name
        });
        optionObject.text(option.name);
        parent.append(optionObject);
    }

    // open dialog for browser selection
    function openBrowserDialog() {
        var planId = $(this).attr('planId');
        var testPlanName = $(this).attr('testPlanName');
        testPlanId = planId;
        var browserHeader = $('.aui-dialog2-header-main');
        browserHeader.text(testPlanName);
        AJS.dialog2("#edit-schedule-browser-selector-dialog").show();

        // Apply browser from test plan dialog
        $('#edit-schedule-apply-browser-btn').click(function () {
            selectedBrowserForTestPlan = $("#edit-schedule-browser-select-test-plan").val();
            var selectedTestPlans = $('#selected-schedule-table-tbody').children();
            for (var i = 0; i < selectedTestPlans.length; i++) {
                selectedTestPlans.each(function () {
                    var testPlanIds = selectedTestPlans[i];
                    var tpId = jQuery(testPlanIds.closest('tr')).attr('id');
                    var selectedTestPlanId = tpId.split("-")[0];
                    if (selectedTestPlanId == testPlanId) {
                        var planTestCases = $($($(selectedTestPlans[i]).children()[1]).children()[1]).find('tbody tr');
                        planTestCases.each(function () {
                            var testCaseTR = $(this);
                            var testCaseBrowserTD = $(testCaseTR.children()[5]);
                            if (selectedBrowserForTestPlan != '') {
                                $(testCaseBrowserTD.find('select')).val(selectedBrowserForTestPlan);
                            }
                        });
                    }
                });
            }
            AJS.dialog2("#edit-schedule-browser-selector-dialog").hide();
        });
    }

    // Auto Generate From AI
    function autoGenerateFromAI(parent, responseObject, testCaseId) {
        if (responseObject.testCaseAgent) {
            $(parent).find('tr').find('td select[name=' + testCaseId + '-test-exec-test-plan-agent-select]').val(responseObject.testCaseAgent);
        }
        if (responseObject.testCaseUser) {
            $(parent).find('tr').find('td select[name=' + testCaseId + '-test-exec-test-plan-user-select]').val(responseObject.testCaseUser);
        }
    }

    function addSelectedPlanToList(parent, plan, isRun, count, planExecuted, planCount) {

        var testPlanTRObject = $('<tr>', {
            'id': plan.id + '-test-exec-test-plan-tr'
        });

        var testPlanNameTDObject = $('<td>', {
            'width': '15%'
        });
        testPlanNameTDObject.text(plan.name);

        var testPlanConfigTDObject = $('<td>', {
            'width': '80%'
        });

        // Heading..
        var testPlanConfigHeadingDIV = $('<div>', {
            'class': 'aui-nav-heading'
        });
        var testPlanConfigHeadingStrong = $('<strong>');
        testPlanConfigHeadingStrong.text('Test Cases');

        testPlanConfigHeadingDIV.append(testPlanConfigHeadingStrong);

        // Test Cases Table..
        var testPlanConfigTCsTable = $('<table>', {
            'class': 'aui'
        })
        var testPlanConfigTCsTHEAD = $('<thead>', {
            'style': 'background-color: ' + tableHEADBGColor + ';'
        });
        var testPlanConfigTCsTHEAD_TR = $('<tr>');

        var testPlanConfigTCsTHEAD_TR_TH_Name = $('<th>', {
            'style': 'color: #000; width: 25%; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Name.text('Name')

        var testPlanConfigTCsTHEAD_TR_TH_Automated = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Automated.text('Automated');

        var testPlanConfigTCsTHEAD_TR_TH_Manual = $('<th>', {
            'style': 'width: 10%; text-align: center; color: ' + tableHEADColor + '; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Manual.text('Manual');

        var testPlanConfigTCsTHEAD_TR_TH_Agent = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Agent.text('Agent');

        var testPlanConfigTCsTHEAD_TR_TH_User = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_User.text('User');

        // Browser header
        var testPlanConfigTCsTHEAD_TR_TH_Browser = $('<th>', {
            'style': 'width: 15%; color: ' + tableHEADColor + '; text-align: center; vertical-align: middle;'
        });
        testPlanConfigTCsTHEAD_TR_TH_Browser.text('Browser');

        // Browser button header
        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header = $('<th>', {
            'style': 'width: 5%; color:' + tableHEADColor + ';'
        });

        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button = $('<button>', {
            'class': 'aui-button',
            'planId': plan.id,
            'testPlanName': plan.name,
            'title': 'Browser Selector For Test Plan',
            'style': 'outline: none;'
        }).click(openBrowserDialog);

        testPlanConfigTCsTHEAD_TR_TH_Browser_Button.tooltip({
            gravity: 'w'
        });

        var testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Span = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-world',
            'style': 'height: 20px; width: 20px; vertical-align: middle;'
        });

        testPlanConfigTCsTHEAD_TR_TH_Browser_Button.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Span);
        testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button);

        var testPlanConfigTCsTHEAD_TR_TH_AutoGenerateAI = $('<th>', {
            'style': 'width: 5%; color: ' + tableHEADColor + ';'
        });

        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Name);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Automated);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Manual);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Agent);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_User);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Browser);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_Browser_Button_Header);
        testPlanConfigTCsTHEAD_TR.append(testPlanConfigTCsTHEAD_TR_TH_AutoGenerateAI);

        testPlanConfigTCsTHEAD.append(testPlanConfigTCsTHEAD_TR);
        testPlanConfigTCsTable.append(testPlanConfigTCsTHEAD);

        var testPlanConfigTCsTBODY = $('<tbody>');
        var errorAgentCount = 0;

        for (var i = 0; i < plan.testCases.length; i++) {
            var testCase = plan.testCases[i];

            var testPlanConfigTCsTBODY_TR = $('<tr>', {
                'id': testCase.testCase.id + '-test-exec-test-plan-tr-tcs',
                'test-case-id': testCase.testCase.testCaseId
            });

            var testPlanConfigTCsTBODY_TR_TD_Name = $('<td>', {
                'style': 'vertical-align: middle; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_Name.text(testCase.testCase.name);

            var testPlanConfigTCsTBODY_TR_TD_Automated = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Automated_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.testCase.id + '-test-exec-test-plan-automated-check',
                'id': testCase.testCase.id + '-test-exec-test-plan-automated-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Automated.append(testPlanConfigTCsTBODY_TR_TD_Automated_Input);

            var testPlanConfigTCsTBODY_TR_TD_Manual = $('<td>', {
                'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Manual_Input = $('<input>', {
                'class': 'checkbox',
                'type': 'checkbox',
                'name': testCase.testCase.id + '-test-exec-test-plan-manual-check',
                'id': testCase.testCase.id + '-test-exec-test-plan-manual-check',
                'style': 'margin: auto;',
                'disabled': ''
            });
            testPlanConfigTCsTBODY_TR_TD_Manual.append(testPlanConfigTCsTBODY_TR_TD_Manual_Input);

            var testPlanConfigTCsTBODY_TR_TD_Agent = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.testCase.id + '-test-exec-test-plan-agent-select',
                'name': testCase.testCase.id + '-test-exec-test-plan-agent-select'
            });

            var testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option);

            // Agents
            if (testCase.testCase.automated == 'Yes') {
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

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Pool_Optgroup);

                var tdAgentSelect_Agent_Optgroup = $('<optgroup>', {
                    'label': 'Agents'
                });

                var isEmptyAgents = setAgents(tdAgentSelect_Agent_Optgroup, isExecuted);

                testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Agent_Optgroup);

                if (planExecuted && planCount == 1 && typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                    errorAgentCount += 1;
                }

                if (errorAgentCount == 1) {
                    AJS.flag({
                        type: 'error',
                        title: 'Agent Unavailable',
                        close: 'auto'
                    });
                }
            }

            if (testCase.agent) {
                testPlanConfigTCsTBODY_TR_TD_Agent_Select.val(testCase.agent);
            }

            var testPlanConfigTCsTBODY_TR_TD_User = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_User_Select = $('<select>', {
                'class': 'select form-control',
                'id': testCase.testCase.id + '-test-exec-test-plan-user-select',
                'name': testCase.testCase.id + '-test-exec-test-plan-user-select',
                'style': 'vertical-align: middle;'
            })
            var testPlanConfigTCsTBODY_TR_TD_User_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_User_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_User_Select.append(testPlanConfigTCsTBODY_TR_TD_User_Select_Option);

            // Users
            setUsers(testPlanConfigTCsTBODY_TR_TD_User_Select, testCase.user)

            // Browser selection
            var testPlanConfigTCsTBODY_TR_TD_Browser = $('<td>', {
                'style': 'width: 20%; border: none; vertical-align: middle;'
            });
            var testPlanConfigTCsTBODY_TR_TD_Browser_Select = $('<select>', {
                'class': 'select form-control',
            })

            var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Select');
            testPlanConfigTCsTBODY_TR_TD_Browser_Select.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

            testPlanConfigTCsTBODY_TR_TD_Browser.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            setBrowser(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

            if (testCase.testCase.automated === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_Agent.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', false);
            }

            if (testCase.testCase.manual === 'Yes') {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', true);
                testPlanConfigTCsTBODY_TR_TD_User.append(testPlanConfigTCsTBODY_TR_TD_User_Select);
            } else {
                testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', false);
            }

            var testPlanConfigTCsTBODY_TR_TD_ExpectedResult = $('<td>', {
                'style': 'vertical-align: middle; display: none; border: none;'
            });
            testPlanConfigTCsTBODY_TR_TD_ExpectedResult.text(testCase.testCase.overallExpectedResult);

            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI = $('<td>', {
                'style': 'vertical-align: middle; border: none;'
            });
            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton = $('<button>', {
                'id': testCase.testCase.id + '-auto-gen-ai-button',
                'class': 'aui-button',
                'title': 'Auto Generate Configuration from AI',
                'style': 'outline: none;'
            }).on('click', function (event) {
                var testCaseId = $(this).attr('id').split('-')[0];
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/execution-entity-data-management',
                    async: false,
                    data: {
                        user: username,
                        action: 'get-executed-test-case-data',
                        data: JSON.stringify({
                            'test_case_id': testCaseId
                        })
                    },
                    success: function (response) {
                        var responseObject = JSON.parse(response);
                        if (responseObject.id == '') {
                            AJS.flag({
                                type: 'error',
                                title: 'Not Executed Previously',
                                body: 'No Such Record To Display',
                                close: 'auto'
                            });
                        } else {
                            autoGenerateFromAI(parent, responseObject, testCaseId);
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'Not Executed Previously..',
                            close: 'auto'
                        });
                    }
                });
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.tooltip({
                gravity: 'w'
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.click(function (event) {
                console.log($(event.currentTarget).attr('id'));
            });

            var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan = $('<span>', {
                'class': 'aui-icon aui-icon-small aui-iconfont-vid-raised-hand',
                'style': 'height: 20px; width: 20px;'
            });
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan);
            testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton);

            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Name);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Automated);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Manual);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Agent);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_User);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Browser);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_ExpectedResult);
            testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI);

            testPlanConfigTCsTBODY.append(testPlanConfigTCsTBODY_TR);

        }
        testPlanConfigTCsTable.append(testPlanConfigTCsTBODY);

        testPlanConfigTDObject.append(testPlanConfigHeadingDIV);
        testPlanConfigTDObject.append(testPlanConfigTCsTable);

        var testPlanRemoveTDObject = $('<td>', {
            'width': '5%'
        });
        var testPlanRemoveBtn = $('<button>', {
            'class': 'aui-button'
        })
        var testPlanRemoveBtnSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-trash'
        })
        testPlanRemoveBtn.append(testPlanRemoveBtnSpan);
        testPlanRemoveBtn.click(function (event) {
            $(event.currentTarget).parent().parent().remove();
            if ($(event.currentTarget).parent().parent().attr('id') !== 'TPDefaultPlan-test-exec-test-plan-tr') {
                var planObject = {
                    'id': plan.id,
                    'name': plan.name,
                    'count': plan.testCases.length
                }
                if (plan.name.length > 0) {
                    appendTestExecTestPlanNodes($('#test-schedule-test-plan-ul'), planObject);
                    $('div.test-schedule-test-plans-grouping').animate({
                        scrollTop: $('div.test-schedule-test-plans-grouping').prop('scrollHeight')
                    }, 500);
                }
                event.preventDefault();
            }
        });
        testPlanRemoveTDObject.append(testPlanRemoveBtn);

        testPlanTRObject.append(testPlanNameTDObject);
        testPlanTRObject.append(testPlanConfigTDObject);
        testPlanTRObject.append(testPlanRemoveTDObject);

        parent.append(testPlanTRObject);
    }

    function addSelectedTestCaseToList(parent, testCase, isRun, count) {

        var testPlanConfigTCsTBODY_TR = $('<tr>', {
            'id': testCase.id + '-test-exec-test-plan-tr-tcs',
            'test-case-id': testCase.testCaseId
        });

        var testPlanConfigTCsTBODY_TR_TD_Name = $('<td>', {
            'style': 'vertical-align: middle; border: none; margin-top:5px'
        });
        testPlanConfigTCsTBODY_TR_TD_Name.text(testCase.name);

        var testPlanConfigTCsTBODY_TR_TD_Automated = $('<td>', {
            'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
        });
        var testPlanConfigTCsTBODY_TR_TD_Automated_Input = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': testCase.id + '-test-exec-test-plan-automated-check',
            'id': testCase.id + '-test-exec-test-plan-automated-check',
            'style': 'margin: auto;',
            'disabled': ''
        });
        testPlanConfigTCsTBODY_TR_TD_Automated.append(testPlanConfigTCsTBODY_TR_TD_Automated_Input);

        var testPlanConfigTCsTBODY_TR_TD_Manual = $('<td>', {
            'style': 'width: 10%; vertical-align: middle; border: none; text-align: center;'
        });
        var testPlanConfigTCsTBODY_TR_TD_Manual_Input = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': testCase.id + '-test-exec-test-plan-manual-check',
            'id': testCase.id + '-test-exec-test-plan-manual-check',
            'style': 'margin: auto;',
            'disabled': ''
        });
        testPlanConfigTCsTBODY_TR_TD_Manual.append(testPlanConfigTCsTBODY_TR_TD_Manual_Input);

        var testPlanConfigTCsTBODY_TR_TD_Agent = $('<td>', {
            'style': 'width: 20%; border: none; vertical-align: middle;'
        });
        var testPlanConfigTCsTBODY_TR_TD_Agent_Select = $('<select>', {
            'class': 'select form-control',
            'id': testCase.id + '-test-exec-test-plan-agent-select',
            'name': testCase.id + '-test-exec-test-plan-agent-select',
        });

        var testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option.text('Select');
        testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select_Option);

        // Agents
        if (testCase.automated == 'Yes') {
            var isExecuted = false;
            count += 1;
            if (isRun) {
                if (count == 1) {
                    isExecuted = true;
                }
            } else {
                if (count == 1) {
                    isExecuted = true;
                }
            }

            var tdAgentSelect_Pool_Optgroup = $('<optgroup>', {
                'label': 'Pools'
            });

            var isEmptyPool = setPools(tdAgentSelect_Pool_Optgroup, isExecuted);

            testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Pool_Optgroup)
            var tdAgentSelect_Agent_Optgroup = $('<optgroup>', {
                'label': 'Agents'
            });

            var isEmptyAgents = setAgents(tdAgentSelect_Agent_Optgroup, isExecuted);

            testPlanConfigTCsTBODY_TR_TD_Agent_Select.append(tdAgentSelect_Agent_Optgroup);

            if (typeof isEmptyPool != 'undefined' && typeof isEmptyAgents != 'undefined' && isEmptyPool && isEmptyAgents) {
                AJS.flag({
                    type: 'error',
                    title: 'Agent Unavailable',
                    close: 'auto'
                });
            }
        }

        if (testCase.agent) {
            testPlanConfigTCsTBODY_TR_TD_Agent_Select.val(testCase.agent);
        }

        var testPlanConfigTCsTBODY_TR_TD_User = $('<td>', {
            'style': 'width: 20%; border: none; vertical-align: middle;'
        });
        var testPlanConfigTCsTBODY_TR_TD_User_Select = $('<select>', {
            'class': 'select form-control',
            'id': testCase.id + '-test-exec-test-plan-user-select',
            'name': testCase.id + '-test-exec-test-plan-user-select'
        })
        var testPlanConfigTCsTBODY_TR_TD_User_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_User_Select_Option.text('Select');
        testPlanConfigTCsTBODY_TR_TD_User_Select.append(testPlanConfigTCsTBODY_TR_TD_User_Select_Option);

        // Users
        setUsers(testPlanConfigTCsTBODY_TR_TD_User_Select, testCase.user);

        // Browser selection
        var testPlanConfigTCsTBODY_TR_TD_Browser = $('<td>', {
            'style': 'width: 20%; border: none; vertical-align: middle;'
        });
        var testPlanConfigTCsTBODY_TR_TD_Browser_Select = $('<select>', {
            'class': 'select form-control',
            'id': testCase.id + '-test-exec-test-plan-browser-select',
        })

        var testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option = $('<option>');
        testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option.text('Select');
        testPlanConfigTCsTBODY_TR_TD_Browser_Select.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select_Option);

        testPlanConfigTCsTBODY_TR_TD_Browser.append(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

        setBrowser(testPlanConfigTCsTBODY_TR_TD_Browser_Select);

        if (testCase.automated === 'Yes') {
            testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', true);
            testPlanConfigTCsTBODY_TR_TD_Agent.append(testPlanConfigTCsTBODY_TR_TD_Agent_Select);
        } else {
            testPlanConfigTCsTBODY_TR_TD_Automated_Input.prop('checked', false);
        }

        if (testCase.manual === 'Yes') {
            testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', true);
            testPlanConfigTCsTBODY_TR_TD_User.append(testPlanConfigTCsTBODY_TR_TD_User_Select);
        } else {
            testPlanConfigTCsTBODY_TR_TD_Manual_Input.prop('checked', false);
        }

        var testPlanConfigTCsTBODY_TR_TD_ExpectedResult = $('<td>', {
            'style': 'vertical-align: middle; display: none; border: none;'
        });
        testPlanConfigTCsTBODY_TR_TD_ExpectedResult.text(testCase.overallExpectedResult);

        var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI = $('<td>', {
            'style': 'vertical-align: middle; border: none;'
        });
        var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton = $('<button>', {
            'id': testCase.id + '-auto-gen-ai-button',
            'class': 'aui-button',
            'title': 'Auto Generate Configuration from AI',
            'style': 'outline: none;'
        }).on('click', function (event) {
            var testCaseId = $(this).attr('id').split('-')[0];
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/execution-entity-data-management',
                async: false,
                data: {
                    user: username,
                    action: 'get-executed-test-case-data',
                    data: JSON.stringify({
                        'test_case_id': testCaseId
                    })
                },
                success: function (response) {
                    var responseObject = JSON.parse(response);
                    if (responseObject.id == '' && typeof responseObject.id == 'undefined') {
                        AJS.flag({
                            type: 'error',
                            title: 'Not Executed Previously',
                            body: 'No Such Record To Display',
                            close: 'auto'
                        });
                    } else {
                        autoGenerateFromAI(parent, responseObject, testCaseId);
                    }
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'Not Executed Previously..',
                        close: 'auto'
                    });
                }
            });
        });

        testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.tooltip({
            gravity: 'w'
        });

        var testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-vid-raised-hand',
            'style': 'height: 20px; width: 20px;'
        });
        testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButtonSpan);
        testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAIButton);

        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Name);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Automated);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Manual);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Agent);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_User);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_Browser);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_ExpectedResult);
        testPlanConfigTCsTBODY_TR.append(testPlanConfigTCsTBODY_TR_TD_AutoGenerateAI);

        parent.append(testPlanConfigTCsTBODY_TR);
    }

    function appendSchedulePlanList(isFromCancel, param, scheduleName) {
        var isRun = false;
        var count = 0;
        var planExecuted = true;
        var eEntityInstanceId;
        if (isFromCancel) {
            eEntityInstanceId = param;
        } else {
            eEntityInstanceId = $(param.currentTarget).attr('id').split('-')[0]
        }
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            data: {
                user: username,
                action: 'get-schedule-data',
                data: JSON.stringify({
                    'id': eEntityInstanceId
                })
            },
            success: function (response) {

                var planCount = 1;
                var responseObject = JSON.parse(response);
                var planList = responseObject.plans;
                $('tbody#selected-schedule-table-tbody').empty();
                var testPlanTRObj = $('<tr>', {
                    'style': 'margin: -10px;'
                });

                var testPlanNameTDObject = $('<td>', {
                    'width': '15%',
                    'style': 'border-bottom: none; font-weight: 550; padding-bottom: 5px;'
                });

                testPlanNameTDObject.text(scheduleName);

                var testPlanConfigTDObject = $('<td>', {
                    'width': '80%',
                    'style': 'border-bottom: none; padding-bottom: 18px;'
                });

                var testPlanEditBtn = $('<button>', {
                    'class': 'aui-button',
                    'style': 'position: absolute; margin-top: 5px; outline: none; right: 1%;',
                    'title': 'Edit Schedule',
                    'id': eEntityInstanceId + 'edit-button'
                }).on('click', function (event) {
                    $('#schedule-hr').css("display", "block");
                    $('#test-schedule-test-plans-div').css("display", "block");
                    $('#test-schedule-test-plans-div').css("height", "300px");
                    $('#schedule-test-case-header').css("display", "block");
                    $('#scheduler-test-case-div').css("display", "block");
                    $('#test-schedule-div').css("display", "none");
                    $('#test-exec-actions-div').css("display", "block");
                    $('#schedule-view-header').text("EDIT SCHEDULED EXECUTIONS");
                    initTestExecTestPlan();
                    initTestExecTC('null');
                    editViewInit(eEntityInstanceId);
                    selectedEditEntityInstanceId = eEntityInstanceId;
                    selectedEditScheduleName = scheduleName;
                    AJS.$('#' + event.currentTarget.id).tooltip('hide');
                });

                testPlanEditBtn.tooltip({
                    gravity: 'e'
                });

                var testPlanEditBtnSpan = $('<span>', {
                    'class': 'aui-icon aui-icon-small aui-iconfont-edit-filled'
                });
                testPlanEditBtn.append(testPlanEditBtnSpan);
                testPlanConfigTDObject.append(testPlanEditBtn);
                testPlanTRObj.append(testPlanNameTDObject);
                testPlanTRObj.append(testPlanConfigTDObject);
                $('tbody#selected-schedule-table-tbody').append(testPlanTRObj);

                planList.forEach(planObj => {
                    addSchedulePlanToList($('tbody#selected-schedule-table-tbody'), planObj, isRun, count, planExecuted, planCount);
                });
                $('div.selected-schedule-table-content').animate({
                    scrollTop: $('div.selected-schedule-table-content').prop('scrollHeight')
                }, 500);

            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Schedules..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#edit-schedule-name').keyup(function (e) {
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

    $('body').on('contextmenu', treeNodeATagSelector, function (event) {
        $scheduleContext.hide();
        if ($(event.currentTarget).parent().parent().attr('type') === 'schedule') {
            selectedScheduleID = $(event.currentTarget).parent().attr('id');
            status = $(event.currentTarget).parent().attr('status');
            $scheduleContext.css({
                display: "block",
                left: event.pageX,
                top: 'calc(' + event.pageY + 'px - 40px)'
            });
            $scheduleContext.show();
        }
        return false;
    });

    $('#deleteScheduleLI').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#warning-dialog-folder').show();
    });

    $('#schedule-delete-confirm').on('click', function () {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
            async: false,
            data: {
                user: username,
                action: 'delete-schedule',
                data: JSON.stringify({
                    'id': selectedScheduleID.split('-')[0],
                    'status': status,
                    'isFromDelete': true
                })
            },
            success: function (response) {
                var responseObj = JSON.parse(response);
                if (responseObj) {
                    $('li#' + selectedScheduleID).remove();
                    $('#selected-schedule-table-tbody').empty();
                    AJS.dialog2('#warning-dialog-folder').hide();
                    AJS.flag({
                        type: 'success',
                        title: 'Schedule Deleted..',
                        close: 'auto'
                    });
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Schedule..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('#schedule-delete-cancel').on('click', function () {
        AJS.dialog2('#warning-dialog-folder').hide();
    });
});