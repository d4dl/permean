<?php
/**
 * TraceHeader is a simple holder class for NetDebug::trace headers
 * 
 * @license http://opensource.org/licenses/gpl-license.php GNU Public License
 * @copyright (c) 2003 amfphp.org
 * @package flashservices
 * @author Justin Watkins
 * @version $Id: TraceHeader.php,v 1.1 2009-01-03 22:00:25 jdeford Exp $
 * @subpackage debug
 */
 
class TraceHeader {
	function TraceHeader($traceStack) {
		$this->EventType = "trace";
		$this->Time = time();
		$this->Source = "Server";
		$this->Date = array(date("D M j G:i:s T O Y"));
		$this->messages = $traceStack;
	} 
} 
class ProfilingHeader {
	function ProfilingHeader() {
		global $amfphp;
		$this->EventType = "profiling";
		
		$this->includeTime = (int) ($amfphp['includeTime']*1000);
		$this->decodeTime = (int) ($amfphp['decodeTime']*1000);
		$this->callTime = (int) ($amfphp['callTime']*1000);
		$this->totalTime = -268435457;
		$this->frameworkTime = ((int) ($amfphp['totalTime']*1000)) 
							- $this->includeTime
							- $this->decodeTime
							- $this->callTime;
	}
}
?>