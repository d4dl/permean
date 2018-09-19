<?php
include_once('../FieldHandler.php');
class Tests
{
    function testFieldHandler() {
        $fieldHandler = new FieldHandler();
        //return $fieldHandler->calculateFields(10, 21, 10, 11);
        return $fieldHandler->calculateFields(30.680302384106476, 29.315001887482143, -97.75256347656249, -96.24743652343749);
    }
}
?>
