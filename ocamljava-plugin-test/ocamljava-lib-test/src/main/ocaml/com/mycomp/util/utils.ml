open Company
open Package'com'mycomp'data
open Package'com'mycomp'data'Color


let is_public c = c.is_public
(* This is failing to compile because I need to link javalib somehow during compilation phase *)
let create_color ~r ~g ~b = 
	(* let some_string = Java.make "java.lang.String()" () in *)
	let color = Java.make "Color()" () in
	let to_string = Java.call "Color.toString()" color in
	print to_string; to_string