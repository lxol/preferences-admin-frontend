
# preferences-admin-frontend

[![Build Status](https://travis-ci.org/hmrc/preferences-admin-frontend.svg)](https://travis-ci.org/hmrc/preferences-admin-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/preferences-admin-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/preferences-admin-frontend/_latestVersion)

**IMPORTANT NOTE!!!** THIS BRANCH IS A WORK IN PROGRESS TO UPGRADE THE UNDERLYING BOOTSTRAP LIBRARY FROM THE ARCHIVED [frontend-bootstrap](https://github.com/hmrc/frontend-bootstrap) to [bootstrap-play-25](https://github.com/hmrc/bootstrap-play-25)

## How to add a new user?

You need to provide a username and a password. 
The username is a string i.e. "firstName.lastName".
The password is a Base64 encoded an encrypted password. In order to generate a password you can refer to the ticket SUP-8526.

The configuration must be updated with the new user and the application should be redeployed.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")