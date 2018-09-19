Ext.define('Permian.controller.MapController', {
    extend:'Ext.app.Controller',
    stores: [
        "GeoStore"
    ],
    backerToken: null,
    allotmentCount: null,
    allPolygon:null,
    selectedPolygonOverlays:null,
    highlightedPolygonOverlay:null,
    addedPolygonOverlays:null,
    lastBoundsChanged:null,
    refs: [{
        ref: 'map',
        selector: '#gmapPanel'
    }],
    expandedIds:[],
    tree:null,
    mapBounds:null,

    tileTemplate: new Ext.Template("<div style='font-size: 0px;' class='sliceTile'><img style='font-size: 0px; height: 64px; width: 64px;'src='http://www.gridocracy17.net/php/services/location/googlemapsproxy.php?{queryParams}'/></div>"),

    onLaunch:function () {
        this.callParent(arguments);
        var mapInterface = this.getMap();

        var self = this;
        if (mapInterface.gmap) {
            this.map = mapInterface.gmap;
            google.maps.event.addListener(mapInterface.gmap, "bounds_changed", function () {
                setTimeout(function () {
                    self.boundsChanged(mapInterface.gmap)
                }, 100);
            });

        }
        console.log("Active interface " + name);

        var commitInstructions = Ext.ComponentQuery.query("#commitInstructions")[0];
        var commitSlicesButton = Ext.ComponentQuery.query("#commitSlicesButton")[0];
        commitSlicesButton.hide();
        commitInstructions.hide();


        this.lastBoundsChanged = new Date();
        //These also should be in a store.
        this.allPolygonOverlays = {};
        this.selectedPolygonOverlays = {};
        this.addedPolygonOverlays = {};

        Ext.ComponentQuery.query('#addFieldsButton')[0].on({click: this.addSelectedFields, scope: this});
        Ext.ComponentQuery.query('#submitTokenButton')[0].on({click: this.submitToken, scope: this});
        var commitSlicesButton = Ext.ComponentQuery.query('#commitSlicesButton')[0];
        commitSlicesButton.on({click: this.commitSlices, scope: this});

        //Ext.ComponentQuery.query("#commitInstructions")[0].fadeOut({duration:1});
       // Ext.fly(commitSlicesButton).fadeOut({duration:1});
        var self = this;
        window.onkeydown = function (evt) {
            self.onKeyDown(evt);
        }
        window.onkeyup = function (evt) {
            self.onKeyUp(evt);
        }

        this.getGeoStoreStore().on({
            load: this.polygonsReceived,
            scope: this
        });
        this.getApplication().on({
            submitToken: this.submitToken,
            scope: this
        })
    },
    onKeyUp:function (event) {
        Permian.controller.MapController.altKeyDown = false;
    },
    onKeyDown:function (event) {
        Permian.controller.MapController.altKeyDown = event.altKey;
    },

    boundsChanged:function (map) {
        if (new Date().getTime() - this.lastBoundsChanged.getTime() > 200) {
            this.lastBoundsChanged = new Date();//These events fire really fast sometimes.
            //if(checkBox.selected) {
            // get some data, convert to JSON
            var zoomTarget = 12;
            var currentZoom = map.getZoom();
            if (currentZoom > zoomTarget) {
                Ext.fly("zoomSuggestion").setHTML("Hold down the <b>alt key</b> while clicking a region to choose it.  " +
                    "Click <b>'Add Selected Regions'</b> to assign yourself as a monitor to those regions.");
                var bounds = map.getBounds();
                var params = {
                    bottom:Math.max(bounds.getNorthEast().lat(), bounds.getSouthWest().lat()),
                    top:Math.min(bounds.getNorthEast().lat(), bounds.getSouthWest().lat()),
                    east:Math.min(bounds.getSouthWest().lng(), bounds.getNorthEast().lng()),
                    west:Math.max(bounds.getSouthWest().lng(), bounds.getNorthEast().lng()),
                    projection:"vertices"
                }
                this.getGeoStoreStore().load({params: params});
            } else {
                var zoomDiff = (zoomTarget - currentZoom) + 1;
                Ext.fly("zoomSuggestion").setHTML("Double click the map to zoom in  " + zoomDiff + " " + (zoomDiff > 8 ? "" : "more") + " time" + (zoomDiff > 1 ? "s" : "") + ".");

            }

        }
    },

    updateUIForAllotment:function (data) {
        this.allotmentCount = data.allotmentCount;

        var mapControls = Ext.ComponentQuery.query("#mapControls")[0].getEl();
        var self = this;
        mapControls.fadeOut({
            opacity:.01, //can be any value between 0 and 1 (e.g. .5)
            easing: 'easeOut',
            duration: 500,
            remove: false,
            callback: function() {
                var introInstructions = Ext.ComponentQuery.query("#introInstructions")[0];
                if (introInstructions) {
                    Ext.destroy(introInstructions);
                }
                var submitTokenButton = Ext.ComponentQuery.query("#submitTokenButton")[0];
                if (submitTokenButton) {
                    submitTokenButton.destroy();
                }
                var inputTokenStuff = Ext.ComponentQuery.query("#inputTokenStuff")[0];
                if (inputTokenStuff) {
                    Ext.destroy(inputTokenStuff);
                }
                var commitInstructions = Ext.ComponentQuery.query("#commitInstructions")[0];
                var hiddenInstructions = Ext.ComponentQuery.query("#hiddenInstructions")[0];
                //Ext.fly(commitInstructions).setStyle("display", "block");
                var newInstructions = hiddenInstructions.getEl().dom.innerHTML.replace("{sliceCount}", data.allotmentCount);
                commitInstructions.getEl().setHTML(newInstructions);
                var submitTokenButton = Ext.ComponentQuery.query("#submitTokenButton")[0];
                var commitSlicesButton = Ext.ComponentQuery.query("#commitSlicesButton")[0];

                commitInstructions.show();
                commitSlicesButton.show();
                mapControls.fadeIn({
                    opacity: 1, //can be any value between 0 and 1 (e.g. .5)
                    easing: 'easeIn',
                    duration: 500
                });
            }
        });

    },


    submitToken:function () {
        var tokenInput = Ext.ComponentQuery.query("#inputTokenStuff")[0].value;
        if(!tokenInput) {
            this.showDialog("You need to enter a backer token.  You can get a token from the <a target='_blank' href='http://www.kickstarter.com/projects/deford/88845179/'>Kickstarter</a> project.")
        } else {
            this.backerToken = tokenInput;
            var self = this;
            var params = {
                action:'submitToken',
                token:tokenInput
            }

            Ext.Ajax.request({
                url:"http://www.gridocracy5.net/php/services/sliceService.php",
                params:params,
                success:function (response, ioargs) {
                    var data = Ext.JSON.decode(response.responseText);
                    self.showDialog(data.message);
                    if (data.allotmentCount !== undefined) {
                        self.updateUIForAllotment(data);
                    }
                    if (data.slices) {
                        self.polygonsReceived(data.slices);

                        for (var i = 0; i < data.slices.length; i++) {
                            var slice = data.slices[i];
                            self.fieldSelected(self.allPolygonOverlays[slice.uid], true)
                        }
                        self.addSelectedFields(true);
                    }
                },
                failure:function (error, ioargs) {
                    alert("An unexpected error occurred: " + error);
                }
            });
        }
    },

    commitSlices:function () {
        var self = this;
        var slices = self.getAddedSlices();
        var params = {
            action:'commitSlices',
            token: self.backerToken,
            slices:JSON.stringify(slices)
        }
        if (slices.length > self.allotmentCount) {
            this.showDialog("You may choose to be the monitor for up to " + self.allotmentCount +
                " regions but you have added " + slices.length + ".  Deselect some regions and click the <b>Add Selected Regions</b> button.  Then try to reconfirm.");
        } else if (slices.length < self.allotmentCount) {
            var phrase = slices.length ? ("you have only added " + slices.length) : "you haven't added any";
            this.showDialog("You may choose to be the monitor for up to " + self.allotmentCount +
                " regions but " + phrase + ".  Choose some more regions and click the <b>Add Selected Regions</b> button.  Then try to reconfirm.");
        } else {
            //This should be handled by the polygon store (which does not yet exist).
            Ext.Ajax.request({
                url:"http://www.gridocracy6.net/php/services/sliceService.php",
                params:params,
                success:function (response) {
                    var data = Ext.JSON.decode(response.responseText);
                    self.showDialog(data.message);
                    if (data.allotmentCount !== undefined) {
                        self.updateUIForAllotment(data);
                    }
                    if (data.slices) {
                        self.polygonsReceived(data.slices);
                    }
                },
                failure:function (error) {
                    alert("An unexpected error occurred: " + error);
                }
            });
        }
    },


    getAddedSlices:function () {
        var slices = [];
        for (var key in this.addedPolygonOverlays) {
            var polygonOverlay = this.addedPolygonOverlays[key];
            if (!polygonOverlay.polygon.backerToken) {
                slices.push(polygonOverlay.polygon.raw);
            }
        }
        return slices;
    },


    addSelectedFields:function (skipSelectionCheck) {
        //   debugger;
        var selectionsExist = false;
        var sliceContainer = Ext.ComponentQuery.query("#sliceContainer")[0];
        Ext.select(".sliceTile").each(function(tile){tile.destroy()});
        //sliceContainer.removeChildEls(function(){return true;});
        this.addedPolygonOverlays = {};
        var self = this;
        for (var key in this.selectedPolygonOverlays) {
            var polygonOverlay = this.selectedPolygonOverlays[key];
            this.addedPolygonOverlays[key] = polygonOverlay;
            selectionsExist = true;
            var queryParams = Ext.Object.toQueryString(polygonOverlay.getImageParams());
            var newTile = self.tileTemplate.append(sliceContainer.getEl(), {queryParams: queryParams}, true);
            newTile.on("dblclick", function(event, element, options) {
                self.selectSlice(options.polygonOverlay)
            }, this, {polygonOverlay: polygonOverlay});
        }
        if (!selectionsExist && skipSelectionCheck !== true) {
            self.showDialog("You haven't selected any regions.  Double click to zoom in until you see the regions outlined.  Hold down the ALT key and click to select a region.");
        } else {
            var inputTokenStuff = Ext.ComponentQuery.query("#inputTokenStuff");
            if(inputTokenStuff.length > 0) {
                inputTokenStuff[0].getEl().highlight();
            }

        }
    },

    selectSlice:function (polygonOverlay) {
        this.map.fitBounds(polygonOverlay.bounds);
        this.highlightedPolygonOverlay = polygonOverlay;
    },

    showDialog:function (message) {
        if (message) {
            var myDialog = Ext.Msg.show({
                title:"Message",
                buttons: Ext.Msg.OK,
                style:"width: 300px",
                msg: message
            });
        }
    },

    fieldSelected:function (polygonOverlay, select) {
        if (select) {
            this.selectedPolygonOverlays[polygonOverlay.polygon.get('uid')] = polygonOverlay;
        } else {
            delete this.selectedPolygonOverlays[polygonOverlay.polygon.get('uid')];
        }
    },

    /**
     * Fires a notification with all new polygons that were updated.  The new ones
     * are sent as a payload. If all of the polygons are needed use getPolygonOverlays
     */
    polygonsReceived:function () {
        var self = this;
        this.getGeoStoreStore().each(function(polygon, i, totalCount)
        {
            //Don't add polygons that already have been added.
            var uid = polygon.get('uid');
            var polygonOverlay = this.allPolygonOverlays[uid];
            if (polygonOverlay && polygon.backerToken) {//If one comes through with a backerToken get rid of any current ones.
                polygonOverlay.DESTROYED = true;
                delete this.selectedPolygonOverlays[polygonOverlay.polygon.get('uid')];
                polygonOverlay.destroy();
                polygonOverlay = null;
            }
            if (!polygonOverlay) {
                var polygon = polygon;
                polygonOverlay = Ext.create("Permian.view.PolygonOverlay",{
                    polygon: polygon,
                    map: self.map,
                    onselect: Ext.bind(this.fieldSelected, this),
                    currentBackerToken: self.backerToken
                });
                this.allPolygonOverlays[uid] = polygonOverlay;
            }
        }, this)

    }
});
