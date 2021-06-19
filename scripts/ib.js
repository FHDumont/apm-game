function sortValue(list) {
  return list[Math.floor(Math.random() * list.length)];
}

function appdGetParameterByName(_name, _url = window.location.href) {
  let _result = undefined;
  try {
      _result = new URLSearchParams(window.location.search).get(_name);
  } catch (error) {
      try {
          _name = _name.replace(/[\[\]]/g, '\\$&');
          let _regex = new RegExp('[?&]' + _name + '(=([^&#]*)|&|#|$)'), _results = _regex.exec(_url);
          if (!_results) return null;
          if (!_results[2]) return '';
          _result =  decodeURIComponent(_results[2].replace(/\+/g, ' '));
      } catch (error) {_result = undefined;}
  }
  if ( _result == null) {
      _result = undefined;
  }
  return _result;
}

function appdCreatePageView() {
  let userDataStr = {};
  let userDataLong = {};
  let userDataBoolean = {};
  let userDataDouble = {};
  userDataStr["customVersion"] = "v1.0";

  try {
    let _url = window.location.href;
    if ( _url.indexOf("/login") != -1 ) {
      userDataStr["firstName"] = sortValue(["Fulano","Ciclano","Antonio","Pedro","Joao","Maria","Patricia","Aline"]);
      userDataStr["lastName"] = sortValue(["Santos","Oliveira","Soares","Souza","Alvez","Pereira"]);
      userDataStr["eMail"] = sortValue(["abc@gmail.com","cde@outlook.com","123@gmail.com","456@outlook.com","zeroum@icloud.com"]);
      userDataLong["brand"] = sortValue([1001,1002,1003,1004]);
      userDataLong["account"] = sortValue([5001,5002,5003,5004,5005,5006,5007,5008,5009,5010]);
      userDataStr["segment"] = sortValue(["Exclusive","Prime","Universitario","Salario"]);
    }

    if ( _url.indexOf("/conta-corrente/saldo") != -1 ) {
      userDataDouble["balance"] = sortValue([1200.12,523.12,65125.21,5423.12,6421.00,542431.43]);
    }

    if ( _url.indexOf("/conta-corrente/fazer-transferencia") != -1 ) {
      userDataStr["paymentType"] = sortValue(["DOC","TED"]);
      userDataStr["paymentTarget"] = sortValue(["1001-5001","1001-5002","1004-5001","1003-5005","1005-5010","1002-1002"]);
      userDataDouble["paymentValue"] = sortValue([124.12,512.23,6423.12,123.43,33.65,6.23,863.12,6723.52]);
    }    

    if ( _url.indexOf("/conta-corrente/pagar-boleto") != -1 ) {
      userDataStr["paymentType"] = "Debito";
      userDataDouble["paymentValue"] = sortValue([124.12,512.23,6423.12,123.43,33.65,6.23,863.12,6723.52]);
    }

    if ( _url.indexOf("/pix/transferir") != -1 ) {
      userDataStr["paymentType"] = "PIX";
      userDataStr["paymentTarget"] = sortValue(["1001-5001","1001-5002","1004-5001","1003-5005","1005-5010","1002-1002"]);
      userDataDouble["paymentValue"] = sortValue([124.12,512.23,6423.12,123.43,33.65,6.23,863.12,6723.52]);
    }
    
    if ( _url.indexOf("/pix/pagar-qr-code") != -1 ) {
      userDataStr["paymentType"] = "PIX-QRC";
      userDataStr["paymentTarget"] = sortValue(["1001-5001","1001-5002","1004-5001","1003-5005","1005-5010","1002-1002"]);
      userDataDouble["paymentValue"] = sortValue([124.12,512.23,6423.12,123.43,33.65,6.23,863.12,6723.52]);
    }
    
    if ( _url.indexOf("/cartao/pagar") != -1 ) {
      userDataStr["paymentType"] = "CreditCard";
      userDataStr["paymentTarget"] = sortValue(["VISA","MASTERCARD","ELO","REDECARD"]);
      userDataDouble["paymentValue"] = sortValue([124.12,512.23,6423.12,123.43,33.65,6.23,863.12,6723.52]);
    }

    if ( _url.indexOf("/emprestimo/pedir") != -1 ) {
      userDataStr["loanCategory"] = sortValue(["Personal","RealState","Car","Other"]);
      userDataStr["loanType"] = "GET";
      userDataDouble["paymentValue"] = sortValue([5123.12,51532.23,642312.12,1323.43,343.65,6.23,834563.12,67223.52]);
    }

    if ( _url.indexOf("/emprestimo/pagar") != -1 ) {
      userDataStr["loanCategory"] = sortValue(["Personal","RealState","Car","Other"]);
      userDataStr["loanType"] = "PAY";
      userDataDouble["paymentValue"] = sortValue([5123.12,51532.23,642312.12,1323.43,343.65,6.23,834563.12,67223.52]);
    }

    unique_session_id = appdGetParameterByName("unique_session_id");
    if ( unique_session_id != undefined ) {
      userDataStr["unique_session_id"] = unique_session_id;
    }

  } catch (error) {console.error(error);}

  return {
    userData: userDataStr,
    userDataLong: userDataLong,
    userDataDouble: userDataDouble,
    userDataBoolean: userDataBoolean
  }
}

// CONFIGURAÇÕES INICIAIS
(function(config){
  config.resTiming = { "bufSize": 200, "clearResTimingOnBeaconSend": true };
  config.maxUrlLength = 512;
  config.xd = {enable : true};
  config.xhr = {"maxPerPageView":"UNLIMITED"};
  config.channel = { bufferMode: false };
})(window['adrum-config'] || (window['adrum-config'] = {}));

// CUSTOMIZAÇÕES
(function(config){
  (function(info) {
      info.PageView = appdCreatePageView;
  })(config.userEventInfo || (config.userEventInfo = {}))
})(window['adrum-config'] || (window['adrum-config'] = {}));
