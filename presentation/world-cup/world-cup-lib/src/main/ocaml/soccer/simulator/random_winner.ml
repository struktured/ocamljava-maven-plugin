open Java
open Package'java'util
open Package'java'util'Random
open World_cup

let random = Java.make "Random()" ()

let random_choice first second = 
  let result = Java.call "Random.nextBoolean()" random in
  if result then first,second else second,first 

let pick_winner team1 team2 = 
  let winner, loser = random_choice team1 team2 in
  World_cup.beats winner loser 


let rec iter i n team1 team2 =
  if i >= n then (team1,team2) else (let team1,team2 = pick_winner team1 team2 in (iter (i+1) n team1 team2)) 
let _ = 
  let winner, loser = iter 0 10 World_cup.winning_team World_cup.losing_team in
  print_endline (String.concat " " ["country: "; winner.country; "wins:"; string_of_int winner.wins]);
  print_endline (String.concat " " ["country: "; loser.country; "wins:"; string_of_int loser.wins])

