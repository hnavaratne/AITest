AJS.toInit(function () {
    // AJS.$('#virtusa-site').click(function (e) {

    //     issueForm = JIRA.Forms.createCreateIssueForm();
    //     issueForm.asDialog().show();
    //     console.log("yoooooooo");
    //     e.preventDefault();

    // });

    // // Shows the dialog when the "Show dialog" button is clicked
    // AJS.$("#dialog-show-button").click(function (e) {
    //     e.preventDefault();
    //     AJS.dialog2("#demo-dialog").show();
    // });

    // // Hides the dialog
    // AJS.$("#dialog-submit-button").click(function (e) {
    //     e.preventDefault();
    //     AJS.dialog2("#demo-dialog").hide();
    // });
    // check();
    function check() {
        if (window.location.href.indexOf("OscarIntegrationView.jspa") > -1) {
            $('body').css({ "overflow-y": "hidden" });
        }
    }

    // $(document).live('click', '.issue-link-key', function () {
    //     if (!($('#type-val').text().trim() == "Story")) {
    //         AJS.$('#linkingmodule').css('display', 'none');
    //     } else {
    //         $('#linkingmodule_heading > h4').text("Linked TestCases");
    //     }
    //     $('#content > div.navigator-container > div.navigator-body > div > div > div > div > div > div > div > div.aui-item.detail-panel').load(self)
    // });
   
})(jQuery);
