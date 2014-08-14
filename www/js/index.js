var db;
var phonecatApp = angular.module('phonecatApp', []);

phonecatApp.controller('PhoneListCtrl', function ($scope, $timeout) {
    $scope.records = [];
    $scope.searchKey = "";
    $scope.searchValue = "";
    $scope.isSearchInProgress = false;
    $scope.isResultEmpty = true;
    $scope.isMenuVisible = false;
    $scope.isDatabaseReady = false;

    $scope.isMoreRecordsAvaialble = false;

    // How many records load per one page
    $scope.pageSize = 100;

    $scope.initialize =  function() {
        document.addEventListener('deviceready', $scope.onDeviceReady, false);
        document.addEventListener("menubutton", $scope.menuButtonEvent, false);
    };


    $scope.processSearchResults = function (tx, res) {
        for (var i=0; i<res.rows.length; i++) {
            var word = res.rows.item(i).record_descr;

            $scope.records.push(
                {'k': res.rows.item(i).record_key,
                    'v': word });
        }
        $scope.isResultEmpty = ($scope.records.length == 0);
        $scope.isMoreRecordsAvaialble = (res.rows.length == $scope.pageSize);
        $scope.isSearchInProgress = false;
    };

    $scope.search = function(keyQuery, valueQuery) {
        $scope.isSearchInProgress = true;
        $scope.isResultEmpty = false;

        $timeout(function() {
            db.transaction(function(tx) {

                if ((keyQuery == null) || (keyQuery == "")) {
                    keyQuery = "%";
                }

                if (valueQuery == null) {
                    valueQuery = "";
                }

                keyQuery = keyQuery.replace(/\./g,"_");
                valueQuery = valueQuery.replace(/\./g,"_");

                keyQuery = keyQuery.replace(/\+/g,"%");
                valueQuery = valueQuery.replace(/\+/g,"%");

                var queryFields = [keyQuery];
                var queryString = "select record_key, record_descr from dictionary where record_key LIKE ? ";


                var valueParts = valueQuery.split(" ");

                if (valueQuery == "") {
                    valueParts.push("%");
                }

                for (var index = 0 ; index < valueParts.length; index++) {
                    var part = valueParts[index];
                    if (part.length == 0) {
                        continue;
                    }

                    if (part.indexOf("%") == -1) {
                        if (part != "") {
                            part = "%" + part + "%";
                        }
                    }


                    queryString += "and record_descr like ? ";
                    queryFields.push(part)
                }

                queryString += "limit " + $scope.records.length + "," + $scope.pageSize + ";";

                tx.executeSql(queryString, queryFields, $scope.processSearchResults);
            })});

    };

    $scope.loadMoreResults = function() {
        $scope.search($scope.searchKey, $scope.searchValue);
    };

    $scope.onSearchClear = function(event) {
        $scope.searchKey = "";
        $scope.searchValue = "";
        $scope.records = [];
        $scope.isResultEmpty = true;
        $scope.isMoreRecordsAvaialble = false;
    };

    $scope.menuButtonEvent = function (evt) {
        $scope.isMenuVisible = !$scope.isMenuVisible;
        $scope.$apply();
    };

    $scope.openDatabase = function() {
        db = window.sqlitePlugin.openDatabase("vks", "1.0", "My Database", -1);
        $scope.isDatabaseReady = true;
        $scope.isMenuVisible = false;
        $scope.$apply();
    };

    $scope.closeDatabase = function() {
        $scope.isDatabaseReady = false;
        if (db) {
            db.close();
            db = null;
        }
    };

    $scope.deleteDatabase = function() {
        $scope.closeDatabase();
        window.sqlitePlugin.deleteDatabase("vks", $scope.databaseDeleted, $scope.databaseDeleteError);
    };

    $scope.databaseDeleted = function (evt) {
        alert("Database deleted");
    };

    $scope.databaseDeleteError = function (evt) {
        alert("Database delete failed");
    };

    $scope.databaseNotFound = function(evt) {
        $scope.isMenuVisible = true;
        $scope.$apply();
    };

    $scope.onDeviceReady = function() {
        cordova.exec($scope.openDatabase, $scope.databaseNotFound, "DatabaseManagerPlugin",
            "exists", ["vks"]);
    };

    $scope.onSearchChange = function(event) {
        $scope.records = [];
        $scope.search($scope.searchKey, $scope.searchValue);
    };

    $scope.openFile = function (event) {
        fileChooser.open($scope.fileOpenSuccess, $scope.fileOpenFailed);
    };

    $scope.databaseReplaced = function (param) {
        $scope.openDatabase();
        alert("Database reloaded");
        $scope.$apply();
    };

    $scope.databaseReplaceFailed = function(error) {
        alert("Unable to update database");
        $scope.openDatabase();
    };

    $scope.fileOpenSuccess = function (uri) {
        $scope.isMenuVisible = false;
        $scope.closeDatabase();
        cordova.exec($scope.databaseReplaced, $scope.databaseReplaceFailed, "DatabaseManagerPlugin",
            "load", [uri]);
    };

    $scope.fileOpenFailed = function (event) {
        alert("Open failed");
    };
});
