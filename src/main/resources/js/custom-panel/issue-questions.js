AJS.toInit(function ($) {

    var $user_name = AJS.$('#header-details-user-fullname');
    var username = $user_name.attr('data-username');

    var applicationUsers = undefined;
    var issueQuestionsCount = undefined;
    var issueQuestions = undefined;
    var questionID; var currentBlockNo = 0;
    var userPropic;
    var responseSize;
    var blocks;
    var lastPost;
    var firstPost;
    var postGlobalResponse;
    var numberOfItems = 4; var start = 1;
    var moveTo = '';
    var selectedSection = 1;
    var noOfSections = 0;
    var commentList = new Array();
    var url;

    JIRA.ViewIssueTabs.onTabReady(function (tab) {
        loadTestCases();
       // url = window.location.href.split("browse")[0];
        function loadTestCases() {
            if (!($('#type-val').text().trim() == "Story")) {
              
            } else {
                $('#linkingmodule').css('display', 'none');
                var key = $('#key-val').text().trim()
                AJS.$.ajax({
                    type: 'POST',
                    url: '/jira/plugins/servlet/test-case-management',
                    async: false,
                    data: {
                        user: username,
                        action: 'getTestCaseForStory',
                        data: JSON.stringify({ 'storykey': key })
                    },
                    success: function (response) {
                        var testcases = JSON.parse(response);
                        $("#issuelink-tbody").empty();
                        var Tbody = $('#issuelink-tbody');
                        for (var i = 0; i < testcases.length; i++) {
                            var TestCase = testcases[i];
                            var TR = $('<tr>');
                            var TD = $('<td>', {
                                'style': 'border-top: none;border-bottom: 1px solid #dfe1e6'
                            }
                            );
                            var iconSpan = $('<span>', {
                                'style': 'vertical-align:middle;margin-right:3px;margin-left:5px;'
                            }
                            );
                            var statusSpan;
                            if (TestCase.status == 'To Do') {
                                statusSpan = $('<span>', {
                                    'class': 'aui-lozenge aui-lozenge-subtle aui-lozenge-default',
                                    'style': 'float: right;margin-right: 75px;margin-top: 1%;background-color: white;'
                                })
                                statusSpan.text(TestCase.status);
                            } else if (TestCase.status == 'In Progress') {
                                statusSpan = $('<span>', {
                                    'class': "aui-lozenge aui-lozenge-subtle aui-lozenge-moved",
                                    'style': 'float: right;margin-right: 35px;margin-top: 1%;background-color: white;'
                                })
                                statusSpan.text(TestCase.status);
                            } else if (TestCase.status == 'Done') {
                                statusSpan = $('<span>', {
                                    'class': 'aui-lozenge aui-lozenge-subtle aui-lozenge-success',
                                    'style': 'float:right;margin-right:12%;margin-top:1%;background-color:white;'
                                })
                                statusSpan.text(TestCase.status);
                            }
                            var testcaseIcon = $('<svg width="16" height="16" xmlns="http://www.w3.org/2000/svg" viewbox="0 0 16 16"><g fill="none" fill-rule="evenodd"><path d="M0 1.777C0 .796.796 0 1.777 0h12.446C15.204 0 16 .796 16 1.777v12.446c0 .981-.796 1.777-1.777 1.777H1.777A1.778 1.778 0 0 1 0 14.223V1.777z" fill="#8993A4"/><path d="M8 10.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM8 12a4 4 0 1 1 0-8 4 4 0 0 1 0 8z" fill="#FFF" fill-rule="nonzero"/></g></svg>');
                            iconSpan.append(testcaseIcon);
                            var nameSpan = $('<span>', {
                                'style': 'vertical-align: inherit;margin-left: 5px;'
                            });
                            var keyAnchor = $('<a>', {
                                'href': url + 'browse/' + TestCase.key,
                                'style': 'vertical-align: text-bottom;'
                            });
                            nameSpan.text(TestCase.name);
                            TD.append(iconSpan);
                            if (TestCase.status == 'Done') {
                                var strike = $('<strike>');
                                keyAnchor.text(TestCase.key);
                                strike.append(keyAnchor);
                                TD.append(strike);
                            } else {
                                keyAnchor.text(TestCase.key);
                                TD.append(keyAnchor);
                            }
                            TD.append(nameSpan);
                            TD.append(statusSpan);
                            TR.append(TD);
                            Tbody.append(TR);
                        }

                    }
                });
            }
        }

        if ($('#issue-tabs').find('.active a').text() == 'Questions') {
            // if($('#issue-questions-tab-panel').hasClass('active-tab')) {
            // AJS.$('#issue-question-status').auiSelect2();
            // AJS.$('#issue-question-assign-to').auiSelect2();
            loadUsers();
            loadQuestions();

            if ($.data($('#issue-questions-add').get(0), 'events') == undefined || $.data($('#issue-questions-add').get(0), 'events').click.length == 0) {
                $('#issue-questions-add').click(function (event) {
                    $('#issue-question-title').val('');
                    $('#issue-question-description').val('');
                    $("#message-body").empty();
                    $('#message-panel').css({ "display": "none" });
                    $('#issue-question-assign-to').val('');
                    $('#issue-question-status').val('Open');
                    $('#issue-question-status').attr('disabled', '');
                    $('#issue-question-title').removeAttr('disabled');
                    $('#save-issue-question-btn').css("visibility", "visible");
                    $('#edit-issue-question-btn').css({ "visibility": "hidden", "display": "none" });
                    $('#message-body').scrollTop($('#message-body').prop("scrollHeight"));
                    AJS.dialog2('#issue-question-dialog').show();
                });
            }

            if ($.data($('#save-issue-question-btn').get(0), 'events') == undefined || $.data($('#save-issue-question-btn').get(0), 'events').click.length == 0) {
                $('#save-issue-question-btn').click(function (event) {
                    if ($('#issue-question-title').val() == '') {
                        AJS.flag({
                            type: 'error',
                            title: 'Error',
                            body: 'Question title is a mandatary field',
                            close: 'auto'
                        });
                        return;
                    }
                    if ($('#issue-question-assign-to').val() == '') {
                        AJS.flag({
                            type: 'error',
                            title: 'Error',
                            body: 'Question should be assign to a user',
                            close: 'auto'
                        });
                        return;
                    }
                    var questionObject = {
                        'title': $('#issue-question-title').val(),
                        'description': $('#issue-question-description').val(),
                        'issue': $('#issue-id-span').text(),
                        'user': $('#issue-question-assign-to').val()
                    }
                    AJS.$.ajax({
                        type: 'POST',
                        url: '/jira/plugins/servlet/issue-questions',
                        async: false,
                        data: {
                            user: username,
                            action: 'insert',
                            data: JSON.stringify(questionObject)
                        },
                        success: function (response) {
                            AJS.flag({
                                type: 'success',
                                title: 'Success',
                                body: 'Question Added..',
                                close: 'auto'
                            });
                            AJS.dialog2('#issue-question-dialog').hide();
                            addQuestionToBody($('#issue-questions-tbody'), response);
                            $('div#issue-questions-list').animate({
                                scrollTop: $('div#issue-questions-list').prop('scrollHeight')
                            }, 500);
                        }
                    });
                });
            } else if ($.data($('#edit-issue-question-btn').get(0), 'events') == undefined || $.data($('#edit-issue-question-btn').get(0), 'events').click.length == 0) {
                $('#edit-issue-question-btn').click(function (event) {
                    var questionObject = {
                        'id': questionID,
                        'title': $('#issue-question-title').val(),
                        'description': $('#issue-question-description').val(),
                        'issue': $('#issue-id-span').text(),
                        'user': $('#issue-question-assign-to').val(),
                        'status': $('#issue-question-status').val()
                    }
                    AJS.$.ajax({
                        type: 'POST',
                        url: '/jira/plugins/servlet/issue-questions',
                        async: false,
                        data: {
                            user: username,
                            action: 'update',
                            data: JSON.stringify(questionObject)
                        },
                        success: function (response) {
                            AJS.flag({
                                type: 'success',
                                title: 'Success',
                                body: 'Question updated',
                                close: 'auto'
                            });
                            AJS.dialog2('#issue-question-dialog').hide();
                            AJS.$.ajax({
                                type: 'POST',
                                url: '/jira/plugins/servlet/issue-questions',
                                async: false,
                                data: {
                                    user: username,
                                    action: 'get',
                                    data: JSON.stringify({
                                        'issue': $('#issue-id-span').text()
                                    })
                                },
                                success: function (response) {
                                    issueQuestions = response;
                                }
                            });

                            $('#issue-questions-tbody').empty();
                            for (var i = 0; i < issueQuestions.length; i++) {
                                addQuestionToBody($('#issue-questions-tbody'), issueQuestions[i]);
                            }
                        }
                    });
                });

            }
            if ($.data($('#cancel-issue-question-btn').get(0), 'events') == undefined || $.data($('#cancel-issue-question-btn').get(0), 'events').click.length == 0) {
                $('#cancel-issue-question-btn').click(function (event) {
                    AJS.dialog2('#issue-question-dialog').hide();
                });
            }
        } else {
            $('div.issue-questions-content').remove();
            $('#issue-question-dialog').remove();
        }


        $('.btn-post').click(function (event) {
            var message = $('#msg-input').val();
            AJS.$.ajax({
                type: 'POST',
                url: '/jira/plugins/servlet/issue-questions',
                async: false,
                data: {
                    user: username,
                    action: 'insert-comment',
                    data: JSON.stringify({
                        'user': username,
                        'questionid': questionID,
                        'message': message,
                    })
                },
                success: function (response) {
                    $('#msg-input').val('');
                    selectedSection = 1;
                    commentList.length = 0;
                    loadMessages();
                    var d = $('#message-body');
                    d.scrollTop(d.prop("scrollHeight"));
                }
            });
        });

        $('#message-body').scroll(function () {
            var pos = $('#message-body').scrollTop();
            if (pos == 0) {
                if (selectedSection < noOfSections) {
                    isFirstTime = false;
                    selectedSection++;
                    loadMessages();
                    if ($('#message-body').hasScrollBar()) {
                        $('#message-body').animate({
                            scrollTop: $("#" + moveTo).offset().top
                        }, 5);
                    }
                }
            }
        });
    });

    $.fn.hasScrollBar = function () {
        return this.get(0).scrollHeight > this.height();
    }

    function loadUsers() {
        if (applicationUsers == undefined) {
            AJS.$.ajax({
                type: 'POST',
                url: '/jira/plugins/servlet/test-management-util',
                async: false,
                data: {
                    user: username,
                    action: 'get-users',
                    data: JSON.stringify({})
                },
                success: function (response) {
                    applicationUsers = response;
                }
            });
        }
        $('#issue-question-assign-to').empty();
        var emptyOption = $('<option>', {
            'value': ''
        });
        $('#issue-question-assign-to').append(emptyOption);
        for (var i = 0; i < applicationUsers.length; i++) {
            var userOption = $('<option>', {
                'value': applicationUsers[i].id
            });
            userOption.text(applicationUsers[i].displayName + ' ( ' + applicationUsers[i].emailAddress + ' )');
            $('#issue-question-assign-to').append(userOption);
        }
    }

    function loadQuestions() {

        AJS.$.ajax({
            type: 'POST',
            url: '/jira/plugins/servlet/issue-questions',
            async: false,
            data: {
                user: username,
                action: 'get-count',
                data: JSON.stringify({
                    'issue': $('#issue-id-span').text()
                })
            },
            success: function (response) {
                if (issueQuestionsCount != undefined) {
                    if (issueQuestionsCount < response.count) {
                        issueQuestionsCount = response.count;
                        issueQuestions = undefined;
                    }
                } else {
                    issueQuestionsCount = response.count;
                }
            }
        });

        if (issueQuestions == undefined) {
            AJS.$.ajax({
                type: 'POST',
                url: '/jira/plugins/servlet/issue-questions',
                async: false,
                data: {
                    user: username,
                    action: 'get',
                    data: JSON.stringify({
                        'issue': $('#issue-id-span').text()
                    })
                },
                success: function (response) {
                    issueQuestions = response;
                }
            });
        }

        $('#issue-questions-tbody').empty();
        for (var i = 0; i < issueQuestions.length; i++) {
            addQuestionToBody($('#issue-questions-tbody'), issueQuestions[i]);
        }
    }

    function addQuestionToBody(parent, question) {
        var trObject = $('<tr>');

        var tdTitleObject = $('<td>', {
            'id': question.id,
            'class': 'question-title',
            'scope': 'row'
        });
        tdTitleObject.text(question.title);
        tdTitleObject.click(function (event) {
            event.preventDefault();
            questionID = $(this).attr('id');
            userPropic = question.user.avatar;
            LoadFirst = 0;
            $('#message-panel').css({ "display": "block" });
            isFirstTime = true;
            selectedSection = 1;
            commentList.length = 0;
            loadMessages();

            $('#issue-question-title').val(question.title);
            $('#issue-question-description').val(question.description);
            $('#issue-question-assign-to').val(question.user.id);
            $('#issue-question-status').val(question.status);
            $('#issue-question-status').removeAttr('disabled');
            $('#issue-question-title').attr('disabled', '');
            $('#save-issue-question-btn').css("visibility", "hidden");
            $('#edit-issue-question-btn').css({ "visibility": "visible", "display": "inline-block" });
            AJS.dialog2('#issue-question-dialog').show();
        });

        var tdUserObject = $('<td>', {
            'class': 'question-user'
        });
        var tdUserImageObject = $('<img>', {
            'style': 'margin-right: 5px; height: 24px; width: 24px;',
            'src': question.user.avatar
        });
        var tdUserSpanObject = $('<span>', {
            'style': 'line-height: 24px; vertical-align: inherit;'
        });
        tdUserSpanObject.text(question.user.name);

        tdUserObject.append(tdUserImageObject);
        tdUserObject.append(tdUserSpanObject);


        var tdActionObject = $('<td>', {
            'class': 'question-action'
        });
        var tdActionRemoveSpan = $('<span>', {
            'id': question.id,
            'class': 'aui-icon aui-icon-small aui-iconfont-cross'
        });
        tdActionRemoveSpan.click(function (event) {
            questionID = $(this).attr('id');
            var questionObject = {
                'id': questionID
            }
            AJS.$.ajax({
                type: 'POST',
                url: '/jira/plugins/servlet/issue-questions',
                async: false,
                data: {
                    user: username,
                    action: 'delete',
                    data: JSON.stringify(questionObject)
                },
                success: function (response) {
                    AJS.flag({
                        type: 'success',
                        title: 'Success',
                        body: 'Question removed',
                        close: 'auto'
                    });
                    AJS.dialog2('#issue-question-dialog').hide();
                    AJS.$.ajax({
                        type: 'POST',
                        url: '/jira/plugins/servlet/issue-questions',
                        async: false,
                        data: {
                            user: username,
                            action: 'get',
                            data: JSON.stringify({
                                'issue': $('#issue-id-span').text()
                            })
                        },
                        success: function (response) {
                            issueQuestions = response;
                        }
                    });

                    $('#issue-questions-tbody').empty();
                    for (var i = 0; i < issueQuestions.length; i++) {
                        addQuestionToBody($('#issue-questions-tbody'), issueQuestions[i]);
                    }
                }
            });

        });
        tdActionObject.append(tdActionRemoveSpan);

        trObject.append(tdTitleObject);
        trObject.append(tdUserObject);
        trObject.append(tdActionObject);

        parent.append(trObject);
    }

    function loadMessages() {
        var counter = 0;
        $('#user-pro-pic').attr("src", userPropic);
        AJS.$.ajax({
            type: 'POST',
            url: '/jira/plugins/servlet/issue-questions',
            async: false,
            data: {
                user: username,
                action: 'get-comment',
                data: JSON.stringify({
                    'questionid': questionID,
                    'selectedSection': selectedSection
                })
            },
            success: function (response) {
                if (response.length > 0) {
                    response.forEach(element => {
                        commentList.unshift(element);
                        counter++;
                    });
                    if (counter == response.length) {
                        loadComments();
                    }
                }

            }
        });
    }

    function loadComments() {
        $("#message-body").html('');
        for (var i = 0; i < commentList.length; i++) {
            var post = JSON.parse(commentList[i]);
            if (i == 0) {
                moveTo = post.postid;
                noOfSections = post.section;
            }
            if (post != null) {
                if (post.time != "0 seconds") {
                    $('#message-body').append('<div class="message"><div class="user-icon" id=' + post.id + '><img src=' + post.icon + ' class="avatar" style="vertical-align: middle; width: 30px; height: 30px;border-radius: 50%;"><p class="u-name-text">' + post.name + '</p></div><div class="user-message" id=' + post.postid + '><p style="">' + post.message + '</p><p class="post-time">' + post.time + ' ago</p></div></div>');
                } else {
                    $('#message-body').append('<div class="user-icon" id=' + post.id + '><img src=' + post.icon + ' class="avatar" style="vertical-align: middle; width: 30px; height: 30px;border-radius: 50%;"><p class="u-name-text">' + post.name + '</p></div><div class="user-message" id=' + post.postid + '><p>' + post.message + '</p><p class="post-time">now</p></div>');
                }
            }
        }
    }




})(jQuery);