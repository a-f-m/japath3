function fjoin(x, sep, ...a) {
	return a.flat().join(sep)
}
function fconc(x, ...a) {
	return fjoin(x, '', a.flat())
}
function conc(x, ...a) {
	return  a.join('')
}
function incr(x) {
	return  x + 1
}
