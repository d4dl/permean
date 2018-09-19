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
        this.mapPolygon = new google.maps.Polygon();
        this.selected = false;
        this.polygon = polygon;

        var vertices = polygon.get('vertices');
        var latLngPolygon = [];
        for(var i=0; i < vertices.length; i++) {
            var position = {lat: vertices[i].latitude, lng: vertices[i].longitude};
            latLngPolygon.push(position);
        }

        this.polyPath = latLngPolygon;
        var fillColor = null;
        var thisHasABacker = !!this.polygon.get("backerToken");
        var thisIsTheBackers = backerToken && this.polygon.set("backerToken", backerToken);
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
            paths: this.polyPath
        };
        this.mapPolygon.setOptions(this.mapPolygonOptions);
        var self = this;
        //https://maps.googleapis.com/maps/api/staticmap?center=Brooklyn+Bridge,New+York,NY&zoom=13&size=600x300&maptype=roadmap&markers=color:blue%7Clabel:S%7C40.702147,-74.015794&markers=color:green%7Clabel:G%7C40.711614,-74.012318&markers=color:red%7Clabel:C%7C40.718217,-73.998284&key=AIzaSyDCP645m-UOXC0tv4nKfp4PxqiATSQfpwY
        //Static maps AIzaSyDCP645m-UOXC0tv4nKfp4PxqiATSQfpwY

        if(!thisHasABacker) {
            google.maps.event.addListener(this.mapPolygon, 'click', function(event, options) {
                if(event.xa.altKey) {
                    self.selected = !self.selected;
                    onSelect(self, self.selected);
                    fillColor: "#00FF00",
                        self.mapPolygonOptions.fillOpacity = self.selected ? .25 : 0;
                    self.mapPolygon.setOptions(self.mapPolygonOptions);
                }
            });
        }
        this.mapPolygon.setMap(map);
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
        var path="color:0xff0000|weight:2|";
        var vertices = this.polygon.get('vertices');
        for(var i=0; i <= vertices.length; i++) {
            var idx = i == vertices.length ? 0 : i;
            var lat = vertices[idx].latitude;
            var lng = vertices[idx].longitude;
            path += lat + "," + lng + (i == vertices.length ? "" : "|")
        }

        var params = {
            key: "AIzaSyDCP645m-UOXC0tv4nKfp4PxqiATSQfpwY",
            maptype: "roadmap",
            sensor: "false",
            path: path,
            size: "80x80"};
        return params;
    },


    setHighlighted: function(highlight) {
        this.fillColor = highlight ? "#FFFF00" : "#0000FF";
        this.mapPolygonOptions.fillColor = this.fillColor;
        //this.mapPolygonOptions.fillOpacity = 1;
        this.mapPolygon.setOptions(this.mapPolygonOptions);
    }
})
