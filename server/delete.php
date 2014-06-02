<?php
include_once "db.php";

$keyName = $_POST['KEY_NAME'];
$keyValue = $_POST[$keyName];

dbConnect ();
mysql_query ("DELETE FROM `{$_POST['TABLE_NAME']}` WHERE `{$keyName}` = '{$keyValue}'");
dbClose ();
?>
