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
 * @version $Id: pdoAdapter.php,v 1.1 2009-01-03 22:04:21 jdeford Exp $
 */
require_once(AMFPHP_BASE . "shared/adapters/RecordSetAdapter.php");
class pdoAdapter extends RecordSetAdapter 
{
	function pdoAdapter($d) {
		parent::RecordSetAdapter($d);
		
		$line = $d->fetch(PDO::FETCH_ASSOC, PDO::FETCH_ORI_ABS, 0);
		if($line != null)
		{
			$colNum = 0;
			$firstLine = array();
			foreach($line as $k => $v)
			{
				$this->columns[$colNum] = $k;
				$firstLine[] = $v;
				$colNum++;
			}
			
			$lastLines = $d->fetchAll(PDO::FETCH_NUM);
			if($lastLines == NULL)
			{
				$this->rows = array($firstLine);
			}
			else
			{
				array_unshift($lastLines, $firstLine);
				$this->rows = $lastLines;
			}
		}
	}
}
?>