start = simpleExpr !. 
simpleExpr = 
    '(' simpleExpr ')' /
    'null' /
    (PosInt / String!(':')) /
    path!(':') /
    assignment
assignment = (path / String) ':' simpleExpr
path =  
    stepExpr (('.') stepExpr)* 
stepExpr = 
    step  subscript* filter? binding? subExpr? 
step = 
    self / wild / selector / union / comparison / boolExpr / 
    filter / cond / optional / 
    type / text / var /
    exprDef / argNumber / exprAppl / create / struct /
    funcCall /
    property 
self = ('_'!Identifier / 'self') 
wild = 
    '**' / 
    '*' 
selector = 
    'selector' / '§' 
union = 
    'union' '(' args ')' / 
    '[' args ']'  
comparison = ('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match') '(' simpleExpr ')' 
/* 'assert' used in schema context, same semantics as 'and' */
boolExpr = 
    ('and'/'assert'/'or'/'xor'/'not') '(' args ')' /
    ('true' /'false') /
    'imply' '(' simpleExpr ',' simpleExpr ')' /
    'every' '(' path ',' simpleExpr ')' /
    'some' '(' path ',' simpleExpr ')' 
filter = ('filter'/'?') '(' simpleExpr ')' 
cond = 'cond' '(' simpleExpr ',' simpleExpr (',' simpleExpr)? ')' 
optional = ('optional'/'opt') '(' path ')' 
type = 'type' '(' ('String' / 'Number' / 'Boolean' / 'Any') ')' 
text = 'text' '(' ')' 
var = '$'  Identifier?  
exprDef = 'def'  '(' Identifier ',' simpleExpr ')' 
argNumber = '#' PosInt
exprAppl = Identifier '(' args? ')' 
/* ... */
subExpr = '{' args '}' 
create = 'new' 
struct = '{' structArgs '}' 
funcCall =
    '::' Identifier ('(' args? ')')? /
    ('j''ava'? '::')? Identifier '::' Identifier '(' args? ')'  
property = 
    Identifier / QIdentifier
subscript = 
    '[' '#' PosInt ('..' (PosInt)?)? ']' /
    '[' PosInt ']'  
binding = '$'  Identifier  
args = 
    simpleExpr ((',') simpleExpr)* 
structArgs = 
    assignment ((',') assignment)* 
PosInt = ('0' / [1-9][0-9]*) 
String = SingleQuoteString / DoubleQuoteString
SingleQuoteString = "'" ('\\\''/[^'])* "'"  
DoubleQuoteString = "\"" ('\\"'/[^\"])* "\""  
Identifier = ([a-zA-Z ] [a-zA-Z0-9 ]*) 
QIdentifier = 
    '`' ('\\`'/[^`])+ '`'  
