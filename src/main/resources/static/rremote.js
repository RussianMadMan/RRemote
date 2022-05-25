function doRequest(method, url, data, callback){
    var xhr = new XMLHttpRequest();
    xhr.open(method, url);
    xhr.onload = (e) => {
        if(callback){
            callback(e)
        }
    }
    if(data){
        xhr.send(data);
    }else{
        xhr.send();
    }
}

function postFormData(formData, url, callback){
    doRequest("POST", url, formData, callback);
}

function postForm(form, url, callback){
    var formData = new FormData(form);
    postFormData(formData, url, callback);
}

function postFormFromId(formId, url, callback){
    var form = document.getElementById(formId);
    postForm(form, url, callback);
}

function doGetWithObject(data, url, callback){
    var params = new URLSearchParams(data)
    doRequest("GET", url + "?" + params, callback);
}