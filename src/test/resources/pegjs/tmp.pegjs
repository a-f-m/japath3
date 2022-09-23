expression = x:( x:( _ (definitionStatus _)? subExpression _ ) )
  { return stringify(flatten(x)); }
subExpression = x:( focusConcept (_ ":" _ refinement)? ) { return {subExpression: flatten(x)}}
definitionStatus = x:( equivalentTo / subtypeOf ) { return {definitionStatus: flatten(x)}}
equivalentTo = x:( "===" ) { return {equivalentTo: flatten(x)}}
subtypeOf = x:( "<<<" ) { return {subtypeOf: flatten(x)}}
focusConcept = x:( conceptReference (_ "+" _ conceptReference)*  ) { return {focusConcept: flatten(x)}}
conceptReference = x:( conceptId (_ "|" _ term _ "|")? ) { return {conceptReference: flatten(x)}}
conceptId = x:( sctId ) { return {conceptId: flatten(x)}}
term = x:( nonwsNonPipe ( _ nonwsNonPipe )* ) { return {term: flatten(x)}}
refinement = x:(  (attributeSet / attributeGroup) ( _ ("," _)? attributeGroup )* ) { return {refinement: flatten(x)}}
attributeGroup = x:( "{" _ attributeSet _ "}" ) { return {attributeGroup: flatten(x)}}
attributeSet = x:( attribute (_ "," _ attribute)* ) { return {attributeSet: flatten(x)}}
attribute = x:( attributeName _ "=" _ attributeValue ) { return {attribute: flatten(x)}}
attributeName = x:( conceptReference ) { return {attributeName: flatten(x)}}
attributeValue = x:(  expressionValue / QM stringValue QM / "#" numericValue  ) { return {attributeValue: flatten(x)}}
expressionValue = x:( conceptReference / "(" _ subExpression _ ")" ) { return {expressionValue: flatten(x)}}
stringValue = x:( /*1*/  (anyNonEscapedChar / escapedChar)* ) { return {stringValue: flatten(x)}}
numericValue = x:( decimalValue / integerValue ) { return {numericValue: flatten(x)}}
integerValue = x:( (("-"/"+")? digitNonZero digit* ) / zero ) { return {integerValue: flatten(x)}}
decimalValue = x:( integerValue  "." /*1*/ digit* ) { return {decimalValue: flatten(x)}}
sctId = x:( digitNonZero /*5*17*/( digit+ ) )
  { return Number(text()); }
HTAB = x:( '\x09' ) { return {HTAB: flatten(x)}}
CR = x:( '\x0D' ) { return {CR: flatten(x)}}
LF = x:( '\x0A' ) { return {LF: flatten(x)}}
QM = x:( "'" ) { return {QM: flatten(x)}}
BS = x:( "\\" ) { return {BS: flatten(x)}}
digit = x:( [0-9] ) { return {digit: flatten(x)}}
zero = x:( '0' ) { return {zero: flatten(x)}}
digitNonZero = x:( [1-9]  ) { return {digitNonZero: flatten(x)}}
nonwsNonPipe = x:( [\x21-\x7B] / [\x7D-\x7E] / UTF8_2 / UTF8_3 / UTF8_4 ) { return {nonwsNonPipe: flatten(x)}}
anyNonEscapedChar = x:( HTAB / CR / LF / [\x20-\x21] / [\x23-\x5B] / [\x5D-\x7E] / UTF8_2 / UTF8_3 / UTF8_4 ) { return {anyNonEscapedChar: flatten(x)}}
escapedChar = x:( BS QM /  BS BS ) { return {escapedChar: flatten(x)}}
UTF8_2 = x:( [\xC2-\xDF] UTF8_tail ) { return {UTF8_2: flatten(x)}}
UTF8_3 = x:( '\xE0' [\xA0-\xBF] UTF8_tail / [\xE1-\xEC] ( UTF8_tail ) / '\xED' [\x80-\x9F] UTF8_tail / [\xEE-\xEF] ( UTF8_tail ) ) { return {UTF8_3: flatten(x)}}
UTF8_4 = x:( '\xF0' [\x90-\xBF] ( UTF8_tail ) / [\xF1-\xF3] ( UTF8_tail ) / '\xF4' [\x80-\x8F] ( UTF8_tail ) ) { return {UTF8_4: flatten(x)}}
UTF8_tail = x:( [\x80-\xBF] ) { return {UTF8_tail: flatten(x)}}
_ "whitespace"
  = ([ \x09\x0A\x0D]+ / "//" (!([\r\n]/!.) .)* ([\r\n]/!.) )* 
    {return null;}
