<?php
include_once "db.php";

$dataArray = array ();
foreach ($_POST as $key => $value) {
	if ($key == "TABLE_NAME") continue;
	if ($key == "KEY_NAME") continue;

	$dataArray[$key] = $value;
}

$fields = implode ("`, `", array_keys ($dataArray));
$values = implode ("', '", array_values ($dataArray));
$query = "INSERT INTO `{$_POST['TABLE_NAME']}` (`{$fields}`) VALUES ('{$values}')";

dbConnect ();
mysql_query ($query);
echo "index=" . mysql_insert_id ();
dbClose ();
?>
