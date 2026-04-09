var exec = require('cordova/exec');

exports.readHoldingRegister = function (arg0, arg1, arg2, success, error) {
    exec(success, error, 'MyModbusTCP', 'readHoldingRegister', [arg0, arg1, arg2]);
};

exports.readCoil = function (arg0, arg1, arg2, success, error) {
    exec(success, error, 'MyModbusTCP', 'readCoil', [arg0, arg1, arg2]);
};

exports.writeHoldingRegister = function (arg0, arg1, arg2, success, error) {
    exec(success, error, 'MyModbusTCP', 'writeHoldingRegister', [arg0, arg1, arg2]);
};

exports.writeCoil = function (arg0, arg1, arg2, success, error) {
    exec(success, error, 'MyModbusTCP', 'writeCoil', [arg0, arg1, arg2]);
};

exports.ping = function (arg0, success, error) {
    exec(success, error, 'MyModbusTCP', 'ping', [arg0]);
};
