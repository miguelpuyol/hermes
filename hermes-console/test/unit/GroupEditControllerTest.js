
describe("GroupEditController", function() {

    function hermesUrl(path) {
        return 'http://hermes.allegro.tech' + path;
    }

    beforeEach(module('hermes', function(_$provide_){
        $provide = _$provide_;
    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_){
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it("should create group with contact", function() {
        // given
        var group = {
            "groupName": "groupWithContact",
            "groupPassword": "*****",
            "technicalOwner": "Owner",
            "supportTeam": "Example Team",
            "contact": "fake-contact-xyz@zyz.allegro.pl"
        };

        $httpBackend.when('POST', hermesUrl('/groups'), group).respond(201, true);

        // when
        var $scope = {};
        $controller('GroupEditController', {$scope: $scope, group: group, operation: "ADD", $modalInstance: { close: function() {} }});
        $scope.save();

        $httpBackend.flush();

        // then part is in afterEach()
    });

});
