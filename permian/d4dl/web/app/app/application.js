Ext.define('Permian.Application', {
    name: 'Permian',

    extend: 'Ext.app.Application',

    views: [
        "QuadrangleOverlay",
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
