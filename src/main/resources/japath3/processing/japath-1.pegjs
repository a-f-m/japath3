// !!! skip blocks: 
// x:(
//     ...
// )
// y:($1)


start = _ p:simpleExpr _ !. { return stringify({start: p}); }

simpleExpr = 
    '(' _ s:simpleExpr ')' _ {return s;} /
    'null' {return {constant: {}};} /
    c:(pi:PosInt {return pi;} / s:String!(_ ':') {return s;} ) 
            {return {constant: c};} /
    p:path!(_ ':') {return p;} /
    assignment


assignment = y:(path / String) ':' _ e:simpleExpr
    { return {assignment: {lhs: typeof y === 'string' ? { step: { property: y}} : y, rhs: e }}}

////
//    step (subStep)? (('.' _ ) step (subStep)? )* 
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
    self / wild / selector / union / comparison / boolExpr / 
    filter / cond  / optional / 
    type / text / var /
    exprDef / argNumber / exprAppl / create / struct /
    funcCall /
    property // / subscript
     
//-
)
{ return {step: x}; }

////
self = ('_'!Identifier / 'self') _ { return {self: ''}; }

////
wild = 
    '**' _ { return {wild: 'desc'}; } / 
    '*' _ { return {wild: 'all'}; }
//-

////
selector = x:(
    'selector' _ / '§' _ 
)
    { return {selector: x}; }

////
union = x:(
    'union' _ '(' _ a:args ')' _ { return {arrayFlag: false, args: a}; } / 
    '[' _ a:args ']' _ { return {arrayFlag: true, args: a}; } 
)
    
    { return {union: x}; }

////
comparison = op:('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match' ) _ '(' _ a:simpleExpr ')' _
    { return {compare: {op: op, arg: a}}; }

////
/* 'assert' used in schema context, same semantics as 'and' */
boolExpr = 
//    op:('and'/'or'/'not') _ '(' _ a:boolExpr (',' _ boolExpr )*  ')' _
    op:('and'/'assert'/'or'/'xor'/'not') _ '(' _ a:args ')' _ { op = op === 'assert' ? 'and' : op; return {boolExpr: {op: op, args: a}}; } /

    a:('true' _ /'false' _ ) { return {boolExpr: {op: 'constant', args: a}}; } /

    'imply' _ '(' _ p:simpleExpr ',' _ c:simpleExpr ')' _ {  return {boolExpr: {op: 'imply', args: [p, c]}}; } /

    'every' _ '(' _ q:path ',' _ c:simpleExpr ')' _ { return {quantifierExpr: {op: 'every', quant: q, check:c}}; } /

    'some' _ '(' _ q:path ',' _ c:simpleExpr ')' _ { return {quantifierExpr: {op: 'some', quant: q, check:c}}; }

////
//    ('filter'/'?') _ b:boolExpr 
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
//    '$' _  id:Identifier  _ { return {var: id}; } 
var = '$' _  id:Identifier? { return {var: (id === null ? 'root' : id)}; } 


// ////
// function = 
//     'call' _ '(' _ f:QIdentifier ')' _ a:( '(' _ args ')' _ )?
// //-
// { if (a !== null) a = a[2]; else a = 'empty'; return {call: {func: f, args: a}}; }

////
exprDef = 'def' _  '(' _ id:Identifier ',' _ e:simpleExpr ')' _
    { return {def: {name: id , expr: e}}; }

////
argNumber = '#' i:PosInt
    { return {argNumber: i}; }

exprAppl = id:Identifier '(' _ a:args? ')' _
    { return {exprAppl: {name: id, args: a}}; }

////
/* ... */
subExpr = '{' _ a:args '}' _
    { return { subExpr: {args: a} }; }

create = 'new' _
    { return {create: ''}; }

struct = '{' _ a:structArgs '}' _
    { return { struct: {args: a} }; }

funcCall =
    '::' _ func:Identifier a:('(' _ args? ')')? _ { return { funcCall: {kind: 'directive', ns: '', func: func, args: a !== null ? a[2] : null } }; } /

    ('j''ava'? _ '::' _)? ns:Identifier '::' _ func:Identifier '(' _ a:args? ')' _ { return { funcCall: {kind: 'java', ns: ns, func: func, args: a} }; } 
    
////
property = x:(
    Identifier / QIdentifier
)
    { return {property: x}; }

////
subscript = 
    '[' _ '#' _ i:PosInt upper:('..' _ (PosInt)?)? ']' _ { return {subscript: i, seq: true, upper: upper === null ? null : (upper[2] === null ? -1 : upper[2]) }; } /
    '[' _ i:PosInt ']' _ { return {subscript: i}; } 
//-


////

////
//    '$' _  id:Identifier  _ { return {bind: id}; } 
binding = '$' _  id:Identifier { return { step: {bind: id}}; } 
    

////
args = x:(
    simpleExpr ((',' _ ) simpleExpr )* 
)
    { return skip(flatten(x), ','); }

structArgs = x:(
    assignment ((',' _ ) assignment )* 
)
    { return skip(flatten(x), ','); }


////

PosInt = ('0' / [1-9][0-9]* ) _ { return parseInt(text(), 10); }
//Number = '-'? Int ('.' [0-9]+)? _ { return parseInt(text(), 10); }
//Int = ('0' / [1-9][0-9]* )

String = SingleQuoteString / DoubleQuoteString

SingleQuoteString = "'" _ s:('\\\''/[^'])* "'" _ { return s.join('').replace(/\\'/g, "'"); } 
//    "'" _ s:('\\\''/[^'])* "'" _ { return s.join(''); } 

DoubleQuoteString = "\"" _ s:('\\"'/[^\"])* "\"" _ { return s.join('').replace(/\\"/g, '"'); } 

Identifier = id:([a-zA-Z_] [a-zA-Z0-9_]*) _ { return flatten(id).join(''); }

//    '`' id:('\\`'/[^`])+ '`' _ { return id.join(''); } /
QIdentifier = 
    '`' id:('\\`'/[^`])+ '`' _ { return id.join('').replace(/\\`/g, '`'); } 
//    /
//    s:DoubleQuoteString  { return s; } 


_ "whitespace"
  = ([ \t\n\r]+ / "//" (!([\r\n]/!.) .)* ([\r\n]/!.) )* 
    {return null;}
