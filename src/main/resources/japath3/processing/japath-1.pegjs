// !!! skip blocks: 
// x:(
//     ...
// )
// y:($1)


start = _ p:simpleExpr _ !. { return stringify({start: p}); }

simpleExpr = 
    '(' _ s:simpleExpr ')' _ {return s;} /
    'null'!Identifier/*!*/ _ {return {constant: {}};} /
    c:(pi:Number {return pi;} / s:String!(_ ':') {return s;} ) 
            {return {constant: c};} /
    p:path!(_ ':') {return p;} /
    assignment


assignment = y:(path / String) ':' _ e:simpleExpr
    { return {assignment: {lhs: typeof y === 'string' ? { step: { lhsProperty: y}} : y, rhs: e }}}

path = x:( 
    stepExpr (('.' _ ) stepExpr )* 
)
    { return {path: skip(flatten(x), '.')}; }

////
stepExpr = x:(
    step  subscript* filter? binding? subExpr? 
)
    { return x; }

////
step = x:(
    nil / self / wild / selector / union / comparison / boolExpr / 
    filter / cond  / optional / 
    type / text / var / argNumber / 
    exprDef / scriptDef / message /
    exprAppl / create / struct / array /
    funcCall /
    property // / subscript
     
//-
)
{ return {step: x}; }

////
nil = 'nil'!Identifier/*!*/ _ { return {nil: ''}; }

////
self = ('_' / 'self')!Identifier/*!*/ _ { return {self: ''}; }

////
wild = 
    '**' _ { return {wild: 'desc'}; } / 
    '*' _ { return {wild: 'all'}; }
//-

////
selector = x:(
    '&' _ / 'selector'!Identifier/*!*/ _ / /* deprecated : */ '§' _ 
)
    { return {selector: x}; }

////
union = x:(
    'union' _ '(' _ a:args ')' _ { return {arrayFlag: false, args: a}; }
)
    { return {union: x}; }

////
array = x:(
    '[' _ a:args? ']' _ { return {args: a === null ? [] : a}; } 
)
    { return {array: x}; }

////
comparison = op:('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match' ) _ '(' _ a:simpleExpr ')' _
    { return {compare: {op: op, arg: a}}; }

////
/* 'assert' used in schema context, same semantics as 'and' */
boolExpr = 
//    op:('and'/'or'/'not') _ '(' _ a:boolExpr (',' _ boolExpr )*  ')' _
    op:('and'/'assert'/'or'/'xor'/'not') _ '(' _ a:args ')' _ { op = op === 'assert' ? 'and' : op; return {boolExpr: {op: op, args: a}}; } /

    a:('true' _ /'false' _ )!Identifier/*!*/ { return {boolExpr: {op: 'constant', args: a}}; } /

    'imply' _ '(' _ p:simpleExpr ',' _ c:simpleExpr ')' _ {  return {boolExpr: {op: 'imply', args: [p, c]}}; } /

    'every' _ '(' _ q:path ',' _ c:simpleExpr ')' _ { return {quantifierExpr: {op: 'every', quant: q, check:c}}; } /

    'some' _ '(' _ q:path ',' _ c:simpleExpr ')' _ { return {quantifierExpr: {op: 'some', quant: q, check:c}}; }

filter = ('filter'/'?') _ '(' _ b:simpleExpr ')' _
    { return {filter: b}; }

////
cond = 'cond' _ '(' _ cond:simpleExpr ',' _ ifExpr:simpleExpr elseExpr:(',' _ simpleExpr)? ')' _
    {   if (elseExpr !== null) elseExpr = elseExpr[2]; else elseExpr = null; 
        return {conditional: {cond: cond, ifExpr: ifExpr, elseExpr: elseExpr }}; }

////
optional = ('optional'/'opt') _ '(' _ o:path ')' _
    { return {optional: o}; }

////
type = 'type' _ '(' _ t:('String' / 'Number' / 'Boolean' / 'Any') _ ')' _
    { return {hasType: t}; }

////
text = 'text' _ '(' _ ')' _ 
    { return {text: ''}; }

////
var = '$' _  id:Identifier? { return {var: (id === null ? '$' : id)}; } 

////
exprDef = 'def' _  '(' _ id:Identifier params:(  '(' _ parameters? ')' _ )? ',' _ e:simpleExpr ')' _
    {   //if (params !== null) print('>>>' + params[2]);
        return {def: {name: id, params: params === null || params[2] === null ? [] : params[2] , expr: e}}; }

////
parameters = x:(
    parameter (',' _ parameter )* 
)
    { return skip(flatten(x), ','); }

parameter = id:Identifier d:(':' _  simpleExpr)?
    { return { param: { name: id, default: d !== null ? d[2] : null }}; }


////
scriptDef = 'def-script' _  '(' _ s:MultilineString ')' _
    { return {defScript: {s: s}}; }

////
message = 'message' _  '(' _ e:simpleExpr ')' _
    { return {message: e}; }

////
argNumber = '#' i:index
    { return {argNumber: i}; }

exprAppl = !('property') id:Identifier '(' _ a:args? ')' _
    { return {exprAppl: {name: id, args: a}}; }

////
/* ... */
subExpr = '{' _ a:args '}' _
    { return { subExpr: {args: a} }; }

create = 'new'!Identifier/*!*/ _
    { return {create: ''}; }

struct = '{' _ a:structArgs? '}' _
    { return { struct: {args: a === null ? [] : a} }; }

funcCall =

    ('js'/'javascript')!Identifier/*!*/ _ '::' _ func:Identifier '(' _ a:args? ')' _ { return { funcCall: {kind: 'javascript', ns: '', func: func, args: a} }; } /

    'j''ava'? _ '::' _ ns:Identifier '::' _ func:Identifier '(' _ a:args? ')' _ { return { funcCall: {kind: 'java', ns: ns, func: func, args: a} }; } /

    '::' _ func:Identifier a:('(' _ args? ')')? _ { return { funcCall: {kind: 'directive', ns: '', func: func, args: a !== null ? a[2] : null } }; }

////
property = 
    'property'!Identifier _ '(' _ p:path ')' _ { return {pathAsProperty: p}; } /
    x:(Identifier / QIdentifier) { return {property: x}; }

////
subscript = 
    '[' _ '#' _ i:index upper:('..' _ index?)? ']' _ { return {subscript: i, seq: true, upper: upper === null ? null : (upper[2] === null ? -1 : upper[2]) }; } /
    '[' _ i:index ']' _ { return {subscript: i}; } /
    '[' _ '>' _ ']' _ { return {subscript: -1}; } 
//-


////

////
//    '$' _  id:Identifier  _ { return {bind: id}; } 
binding = '$' _  id:Identifier { return { step: {bind: id}}; } 
    

////
args = x:(
    simpleExpr (',' _ simpleExpr )* 
)
    { return skip(flatten(x), ','); }

structArgs = x:(
    assignment (',' _ assignment )* 
)
    { return skip(flatten(x), ','); }



////

index = int _ { return parseInt(text(), 10); }

/* equivalent to json spec */
Number = "-"? int frac? exp? _ { return Number(text()); }
exp           = [eE] ("-" / "+")? digit+
frac          = "." digit+
int           = "0" / (digit1_9 digit*)
digit1_9      = [1-9]
digit  = [0-9]
/**/


String = SingleQuoteString / MultilineString / DoubleQuoteString 

SingleQuoteString = "'" s:('\\\''/[^'])* "'" _ { return s.join('').replace(/\\'/g, "'"); } 

DoubleQuoteString = "\"" s:('\\"'/[^\"])* "\"" _ { return s.join('').replace(/\\"/g, '"'); } 

MultilineString = 
    '"""' s:(!'"""'.)* '"""' _ { return flatten(s).join('').replace(/\n/g, '\\n').replace(/\r/g, '\\r'); } 

Identifier = id:([a-zA-Z_] [a-zA-Z0-9_]*) _ { return flatten(id).join(''); }

Keyword = 
    'selector' / 'filter' / 'and' / 'assert' / 'or' / 'xor' / 'not' / 'true' / 'false' / 'cond' / 'imply' / 
    'optional' / 'opt' / 'every' / 'union' / 'eq' / 'neq' / 'lt' / 'gt' / 'le' / 'ge' / 'call' / 'type' / 
    'self' / 'def' / 'def-script' / 'new' / 'java' / 'j' / 'js' / 'match' / 'null' / 'error' / 'message' / 'property'

QIdentifier = 
    '`' id:('\\`'/[^`])+ '`' _ { return id.join('').replace(/\\`/g, '`'); } 


_ "whitespace"
  = ([ \t\n\r]+ / "//" (!([\r\n]/!.) .)* ([\r\n]/!.) )* 
    {return null;}
