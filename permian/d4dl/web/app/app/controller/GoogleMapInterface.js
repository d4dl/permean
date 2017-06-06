Ext.define('Permian.controller.GoogleMapInterface', {
    extend: 'Ext.app.Controller',
    allOverlays: [],
        map: null,
        infoWindow: new google.maps.InfoWindow(),

        panToBounds: function(centerLatitude, centerLongitude, mapBounds) {
            this.map.fitBounds(mapBounds);
        },
        clearOverlays: function(node) {
            for(i=0; i < this.allOverlays.length; i++) {
                this.allOverlays[i].setMap(null);
            }
            this.allOverlays = [];
        },
        addMarker: function(item, depth) {
            var options = this.markerOptions[depth];
            var marker = new google.maps.Marker({
                position: new google.maps.LatLng(item.latitude, item.longitude),
                on: options.image,
                infoWindowText: item.label,
                infoWindow: this.infoWindow,
                title: item.name
            });
            this.allOverlays.push(marker);
        },
        lineTo: function(fromItem, toItem, depth) {
            var options = this.markerOptions[depth];
            var linePath = [
                new google.maps.LatLng(fromItem.latitude, fromItem.longitude),
                new google.maps.LatLng(toItem.latitude, toItem.longitude)
            ];
            var line = new google.maps.Polyline({
                path: linePath,
                geodesic: true,
                strokeColor: options.strokeColor,
                strokeOpacity: 0.8,
                strokeWeight: options.strokeWeight
            });
            line.setMap(this.map);
            this.allOverlays.push(line);
        },
        createPolygons: function(groupMap) {
            console.log("polygon");
            for(var groupId in groupMap) {
                var groupPolygon = groupMap[groupId];
                var triangleCoords = [];

                for(var i=0; i < groupPolygon.length; i++) {
                    triangleCoords.push(new google.maps.LatLng(groupPolygon[i].latitude, groupPolygon[i].longitude));
                    console.log(groupPolygon[i].latitude + "," + groupPolygon[i].longitude);
                }
                polygon = new google.maps.Polygon({
                    paths: triangleCoords,
                    strokeColor: "#FF0000",
                    strokeOpacity: 0.8,
                    strokeWeight: 2,
                    fillColor: "#FF0000",
                    fillOpacity: 0.35
                });
                polygon.setMap(this.map);
            }
            this.allOverlays.push(polygon);
        },
        createListener: function(marker) {
            return function() {
                marker.infoWindow.setContent(marker.infoWindowText);
                marker.infoWindow.open(this.map, marker);
            };
        },
        markerOptions: {
            0: {
                image: new google.maps.MarkerImage('img/icons/school-24x24BW.png',
                    new google.maps.Size(24, 24),
                    new google.maps.Point(0,0))
            },
            1: {
                image: new google.maps.MarkerImage('img/icons/school-16x16.png',
                    new google.maps.Size(16, 16),
                    new google.maps.Point(0,0)),
                strokeWeight: 2,
                strokeColor: "#FF0000"
            },
            2: {
                image: new google.maps.MarkerImage('img/icons/dotRed.png',
                    new google.maps.Size(16, 16),
                    new google.maps.Point(0,0)),
                strokeWeight: 2,
                strokeColor: "#006600"
            },
            3: {
                image: new google.maps.MarkerImage('img/icons/dotRed.png',
                    new google.maps.Size(16, 16),
                    new google.maps.Point(0,0)),
                strokeWeight: 2,
                strokeColor: "#006600"
            }
        }
});
