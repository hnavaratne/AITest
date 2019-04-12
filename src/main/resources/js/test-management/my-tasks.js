mytask.controller('myTaskCtrl', function ($scope, $http, $q, $sce, $timeout) {
    $scope.stepList;
    $scope.selectoverallStatus = "N/A";
    $scope.selectModel = "Select";
    $scope.pass = "Passed";
    $scope.failed = "Failed";
    $scope.notAny = "N/A";

    $scope.openDialog = (e) => {
        $('#iterations-result-data-table').empty();
        var testCase_id = $(e).attr('test-case-id');
        $scope.testCaseID = testCase_id;
        globalTPTC = testCase_id;
        selected_testCase_id = testCase_id;
        var testCase_name = $(e).attr('test-case-name');
        globalTPTCId = $(e).attr('test-case-TpId');
        var overallStatus = $(e).attr('test-case-status');
        var iterationId = $(e).attr('iteration-id');
        globalTPTCI_MyTask = $(e).attr('test-case-TcI-my-task');
        globalTPTCUser = $(e).attr('test-case-user');

        $('textarea#test-case-overall-actual-result').val('');
        //  $('select#test-case-overall-status').val('');

        loadTestCase().then((testCase) => {
            var response = testCase.data;
            $('#iterations-result-data-body').empty();
            $('span#selected-test-case-titile').text(testCase_name);
            $('input#test-case-name').val(testCase_name);
            var testcase_description = $('textarea#test-case-description').val(response.description);
            var testcase_overallExpectedResult = $('textarea#test-case-overall-expected-result').val(response.overallExpectedResult);
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

            if (overallStatus != "In Progress") {
                $('#test-case-overall-status').val('N/A');
            } else {
                $('#test-case-overall-status').val(overallStatus);
            }

            $('#iteration-id').text(iterationId);

            loadTestStep(globalTPTCId, iterationId, globalTPTCI_MyTask).then((steps) => {
                $scope.stepList = steps.data.steps;
            });

        });

        AJS.dialog2('#add-test-case-dialog-my-task').show();
    }

    $scope.actualStatusUpdate = (step) => {
        // console.log(step);
    }

    $scope.saveMyTask = () => {
        var isFromDialog = true;
        var selectedTPTCId = globalTPTCId;
        var selectedTCActualResult = $('textarea#test-case-overall-actual-result').val();
        var selectedTCOverallStatus = $('select#test-case-overall-status').val();
        var iterationId = $('#iteration-id').text();
        var attachedDefectsArray = new Array();
        var attachDefectStatus = null;
        if (selectedTCOverallStatus == "Passed") {
            selectedTCOverallStatus = "Pass";
        }
        getDefects().then((responseObject) => {

            if (responseObject.length > 0) {
                attachedDefectsArray = responseObject;
                attachDefectStatus = 1;
            } else {
                attachDefectStatus = 2;
            }
            var jsonData = JSON.stringify({
                "curActVal": selectedTCActualResult,
                "curOveralStat": selectedTCOverallStatus,
                "curTcId": selectedTPTCId,
                "execEntityTPTCI": selectedTPTCId,
                "iterationId": iterationId,
                "tPTCI_MyTask": globalTPTCI_MyTask,
                "testCaseUser": globalTPTCUser,
                "testCaseId": globalTPTC,
                'attachedDefectList': attachedDefectsArray,
                'attachDefectStatus': attachDefectStatus
            });
            var overallStatus = $('#test-case-overall-status');
            var isNotCompleted = false;
            var isOverallStatusValidated = false;
            for (var i = 0; i < $scope.stepList.length; i++) {
                if ($scope.stepList[i].actualResult != '' && $scope.stepList[i].actualStatus != "In Progress" && overallStatus != "Select" && overallStatus != "N/A" && selectedTCActualResult != '') {
                    isNotCompleted = false;
                } else {
                    isNotCompleted = true;
                }

            }
            if ($scope.selectoverallStatus == 'Passed') {
                for (var i = 0; i < $scope.stepList.length; i++) {
                    if ($scope.stepList[i].actualStatus == "Failed") {
                        isNotCompleted = true
                    }
                }
            } else if ($scope.selectoverallStatus == 'Failed') {
                for (var i = 0; i < $scope.stepList.length; i++) {
                    if ($scope.stepList[i].actualStatus == "Passed") {
                        isNotCompleted = true
                    }
                }
            }

            if (isNotCompleted) {
                AJS.flag({
                    type: 'error',
                    title: 'Invalid Information',
                    close: 'auto'
                });
            } else {
                for (var i = 0; i < $scope.stepList.length; i++) {
                    if ($scope.stepList[i].actualStatus == "Passed") {
                        $scope.stepList[i].actualStatus = "Pass"
                    }
                }
                saveExecutionA(jsonData, isFromDialog, $scope.stepList);
            }

        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Saving Defects..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });

    }

    $scope.setSelectedExpected = (text) => {
        setTimeout(changeCombo(text), 1000);
    }

    $scope.resizeMTD = (e) => {
        setInterval(() => { resizeEvent(e) }, 1000 / 15);
    }

    const changeCombo = (text) => {
        if (typeof text != 'undefined') {
            if (text !== '' && text !== 'Select' && text !== 'N/A') {
                for (var i = 0; i < $scope.stepList.length; i++) {
                    if ($scope.stepList[i].step != '') {
                        if ($scope.stepList[i].actualStatus == "In Progress" || $scope.stepList[i].actualStatus == "") {
                            $scope.stepList[i].actualStatus = text;
                        }
                    } else {
                        if ($scope.stepList[i].actualStatus == "In Progress" || $scope.stepList[i].actualStatus == "") {
                            $scope.stepList[i].actualStatus = text;
                        }
                    }
                }
            } else if (text !== '' && text !== 'N/A' && text == 'Select') {
                for (var i = 0; i < $scope.stepList.length; i++) {
                    if ($scope.stepList[i].step != '') {
                        if ($scope.stepList[i].actualStatus == "In Progress" || $scope.stepList[i].actualStatus == "") {
                            // $scope.stepList[i].actualStatus = text
                        }
                    } else {
                        if ($scope.stepList[i].actualStatus == "In Progress" || $scope.stepList[i].actualStatus == "") {
                            // $scope.stepList[i].actualStatus = text
                        }
                    }
                }
            }
        }
    }

    const resizeEvent = (e) => {
        var elementHeight = $(e.currentTarget).height();
        var textareaList = $(e.currentTarget).parent().parent().find('textarea');
        for (var i = 0; i < textareaList.length; i++) {
            $(textareaList[i]).css('height', elementHeight + 'px');
        }
    }

    const saveExecutionA = (data, isFromDialog, stepsResponse) => {
        result_ajax = undefined;
        var inputData = JSON.parse(data);
        if (isFromDialog) {
            if (inputData != undefined && inputData.curOveralStat != 'N/A' && inputData.curOveralStat != 'Select') {
                var stepsArray = [];
                for (var i = 0; i < stepsResponse.length; i++) {
                    var stepObject = {
                        'actualResult': stepsResponse[i].actualResult,
                        'actualStatus': stepsResponse[i].actualStatus,
                        'stepInstanceId': stepsResponse[i].stepId
                    }
                    stepsArray.push(stepObject);
                }
                execForTestSteps(stepsArray).then((resp) => {
                    sendDataToExec(data).then((resp2) => {
                        if (typeof resp2 != 'undefined' || resp2 != '') {
                            AJS.dialog2("#add-test-case-dialog-my-task").hide();
                            $('button[id="show-details-button-' + globalTPTCI_MyTask + '"]').parent().parent().remove();
                            AJS.flag({
                                type: 'success',
                                title: 'Successfully executed',
                                close: 'auto'
                            });
                          //  reloadTable();
                        } else {
                            AJS.flag({
                                type: 'error',
                                title: 'Execution error',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        }
                    });
                }).catch(err => {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred execFor TestSteps..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                });
            } else {
                if ((inputData.curOveralStat == 'N/A' || inputData.curOveralStat == 'Select') && inputData.curActVal == '') {
                    AJS.flag({
                        type: 'error',
                        title: 'Overall Actual Result and Overall Status must not be empty.',
                        close: 'auto'
                    });
                } else if (inputData.curOveralStat == 'N/A' || inputData.curOveralStat == 'Select') {
                    AJS.flag({
                        type: 'error',
                        title: 'Overall Status cannot be N/A.',
                        close: 'auto'
                    });
                } else if (inputData.curActVal == '') {
                    AJS.flag({
                        type: 'error',
                        title: 'Overall Actual Result cannot be empty.',
                        close: 'auto'
                    });
                }
                // AJS.flag({
                //     type: 'error',
                //     title: 'Please fill all the necessary information',
                //     close: 'auto'
                // });
            }
        }

    }

    const sendDataToExec = (dataV) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'update-exec', 'data': dataV };
        $http
            .post(baseUrl + 'plugins/servlet/exe-entity-instance', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const execForTestSteps = (stepsArray) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'update', 'data': JSON.stringify({ 'steps': stepsArray }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-step-instance', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getDefects = () => {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-saved-defects', 'data': JSON.stringify({ 'tCIMyTask_Id': globalTPTCI_MyTask }) };
        $http
            .post(baseUrl + 'plugins/servlet/defect-entity-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const loadTestCase = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, entity: 'TestCase', 'action': 'get', 'data': JSON.stringify({ 'id': $scope.testCaseID }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-case', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const loadTestStep = (globalTPTCId, iterationId, globalTPTCI_MyTask) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = {
            'user': username, 'action': 'get', 'data': JSON.stringify({
                'execEntityTPTCI': globalTPTCId,
                'iteration_Id': iterationId,
                'tPTCI_MyTask': globalTPTCI_MyTask
            })
        };
        $http
            .post(baseUrl + 'plugins/servlet/test-step-instance', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }
});

AJS.toInit(function ($) {

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');
    var selected_testCase_id, selected_testCase_name, selected_testCase_description, selected_testCase_overall_expected_result, selected_test_case_execution_data, selected_testCase_actual_result, result, result_ajax;
    var savedStepsArray = new Array();
    var attachedDefectsArray = new Array();
    var iterationId, tci_my_task, hasAttachments, attachDefectStatus;
    var selectedPageE = 1;
    var pageCount = 0;
    var globalArray = new Array();
    var isLoadExecData = false;

    function init() {
        //  $('#mytaskstable>tbody').html("");
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/my-task-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-my-tasks',
                data: JSON.stringify({
                    'selectedPage': selectedPageE,
                    'redirectView': '/secure/MyTasksView.jspa'
                })
            },
            success: function (response) {
                // $('#mytaskstable>tbody>tr').empty();
                $('#pagination-ol').css('display', 'none');
                $('#mytaskstable>tbody').find("tr:gt(0)").remove();
                var returnTreeItems = JSON.parse(response);
                globalArray = returnTreeItems;
                for (var i = 0; i < returnTreeItems.length; i++) {
                    pageCount = returnTreeItems[i].page_count;
                    if (pageCount == 1){
                        $('#pagination-ol').css('display', 'none');
                    }else {
                        $('#pagination-ol').css('display', 'block');
                    }
                    var tc_id = returnTreeItems[i].tc_id;
                    var taskSuite = returnTreeItems[i].instanceName;
                    var testCase = returnTreeItems[i].tc_name;
                    var expectedVal = returnTreeItems[i].expected_value;
                    var tpTcId = returnTreeItems[i].tp_tc_id;
                    var tpId = returnTreeItems[i].tp_id;
                    var overallStatus = returnTreeItems[i].overallStatus;
                    var iterationId = returnTreeItems[i].iterationId;
                    var iterationData = "";
                    var testCaseUser = returnTreeItems[i].tc_user;
                    if (returnTreeItems[i].iterationData) {
                        iterationData = returnTreeItems[i].iterationData;
                    } else {
                        iterationData = "";
                    }
                    var tCIMyTask = returnTreeItems[i].tci_my_task;

                    var tr = $('<tr>');

                    var td1 = $('<td>', {
                        'headers': 'basic-number',
                        'class': 'vertical-align-tr'
                    });
                    var td2 = $('<td>', {
                        'headers': 'basic-number',
                        'class': 'vertical-align-tr'
                    });

                    var testCase_id = $('<a>', {
                        'test-case-id': tc_id,
                        'test-case-name': testCase,
                        'test-case-TpId': tpTcId,
                        'test-case-plan-id': tpId,
                        'test-case-status': overallStatus,
                        'iteration-id': iterationId,
                        'test-case-TcI-my-task': tCIMyTask,
                        'test-case-user': testCaseUser
                    });

                    testCase_id.text(tc_id);

                    testCase_id.click(openDialog);

                    var td3 = $('<td>', {
                        'headers': 'basic-number',
                        'class': 'vertical-align-tr'
                    });

                    var td4 = $('<td>', {
                        'style': 'display:none',
                        'class': 'vertical-align-tr'
                    });

                    td1.append(taskSuite);
                    td2.append(testCase_id);
                    td3.append(testCase);
                    td4.append(tpTcId);

                    var td_showIterationDetails = $('<td>', {
                        'class': 'vertical-align-tr',
                    });

                    var showIterationDetails_btn = $('<button>', {
                        'class': 'aui-button',
                        'iteration-data': iterationData,
                        'title': 'View Iteration Data',
                        'style': 'outline: none;'
                    });

                    var showIterationDetails_span = $('<span>', {
                        'class': 'aui-icon aui-icon-small aui-iconfont-app-access'
                    });

                    showIterationDetails_btn.tooltip({
                        gravity: 'w'
                    });

                    showIterationDetails_btn.click(function () {
                        var iteration = $(this).attr('iteration-data');
                        openDataDialog(iteration);
                    });

                    showIterationDetails_btn.append(showIterationDetails_span);
                    td_showIterationDetails.append(showIterationDetails_btn);

                    var td_expVal = $('<td>', {
                        'headers': 'basic-lname',
                        'class': 'vertical-align-tr'
                    });
                    var expVal_ta = $('<textarea>', {
                        'disabled': 'true',
                        'class': 'jira-textarea',
                        'style': 'padding:10px'
                    });
                    expVal_ta.append(expectedVal);
                    td_expVal.append(expVal_ta);

                    var td_actVal = $('<td>', {
                        'headers': 'basic-lname',
                        'class': 'vertical-align-tr'
                    });
                    var actVal_ta = $('<textarea>', {
                        'class': 'jira-textarea',
                        'style': 'padding:10px'
                    });
                    td_actVal.append(actVal_ta);

                    var td_overStat = $('<td>', {
                        'headers': 'basic-username',
                        'class': 'vertical-align-tr'
                    });

                    var overallActualResultStatus_Select = $('<select>', {
                        'class': 'select form-control'
                    });
                    var overallActualResultStatus_Select_Option = $('<option>');
                    overallActualResultStatus_Select_Option.text('Select');

                    // Pass
                    var overallActualResultStatus_Select_Option_Pass = $('<option>');
                    overallActualResultStatus_Select_Option_Pass.text('Passed');

                    //Fail
                    var overallActualResultStatus_Select_Option_Fail = $('<option>');
                    overallActualResultStatus_Select_Option_Fail.text('Failed');

                    //N/A
                    var overallActualResultStatus_Select_Option_Not_Applicable = $('<option>');
                    overallActualResultStatus_Select_Option_Not_Applicable.text('N/A');

                    overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option);
                    overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Pass);
                    overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Fail);
                    overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Not_Applicable);

                    td_overStat.append(overallActualResultStatus_Select);

                    if (overallStatus == "In Progress" || overallStatus == "") {
                        overallActualResultStatus_Select.val('N/A');
                    } else {
                        overallActualResultStatus_Select.val(overallStatus);
                    }

                    var td_showDetails = $('<td>', {
                        'class': 'vertical-align-tr',
                    });

                    var showDetails_btn = $('<button>', {
                        'class': 'aui-button',
                        'id': 'show-details-button-' + tCIMyTask,
                        'test-case-id': tc_id,
                        'test-case-name': testCase,
                        'test-case-TpId': tpTcId,
                        'test-case-plan-id': tpId,
                        'test-case-status': overallStatus,
                        'iteration-id': iterationId,
                        'test-case-TcI-my-task': tCIMyTask,
                        'test-case-user': testCaseUser,
                        'title': 'View Test Case Details',
                        'style': 'outline: none;',
                        'onclick': "angular.element(this).scope().openDialog(this)"
                    });

                    showDetails_btn.tooltip({
                        gravity: 'w'
                    });

                    // showDetails_btn.click(openDialog);

                    var showDetails_img = $('<span>', {
                        'class': 'aui-icon aui-icon-small aui-iconfont-info-filled'
                    });

                    showDetails_btn.append(showDetails_img);
                    td_showDetails.append(showDetails_btn);

                    var td_defect = $('<td>', {
                        'class': 'vertical-align-tr',
                    });

                    var defect_btn = $('<button>', {
                        'class': 'aui-button',
                        'id': 'defect-id' + tCIMyTask,
                        'test-case-id': tc_id,
                        'test-case-name': testCase,
                        'test-case-TpId': tpTcId,
                        'test-case-plan-id': tpId,
                        'test-case-status': overallStatus,
                        'iteration-id': iterationId,
                        'test-case-TcI-my-task': tCIMyTask,
                        'test-case-user': testCaseUser,
                        'title': 'Link Defect Manually',
                        'style': 'outline: none;'
                    });

                    defect_btn.tooltip({
                        gravity: 'w'
                    });

                    defect_btn.click(linkDefectDialog);

                    var defect_img = $('<span>', {
                        'class': 'aui-icon aui-icon-small aui-iconfont-tray'
                    });

                    defect_btn.append(defect_img);
                    td_defect.append(defect_btn);

                    var td_exec = $('<td>', {
                        'class': 'vertical-align-tr'
                    });

                    var overStat_tf = $('<button>', {
                        'class': 'aui-button aui-button-default align-center-tr',
                        'test-case-id': tc_id,
                        'test-case-name': testCase,
                        'test-case-TpId': tpTcId,
                        'test-case-plan-id': tpId,
                        'test-case-status': overallStatus,
                        'iteration-id': iterationId,
                        'test-case-TcI-my-task': tCIMyTask,
                        'test-case-user': testCaseUser
                    });

                    overStat_tf.click(function () {
                        isLoadExecData = false;
                        var curActVal = $(this).closest('tr').find("textarea:eq(1)").val();
                        var curOveralStat = $(this).closest('tr').find("select:eq(0)").val();
                        if (curOveralStat == "Passed") {
                            curOveralStat = "Pass";
                        }
                        var curTcId = $(this).closest('tr').children('td:eq(3)').text();
                        var tpTcId = $(this).attr('test-case-TpId');
                        var tpId = $(this).attr('test-case-plan-id');
                        var tCIMyTask = $(this).attr('test-case-TcI-my-task');
                        var overallStatus = $(this).attr('test-case-status');
                        var iterationId = $(this).attr('iteration-id');
                        var testCaseUser = $(this).attr('test-case-user');
                        var testCaseName = $(this).attr('test-case-name');
                        var testcaseId = $(this).attr('test-case-id');
                        tc_id = testcaseId;

                        var attachedDefectsArray = new Array();
                        var attachDefectStatus = null;

                        $.ajax({
                            type: 'POST',
                            url: baseUrl + 'plugins/servlet/defect-entity-management',
                            async: false,
                            data: {
                                user: username,
                                action: 'get-saved-defects',
                                data: JSON.stringify({
                                    tCIMyTask_Id: tCIMyTask
                                })
                            },
                            success: function (response) {
                                var responseObject = JSON.parse(response);
                                if (responseObject.length > 0) {
                                    attachedDefectsArray = responseObject;
                                    attachDefectStatus = 1;
                                    //reloadTable();
                                } else {
                                    attachDefectStatus = 2;
                                    // reloadTable();
                                }
                            }, error: function (jqXHR, textStatus, errorThrown) {
                                AJS.flag({
                                    type: 'error',
                                    title: 'An Error Occurred Loading The Saved Defects Data..',
                                    body: 'Contact Developer',
                                    close: 'auto'
                                });
                            }
                        });

                        var data = JSON.stringify({
                            'curActVal': curActVal,
                            'curOveralStat': curOveralStat,
                            'curTcId': curTcId,
                            'iterationId': iterationId,
                            'tPTCI_MyTask': tCIMyTask,
                            'execEntityTPTCI': tpTcId,
                            'execEntityTPI': tpId,
                            'testCaseUser': testCaseUser,
                            'testCaseName': testCaseName,
                            'testCaseId': tc_id,
                            'attachedDefectList': attachedDefectsArray,
                            'attachDefectStatus': attachDefectStatus
                        });
                        if (curOveralStat != 'N/A' && curOveralStat != 'Select' && curActVal != '') {
                            if (saveExecution(data, false, "")) {
                                $(this).closest('tr').hide();
                                reloadTable();
                            }
                        } else {
                            if ((curOveralStat == 'N/A' || curOveralStat == 'Select') && curActVal == '') {
                                AJS.flag({
                                    type: 'error',
                                    title: 'Overall Actual Result and Overall Status must not be empty.',
                                    close: 'auto'
                                });
                            } else if (curOveralStat == 'N/A' || curOveralStat == 'Select') {
                                AJS.flag({
                                    type: 'error',
                                    title: 'Overall Status cannot be N/A.',
                                    close: 'auto'
                                });
                            } else if (curActVal == '') {
                                AJS.flag({
                                    type: 'error',
                                    title: 'Overall Actual Result cannot be empty.',
                                    close: 'auto'
                                });
                            }
                        }
                    });
                    overStat_tf.append("Complete Execution");
                    td_exec.append(overStat_tf);
                    tr.append(td1).append(td2).append(td3).append(td4).append(td_showIterationDetails).append(td_expVal).append(td_actVal).append(td_overStat).append(td_showDetails).append(td_defect).append(td_exec);
                    //edited
                    $('#mytaskstable>tbody tr:last').after(tr);
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

    function openDialog() {

    }
    function openDataDialog(iteration) {
        var tableBody = $('#iteration-results-table-tbody').empty();
        if (iteration != "No Iteration Data") {
            var iterationData = JSON.parse(iteration);
            $.each(iterationData, function (key, value) {

                var trObject = $('<tr>', {
                    'id': 'iteration-header',
                    'style': 'user-select: none;'
                });

                var iterationDataHeader = $('<td>', {
                    'id': 'iteration-header-column',
                    'style': 'border: 1px solid #dfe1e6;'
                });

                var tdIterationHeaderSpan = $('<span>', {
                    'style': 'font-size: 13px; font-weight: 550; color: BLACK;'
                });
                tdIterationHeaderSpan.text(key);
                iterationDataHeader.append(tdIterationHeaderSpan);

                var iterationDataRow = $('<td>', {
                    'id': 'iteration-data-column',
                    'style': 'border: 1px solid #dfe1e6;'
                });

                var tdIterationRowSpan = $('<span>', {
                    'style': 'font-size: 13px;'
                });
                tdIterationRowSpan.text(value);
                iterationDataRow.append(tdIterationRowSpan);

                trObject.append(iterationDataHeader);
                trObject.append(iterationDataRow);
                tableBody.append(trObject);
            });
        } else {
            var paramHeader = $('#iteration-table-param-header').remove();
            var dataHeader = $('#iteration-table-data-header').remove();
            var tableBody = $('#iteration-results-table-tbody').empty();
            var trObject = $('<tr>', {
                'id': 'iteration-header',
                'style': 'user-select: none; text-align: center; font-size: 17px; font-weight: 500; color: BLACK;'
            });

            trObject.text(iteration);
            tableBody.append(trObject);
        }

        AJS.dialog2('#iteration-data-dialog').show();
    }

    var tbodyParent = $('#defect-list-tbody');
    $('#defect-list-selector').select2({
        placeholder: "Select deffect",
        allowClear: true
    });

    function linkDefectDialog() {
        getSavedDefects($(this).attr('test-case-TcI-my-task'), $(this).attr('test-case-TcI-my-task'));
        AJS.dialog2('#add-defects-dialog-my-task').show();
        iterationId = $(this).attr('iteration-id');
        tci_my_task = $(this).attr('test-case-TcI-my-task');
    }

    function getSavedDefects(tci_my_task, iterationId) {

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-saved-defects',
                data: JSON.stringify({
                    tCIMyTask_Id: tci_my_task
                })
            },
            success: function (response) {
                initializeDefectSelector(tci_my_task);
                var responseObject = JSON.parse(response);
                if (responseObject.length > 0) {
                    tbodyParent.empty();
                    $('#defect-list-table-div').css('display', 'block');

                    $.each(responseObject, function (key, value) {
                        var defectObject = value;
                        var trObject = $('<tr>', {
                            'id': 'defect-' + defectObject.defectId,
                            'style': 'user-select: none; width: 100%'
                        });

                        var defectId = $('<td>', {
                            'id': 'defect-id-cloumn-' + defectObject.defectId + '-' + defectObject.defectKey,
                            'style': 'border: 1px solid #dfe1e6; padding: 5px; text-align:center; vertical-align: middle;'
                        });

                        var defectKey = $('<a>', {}).on('click', function () {
                            window.open(baseUrl + "browse/" + defectObject.defectKey, '_blank');
                        });

                        defectKey.text(defectObject.defectKey);
                        defectId.append(defectKey);

                        var defectSumamry = $('<td>', {
                            'id': 'defect-summary-cloumn',
                            'style': 'border: 1px solid #dfe1e6; padding: 5px;'
                        });

                        var defectNameSpan = $('<span>', {
                            'style': 'margin-left: 20px; display: list-item;'
                        });
                        defectNameSpan.text(defectObject.defectName.split(',')[0]);

                        var defectDataSpan = $('<span>', {
                            'style': 'margin-left: 20px; display: list-item;'
                        });

                        if (defectObject.defectName.split(',')[1] != '' && typeof defectObject.defectName.split(',')[1] != 'undefined') {
                            defectDataSpan.text(defectObject.defectName.split(',')[1]);
                        } else {
                            defectDataSpan.text('No Data');
                        }
                        defectSumamry.append(defectNameSpan).append(defectDataSpan);

                        var removeDefectFromTable = $('<td>', {
                            'id': 'defect-remove-id',
                            'style': 'text-align: center; vertical-align: middle; border: 1px solid #dfe1e6; padding: 5px'
                        });

                        var removeButton = $('<button>', {
                            'id': defectObject.defectId
                        }).on('click', function () {
                            trObject.remove();
                            AJS.flag({
                                type: 'success',
                                title: 'Deffect Deleted Successfully',
                                close: 'auto'
                            });
                            appendValuesToDefectList(trObject.attr('id'));
                            $("#defect-list-selector").select2("val", "");
                        });

                        var removeButtonSpan = $('<span>', {
                            'class': "aui-icon aui-icon-small aui-iconfont-trash"
                        });
                        removeButton.append(removeButtonSpan);
                        removeDefectFromTable.append(removeButton);

                        trObject.append(defectId);
                        trObject.append(defectSumamry);
                        trObject.append(removeDefectFromTable);
                        tbodyParent.append(trObject);
                    });
                    hasAttachments = true;
                } else {
                    tbodyParent.empty();
                    $('#defect-list-table-div').css('display', 'none');
                    hasAttachments = false;
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Saved Defects Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
        $("#defect-list-selector").select2("val", "");
    }

    function initializeDefectSelector(tCIMyTaskId) {
        $('#defect-list-selector').empty();
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-all-defects',
                data: JSON.stringify({
                    'tCIMyTask_Id': tCIMyTaskId
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                $('#defect-list-selector').append($('<option>', {}));
                if (typeof responseObject != 'undefined') {
                    for (var i = 0; i < responseObject.length; i++) {
                        $.each(responseObject[i], function (defectId, defectSummary) {
                            $('#defect-list-selector').append($('<option>', {
                                value: defectId,
                                text: defectSummary
                            }));
                        });
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Defects List..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $('#cancel-defect-btn').on('click', function () {
        AJS.dialog2('#add-defects-dialog-my-task').hide();
    });

    $("input:checkbox").on('click', function () {
        if ($(this).is(":checked")) {
            attachDefectStatus = $(this).val();
            var group = "input:checkbox[name='" + $(this).attr("name") + "']";
            $(group).prop("checked", false);
            $(this).prop("checked", true);
        } else {
            $(this).prop("checked", false);
        }

        if (attachDefectStatus == 1) {
            $('#defect-list-selector-div').css('display', 'block');
            initializeDefectSelector(tci_my_task);
            if (hasAttachments) {
                getSavedDefects(tci_my_task, iterationId);
            }
            $("#defect-list-selector").select2("val", "");
        } else {
            $('#defect-list-selector-div').css('display', 'none');
            $('#defect-list-table-div').css('display', 'none');
            tbodyParent.empty();
        }
    });

    $("#defect-list-selector").change(function () {
        $('#defect-list-table-div').css('display', 'block');
        var selectedDefectId = $(this).children("option:selected").val();
        var selectedDefectSummary = $(this).children("option:selected").text();

        var trObject = $('<tr>', {
            'id': 'defect-' + selectedDefectId,
            'style': 'user-select: none; width: 100%'
        });

        var defectId = $('<td>', {
            'id': 'defect-id-cloumn-' + selectedDefectId,
            'style': 'border: 1px solid #dfe1e6; padding: 5px; text-align:center; vertical-align: middle;'
        });

        var defectKey = $('<a>', {}).on('click', function () {
            window.open(baseUrl + "browse/" + selectedDefectId.split('-')[1] + '-' + selectedDefectId.split('-')[2], '_blank');
        });

        defectKey.text(selectedDefectId.split('-')[1] + '-' + selectedDefectId.split('-')[2]);
        defectId.append(defectKey);

        var defectSumamry = $('<td>', {
            'id': 'defect-summary-cloumn',
            'style': 'border: 1px solid #dfe1e6; padding: 5px;'
        });

        var defectNameSpan = $('<span>', {
            'style': 'margin-left: 20px; display: list-item;'
        });
        defectNameSpan.text(selectedDefectSummary.split(',')[0]);

        var defectDataSpan = $('<span>', {
            'style': 'margin-left: 20px; display: list-item;'
        });
        if (selectedDefectSummary.split(',')[1] != '' && typeof selectedDefectSummary.split(',')[1] != 'undefined') {
            defectDataSpan.text(selectedDefectSummary.split(',')[1]);
        } else {
            defectDataSpan.text('No Data');
        }

        defectSumamry.append(defectNameSpan).append(defectDataSpan);

        var removeDefectFromTable = $('<td>', {
            'id': 'defect-remove-id',
            'style': 'text-align: center; vertical-align: middle; border: 1px solid #dfe1e6; padding: 5px'
        });

        var removeButton = $('<button>', {
            'id': selectedDefectId,
            'style': 'background: none; border: none;'
        }).on('click', function () {
            trObject.remove();
            AJS.flag({
                type: 'success',
                title: 'Deffect Deleted Successfully',
                close: 'auto'
            });
            appendValuesToDefectList(trObject.attr('id'));
            $("#defect-list-selector").select2("val", "");
        });

        var removeButtonSpan = $('<span>', {
            'class': "aui-icon aui-icon-small aui-iconfont-trash"
        });
        removeButton.append(removeButtonSpan);
        removeDefectFromTable.append(removeButton);

        trObject.append(defectId);
        trObject.append(defectSumamry);
        trObject.append(removeDefectFromTable);
        tbodyParent.append(trObject);
        $(this).children("option:selected").remove();
    });

    $('#save-defect-btn').on('click', function () {
        var defectObjectArray = new Array();
        attachedDefectsArray = [];

        $('#defect-list-tbody').find('tr').each(function () {
            var defectTR = $(this);
            var defectIdTD = $(defectTR.children()[0]);
            var defectSummaryTD = $(defectTR.children()[1]);
            var defectObject = {
                'defectId': defectIdTD.attr('id').split('-')[3],
                'defectKey': defectIdTD.attr('id').split('-')[4] + '-' + defectIdTD.attr('id').split('-')[5],
                'defectName': defectSummaryTD.text()
            }
            defectObjectArray.push(defectObject);
        });

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'save-defects',
                data: JSON.stringify({
                    attached_Defect_List: defectObjectArray,
                    iteration_Id: iterationId,
                    tCIMyTask_Id: tci_my_task
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (typeof responseObject != 'undefined' && responseObject != '') {
                    if (responseObject) {
                        AJS.flag({
                            type: 'success',
                            title: 'Save Successfully',
                            close: 'auto'
                        });
                        AJS.dialog2('#add-defects-dialog-my-task').hide();
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Saving The Defects..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    function appendValuesToDefectList(removedDefectId) {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/defect-entity-management',
            async: false,
            data: {
                user: username,
                action: 'append-selected-defect',
                data: JSON.stringify({
                })
            },
            success: function (response) {
                var responseObject = JSON.parse(response);
                if (typeof responseObject) {
                    for (var i = 0; i < responseObject.length; i++) {
                        $.each(responseObject[i], function (defectId, defectSummary) {
                            if (removedDefectId.split('-')[1] == defectId.split('-')[0]) {
                                $('#defect-list-selector').append($('<option>', {
                                    value: defectId,
                                    text: defectSummary
                                }));
                            }
                        });
                    }
                }
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Removing The Defects..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    var globalTPTCId = undefined;
    var globalTPTC = undefined;
    var globalTPTCI_MyTask = undefined;
    var globalTPTCUser = undefined;

    function saveExecution(data, isFromDialog, stepsResponse) {
        result_ajax = undefined;
        var inputData = JSON.parse(data);
        if (isFromDialog) {
            if (inputData != undefined && inputData.curOveralStat != 'N/A' && inputData.curOveralStat != 'Select') {
                var stepsArray = [];
                for (var i = 0; i < stepsResponse.length - 1; i++) {
                    var stepObject = {
                        'actualResult': stepsResponse[i][3],
                        'actualStatus': stepsResponse[i][4],
                        'stepInstanceId': stepsResponse[i][5]
                    }
                    stepsArray.push(stepObject);
                }

                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-step-instance',
                    async: false,
                    data: {
                        user: username,
                        action: 'update',
                        data: JSON.stringify({
                            'steps': stepsArray
                        })
                    },
                    success: function (response) {
                        $.ajax({
                            type: 'POST',
                            url: baseUrl + 'plugins/servlet/exe-entity-instance',
                            async: false,
                            data: {
                                user: username,
                                action: 'update-exec',
                                data: data
                            },
                            success: function (response) {
                                result_ajax = true;

                                AJS.flag({
                                    type: 'success',
                                    title: 'Successfully executed',
                                    close: 'auto'
                                });


                            }
                        });
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Updating Steps..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }
        } else {
            if (typeof inputData.execEntityTPTCI != "undefined" && inputData != undefined && inputData.curOveralStat != 'N/A' && inputData.curOveralStat != 'Select') {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-step-instance',
                    async: false,
                    data: {
                        user: username,
                        action: 'update-iteration',
                        data: JSON.stringify({
                            data: data
                        })
                    },
                    success: function (response) {
                        var response = JSON.parse(response);
                        if (response) {
                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/exe-entity-instance',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'update-exec',
                                    data: data
                                },
                                success: function (response) {
                                    result_ajax = true;
                                    isLoadExecData = true;
                                    AJS.flag({
                                        type: 'success',
                                        title: 'Successfully executed',
                                        close: 'auto'
                                    });

                                }
                            });
                        } else {
                            AJS.flag({
                                type: 'error',
                                title: 'Please fill all the necessary informations',
                                close: 'auto'
                            });
                            result_ajax = false;
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'Please fill all the necessary informations',
                            close: 'auto'
                        });
                        result_ajax = false;
                    }
                });
            }
        }
        return result_ajax;
    }

    $('#li-prev').click((e) => {
        e.preventDefault();
        let temp = selectedPageE;
        temp--;
        if (temp >= 1) {
            selectedPageE--
            init();
        }
    });

    $('#li-next').click((e) => {
        e.preventDefault();
        let temp = selectedPageE;
        temp++
        if (pageCount >= temp) {
            selectedPageE++;
            init();
        }

    });
    function reloadTable() {
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/my-task-entity-management',
            async: false,
            data: {
                user: username,
                action: 'get-my-tasks',
                data: JSON.stringify({
                    'selectedPage': selectedPageE,
                    'redirectView': '/secure/MyTasksView.jspa'
                })
            },
            success: function (response) {
                var returnTreeItems = JSON.parse(response);
                if (returnTreeItems.length > 0) {
                    var tempArray = new Array();
                    function comparer(otherArray) {
                        return function (current) {
                            return otherArray.filter(function (other) {
                                return other.tci_my_task == current.tci_my_task
                            }).length == 0;
                        }
                    }
                    // $('#mytaskstable>tbody').find("tr:gt(0)").remove();
                    var onlyInA = globalArray.filter(comparer(returnTreeItems));
                    var onlyInB = returnTreeItems.filter(comparer(globalArray));

                    var result = onlyInA.concat(onlyInB);
                    globalArray = returnTreeItems;
                    returnTreeItems = result;

                    //  console.log(tempArray);
                    for (var i = 1; i < returnTreeItems.length; i++) {
                        pageCount = returnTreeItems[i].page_count;
                        if (pageCount == 1){
                            $('#pagination-ol').css('display', 'none');
                        }else {
                            $('#pagination-ol').css('display', 'block'); 
                        }
                        var tc_id = returnTreeItems[i].tc_id;
                        var taskSuite = returnTreeItems[i].instanceName;
                        var testCase = returnTreeItems[i].tc_name;
                        var expectedVal = returnTreeItems[i].expected_value;
                        var tpTcId = returnTreeItems[i].tp_tc_id;
                        var tpId = returnTreeItems[i].tp_id;
                        var overallStatus = returnTreeItems[i].overallStatus;
                        var iterationId = returnTreeItems[i].iterationId;
                        var iterationData = "";
                        var testCaseUser = returnTreeItems[i].tc_user;
                        if (returnTreeItems[i].iterationData) {
                            iterationData = returnTreeItems[i].iterationData;
                        } else {
                            iterationData = "";
                        }
                        var tCIMyTask = returnTreeItems[i].tci_my_task;

                        var tr = $('<tr>');

                        var td1 = $('<td>', {
                            'headers': 'basic-number',
                            'class': 'vertical-align-tr'
                        });
                        var td2 = $('<td>', {
                            'headers': 'basic-number',
                            'class': 'vertical-align-tr'
                        });

                        var testCase_id = $('<a>', {
                            'test-case-id': tc_id,
                            'test-case-name': testCase,
                            'test-case-TpId': tpTcId,
                            'test-plan-id': tpId,
                            'test-case-status': overallStatus,
                            'iteration-id': iterationId,
                            'test-case-TcI-my-task': tCIMyTask,
                            'test-case-user': testCaseUser

                        });

                        testCase_id.text(tc_id);

                        testCase_id.click(openDialog);

                        var td3 = $('<td>', {
                            'headers': 'basic-number',
                            'class': 'vertical-align-tr'
                        });

                        var td4 = $('<td>', {
                            'style': 'display:none',
                            'class': 'vertical-align-tr'
                        });

                        td1.append(taskSuite);
                        td2.append(testCase_id);
                        td3.append(testCase);
                        td4.append(tpTcId);

                        var td_showIterationDetails = $('<td>', {
                            'class': 'vertical-align-tr',
                        });

                        var showIterationDetails_btn = $('<button>', {
                            'class': 'aui-button',
                            'iteration-data': iterationData,
                            'title': 'View Iteration Data',
                            'style': 'outline: none;'
                        });

                        var showIterationDetails_span = $('<span>', {
                            'class': 'aui-icon aui-icon-small aui-iconfont-app-access'
                        });

                        showIterationDetails_btn.tooltip({
                            gravity: 'w'
                        });

                        showIterationDetails_btn.click(function () {
                            var iteration = $(this).attr('iteration-data');
                            openDataDialog(iteration);
                        });

                        showIterationDetails_btn.append(showIterationDetails_span);
                        td_showIterationDetails.append(showIterationDetails_btn);

                        var td_expVal = $('<td>', {
                            'headers': 'basic-lname',
                            'class': 'vertical-align-tr'
                        });
                        var expVal_ta = $('<textarea>', {
                            'disabled': 'true',
                            'class': 'jira-textarea',
                            'style': 'padding:10px'
                        });
                        expVal_ta.append(expectedVal);
                        td_expVal.append(expVal_ta);

                        var td_actVal = $('<td>', {
                            'headers': 'basic-lname',
                            'class': 'vertical-align-tr'
                        });
                        var actVal_ta = $('<textarea>', {
                            'class': 'jira-textarea',
                            'style': 'padding:10px'
                        });
                        td_actVal.append(actVal_ta);

                        var td_overStat = $('<td>', {
                            'headers': 'basic-username',
                            'class': 'vertical-align-tr'
                        });

                        var overallActualResultStatus_Select = $('<select>', {
                            'class': 'select form-control'
                        });
                        var overallActualResultStatus_Select_Option = $('<option>');
                        overallActualResultStatus_Select_Option.text('Select');

                        // Pass
                        var overallActualResultStatus_Select_Option_Pass = $('<option>');
                        overallActualResultStatus_Select_Option_Pass.text('Passed');

                        //Fail
                        var overallActualResultStatus_Select_Option_Fail = $('<option>');
                        overallActualResultStatus_Select_Option_Fail.text('Failed');

                        //N/A
                        var overallActualResultStatus_Select_Option_Not_Applicable = $('<option>');
                        overallActualResultStatus_Select_Option_Not_Applicable.text('N/A');

                        overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option);
                        overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Pass);
                        overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Fail);
                        overallActualResultStatus_Select.append(overallActualResultStatus_Select_Option_Not_Applicable);

                        td_overStat.append(overallActualResultStatus_Select);

                        if (overallStatus == "In Progress" || overallStatus == "") {
                            overallActualResultStatus_Select.val('N/A');
                        } else {
                            overallActualResultStatus_Select.val(overallStatus);
                        }

                        var td_showDetails = $('<td>', {
                            'class': 'vertical-align-tr',
                        });

                        var showDetails_btn = $('<button>', {
                            'class': 'aui-button',
                            'id': 'show-details-button-' + tCIMyTask,
                            'test-case-id': tc_id,
                            'test-case-name': testCase,
                            'test-case-TpId': tpTcId,
                            'test-case-plan-id': tpId,
                            'test-case-status': overallStatus,
                            'iteration-id': iterationId,
                            'test-case-TcI-my-task': tCIMyTask,
                            'test-case-user': testCaseUser,
                            'title': 'View Test Case Details',
                            'style': 'outline: none;',
                            'onclick': "angular.element(this).scope().openDialog(this)"
                        });

                        showDetails_btn.tooltip({
                            gravity: 'w'
                        });

                        //showDetails_btn.click(openDialog);

                        var showDetails_img = $('<span>', {
                            'class': 'aui-icon aui-icon-small aui-iconfont-info-filled'
                        });

                        showDetails_btn.append(showDetails_img);
                        td_showDetails.append(showDetails_btn);

                        var td_defect = $('<td>', {
                            'class': 'vertical-align-tr',
                        });

                        var defect_btn = $('<button>', {
                            'class': 'aui-button',
                            'id': 'defect-id' + tCIMyTask,
                            'test-case-id': tc_id,
                            'test-case-name': testCase,
                            'test-case-TpId': tpTcId,
                            'test-case-plan-id': tpId,
                            'test-case-status': overallStatus,
                            'iteration-id': iterationId,
                            'test-case-TcI-my-task': tCIMyTask,
                            'test-case-user': testCaseUser,
                            'title': 'Link Defect Manually',
                            'style': 'outline: none;'
                        });

                        defect_btn.tooltip({
                            gravity: 'w'
                        });

                        defect_btn.click(linkDefectDialog);

                        var defect_img = $('<span>', {
                            'class': 'aui-icon aui-icon-small aui-iconfont-tray'
                        });

                        defect_btn.append(defect_img);
                        td_defect.append(defect_btn);

                        var td_exec = $('<td>', {
                            'class': 'vertical-align-tr'
                        });

                        var overStat_tf = $('<button>', {
                            'class': 'aui-button aui-button-default align-center-tr',
                            'test-case-id': tc_id,
                            'test-case-name': testCase,
                            'test-case-TpId': tpTcId,
                            'test-case-plan-id': tpId,
                            'test-case-status': overallStatus,
                            'iteration-id': iterationId,
                            'test-case-TcI-my-task': tCIMyTask,
                            'test-case-user': testCaseUser
                        });

                        overStat_tf.click(function () {
                            var curActVal = $(this).closest('tr').find("textarea:eq(1)").val();
                            var curOveralStat = $(this).closest('tr').find("select:eq(0)").val();
                            if (curOveralStat == "Passed") {
                                curOveralStat = "Pass";
                            }
                            var curTcId = $(this).closest('tr').children('td:eq(3)').text();
                            var tpTcId = $(this).attr('test-case-TpId');
                            var tpId = $(this).attr('test-case-plan-id');
                            var tCIMyTask = $(this).attr('test-case-TcI-my-task');
                            var overallStatus = $(this).attr('test-case-status');
                            var iterationId = $(this).attr('iteration-id');
                            var testCaseUser = $(this).attr('test-case-user');
                            var testCaseName = $(this).attr('test-case-name');
                            var attachedDefectsArray = new Array();
                            var attachDefectStatus = null;

                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/defect-entity-management',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'get-saved-defects',
                                    data: JSON.stringify({
                                        tCIMyTask_Id: tCIMyTask
                                    })
                                },
                                success: function (response) {
                                    var responseObject = JSON.parse(response);
                                    if (responseObject.length > 0) {
                                        attachedDefectsArray = responseObject;
                                        attachDefectStatus = 1;
                                        //reloadTable();
                                    } else {
                                        attachDefectStatus = 2;
                                        //  reloadTable();
                                    }
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Loading The Saved Defects Data..',
                                        body: 'Contact Developer',
                                        close: 'auto'
                                    });
                                }
                            });

                            var data = JSON.stringify({
                                'curActVal': curActVal,
                                'curOveralStat': curOveralStat,
                                'curTcId': curTcId,
                                'iterationId': iterationId,
                                'tPTCI_MyTask': tCIMyTask,
                                'execEntityTPTCI': tpTcId,
                                'execEntityTPI': tpId,
                                'testCaseUser': testCaseUser,
                                'testCaseName': testCaseName,
                                'testCaseId': tc_id,
                                'attachedDefectList': attachedDefectsArray,
                                'attachDefectStatus': attachDefectStatus
                            });
                            if (curOveralStat != 'N/A' && curOveralStat != 'Select' && curActVal != '') {
                                if (saveExecution(data, false, "")) {
                                    $(this).closest('tr').hide();
                                }
                            } else {
                                if ((curOveralStat == 'N/A' || curOveralStat == 'Select') && curActVal == '') {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'Overall Actual Result and Overall Status must not be empty.',
                                        close: 'auto'
                                    });
                                } else if (curOveralStat == 'N/A' || curOveralStat == 'Select') {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'Overall Status cannot be N/A.',
                                        close: 'auto'
                                    });
                                } else if (curActVal == '') {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'Overall Actual Result cannot be empty.',
                                        close: 'auto'
                                    });
                                }

                            }
                        });
                        overStat_tf.append("Complete Execution");
                        td_exec.append(overStat_tf);
                        tr.append(td1).append(td2).append(td3).append(td4).append(td_showIterationDetails).append(td_expVal).append(td_actVal).append(td_overStat).append(td_showDetails).append(td_defect).append(td_exec);
                        $('#mytaskstable tr:last').after(tr);
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

    if (window.location.href.indexOf('MyTasksView.jspa') >= 0) {
        init();
    }

})(jQuery);
