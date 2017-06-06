Ext.define("Permian.view.MapControls", {
    extend: 'Ext.Panel',
    alias: 'widget.mapcontrols',
    cls: "mapControls",
    itemId: "mapControls",
    items: [
        {
            itemId: "zoomSuggestion",
            html: '<div id="zoomSuggestion">Double click the map to zoom in 9 times.</div>'
        },
        {
            itemId: "introInstructions",
            html:'<p><ol>' +
                '        <li>Zoom in and explore the map to find regions to monitor.  When you\'re zoomed in enough.  Available regions will appear.</li>' +
                '        <li>Alt click the regions you want to monitor and click <b>Add Selected Regions</b>.<br/><span class="subtext">(There are around 28 million regions to choose from).</span></li>' +
                '        <li>Enter the token you received from our <a target="_blank" href="http://www.kickstarter.com/projects/deford/88845179/">Kickstarter</a> project below to confirm your regions.</li>' +
                '      </ol></p>'
        },
        {
            itemId: "inputTokenStuff",
            xtype: "textfield",
            labelWidth: 200,
            fieldLabel: "Kickstarter backer token"
        },
        {
            xtype: "button",
            itemId: "submitTokenButton",
            text: "Submit Token",
            handler: function(button, options) {
                Permian.getApplication.fireEvent("submitToken");
            }
        },
        {
            itemId: "commitInstructions",
            hideMode: "offsets",
            xtype: "box",
            html: ""
        },
        {
            itemId: "hiddenInstructions",
            hidden: true,
            html: "<p>To add regions zoom in on the right until you see the region outlines.  To select a region click it while holding the <b>alt key</b> down. <br/>" +
                "Then click <b>Add Selected Regions</b> to indicate you are willing to be a monitor for those regions." +
                "</p>"
        },
        {
            xtype: "button",
            itemId: 'addFieldsButton',
            text: 'Add Selected Regions'
        },
        {
            itemId: "sliceContainer",
            cls: "sliceContainer",
            xtype: "container"
        },
        {
            xtype: "button",
            itemId: "commitSlicesButton",
            text: "Confirm Added Regions"
        }
    ]
});