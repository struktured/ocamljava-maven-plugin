open Company
open Package'com'mycomp'data
open Package'com'mycomp'data'Color


let is_public c = c.is_public

let create_color ~r ~g ~b = 
	let some_string = Java.make "String()" () in
	let color = Java.make "Color()" () in
	let to_string = Java.call "Color.toString()" color in
	print to_string; to_string