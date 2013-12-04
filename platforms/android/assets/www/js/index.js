var db;
var phonecatApp = angular.module('phonecatApp', []);

phonecatApp.controller('PhoneListCtrl', function ($scope) {
    $scope.records = [];
    $scope.searchKey = "";
    $scope.searchValue = "";

    $scope.initialize =  function() {
        document.addEventListener('deviceready', $scope.onDeviceReady, false);
    };


    $scope.processSearchResults = function (tx, res) {
        console.log("Loaded:"+res.rows.length);
        $scope.records = [];
        for (var i=0; i<res.rows.length; i++) {
            $scope.records.push(
                {'k': res.rows.item(i).record_key,
                    'v': res.rows.item(i).record_descr});
        }

    };

    $scope.search = function(keyQuery, valueQuery) {
        db.transaction(function(tx) {

            if ((keyQuery == null) || (keyQuery == "")) {
                keyQuery = "%";
            }

            if ((valueQuery == null) || (valueQuery == "")) {
                valueQuery = "%";
            }

            keyQuery = keyQuery.replace(".","_");
            valueQuery = valueQuery.replace(".","_");

            keyQuery = keyQuery.replace("+","%");
            valueQuery = valueQuery.replace("+","%");

            var count = valueQuery.match(/%/g);
            if (count == 0) {
                valueQuery = "%" + valueQuery + "%";
            }

            var queryString = "select record_key, record_descr from dictionary where record_key LIKE ? and record_descr like ? limit 50;";

            tx.executeSql(queryString, [keyQuery, valueQuery], $scope.processSearchResults);
        });

    };

    $scope.onDeviceReady = function() {
        db = window.sqlitePlugin.openDatabase("vks", "1.0", "My Database", -1);
        //$scope.search(null, null);
    };

    $scope.onSearchChange = function(event) {
        $scope.search($scope.searchKey, $scope.searchValue);
    }

});
