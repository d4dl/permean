Ext.define("Permian.view.PolygonOverlay", {
    extend: 'Ext.Component',

    selected: null,
    polygon: null,
    polyPath: null,
    mapPolygonOptions: null,
    strokeColor: null,
    mapPolygon: null,

    constructor: function(config) {
        var map = config.map;
        var onSelect = config.onselect;
        var polygon = config.polygon;
        var backerToken = config.currentBackerToken;
        this.callParent(arguments);
        //this.mapOverlay = new google.maps.GroundOverlay();
        this.mapPolygon = new google.maps.Rectangle();
        this.selected = false;
        this.polygon = polygon;

        var vertices = polygon.get('vertices');
        var latLngPolygon = [];
        for(var i=0; i < vertices.length; i++) {
            latLngPolygon.push(new google.maps.LatLng(vertices[i].latitude, vertices[i].longitude));
        }

        this.polyPath = latLngPolygon;
        var fillColor = null;
        var thisHasABacker = !!this.polygon.get("backerToken");
        var thisIsTheBackers = backerToken &&
            this.polygon.set("backerToken", backerToken);
        if(thisIsTheBackers) {
            fillColor = "#0000FF";//This polygon is the backer's
        } else {
            //Either it has a backer or its totally available.
            fillColor = thisHasABacker ? "#990000" :  "#00FF00"
        }

        this.strokeColor = "#0000FF";
        this.mapPolygonOptions = {
            strokeColor: this.strokeColor,
            strokeOpacity: 0.8,
            strokeWeight: 1,
            fillColor: fillColor,
            fillOpacity: thisHasABacker ? .25 : 0,
            map: map,
            paths: this.polyPath
        };
        this.mapPolygon.setOptions(this.mapPolygonOptions);
        var self = this;

        if(!thisHasABacker) {
            google.maps.event.addListener(this.mapPolygon, 'click', function(event, options) {
                if(Permian.controller.MapController.altKeyDown) {
                    self.selected = !self.selected;
                    onSelect(self, self.selected);
                    fillColor: "#00FF00",
                        self.mapPolygonOptions.fillOpacity = self.selected ? .25 : 0;
                    self.mapPolygon.setOptions(self.mapPolygonOptions);
                }
            });
        }
    },

    destroy: function() {
        google.maps.event.clearListeners(this.mapPolygon, 'click');
        this.selected = null;
        this.polygon = null;
        this.polyPath = null;
        this.mapPolygonOptions = null;
        this.strokeColor = null;
        this.mapPolygon.setMap(null);
        this.mapPolygon = null;

    },

    getImageParams: function() {
        var center = this.polyPath.getCenter();
        var span = this.polyPath.toSpan();
        var params = {
            "center": center.lat() + "," + center.lng(),
            "span": span.lat() + "," + span.lng(),
            "maptype": "hybrid",
            "sensor": "false",
            "zoom": "11",
            "size": "64x64"};
        return params;
    },


    setHighlighted: function(highlight) {
        this.fillColor = highlight ? "#FFFF00" : "#0000FF";
        this.mapPolygonOptions.fillColor = this.fillColor;
        //this.mapPolygonOptions.fillOpacity = 1;
        this.mapPolygon.setOptions(this.mapPolygonOptions);
    }
})
