// from 

function stringify(obj) {
    if (typeof obj !== 'object' || obj === null || obj instanceof Array) {
        return value(obj);
    }

    return '{' + Object.keys(obj).filter(function (k) { return obj[k] !== null; }).map(function (k) {
            return (typeof obj[k] === 'function') ? null : '"' + k + '":' + value(obj[k]);
        }).filter(function (i) { return i; }) + '}';
}

function value(val) {
    switch (typeof val) {
        case 'string':
            // print('+++' + '"' + val.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"')
            return '"' + val.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
            // return '"' + val.replace(/"/g, '\\"') + '"';
        case 'number':
        case 'boolean':
            return '' + val;
        case 'function':
            return 'null';
        case 'object':
            if (val instanceof Array) return '[' + val.filter(function (v) { return v !== null; }).map(value).join(',') + ']';
            if (val === null) return 'null';
            return stringify(val);
    }
}

function skip(arr, skip) {
    return arr.filter(
        function (x) { return x !== skip; }
    );
}

function flatten() {
    var flat = [];
    for (var i = 0; i < arguments.length; i++) {
      if (arguments[i] instanceof Array) {
        flat.push.apply(flat, flatten.apply(this, arguments[i]));
      } else {
        flat.push(arguments[i]);
      }
    }
    return flat;
  }
