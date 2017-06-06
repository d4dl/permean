Ext.define('Permian.view.Main', {
    extend:'Ext.container.Container',
    requires:[
        'Ext.tab.Panel',
        'Ext.ux.GMapPanel',
        'Ext.layout.container.Border'
    ],
    xtype:'app-main',
    initComponent:function () {
        var center = new google.maps.LatLng(27.146, -32.021);
        Ext.applyIf(this, {
            items:[
                {
                    region:'north',
                    height:180,
                    html:'<table class="headerTable">' +
                        '<tbody>' +
                        '<tr>' +
                        '  <td class="logoCell">' +
                        '  </td>' +
                        '  <td class="titleCell">' +
                        '    <ul><li class="one">Software development</li><li class="two">Social development</li><li class="three">Logistics</li><li class="shared">Shared</li></ul>' +
                        '  </td>' +
                        '  <td class="space"></td>' +
                        '  <td class="descriptionCell">' +
                        '    <div class="descriptionCellContent">' +
                        '    </div>' +
                        '  </td>' +
                        '</tr>' +
                        '<tr>' +
                        '  <td colspan="4" width="100%">' +
                        '    <div class="sharePanel" style="width: 100%; text-align: right;">' +
                        '      <span class="st_fblike_large" displaytext="Facebook Like" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;position:relative;margin:3px 3px 0;padding:0px;font-size:11px;line-height:0px;vertical-align:bottom;overflow:visible;bottom:7px;margin-top:9px;"><div data-action="" data-send="false" data-layout="button_count" data-show-faces="false" class="fb-like fb_edge_widget_with_comment fb_iframe_widget" data-href="http://d4dl.com/" fb-xfbml-state="rendered">' +
                        '      <span style="height: 20px;">' +
                        '      <iframe id="fb86884c4" name="f3dcb379ec" scrolling="no" title="Like this content on Facebook." class="fb_ltr" src="http://www.facebook.com/plugins/like.php?api_key=&amp;channel_url=http%3A%2F%2Fstatic.ak.facebook.com%2Fconnect%2Fxd_arbiter.php%3Fversion%3D27%23cb%3Dfe1e2404c%26domain%3Dd4dl.com%26origin%3Dhttp%253A%252F%252Fd4dl.com%252Ff2fd7b8318%26relation%3Dparent.parent&amp;colorscheme=light&amp;extended_social_context=false&amp;href=http%3A%2F%2Fd4dl.com%2F&amp;layout=button_count&amp;locale=en_US&amp;node_type=link&amp;sdk=joey&amp;send=false&amp;show_faces=false&amp;width=90" style="border: none; overflow: hidden; height: 20px; width: 73px;"></iframe></span></div></span></span>' +
                        '      <span class="st_facebook_large" displaytext="Facebook" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/facebook_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <span class="st_sharethis_large" displaytext="ShareThis" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/sharethis_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <span class="st_twitter_large" displaytext="Tweet" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/twitter_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <span class="st_linkedin_large" displaytext="LinkedIn" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/linkedin_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <span class="st_pinterest_large" displaytext="Pinterest" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/pinterest_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <span class="st_email_large" displaytext="Email" st_processed="yes"><span style="text-decoration:none;color:#000000;display:inline-block;cursor:pointer;" class="stButton"><span class="stLarge" style="background-image: url(http://w.sharethis.com/images/email_32.png);"></span><img src="http://w.sharethis.com/images/check-big.png" style="position: absolute; top: -7px; right: -7px; width: 19px; height: 19px; max-width: 19px; max-height: 19px; display: none;"></span></span>' +
                        '      <br><span class="shareInstructions">This works because you share.  Share this.</span><br>' +
                        '    </div>' +
                        '  </td>' +
                        '</tr> ' +
                        '</tbody></table>'
                },
                {
                    region:'west',
                    xtype:'mapcontrols',
                    title:'Choose the regions of the planet you\'d like to monitor',
                    width:450
                },
                {
                    region:'center',
                    xtype:'panel',
                    layout: 'fit',
                    items:[
                        {
                            xtype:'gmappanel',
                            itemId:"gmapPanel",
                            mapOptions:{
                                zoom:3,
                                //zoom:11,
                                center:center,
                                control:"Small",
                                scrollwheel:true,
                                mapTypeId:google.maps.MapTypeId.ROADMAP
                            }
                        }
                    ]
                }
            ]
        });

        this.callParent();
    },
    layout:{
        type:'border'
    }
})
;