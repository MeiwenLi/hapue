/**
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * This file is licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License. A copy of
 * the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

//snippet-sourcedescription:[s3_photoExample.js demonstrates how to manipulate photos in albums stored in an Amazon S3 bucket.]
//snippet-service:[s3]
//snippet-keyword:[JavaScript]
//snippet-sourcesyntax:[javascript]
//snippet-keyword:[Code Sample]
//snippet-keyword:[Amazon S3]
//snippet-sourcetype:[full-example]
//snippet-sourcedate:[]
//snippet-sourceauthor:[AWS-JSDG]

// ABOUT THIS NODE.JS SAMPLE: This sample is part of the SDK for JavaScript Developer Guide topic at
// https://docs.aws.amazon.com/sdk-for-javascript/v2/developer-guide/s3-example-photo-album.html

// snippet-start:[s3.JavaScript.photoAlbumExample.complete]
// snippet-start:[s3.JavaScript.photoAlbumExample.config]
//var albumBucketName = "sourceimageupload";
//var dstBucketName = "sourceimageupload-trans"
//var bucketRegion = "us-east-1";
//var IdentityPoolId = "us-east-1:c0655d85-4e3b-40b8-a246-b078ef130d39"; //transitepool

var albumBucketName = "happytranslateaudioinput";
var dstBucketName = "happytranslateaudioinput-trans"
var bucketRegion = "us-east-1";
var IdentityPoolId = "us-east-1:c0655d85-4e3b-40b8-a246-b078ef130d39"; //transitepool


AWS.config.update({
    region: bucketRegion,
    credentials: new AWS.CognitoIdentityCredentials({
        IdentityPoolId: IdentityPoolId
    })
});

var s3 = new AWS.S3({
    apiVersion: "2006-03-01",
    params: { Bucket: albumBucketName }
});
// snippet-end:[s3.JavaScript.photoAlbumExample.config]


function uploadImage() {

    var htmlTemplate = [
        "<h3>",
        "Upload the image you want to translate",
        "</h3>",
        '<input id="photoupload" type="file" accept="image/*">',
        '<button id="addphoto" onclick="addPhoto()">',
        "Add Photo",
        "</button>",
    ];

    document.getElementById("app").innerHTML = getHtml(htmlTemplate);
    document.getElementById("text").innerHTML = "";
}

function addPhoto(albumName) {
    var files = document.getElementById("photoupload").files;
    if (!files.length) {
        return alert("Please choose a file to upload first.");
    }
    var file = files[0];
    var fileName = file.name;

    var photoKey = fileName;

    // Use S3 ManagedUpload class as it supports multipart uploads
    var upload = new AWS.S3.ManagedUpload({
        params: {
            Bucket: albumBucketName,
            Key: photoKey,
            Body: file,
            ACL: "public-read"
        }
    });

    var promise = upload.promise();

    promise.then(
        function(data) {
            alert("Successfully uploaded photo.");
            viewphoto(photoKey); //change to viewphoto and showTransition button
        },
        function(err) {
            return alert("There was an error uploading your photo: ", err.message);
        }
    );
}

// snippet-start:[s3.JavaScript.photoAlbumExample.viewAlbum]
function viewphoto(photoKey) {

    var params = {
        Bucket: albumBucketName,
        Key: photoKey,
    };

    s3.getObject(params, function(err,data) {
        if (err) {
            return alert("Cannot find you uploaded image: " + err.message);
        }
        var href = this.request.httpRequest.endpoint.href;
        var bucketUrl = href + albumBucketName + "/";
        var photoUrl = bucketUrl + encodeURIComponent(photoKey);
        var htmlTemplate = [
            "<h2>",
            "Your Uploaded Image",
            "</h2>",
            "<div>",
            '<img style="width:128px;height:128px;" src="' + photoUrl + '"/>',
            "</div>",
            "<h2>",
            "\n ",
            "</h2>",
            '<button id="translate" onclick="trans(\'' + photoKey + "')\">",
            "Translate the Image",
            "</button>"
        ];
        document.getElementById("app").innerHTML = getHtml(htmlTemplate);
    });
}
// snippet-start:[s3.JavaScript.photoAlbumExample.translate]
function trans(photeKey) {

    var dstKey = photeKey + ".txt";
    //var dstKey = "outputfile.txt";
    var params = {
        Bucket: dstBucketName,
        Key: dstKey,
    };

    s3.getObject(params, function(err,data) {
        if (err) {
            return alert("Cannot find you uploaded image: " + err.message);
        }

        var href = this.request.httpRequest.endpoint.href;
        var bucketUrl = href + dstBucketName + "/";
        var txtUrl = bucketUrl + encodeURIComponent(dstKey);
        var htmlTemplate = [
            "<h2>",
            "In Chinese, the Text in above image means:  ",
            "</h2>",
            "<div>",
            data.Body.toString('utf-8'),
            "</div>",
            "<h2>",
            "\n ",
            "</h2>",
            '<button id="home" onclick="uploadImage()">',
            "\n Another Translation",
            "</button>"
        ];
        document.getElementById("text").innerHTML = getHtml(htmlTemplate);
    });
}