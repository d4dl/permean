Ext.define("Permian.store.GeoStore", {
    extend:'Ext.data.Store',
    model: 'Permian.model.Quadrangle',

    proxy:{
        type:'ajax',
        url:"http://www.gridocracy.net/php/services/sliceService.php",
        reader:{
            type:'json'
        }
    },
    autoLoad:false,

    constructor: function() {
        this.callParent(arguments);
        this.on({
            load: function(store, records, successful, eOpts) {
                Permian.getApplication().fireEvent('quadranglesReceived');
            }
        });
    }

});
