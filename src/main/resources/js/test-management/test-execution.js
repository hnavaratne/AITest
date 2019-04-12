AJS.toInit(function ($) {

    'use strict';

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');
    //Heat Map Related
    if ((typeof (echarts) !== "undefined")) {
        var dom = $('#container')[0];
        var myChart = echarts.init(dom);
        var option = null;
        myChart.showLoading();
        var dataList;
    }
    //if(typeof(useGentleSelect)!=='undefined'){
    // $('#selector').cron({
    //     onChange: function() {
    //         $('#cron-value').val($(this).cron("value"));
    //     },
    //     useGentleSelect: true
    // });
    //  }

    var selectTestExecTFId;

    // var host = '54.149.85.163';
    // var port = '8181';

    var host = 'cmlvdhhewapathi';
    var port = '8080';

    // var host = 'CD-TAHFERNANDO';
    // var port = '8181';

    var centroidHost;
    var centroidUrl;
    var centroidPort;

    var isValidated = false;
    var parallelExecutionStatus = true;
    var selectedTestCasesCount = 0;
    var selectorElementVersion = "";

    var spinnerUI = $('<aui-spinner>', {
        'size': 'large'
    });

    var testPlanId;
    var selectedBrowserForTestPlan;

    function initTestExecTestPlan() {
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
                    appendTestExecTestPlanNodes($('#test-execution-test-plan-ul'), planObject);
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

    $('body').on('click', 'ul#test-execution-test-plan-ul li', function (event) {
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
                addSelectedPlanToList($('tbody#selected-test-exec-table-tbody'), responseObject, isRun, count, planExecuted, planCount);
                $(event.currentTarget).remove();
                $('div.selected-test-exec-table-content').animate({
                    scrollTop: $('div.selected-test-exec-table-content').prop('scrollHeight')
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
                if ($('tbody#selected-test-exec-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').length == 0) {
                    addDefaultTestPlan($('tbody#selected-test-exec-table-tbody'), isRun, count);
                }
                addSelectedTestCaseToList($('tbody#selected-test-exec-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').find('table.aui tbody'), response, isRun, count);
                $(event.currentTarget).remove();
                $('div.selected-test-exec-table-content').animate({
                    scrollTop: $('div.selected-test-exec-table-content').prop('scrollHeight')
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

    var tableHEADBGColor = '#f4f5f7';
    var tableHEADColor = '#000';

    function addSelectedTestCaseToList(parent, testCase, isRun, count) {

        var testPlanConfigTCsTBODY_TR = $('<tr>', {
            'id': testCase.id + '-test-exec-test-plan-tr-tcs'
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

    function addDefaultTestPlan(parent, isRun, count) {
        var plan = {
            'id': 'TPDefaultPlan',
            'name': 'Ad Hoc Test Plan',
            'testCases': []
        }
        addSelectedPlanToList(parent, plan, isRun, count);
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
                'id': testCase.testCase.id + '-test-exec-test-plan-tr-tcs'
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
            console.log(event);
            event.preventDefault();
            $(event.currentTarget).parent().parent().remove();
            if ($(event.currentTarget).parent().parent().attr('id') !== 'TPDefaultPlan-test-exec-test-plan-tr') {
                var planObject = {
                    'id': plan.id,
                    'name': plan.name,
                    'count': plan.testCases.length
                }
                appendTestExecTestPlanNodes($('#test-execution-test-plan-ul'), planObject);
                $('div.test-execution-test-plans-grouping').animate({
                    scrollTop: $('div.test-execution-test-plans-grouping').prop('scrollHeight')
                }, 500);
            }
        })
        testPlanRemoveTDObject.append(testPlanRemoveBtn);

        testPlanTRObject.append(testPlanNameTDObject);
        testPlanTRObject.append(testPlanConfigTDObject);
        testPlanTRObject.append(testPlanRemoveTDObject);

        parent.append(testPlanTRObject);
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
                    'redirectView': '/secure/TestExecutionView.jspa'
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

    $('#execute-test-plan-btn').click(function (event) {
        event.preventDefault();
        if ($('#selected-test-exec-table-tbody').children().length !== 0) {
            getVersion('#exection-entity-release');
            AJS.dialog2('#run-execution-entity-dialog').show();
        }
    });

    $('#run-exection-entity-btn').click(function (event) {
        var agentValidationCounter = 0;
        var userValidationCounter = 0;
        var browserValidationCounter = 0;
        var selectedRelease = $('#exection-entity-release').val();
        var versionDesc = $('#exection-entity-release-description').val();
        var isChecked = toggle.prop("checked");
        if (isChecked) {
            parallelExecutionStatus = true;
        }
        if (selectedRelease != '') {
            var validateFields = true;
            var execEntityObject = {
                'release': selectedRelease,
                'description': versionDesc,
                'plans': [],
                'parallelExecution': parallelExecutionStatus,
                'isFromSchedule': false
            }
            var selectedTestPlans = $('#selected-test-exec-table-tbody').children();
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
                    if (typeof testAgentStatus == 'undefined') {
                        testAgentStatus = false;
                    } else {
                        testAgentStatus = $('option:selected', testCaseAgentTD).attr('agentstatus');
                    }
                    var testCaseObject = {
                        'testCaseId': testCaseTR.attr('id').split('-')[0],
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
                            //var id = $(testCaseAgentTD.find('select')).attr('id');
                            // testCaseObject.agent = $("#"+id+ " option:selected").text();
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
                                // var id = $(testCaseAgentTD.find('select')).attr('id');
                                // testCaseObject.agent = $("#"+id+ " option:selected").text();
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
                                url: baseUrl + 'plugins/servlet/exe-entity',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'insert',
                                    data: JSON.stringify(execEntityObject)
                                },
                                success: function (response) {
                                    var inserted = JSON.parse(response);
                                    $.ajax({
                                        type: 'POST',
                                        url: baseUrl + 'plugins/servlet/exe-entity',
                                        async: false,
                                        data: {
                                            user: username,
                                            action: 'clone',
                                            data: JSON.stringify({
                                                'id': inserted.id
                                            })
                                        },
                                        success: function (clonedInstance) {
                                            var result = JSON.parse(clonedInstance);
                                            var cloneOfInstance = JSON.parse(clonedInstance);
                                            var automatedResult;
                                            var hasAttachedTestData = true;

                                            for (let index = 0; index < result["plans"].length; index++) {
                                                var plans = result["plans"][index];
                                                for (let testCaseCon = 0; testCaseCon < plans["testCases"].length; testCaseCon++) {
                                                    if (!(plans["testCases"][testCaseCon]["automated"] == "Yes")) {
                                                        cloneOfInstance["plans"][index]["testCases"].splice([testCaseCon], 1);
                                                    }
                                                }
                                            }

                                            sendAutomatedTestCases(cloneOfInstance);

                                            window.location.replace(baseUrl + 'secure/TestReportsView.jspa');
                                            AJS.dialog2('#run-execution-entity-dialog').hide();
                                        }, error: function (jqXHR, textStatus, errorThrown) {
                                            AJS.flag({
                                                type: 'error',
                                                title: 'Clone Error..',
                                                close: 'auto'
                                            });
                                        }
                                    });
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Cloning The Data..',
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
            } else {
                AJS.dialog2('#run-execution-entity-dialog').hide();
            }
        }
    });

    function sendAutomatedTestCases(cloneOfInstance) {
        console.log(JSON.stringify({
            'data': cloneOfInstance
        }));
        if (centroidHost != '' && centroidPort != '') {
            $.ajax({
                type: 'POST',
                url: 'http://' + centroidHost + ':' + centroidPort + '/control-center/services/test/execute',
                dataType: 'json',
                contentType: 'application/json',
                async: true,
                data: JSON.stringify(cloneOfInstance),
                success: function (centroidResponse) {
                    console.log('centroidResponse', centroidResponse);
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Sending Data To Automation..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        }
    }

    $('#cancel-exection-entity-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#run-execution-entity-dialog').hide();
    });

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

    function getNonReleasedVersion() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-management-util',
            async: false,
            data: {
                user: username,
                action: 'get-non-released-versions',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                addReleaseSelectOptions($('#impact-analysis-release-selector'), {
                    'id': 'select-version',
                    'value': 'Select Version',
                    'disabled': ''
                });
                $('#impact-analysis-release-selector').empty();
                for (var i = 0; i < response.length; i++) {
                    selectorElementVersion = response[i];
                    addReleaseSelectOptions($('#impact-analysis-release-selector'), response[i]);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Released Versions..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        return selectorElementVersion;
    }

    function addReleaseSelectOptions(parent, option) {
        var optionObject = $('<option>', {
            'id': option.id,
            'value': option.name
        });
        optionObject.text(option.name);
        parent.append(optionObject);
    }

    $('#create-exection-entity-btn').click(function (event) {
        event.preventDefault();
        var executionEntityObject = {
            'name': $('#new-exection-entity-name').val()
        };
        $('#new-exection-entity-name').val('');
        AJS.dialog2('#add-execution-entity-dialog').hide();
        $('#execution-entity-name').val(executionEntityObject.name);
        $('#test-execution-table-container').attr('style', 'display: block');
    });

    $('#execution-entity-btn').click(function (event) {
        $.ajax({
            type: 'GET',
            url: 'http://' + centroidHost + ':' + centroidPort + '/accello-web/services/test/execute',
            async: true,
            success: function (response) {
                console.log(response)
            }
        });
        $('#execution-run-spinner').attr('style', 'display: block;');

        window.location.replace(baseUrl + 'secure/TestReportsView.jspa?name=' + $('#execution-entity-name').val());
    });

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

    // open dialog for browser selection
    function openBrowserDialog() {
        var planId = $(this).attr('planId');
        var testPlanName = $(this).attr('testPlanName');
        testPlanId = planId;
        var browserHeader = $('.aui-dialog2-header-main');
        browserHeader.text(testPlanName);
        AJS.dialog2("#browser-selector-dialog").show();

        // Apply browser from test plan dialog
        $('#apply-browser-btn').click(function () {
            selectedBrowserForTestPlan = $("#browser-select-test-plan").val();
            var selectedTestPlans = $('#selected-test-exec-table-tbody').children();
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
            AJS.dialog2("#browser-selector-dialog").hide();
        });

        // Hide browser from test plan dialog
        $('#cancel-browser-btn').click(function () {
            AJS.dialog2("#browser-selector-dialog").hide();
        });
    }

    var toggle = $('#parallel-execution');
    toggle.click('change', function (e) {
        var isChecked = toggle.prop("checked");
        if (isChecked) {
            parallelExecutionStatus = true;
        } else {
            parallelExecutionStatus = false;
        }
    });

    // Auto Generate From AI
    function autoGenerateFromAI(parent, responseObject, testCaseId) {
        if (responseObject.testCaseAgent) {
            $(parent).find('tr').find('td select[name=' + testCaseId + '-test-exec-test-plan-agent-select]').val(responseObject.testCaseAgent);
        }
        if (responseObject.testCaseUser) {
            $(parent).find('tr').find('td select[name=' + testCaseId + '-test-exec-test-plan-user-select]').val(responseObject.testCaseUser);
        }
    }
    // End here
    // Impact analysis dialog
    $('#impact-anaylsis-btn').click('click', openImpactAnalysisDialog);
    function openImpactAnalysisDialog() {
        $('#impact-analysis-item').removeClass('aui-nav').addClass('aui-nav-selected');
        $('#heat-map-item').removeClass('aui-nav-selected').addClass('aui-nav');
        selectedTestCasesCount = 0;
        $('#added-to-build').text("Added to build - " + selectedTestCasesCount);
        $('#components-results-table-tbody').empty();
        $('#requirment-data-body').empty();
        $('#total-test-casses').text('All - ' + 0);
        var releasedVersion = getNonReleasedVersion();
        if (releasedVersion.id != "" && typeof releasedVersion.id != 'undefined') {
            var version = releasedVersion.name;
            $('#impact-analysis-release-selector').auiSelect2('val', version);
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/components',
                async: false,
                data: {
                    user: username,
                    action: 'get-components',
                    data: JSON.stringify({
                        'release': releasedVersion.name
                    })
                },
                success: function (response) {
                    var responseObject = JSON.parse(response);
                    if (JSON.parse(response).length < 0) {
                        AJS.flag({
                            type: 'error',
                            title: 'Components Unavailable For This Project',
                            close: 'auto'
                        });
                    } else {
                        $('#components-results-table-tbody').empty();
                        $('#requirment-data-body').empty();
                        $('#total-test-casses').text('All - ' + 0);
                        generateComponents($('#components-results-table-tbody'), responseObject);
                        $('#heat-map-tab').css('display', 'none');
                        if ($('#impact-analysis-tab').css('display') == 'flex') {
                            $('#impact-analysis-tab').css('display', '');
                        } else if ($('#impact-analysis-tab').css('display') == 'none') {
                            $('#impact-analysis-tab').css('display', '');
                        }
                        AJS.dialog2("#impact-analysis-dialog").show();
                    }
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Components..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        } else {
            AJS.flag({
                type: 'error',
                title: 'Components Unavailable for this project',
                close: 'auto'
            });
        }
    }

    $('#impact-analysis-item').click(function () {
        $('#impact-analysis-tab').css('display', 'none');
        $('#impact-analysis-tab').css('display', 'block');
        $('#impact-analysis-tab').css('display', '');
        $('#heat-map-tab').css('display', 'none');
        $('#impact-analysis-item').removeClass('aui-nav').addClass('aui-nav-selected');
        $('#heat-map-item').removeClass('aui-nav-selected').addClass('aui-nav');
        var selectedRelease = $('#impact-analysis-release-selector').val();
        generateImpactAnalysisData(selectedRelease);
    });

    $('#heat-map-item').click(function () {
        $('#impact-analysis-tab').css('display', 'none');
        $('#heat-map-tab').css('display', 'block');
        $('#impact-analysis-item').removeClass('aui-nav-selected').addClass('aui-nav');
        $('#heat-map-item').removeClass('aui-nav').addClass('aui-nav-selected');
        var releasedVersion = $('#impact-analysis-release-selector').val();
        generateHeatMapList(releasedVersion);
        generateEChart(dataList);
    });

    $('#impact-analysis-release-selector').click('change', function () {
        $('#components-results-table-tbody').empty();
        $('#requirment-data-body').empty();
        $('#total-test-casses').text('All - ' + 0);
        var selectedRelease = $('#impact-analysis-release-selector').val();
        if (!selectedRelease == '') {
            if ($('#heat-map-tab').css('display') == "block") {
                generateHeatMapList(selectedRelease);
                generateEChart(dataList);
                $('#heat-map-tab').click();
            } else {
                generateImpactAnalysisData(selectedRelease);
            }
        }
    });

    function generateImpactAnalysisData(selectedRelease) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/components',
            async: false,
            data: {
                user: username,
                action: 'get-components',
                data: JSON.stringify({
                    'release': selectedRelease
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (typeof responseObject != "undefined") {
                    generateComponents($('#components-results-table-tbody'), responseObject);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Components Related To A Version..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function generateComponents(parent, responseObject) {
        $('#components-results-table-tbody').empty();
        componentRanger(parent);
        addComponentNode(parent);
        $.each(responseObject, function (i, componentObj) {
            addComponentTPNode(parent, componentObj);
        });
        // if (componentNames["componentNames"] != undefined && componentNames["componentNames"].length > 0) {
        //     for (var index = 0; index < componentNames["componentNames"].length; index++) {
        //         if (typeof componentNames["componentNames"][index] != "undefined") {
        //             addComponentTPNode(parent, componentNames["componentNames"][index]);
        //         }
        //     }
        // }
    }

    function addComponentNode(parent) {

        var trObject = $('<tr>', {
            'data-level': '1',
            'id': 'component-root',
            'style': 'background: #dfe1e687; user-select: none; cursor: pointer;'
        });

        var tdComponentObject = $('<td>', {
            'style': 'width: 30%;'
        });

        var tdComponentTitleSpanObject = $('<span>', {
            'style': 'font-size: 15px; font-weight: 600; color: BLACK;'
        });
        tdComponentTitleSpanObject.text('Components');

        tdComponentObject.append(tdComponentTitleSpanObject);
        trObject.append(tdComponentObject);

        parent.append(trObject);
    }

    function componentRanger(parent) {

        var trObject = $('<tr>', {
            'id': 'component-ranger',
            'style': 'height: 25px;'
        });

        var tdComponentRangerObject = $('<td>', {
            'style': 'border: none !important; vertical-align: bottom;'
        });

        var divRangerTopic = $('<div>', {
            'style': 'margin-bottom: 5px; margin-top: -7px;'
        });

        var topicLabel = $('<span>', {
            'text-alignt': 'center',
            'style': 'font-weight:bold;'
        });
        topicLabel.text("Impact");

        divRangerTopic.append(topicLabel);

        tdComponentRangerObject.append(divRangerTopic);

        var divContainer = $('<div>', {
            'class': 'slider',
            'style': 'width: 100%; display: -webkit-box; border-radius: 25px;'
        });

        var percentage = 25;
        var previousPercentage = 0;
        var backgroundColors = ['#FFD031', '#56D78B', '#FF6530', '#c23531'];
        for (var i = 0; i < 4; i++) {
            if (i == 0) {
                var divGridItem = $('<div>', {
                    'id': 'grid-item' + i,
                    'style': 'width: 25%; height: 15px; border-top-left-radius: 25px; border-bottom-left-radius: 25px; background:' + backgroundColors[i],
                    'data-title': previousPercentage + '% - ' + percentage + '%'
                });
            } else if (i == 3) {
                var divGridItem = $('<div>', {
                    'id': 'grid-item' + i,
                    'style': 'width: 25%; height: 15px; border-top-right-radius: 25px;  border-bottom-right-radius: 25px; background:' + backgroundColors[i],
                    'data-title': previousPercentage + '% - ' + percentage + '%'
                });
            } else {
                var divGridItem = $('<div>', {
                    'id': 'grid-item' + i,
                    'style': 'width: 25%; height: 15px; background:' + backgroundColors[i],
                    'data-title': previousPercentage + '% - ' + percentage + '%'
                });
            }

            // .on('click', function () {
            //     $('.grid-item').removeClass('active-status');
            //     $(this).addClass('active-status');
            //     if (componentNames["componentNames"] != undefined && componentNames["componentNames"].length > 0) {
            //         for (var index = 0; index < componentNames["componentNames"].length; index++) {
            //             if (typeof componentNames["componentNames"][index] != "undefined") {
            //                 addComponentTPNode(parent, componentNames["componentNames"][index]);
            //             }
            //         }
            //     }
            // });

            previousPercentage = percentage;
            percentage += 25;
            divContainer.append(divGridItem);
        }

        tdComponentRangerObject.append(divContainer);

        var divLabel = $('<div>', {
            'style': 'margin-top: 5px;'
        });

        var startLabel = $('<span>', {
            'style': 'float: left;'
        });
        startLabel.text("Low");

        var endLabel = $('<span>', {
            'style': 'float: right;'
        });
        endLabel.text("High");

        divLabel.append(startLabel);
        divLabel.append(endLabel);

        tdComponentRangerObject.append(divLabel);
        trObject.append(tdComponentRangerObject);

        parent.append(trObject);

    }

    function addComponentTPNode(parent, componentObj) {

        var backgroundColor = getComponentColor(componentObj);

        var trObject = $('<tr>', {
            'data-level': '2',
            'id': componentObj.ComponentName + '-component-name',
            'style': 'user-select: none; background: ' + backgroundColor
        });
        trObject.click(function (event) {
            addComponentDetails(componentObj.ComponentName);
        });

        // trObject.hover(function () {
        //     $(this).addClass("highlight");
        //     trObject.css('background-color', 'white');
        // }, function () {
        //     $(this).removeClass("highlight");
        //     trObject.css('background-color', backgroundColor);
        // });

        var tdComponentObject = $('<td>', {
            'style': 'width: 25%;'
        });

        var tdComponentSpanObject = $('<span>', {
            'style': 'font-size: 12px; font-weight: 550; color: BLACK; margin-left: 30px; vertical-align: middle;'
        });
        tdComponentSpanObject.text(componentObj.ComponentName);

        tdComponentObject.append(tdComponentSpanObject);

        trObject.append(tdComponentObject);

        parent.append(trObject);

        var seen = {};
        $('#components-results-table-tbody tr').each(function () {
            var txt = $(this).text();
            if (seen[txt])
                $(this).remove();
            else
                seen[txt] = true;
        });
    }

    function getComponentColor(componentObj) {
        var color = "";
        if (componentObj.Percentage >= 0 && componentObj.Percentage <= 25) {
            color = "#FFD031";
        } else if (componentObj.Percentage > 25 && componentObj.Percentage <= 50) {
            color = "#56D78B";
        } else if (componentObj.Percentage > 50 && componentObj.Percentage <= 75) {
            color = "#FF6530";
        } else if (componentObj.Percentage > 75 && componentObj.Percentage <= 100) {
            color = "#c23531";
        }
        return color;
    }

    function addComponentDetails(componentName) {
        $('#requirment-data-body').empty();
        var parent = $('#requirment-data-body');
        var selectedRelease = $('#impact-analysis-release-selector').val();

        if (!componentName == '') {
            $.ajax({
                type: 'POST',
                url: baseUrl + 'plugins/servlet/components',
                async: false,
                data: {
                    user: username,
                    action: 'get-test-cases-for-component',
                    data: JSON.stringify({
                        'componentName': componentName,
                        'release': selectedRelease
                    })
                },
                success: function (response) {
                    var responseObject = JSON.parse(response);
                    var totalTestCasesCount = responseObject.totalTestCasesCount;
                    generateComponentDetails(parent, responseObject);

                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Components Related To A Test Case..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            })
        }
    }

    function generateComponentDetails(parent, responseObject) {
        var testCasesCount = 0;

        $('#total-test-casses').text('All - ' + 0);

        $.each(responseObject, function (key, value) {

            if (value.length > 0) {

                testCasesCount += value.length;

                $('#total-test-casses').text('All - ' + testCasesCount);

                var trRequirementObject = $('<tr>', {
                    'data-level': '1',
                    'id': key + '-requirement-name',
                    'style': 'background: lightgrey; user-select: none;'
                });

                var tdRequirementObject = $('<td>', {
                    'class': 'requirement-table',
                });

                var tdComponentSpanObject = $('<span>', {
                    'style': 'font-size: 12px; font-weight: 550; color: BLACK;'
                });
                tdComponentSpanObject.text(key);

                tdRequirementObject.append(tdComponentSpanObject);

                trRequirementObject.append(tdRequirementObject);

                parent.append(trRequirementObject);

                $.each(value, function (key, value) {

                    $.each(value, function (key, value) {

                        var trTestCaseObject = $('<tr>', {
                            'data-level': '2',
                            'id': key + '-requirement-id',
                            'name': value + '-requirement-name',
                            'style': 'background: #dfe1e687; user-select: none;'
                        });

                        var tdTestCaseObject = $('<td>', {
                            'class': 'requirement-table',
                        });

                        var tdTestCaseSpanObject = $('<span>', {
                            'style': 'font-size: 11px; font-weight: 550; color: BLACK; margin-left: 30px; vertical-align: middle;'
                        });
                        tdTestCaseSpanObject.text(value);

                        tdTestCaseObject.append(tdTestCaseSpanObject);

                        var tdAddCheckBox = $('<input>', {
                            'id': key + '-add-check-box-id',
                            'name': value + '-add-check-box-name',
                            'class': 'checkbox-button',
                            'style': 'float:right; margin-top: 2px; height: 15px; width:15px',
                            'testCase': value,
                            'type': 'checkbox',
                            'unchecked': "unchecked"
                        });

                        tdTestCaseObject.append(tdAddCheckBox);

                        trTestCaseObject.append(tdTestCaseObject);

                        parent.append(trTestCaseObject);
                    });

                });
            } else {
                $('#total-test-casses').text('All - ' + 0);
            }
        });
    }

    $('#selected-test-exec-table').children().each(function () {
        if ($(this).find('tr').attr('id') != undefined) {

        }
    });

    $("input[class='checkbox-button']").live("click", function () {
        if ($(this).is(':checked')) {
            selectedTestCasesCount += 1;
            $('#added-to-build').text("Added to build - " + selectedTestCasesCount);
        } else {
            selectedTestCasesCount -= 1;
            $('#added-to-build').text("Added to build - " + selectedTestCasesCount);
        }
    });

    $('#add-to-build-btn').on('click', function (event) {
        var checkedTestCases = new Array();
        $('#requirment-data-body').children().each(function () {
            if ($(this).find('input').attr('id') != undefined && $(this).find('input').prop('checked')) {
                checkedTestCases.push($(this).find('input').attr('id').split('-')[0]);
                $("input[id='" + $(this).find('input').attr('id').split('-')[0] + "-add-check-box-id']").parent().parent().remove();
            }
        });

        $.each(checkedTestCases, function (index, value) {
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
                        'id': value
                    })
                },
                success: function (response) {
                    var response = JSON.parse(response);
                    if ($('tbody#selected-test-exec-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').length == 0) {
                        addDefaultTestPlan($('tbody#selected-test-exec-table-tbody'), isRun, count);
                    }
                    addSelectedTestCaseToList($('tbody#selected-test-exec-table-tbody').find('tr#TPDefaultPlan-test-exec-test-plan-tr').find('table.aui tbody'), response, isRun, count);
                    $('div.selected-test-exec-table-content').animate({
                        scrollTop: $('div.selected-test-exec-table-content').prop('scrollHeight')
                    }, 500);
                }, error: function (jqXHR, textStatus, errorThrown) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Test Cases..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        });
    });

    // End here

    if (window.location.href.indexOf('TestExecutionView.jspa') >= 0) {
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
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        });
        AJS.$("#exection-entity-release").auiSelect2();
        AJS.$("#schedule-entity-release").auiSelect2();
        AJS.$("#impact-analysis-release-selector").auiSelect2();
        initTestExecTestPlan();
        initTestExecTC('null');
    }

    function generateEChart(dataList) {
        myChart.resize();
        myChart.hideLoading();
        myChart.setOption(option = {
            title: {
                text: 'Heat Map',
                left: 'leafDepth'
            },
            tooltip: {},
            series: [{
                name: 'Root',
                type: 'treemap',
                visibleMin: 300,
                data: dataList,
                leafDepth: 1,
                levels: [
                    {
                        itemStyle: {
                            normal: {
                                borderColorSaturation: 0.5,
                                gapWidth: 1
                            }
                        }
                    },
                    {
                        colorSaturation: [0.45, 0.5],
                        itemStyle: {
                            normal: {
                                borderColorSaturation: 0.6,
                                gapWidth: 1
                            }
                        }
                    },
                    {
                        colorSaturation: [0.4, 0.45],
                        itemStyle: {
                            normal: {
                                borderColorSaturation: 0.5,
                                gapWidth: 1
                            }
                        }
                    }
                ]
            }]
        });
    }

    function generateHeatMapList(releasedVersion) {
        dataList = [];
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/components',
            async: false,
            data: {
                user: username,
                action: 'get-components',
                data: JSON.stringify({
                    'release': releasedVersion
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                responseObject.forEach(componentObj => {

                    var component = {
                        "value": componentObj.Percentage,
                        "name": componentObj.ComponentName,
                        itemStyle: {
                            normal: {
                                color: getComponentColor(componentObj)
                            }
                        },
                        "children": []
                    };

                    $.ajax({
                        type: 'POST',
                        url: baseUrl + 'plugins/servlet/components',
                        async: false,
                        data: {
                            user: username,
                            action: 'get-all-test-cases-for-component',
                            data: JSON.stringify({
                                'componentName': componentObj.ComponentName,
                                'release': releasedVersion
                            })
                        },
                        success: function (response) {
                            var responseObject = JSON.parse(response);

                            responseObject.forEach(testCaseObj => {
                                var testCaseName;
                                var testCaseId;

                                $.each(testCaseObj, function (key, value) {
                                    testCaseName = value;
                                    testCaseId = key;
                                });

                                var testCase = {
                                    "value": 100 / responseObject.length,
                                    "name": componentObj.ComponentName + "." + testCaseName,
                                    "children": []
                                }

                                component.children.push(testCase);

                                $.ajax({
                                    type: 'POST',
                                    url: baseUrl + 'plugins/servlet/components',
                                    async: false,
                                    data: {
                                        user: username,
                                        action: 'get-all-defects-for-testcase',
                                        data: JSON.stringify({
                                            'testcaseId': testCaseId
                                        })
                                    },
                                    success: function (response) {
                                        var responseObject = JSON.parse(response);

                                        responseObject.forEach(defectObj => {
                                            var defect = {
                                                "value": 100 / responseObject.length,
                                                "name": componentObj.ComponentName + "." + testCaseName + "." + defectObj.name
                                            }

                                            testCase.children.push(defect);

                                        });
                                    }
                                });

                            });
                        }
                    });
                    dataList.push(component);
                });
            }
        });
        return dataList;
    }

    AJS.$("#schedule-execute-test-plan-btn").click(function (e) {
        getVersion('#schedule-entity-release');
        e.preventDefault();
        AJS.dialog2("#schedule-dialog").show();
    });

    $('#schedule-cancel-button').click(() => {
        AJS.dialog2("#schedule-dialog").hide();
    });

    $('#add-schedule-button').on('click', function () {
        var agentValidationCounter = 0;
        var userValidationCounter = 0;
        var browserValidationCounter = 0;
        var selectedRelease = $('#schedule-entity-release').val();
        var versionDesc = $('#schedule-entity-release-description').val();
        var scheduleName = $('#schedule-name').val();
        var cronValue = $('#cron-value').val();
        var isChecked = toggle.prop("checked");
        if (isChecked) {
            parallelExecutionStatus = true;
        }
        if (!validateInput(scheduleName)) {
            if (selectedRelease != '' && scheduleName != '' && cronValue != '') {
                var validateFields = true;
                var execEntityObject = {
                    'release': selectedRelease,
                    'description': versionDesc,
                    'plans': [],
                    'parallelExecution': parallelExecutionStatus,
                    'schedulerName': scheduleName,
                    'cronValue': cronValue
                };
                var selectedTestPlans = $('#selected-test-exec-table-tbody').children();
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
                        if (typeof testAgentStatus == 'undefined') {
                            testAgentStatus = false;
                        } else {
                            testAgentStatus = $('option:selected', testCaseAgentTD).attr('agentstatus');
                        }
                        var testCaseObject = {
                            'testCaseId': testCaseTR.attr('id').split('-')[0],
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
                                        action: 'save-schedule',
                                        data: JSON.stringify(execEntityObject)
                                    },
                                    success: function (response) {
                                        var inserted = JSON.parse(response);
                                        if (inserted.isInserted) {
                                            var execEntityObject = {
                                                'releaseVersion': selectedRelease,
                                                'description': versionDesc,
                                                'scheduleName': scheduleName,
                                                'cronSchedule': '0 0/3 * 1/1 * ? *',
                                                'executionEntityInstance': inserted.executionEntityInstance
                                            }

                                            $.ajax({
                                                type: 'POST',
                                                url: baseUrl + 'plugins/servlet/quartz-scheduler-management',
                                                async: false,
                                                data: {
                                                    user: username,
                                                    action: 'start-schedule',
                                                    data: JSON.stringify(execEntityObject)
                                                },
                                                success: function (response) {
                                                    var inserted = JSON.parse(response);
                                                    if (inserted) {
                                                        AJS.flag({
                                                            type: 'success',
                                                            title: 'Scheduler Added Success..',
                                                            close: 'auto'
                                                        });
                                                        window.location.replace(baseUrl + 'secure/TestReportsView.jspa');
                                                        AJS.dialog2("#schedule-dialog").hide();
                                                    }
                                                }, error: function (jqXHR, textStatus, errorThrown) {
                                                    AJS.flag({
                                                        type: 'error',
                                                        title: 'An Error Occurred Starting Schedule..',
                                                        body: 'Contact Developer',
                                                        close: 'auto'
                                                    });
                                                }
                                            });
                                        }
                                    }, error: function (jqXHR, textStatus, errorThrown) {
                                        AJS.flag({
                                            type: 'error',
                                            title: 'An Error Occurred Scheduling The Data..',
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
            }else {
                AJS.flag({
                    type: 'error',
                    title: 'Enter Relevant Details Before Creating Schedule..',
                    close: 'auto'
                });
            }
        }
    });

    $('#schedule-name').keyup(function (e) {
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

})(jQuery);