(* world-cup.ml *)
type team = {country:string;wins:int;losses:int}

let create_team country = {country;wins=0;losses=0}

let beats team1 team2 = print_endline (String.concat " " [team1.country;"beats";team2.country;"!"])
;
{team1 with wins = team1.wins+1}
,
{team2 with losses = team2.losses+1}
 
 
let winning_team, losing_team =
  let w,l = create_team "thefavorite", create_team "theunderdog" in
  beats w l

  
