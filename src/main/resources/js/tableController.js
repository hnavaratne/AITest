var baseUrl = window.location.href.split("secure")[0];
var $user_name = AJS.$('#header-details-user-fullname');
var username = $user_name.attr('data-username');

app.directive('ngEnter', function () {
    return function (scope, element, attrs) {
        element.bind("keydown keypress", function (event) {
            if (event.which === 13) {
                scope.$apply(function () {
                    scope.$eval(attrs.ngEnter);
                });
                event.preventDefault();
            }
        });
    };
});

app.directive('ngRightClick', function ($parse) {
    return function (scope, element, attrs) {
        var fn = $parse(attrs.ngRightClick);
        element.bind('contextmenu', function (event) {
            scope.$apply(function () {
                event.preventDefault();
                fn(scope, { $event: event });
            });
        });
    };
});
app.directive('pasteExcel', function () {
    var linkFn = function (scope, element, attrs) {
        var onPasteSpecial = false;
        element.on('keydown keypress', function (event) {
            if (event.ctrlKey && event.shiftKey && event.which == 86) {
                onPasteSpecial = true;
                console.log('on paste');
            }
        });

        element.on('paste', function (event) {
            if (onPasteSpecial) {
                jQuery.each(event.originalEvent.clipboardData.items, function (i, v) {
                    if (v.type === 'text/plain') {
                        v.getAsString(function (clipText) {
                            setTimeout(function () {
                                clipRows = clipText.split(String.fromCharCode(13));
                                var data = [];
                                // split rows into columns
                                for (var i = 0; i < clipRows.length; i++) {
                                    var cells = clipRows[i].split(String.fromCharCode(9));
                                    var newObj = {};
                                    if (scope.clickedCellIndex == 0) {
                                        if (cells[0]) {
                                            if (cells[0].trim().charAt(0) === '"' && cells[0].trim().charAt(cells[0].trim().length - 1) === '"') {
                                                newObj.step = cells[0].trim().substr(1, cells[0].trim().length - 2).trim();
                                            } else {
                                                newObj.step = cells[0].trim();
                                            }
                                        }
                                        if (cells[1]) {
                                            if (cells[1].trim().charAt(0) === '"' && cells[1].trim().charAt(cells[0].trim() - 1) === '"') {
                                                newObj.expectedResult = cells[1].trim().substr(1, cells[0].trim().length - 2).trim();
                                            } else {
                                                newObj.expectedResult = cells[1].trim();
                                            }
                                        }
                                        if (cells[2]) {
                                            if (cells[2].trim().charAt(0) === '"' && cells[2].trim().charAt(cells[0].trim().length - 1) === '"') {
                                                newObj.data = cells[2].trim().substr(1, cells[0].trim().length - 2).trim()
                                            } else {
                                                newObj.data = cells[2].trim();
                                            }
                                        }

                                    } else if (scope.clickedCellIndex == 1) {
                                        if (cells[0]) {
                                            if (cells[0].trim().charAt(0) === '"' && cells[0].trim().charAt(cells[0].trim().length - 1) === '"') {
                                                newObj.expectedResult = cells[0].trim().substr(1, cells[0].trim().length - 2).trim();
                                            } else {
                                                newObj.expectedResult = cells[0].trim();
                                            }
                                        }
                                        if (cells[1]) {
                                            if (cells[1].trim().charAt(0) === '"' && cells[1].trim().charAt(cells[0].trim().length - 1) === '"') {
                                                newObj.expectedResult = cells[1].trim().substr(1, cells[0].trim().length - 2).trim();
                                            } else {
                                                newObj.expectedResult = cells[1].trim();
                                            }
                                        }
                                    } else if (scope.clickedCellIndex == 2) {
                                        if (cells[0]) {
                                            if (cells[0].trim().charAt(0) === '"' && cells[0].trim().charAt(cells[0].trim().length - 1) === '"') {
                                                newObj.data = cells[0].trim().substr(1, cells[0].trim().length - 2).trim();
                                            } else {
                                                newObj.data = cells[0].trim();
                                            }
                                        }
                                    }

                                    if (cells[0] || cells[1] || cells[2]) {
                                        data.push(newObj);
                                    }

                                }
                                var startIndex = scope.clickedIndex;
                                // if (scope.newstep.step) {
                                //     if (scope.clickedIndex == 0) {
                                //         scope.steps.splice(0, 1);
                                //         startIndex = 0;
                                //     } else if (scope.clickedIndex != -1) {
                                //         scope.steps.splice(scope.clickedIndex, 1);
                                //         startIndex = scope.clickedIndex;
                                //     }
                                // }
                                if (scope.clickedIndex == -1) {
                                    if (data.length == 1) {
                                        if (data[0].step) {
                                            scope.newstep.step = data[0].step;
                                        }
                                        if (data[0].description) {
                                            scope.newstep.description = data[0].description;
                                        }
                                        if (data[0].data) {
                                            scope.newstep.data = data[0].data;
                                        }

                                    } else {
                                        for (var j = 0; j < data.length; j++) {
                                            if (data[j].step || data[j].expectedResult || data[j].data) {
                                                scope.steps.push(data[j]);
                                            }
                                        }
                                        scope.newstep = {};
                                    }

                                } else {
                                    for (var j = 0; j < data.length; j++) {
                                        if (scope.steps[startIndex]) {
                                            if (scope.selectedStep && scope.selectedStep.step && data[j].step) {
                                                scope.steps[startIndex].step = data[j].step;
                                            } else if (scope.selectedStep && data[j].step) {
                                                scope.steps[startIndex].step = data[j].step;
                                            }
                                            if (scope.selectedStep && scope.selectedStep.expectedResult && data[j].expectedResult) {
                                                scope.steps[startIndex].expectedResult = data[j].expectedResult;
                                            } else if (scope.selectedStep && data[j].expectedResult) {
                                                scope.steps[startIndex].expectedResult = data[j].expectedResult;
                                            }

                                            if (scope.selectedStep && scope.selectedStep.data && data[j].data) {
                                                scope.steps[startIndex].data = data[j].data;
                                            } else if (scope.selectedStep && data[j].data) {
                                                scope.steps[startIndex].data = data[j].data;
                                            }

                                        } else {
                                            if (data[j].step || data[j].expectedResult || data[j].data) {
                                                scope.steps.splice(startIndex, 0, data[j]);
                                            }
                                        }
                                        startIndex++;
                                    }
                                }
                                scope.$apply();

                            }, 0);

                        });

                    }
                });
            }
            onPasteSpecial = false;
        });

    };

    return {
        restrict: 'A',
        link: linkFn
    };
});


app.controller("excelCtrl", function ($scope, $http, $q, $sce, $timeout, mentioUtil) {

    $scope.newstep = {};
    $scope.newbsbdtstep = {};
    $scope.bdtSteps;
    $scope.bsbdtSteps;
    $scope.changeSet = [];
    window.location.hash = '';
    $scope.currentTarget;
    $scope.elements;
    $scope.isMentioned = false;
    $scope.selectedSCID = 0;
    $scope.isViewSteps = false;
    $scope.index = 0;
    $scope.clickedIndex = -1;
    $scope.clickedCellIndex = -1;
    $scope.testCaseId = '';
    $scope.selectedStep = {};
    $scope.selectedBCStep = {};
    $scope.issues = [];
    $scope.issueKeyForDelete;
    $scope.targetField = null;
    $(window).on('hashchange', function () {
        var id = window.location.hash.replace(/^#!/, '');
        id = id.split("/")[1];
        var testid = id;
        var testCaseId = testid.split("-")[0];

        if (testCaseId) {
            $scope.testCaseId = testCaseId;
            $('#test-step-tb td textarea').val('');
            // testCaseId.split('#')[testCaseId.split('#').length - 1].split('-')[0]
            getSteps($scope.testCaseId).then(function (data) {
                $scope.steps = data.data.steps;
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Steps..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
            if (testid.split("-")[1] != "business") {
                populateLinkedIssueTable().then((resp) => {
                    $scope.issues = resp.data;
                }).catch(err => {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Loading The Linked Issues..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                });;
            }
        }

        //console.log();
        if (testid.split("-")[1] == "business") {
            // testCaseId.split('#')[testCaseId.split('#').length - 1].split("-")[1] == "business") {
            getBdtSteps().then((resp) => {
                $scope.bsbdtSteps = resp.data.steps;
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The BDT Steps..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
        }

        $scope.isViewSteps = true;
        if (!$scope.isViewSteps) {
            jQuery('#bdt-step-details')[0].style.visibility = "hidden";
            jQuery('#tab-bdt-steps')[0].style.visibility = "hidden";
            //  jQuery('#bdt-steps-table-content')[0].style.display = "none";
        }
    });

    $scope.getCaretCoordinates = function (element) {

        const clone = document.createElement('div')
        const computedStyle = getComputedStyle(element)
        for (const prop of computedStyle) {
            clone.style[prop] = computedStyle[prop]
        }
        clone.style.whiteSpace = 'pre-wrap'
        clone.style.wordWrap = 'break-word'
        clone.style.position = 'absolute'
        clone.style.visibility = 'hidden'
        clone.style.overflow = 'hidden'
        clone.textContent = element.value.substr(0, element.selectionStart)

        const caret = document.createElement('span')
        caret.textContent = '|'

        clone.appendChild(caret)
        document.body.appendChild(clone)
        const { offsetTop, offsetLeft, offsetHeight } = caret
        clone.remove()

        return {
            x: offsetLeft + element.offsetLeft - element.scrollLeft,
            y: offsetTop + element.offsetTop - element.scrollTop,
            height: offsetTop
        }
    };

    $scope.mentionSelect = ($event) => {

        //  $($scope.currentTarget).val(ltrim($($event.currentTarget).text()));
        $scope.selectedSCID = $($event.currentTarget).attr('scid');
        $($scope.currentTarget).val($($event.currentTarget).text());
        $($scope.currentTarget).attr('bcid', $($event.currentTarget).attr('scid'));
        $scope.isMentioned = false;
        $('.jira-mention')[0].style.display = "none";
        //  if (typeof $scope.steps[$scope.clickedIndex].step != undefined) {
        if ($scope.clickedIndex == -1) {
            $scope.newstep.step = $($scope.currentTarget).val();
            $scope.newstep.function = $scope.selectedSCID;
            $scope.steps.push($scope.newstep);
            $scope.newstep = {};
        } else {
            $scope.steps[$scope.clickedIndex].step = $($scope.currentTarget).val();
            $scope.steps[$scope.clickedIndex].function = $scope.selectedSCID;
        }

        // }
    }

    $scope.resize = (e) => {
        setInterval(() => { resizeEvent(e) }, 1000 / 15);
    }
    const resizeEvent = (e) => {
        var elementHeight = $(e.currentTarget).height();
        var textareaList = $(e.currentTarget).parent().parent().find('textarea');
        for (var i = 0; i < textareaList.length; i++) {
            // console.log($(textareaList[i]));
            $(textareaList[i]).css('height', elementHeight + 'px');
        }
    }

    jQuery('#steps-tab').click(() => {
        jQuery('#bdt-step-details')[0].style.visibility = "hidden";
        jQuery('#tab-bdt-steps')[0].style.visibility = "hidden";
    });

    function ltrim(str) {
        if (str == null) return str;
        return str.replace(/^\s+/g, '');
    }

    $scope.mention = ($event) => {
        $scope.currentTarget = $($event.currentTarget)[0];
        if ($event.which == 50) {
            $scope.isMentioned = true;
        }
        if ($event.which == 8) {
            if ($($event.currentTarget).attr('bcid') != '') {
                var id = $($event.currentTarget).attr('bcid');
                if (id) {
                    for (var i = 0; i < $scope.steps.length; i++) {
                        if ($scope.steps[i].function == id && i == $scope.clickedIndex) {
                            $scope.steps[i].function = 0;
                        }
                    }
                } else {
                    $scope.newstep.function = 0;
                }

            }
        }
        if ($scope.isMentioned) {
            $scope.appendToMentionList($($scope.currentTarget).val().split("@")[1]);
            setTimeout(() => {
                const coords = $scope.getCaretCoordinates($($event.currentTarget)[0]);
                $('.jira-mention')[0].style.top = coords.y + $($event.currentTarget).position().top + 10 + 'px'
                $('.jira-mention')[0].style.left = coords.x + $($event.currentTarget).position().left + 'px'
                $('.jira-mention')[0].style.display = "block";
            }, 0);
        }
    }

    $scope.viewSteps = (id) => {
        $('#bdt-step-details')[0].style.visibility = "visible";
        $('#tab-bdt-steps')[0].style.visibility = "visible";
        $('#bdt-step-details>a').click();
        getBCSteps(id).then((data) => {
            $scope.bdtSteps = data.data.steps;
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Loading The Business Script Steps..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });;
    }

    const getBCSteps = (id) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get_business_steps', 'data': JSON.stringify({ 'businessScriptId': id }) };
        $http
            .post(baseUrl + 'plugins/servlet/business-script-steps-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    $scope.addStep = function () {
        if ($scope.newstep.step) {
            if (!$scope.steps) {
                $scope.steps = [];
            }
            $scope.steps.push($scope.newstep);
            $scope.newstep = {};
        }
    }

    $scope.addbsbdtStep = () => {
        if ($scope.newbsbdtstep.step) {
            if (!$scope.bsbdtSteps) {
                $scope.bsbdtSteps = [];
            }
            $scope.bsbdtSteps.push($scope.newbsbdtstep);
            if ($scope.newbsbdtstep.data) {
                var data = { id: '', param_Name: $scope.newbsbdtstep.data, state: 'new' };
                $scope.changeSet.push(data);
            }
            $scope.newbsbdtstep = {};
        }
    }

    $scope.appendToMentionList = (text) => {

        if (text == "") {
            var array = [];
            var count = 0;
            $scope.getAllScripts().then((resp) => {
                for (var i = 0; i < resp.data.length; i++) {
                    if (count < 10) {
                        array.push(resp.data[i]);
                        count++;
                    }

                }
                $scope.elements = array;
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Business Script..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
        } else {
            var array = [];
            var count = 0;
            var respText;
            $scope.getAllScripts().then((resp) => {
                for (var i = 0; i < resp.data.length; i++) {
                    respText = resp.data[i].Name
                    if (count < 10 && ~respText.indexOf(text)) {
                        array.push(resp.data[i]);
                        count++;
                    }
                }
                $scope.elements = array;
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Business Script..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
        }

    }

    $scope.resetIndex = function () {
        $scope.index = 0;
    };

    $scope.myContextDiv = "<ul id='contextmenu-node'><li class='contextmenu-item' ng-click='clickedDelete()'> Delete </li><li class='contextmenu-item' ng-click='pasteSpecial(event)'> PasteSpecial </li></ul>";

    $scope.clickedDelete = function () {
        if ($scope.index == 0) {
            $scope.steps.splice(0, 1);
        } else {
            $scope.steps.splice($scope.index, 1);
        }
    };

    $scope.clickedDeleteBdt = () => {
        if ($scope.changeSet.length > 0) {
            if ($scope.bsbdtSteps[$scope.index].id == undefined) {
                for (i = 0; i < $scope.changeSet.length; i++) {
                    if ($scope.changeSet[i].param_Name == $scope.bsbdtSteps[$scope.index].data) {
                        $scope.changeSet.splice(i, 1);
                        break;
                    }
                }
            } else {
                for (i = 0; i < $scope.changeSet.length; i++) {
                    if ($scope.changeSet[i].id == $scope.bsbdtSteps[$scope.index].id) {
                        $scope.changeSet[i].state = 'deleted';
                        break;
                    }
                }
            }
        } else {
            var elementId = $scope.bsbdtSteps[$scope.index].id;
            var paramName = $scope.bsbdtSteps[$scope.index].data;
            var data = { id: elementId, param_Name: paramName, state: 'deleted'};
            $scope.changeSet.push(data); 
        }

        if ($scope.index == 0) {
            $scope.bsbdtSteps.splice(0, 1);
        } else {
            $scope.bsbdtSteps.splice($scope.index, 1);
        }
    }

    $scope.clickedDeleteLink = () => {
        if ($scope.index == 0) {
            var issue = $scope.issues.splice(0, 1);
            deleteLink(issue[0].issue_name).then((resp) => {
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Linked Issue..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
        } else {
            var issue = $scope.issues.splice($scope.index, 1);
            deleteLink(issue[0].issue_name).then((resp) => {
                console.log(resp);
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting The Linked Issue..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;
        }
    }

    $scope.save = function () {
        saveTestCase($scope.testCaseId).then(
            function (data) {
                validateParameters($scope.testCaseId, $scope.steps).then(function (validate) {
                    if (validate.data.length > 0) {
                        AJS.flag({
                            type: 'error',
                            title: 'Error Saving Test Steps',
                            body: 'Duplicate parameter(s) exists in either in Bussiness Function steps or test steps.' + validate.data,
                            close: 'auto'
                        });
                    } else {
                        saveTestSteps($scope.testCaseId, $scope.steps, $scope.changeSet).then(function (insert) {
                            AJS.flag({
                                type: 'success',
                                title: 'Successfully Updated Test Steps..',
                                close: 'auto'
                            });
                        }, function (insertError) {
                            AJS.flag({
                                type: 'error',
                                title: 'An Error Occurred Inserting The Test Steps..',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        });
                    }
                }, function (validateError) {
                });

            }, function (error) {
            });
    }

    $scope.setSelectedIndex = function (index) {
        $scope.index = index;
    }

    $scope.onChange = function () {
        var elementId = $scope.targetField.attributes['step_id'].value;
        var paramName = $scope.bsbdtSteps[$scope.clickedIndex].data;
        var data = { id: elementId, param_Name: paramName };
        var idExists = false;
        for (i = 0; i < $scope.changeSet.length; i++) {
            if (elementId && $scope.changeSet[i].id == elementId) {
                $scope.changeSet[i].param_Name = paramName;
                $scope.changeSet[i].state = 'change';
                idExists = true;
                break;
            } else {
                $scope.changeSet[i].param_Name = paramName;
                $scope.changeSet[i].state = 'new';
            }
        }
        if (!idExists) {
            $scope.changeSet.push(data);
        }

    }

    $scope.onStepParamChange = function () {
        var stepId = $scope.targetField.attributes['stepId'].value;
    }

    $scope.setClickedIndex = function (index, cellIndex) {
        $scope.clickedIndex = index;
        $scope.clickedCellIndex = cellIndex;
        if ($scope.steps[index] && $scope.steps[index].step) {
            $scope.selectedStep.step = $scope.steps[index].step;
        }
        if ($scope.steps[index] && $scope.steps[index].expectedResult) {
            $scope.selectedStep.expectedResult = $scope.steps[index].expectedResult;
        }
        if ($scope.steps[index] && $scope.steps[index].data) {
            $scope.selectedStep.data = $scope.steps[index].data;
        }
    }

    $scope.setBCClickedIndex = function (index, cellIndex, $event) {
        $scope.clickedIndex = index;
        $scope.clickedCellIndex = cellIndex;

        if ($event.target == null) {
            return;
        }

        $scope.targetField = $event.target;
    }

    $scope.insertNewRow = () => {
        var emptyRow = {};
        $scope.steps.splice($scope.index + 1, 0, emptyRow);

    }

    $scope.insertNewRowUp = () => {
        var emptyRow = {};
        if ($scope.index - 1 <= 0) {
            $scope.steps.splice(0, 0, emptyRow);
        } else {
            $scope.steps.splice($scope.index - 1, 0, emptyRow);
        }

    }

    $scope.insertNewRowBdt = () => {
        var emptyRow = {};
        $scope.bsbdtSteps.splice($scope.index + 1, 0, emptyRow);
    }

    $scope.insertNewRowBdtUp = () => {
        var emptyRow = {};
        if ($scope.index - 1 <= 0) {
            $scope.bsbdtSteps.splice(0, 0, emptyRow);
        } else {
            $scope.bsbdtSteps.splice($scope.index - 1, 0, emptyRow);
        }
    }

    $scope.isBCFunction = (data) => {
        if (data) {
            return true;
        } else {
            return false;
        }
    }

    function validateParameters(testCaseId, steps) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'validate', 'data': JSON.stringify({ 'testCaseId': testCaseId, 'steps': steps }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-step', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    function saveTestSteps(testCaseId, steps) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'insert', 'data': JSON.stringify({ 'testCaseId': testCaseId, 'steps': steps }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-step', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    function saveTestCase(testCaseId) {

        var name = jQuery('#test-case-name').val();
        var description = jQuery('#test-case-description').val();
        var overall_expected_result = jQuery('#test-case-overall-expected-result').val();
        var manualString = jQuery('input[name="manualRadioButtons"]:checked').val();
        var automatedString = jQuery('input[name="automatedRadioButtons"]:checked').val();

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'update', 'data': JSON.stringify({ 'id': testCaseId, 'name': name, 'description': description, 'overall_expected_result': overall_expected_result, 'manual': manualString, 'automated': automatedString }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-case', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    function getSteps(testCaseId) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get', 'data': JSON.stringify({ 'testCaseId': testCaseId }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-step', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    var searchObjectArray = new Array();
    function getSteps(testCaseId) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get', 'data': JSON.stringify({ 'testCaseId': testCaseId }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-step', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    $scope.getAllScripts = () => {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-project-bcscripts', 'data': JSON.stringify({}) };
        $http
            .post(baseUrl + 'plugins/servlet/bcomponent', data)
            .then((_data) => {
                deferred.resolve(_data);
            }, (_error) => {
                deferred.reject(_error);
            })
        return deferred.promise;
    }

    $scope.saveBCSteps = () => {

        validateBdtParameters($scope.testCaseId, $scope.bsbdtSteps).then(function (validate) {
            if (validate.data.length > 0) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Inserting The Bdt Steps..',
                    body: 'Duplicate parameter(s) exists.' + validate.data,
                    close: 'auto'
                });
            } else {
                saveBdtSteps($scope.testCaseId, $scope.bsbdtSteps, $scope.changeSet).then(function (insert) {
                    AJS.flag({
                        type: 'success',
                        title: 'Business Script updated..',
                        close: 'auto'
                    });
                    $scope.changeSet = [];
                }, function (insertError) {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Inserting The Bdt Steps..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }).catch(err => {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Saving The Bdt Steps..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                });;
            }
        }, function (validateError) {

        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Validating The Bdt Steps..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });;

    }

    function validateBdtParameters(testCaseId, bdtsteps) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'validate_business_steps', 'data': JSON.stringify({ 'businessScriptId': testCaseId, 'steps': bdtsteps }) };
        $http
            .post(baseUrl + 'plugins/servlet/bcomponent', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    function saveBdtSteps(testCaseId, bdtsteps, changeSet) {

        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'insert_business_steps', 'data': JSON.stringify({ 'businessScriptId': testCaseId, 'steps': bdtsteps, 'changeSet': changeSet }) };
        $http
            .post(baseUrl + 'plugins/servlet/business-script-steps-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getBdtSteps = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get_business_steps', 'data': JSON.stringify({ 'businessScriptId': $scope.testCaseId }) };
        $http
            .post(baseUrl + 'plugins/servlet/business-script-steps-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    AJS.$("#createissuebtn").click(function (e) {
        var value = jQuery('#select2-issueid:selected').text();
        var optionValues = [];
        var count = 0;
        for (var i = 0; i < $("#select2-issueid").select2('data').length; i++) {
            optionValues.push($("#select2-issueid").select2('data')[i].element[0].id);
            count++;
        }
        createLink(count, optionValues).then((res) => {
            populateLinkedIssueTable().then((resp) => {
                $scope.issues = resp.data;
                AJS.dialog2("#demo-dialog").hide();
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading The Linked Issues..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });;

        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Creating The Linked Issues..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });
    });

    const createLink = (count, optionValues) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'create', 'data': JSON.stringify({ 'testcaseid': $scope.testCaseId, 'number': count, 'issues': JSON.stringify(optionValues) }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-case-management-view-project-issues', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    };

    const deleteLink = (issueKey) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'delete-issue-link', 'data': JSON.stringify({ 'testcaseid': $scope.testCaseId, 'issuekey': issueKey }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-case-management-view-project-issues', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const populateLinkedIssueTable = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get', 'data': JSON.stringify({ 'testcaseid': $scope.testCaseId }) };
        $http
            .post(baseUrl + 'plugins/servlet/test-case-management-view-project-issues', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    $scope.viewUserStory = (userStoryKey) => {
        window.open(baseUrl + "browse/" + userStoryKey, '_blank');
    };


});