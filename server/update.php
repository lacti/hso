<?php
include_once "db.php";

$keyName = $_POST['KEY_NAME'];
$keyValue = $_POST[$keyName];

$dataArray = array ();
foreach ($_POST as $key => $value) {
	if ($key == "TABLE_NAME") continue;
	if ($key == "KEY_NAME") continue;

	array_push ($dataArray, "`{$key}` = '{$value}'");
}
$query = "UPDATE `{$_POST['TABLE_NAME']}` SET " . implode (", ", $dataArray) . " WHERE `{$keyName}` = '{$keyValue}'";

dbConnect ();
mysql_query ($query);

$result = mysql_query ("SELECT * FROM `{$_POST['TABLE_NAME']}` WHERE `{$keyName}` = '{$keyValue}'");
if ($result) {
	$data = mysql_fetch_assoc ($result);
	$messages = array ();
	foreach ($data as $key => $value) {
		array_push ($messages, "{$key}={$value}");
	}
	echo implode ("&", $messages);
}
dbClose ();
?>
