Ext.define('Permian.Application', {
    name: 'Permian',

    extend: 'Ext.app.Application',

    views: [
        "PolygonOverlay",
        "MapControls"
    ],

    controllers: [
        "GoogleMapInterface",
        "Main",
        "MapController"
    ],

    stores: [
        //"GeoStore"
    ]
});
