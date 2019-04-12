AJS.toInit(function ($) {

    'use strict';

    $('#footer').remove();

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');

    var executionCycleCount = {};
    var currentExecutionCycleCount = {};
    var currentTotalTC = 0;
    var currentExecutedPassTC = 0;
    var currentExecutedFailedTC = 0;
    var currentInProgressTC = 0;

    var overallTotalTC = 0;
    var overallExecutedPassTC = 0;
    var overallExecutedFailedTC = 0;
    var overallInProgressTC = 0;

    var selectedVersions = [];

    var entityTCAutomatedCount = 0;
    var entityTCManualCount = 0;
    var entityCompletedTCAutomatedCount = 0;
    var entityCompletedTCManualCount = 0;
    var entityInProgressTCManualCount = 0;
    var entityInProgressTCAutomatedCount = 0;
    var entityTPTCAutomatedCount = 0;
    var entityTPTCManualCount = 0;
    var entityTPCompletedTCAutomatedCount = 0;
    var entityTPCompletedTCManualCount = 0;
    var entityTPInProgressTCManualCount = 0;
    var entityTPInProgressTCAutomatedCount = 0;
    var entityTCDefectCount = 0;
    var entityTPDefectCount = 0;
    var entityTPTCDefectCount = 0;

    $('#test-results-summary-graph-canvas').donutpie({
        radius: 200,
        tooltip: true,
        tooltipClass: 'donut-pie-tooltip-bubble'
    });

    $('#test-results-summary-overall-graph-canvas').donutpie({
        radius: 200,
        tooltip: true,
        tooltipClass: 'donut-pie-tooltip-bubble'
    });

    function getReports() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-list',
                data: JSON.stringify({
                    'redirectView': '/secure/TestReportsView.jspa'
                })
            },
            success: function (response) {
                executionCycleCount = {};
                currentExecutionCycleCount = {};
                $('#test-results-table-tbody').children().remove();
                var responseObject = JSON.parse(response);
                generateResultTable($('#test-results-table-tbody'), groupByRelease(responseObject));
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

    function groupByRelease(response) {
        var releaseGroup = [];
        for (var i = 0; i < response.length; i++) {
            var execEntity = response[i];
            if (!releaseGroup[execEntity.release]) {
                releaseGroup[execEntity.release] = [];
            }
            releaseGroup[execEntity.release].push(execEntity);
        }
        return releaseGroup;
    }

    function updatedNoCard(execEntityId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-entity-overall-summary',
                data: JSON.stringify({
                    'execEntityInstanceId': execEntityId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                currentExecutedPassTC = responseObject[0];
                currentExecutedFailedTC = responseObject[1];
                currentInProgressTC = responseObject[2];
                currentTotalTC = currentExecutedPassTC + currentExecutedFailedTC + currentInProgressTC;

                var currentExecutedPassTCPer = Math.ceil((currentExecutedPassTC / currentTotalTC) * 100);
                var currentExecutedFailedTCPer = Math.ceil((currentExecutedFailedTC / currentTotalTC) * 100);

                var chartData = [
                    {
                        'name': 'Pass',
                        'hvalue': currentExecutedPassTCPer,
                        'color': '#39b39b'
                    },
                    {
                        'name': 'Failed',
                        'hvalue': currentExecutedFailedTCPer,
                        'color': '#e75236'
                    },
                    {
                        'name': 'In Progress',
                        'hvalue': 100 - (currentExecutedPassTCPer + currentExecutedFailedTCPer),
                        'color': '#ffca0f'
                    }
                ];

                $('span#current-total-tc-count').text(currentTotalTC);
                $('span#current-inprogress-tc-count').text(currentInProgressTC);
                $('span#current-executed-pass-tc-count').text(currentExecutedPassTC);
                $('span#current-executed-failed-tc-count').text(currentExecutedFailedTC);

                if ($('#test-results-summary-graph-canvas').length !== 0) {
                    $('#test-results-summary-graph-canvas').donutpie('update', chartData);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'Entity Overall-Summary Loading Error..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function updateOverallGraph() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-overall-summary',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                overallExecutedPassTC = responseObject[0];
                overallExecutedFailedTC = responseObject[1];
                overallInProgressTC = responseObject[2];
                overallTotalTC = overallExecutedPassTC + overallExecutedFailedTC + overallInProgressTC;

                var overallExecutedPassTCPer = Math.ceil((overallExecutedPassTC / overallTotalTC) * 100);
                var overallExecutedFailedTCPer = Math.ceil((overallExecutedFailedTC / overallTotalTC) * 100);

                var overallChartData = [
                    {
                        'name': 'Pass',
                        'hvalue': overallExecutedPassTCPer,
                        'color': '#39b39b'
                    },
                    {
                        'name': 'Failed',
                        'hvalue': overallExecutedFailedTCPer,
                        'color': '#e75236'
                    },
                    {
                        'name': 'In Progress',
                        'hvalue': 100 - (overallExecutedPassTCPer + overallExecutedFailedTCPer),
                        'color': '#ffca0f'
                    }
                ];

                if ($('#test-results-summary-overall-graph-canvas').length !== 0) {
                    $('#test-results-summary-overall-graph-canvas').donutpie('update', overallChartData);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'Overall-Summary Loading Error..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function loadSelectedExecutionEntity(execEntity, cycle) {
        if (cycle) {
            $('#selected-eentity-title').text('Execution Cycle ' + cycle + ' for ' + execEntity.release);
        } else {
            $('#selected-eentity-title').text('Execution Cycle ' + currentExecutionCycleCount[execEntity.release] + ' for ' + execEntity.release);
        }

        $('#selected-eentity-status').children().remove();

        var properties = {
            'class': 'aui-lozenge',
            'style': 'font-size: 13px; color: #FFF;'
        }

        if (execEntity.status === 'In Progress') {
            properties.style = properties.style + ' background-color: #ffca0f; '
            properties.style = properties.style + ' border: 1px solid #ffca0f; '
        } else if (execEntity.status === 'Not Started') {
            properties.style = properties.style + ' background-color: gray; '
            properties.style = properties.style + ' border: 1px solid gray; '
        } else if (execEntity.status === 'Completed') {
            properties.style = properties.style + ' background-color: #39b39b; '
            properties.style = properties.style + ' border: 1px solid #39b39b; '
        }

        var statusSpan = $('<span>', properties);
        statusSpan.text(execEntity.status);

        $('#selected-eentity-status').append(statusSpan);
    }

    function expandResultsTableRow(execEntityArray) {
        var releaseSuffix = '-result-release';
        var execEntitySuffix = '-result-exec-entity';
        var testPlanSuffix = '-result-exec-entity-plan-entity';
        var testCaseSuffix = '-result-exec-entity-testcase-entity';

        $('#' + execEntityArray[0].release.replace(/\./g, '_') + releaseSuffix).removeClass('contracted')
        $('#' + execEntityArray[0].release.replace(/\./g, '_') + releaseSuffix).addClass('expanded  l1-first');

        for (var i = 0; i < execEntityArray.length; i++) {
            $('#' + execEntityArray[i].id + execEntitySuffix).removeClass('hidden');
        }

        $('#' + execEntityArray[0].id + execEntitySuffix).removeClass('contracted');
        $('#' + execEntityArray[0].id + execEntitySuffix).addClass('expanded  l2-first');
    }

    function generateResultTable(parent, response) {
        var releases = Object.keys(response);
        for (var r = 0; r < releases.length; r++) {
            var release = releases[r];
            var responseEntityArray = response[release];
            addReleaseNode(parent, release, responseEntityArray);
            executionCycleCount[release] = responseEntityArray.length;
            currentExecutionCycleCount[release] = responseEntityArray.length;
            for (var i = 0; i < responseEntityArray.length; i++) {
                var responseEntity = responseEntityArray[i];
                addExecutionEntityNode(parent, responseEntity);
            }
        }
        $('#test-results-table').tabelize({
            onRowClick: self.rowClicker,
            fullRowClickable: true,
            onBeforeRowClick: null,
            onAfterRowClick: null,
            onReady: null
        });
        if (releases[0]) {
            loadSelectedExecutionPlanList("#" + response[releases[0]][0].id + "-result-exec-entity", response[releases[0]][0].id);
            expandResultsTableRow(response[releases[0]]);
            updatedNoCard(response[releases[0]][0].id);
            updateOverallGraph();
        }
    }

    function generateCycle(release) {
        if (executionCycleCount[release]) {
            executionCycleCount[release] += 1;
        } else {
            executionCycleCount[release] = 1;
        }
    }

    function addReleaseNode(parent, release, entityArray) {
        var trObject = $('<tr>', {
            'data-level': '1',
            'id': release.replace(/\./g, '_') + '-result-release',
            'style': 'background: #dfe1e687; user-select: none; cursor: pointer;'
        });

        var tdEmptyObject = $('<td>', {
            'style': 'width: 70px; min-width: 70px;'
        });

        var tdReleaseObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdReleaseSpanObject = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-current',
            'style': 'text-transform: none; vertical-align: middle; margin-left: 5px;'
        });
        tdReleaseSpanObject.text(release);

        tdReleaseObject.append(tdReleaseSpanObject);

        var tdNameObject = $('<td>', {
            'style': 'width: 23%; font-size: 11px;'
        });

        var tdExecutionDateObject = $('<td>', {
            'style': 'width: 10%; font-size: 11px; font-style: italic;'
        });

        var tdTotalExecutionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        var tdStatusObject = $('<td>', {
            'style': 'width: 8%; font-size: 11px;'
        });

        var tdStateObject = $('<td>', {
            'style': 'width: 7%;'
        });

        var tdCompletedExectionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        var tdPendingExecutionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        trObject.append(tdEmptyObject);
        trObject.append(tdReleaseObject);
        trObject.append(tdNameObject);
        trObject.append(tdExecutionDateObject);
        trObject.append(tdTotalExecutionObject);
        trObject.append(tdStatusObject);
        trObject.append(tdStateObject);
        trObject.append(tdCompletedExectionObject);
        trObject.append(tdPendingExecutionObject);

        parent.append(trObject);
    }

    function addExecutionEntityNode(parent, execEntity) {
        var cycle = executionCycleCount[execEntity.release];

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-entity-test-case-count',
                data: JSON.stringify({
                    'execEntityInstanceId': execEntity.id
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                entityTCAutomatedCount = responseObject[0];
                entityTCManualCount = responseObject[1];
                entityCompletedTCManualCount = responseObject[2];
                entityCompletedTCAutomatedCount = responseObject[3];
                entityInProgressTCManualCount = responseObject[4];
                entityInProgressTCAutomatedCount = responseObject[5];

                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/defect-entity-management',
                    async: false,
                    data: {
                        user: username,
                        action: 'get-entity-defect-count',
                        data: JSON.stringify({
                            'execEntityInstanceId': execEntity.id
                        })
                    },
                    success: function (response) {
                        entityTCDefectCount = JSON.parse(response);
                    }
                });
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Entity Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        var trObject = $('<tr>', {
            'data-level': '2',
            'id': execEntity.id + '-result-exec-entity',
            'style': 'background: #dfe1e687; user-select: none; cursor: pointer;'
        });
        trObject.click(function (event) {
            event.preventDefault();
            loadSelectedExecutionPlanList("#" + this.id, execEntity.id);
            loadSelectedExecutionEntity(execEntity, cycle);
            updatedNoCard(execEntity.id);
        });

        var tdEmptyObject = $('<td>', {
            'style': 'width: 70px; min-width: 70px;'
        });

        var tdCycleObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdCycleSpanObject = $('<span>', {
            'style': 'font-size: 11px; font-weight: 600;'
        });
        tdCycleSpanObject.text('Execution Cycle ' + cycle + ' for ');
        executionCycleCount[execEntity.release] -= 1;

        var tdCycleVersionSpanObject = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-current',
            'style': 'text-transform: none; vertical-align: middle; margin-left: 5px;'
        });
        tdCycleVersionSpanObject.text(execEntity.release);

        tdCycleObject.append(tdCycleSpanObject);
        tdCycleObject.append(tdCycleVersionSpanObject);

        var tdNameObject = $('<td>', {
            'style': 'width: 23%; font-size: 11px;'
        });
        tdNameObject.text(execEntity.release);

        var tdExecutionDateObject = $('<td>', {
            'style': 'width: 10%; font-size: 11px; font-style: italic;'
        });
        tdExecutionDateObject.text(execEntity.executionDate);

        var tdStatusObject = $('<td>', {
            'style': 'width: 8%; font-size: 11px;'
        });
        tdStatusObject.text(execEntity.status);

        var tdStateObject = $('<td>', {
            'style': 'width: 7%; text-align: center;'
        });

        var tdStateSpan = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-new',
            'style': 'margin-left: 5px; vertical-align: middle; background-color: transparent; color: #0647a5; border-color: #0647a5; font-size: 11px;'
        }).on('click', function () {

        });

        var tdStateNameSpan = $('<span>', {
            'style': 'margin-left: auto; font-size: 11px;'
        });
        tdStateNameSpan.text('Defects');
        tdStateSpan.text(entityTCDefectCount);
        tdStateObject.append(tdStateNameSpan);
        tdStateObject.append(tdStateSpan);

        trObject.append(tdEmptyObject);
        trObject.append(tdCycleObject);
        trObject.append(tdNameObject);
        trObject.append(tdExecutionDateObject);
        trObject.append(tableEntityExecutionCountTD(execEntity, 'total', entityTCAutomatedCount, entityTCManualCount));
        trObject.append(tdStatusObject);
        trObject.append(tdStateObject);
        trObject.append(tableEntityExecutionCountTD(execEntity, 'completed', entityCompletedTCAutomatedCount, entityCompletedTCManualCount));
        trObject.append(tableEntityExecutionCountTD(execEntity, 'pending', entityInProgressTCAutomatedCount, entityInProgressTCManualCount));

        parent.append(trObject);
    }

    function addExecutionEntityTPNode(entity, parent, plan) {

        $('#' + plan.id + "-result-exec-entity-plan").remove();

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-plan-test-case-count',
                data: JSON.stringify({
                    'execEntityPlanInstanceId': plan.id
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                entityTPTCAutomatedCount = responseObject[0];
                entityTPTCManualCount = responseObject[1];
                entityTPCompletedTCAutomatedCount = responseObject[2];
                entityTPCompletedTCManualCount = responseObject[3];
                entityTPInProgressTCAutomatedCount = responseObject[4];
                entityTPInProgressTCManualCount = responseObject[5];

                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/defect-entity-management',
                    async: false,
                    data: {
                        user: username,
                        action: 'get-plan-defect-count',
                        data: JSON.stringify({
                            'execEntityPlanInstanceId': plan.id
                        })
                    },
                    success: function (response) {
                        entityTPDefectCount = JSON.parse(response);
                    }
                });
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Plan Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        var trObject = $('<tr>', {
            'data-level': '3',
            'id': plan.id + '-result-exec-entity-plan',
            'style': 'background: #dfe1e638; user-select: none; cursor: pointer;',
            'entity': entity + '-result-exec-entity-plan-entity'
        });

        var tdCollapseIcon = $('<span>', {
            'id': 'entity-plan-collapse-button',
            'class': 'aui-icon aui-icon-small aui-iconfont-vid-play',
            'collapsed': 'true',
            'style': 'color: #7f7f7f;vertical-align: middle; margin-right: 10px;'
        }).on('click', function () {
            event.preventDefault();
            if ($(this).attr('collapsed') == 'true') {
                $(this).css('transform', 'rotate(90deg)');
                $(this).attr('collapsed', 'false');
                var testPlanInstanceId = parent.split('-')[0];
                loadSelectedUserStory('#' + plan.id + "-result-exec-entity-plan", plan.id);
                loadSelectedExecutionTestCaseList('#' + plan.id + "-result-exec-entity-plan", plan.id, "", false);
            } else {
                $(this).css('transform', '');
                $(this).attr('collapsed', 'true');
                var currentLevel = $(this).parent().parent().attr('data-level');
                var currentParent = $(this).parent().parent();
                while ($(currentParent).next().attr('data-level') > currentLevel) {
                    $(currentParent).next().remove();
                }
            }
        });

        var tdEmptyObject = $('<td>', {
            'style': 'width: 70px; min-width: 70px;'
        });

        var tdIdObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdNameObject = $('<td>', {
            'style': 'width: 23%; font-size: 11px; font-weight:600; margin-left:10px'
        });

        tdNameObject.text(plan.testPlanName);
        tdNameObject.prepend(tdCollapseIcon);

        var tdDateObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdStatusObject = $('<td>', {
            'style': 'width: 8%; font-size: 11px;'
        });
        tdStatusObject.text(plan.status);

        var tdStateObject = $('<td>', {
            'style': 'width: 7%; text-align: center;'
        });

        var tdStateSpan = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-new',
            'style': 'margin-left: 5px; vertical-align: middle; background-color: #0647a5; border-color: #0647a5; font-size: 11px;'
        }).on('click', function () {
            viewDefectsSummary(plan.id);
        });
        var tdStateNameSpan = $('<span>', {
            'style': 'margin-left: auto; font-size: 11px;'
        });
        tdStateNameSpan.text('Defects');
        tdStateSpan.text(entityTPDefectCount);
        tdStateObject.append(tdStateNameSpan);
        tdStateObject.append(tdStateSpan);

        trObject.append(tdEmptyObject);
        trObject.append(tdIdObject);
        trObject.append(tdNameObject);
        trObject.append(tdDateObject);
        trObject.append(tableTPExecutionCountTD(plan, 'total', entityTPTCAutomatedCount, entityTPTCManualCount));
        trObject.append(tdStatusObject);
        trObject.append(tdStateObject);
        trObject.append(tableTPExecutionCountTD(plan, 'completed', entityTPCompletedTCAutomatedCount, entityTPCompletedTCManualCount));
        trObject.append(tableTPExecutionCountTD(plan, 'pending', entityTPInProgressTCAutomatedCount, entityTPInProgressTCManualCount));

        trObject.insertAfter(parent);
    }

    function addExecutionEntityTPTCNode(entity, parent, testCase, dataLevel) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-test-case-defect-count',
                data: JSON.stringify({
                    'execEntityTPTCInstanceId': testCase.id
                })
            },
            success: function (response) {
                entityTPTCDefectCount = JSON.parse(response);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        var trObject = $('<tr>', {
            'data-level': dataLevel,
            'id': testCase.id + '-result-exec-entity-testcase',
            'style': 'background: #dfe1e638; user-select: none; cursor: pointer;',
            'entity': entity + '-result-exec-entity-testcase-entity'
        });

        var tdEmptyObject = $('<td>', {
            'style': 'width: 70px; min-width: 70px;'
        });

        var tdIdObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdNameObject;
        var tdNameATagObject;

        if (dataLevel == 4) {
            tdNameObject = $('<td>', {
                'style': 'width: 23%; font-size: 11px; margin-left:20px'
            });
            tdNameATagObject = $('<a>', {
                'style': 'margin-left: 36px;'
            });
        } else {
            tdNameObject = $('<td>', {
                'style': 'width: 23%; font-size: 11px; margin-left:40px'
            });
            tdNameATagObject = $('<a>', {
                'style': 'margin-left: 60px;'
            });
        }

        tdNameATagObject.text(testCase.testCaseName);
        tdNameATagObject.click(function (event) {
            event.preventDefault();
            showTCDetails(testCase);
        });

        var iterationAutomatedIcon = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-vid-raised-hand',
            'style': 'font-size: large; padding-left: 20px; margin-left: 5px;'
        });
        iterationAutomatedIcon.click(function (event) {
            viewDataItertations($(event.currentTarget).parent().parent().attr('id').split('-')[0]);
        });

        var iterationManualIcon = $('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-people-group',
            'style': 'font-size: large; padding-left: 20px; margin-left: 5px;'
        });
        iterationManualIcon.click(function (event) {
            viewIterationsForManual(testCase, testCase.id);
        });

        if (testCase.testCaseAutomated === 'Yes') {
            tdNameObject.append(tdNameATagObject).append(iterationAutomatedIcon);
        } else {
            tdNameObject.append(tdNameATagObject).append(iterationManualIcon);
        }

        var tdDateObject = $('<td>', {
            'style': 'width: 10%;'
        });

        var tdTotalExecutionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        var tdTotalExecutionObjectContainer = $('<div>', {
            'style': 'display: flex;'
        });

        var totalExecAutomatedDIV = $('<div>', {
            'style': 'width: 50%; text-align: right; display: flex;'
        });
        var spanAutomated = $('<span>', {
            'style': 'margin-left: auto;  font-size: 11px;'
        });
        spanAutomated.text('Auto ');
        var inputAutomatedValue = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': testCase.id + '-test-report-test-case-automated-check',
            'id': testCase.id + '-test-report-test-case-automated-check',
            'style': 'margin: auto;margin-right: 3px;margin-left: auto;',
            'disabled': ''
        });

        totalExecAutomatedDIV.append(spanAutomated);
        totalExecAutomatedDIV.append(inputAutomatedValue);

        var separatorDIV = $('<div>', {
            'style': 'width: 2px; margin: 0 7px; background: #bbbbbb;'
        })

        var totalExecManualDIV = $('<div>', {
            'style': 'width: 50%; text-align: left; display: flex;'
        });
        var spanManual = $('<span>', {
            'style': 'font-size: 11px;'
        });
        spanManual.text('Manual ');
        var inputManualValue = $('<input>', {
            'class': 'checkbox',
            'type': 'checkbox',
            'name': testCase.id + '-test-report-test-case-manual-check',
            'id': testCase.id + '-test-report-test-case-manual-check',
            'style': 'margin: auto;',
            'disabled': ''
        });

        totalExecManualDIV.append(spanManual);
        totalExecManualDIV.append(inputManualValue);

        if (testCase.testCaseAutomated === 'Yes') {
            inputAutomatedValue.prop('checked', true);
        } else {
            inputAutomatedValue.prop('checked', false);
        }

        if (testCase.testCaseManual === 'Yes') {
            inputManualValue.prop('checked', true);
        } else {
            inputManualValue.prop('checked', false);
        }

        tdTotalExecutionObjectContainer.append(totalExecAutomatedDIV);
        tdTotalExecutionObjectContainer.append(separatorDIV);
        tdTotalExecutionObjectContainer.append(totalExecManualDIV);

        tdTotalExecutionObject.append(tdTotalExecutionObjectContainer);

        var tdStatusObject = $('<td>', {
            'style': 'width: 8%; font-size: 11px;'
        });
        tdStatusObject.text(testCase.overallStatus);

        var tdStateObject = $('<td>', {
            'style': 'width: 7%; text-align: center;'
        });

        var tdStateSpan = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-new',
            'style': 'margin-left: 5px; vertical-align: middle; background-color: #0647a5; border-color: #0647a5; font-size: 11px;'
        }).on('click', function () {
            viewDefectsSummaryTPTC(testCase.id);
        });
        var tdStateNameSpan = $('<span>', {
            'style': 'margin-left: auto; font-size: 11px;'
        });
        tdStateNameSpan.text('Defects');
        tdStateSpan.text(entityTPTCDefectCount);
        tdStateObject.append(tdStateNameSpan);
        tdStateObject.append(tdStateSpan);

        var tdCompletedExecutionObject = $('<td>', {
            'style': 'width: 14%;'
        });

        var tdPendingExecutionObject = $('<td>', {
            'style': 'width: 14%;'
        });

        trObject.append(tdEmptyObject);
        trObject.append(tdIdObject);
        trObject.append(tdNameObject);
        trObject.append(tdDateObject);
        trObject.append(tdTotalExecutionObject);
        trObject.append(tdStatusObject);
        trObject.append(tdStateObject);
        trObject.append(tdCompletedExecutionObject);
        trObject.append(tdPendingExecutionObject);

        trObject.insertAfter(parent);
    }

    function addExecutionEntityUserStory(parent, userStoryObj) {

        if (typeof userStoryObj.userStoryId != 'undefined' && typeof userStoryObj.userStorySummary != 'undefined') {
            var trObject = $('<tr>', {
                'data-level': '4',
                'id': userStoryObj.userStoryId + '-result-exec-entity-user-story',
                'style': 'background: #dfe1e638; user-select: none; cursor: pointer;'
            });

            var divTd = $('<div>', {
                'style': 'display: flex;'
            });

            var tdCollapseIcon = $('<span>', {
                'id': 'entity-user-story-collapse-button',
                'class': 'aui-icon aui-icon-small aui-iconfont-vid-play',
                'collapsed': 'true',
                'style': 'color: #7f7f7f;10px;padding-top: 10px;padding-left: 10px;margin-left: 10px;'
            }).on('click', function () {
                event.preventDefault();
                if ($(this).attr('collapsed') == 'true') {
                    $(this).css('transform', 'rotate(90deg)');
                    $(this).attr('collapsed', 'false');
                    var userStoryId = $(this).parent().parent().parent().attr('id').split('-')[0];
                    var testPlanInstanceId = parent.split('-')[0];
                    loadSelectedExecutionTestCaseList($(this).parent().parent().parent(), testPlanInstanceId.split('#')[1], userStoryId, true);
                } else {
                    $(this).css('transform', '');
                    $(this).attr('collapsed', 'true');
                    var currentLevel = $(this).parent().parent().parent().attr('data-level');
                    var currentParent = $(this).parent().parent().parent();
                    while ($(currentParent).next().attr('data-level') > currentLevel) {
                        $(currentParent).next().remove();
                    }
                }
            });

            var tdCollapseIcon1 = $('<span>', {
                'id': 'entity-user-story-collapse1-button',
                'class': 'aui-icon aui-icon-small aui-iconfont-vid-play',
                'collapsed': 'true',
                'style': 'color: #7f7f7f;10px;padding-top: 10px;margin-left: 10px;'
            }).on('click', function () {
                event.preventDefault();
                if ($(this).attr('collapsed') == 'true') {
                    $(this).css('transform', 'rotate(90deg)');
                    $(this).attr('collapsed', 'false');
                    var userStoryId = $(this).parent().parent().parent().attr('id').split('-')[0];
                    var testPlanInstanceId = parent.split('-')[0];
                    loadSelectedExecutionTestCaseList($(this).parent().parent().parent(), testPlanInstanceId.split('#')[1], userStoryId, true);
                } else {
                    $(this).css('transform', '');
                    $(this).attr('collapsed', 'true');
                    var currentLevel = $(this).parent().parent().parent().attr('data-level');
                    var currentParent = $(this).parent().parent().parent();
                    while ($(currentParent).next().attr('data-level') > currentLevel) {
                        $(currentParent).next().remove();
                    }
                }

            });

            var tdEmptyObject = $('<td>', {
                'style': 'width: 70px; min-width: 70px;'
            });

            var tdIdObject = $('<td>', {
                'style': 'width: 10%;'
            });

            var tdNameObject = $('<td>', {
                'style': 'width: 23%; font-size: 11px; margin-left:10px'
            });

            var tdNameObjectSpan = $('<span>', {
                'style': 'font-size: 11px;padding-left: 10px;display: block;'
            });
            if (userStoryObj.userStorySummary.length > 30) {
                divTd.append(tdCollapseIcon);
                divTd.append(tdNameObjectSpan.text(userStoryObj.userStorySummary));
            } else {
                divTd.append(tdCollapseIcon1);
                divTd.append(tdNameObjectSpan.text(userStoryObj.userStorySummary));
            }

            tdNameObject.append(divTd);

            var tdDateObject = $('<td>', {
                'style': 'width: 10%;'
            });

            var tdTotalExecutionObject = $('<td>', {
                'style': 'text-align: center; margin: 0; width: 14%;'
            });

            var tdStatusObject = $('<td>', {
                'style': 'width: 8%; font-size: 11px;'
            });

            var tdStateObject = $('<td>', {
                'style': 'width: 7%;'
            });

            var tdCompletedExecutionObject = $('<td>', {
                'style': 'width: 14%;'
            });

            var tdPendingExecutionObject = $('<td>', {
                'style': 'width: 14%;'
            });

            trObject.append(tdEmptyObject);
            trObject.append(tdIdObject);
            trObject.append(tdNameObject);
            trObject.append(tdDateObject);
            trObject.append(tdTotalExecutionObject);
            trObject.append(tdStatusObject);
            trObject.append(tdStateObject);
            trObject.append(tdCompletedExecutionObject);
            trObject.append(tdPendingExecutionObject);

            trObject.insertAfter(parent);
        }
    }

    $('#test-reports-steps-excel-view').jexcel({
        colHeaders: [
            'Step Description',
            'Data',
            'Expected Result',
            'Actual Result',
            'Actual Status'
        ],
        colAlignments: [
            'left',
            'left',
            'left',
            'left',
            'left'
        ],
        columns: [
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true }
        ],
        minSpareRows: 1,
        allowInsertColumn: false,
        data: []
    });

    function showTCDetails(testcase) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case',
            async: false,
            data: {
                user: username,
                action: 'get',
                data: JSON.stringify({
                    'id': testcase.testCaseId
                })
            },
            success: function (response) {
                var iterationIdList = new Array();
                $('input#test-reports-test-case-name').val(testcase.testCaseName);
                if (typeof response.description != "") {
                    $('textarea#test-reports-test-case-description').val(response.description);
                } else {
                    $('textarea#test-reports-test-case-description').val("");
                }
                $('textarea#test-reports-test-case-overall-expected-result').val(testcase.overallExpectedResult);
                if (typeof response.description != "") {
                    $('textarea#test-reports-test-case-overall-actual-result').val(response.actualResult);
                } else {
                    $('textarea#test-reports-test-case-overall-actual-result').val("");
                }
                $('input#test-reports-test-case-overall-status').val(testcase.overallStatus);
                var agentLabel = $('#agentLabel');
                var inputArea = $('#test-case-agent-detail');

                if (testcase.testCaseManual == 'Yes') {
                    $('#test-reports-manualYes').prop('checked', true);
                    agentLabel.text('User');
                    inputArea.val(testcase.testCaseUser);

                    $.ajax({
                        type: 'POST',
                        url: baseUrl + 'plugins/servlet/my-task-entity-management',
                        async: false,
                        data: {
                            user: username,
                            action: 'get-test-iteration-data-list',
                            data: JSON.stringify({
                                'execEntityTPTCI': testcase.id
                            })
                        },
                        success: function (response) {
                            var responseObject = JSON.parse(response);
                            if (responseObject != undefined && responseObject.tciMyTaskIterationDataObj.length > 0) {
                                for (let index = 0; index < responseObject.tciMyTaskIterationDataObj.length; index++) {
                                    var iterationId = responseObject.tciMyTaskIterationDataObj[index].iterationId;
                                    iterationIdList.push(iterationId);
                                }
                            }
                        }, error: function (jqXHR, textStatus, errorThrown) {
                            AJS.flag({
                                type: 'error',
                                title: 'An Error Occurred Loading The Test Case Details Data..',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        }
                    });

                } else {
                    $('#test-reports-manualNo').prop('checked', true);
                }
                if (testcase.testCaseAutomated == 'Yes') {
                    $('#test-reports-automatedYes').prop('checked', true);
                    agentLabel.text('Agent');
                    inputArea.val(testcase.testCaseAgent);
                } else {
                    $('#test-reports-automatedNo').prop('checked', true);
                }

                if (iterationIdList.length > 0) {
                    iterationIdList.forEach(element => {
                        if (element != "empty") {
                            var stepsArray = [];
                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/test-step-instance',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'get',
                                    data: JSON.stringify({
                                        'execEntityTPTCI': testcase.id,
                                        'iteration_Id': element
                                    })
                                },
                                success: function (response) {
                                    var steps = JSON.parse(response);
                                    if (steps.steps.length !== 0) {
                                        for (var i = 0; i < steps.steps.length; i++) {
                                            var stepObject = [];
                                            stepObject[0] = steps.steps[i].step;
                                            stepObject[1] = "View In Iterations";
                                            stepObject[2] = steps.steps[i].expectedResult;
                                            stepObject[3] = steps.steps[i].actualResult;
                                            stepObject[4] = steps.steps[i].actualStatus;
                                            stepsArray.push(stepObject);
                                        }
                                    }
                                    $('#test-reports-steps-excel-view').jexcel('setData', stepsArray);
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Loading The Steps Data..',
                                        body: 'Contact Developer',
                                        close: 'auto'
                                    });
                                }
                            });
                        } else {
                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/test-step-instance',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'get',
                                    data: JSON.stringify({
                                        'execEntityTPTCI': testcase.id,
                                        'iteration_Id': ''
                                    })
                                },
                                success: function (response) {
                                    var steps = JSON.parse(response);
                                    var stepsArray = [];
                                    if (steps.steps.length !== 0) {
                                        for (var i = 0; i < steps.steps.length; i++) {
                                            var stepObject = [];
                                            stepObject[0] = steps.steps[i].step;
                                            stepObject[1] = steps.steps[i].data;
                                            stepObject[2] = steps.steps[i].expectedResult;
                                            stepObject[3] = steps.steps[i].actualResult;
                                            stepObject[4] = steps.steps[i].actualStatus;
                                            stepsArray.push(stepObject);
                                        }
                                        $('#test-reports-steps-excel-view').jexcel('setData', stepsArray);
                                    } else {
                                        var stepObject = [];
                                        stepObject[0] = 'No Steps Available';
                                        stepObject[1] = '';
                                        stepObject[2] = '';
                                        stepObject[3] = '';
                                        stepObject[4] = '';
                                        stepsArray.push(stepObject);
                                        $('#test-reports-steps-excel-view').jexcel('setData', stepsArray);
                                    }
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Loading The Steps Data..',
                                        body: 'Contact Developer',
                                        close: 'auto'
                                    });
                                }
                            });
                        }
                    });
                } else {
                    var stepsArray = [];
                    var stepObject = [];
                    stepObject[0] = 'No Steps Available';
                    stepObject[1] = '';
                    stepObject[2] = '';
                    stepObject[3] = '';
                    stepObject[4] = '';
                    stepsArray.push(stepObject);
                    $('#test-reports-steps-excel-view').jexcel('setData', stepsArray);
                }
                AJS.dialog2('#show-tc-dialog').show();
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case Details Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }


    function getCount(testCases, key) {
        var count = 0;
        if (typeof testCases != "undefined") {
            for (var ii = 0; ii < testCases.length; ii++) {
                if (testCases[ii][key] === 'Yes') {
                    count += 1;
                }
            }
        }
        return count;
    }

    function getPendingCount(testCases, key) {
        var count = 0;
        if (typeof testCases != "undefined") {
            for (var ii = 0; ii < testCases.length; ii++) {
                if (testCases[ii][key] === 'Yes') {
                    if (!testCases[ii].isExecuted) {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    function getCompletedCount(testCases, key) {
        var count = 0;
        if (typeof testCases != "undefined") {
            for (var ii = 0; ii < testCases.length; ii++) {
                if (testCases[ii][key] === 'Yes') {
                    if (testCases[ii].isExecuted) {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    function getEECount(execEntity, type, key) {
        var count = 0;
        if (typeof execEntity.plans != "undefined") {
            for (var i = 0; i < execEntity.plans.length; i++) {
                var plan = execEntity.plans[i];
                if (type === 'total') {
                    count += getCount(plan.testCases, key);
                } else if (type === 'completed') {
                    count += getCompletedCount(plan.testCases, key);
                } else if (type === 'pending') {
                    count += getPendingCount(plan.testCases, key);
                }
            }
        }
        return count;
    }

    function tableEntityExecutionCountTD(execEntity, type, automatedCount, manualCount) {
        var tdTotalExecutionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        var tdTotalExecutionObjectContainer = $('<div>', {
            'style': 'display: flex;'
        });

        var totalExecAutomatedDIV = $('<div>', {
            'style': 'width: 50%; text-align: right;'
        });
        var spanAutomated = $('<span>', {
            'style': 'margin-left: auto; vertical-align: middle; font-size: 11px;'
        });
        spanAutomated.text('Auto ');
        var spanAutomatedValue = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-new',
            'style': 'margin-left: 9.5px; vertical-align: middle; background-color: transparent; color: #0647a5; border-color: #0647a5; font-size: 11px;'
        });
        if (type === 'total') {
            spanAutomatedValue.text(automatedCount);
        } else if (type === 'completed') {
            spanAutomatedValue.text(automatedCount);
        } else if (type === 'pending') {
            spanAutomatedValue.text(automatedCount);
        }

        totalExecAutomatedDIV.append(spanAutomated);
        totalExecAutomatedDIV.append(spanAutomatedValue);

        var totalExecManualDIV = $('<div>', {
            'style': 'width: 50%; text-align: left; vertical-align: middle;'
        });
        var spanManual = $('<span>', {
            'style': 'font-size: 11px;'
        });
        spanManual.text('Manual ');
        var spanManualValue = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-moved',
            'style': 'margin-left: 5px; margin-right: auto; vertical-align: middle; background-color: transparent; font-size: 11px; color: #000;'
        });
        if (type === 'total') {
            spanManualValue.text(manualCount);
        } else if (type === 'completed') {
            spanManualValue.text(manualCount);
        } else if (type === 'pending') {
            spanManualValue.text(manualCount);
        }

        totalExecManualDIV.append(spanManual);
        totalExecManualDIV.append(spanManualValue);

        var separatorDIV = $('<div>', {
            'style': 'width: 2px; margin: 0 7px; background: #bbbbbb;'
        });


        tdTotalExecutionObjectContainer.append(totalExecAutomatedDIV);
        tdTotalExecutionObjectContainer.append(separatorDIV);
        tdTotalExecutionObjectContainer.append(totalExecManualDIV);

        tdTotalExecutionObject.append(tdTotalExecutionObjectContainer);

        return tdTotalExecutionObject;
    }

    function tableTPExecutionCountTD(plan, type, automatedCount, manualCount) {
        var tdTotalExecutionObject = $('<td>', {
            'style': 'text-align: center; margin: 0; width: 14%;'
        });

        var tdTotalExecutionObjectContainer = $('<div>', {
            'style': 'display: flex;'
        });

        var totalExecAutomatedDIV = $('<div>', {
            'style': 'width: 50%; text-align: right;'
        });
        var spanAutomated = $('<span>', {
            'style': 'margin-left: auto; font-size: 11px;'
        });
        spanAutomated.text('Auto ');
        var spanAutomatedValue = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-new',
            'style': 'margin-left: 9.5px; vertical-align: middle; background-color: #0647a5; border-color: #0647a5; font-size: 11px;'
        });
        if (type === 'total') {
            spanAutomatedValue.text(automatedCount);
        } else if (type === 'completed') {
            spanAutomatedValue.text(automatedCount);
        } else if (type === 'pending') {
            spanAutomatedValue.text(automatedCount);
        }

        totalExecAutomatedDIV.append(spanAutomated);
        totalExecAutomatedDIV.append(spanAutomatedValue);

        var totalExecManualDIV = $('<div>', {
            'style': 'width: 50%; text-align: left;'
        });
        var spanManual = $('<span>', {
            'style': 'font-size: 11px;'
        });
        spanManual.text('Manual ');
        var spanManualValue = $('<span>', {
            'class': 'aui-lozenge aui-lozenge-moved',
            'style': 'margin-left: 5px; margin-right: auto; vertical-align: middle; font-size: 11px;'
        });
        if (type === 'total') {
            spanManualValue.text(manualCount);
        } else if (type === 'completed') {
            spanManualValue.text(manualCount);
        } else if (type === 'pending') {
            spanManualValue.text(manualCount);
        }

        totalExecManualDIV.append(spanManual);
        totalExecManualDIV.append(spanManualValue);

        var separatorDIV = $('<div>', {
            'style': 'width: 2px; margin: 0 7px; background: #bbbbbb;'
        });


        tdTotalExecutionObjectContainer.append(totalExecAutomatedDIV);
        tdTotalExecutionObjectContainer.append(separatorDIV);
        tdTotalExecutionObjectContainer.append(totalExecManualDIV);

        tdTotalExecutionObject.append(tdTotalExecutionObjectContainer);

        return tdTotalExecutionObject;
    }

    function viewDataItertations(instanceId) {

        var passCount = 0;
        var failCount = 0;
        var inprogressCount = 0;

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-iteration-data',
                data: JSON.stringify({
                    'instance_id': instanceId
                })
            },
            success: function (response) {
                var result = JSON.parse(response);
                if (result != undefined) {
                    var tbody = $('#v_tcdi');
                    tbody.empty();
                    if (result.iterations != undefined && result.iterations.length > 0) {
                        for (let index = 0; index < result.iterations.length; index++) {

                            var tableRow = $('<tr>', {
                            });

                            var iteration = result.iterations[index];

                            var countCell = $('<td>', {

                            });

                            tableRow.append(countCell.text(index));

                            for (key in iteration) {

                                if (iteration[key] == 'In Progress') {
                                    inprogressCount += 1;
                                } else if (iteration[key] == 'Pass') {
                                    passCount += 1;
                                } else if (iteration[key] == 'Failed') {
                                    failCount += 1;
                                }

                                var tableCell = $('<td>', {

                                });

                                tableCell.text(iteration[key]);
                                tableRow.append(tableCell);
                            }
                            tbody.append(tableRow);

                        }
                    } else {
                        $('#di-data-unavailable').css('display', 'block').css('align-self', 'center').css('font-size', 'x-large');
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Iteration Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        var total = passCount + failCount + inprogressCount;
        $('#di-current-total-tc-count').text(total);
        $('#di-current-executed-pass-tc-count').text(passCount);
        $('#di-current-executed-failed-tc-count').text(failCount);
        $('#di-current-inprogress-tc-count').text(inprogressCount);

        AJS.dialog2('#show-tcdi-dialog').show();

    }

    function viewIterationsForManual(testCase, execEntityTPTCI) {
        var passCount = 0;
        var failCount = 0;
        var inprogressCount = 0;

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/my-task-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-test-iteration-data-list',
                data: JSON.stringify({
                    'execEntityTPTCI': execEntityTPTCI
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (responseObject != undefined && responseObject.tciMyTaskIterationDataObj.length > 0) {
                    var tbody = $('#manul_tc_body');
                    tbody.empty();
                    for (let index = 0; index < responseObject.tciMyTaskIterationDataObj.length; index++) {

                        if (!responseObject.tciMyTaskIterationDataObj[index].tciMyTaskIterationData.trim()) {

                            var tbody = $('#manul_tc_body');
                            tbody.empty();

                            var tableRow = $('<tr>', {
                            });

                            var countCell = $('<td>', {});

                            var iterationId = $('<a>', {}).on('click', function () {
                                openTCSummary(testCase, "");
                            });

                            iterationId.text("");
                            tableRow.append(countCell.append(""));

                            inprogressCount = 0;
                            passCount = 0;
                            failCount = 0;

                            var tableCell = $('<td>', {
                                style: 'font-size: 17px; font-weight: 600; text-align:center'
                            });

                            var tableCellStatus = $('<td>', {

                            });

                            tableCell.text("No Iteration Data");
                            tableRow.append(tableCell);
                            tableCellStatus.text("");
                            tableRow.append(tableCellStatus);
                            tbody.append(tableRow);
                            $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                        } else {

                            var tableRow = $('<tr>', {
                            });

                            var iteration = responseObject.tciMyTaskIterationDataObj[index].tciMyTaskIterationData;

                            var countCell = $('<td>', {});

                            var iterationId = $('<a>', {}).on('click', function () {
                                openTCSummary(testCase, responseObject.tciMyTaskIterationDataObj[index]);
                            });

                            iterationId.text(responseObject.tciMyTaskIterationDataObj[index].iterationId);
                            tableRow.append(countCell.append(iterationId));

                            if (responseObject.tciMyTaskIterationDataObj[index].tciMyTaskStatus == 'In Progress') {
                                inprogressCount += 1;
                            } else if (responseObject.tciMyTaskIterationDataObj[index].tciMyTaskStatus == 'Pass') {
                                passCount += 1;
                            } else if (responseObject.tciMyTaskIterationDataObj[index].tciMyTaskStatus == 'Failed') {
                                failCount += 1;
                            }

                            var tableCell = $('<td>', {

                            });

                            var tableCellStatus = $('<td>', {

                            });

                            tableCell.text(iteration);
                            tableRow.append(tableCell);
                            tableCellStatus.text(responseObject.tciMyTaskIterationDataObj[index].tciMyTaskStatus);
                            tableRow.append(tableCellStatus);
                            tbody.append(tableRow);
                            $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                        }
                    }
                } else {
                    $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                }

                var total = passCount + failCount + inprogressCount;
                $('#manual-iteration-current-total-tc-count').text(total);
                $('#manual-iteration-current-executed-pass-tc-count').text(passCount);
                $('#manual-iteration-current-executed-failed-tc-count').text(failCount);
                $('#manual-iteration-current-inprogress-tc-count').text(inprogressCount);
                AJS.dialog2('#show-manualtcdi-dialog').show();
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Manul Test Case Iteration Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#test-case-close-button').on('click', function () {
        AJS.dialog2('#view-test-case-dialog').hide();
    });

    function loadSelectedExecutionPlanList(parent, execEntityInstanceId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-plan-list',
                data: JSON.stringify({
                    'execEntityInstanceId': execEntityInstanceId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                var responseEntity = responseObject.plans;
                for (var j = 0; j < responseEntity.length; j++) {
                    var responsePlan = responseEntity[j];
                    addExecutionEntityTPNode(responseEntity[j].id, parent, responsePlan);
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Plan List Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function loadSelectedExecutionTestCaseList(parent, execEntityTPInstanceId, userStoryId, hasUserStory) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/reports-view-data-management',
            async: false,
            data: {
                user: username,
                action: 'get-test-case-list',
                data: JSON.stringify({
                    'execEntityTPInstanceId': execEntityTPInstanceId,
                    'hasUserStory': hasUserStory
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                for (var i = 0; i < responseObject.testCases.length; i++) {
                    var responseTestCase = responseObject.testCases[i];
                    var userStoryIdF = responseObject.testCases[i].userStoryId;
                    if (hasUserStory) {
                        if (userStoryIdF == userStoryId) {
                            addExecutionEntityTPTCNode(responseTestCase.id, parent, responseTestCase, 5);
                        }
                    } else {
                        addExecutionEntityTPTCNode(responseTestCase.id, parent, responseTestCase, 4);
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case List Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }


    function loadSelectedUserStory(parent, execEntityTPInstanceId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/user-story',
            async: true,
            data: {
                user: username,
                action: 'get-plan-user-story',
                data: JSON.stringify({
                    'execEntityTPInstanceId': execEntityTPInstanceId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (responseObject.stories.length > 0) {
                    for (var i = 0; i < responseObject.stories.length; i++) {
                        var responseUserStory = responseObject.stories[i];
                        addExecutionEntityUserStory(parent, responseUserStory);
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The User Story List Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#steps-excel-view-my-report').jexcel({
        colHeaders: [
            'Step Description',
            'Data',
            'Expected Result',
            'Actual Result',
            'Actual Status',
            ''
        ],
        colAlignments: [
            'left',
            'left',
            'left',
            'left',
            'left',
            'left'
        ],
        columns: [
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'text', readOnly: true },
            { type: 'dropdown', source: ['In Progress', 'Pass', 'Failed'], readOnly: true },
            { type: 'text', readOnly: true }
        ],
        minSpareRows: 1,
        allowInsertColumn: false,
        data: []
    });

    function openTCSummary(testCase, iterationObject) {
        var testCase_id = testCase.testCaseId;
        var globalTPTCId = testCase.id;
        $('span#selected-test-case-titile').text(testCase.testCaseName);
        $('input#test-case-name').val(testCase.testCaseName);
        var testcase_description = $('textarea#test-case-description').val(testCase.description);
        var testcase_overallExpectedResult = $('textarea#test-case-overall-expected-result').val(testCase.overallExpectedResult);
        var testcase_overallActualResult = $('textarea#test-case-overall-actual-result').val(testCase.actualResult);
        var testcase_overallStatus = $('#test-case-overall-status').val(testCase.overallStatus);
        if (testCase.manual == 'Yes') {
            $('#manualYes').prop('checked', true);
        } else {
            $('#manualNo').prop('checked', true);
        }
        if (testCase.automated == 'Yes') {
            $('#automatedYes').prop('checked', true);
        } else {
            $('#automatedNo').prop('checked', true);
        }
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-step-instance',
            async: false,
            data: {
                user: username,
                action: 'get',
                data: JSON.stringify({
                    'execEntityTPTCI': globalTPTCId,
                    'iteration_Id': iterationObject.iterationId
                })
            },
            success: function (stepsResponse) {
                $('#steps-tbody').empty();
                var steps = JSON.parse(stepsResponse);
                var stepsArray = [];
                if (steps.steps.length !== 0) {
                    for (var i = 0; i < steps.steps.length; i++) {

                        var stepObject = [];
                        stepObject[0] = steps.steps[i].step;
                        stepObject[1] = steps.steps[i].data;
                        stepObject[2] = steps.steps[i].expectedResult;
                        stepObject[3] = steps.steps[i].actualResult;
                        if (steps.steps[i].actualStatus == 'In Progress') {
                            stepObject[4] = '';
                        } else {
                            stepObject[4] = steps.steps[i].actualStatus;
                        }

                        stepObject[5] = steps.steps[i].stepId;
                        stepsArray.push(stepObject);

                        if (steps.steps[i].actualStatus === "In Progress") {
                            $('select#test-case-overall-status').val("N/A");
                        } else {
                            $('select#test-case-overall-status').val(steps.steps[i].actualStatus);
                        }
                    }
                }

                $('#steps-excel-view-my-report').jexcel('setData', stepsArray);
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case Summary Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        AJS.dialog2('#view-test-case-dialog').show();
    }

    function viewDefectsSummary(execEntityTPInstanceId) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-plan-defect-list',
                data: JSON.stringify({
                    'execEntityPlanInstanceId': execEntityTPInstanceId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                var tbody = $('#defect_tc_body').empty();
                if (!responseObject.length > 0) {
                    var tbody = $('#defect_tc_body').empty();

                    var tableRow = $('<tr>', {
                    });

                    var countCell = $('<td>', {});

                    var defectKey = $('<a>', {}).on('click', function () {
                    });

                    tableRow.append(countCell.append(""));

                    var tableCell = $('<td>', {
                        style: 'font-size: 17px; font-weight: 600; text-align:center'
                    });

                    var tableCellStatus = $('<td>', {
                        style: 'font-size: 15px;'
                    });

                    tableCell.text("No Defects Found");
                    tableRow.append(tableCell);
                    tableRow.append(tableCellStatus);
                    tbody.append(tableRow);
                    $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                }
                else {
                    for (let index = 0; index < responseObject.length; index++) {
                        var tableRow = $('<tr>', {
                        });

                        var defectName = responseObject[index].defectName.split(',')[0];
                        var defectData = responseObject[index].defectName.split(',')[1];

                        var countCell = $('<td>', {});

                        var defectKey = $('<a>', {}).on('click', function () {
                            window.open(baseUrl + "browse/" + responseObject[index].defectKey, '_blank');
                        });

                        defectKey.text(responseObject[index].defectKey);
                        tableRow.append(countCell.append(defectKey));

                        var tableCell = $('<td>', {
                        });

                        var defectNameSpan = $('<span>', {
                            'style': 'display: list-item'
                        });
                        defectNameSpan.text(defectName);

                        var defectDataSpan = $('<span>', {
                            'style': 'display: list-item'
                        });

                        if (defectData != '' && typeof defectData != 'undefined') {
                            defectDataSpan.text(defectData);
                        } else {
                            defectDataSpan.text('No Data');
                        }

                        var tableCellStatus = $('<td>', {
                            style: 'font-size: 15px;'
                        });

                        tableCell.append(defectNameSpan).append(defectDataSpan);
                        tableRow.append(tableCell);
                        tableRow.append(tableCellStatus.text(responseObject[index].defectStatus));
                        tbody.append(tableRow);
                    }
                    $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                }
                AJS.dialog2('#show-defects-dialog').show();
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Defect Summary Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function viewDefectsSummaryTPTC(execEntityTPTCInstanceId) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-test-case-defect-list',
                data: JSON.stringify({
                    'execEntityTPTCInstanceId': execEntityTPTCInstanceId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                var tbody = $('#test_case_defect_tc_body').empty();
                if (!responseObject.length > 0) {
                    var tbody = $('#test_case_defect_tc_body').empty();
                    var tableRow = $('<tr>', {
                    });

                    var countCell = $('<td>', {});

                    var defectKey = $('<a>', {}).on('click', function () {
                    });

                    tableRow.append(countCell.append(""));

                    var tableCell = $('<td>', {
                        style: 'font-size: 17px; font-weight: 600; text-align:center'
                    });

                    var tableCellStatus = $('<td>', {
                        style: 'font-size: 15px;'
                    });

                    tableCell.text("No Defects Found");
                    tableRow.append(tableCell);
                    tableRow.append(tableCellStatus);
                    tbody.append(tableRow);
                    $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                }
                else {
                    for (let index = 0; index < responseObject.length; index++) {
                        var tableRow = $('<tr>', {
                        });

                        var defectName = responseObject[index].defectName.split(',')[0];
                        var defectData = responseObject[index].defectName.split(',')[1];

                        var countCell = $('<td>', {});

                        var defectKey = $('<a>', {}).on('click', function () {
                            window.open(baseUrl + "browse/" + responseObject[index].defectKey, '_blank');
                        });

                        defectKey.text(responseObject[index].defectKey);
                        tableRow.append(countCell.append(defectKey));

                        var tableCell = $('<td>', {
                        });

                        var defectNameSpan = $('<span>', {
                            'style': 'display: list-item'
                        });
                        defectNameSpan.text(defectName);

                        var defectDataSpan = $('<span>', {
                            'style': 'display: list-item'
                        });
                        if (defectData != '' && typeof defectData != 'undefined') {
                            defectDataSpan.text(defectData);
                        } else {
                            defectDataSpan.text('No Data');
                        }

                        var tableCellStatus = $('<td>', {
                            style: 'font-size: 15px; text-align:-webkit-auto'
                        });

                        tableCell.append(defectNameSpan).append(defectDataSpan);
                        tableRow.append(tableCell);
                        tableRow.append(tableCellStatus.text(responseObject[index].defectStatus));
                        tbody.append(tableRow);
                    }
                    $('#di-data-unavailable').css('display', 'none').css('align-self', 'center').css('font-size', 'x-large');
                }
                AJS.dialog2('#show-test-case-defects-dialog').show();
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Test Case Defect Sumamry..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    if (window.location.href.indexOf('TestReportsView.jspa') >= 0) {
        getReports();
        //refresh function
        setTimeout(function () {
            if ($(location).attr("href").split('/').pop() === 'TestReportsView.jspa') {
                window.location.reload(1);
            }
        }, 180000);
    }

})(jQuery);