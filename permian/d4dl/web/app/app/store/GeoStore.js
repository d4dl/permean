Ext.define("Permian.store.GeoStore", {
    extend:'Ext.data.Store',
    model: 'Permian.model.Polygon',

    proxy:{
        type:'ajax',
        reader:{
            type:'json',
            root: '_embedded.cells'
        }
    },
    autoLoad:false,

    constructor: function() {
        this.callParent(arguments);
        this.proxy.url = Permian.baseUrl + "/cells/search/findByCenterLatitudeBetweenAndCenterLongitudeBetween",
        this.on({
            load: function(store, records, successful, eOpts) {
                Permian.getApplication().fireEvent('polygonsReceived');
            }
        });
    }

})
