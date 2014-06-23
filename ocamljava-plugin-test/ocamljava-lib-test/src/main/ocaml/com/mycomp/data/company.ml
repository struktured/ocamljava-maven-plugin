type company = {name:string; is_public:bool}

let noop () = print_endline "noop"

let create_private_company name = {name;is_public=false}

