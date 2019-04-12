AJS.toInit(function ($) {
  var $user_name = AJS.$('#header-details-user-fullname');
  var baseUrl = window.location.href.split("secure")[0];
  var username = $user_name.attr('data-username');
  var currentTab;
  var selectedFile;
  var selectedProject;
  var diag = $('<div id="myDialog"><div id="config-panel"><div id="second-div"><div id="regForm"> <div class="tab first-tab"><div id="tlform"><div class="tlpara"> <p>Project</p> <select class="tl-select project" id="tlprojects"></select></div><div class="tlpara"> <p>File Path:</p> <p><input id="uploadFile" class="tltextfield" placeholder="Select File" /><label for="file-upload" class="custom-file-upload"> Import File </label> <input id="file-upload" name="upload_cont_img" type="file" style="display:none;"></p></div></div> </div> <div class="tab"><h4 id="tlink-configurefieldh4">Configure Fields</h4><table class="aui" id="tlink-table"> <thead> <tr> <th id="jira-field" width="35%">AiTest Field</th> <th id="xml-column" width="30%">XML Column</th><th width="5%">Value In Attribute</th><th width="30%">Attributes</th> </tr> </thead> <tbody> </tbody> </table> </div> <div style="overflow:auto;" id="btn-panel"> <div style="float:right;"> <button class="tlbtn" type="button" id="prevBtn">Back</button> <button class="tlbtn" type="button" id="nextBtn">Next</button> </div> </div> </div></div></div></div>');
  var temporyDirectoryPath;
  diag.dialog({
    close: function (event, ui) {
      $.ajax({
        type: 'POST',
        url: baseUrl + 'plugins/servlet/test-link-data-management',
        data: {
          user: username,
          action: 'delete-file',
          data: JSON.stringify({
            'path': temporyDirectoryPath,
          }),
        },
        success: function (response) {
          location.reload();
        }
      });
    },
    position: {
      my: 'center',
      at: 'center',
      of: $("body"),
      within: $("body")
    },
    appendTo: "#jira",
    autoOpen: false,
    modal: true,
    height: 470,
    width: 700,
    title: '<span class="aui-icon aui-icon-small aui-iconfont-import">Insert meaningful text here for accessibility</span> Import',
    resizable: false,
    draggable: false,
    dialogClass: 'no-close success-dialog',
    buttons: {
      "X": {
        text: "X",
        id: "tlclose",
        click: function () {
          $(this).dialog("close");
        }
      }
    },
    create: function (event, ui) {
      $('.ui-dialog-buttonset').prependTo('.ui-dialog-titlebar');
    }
  }).parent().draggable({
    containment: '#jira'
  });

  AJS.$('#import-link').click(function (e) {
    e.preventDefault();
    currentTab = 0; // Current tab is set to be the first tab (0)
    showTab(currentTab);
    $('#myDialog').prev().last().children().last().attr('style', 'display:none');
    loadProjects();
    diag.dialog("open");

  });

  $("#file-upload").change(function () {
    if ($("#file-upload").length < 0) {
      return;
    }
    var file = $("#file-upload")[0].files[0];
    if (file.type == "text/xml") {
      document.getElementById("uploadFile").value = this.value;
      document.getElementById("nextBtn").disabled = false;
      getBase64(file).then(
        data => {
          selectedFile = data;
        }
      );
    } else {
      document.getElementById("nextBtn").disabled = true;
      AJS.flag({
        type: 'error',
        title: 'Invalid file type',
        close: 'auto'
      });
    }
  });

  const getBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = error => reject(error);
    });
  }


  $('#nextBtn').click((e) => {
    e.preventDefault();
    if (typeof selectedFile != 'undefined') {
      nextPrev(1);
    } else {
      AJS.flag({
        type: 'error',
        title: 'File Not Selected',
        close: 'auto'
      });
    }

  });
  $('#prevBtn').click((e) => {
    e.preventDefault();
    nextPrev(-1);
  });

  function showTab(n) {
    var x = document.getElementsByClassName("tab");
    x[n].style.display = "block";
    if (n == 0) {
      document.getElementById("prevBtn").style.display = "none";
    } else {
      document.getElementById("prevBtn").style.display = "inline";
    }
    if (n == (x.length - 1)) {
      // document.getElementById("nextBtn").innerHTML = "OK";
      // document.getElementById("prevBtn").style.display = "none";
      document.getElementById("nextBtn").innerHTML = "Start Import";
    } else if (n == 0) {
      document.getElementById("nextBtn").innerHTML = "Next";
    }
    // else {
    //   document.getElementById("nextBtn").innerHTML = "Start Import";
    // }
  }

  function nextPrev(n) {
    var x = document.getElementsByClassName("tab");
    x[currentTab].style.display = "none";
    currentTab = currentTab + n;
    if (currentTab == 1) {
      var path = $('#uploadFile')[0].value;
      selectedProject = $('#tlprojects option:selected').attr('id');
      loadXML(path, selectedProject);
    }
    if (currentTab == 2) {
      $.when(readTable()).done(function (info) {
        $.blockUI({
          message: 'Importing Data...',
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
          url: baseUrl + 'plugins/servlet/test-link-data-management',
          async: true,
          data: {
            user: username,
            action: 'import-xml-data',
            data: JSON.stringify({
              'map': info.map,
              'manual': info.execution[0],
              'automated': info.execution[1],
              'path': temporyDirectoryPath,
              'project': selectedProject
            })
          },
          success: function (response) {
            var isPassed = JSON.parse(response);
            if (isPassed) {
              AJS.flag({
                type: 'success',
                title: 'Successfully Imported XML Data',
                body: '',
                close: 'auto'
              });
            } else {
              AJS.flag({
                type: 'error',
                title: 'XML Data Import Failed',
                body: '',
                close: 'auto'
              });
            }
            $.unblockUI();
            if (currentTab >= x.length) {
              diag.dialog("close");
              return false;
            }
            //  showTab(currentTab);
          }, error: function (jqXHR, textStatus, errorThrown) {
            $.unblockUI();
            // AJS.flag({
            //   type: 'error',
            //   title: 'An Error Occurred Importing XML',
            //   body: 'Contact Developer',
            //   close: 'auto'
            // });
            if (currentTab >= x.length) {
              diag.dialog("close");
              return false;
            }
          }
        });


      });
    }
    if (currentTab <= 2) {
      showTab(currentTab);
    }

    // if (currentTab >= x.length) {
    //   diag.dialog("close");
    //   return false;
    // }
  }

  const loadXML = (path, selectedProject) => {

    $.ajax({
      type: 'POST',
      url: baseUrl + 'plugins/servlet/test-link-data-management',
      data: {
        user: username,
        action: 'upload-xml-file',
        data: JSON.stringify({
          'project': selectedProject,
          'path': path,
          'file': selectedFile
        }),
      },
      success: function (response) {
        var resp = JSON.parse(response);
        appendToTable(resp);

      }, error: function (jqXHR, textStatus, errorThrown) {
        AJS.flag({
          type: 'error',
          title: 'An Error Occurred Importing XML',
          body: 'Contact Developer',
          close: 'auto'
        });
      }
    });
  }
  const loadProjects = () => {
    $.ajax({
      type: 'POST',
      url: baseUrl + 'plugins/servlet/test-link-data-management',
      async: false,
      data: {
        user: username,
        action: 'get-all-projects',
        data: {}
      },
      success: function (response) {
        var respObj = JSON.parse(response);
        var projectSelector = $('#tlprojects');
        projectSelector.empty();
        if (respObj.length > 0 && respObj != null) {
          respObj.forEach(element => {
            $.each(element, function (key, value) {
              var option = $('<option>', {
                'id': key,
                'text': value
              });
              projectSelector.append(option);
            });
          });
        }
      }, error: function (jqXHR, textStatus, errorThrown) {
        AJS.flag({
          type: 'error',
          title: 'An Error Occurred Importing XML',
          body: 'Contact Developer',
          close: 'auto'
        });
      }
    });
  }

  const appendToTable = (data) => {

    var xmlNodes = data.xmlColumnList;
    var aiTestFields = data.aiTestFieldList;
    var attributesList = data.attributeList;
    temporyDirectoryPath = data.path;
    $('#tlink-table tbody tr').remove();
    var tlinkTable = $('#tlink-table');
    var toggleSpan;
    aiTestFields.forEach((field) => {
      var TR = $('<tr>');
      if (field == 'TestCase Execution Type') {
        toggleSpan = $('<span>', {
          'style': 'float:left;padding-left: 5px;'
        }).addClass('aui-icon aui-icon-small aui-iconfont-arrow-down');
        var AiTD = $('<td>', {
          'text': field
        });
        AiTD.append(toggleSpan);
      } else {
        var AiTD = $('<td>', {
          'text': field
        });
      }
      var xmlTD = $('<td>');
      var isAttributeTD = $('<td>');
      var attributesTD = $('<td>');
      if (field == 'TestCase Execution Type') {
        var xmlNodeSelector = $('<select>').addClass('executiontype-selector');
      } else {
        var xmlNodeSelector = $('<select>');
      }
      var isAttributeChkBox = $('<input type="checkbox" style="margin-left: 16px; margin-top: 11px;">').addClass("isAttributeChkBox");
      var attributesSelector = $('<select>', {
        'style': 'display:none;'
      });
      xmlNodes.forEach((node) => {
        var xmlOption = $('<option>', {
          'value': node,
          'text': node
        });
        xmlNodeSelector.append(xmlOption);
      });
      xmlTD.append(xmlNodeSelector);
      isAttributeTD.append(isAttributeChkBox);
      attributesTD.append(attributesSelector);
      TR.append(AiTD);
      TR.append(xmlTD);
      TR.append(isAttributeTD);
      TR.append(attributesTD);
      tlinkTable.append(TR);
      if (field == 'TestCase Execution Type') {
        var ManualTR = $('<tr>').addClass('ai-expand manualtl')
        var spanManual = $('<span>', {
          'style': 'float:left;padding-left: 5px;'
        }).addClass('aui-icon aui-icon-small aui-iconfont-collapsed');
        var ManualTD = $('<td>', {
          'text': 'Manual',
          'style': 'padding-left:60px;border: none;'
        }).addClass('expecttype-text');
        ManualTD.append(spanManual);
        var ManualValueTD = $('<td>', {
          'colspan': '3'
        });
        var xmlManualInput = $('<input type="text" list="manuel-execution-option" value="Default">').addClass('tlet-textbox');
        var xmlManualInputDataList = $('<datalist id="manuel-execution-option">');
        var option = $('<option>', {
          'text': 'Default'
        });
        xmlManualInputDataList.append(option);
        ManualValueTD.append(xmlManualInput);
        ManualValueTD.append(xmlManualInputDataList);
        ManualTR.append(ManualTD);
        ManualTR.append(ManualValueTD);
        tlinkTable.append(ManualTR);
        ManualTR.slideToggle(100);
        var AutomationTR = $('<tr>').addClass('ai-expand automationtl');
        var spanAutomation = $('<span>', {
          'style': 'float:left;padding-left: 5px;'
        }).addClass('aui-icon aui-icon-small aui-iconfont-collapsed');
        var AutomationTD = $('<td>', {
          'text': 'Automated',
          'style': 'padding-left:60px;'
        }).addClass('expecttype-text');
        AutomationTD.append(spanAutomation);
        var AutomationValueTD = $('<td>', {
          'colspan': '3'
        });
        var xmlAutomationInput = $('<input type="text" list="automation-execution-option" value="Default">').addClass('tlet-textbox');
        var xmlAutomationInputDataList = $('<datalist id="automation-execution-option">');
        var optionA = $('<option>', {
          'text': 'Default'
        });
        xmlAutomationInputDataList.append(optionA);
        AutomationValueTD.append(xmlAutomationInput);
        AutomationValueTD.append(xmlAutomationInputDataList);
        AutomationTR.append(AutomationTD);
        AutomationTR.append(AutomationValueTD);
        tlinkTable.append(AutomationTR);
        AutomationTR.slideToggle(100);
      }
    });
    $('.isAttributeChkBox').change((e) => {
      if (e.currentTarget.checked) {
        appendToAttributeSelector($(e.currentTarget).closest('td').next('td').find('select'), attributesList)
        $(e.currentTarget).closest('td').next('td').find('select').show();
      } else {
        $(e.currentTarget).closest('td').next('td').find('select').hide();
      }
    });
    toggleSpan.click((e) => {
      var currentRow = $(e.currentTarget).parent().parent();
      var rows = currentRow.closest('tr');
      var manualTR = rows.next();
      manualTR.slideToggle(100);
      var automationTR = manualTR.next();
      automationTR.slideToggle(100);
    })

  }

  const appendToAttributeSelector = (element, attributes) => {
    element.empty();
    var option;
    attributes.forEach((attribute) => {
      option = $('<option>', {
        'value': attribute,
        'text': attribute
      });
      element.append(option);
    });
  }

  const readTable = () => {
    document.getElementById("prevBtn").style.display = "none";
    document.getElementById("nextBtn").style.display = "none";
    var executionArray = new Array();
    var mappingArray = new Array();
    var table = $('#tlink-table>tbody')[0];

    for (var i = 0; i < table.rows.length; i++) {
      var mapObject = {
        'aiTestField': '',
        'xmlColumn': '',
        'attributes': ''
      };
      if (table.rows[i].cells.length == 4) {

        for (var j = 0; j < table.rows[i].cells.length; j++) {
          // console.log(table.rows[i].cells);
          // if (j == 0) {
          var aiField = table.rows[i].cells[0].innerText;
          mapObject.aiTestField = aiField;
          // }
          //  else if (j == 1) {
          mapObject.xmlColumn = table.rows[i].cells[1].getElementsByTagName('select')[0].value;
          // var mapObject = {
          //   'aiTestField': aiField,
          //   'xmlColumn': table.rows[i].cells[j].getElementsByTagName('select')[0].value,
          // }
          //  } else if (j == 3) {
          if (table.rows[i].cells[3].getElementsByTagName('select')[0]) {
            mapObject.attributes = table.rows[i].cells[3].getElementsByTagName('select')[0].value;
            //  }
          }

        }
        mappingArray.push(mapObject);
      } else {
        for (var j = 0; j < table.rows[i].cells.length; j++) {
          if (j == 1) {
            var execution = table.rows[i].cells[1].getElementsByTagName('input')[0].value;
            executionArray.push(execution);
          }
        }
      }

    }
    return mapObject = {
      'map': mappingArray,
      'execution': executionArray
    };
  }


})(jQuery);
