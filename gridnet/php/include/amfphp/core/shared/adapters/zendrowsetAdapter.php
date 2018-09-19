<?php
/**
 * This Adapter translates the specific Database type links to the data and pulls the data into very
 * specific local variables to later be retrieved by the gateway and returned to the client.
 *
 * pdoAdapter is a contribution of Andrea Giammarchi
 *
 * Now using fast serialization
 * 
 * @license http://opensource.org/licenses/gpl-license.php GNU Public License
 * @copyright (c) 2003 amfphp.org
 * @package flashservices
 * @subpackage adapters
 * @version $Id: zendrowsetAdapter.php,v 1.1 2009-01-03 22:04:22 jdeford Exp $
 */
require_once(AMFPHP_BASE . "shared/adapters/RecordSetAdapter.php");
class zendrowsetAdapter extends RecordSetAdapter 
{
	function zendrowsetAdapter($d) {
		parent::RecordSetAdapter($d);
		
		if($d->count() > 0)
		{
			$d->rewind();
			$firstRow = $d->current()->toArray();
			
			$this->columns = array_keys($firstRow);
			
			//Note: foreach resets array iterator pointer
			
			foreach($d as $row)
			{
				$this->rows[] = array_values($row->toArray());
			}
		}
	}
}
?
>