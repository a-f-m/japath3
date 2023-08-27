// !!! skip blocks: 
// x:(
//     ...
// )
// y:($1)


start = _ p:simpleExpr _ !. { return stringify({start: p}); }

simpleExpr = 
    // path
    p:path!(_ ':') {return p;} /
    a:assignment {return a;}


assignment = y:(path / String) ':' _ e:path
    { return {assignment: {lhs: typeof y === 'string' ? { step: { lhsProperty: y}} : y, rhs: e }}}

path = x:( 
    stepExpr (('.' _ ) stepExpr )* 
)
    { return {path: skip(flatten(x), '.')}; }

////
stepExpr = x:(
    step  subscript* filter? varBinding? ( subExpr /*deprecated, use 'do' */ / do)? 
)
    { return x; }

////
step = x:(
    /* empty evaluation */
    nil / 
    /* constants */
    primitive / struct / array / 
    /* reflective */
    self / selector / 
    /* control & merge*/
    do / filter / cond / optional / boolExpr / 
    union / wildCard / 
    /* variables */
    create / varAppl / 
    /* misc */
    /*<*/text / /*>*/message / 
    /* parametric expressions */
    exprDef / exprAppl / argNumber / 
    /* external functions */
    scriptDef / funcCall /
    /* property selection */
    property
     
//-
)
{ return {step: x}; }

subExpr = 
    /* deprecated: */
    '{' _ e:expressions '}' _ { return { subExpr: {args: e, _deprecatedSyntax: true} }; }

do = 
    'do'!Identifier/*!*/ _ '(' _ e:expressions ')' _ { return { subExpr: {args: e} }; } 

primitive = x:(
    'null'!Identifier/*!*/ _ {return {constant: {}};} /
    c:(pi:Number _ {return pi;} / s:String!(_ ':') {return s;} ) 
            {return {constant: c};} /
    a:('true' _ /'false' _ )!Identifier/*!*/ { return {boolExpr: {op: 'constant', args: a}}; }
)

////
nil = 'nil'!Identifier/*!*/ _ { return {nil: ''}; }

////
self = ('_' / 'self')!Identifier/*!*/ _ { return {self: ''}; }

////
wildCard = 
    '**' _ bu:('^' _)? { return {wild: 'desc' + (bu !== null ? '-bu' : '')}; } / 
    '*' _ { return {wild: 'all'}; }
//-

////
selector = x:(
    '&' _ / 'selector'!Identifier/*!*/ _ / /* deprecated : */ '§' _ 
)
    { return {selector: x}; }

////
union = x:(
    'union'!Identifier/*!*/ _ '(' _ a:args ')' _ { return {arrayFlag: false, args: a}; }
)
    { return {union: x}; }

////
array = x:(
    '[' _ a:args? ']' _ { return {args: a === null ? [] : a}; } 
)
    { return {array: x}; }

////
// comparison = op:('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match' ) _ '(' _ a:path ')' _
//     { return {compare: {op: op, arg: a}}; }

////
/* 'assert' used in schema context, same semantics as 'and' */
boolExpr = 
    /* comparison */
    op:('eq'/'neq'/'lt'/'gt'/'le'/'ge'/'match' ) _ '(' _ a:path ')' _ { return {compare: {op: op, arg: a}}; } /
    /* junction */
    op:('and'/'assert'/'or'/'xor'/'not') _ '(' _ a:args ')' _ { op = op === 'assert' ? 'and' : op; return {boolExpr: {op: op, args: a}}; } /
    /* implication */
    'imply' _ '(' _ p:path ',' _ c:path ')' _ {  return {boolExpr: {op: 'imply', args: [p, c]}}; } /
    /* all quantification */
    'every' _ '(' _ q:path ',' _ c:path ')' _ { return {quantifierExpr: {op: 'every', quant: q, check:c}}; } /
    /* exists quantification */
    'some' _ '(' _ q:path ',' _ c:path ')' _ { return {quantifierExpr: {op: 'some', quant: q, check:c}}; } /
    /* type check */
    'type' _ '(' _ t:('String' / 'Number' / 'Boolean' / 'Any') _ ')' _ { return {hasType: t}; }

filter = ('filter'/'?') _ '(' _ b:path ')' _
    { return {filter: b}; }

////
cond = 'cond' _ '(' _ cond:path ',' _ ifExpr:path elseExpr:(',' _ path)? ')' _
    {   if (elseExpr !== null) elseExpr = elseExpr[2]; else elseExpr = null; 
        return {conditional: {cond: cond, ifExpr: ifExpr, elseExpr: elseExpr }}; }

////
optional = ('optional'/'opt') _ '(' _ o:path ')' _
    { return {optional: o}; }

////
// type = 'type' _ '(' _ t:('String' / 'Number' / 'Boolean' / 'Any') _ ')' _
//     { return {hasType: t}; }

////
text = 'text' _ '(' _ ')' _ 
    { return {text: ''}; }

////
varAppl = '$' _  id:Identifier? { return {var: (id === null ? '$' : id)}; } 

////
exprDef = 'def' _  '(' _ id:Identifier params:(  '(' _ parameters? ')' _ )? ',' _ e:simpleExpr ')' _
    {   //if (params !== null) print('>>>' + params[2]);
        return {def: {name: id, params: params === null || params[2] === null ? [] : params[2] , expr: e}}; }

////
parameters = x:(
    parameter (',' _ parameter )* 
)
    { return skip(flatten(x), ','); }

parameter = id:Identifier d:(':' _  path)?
    { return { param: { name: id, default: d !== null ? d[2] : null }}; }


////
scriptDef = 'def-script' _  '(' _ s:MultilineString ')' _
    { return {defScript: {s: s}}; }

////
message = 'message' _  '(' _ e:path ')' _
    { return {message: e}; }

////
argNumber = '#' i:index
    { return {argNumber: i}; }

exprAppl = !('asProperty'/'regex') id:Identifier '(' _ a:args? ')' _
    { return {exprAppl: {name: id, args: a}}; }

////
/* ... */
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
    'asProperty'!Identifier/*!*/ _ '(' _ p:path ')' _ { return {pathAsProperty: p}; } /
    'regex'!Identifier/*!*/ _ '(' _ s:String ')' _ { return {propertyRegex: s}; } /
    x:(
        Identifier / QIdentifier
    ) 
    { return {property: x}; }


////
subscript = 
    '[' _ '#' _ i:index upper:('..' _ index?)? ']' _ { return {subscript: i, seq: true, upper: upper === null ? null : (upper[2] === null ? -1 : upper[2]) }; } /
    '[' _ i:index ']' _ { return {subscript: i}; } /
    '[' _ '>' _ ']' _ { return {subscript: -1}; } 
//-


////

////
//    '$' _  id:Identifier  _ { return {bind: id}; } 
varBinding = '$' _  id:Identifier { return { step: {bind: id}}; } 
    

////
args = x:(
    path (',' _ path )* 
)
    { return skip(flatten(x), ','); }

expressions = x:(
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
Number = "-"? int frac? exp? { return Number(text()); }
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

Identifier = !Keyword id:([a-zA-Z_] [a-zA-Z0-9_]*) _ { return flatten(id).join(''); }

Keyword = '§' 
// seems not neccessary:
//    ('filter' / 'selector' / 'and' / 'assert' / 'or' / 'xor' / 'not' / 'true' / 'false' / 'cond' / 'imply' / 
//    'optional' / 'opt' / 'every' / 'union' / 'eq' / 'neq' / 'lt' / 'gt' / 'le' / 'ge' / 'call' / 'type' / 
//    'self' / 'def' / 'def-script' / 'new' / 'java' / 'javascript' / 'j' / 'js' / 'match' / 'null' / 'nil' / 'error' / 'message' / 
//    'property' / 'asProperty' / 'regex' / 'do') !Identifier

QIdentifier = 
    '`' id:('\\`'/[^`])+ '`' _ { return id.join('').replace(/\\`/g, '`'); } 


_ "whitespace"
  = ([ \t\n\r]+ / "//" (!([\r\n]/!.) .)* ([\r\n]/!.) )* 
    {return null;}
