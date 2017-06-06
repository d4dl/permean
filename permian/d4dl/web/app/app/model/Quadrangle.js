Ext.define('Permian.model.Quadrangle', {
    extend: 'Ext.data.Model',
    
    fields: [
        { name: 'id', type: 'auto' },
        { name: 'uid', type: 'auto' },
        { name: 'backerToken', type: 'auto' },
        { name: 'bottom', type: 'auto' },
        { name: 'west', type: 'auto' },
        { name: 'east', type: 'auto' },
        { name: 'top', type: 'auto' }

    ]
});
