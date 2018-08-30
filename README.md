<div align='center'>
	<img src='https://raw.githubusercontent.com/computingfoundation/enterprise-android-application-development/images/logo.png' width='36%' alt='logo.png'>
</div>
<br><br><br>

**Enterprise-android-application-development** is a resource of three components forming a foundational enterprise Android application:

1. A backend application
2. A native Android application
3. A commons library (shared between the backend and Android application)

This application is a completely implemented enterprise Android application. Each application component is implemented in the way you would want an enterprise Android application to implement, with complete coverege of all respective and relative enterprise application requirements in its respective component(s).

All you have to do is start this project to have the following complete generic enterprise frameworks, among other infrastructure frameworks, completely in place:

* Backend modularized HTTP web service
* Backend and frontend *common* HTTP response parceling and JSON REST API
* Frontend modularized UI and event processing

The following is a complete list of all the application components this application implements in each paradigm:

## Features

* User accounts
    * Log in
    * Create account
    * Reset/forgot password
    * Account settings (change or remove email and phone and change username and password)
    * Account deletion
    * Phone number and email address verification (built on the Nexmo messaging service [can be easily changed])
* Remote client configurations
    * Pre-implemented base model validation and application regulation configurations as a starting point (the only application regulation currently implemented is user token lifetime)
    * Completely implemented in backend and frontend with all application features
    * Extensible
* Remotely configured application event messages
    * Pre-created messages for application start and quit, account creation and log in events
    * "First show" and reshow intervals with complete frontend implementation
    * Enabling/disabling of the "Do not show again" option with complete frontend implementation
    * Extensible

## Authentication

* A stateless session authentication framework
    * Secure token creation and validation framework
    * Completely implemented in backend and frontend *common* HTTP response parceling framework
    * Completely implemented backend and frontend logic (e.g. denying session-based request)
    * Pre-implemented generic token algorithm (based on the HMAC hashing algorithm) as a starting point
* A stateful user authentication framework
    * Key generation and storage
    * Secure token creation and validation framework
    * Completely implemented in backend and frontend *common* HTTP response parceling framework
    * Completely implemented backend and frontend logic (e.g. denying user-based requests and logging user out)
    * Pre-implemented generic token algorithm (based on the HMAC hashing algorithm) as a starting point

## Infrastructure

### Backend

* A completely implemented backend modularized JAX-RS HTTP web service framework
  * Session and user token validation
  * Completely implemented parameter and resource validation
* Completely implemented multi-database framework
    * Completely implemented PostgreSQL database for application primary data and relation SQL database data
    * Completely implemented MongoDB database for remote client configurations and document-oriented NoSQL database data
    * Completely implemented Apache Cassandra database for base counter data and distributed wide column store NoSQL database data

### Frontend

* Completely implemented frontend modularized event processing framework
    * Complete event processing of user account settings feature HTTP requests and request errors
    * Complete event processing of base model data GET HTTP requests and request errors and processing
* Completely implemented frontend modularized error management framework

### Backend and frontend

Note that **common** refers to a type of component based on shared code between the backend and frontend.

* A **common** HTTP response parceling framework
  * Included completely implemented **common** modularized error management framework based on three error types: Client fatal error, server fatal error and client general error
* A **common** generic JSON REST API framework
    * Pre-implemented base models as a starting point
    * **Common** REST JSON serialization and deserialization
    * Completely implemented backend and frontend JSON data storage logic
* Completely implemented **common** model object validation of [user](https://github.com/computingfoundation/enterprise-android-applications/blob/master/main/core/modules/commons/src/java/com/organization/commons/validation/UserValidator.java) and base models
    * Completely implemented backend HTTP web service business logic
    * Completely implemented frontend business logic
* Completely implemented **common** modularized utilities for every application category

## Security

* Two-way SSL/TLS encryption
    * Completely implemented certificate pinning and processing in the frontend
    * Provided set-by-step tutorial of generating and implementing SSL certificates for two-way SSL/TLS authentication with the WildFly application server

## UI

* A complete modularized UI framework
    * Completely implemented modularized UI components (e.g. extensible log in screen and content list and view pager)
    * Completely implemented UI logic of every application feature
    * A modularized activity hierarchy with generic base activity features (e.g. enabling a view pager or activity drawer and committing fragment transactions)
* Modularized UI components
    * Modularized API-based widgets (e.g. extensible TextView with custom font loading)
    * Modularized API-based dialogs (e.g. extensible info, text input and options)
    * Modularized view layouts (e.g. extensible activity layout with drawer)

Please note: This project is still unreleased. See [Release](https://github.com/computingfoundation/enterprise-android-application-development#release) for more information.

# Project setup

The following is an overview of the setup of this project.

## IDE

This project is set up as an IntelliJ project and each of the three components are modules. [Here](https://raw.githubusercontent.com/computingfoundation/enterprise-android-applications/images/intellij-modules-illustration.png) is an illustration of the setup. *Please note that core/dao will be removed and core/commons will be changed to a top-level module.*

## Scripts

This project comes with scripts to manage every licecycle stage of every component and the databases.

## Build systems

This project is set up to use the [Twitter Pants](https://www.pantsbuild.org/index.html) build system for the backend application and the commons library and the [Facebook Buck](https://buckbuild.com/) build system for the Android application. This allows almost the fastest builds for each component.

# Set up

As this project is unreleased, the following is only an overview of the set up process. Full detailed instructions will be provided when it is released.

## Commons library

1. Change variable values in `CommonsConstants.java` (e.g. the network address of the server) to the appropriate values.

## Build

1. Build each component using the provided scripts.

## Server backend application

1. Install the WildFly application server.
2. Deploy the built backend application jar to it.

## Client Android application

1. Install the built Android APK to a device or emulator.

# Release

This project is still unreleased as it still needs to be finalized.

Some source code files from the backend and commons library components and the databases are provided.

Thank you.

