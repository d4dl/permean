Ext.define("Permian.view.PolygonOverlay", {
    extend: 'Ext.Component',

    selected: null,
    polygon: null,
    bounds: null,
    rectOptions: null,
    strokeColor: null,
    rectangle: null,

    constructor: function(config) {
        var map = config.map;
        var onSelect = config.onselect;
        var polygon = config.polygon;
        var backerToken = config.currentBackerToken;
        this.callParent(arguments);
        //this.mapOverlay = new google.maps.GroundOverlay();
        this.rectangle = new google.maps.Rectangle();
        this.selected = false;
        this.polygon = polygon;

        var latLngPolygon = [];
        for(var i=0; i < polygon.length; i++) {
            latLngPolygon.push(new google.maps.LatLng(polygon[i]*1));
        }

        this.bounds = new google.maps.LatLngBounds(latLngPolygon);
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
        this.rectOptions = {
            strokeColor: this.strokeColor,
            strokeOpacity: 0.8,
            strokeWeight: 1,
            fillColor: fillColor,
            fillOpacity: thisHasABacker ? .25 : 0,
            map: map,
            bounds: this.bounds
        };
        this.rectangle.setOptions(this.rectOptions);
        var self = this;

        if(!thisHasABacker) {
            google.maps.event.addListener(this.rectangle, 'click', function(event, options) {
                if(Permian.controller.MapController.altKeyDown) {
                    self.selected = !self.selected;
                    onSelect(self, self.selected);
                    fillColor: "#00FF00",
                        self.rectOptions.fillOpacity = self.selected ? .25 : 0;
                    self.rectangle.setOptions(self.rectOptions);
                }
            });
        }
    },

    destroy: function() {
        google.maps.event.clearListeners(this.rectangle, 'click');
        this.selected = null;
        this.polygon = null;
        this.bounds = null;
        this.rectOptions = null;
        this.strokeColor = null;
        this.rectangle.setMap(null);
        this.rectangle = null;

    },

    getImageParams: function() {
        var center = this.bounds.getCenter();
        var span = this.bounds.toSpan();
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
        this.rectOptions.fillColor = this.fillColor;
        //this.rectOptions.fillOpacity = 1;
        this.rectangle.setOptions(this.rectOptions);
    }
})
