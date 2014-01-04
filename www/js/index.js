var db;
var phonecatApp = angular.module('phonecatApp', []);

phonecatApp.controller('PhoneListCtrl', function ($scope, $timeout) {
    $scope.records = [];
    $scope.searchKey = "";
    $scope.searchValue = "";
    $scope.isSearchInProgress = false;
    $scope.isResultEmpty = true;

    $scope.initialize =  function() {
        document.addEventListener('deviceready', $scope.onDeviceReady, false);
    };


    $scope.processSearchResults = function (tx, res) {
        $scope.records = [];
        for (var i=0; i<res.rows.length; i++) {
            $scope.records.push(
                {'k': res.rows.item(i).record_key,
                    'v': res.rows.item(i).record_descr});
        }
        $scope.isResultEmpty = ($scope.records.length == 0);
        $scope.isSearchInProgress = false;
    };

    $scope.search = function(keyQuery, valueQuery) {
        $scope.isSearchInProgress = true;
        $scope.isResultEmpty = false;
        $scope.records = [];
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

                keyQuery = keyQuery.replace("+","%");
                valueQuery = valueQuery.replace("+","%");

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

                queryString += "limit 50;";

                tx.executeSql(queryString, queryFields, $scope.processSearchResults);
            })});

    };

    $scope.onSearchClear = function(event) {
        $scope.searchKey = "";
        $scope.searchValue = "";
        $scope.records = [];
        $scope.isResultEmpty = true;
    };

    $scope.onDeviceReady = function() {
        db = window.sqlitePlugin.openDatabase("vks", "1.0", "My Database", -1);
        //$scope.search(null, null);
    };

    $scope.onSearchChange = function(event) {
        $scope.search($scope.searchKey, $scope.searchValue);
    }

});
