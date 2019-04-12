// agentregistry.config(['$mdThemingProvider', function ($mdThemingProvider) {
//     'use strict';

//     $mdThemingProvider.theme('default')
//         .primaryPalette('blue');
// }])
console.log('registru');
agentregistry.controller("agentregistryCtrl", function ($mdEditDialog, $scope, $http, $q, $filter, $sce, $timeout) {

    var centroidBaseUrl = "http://amupractice01:8282/control-center/services/agent";
    var foundItems;
    $scope.tempHolder;
    $scope.selectedAgent;
    $scope.vrselectedAgent;
    $scope.vrselectedAgent = [];
    $scope.agentPoolAgentsList = [];
    $scope.agentPoolDeletedAgents = [];
    $scope.agentPoolAddedAgents = [];
    $scope.sizes = [2, 5, 10];
    $scope.arsize = 2;
    $scope.init = () => {
        checkIsLogin().then(result => {
            // console.log(result.data);
        }).catch(err => {
            if (err.status === 302) {
                if (err.data.redirect) {
                    window.location.replace(err.data.redirect);
                    return;
                }
            }
        })
        $scope.loadAgents();
        $scope.loadAgentPools();
        $('#vr-ar-agents').on('change', function (e) {
            //console.log(e);
            if (typeof (e.removed) !== 'undefined') {
                var agent = e.removed.element[0].id;
                var oldList = $scope.agentPoolAgentsList;
                if (checkForDeleteAgents(agent, oldList)) {
                    $scope.agentPoolDeletedAgents.push(parseInt(agent));
                }

            } else if (typeof (e.added) !== 'undefined') {
                //  console.log($scope.vrselectedAgent[$scope.vrselectedAgent.length - 1])
                var agent = e.added.element[0].id;
                $scope.agentPoolAddedAgents.push(parseInt(agent));
            }
        });
        $('#ar-agents').on('change', function (e) {
            //console.log(e);
            if (typeof (e.removed) !== 'undefined') {
                var agent = e.removed.element[0].id;
                var index = $scope.agentPoolAddedAgents.indexOf(parseInt(agent));
                if (index > -1) {
                    $scope.agentPoolAddedAgents.splice(index, 1);
                }
            } else if (typeof (e.added) !== 'undefined') {
                //  console.log($scope.vrselectedAgent[$scope.vrselectedAgent.length - 1])
                var agent = e.added.element[0].id;
                $scope.agentPoolAddedAgents.push(parseInt(agent));
            }
          //  console.log($scope.agentPoolDeletedAgents);
          //  console.log($scope.agentPoolAddedAgents);
        });
    }

    $scope.showAddAgent = (e) => {
        e.preventDefault();
        var today = new Date();
        $('#ar-expire-input').val(today.getFullYear() + '-' + ('0' + (today.getMonth() + 1)).slice(-2) + '-' + ('0' + today.getDate()).slice(-2));
        AJS.dialog2("#add-agent-dialog").show();
    }

    $scope.showAgentPoolDisplayDialog = (e) => {

    }

    $scope.selected = [];
    $scope.limitOptions = [5, 10, 15, {
        label: 'All',
        value: function () {
            return $scope.agents.length ? $scope.agents.length : 0;
        }
    }];

    $scope.options = {
        rowSelection: true,
        multiSelect: false,
        boundaryLinks: true,
        limitSelect: true,
        pageSelect: true
    };
    $scope.query = {
        order: 'name',
        limit: 5,
        page: 1
    };
    $scope.labelOpts = {
        page: 'Page',
        rowsPerPage: "Rows",
        of: "Of"
    }

    $scope.loadAgents = () => {
        getAgents().then(agents => {
            $scope.tempHolder = agents.data;
            $scope.agents = agents.data;
            $scope.loadAgentsToSelect();
        });
    }

    $scope.loadAgentPools = () => {
        getAgentPools().then(results => {
            //  console.log(results);
            $scope.agentPools = results.data;
        })
    }

    $scope.toggleLimitOptions = function () {
        $scope.limitOptions = $scope.limitOptions ? undefined : [5, 10, 15];
    };

    $scope.loadStuff = function () {
        $scope.promise = $timeout(function () {
        }, 2000);
    }

    $scope.logItem = function (item) {
        // console.log(item, 'was selected');
        $scope.selectedAgentForDelete = item.id;

    };

    $scope.logOrder = function (order) {
        //  console.log('order: ', order);
        $scope.promise = $timeout(function () {

        }, 2000);
    };

    $scope.logPagination = function (page, limit) {
        // console.log('page: ', page);
        // console.log('limit: ', limit);
        $scope.promise = $timeout(function () {

        }, 2000);
        //$scope.promise=$scope.agents;
    }

    $scope.dismissAgentDialog = (e) => {
        e.preventDefault();
        AJS.dialog2("#add-agent-dialog").hide();
    }

    $scope.dismissAgentPoolDialog = (e) => {
        e.preventDefault();
        AJS.dialog2("#view-agent-pool-dialog").hide();
    }
    $scope.dismissCreateAgentPoolDialog = (e) => {
        e.preventDefault();
        AJS.dialog2("#add-agent-pool-dialog").hide();
    }

    $scope.createAgentPool = (e) => {
        e.preventDefault();
        //     $('#agent-creation-form')[0].reset();
        //     var date = new Date();

        //     var day = date.getDate();
        //     var month = date.getMonth() + 1;
        //     var year = date.getFullYear();

        //     if (month < 10) month = "0" + month;
        //     if (day < 10) day = "0" + day;

        //     var today = year + "-" + month + "-" + day;       
        //     $("#ar-expire-input").attr("value", today);
        //    // $('').val(new Date().toDateInputValue());
        AJS.dialog2("#add-agent-pool-dialog").show();
    }

    $scope.findAgents = () => {
        var searchText = $scope.searchName.toLowerCase();
        if (searchText != "") {
            foundItems = new Array();
            $scope.agents.forEach(agent => {
                if (~agent.name.toLowerCase().indexOf(searchText)) {
                    foundItems.push(agent);
                }
            });
            $scope.agents = foundItems;
        } else {
            $scope.agents = $scope.tempHolder;
        }

    }

    $scope.showAgentPool = (e, ap) => {
        $scope.selectedAgentPoolObj = ap;
        e.preventDefault();
        $scope.vrSelectAgentpool = ap.name;
        var poolObj = {
            "poolId": ap.id
        };
        getAgentPoolAgents(poolObj).then(results => {
            $scope.vrselectedAgent = [];
            $scope.agentPoolAgentsList = results.data;
            $("#vr-ar-agents").select2("val", "");
            $('#vr-ar-agents')
                .find('option')
                .remove()
                .end();
            $('#vr-ar-agents').select2('data', null);
            var vrAgent = $('#vr-ar-agents');
            $scope.agents.forEach(agent => {
                if (checkIsAgent(agent.name, results.data)) {
                    var vrAOption = $('<option selected>', {
                        'value': agent.name
                    });
                    //  $scope.vrselectedAgent.push(agent.name);
                    vrAOption.attr('id', agent.id);
                    vrAOption.text(agent.name);
                    vrAgent.append(vrAOption);
                } else {
                    var vrAOption = $('<option>', {
                        'value': agent.name
                    });
                    vrAOption.attr('id', agent.id);
                    vrAOption.text(agent.name);
                    vrAgent.append(vrAOption);
                }

            })
            $scope.agentPoolDeletedAgents = [];
            $scope.agentPoolAddedAgents = [];
            AJS.$("#vr-ar-agents").auiSelect2();
            AJS.dialog2("#view-agent-pool-dialog").show();
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Loading Agent Pools..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });;

    }

    const checkIsAgent = (name, arr) => {
        var count = 0;
        arr.forEach(element => {
            if (element.name == name) {
                count++;
            }
        });
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    $scope.loadAgentsToSelect = () => {
        var arAgent = $('#ar-agents');

        $scope.agents.forEach(agent => {
            var arAOption = $('<option>', {
                'value': agent.name
            });
            arAOption.text(agent.name);
            arAOption.attr('id', agent.id);
            arAgent.append(arAOption);

        });
        AJS.$("#ar-agents").auiSelect2();

    }

    $scope.removeAgentPool = (agentPool) => {
        //console.log(agentPool.id);
        deleteAgentPool(agentPool.id).then(result => {
            if (result.data.success) {
                $scope.loadAgentPools();
                console.log(result.data);
                AJS.flag({
                    type: 'success',
                    title: 'AgentPool Deleted',
                    close: 'auto'
                });
            } else {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting AgentPool..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Deleting AgentPool..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });
    }

    $scope.saveAgentPool = () => {
        var agentPoolName = $scope.arAgentPoolName;
        if (agentPoolName != "") {
            var poolObj = {
                "poolName": agentPoolName
            }
            setAgentPool(poolObj).then(result => {
                //console.log(result.data.id);
                if (result.data != null) {
                    var addedAgents = {
                        poolId: result.data.id,
                        agentId: $scope.agentPoolAddedAgents
                    }
                    setAgentsForPool(addedAgents).then(result3 => {
                        if (result3.data != null) {
                            $scope.loadAgentPools()
                        }
                    });
                    AJS.dialog2("#add-agent-pool-dialog").hide();
                    //  $scope.loadAgentPools();
                    AJS.flag({
                        type: 'success',
                        title: 'AgentPool Created',
                        close: 'auto'
                    });
                } else {
                    AJS.flag({
                        type: 'error',
                        title: 'An Error Occurred Creating AgentPool..',
                        body: 'Contact Developer',
                        close: 'auto'
                    });
                }
            });
        } else {
            AJS.flag({
                type: 'error',
                title: 'Fill required information',
                close: 'auto'
            });
        }
    }
    $scope.createAgent = () => {
        //ar-size-input
        var date = Date.parse($scope.arexpireDate);;
        var AgentObj = {
            "agentName": $scope.aragentName,
            "expDate": date,
            "size": $scope.arsize,
            "category": "",
            "active": true
        }
        setAgent(AgentObj).then(result => {
            if (result.data != null) {
                $scope.loadAgents();
                AJS.flag({
                    type: 'success',
                    title: 'Agent Created',
                    close: 'auto'
                });
            } else {
                AJS.flag({
                    type: 'error',
                    title: 'Agent Creatation Failed',
                    close: 'auto'
                });
            }
            AJS.dialog2("#add-agent-dialog").hide();
        });
    }

    $scope.removeAgent = () => {
        var removeAgentObj = {
            "agentId": $scope.selectedAgentForDelete
        }
        deleteAgent(removeAgentObj).then(result => {
            if (result.data.success) {
                $scope.agents.forEach(agent => {
                    if (agent.id == $scope.selectedAgentForDelete) {
                        var index = $scope.agents.indexOf(agent);
                        if (index > -1) {
                            $scope.agents.splice(index, 1);
                        }
                    }
                });
                AJS.flag({
                    type: 'success',
                    title: 'Agent removed...',
                    body: 'Contact Developer',
                    close: 'auto'
                });

            } else {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Deleting Agents..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    $scope.upsertChanges = () => {
        var updatedPoolObj = {
            poolName: $scope.vrSelectAgentpool,
            poolId: $scope.selectedAgentPoolObj.id
        };
        updateAgentPool(updatedPoolObj).then(result => {
            if (result.data != null) {
                if ($scope.agentPoolDeletedAgents.length > 0) {
                    var removedAgents = {
                        poolId: $scope.selectedAgentPoolObj.id,
                        agentId: $scope.agentPoolDeletedAgents
                    }
                    deleteAgentsFromPool(removedAgents).then(result2 => {
                        if (result2.data != null) {
                            $scope.loadAgentPools()
                        }
                    });
                }
                if ($scope.agentPoolAddedAgents.length > 0) {
                    var addedAgents = {
                        poolId: $scope.selectedAgentPoolObj.id,
                        agentId: $scope.agentPoolAddedAgents
                    }
                    setAgentsForPool(addedAgents).then(result3 => {
                        if (result3.data != null) {
                            $scope.loadAgentPools()
                        }

                    });
                }
            }
            AJS.dialog2("#view-agent-pool-dialog").hide();
        }).catch(err => {
            AJS.flag({
                type: 'error',
                title: 'An Error Occurred Updating Agent Pool..',
                body: 'Contact Developer',
                close: 'auto'
            });
        });;
    }

    const checkForDeleteAgents = (agent, newList) => {
        var count = 0;
        newList.forEach(item => {
            if (item.id == agent) {
                count++;
            }
        });
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    const getAgents = () => {
        var deferred = $q.defer();
        $http
            .get(centroidBaseUrl + "/browseagent")
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getAgentPools = () => {
        var deferred = $q.defer();
        $http
            .get(centroidBaseUrl + "/browseagentpool")
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const deleteAgentPool = (id) => {
        var deferred = $q.defer();
        var data = { 'poolId': id };
        $http
            .post(centroidBaseUrl + "/removepool", data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    // const setAgentPool = () => {
    //     var deferred = $q.defer();
    //     var data = { 'removepool': id };
    //     $http
    //         .post(centroidBaseUrl + "/browseagentpool")
    //         .then(function (_data) {
    //             deferred.resolve(_data);
    //         }, function (_error) {
    //             deferred.reject(_error);
    //         });
    //     return deferred.promise;
    // }

    const setAgent = (agent) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/createagent", agent)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const deleteAgent = (agent) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/removeagent", agent)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const setAgentPool = (agentPool) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/createpool", agentPool)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const getAgentPoolAgents = (agentPool) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/readpoolagent", agentPool)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const updateAgentPool = (agentPool) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/updateagentpool", agentPool)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const setAgentsForPool = (agentPoolAgent) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/addagenttopool", agentPoolAgent)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const deleteAgentsFromPool = (agentPoolAgent) => {
        var deferred = $q.defer();
        $http
            .post(centroidBaseUrl + "/removeagentfrompool", agentPoolAgent)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }

    const checkIsLogin = () => {
        var deferred = $q.defer();
        var username = jQuery('#header-details-user-fullname').attr('data-username');
        var data = { 'user': username, 'action': 'check-login', 'data': JSON.stringify({ 'redirectView': '/secure/AgentRegistryView.jspa' }) };
        $http
            .post(baseUrl + 'plugins/servlet/agent-registry-management', data)
            .then(function (_data) {
                deferred.resolve(_data);
            }, function (_error) {
                deferred.reject(_error);
            });
        return deferred.promise;
    }
});


