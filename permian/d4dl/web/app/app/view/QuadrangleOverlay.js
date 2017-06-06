Ext.define("Permian.view.QuadrangleOverlay", {
    extend: 'Ext.Component',

    selected: null,
    quadrangle: null,
    bounds: null,
    rectOptions: null,
    strokeColor: null,
    rectangle: null,

    constructor: function(config) {
        var map = config.map;
        var onSelect = config.onselect;
        var quadrangle = config.quadrangle;
        var backerToken = config.currentBackerToken;
        this.callParent(arguments);
        //this.mapOverlay = new google.maps.GroundOverlay();
        this.rectangle = new google.maps.Rectangle();
        this.selected = false;
        this.quadrangle = quadrangle;


        this.bounds = new google.maps.LatLngBounds(new google.maps.LatLng(quadrangle.get("bottom")*1, quadrangle.get("west")*1),
                                                   new google.maps.LatLng(quadrangle.get("top")*1   , quadrangle.get("east")*1));
        var fillColor = null;
        var thisHasABacker = !!this.quadrangle.get("backerToken");
        var thisIsTheBackers = backerToken &&
            this.quadrangle.set("backerToken", backerToken);
        if(thisIsTheBackers) {
            fillColor = "#0000FF";//This quadrangle is the backer's
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
        this.quadrangle = null;
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
