AJS.toInit(function ($) {
  var $qa_star_rating = $('.qarci .aui-icon.aui-icon-small');
  var $dev_star_rating = $('.devrci .aui-icon.aui-icon-small');
  var $dev_star_rating_input = $('.devrci .text');
  var $requirement_id = $('.requirementId');
  var $advance_search = $('#advance-search');
  var $requirement_filter_panel = $('#filter-panel');
  var $edit_issue_submit = $('#edit-issue-submit');
  var $requirement_user_name = $('#header-details-user-fullname');
  var $my_requirement = $('#myRequiremets');
  var $testcase_parent = $('#select2-example');
  var $filter_highiest = $('.req-highest');
  var $filter_high = $('.req-high');
  var $filter_lowest = $('.req-lowest');
  var $filter_low = $('.req-low');
  var $load_user = $('#select2-example');
  var $get_users_click = $('#users');
  var $get_user_name = $('#users');
  var $setIssuePriority = $('#highest');

  var setQaRciRating = function (count) {
    $qa_star_rating.each(function () {
      if ($(this).attr('rating') <= count) {
        $(this).removeClass('aui-iconfont-new-star').addClass('aui-iconfont-star-filled');
      } else {
        $(this).removeClass('aui-iconfont-star-filled').addClass('aui-iconfont-new-star');
      }
    });
  };
  var setDevRciRating = function (count) {
    var username = $requirement_user_name.attr('data-username');
    $dev_star_rating.each(function () {
      if ($(this).attr('rating') <= count) {
        $(this).removeClass('aui-icon aui-icon-small aui-iconfont-new-star').addClass('aui-icon aui-icon-small aui-iconfont-star-filled');
      } else {
        $(this).removeClass('aui-icon aui-icon-small aui-iconfont-star-filled').addClass('aui-icon aui-icon-small aui-iconfont-new-star');
      }
    });
    var json = "{'value':'" + count + "', 'user':'" + username + "'}";

    $dev_star_rating_input.each(function () {
      $(this).attr('value', json);
    });
  };

  function viewIssue(issue) {
    var num = '';
    var baseUrl = window.location.href.split("secure")[0];
    var finalUrl = baseUrl + 'browse/' + issue;
    window.open(finalUrl, '_blank');
  }

  function setRciRating() {
    var num = '';
    var baseUrl = window.location.href.split("secure")[0];
    var issue_Id = window.location.href.split("secure")[1];
    $.ajax({
      type: "GET",
      url: baseUrl + 'plugins/servlet/requirementissue',
      async: false,
      data: {
        view: 'rci',
        issueId: issue_Id
      },
      success: function (response) {
        num = response;
      }
    });
    return num;
  }




  function updateRCIRating() {
    var num = '';
    var baseUrl = window.location.href.split("secure")[0];
    var issue_Id = window.location.href.split("secure")[1];
    var username = $requirement_user_name.attr('data-username');
    $.ajax({
      type: "GET",
      url: baseUrl + 'plugins/servlet/requirementissue',
      async: false,
      data: {
        view: 'updateRCI',
        issueId: issue_Id,
        user_name: username
      },
      success: function (response) {
        num = response;
      }
    });
    return num;
  }

  $('#ai-rci-details').on('click', function (){
      AJS.dialog2('#ai-details-dialog').show();
//    $('#ai-details-dialog').attr('aria-hidden',false);
  });

  $qa_star_rating.on('click', function () {
    var rating = $(this).attr('rating');
    setQaRciRating(rating);
    $('#QA-RCI').attr('value', rating);
  });

  $dev_star_rating.on('click', function () {
    var rating = $(this).attr('rating');
    setDevRciRating(rating);
    $('#DEV-RCI').attr('value', rating);
  });

  $requirement_id.on('click', function () {
    var issueId = $(this).attr('issue-id');
    var editIssueView = viewIssue(issueId);
  });

  $my_requirement.on('click', function () {
    var num = "";
    var baseUrl = window.location.href.split("secure")[0];
    var username = $my_requirement.attr('data-username');
    $.ajax({
      type: "GET",
      url: baseUrl + 'plugins/servlet/requirementissue',
      async: false,
      data: {
        view: 'issueView',
        my_requirement: username
      },
      success: function (response) { num = response; }
    });
    document.getElementById("requirementTable").innerHTML = num;
    //  AJS.$("#tttt").innerHTML num;
  });



  $get_users_click.on('click', function () {
    getUsers();
  });

  function getUsers() {
    var baseUrl = window.location.href.split("secure")[0];
    var username = $get_user_name.attr('data-username');
    $.ajax({
      type: "GET",
      url: baseUrl + 'plugins/servlet/requirementusers',
      async: false,
      data: {
        view: 'getUsers',
        user_name: username
      },
      success: function (response) {
        num = response;
      }
    });
    document.getElementById("users").innerHTML = num;
  }

  $setIssuePriority.on('click', function () {
    var num = '';
    var baseUrl = window.location.href.split("secure")[0];
    var issue_Id = window.location.href.split("secure")[1];
    $.ajax({
      type: "GET",
      url: baseUrl + 'plugins/servlet/requirementusers',
      async: true,
      data: {
        view: 'getIssueStatus',
        issueId: issue_Id
      },
      success: function (response) { num = response; }
    });
    document.getElementById("requirementTable").innerHTML = num;
  });



  $advance_search.on('click', function () {
    if ($requirement_filter_panel.attr('is-collapsed') == "true") {
      $requirement_filter_panel.removeClass('collapse filter-panel').addClass('collapse in filter-panel');
      $requirement_filter_panel.attr("is-collapsed", "false");
    } else {
      $requirement_filter_panel.attr("is-collapsed", "true");
      $requirement_filter_panel.removeClass('collapse in filter-panel').addClass('collapse filter-panel');
    }
  });

  $edit_issue_submit.on('click', function () {
    updateRCIRating();

  });

  $('.testcase-parent .text').on('change', function () {
    var parent = $testcase_parent.select2('data');
    console.log(parent);
    $testcase_parent_value.attr('value', parent.id)
  });

  $filter_highiest.on('click', function () {
    if ($(this).attr('value') == "false") {
      $(this).css('background', '#de350b');
      $(this).attr('value', "true");
      $(this).removeClass('aui-lozenge-subtle');

    } else {
      $(this).attr('value', "false");
      $(this).css('background', "rgb(208, 208, 208)");
      $(this).addClass('aui-lozenge-subtle');

    }
  });

  $filter_high.on('click', function () {
    if ($(this).attr('value') == "false") {
      $(this).css('background', '#e40f0fcf');
      $(this).attr('value', "true");
      $(this).removeClass('aui-lozenge-subtle');
    } else {
      $(this).attr('value', "false");
      $(this).css('background', "rgb(208, 208, 208)");
      $(this).addClass('aui-lozenge-subtle');
    }
  });

  $filter_lowest.on('click', function () {
    if ($(this).attr('value') == "false") {
      $(this).css('background', '#00875a');
      $(this).attr('value', "true");
      $(this).removeClass('aui-lozenge-subtle');
    } else {
      $(this).attr('value', "false");
      $(this).css('background', "rgb(208, 208, 208)");
      $(this).addClass('aui-lozenge-subtle');
    }
  });

  $filter_low.on('click', function () {
    if ($(this).attr('value') == "false") {
      $(this).css('background', '#ffab00');
      $(this).attr('value', "true");
      $(this).removeClass('aui-lozenge-subtle');
    } else {
      $(this).attr('value', "false");
      $(this).css('background', "rgb(208, 208, 208)");
      $(this).addClass('aui-lozenge-subtle');
    }
  });

  $load_user.on('click', function () {
    getUsers();
  });

//  $( "div[field-id*='description']").mouseout(function() {
//    var baseUrl = window.location.href.split("secure")[0];
//  	var dataF  = $("#tinymce").textContent;
//  	console.log("yoooo");
//  	console.log(dataF);
//  	var rci = 1 ;
//  		 $.ajax({
//              type: 'POST',
//              url: baseUrl + 'plugins/servlet/accellohandler',
//              async: false,
//              data: {
//                 api: 'ai-rci',
//                 description : dataF
//              },
//              success: function (response) {
//                 rci = response ;
//              }
//          });
//
//    $(".rci .airci").each(function () {
//        if ($(this).attr('rating') <= rci) {
//          $(this).removeClass('aui-icon aui-icon-small aui-iconfont-new-star').addClass('aui-icon aui-icon-small aui-iconfont-star-filled');
//        } else {
//          $(this).removeClass('aui-icon aui-icon-small aui-iconfont-star-filled').addClass('aui-icon aui-icon-small aui-iconfont-new-star');
//        }
//      });
//  });

})(jQuery);
