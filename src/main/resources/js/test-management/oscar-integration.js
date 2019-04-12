AJS.toInit(function ($) {

    var $user_name = AJS.$('#header-details-user-fullname');
    var baseUrl = window.location.href.split("secure")[0];
    var username = $user_name.attr('data-username');

    var testCaseId = null;
    var testCaseIdForRedirect = null;
    var folderIdForRedirect = null;
    var parentFolderId = null;
    var mindMapObjectStatus = 'insert';
    var mindMapDoc = null;

    var colorValue = '#dfe1e6';
    var fontColorValue = '#344563';
    var scenarioBorderColor = '#FFFFFF';

    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    function guid() {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }
        return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
    }

    function getRandomColor() {
        var letters = '0123456789ABCDEF';
        var color = '#';
        for (var i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 16)];
        }
        return color;
    }

    if (window.location.href.indexOf('OscarIntegrationView.jspa') >= 0) {
        setTimeout(() => {
            testCaseId = getParameterByName('testcase');
            testCaseIdForRedirect = getParameterByName('testcase').split('-')[0];
            folderIdForRedirect = getParameterByName('folderid').split('-')[0];
            if (!testCaseId) {
                window.location.replace(baseUrl + 'secure/TestCaseDesigningView.jspa');
            } else {

                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/oscar-generation',
                    async: false,
                    data: {
                        user: username,
                        action: 'get',
                        data: JSON.stringify({
                            'testCaseId': testCaseId.split('-')[0],
                            'redirectView': '/secure/TestCaseDesigningView.jspa'
                        })
                    },
                    success: function (response) {
                        if (response.length !== 0) {
                            mindMapObjectStatus = 'update';
                            mindMapDoc = mindmaps.ApplicationController().go(response[0].mindMap);
                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/oscar-generation',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'save',
                                    data: JSON.stringify({
                                        'xml': generateXML()
                                    })
                                },
                                success: function (response) {
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Saving Mind Map..',
                                        body: 'Contact Developer',
                                        close: 'auto'
                                    });
                                }
                            });
                        } else {
                            mindMapObjectStatus = 'insert';
                            var mindmapID = guid();
                            var rootID = guid();

                            var fontObject = {
                                'style': 'normal',
                                'weight': 'bold',
                                'decoration': 'none',
                                'size': 17,
                                'color': '#000000'
                            }

                            var mindmapJSON = {
                                'id': mindmapID,
                                'title': 'JIRA Test Management Plugin',
                                'mindmap': {
                                    'root': {
                                        'id': rootID,
                                        'text': {
                                            'font': fontObject
                                        },
                                        'offset': {
                                            'x': 0,
                                            'y': 0
                                        },
                                        'foldChildren': false,
                                        'branchColor': '#000000',
                                        'children': []
                                    }
                                },
                                'dates': {
                                    'created': (new Date()).getTime(),
                                    'modified': (new Date()).getTime()
                                },
                                'dimensions': {
                                    'x': 4000,
                                    'y': 2000
                                },
                                "autosave": false
                            }

                            $.ajax({
                                type: 'POST',
                                url: baseUrl + 'plugins/servlet/test-case',
                                async: false,
                                data: {
                                    user: username,
                                    entity: 'TestCase',
                                    action: 'get',
                                    data: JSON.stringify({
                                        'id': testCaseId.split('-')[0]
                                    })
                                },
                                success: function (testCaseResponse) {
                                    // console.log(testCaseResponse);
                                    var testCaseResponse = JSON.parse(testCaseResponse);
                                    mindmapJSON['mindmap']['root']['text'].caption = testCaseResponse.name;
                                    $.ajax({
                                        type: 'POST',
                                        url: baseUrl + 'plugins/servlet/test-case-data-management',
                                        async: false,
                                        data: {
                                            user: username,
                                            action: 'get_test_case_params',
                                            data: JSON.stringify({
                                                //project: projectid,
                                                test_case_id: testCaseId.split('-')[0]
                                            })
                                        },
                                        success: function (testDataParamResponse) {
                                            // console.log(testDataParamResponse);

                                            var iterationZero = 1;
                                            var iterationOne = 1;
                                            var iterationTwo = 1;
                                            var iterationThree = 1;

                                            var xValue = 200;
                                            var yValue = 130;
                                            var minusXValue = -200;
                                            var minusYValue = -130

                                            for (var i = 0; i < testDataParamResponse.length; i++) {
                                                var testDataParamResponseObject = testDataParamResponse[i];
                                                var paramID = guid();
                                                var paramColor = getRandomColor();

                                                var offsetObject = {};
                                                if (i % 4 === 0) {
                                                    offsetObject.y = minusYValue * iterationZero;
                                                    offsetObject.x = xValue * 1;
                                                    iterationZero++;
                                                } else if (i % 4 === 1) {
                                                    offsetObject.y = yValue * iterationOne
                                                    offsetObject.x = xValue * 1;
                                                    iterationOne++;
                                                } else if (i % 4 === 2) {
                                                    offsetObject.y = yValue * iterationTwo
                                                    offsetObject.x = minusXValue * 1;
                                                    iterationTwo++;
                                                } else if (i % 4 === 3) {
                                                    offsetObject.y = minusYValue * iterationThree
                                                    offsetObject.x = minusXValue * 1;
                                                    iterationThree++;
                                                }

                                                var paramObject = {
                                                    'id': paramID,
                                                    'parentId': rootID,
                                                    'text': {
                                                        'caption': testDataParamResponseObject.paramName,
                                                        'font': fontObject
                                                    },
                                                    'offset': offsetObject,
                                                    'foldChildren': false,
                                                    'branchColor': paramColor,
                                                    'children': []
                                                };
                                                mindmapJSON['mindmap']['root']['children'].push(paramObject);
                                            }
                                            mindMapDoc = mindmaps.ApplicationController().go(JSON.stringify(mindmapJSON));
                                        }, error: function (jqXHR, textStatus, errorThrown) {
                                            AJS.flag({
                                                type: 'error',
                                                title: 'An Error Occurred Loading Test Data Param..',
                                                body: 'Contact Developer',
                                                close: 'auto'
                                            });
                                        }
                                    });
                                }, error: function (jqXHR, textStatus, errorThrown) {
                                    AJS.flag({
                                        type: 'error',
                                        title: 'An Error Occurred Loading Test Case Data..',
                                        body: 'Contact Developer',
                                        close: 'auto'
                                    });
                                }
                            });
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
        }, 1);
    }

    $('#generate-testdata-ai-cancel-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#oscar-integration-dialog').hide();
    });

    $('#generate-testdata-ai-btn').click(function (event) {
        event.preventDefault();
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/test-case-data-management',
            async: false,
            data: {
                user: username,
                action: 'get_test_case_params',
                data: JSON.stringify({
                    //project: projectid,
                    test_case_id: testCaseId.split('-')[0]
                })
            },
            success: function (oldResponse) {
                //console.log(getPemutationValues());
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/test-case-data-management',
                    async: false,
                    data: {
                        user: username,
                        testCaseId: testCaseId.split('-')[0],
                        action: 'oscar_save_and_update',
                        oldData: JSON.stringify(oldResponse),
                        removeInfo: JSON.stringify(generateRemoveData(oldResponse)),
                        data: JSON.stringify(getPemutationValues())

                    },
                    success: function (response) {
                        AJS.flag({
                            type: 'success',
                            title: 'Permutations Saved Successfully..',
                            close: 'auto'
                        });
                        AJS.dialog2('#oscar-integration-dialog').hide();
                        routeToTestDesigning(testCaseIdForRedirect);
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Saving Permutations..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Test Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('#oscar-save-btn').click(function (event) {
        event.preventDefault();

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/oscar-generation',
            async: false,
            data: {
                user: username,
                action: mindMapObjectStatus,
                data: JSON.stringify({
                    'testCaseId': testCaseId.split('-')[0],
                    'mindMap': JSON.stringify(getJSON())
                })
            },
            success: function (response) {
                $.ajax({
                    type: 'POST',
                    url: baseUrl + 'plugins/servlet/oscar-generation',
                    async: false,
                    data: {
                        user: username,
                        action: 'save',
                        data: JSON.stringify({
                            'xml': generateXML()
                        })
                    },
                    success: function (response) {
                        if (response.result) {
                            AJS.flag({
                                type: 'success',
                                title: 'Saved Successfully..',
                                close: 'auto'
                            });
                        } else {
                            AJS.flag({
                                type: 'error',
                                title: 'An Error Occurred Saving Data..',
                                body: 'Contact Developer',
                                close: 'auto'
                            });
                        }
                    }, error: function (jqXHR, textStatus, errorThrown) {
                        AJS.flag({
                            type: 'error',
                            title: 'An Error Occurred Saving Data..',
                            body: 'Contact Developer',
                            close: 'auto'
                        });
                    }
                });
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Saving Data..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    });

    $('#oscar-back-btn').click(function (event) {
        event.preventDefault();
        routeToTestDesigning(testCaseIdForRedirect);
    });

    $('#generate-oscar-btn').click(function (event) {
        event.preventDefault();
        $.blockUI({
            message: 'Generating Test Scenario...',
            css: {
                border: 'none',
                padding: '25px',
                backgroundColor: '#000',
                '-webkit-border-radius': '10px',
                '-moz-border-radius': '10px',
                opacity: '.5',
                color: '#fff',
                fontSize: '18px',
                fontFamily: 'Verdana,Arial',
                fontWeight: 200,
            }
        });
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/oscar-generation',
            async: true,
            data: {
                user: username,
                action: 'generate',
                data: JSON.stringify({
                     test_case_id: testCaseId.split('-')[0]
                })
            },
            success: function (response) {
               // var responseObject = parseXMLOutput(response.data);
                generateTestDataHeaders($('#generated-test-data-header > tr'), Object.keys(response.data));
                generateTestDataValues($('#generated-test-data-body'), response.data);
                $.unblockUI();
                AJS.dialog2('#oscar-integration-dialog').show();
            },
            error: function (xhr, ajaxOptions, thrownError) {
                $.unblockUI();
            }
        });
    });

    function getJSON() {
        return JSON.parse(JSON.stringify(mindMapDoc.toJSON()));
    }

    function generateXML() {
        var mindmapJSON = getJSON();
        var xw = new XMLWriter('UTF-8', "1.0");

        // Start Document
        xw.writeStartDocument();

        // Start System Document
        xw.writeStartElement('System');
        xw.writeAttributeString('name', 'Tcases');

        // Start Function Document
        xw.writeStartElement('Function');
        xw.writeAttributeString('name', 'TC');

        // Start Input Document
        xw.writeStartElement('Input');

        // Start VarSet Document
        xw.writeStartElement('VarSet');
        xw.writeAttributeString('name', mindmapJSON.mindmap.root.text.caption.replace(/\s/g, '-'));

        var childrenNodes = mindmapJSON.mindmap.root.children;
        for (var i = 0; i < childrenNodes.length; i++) {
            var childNode = childrenNodes[i];

            // Start Var Document
            xw.writeStartElement('Var');
            xw.writeAttributeString('name', childNode.text.caption);

            var childNodeChildNodes = childNode.children;
            for (var j = 0; j < childNodeChildNodes.length; j++) {
                var childNodeChild = childNodeChildNodes[j];

                // Start Value Document
                xw.writeStartElement('Value');
                xw.writeAttributeString('name', childNodeChild.text.caption);

                // End Value Document
                xw.writeEndElement();
            }

            // End Var Document
            xw.writeEndElement();
        }

        // End VarSet Document
        xw.writeEndElement();

        // End Input Document
        xw.writeEndElement();

        // End Function Document
        xw.writeEndElement();

        // End System Document
        xw.writeEndElement();

        // End Document
        xw.writeEndDocument();

        var xml = xw.flush();
        xw.close();
        xw = undefined;

        return xml.replace(/\r?\n|\r|\t/g, '');
    }

    function parseXMLOutput(xml) {
        var responseObject = {};
        var parser = new DOMParser();
        var xmlObject = parser.parseFromString(xml, 'text/xml');
        var functions = xmlObject.getElementsByTagName('Function');
        for (var i = 0; i < functions.length; i++) {
            var testCaseObjects = functions[i].children;
            for (var j = 0; j < testCaseObjects.length; j++) {
                var inputObjects = testCaseObjects[j].children;
                for (var k = 0; k < inputObjects.length; k++) {
                    var varObjects = inputObjects[k].children;
                    for (var l = 0; l < varObjects.length; l++) {
                        var varObject = varObjects[l];
                        if (!responseObject[varObject.getAttribute('name').split('.')[1]]) {
                            responseObject[varObject.getAttribute('name').split('.')[1]] = [];
                        }
                        responseObject[varObject.getAttribute('name').split('.')[1]].push(varObject.getAttribute('value'))
                    }
                }
            }
        }
        return responseObject;
    }

    function generateTestDataHeaders(parent, keys) {
        parent.empty();
        var thScenarioObject = $('<th>', {
            'style': 'min-width: 100px; border-right: 1px solid ' + colorValue + '; border-bottom: 2px solid ' + scenarioBorderColor + '; background: ' + colorValue + ';'
        });
        var thScenarioSpanObject = $('<span>', {
            'style': 'font-size: 13px; color: ' + fontColorValue + '; text-transform: capitalize;'
        });
        thScenarioSpanObject.text('Scenario');
        thScenarioObject.append(thScenarioSpanObject);
        parent.append(thScenarioObject);
        for (var i = 0; i < keys.length; i++) {
            var thObject = $('<th>', {
                'style': 'min-width: 200px; border-right: 1px solid ' + colorValue + '; border-bottom: 2px solid ' + colorValue + ';'

            });
            var thSpanObject = $('<span>', {
                'style': 'font-size: 13px; color: ' + fontColorValue + '; text-transform: capitalize;'
            });
            thSpanObject.text(keys[i]);

            thObject.append(thSpanObject);
            parent.append(thObject);
        }
    }

    var countObject = {};
    function generateTestDataValues(parent, response) {
        parent.empty();
        countObject = {};
        var keys = Object.keys(response);
        var maxValueLength = 0;
        for (var i = 0; i < keys.length; i++) {
            if (maxValueLength < response[keys[i]].length) {
                maxValueLength = response[keys[i]].length;
            }
        }

        var allParamVEs = undefined;
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/oscar-generation',
            async: false,
            data: {
                user: username,
                action: 'get-param-ves',
                data: JSON.stringify({
                    'testCaseId': testCaseId.split('-')[0]
                })
            },
            success: function (response) {
                allParamVEs = response;
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Generate Test Data Values..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        for (var i = 0; i < maxValueLength; i++) {
            var trObject = $('<tr>');
            var tdScenarioObject = $('<td>', {
                'style': 'border-right: 1px solid ' + colorValue + '; border-bottom: 1px solid ' + scenarioBorderColor + '; background: ' + colorValue + ';'
            });
            tdScenarioObject.text('Scenario ' + (i + 1));
            trObject.append(tdScenarioObject);
            for (var j = 0; j < keys.length; j++) {
                if (countObject[keys[j] + '-' + response[keys[j]][i]] !== undefined) {
                    countObject[keys[j] + '-' + response[keys[j]][i]] += 1;
                } else {
                    countObject[keys[j] + '-' + response[keys[j]][i]] = 0;
                }
                var tdObject = $('<td>', {
                    'key': keys[j],
                    'style': 'border-right: 1px solid ' + colorValue + '; border-bottom: 1px solid ' + colorValue + ';'
                });
                tdObject.text(getValueExpansionForKeyValue(keys[j], response[keys[j]][i], allParamVEs[keys[j] + '-' + response[keys[j]][i]], countObject[keys[j] + '-' + response[keys[j]][i]]));
                trObject.append(tdObject);
            }
            parent.append(trObject);
        }
    }

    function generateRemoveData(response) {
        var maxValueLength = 0;
        var responseArray = [];
        for (var i = 0; i < response.length; i++) {
            if (maxValueLength < response[i].values.length) {
                maxValueLength = response[i].values.length;
            }
        }
        for (var j = 0; j < response.length; j++) {
            for (var i = 0; i < maxValueLength; i++) {
                var responseObject = {};
                responseObject.name = response[j]['paramName'];
                responseObject.value = response[j]['values'][i];
                responseArray.push(responseObject);
            }
        }
        return responseArray;
    }

    function getPemutationValues() {
        var responseArray = [];
        $('#generated-test-data-body').children().each(function (trObject) {
            var trObject = $(this);
            var responseTRArray = [];
            $(trObject).children().each(function () {
                var responseObject = {};
                var tdObject = $(this);
                if ($(tdObject).attr('key')) {
                    responseObject['name'] = $(tdObject).attr('key');
                    responseObject['value'] = $(tdObject).text();
                    responseTRArray.push(responseObject);
                }
            });
            responseArray.push(responseTRArray);
        });
        return responseArray;
    }

    $('#oscar-value-expansion-btn').click(function (event) {
        event.preventDefault();
        generateValueExpansion($('#value-expansion-data-body'));
        AJS.dialog2('#oscar-value-expansion-dialog').show();
    });

    $('#apply-value-expansion-ai-btn').click(function (event) {
        event.preventDefault();
        inserValueExpansion($('#value-expansion-data-body'));
    });

    $('#cancel-value-expansion-ai-btn').click(function (event) {
        event.preventDefault();
        AJS.dialog2('#oscar-value-expansion-dialog').hide();
    });

    function generateValueExpansion(parent) {
        var mindMap = getJSON();
        parent.empty();

        var allParamVEs = undefined;
        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/oscar-generation',
            async: false,
            data: {
                user: username,
                action: 'get-param-ves',
                data: JSON.stringify({
                    'testCaseId': testCaseId.split('-')[0]
                })
            },
            success: function (response) {
                allParamVEs = response;
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Loading Generate Value Expansion..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });

        var params = mindMap['mindmap']['root']['children'];
        for (var i = 0; i < params.length; i++) {
            var paramText = params[i]['text']['caption'];
            var paramValues = params[i]['children'];
            for (var j = 0; j < paramValues.length; j++) {
                var paramValueText = paramValues[j]['text']['caption'];

                var paramTRObject = $('<tr>');

                var paramTDNameObject = $('<td>', {
                    'style': 'width: 400px;'
                });
                paramTDNameObject.text(paramText + ' --> ' + paramValueText);

                var paramTDValueObject = $('<td>', {
                    'key': paramText + '-' + paramValueText
                });

                //-------------- Load saved values --------------

                if (allParamVEs) {
                    if (allParamVEs[paramText + '-' + paramValueText]) {
                        var paramVEs = allParamVEs[paramText + '-' + paramValueText];
                        for (var mm = 0; mm < paramVEs.length; mm++) {
                            if (paramVEs[mm] !== '') {
                                var paramValueWrapper = $('<div>', {
                                    'class': 'param-value-wrapper',
                                    'style': 'border: 1px solid ' + colorValue + '; display:inline-block; margin: 5px; border-radius: 2px;'
                                });
                                var paramValueInput = $('<input>', {
                                    'parent': paramText + '-' + paramValueText,
                                    'style': 'width: 150px; height: 30px; background-color:transparent; border:0;'
                                });
                                paramValueInput.val(paramVEs[mm]);
                                var paramValueButton = $('<button>', {
                                    'style': 'background-color:transparent; border:0; outline: none;'
                                });
                                paramValueButton.click(function (event) {
                                    event.preventDefault();
                                    $(event.currentTarget).parent().remove();
                                });
                                var paramValueButtonSpan = $('<span>', {
                                    'class': 'aui-icon aui-icon-small aui-iconfont-cross-circle'
                                });
                                paramValueButton.append(paramValueButtonSpan);

                                paramValueWrapper.append(paramValueInput);
                                paramValueWrapper.append(paramValueButton);

                                paramTDValueObject.append(paramValueWrapper);
                            }
                        }
                    }
                }

                // ----------------------------------------------

                var paramValueAddBtn = $('<button>', {
                    'class': 'aui-button',
                    'style': 'outline: none;'
                });
                paramValueAddBtn.click(function (event) {
                    event.preventDefault();
                    var currentTarget = $(event.currentTarget);

                    var paramValueWrapper = $('<div>', {
                        'class': 'param-value-wrapper',
                        'style': 'border: 1px solid ' + colorValue + '; display:inline-block; margin: 5px; border-radius: 2px;'
                    });
                    var paramValueInput = $('<input>', {
                        'parent': paramText + '-' + paramValueText,
                        'style': 'width: 150px; height: 30px; background-color:transparent; border:0;'
                    });
                    var paramValueButton = $('<button>', {
                        'style': 'background-color:transparent; border:0; outline: none;'
                    });
                    paramValueButton.click(function (event) {
                        event.preventDefault();
                        paramValueWrapper.remove();
                    });
                    var paramValueButtonSpan = $('<span>', {
                        'class': 'aui-icon aui-icon-small aui-iconfont-cross-circle'
                    });
                    paramValueButton.append(paramValueButtonSpan);

                    paramValueWrapper.append(paramValueInput);
                    paramValueWrapper.append(paramValueButton);

                    paramValueWrapper.insertBefore(currentTarget);
                });

                var paramValueAddBtnSpan = $('<span>', {
                    'class': 'aui-icon aui-icon-small aui-iconfont-add-circle'
                });

                paramValueAddBtn.append(paramValueAddBtnSpan);
                paramTDValueObject.append(paramValueAddBtn);

                paramTRObject.append(paramTDNameObject);
                paramTRObject.append(paramTDValueObject);

                parent.append(paramTRObject);
            }
        }
    }

    function inserValueExpansion(parent) {
        var parametersArray = [];
        parent.children().each(function () {
            var trObject = $(this);
            var tdObject = $(trObject.children()[1]);
            var parameterObject = {
                'param': tdObject.attr('key').split('-')[0],
                'paramV': tdObject.attr('key').split('-')[1]
            };
            var paramVEArray = [];
            tdObject.children('div.param-value-wrapper').each(function () {
                var inputVal = $($(this).children('input')[0]).val();
                if (inputVal != '') {
                    paramVEArray.push({'value': inputVal});
                }
            });
            parameterObject['paramVE'] = paramVEArray;
            parametersArray.push(parameterObject);
        });

        $.ajax({
            type: 'POST',
            url: baseUrl + 'plugins/servlet/oscar-generation',
            async: false,
            data: {
                user: username,
                action: 'insert-param-ve',
                data: JSON.stringify({
                    'testCaseId': testCaseId.split('-')[0],
                    'parameters': parametersArray
                })
            },
            success: function (response) {
                AJS.flag({
                    type: 'success',
                    title: 'Value Expansion Saved Successfully..',
                    close: 'auto'
                });
                AJS.dialog2('#oscar-value-expansion-dialog').hide();
            }, error: function (jqXHR, textStatus, errorThrown) {
                AJS.flag({
                    type: 'error',
                    title: 'An Error Occurred Saving Value Expansion..',
                    body: 'Contact Developer',
                    close: 'auto'
                });
            }
        });
    }

    function getValueExpansionForKeyValue(key, value, paramVE, count) {
        var returnValue = value
        if (paramVE) {
            var paramVEs = paramVE.split(',')
            if (paramVEs[count] && paramVEs[count] != '') {
                returnValue = paramVEs[count];
            }
            if (count == paramVEs.length - 1) {
                countObject[key + '-' + value] = 0;
                delete countObject[key + '-' + value];
            }
        }
        return returnValue;
    }

    function routeToTestDesigning(testCaseIdForRedirect) {

        if (localStorage.folderHierarchy != "root-node") {
            window.location.replace(baseUrl + 'secure/TestCaseDesigningView.jspa?testcase=' + testCaseIdForRedirect + '-test-case' + '?folderid=' + folderIdForRedirect + '-folder-id?ai-redirect=true');
        } else {
            window.location.replace(baseUrl + 'secure/TestCaseDesigningView.jspa?testcase=' + testCaseIdForRedirect + '-test-case' + '?folderid=' + folderIdForRedirect + '-folder-id?ai-redirect=true');
        }
    }

});