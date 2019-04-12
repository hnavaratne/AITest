appdefect.directive('ngEnter', function () {
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

appdefect.controller("defectCtrl", function ($scope, $http, $q, $sce, $timeout) {

    var storyNodeSelector = 'li[type="test-defect-story"]';
    var testCaseNodeSelector = 'li[type="test-case"]';
    $scope.selectedStoryId;
    $scope.selectedStory;
    $scope.selectedTestCase;
    $scope.selectedTestCaseId;
    $scope.versions = [];
    $scope.stories = [];
    $scope.testCases = [];
    $scope.selected = false;
    $scope.defects = [];
    $scope.availableTags = [];
    $scope.selectedStory = "";
    $scope.statusFilterArray = ["All", "To Do", "In Progress", "Done"];
    $scope.isShowStopper = false;
    $scope.isSiverityHigh = false;
    $scope.isSiverityMedium = false;
    $scope.isSiverityLow = false;
    $scope.tempHolder;
    $scope.storyTempHolder;
    $scope.totalTestCaseCount = 0;
    $scope.totalDefectCount = 0;
    $scope.totalStoryCount = 0;

    $scope.init = () => {
        //AJS.tablessortable.setTableSortable(AJS.$("#defect-table"));
        selectStatus($scope.statusFilterArray);
        getVersions().then((resp) => {
            AJS.$("#select-defect-release").tooltip();
            AJS.$("#story-search").tooltip();
            AJS.$("#defect-filter-name").tooltip();
            AJS.$("#defect-status-filter").tooltip();
            AJS.$("#assigned-user-search").tooltip();
            AJS.$("#siverity-showStopper").tooltip();
            AJS.$("#siverity-high").tooltip();
            AJS.$("#siverity-medium").tooltip();
            AJS.$("#siverity-low").tooltip();
            AJS.$('#testcase-summary').tooltip();
            $scope.versions = selectLastOption(resp.data);
            $scope.selectAction();
        }).catch(function (err) {
            if (err.status === 302) {
                if (err.data.redirect) {
                    window.location.replace(err.data.redirect);
                    return;
                }
            }
        });

    }

    jQuery(storyNodeSelector).click((event) => {
        jQuery(storyNodeSelector).removeClass('active');
        jQuery(testCaseNodeSelector).removeClass('active');
        var childUL = jQuery('ul[parent=' + $(event.currentTarget).attr('id') + ']');
        if (childUL.length !== 0) {
            if (childUL.css('display') === 'block') {
                childUL.css('display', 'none');
            } else {
                if (jQuery(event.currentTarget).attr('id') === 'test-defect-story-root-node') {
                    init("null", $scope.stories);
                } else {
                    init(jQuery(event.currentTarget).attr('id').split('-')[0], $scope.stories);
                }
                childUL.css('display', 'block');
            }
        }
    });

    jQuery(testCaseNodeSelector).click((event) => {
        jQuery(storyNodeSelector).removeClass('active');
        jQuery(testCaseNodeSelector).removeClass('active');
    });

    $scope.searchForStory = () => {
        var searchStory = $scope.searchStory.toLowerCase();
        $scope.stories = $scope.storyTempHolder;
        if (searchStory != '') {
            var storySearchArray = new Array();
            $scope.stories.forEach(story => {
                var issueKey = story.issuekey.toLowerCase();
                var name = story.summary.toLowerCase();
                if (~issueKey.indexOf(searchStory) | ~name.indexOf(searchStory)) {
                    storySearchArray.push(story);
                }
            });
            $scope.stories = storySearchArray;
        } else {
            getStroriesByVersion($scope.selectedVersion.name).then((resp) => {
                $scope.totalTestCaseCount = resp.data.totaltestcases;
                $scope.totalDefectCount = resp.data.totaldefectcount;
                $scope.storylist = resp.data.stories;
                $scope.stories = resp.data.stories;
                $scope.totalStoryCount = $scope.stories.length;
                $scope.storyTempHolder = resp.data.stories;
                $scope.defects = [];
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Stories For Version..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });
        }
    }

    $scope.setTestcase = (testCase) => {
        $scope.selectedTestCase = testCase;
    }
    $scope.showTestCaseAndDefect = (story) => {
        jQuery('#sv-breadcrum').css('display', 'block');
        $scope.selectedStory = story;
        $scope.selectedTestCase = {
            'Name': ''
        };
        $scope.selectedStoryId = story.storyid;
        getTestCases(story.storyid).then(resp => {
            testCases = resp.data;
            $scope.testCases = testCases;
            $scope.tempHolder = testCases;
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Loading Test Case..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });
    }

    $scope.searchResult = () => {
        var myText = $scope.MyText.toLowerCase();
        if (typeof $scope.selectedStoryId != 'undefined') {
            if (myText != '') {
                $scope.testCases = $scope.tempHolder;
                var selectedArray = new Array();
                for (var i = 0; i < $scope.testCases.length; i++) {
                    var testcaseObject = $scope.testCases[i];
                    var tempTestCaseObj = {
                        'id': testcaseObject.id,
                        'Name': testcaseObject.Name,
                        'instanceid': testcaseObject.instanceid,
                        'time': testcaseObject.time,
                        'defects': [],
                        'defectCount': testcaseObject.defectCount
                    };
                    var defectsList = $scope.testCases[i].defects;
                    if (defectsList.length > 0) {
                        for (var j = 0; j < defectsList.length; j++) {
                            respText = defectsList[j].name.toLowerCase();
                            if (~respText.indexOf(myText)) {
                                tempTestCaseObj.defects.push(defectsList[j]);
                                // selectedArray.push($scope.testCases[i]);
                            }
                        }
                    }
                    if (tempTestCaseObj.defects.length > 0) {
                        selectedArray.push(tempTestCaseObj);
                    }
                }
                $scope.testCases = selectedArray;
            } else {
                $scope.testCases = $scope.tempHolder;
            }
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }
    $scope.findByTestCase = () => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.serchTestcase != '') {
                $scope.testCases = $scope.tempHolder;
                var tstArray = new Array();
                for (var j = 0; j < $scope.testCases.length; j++) {
                    respText = $scope.testCases[j].Name.toLowerCase();
                    if (~respText.indexOf($scope.serchTestcase.toLowerCase())) {
                        tstArray.push($scope.testCases[j]);
                    }
                }
                $scope.testCases = tstArray;
            } else {
                $scope.testCases = $scope.tempHolder;
            }
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }

    }

    $scope.selectAction = () => {
        getStroriesByVersion($scope.selectedVersion.name).then((resp) => {
            jQuery('#sv-breadcrum').css('display', 'none');
            $scope.testCases = [];
            $scope.totalTestCaseCount = resp.data.totaltestcases;
            $scope.totalDefectCount = resp.data.totaldefectcount;
            $scope.storylist = resp.data.stories;
            $scope.storyTempHolder = resp.data.stories;
            $scope.stories = resp.data.stories;
            $scope.totalStoryCount = $scope.stories.length;
            init("null", $scope.stories);
            $scope.defects = [];
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Loading Stories For Version..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });
    }

    $scope.findByStatus = () => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.selectedStatus == 'All') {
                $scope.testCases = $scope.tempHolder;
            } else {
                $scope.testCases = $scope.tempHolder;
                var searchableDefectArray = new Array();
                for (var i = 0; i < $scope.testCases.length; i++) {
                    var testcaseObject = $scope.testCases[i];
                    var tempTestCaseObj = {
                        'id': testcaseObject.id,
                        'Name': testcaseObject.Name,
                        'instanceid': testcaseObject.instanceid,
                        'time': testcaseObject.time,
                        'defects': [],
                        'defectCount': testcaseObject.defectCount
                    };
                    var defectsList = $scope.testCases[i].defects;
                    if (defectsList.length > 0) {
                        for (var j = 0; j < defectsList.length; j++) {
                            if ($scope.selectedStatus.toLowerCase() == defectsList[j].status.toLowerCase()) {
                                tempTestCaseObj.defects.push(defectsList[j]);
                            }
                        }
                    }
                    if (tempTestCaseObj.defects.length > 0) {
                        searchableDefectArray.push(tempTestCaseObj);
                    }
                }
                $scope.testCases = searchableDefectArray;
            }
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }

    }

    $scope.findByAssignee = () => {
        var assignee = $scope.searchableAssignee;
        if (typeof $scope.selectedStoryId != 'undefined') {
            if (assignee != '') {
                var searchableDefectArray = new Array();
                $scope.testCases = $scope.tempHolder;
                for (var i = 0; i < $scope.testCases.length; i++) {
                    var testcaseObject = $scope.testCases[i];
                    var tempTestCaseObj = {
                        'id': testcaseObject.id,
                        'Name': testcaseObject.Name,
                        'instanceid': testcaseObject.instanceid,
                        'time': testcaseObject.time,
                        'defects': [],
                        'defectCount': testcaseObject.defectCount
                    };
                    var defectsList = $scope.testCases[i].defects;
                    if (defectsList.length > 0) {
                        for (var j = 0; j < defectsList.length; j++) {
                            if (assignee == defectsList[j].assigned) {
                                tempTestCaseObj.defects.push(defectsList[j]);
                            }
                        }
                    }
                    if (tempTestCaseObj.defects.length > 0) {
                        searchableDefectArray.push(tempTestCaseObj);
                    }
                }
                $scope.testCases = searchableDefectArray;
            } else {
                $scope.testCases = $scope.tempHolder;
            }
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }
    $scope.setSiveritySS = (e) => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.isShowStopper) {
                e.currentTarget.style.backgroundColor = "#fff";
                $scope.isShowStopper = false;
            } else {
                e.currentTarget.style.backgroundColor = "rgba(9,30,66,0.08)";
                $scope.isShowStopper = true;
            }
            searchForSiverity();
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }
    $scope.setSiverityH = (e) => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.isSiverityHigh) {
                e.currentTarget.style.backgroundColor = "#fff";
                $scope.isSiverityHigh = false;
            } else {
                e.currentTarget.style.backgroundColor = "rgba(9,30,66,0.08)";
                $scope.isSiverityHigh = true;
            }
            searchForSiverity(e);
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }
    $scope.setSiverityM = (e) => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.isSiverityMedium) {
                e.currentTarget.style.backgroundColor = "#fff";
                $scope.isSiverityMedium = false;
            } else {
                e.currentTarget.style.backgroundColor = "rgba(9,30,66,0.08)";
                $scope.isSiverityMedium = true;
            }
            searchForSiverity();
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }
    $scope.setSiverityL = (e) => {
        if (typeof $scope.selectedStoryId != 'undefined') {
            if ($scope.isSiverityLow) {
                e.currentTarget.style.backgroundColor = "#fff";
                $scope.isSiverityLow = false;
            } else {
                e.currentTarget.style.backgroundColor = "rgba(9,30,66,0.08)";
                $scope.isSiverityLow = true;
            }
            searchForSiverity();
        } else {
            var myFlag = AJS.flag({
                type: 'error',
                body: 'No Selected Story',
            });
        }
    }

    $scope.viewDefectInfo = (defectKey) => {
        window.open(baseUrl + "browse/" + defectKey, '_blank');
    };

    const searchForSiverity = () => {
        $scope.defects = [];
        var resultSiverityArray = new Array();
        $scope.testCases = [];
        $scope.testCases = $scope.tempHolder;
        var isAll = false;
        $scope.testCases.forEach((testcaseObject) => {
            var tempTestCaseObj = {
                'id': testcaseObject.id,
                'Name': testcaseObject.Name,
                'instanceid': testcaseObject.instanceid,
                'time': testcaseObject.time,
                'defects': [],
                'defectCount': testcaseObject.defectCount
            };
            if (testcaseObject.defects.length > 0) {
                testcaseObject.defects.forEach(defect => {
                    var siverityValue = defect.siverity;
                    if (siverityValue != 'N/A') {
                        if (siverityValue == 'Show Stopper' && $scope.isShowStopper) {
                            tempTestCaseObj.defects.push(defect);
                        } else if (siverityValue == 'High' && $scope.isSiverityHigh) {
                            tempTestCaseObj.defects.push(defect);
                        } else if (siverityValue == 'Medium' && $scope.isSiverityMedium) {
                            tempTestCaseObj.defects.push(defect);
                        } else if (siverityValue == 'Low' && $scope.isSiverityLow) {
                            tempTestCaseObj.defects.push(defect);
                        } else if (!$scope.isShowStopper && !$scope.isSiverityHigh && !$scope.isSiverityMedium && !$scope.isSiverityLow) {
                            isAll = true;
                        }
                    }
                });

            }
            if (tempTestCaseObj.defects.length > 0) {
                resultSiverityArray.push(tempTestCaseObj);
            }

        });
        if (!isAll) {
            $scope.testCases = resultSiverityArray;
        } else {
            $scope.testCases = $scope.tempHolder;
        }

    }

    const init = (parent, stories) => {
        jQuery(storyNodeSelector).removeClass('active');
        jQuery(testCaseNodeSelector).removeClass('active');
        if (parent === 'null') {
            jQuery('ul[parent="test-defect-story-root-node"]').empty();
        } else {
            jQuery('ul[parent="' + parent + '-test-case-defect-folder"]').empty();
        }
        stories.forEach(story => {
            // appendStories(jQuery('ul[parent="test-defect-story-root-node"]'), story);
        });
    };

    const appendStories = (parent, story) => {

        var liObject = jQuery('<li>', {
            'id': story.storyid + '-test-case-defect-folder',
            'type': 'test-defect-story',
        });
        var aObject = jQuery('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = jQuery('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-book'
        });
        var nameSpan = jQuery('<span>', {
            'class': 'aui-nav-item-label'
        });
        nameSpan.text(story.summary);

        aObject.append(iconSpan);
        aObject.append(nameSpan);

        liObject.append(aObject);

        liObject.click(function (event) {
            jQuery(storyNodeSelector).removeClass('active');
            jQuery(testCaseNodeSelector).removeClass('active');
            jQuery('#db-story').text(story.summary);
            jQuery('#db-testcase').text("");
            this.classList.toggle('active');
            var childUL = jQuery('ul[parent=' + jQuery(event.currentTarget).attr('id') + ']');
            var storyId = jQuery(event.currentTarget).attr('id').split("-")[0];
            $scope.selectedStoryId = storyId;
            var currentTestcase;
            $scope.defects = [];
            if (childUL.length !== 0) {
                if (childUL.css('display') === 'block') {
                    childUL.css('display', 'none');
                } else {
                    getTestCases(storyId).then(resp => {
                        testCases = resp.data;
                        if (testCases != "") {
                            jQuery('ul[parent="' + jQuery(event.currentTarget).attr('id') + '"]').empty();
                            testCases.forEach(testcase => {
                                currentTestcase = testcase;
                                appendTestCases(jQuery('ul[parent="' + jQuery(event.currentTarget).attr('id') + '"]'), currentTestcase);
                            });
                        }
                    }).catch(err => {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Loading Test Case..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    });
                    childUL.css('display', 'block');
                }
            }
        });

        var ulObject = jQuery('<ul>', {
            'class': 'aui-nav',
            'parent': story.storyid + '-test-case-defect-folder',
            'style': 'display: none'
        });

        parent.append(liObject);
        parent.append(ulObject);
    }

    const appendTestCases = (parent, testcase) => {

        var liObject = jQuery('<li>', {
            'id': testcase.id + '-test-case',
            'type': 'test-case',
        });

        var aObject = jQuery('<a>', {
            'class': 'aui-nav-item'
        });
        var iconSpan = jQuery('<span>', {
            'class': 'aui-icon aui-icon-small aui-iconfont-bullet-list'
        });
        var nameSpan = jQuery('<span>', {
        });
        if (testcase.Name.length > 15) {
            var text = testcase.Name.substr(0, 15) + '...';
            nameSpan.text(text);
        } else {
            nameSpan.text(testcase.Name);
        }

        var nameDefectCountSpan = jQuery('<span>', {
            'class': 'aui-lozenge aui-lozenge-current',
            'style': 'margin-left: 10px;'
        });
        nameDefectCountSpan.text(testcase.defectCount);

        aObject.append(iconSpan);
        aObject.append(nameSpan);
        aObject.append(nameDefectCountSpan);

        liObject.append(aObject);

        liObject.click(function (event) {
            jQuery(storyNodeSelector).removeClass('active');
            jQuery(testCaseNodeSelector).removeClass('active');
            this.classList.toggle("active");
            var ulList = jQuery(event.currentTarget).parent()[0];
            jQuery('#db-story').text(jQuery(ulList).prev().text());
            jQuery('#db-testcase').text(testcase.Name);
            $scope.selectedTestCaseId = jQuery(event.currentTarget).attr("id").split("-")[0];
            getDefectsForTestCase().then((resp) => {
                $scope.defects = resp.data;
            }).catch(err => {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Defects..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            });
        });

        parent.append(liObject);
    }

    const getStories = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-user-stories', 'data': JSON.stringify({ 'redirectView': '/secure/TestCaseDefectsView.jspa' }) };
        $http
            .post(baseUrl + 'plugins/servlet/user-story', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getVersions = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-versions', 'data': JSON.stringify({ 'redirectView': '/secure/TestCaseDefectsView.jspa' }) };
        $http
            .post(baseUrl + 'plugins/servlet/user-story', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getTestCases = (storyid) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-test-cases', 'data': JSON.stringify({ 'userStoryId': storyid, 'baseUrl': baseUrl }) };
        $http
            .post(baseUrl + 'plugins/servlet/user-story', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getDefectsForTestCase = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-defects-for-testcase', 'data': JSON.stringify({ 'testcaseid': $scope.selectedTestCaseId, 'user-story-id': $scope.selectedStoryId, 'baseUrl': baseUrl }) };
        $http
            .post(baseUrl + 'plugins/servlet/defect-entity-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getStroriesByVersion = (version) => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'get-story-by-version', 'data': JSON.stringify({ 'version': version, 'redirectView': '/secure/TestCaseDefectsView.jspa' }) };
        $http
            .post(baseUrl + 'plugins/servlet/user-story', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const selectLastOption = (versions) => {
        $scope.selectedVersion = versions[versions.length - 1];
        return $scope.versions = versions;
    };

    const selectStatus = (statusArray) => {
        $scope.selectedStatus = statusArray[0];
    }
});




