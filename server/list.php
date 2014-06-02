<?php
include_once "db.php";

$conditionArray = array ();
foreach ($_POST as $key => $value) {
	if ($key == "TABLE_NAME") continue;
	array_push ($conditionArray, "`{$key}` = '{$value}'");
}
$conditions = implode (" AND ", $conditionArray);
if (strlen (trim ($conditions)) == 0)
	$conditions = "TRUE";

dbConnect ();
$result = mysql_query ("SELECT * FROM `{$_POST['TABLE_NAME']}` WHERE {$conditions}");
if ($result) {
	while (($data = mysql_fetch_assoc ($result))) {
		$messages = array ();
		foreach ($data as $key => $value) {
			array_push ($messages, "{$key}={$value}");
		}
		echo implode ("&", $messages) . "\n";
	}
}
dbClose ();
?>
