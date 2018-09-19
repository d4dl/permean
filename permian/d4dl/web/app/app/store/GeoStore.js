Ext.define("Permian.store.GeoStore", {
    extend:'Ext.data.Store',
    model: 'Permian.model.Polygon',

    proxy:{
        type:'ajax',
        url:"http://35.168.144.221:8080/cells/search/findByVerticesLatitudeBetweenAndVerticesLongitudeBetween",
        reader:{
            type:'json',
            root: '_embedded.cells'
        }
    },
    autoLoad:false,

    constructor: function() {
        this.callParent(arguments);
        this.on({
            load: function(store, records, successful, eOpts) {
                Permian.getApplication().fireEvent('polygonsReceived');
            }
        });
    }

})
