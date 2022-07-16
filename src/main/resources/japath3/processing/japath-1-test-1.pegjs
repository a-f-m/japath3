// !!! skip blocks: 
// x:(
//     ...
// )
// y:($1)


start = _ p:simpleExpr _ !. { return stringify({start: p}); }

simpleExpr = 
    '(' _ s:simpleExpr ')' _  /
    'null'!Identifier/*!*/ _  /
    c:(pi:Number  / s:String!(_ ':')  ) 
             /
    p:path!(_ ':')  /
    assignment


assignment = y:(path / String) ':' _ e:simpleExpr
    

path = x:( 
    stepExpr (('.' _ ) stepExpr )* 
)

////
stepExpr = x:(
    step  subscript* filter? binding? subExpr? 
)
    

////
step = x:(
    self / wild / selector / union / comparison / boolExpr / 
    filter / cond  / optional / 
    type / text / var / argNumber / 
    exprDef / scriptDef / message /
    exprAppl / create / struct / array /
    funcCall /
    property // / subscript
     
//-
)

////
self = ('_' / 'self')!Identifier/*!*/ _ 

////
wild = 
    '**' _  / 
    '*' _ 
//-

////
selector = x:(
    '&' _ / 'selector'!Identifier/*!*/ _ / /* deprecated : */ '§' _ 
)
    

////
union = x:(
    'union' _ '(' _ a:args ')' _ 
)
    

////
array = x:(
    '[' _ a:args? ']' _  
)
    

////
comparison = op:('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match' ) _ '(' _ a:simpleExpr ')' _

////
/* 'assert' used in schema context, same semantics as 'and' */
boolExpr = 
//    op:('and'/'or'/'not') _ '(' _ a:boolExpr (',' _ boolExpr )*  ')' _
    op:('and'/'assert'/'or'/'xor'/'not') _ '(' _ a:args ')' _ /

    a:('true' _ /'false' _ )!Identifier/*!*/ /

    'imply' _ '(' _ p:simpleExpr ',' _ c:simpleExpr ')' _ /

    'every' _ '(' _ q:path ',' _ c:simpleExpr ')' _ /

    'some' _ '(' _ q:path ',' _ c:simpleExpr ')' _

filter = ('filter'/'?') _ '(' _ b:simpleExpr ')' _
    

////
cond = 'cond' _ '(' _ cond:simpleExpr ',' _ ifExpr:simpleExpr elseExpr:(',' _ simpleExpr)? ')' _

////
optional = ('optional'/'opt') _ '(' _ o:path ')' _
    

////
type = 'type' _ '(' _ t:('String' / 'Number' / 'Boolean' / 'Any') _ ')' _
    

////
text = 'text' _ '(' _ ')' _ 
    

////
var = '$' _  id:Identifier?  

////
exprDef = 'def' _  '(' _ id:Identifier ',' _ e:simpleExpr ')' _
   

////
scriptDef = 'def-script' _  '(' _ s:MultilineString ')' _
   

////
message = 'message' _  '(' _ e:simpleExpr ')' _
    

////
argNumber = '#' i:index
    

exprAppl = !('property') id:Identifier '(' _ a:args? ')' _
   

////
/* ... */
subExpr = '{' _ a:args '}' _

create = 'new'!Identifier/*!*/ _
    

struct = '{' _ a:structArgs? '}' _

funcCall =

    ('js'/'javascript')!Identifier/*!*/ _ '::' _ func:Identifier '(' _ a:args? ')' _  /

    'j''ava'? _ '::' _ ns:Identifier '::' _ func:Identifier '(' _ a:args? ')' _  /

    '::' _ func:Identifier a:('(' _ args? ')')? _ 

////
property = 
    'property'!Identifier _ '(' _ p:path ')' _  /
    x:(Identifier / QIdentifier) 

////
subscript = 
    '[' _ '#' _ i:index upper:('..' _ index?)? ']' _  /
    '[' _ i:index ']' _  
//-


////

////
//    '$' _  id:Identifier  _  
binding = '$' _  id:Identifier 
    

////
args = x:(
    simpleExpr (',' _ simpleExpr )* 
)
    

structArgs = x:(
    assignment (',' _ assignment )* 
)
    


////

index = int _ 

/* equivalent to json spec */
Number = "-"? int frac? exp? _ 
exp           = [eE] ("-" / "+")? digit+
frac          = "." digit+
int           = "0" / (digit1_9 digit*)
digit1_9      = [1-9]
digit  = [0-9]
/**/


String = SingleQuoteString / MultilineString / DoubleQuoteString 

SingleQuoteString = "'" s:('\\\''/[^'])* "'" _  

DoubleQuoteString = "\"" s:('\\"'/[^\"])* "\"" _  

MultilineString = 
    '"""' s:(!'"""'.)* '"""' _  

Identifier = id:([a-zA-Z_] [a-zA-Z0-9_]*) _ 

Keyword = 
    'selector' / 'filter' / 'and' / 'assert' / 'or' / 'xor' / 'not' / 'true' / 'false' / 'cond' / 'imply' / 
    'optional' / 'opt' / 'every' / 'union' / 'eq' / 'neq' / 'lt' / 'gt' / 'le' / 'ge' / 'call' / 'type' / 
    'self' / 'def' / 'def-script' / 'new' / 'java' / 'j' / 'js' / 'match' / 'null' / 'error' / 'message' / 'property'

QIdentifier = 
    '`' id:('\\`'/[^`])+ '`' _  


_ "whitespace"
  = ([ \t\n\r]+ / "//" (!([\r\n]/!.) .)* ([\r\n]/!.) )* 
    
