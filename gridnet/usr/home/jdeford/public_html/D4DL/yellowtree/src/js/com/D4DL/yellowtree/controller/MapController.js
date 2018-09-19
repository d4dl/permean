dojo.require("dijit._Widget");
dojo.provide("com.D4DL.yellowtree.controller.MapController");

//dojo.declare( "com.D4DL.yellowtree.controller.MapController", dijit.Tree,
dojo.declare( "com.D4DL.yellowtree.controller.MapController", dijit._Widget,
    {
        expandedIds: [],
        interfaces: {},
        currentInterface: null,
        tree: null,
        mapBounds: null,
        postCreate: function(){
            this.inherited(arguments);
            console.log("Connectiong " + this.tree);
//            dojo.connect(this.tree, "_collapseNode", this, this.onCollapse);
//            dojo.connect(this.tree, "_expandNode", this, this.onExpand);
        },
        addInterface: function(name, mapInterface) {
        	console.log("Adding interface " + name);
            this.interfaces[name]=mapInterface;
            if(mapInterface.map) {
                google.maps.event.addListener(mapInterface.map, "zoom_changed", dojo.hitch(this, function() {
                    this.mapZoomed(mapInterface.map);
                }));

            }
        },
        setActiveInterface: function(name, mapInterface) {
        	console.log("Active interface " + name);
            this.currentInterface = this.interfaces[name];
        },
        mapZoomed: function(map) {
            //if(checkBox.selected) {
                // get some data, convert to JSON
            if(map.getZoom() >= 8) {
                var bounds = map.getLatLngBounds();
                var params = {
                    top:    bounds.getNorth(),
                    bottom: bounds.getSouth(),
                    west:   bounds.getWest(),
                    east:   bounds.getEast(),
                    sliceSize: 7200
                }
                xhr.get({
                    url:"http://www.gridocracy4.net/php/services/sliceService.php",
                    handleAs:"json",
                    content: params,
                    load: function(data){
                        this.polygonsReceived(data);
                    }
                });
            }
//            } else {
//                this._mapStateProxy.shouldShowFields =  false;
//                sendNotification(MainFacade.REMOVE_GEO_QUADRANGLES);
//            }
        },


        /**
         * Fires a notification with all new polygons that were updated.  The new ones
         * are sent as a payload. If all of the polygons are needed use getPolygonOverlays
         */
        polygonsReceived:function (polygons, map) {
            if (this._mapStateProxy == null) {
                this._mapStateProxy = MapStateProxy(facade.retrieveProxy(MapStateProxy.NAME));
            }
            var polygonResults = ArrayUtil.toArray(event.result);
            var polygons = [];
            var factory = new SmartClassFactory(GeoPolygon);
            var polygonIds = DictionaryUtil.getKeys(_polygonOverlayMap);
            for(var polygonProperties in polygonResults) {
                if (polygonIds.indexOf(polygonProperties.id) < 0) {
                    factory.properties = polygonProperties;
                    var polygon = GeoPolygon(factory.newInstance());
                    //trace(polygon);
                    var field = new Field(this._mapStateProxy.showBases, polygon);
                    var overlay = new PolygonOverlay(polygon, field);
                    facade.registerMediator(new FieldMediator(createMediatorName(overlay.field.polygon.id), overlay.field));
                    this._polygonOverlayMap[polygonProperties.id] = overlay;
                    polygons.push(overlay);
                }
            }
            sendNotification(MainFacade.GEO_POLYGONS_UPDATED, polygons);
        },


        onCollapse: function(node) {
           console.log("collapse");
           this.currentInterface.clearOverlays();
           this.updateSpots(node);
        },
        onExpand: function(node, recursive) {
           console.log("expand");
           this.currentInterface.clearOverlays();
           this.updateSpots(node);
        },
        updateSpots: function(node) {
           this.inherited(arguments);
           item = node.item;
           //if(this.expandedIds.indexOf(item.id) < 0) {
               //this.expandedIds.push(item.id);
               //this.currentInterface.clearOverlays();
               this.mapBounds = this.addMarkersWithLines(node.item, node.item.children, 1, null);
               this.currentInterface.panToBounds(item.latitude, item.longitude, this.mapBounds);
               var allItems = node.item.children.slice(0);
console.log("Root item length: " + allItems.length);
               allItems.push(node.item);
               this.requestPolygons(allItems);
               //console.log("tree expand successfully intercepted " + item.id);
               //children = this.tree.model.getChildren(item);
           //}
        },
        addMarkersWithLines: function(centerItem, items, depth, latLngBounds) {
            console.log("Adding items at depth " + depth);
            latLngBounds = this.addToBounds(centerItem, latLngBounds);
            if(centerItem && items) {
                this.currentInterface.addMarker(centerItem, depth-1);
                var itemCount = items.length;
                for (var index=0; index < itemCount; index++) {
                    var item = items[index];
                    latLngBounds = this.addToBounds(item, latLngBounds);
                    var children = item.children;
                    if(children) {
                        this.addMarkersWithLines(item, children, depth+1, latLngBounds);
                    }
                    var realDepth = depth; 
                    this.currentInterface.lineTo(centerItem, item, depth);
                    this.currentInterface.addMarker(item, depth);
                }
            }
            return latLngBounds;
        },
        addToBounds: function(item, bounds) {
          if(bounds == null) {
              //Only good for the US proper
              //bounds = new google.maps.LatLngBounds(new google.maps.LatLng(89.99999, 0.9999), new google.maps.LatLng(-89.99999, -179.9999));
              //bounds = new google.maps.LatLngBounds(new google.maps.LatLng(89.9999,0), new google.maps.LatLng(-89.9999,0));
              bounds = new google.maps.LatLngBounds(new google.maps.LatLng(90,180), new google.maps.LatLng(-90,180));
          }
          bounds.extend(new google.maps.LatLng(item.latitude, item.longitude));
          return bounds;
        },
        requestPolygons: function(items) {
           var content = [];
           for(var i=0; i < items.length; i++) {
               content["ids["+i+"]"] = item.id;
           }
           var mapInterface = this.currentInterface;
           var xhrArgs = {
               url: "http://D4DL.com/yellowtree/src/php/controller/PolygonQueryService.php",
               handleAs: "json",
               load: function(data) {mapInterface.createPolygons(data);},
               content: content,
               error: function(error) {
                   alert(error);
               }
           };
           var deferred = dojo.xhrGet(xhrArgs);
        }
    }
);

