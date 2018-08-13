<div align='center'>
	<img src='https://raw.githubusercontent.com/computingfoundation/enterprise-client-server-android-applications/images/logo.png' width='36%' alt='logo.png'>
</div>
<br><br><br>

**Enterprise-client-server-android-applications** is a complete generic enterprise-ready foundation for client-server Android applications.

It consists of three components:

1. A backend application
2. A native Android application
3. A commons library

This project is a complete generic working Android client-server application. Its purpose is to provide a complete foundation for almost any new enterprise Android client-server application and reduce its development time to a fraction. The following is a list/overview of each application component it implements:

## Features

* Fully implemented user account features
    * Log in
    * Create account
    * Reset/forgot password
    * Account settings (change or remove email and phone and change username and password)
    * Account deletion
    * Phone number and email address verification (built on the Nexmo messaging service [can be easily changed])
* Schemaless remote client configurations
    * Provides base model validation configurations as a starting point
    * Provides base application regulation configurations as a starting point (currently the only configuration is user token lifetime)
    * Fully implemented in frontend and backend code
* Application event messages (via remote client configurations)
    * "First show" and reshow intervals
    * Enabling/disabling of the "Do not show again" option
    * Fully implemented in frontend code at multiple events, including app start up and exit, account created and log in
    * Easily extensible

## Authentication

* Stateless session authentication
    * Complete secure implementation of token creation and validation in frontend and backend with processing (e.g. denying session-based request)
    * Provides a generic token algorithm (based on the HMAC hashing algorithm) as a starting point
    * Completely integrated with backend HTTP request, response and request error handling and processing framework
* Stateful user authentication
    * Key generation and storage
    * Complete secure implementation of token creation and validation in frontend and backend with processing (e.g. denying user-based requests and logging user out)
    * Provides a generic token algorithm (based on the HMAC hashing algorithm) as a starting point
    * Completely integrated with backend HTTP request, response and request error handling and processing framework

## Infrastructure

* Complete frontend and [backend](https://github.com/computingfoundation/enterprise-client-server-android-applications/tree/master/main/backend/modules/ejb/src/java/com/myorganization/backend/rest) HTTP request, response and request error frameworks
    * Fully-implemented REST framework based on JAX-RS
    * Full error management on both the frontend and backend with three error types: Client fatal error, server fatal error and client general error
    * A complete generic solution to process a response body
* A complete generic REST model processing framework
    * Provides base models as a starting point
    * JSON serialization and deserialization
    * A generic backend framework for processing HTTP responses
    * Frontend JSON processing to a DAO database
* Fully implemented model object validation
    * Complete validation of [user](https://github.com/computingfoundation/enterprise-client-server-android-applications/blob/master/main/core/modules/commons/src/java/com/organization/commons/validation/UserValidator.java) and base models
    * Fully implemented on the frontend and backend
* Fully implemented frontend event processing
    * Fully implemented event processing for all account settings feature HTTP requests and request errors
    * Fully implemented event processing for all generic model data get HTTP requests and request errors and processing
* Multi-database integration
    * Full extensible implementation of PostgreSQL for application primary data and relation SQL database data
    * Full extensible implementation of MongoDB for remote client configurations and document-oriented NoSQL database data
    * Full extensible implementation of [Apache Cassandra](https://github.com/computingfoundation/enterprise-client-server-android-applications/blob/master/main/backend/modules/ejb/src/java/com/myorganization/backend/database/Cassandra.java) for base vote/counter data and distributed wide column store NoSQL database data
* Complete generic utilities for every application category

## Security

* Two-way SSL/TLS encryption
    * Fully implemented certificate pinning and processing in frontend
    * Provided set-by-step tutorial of generating SSL certificates for use between client application and the WildFly application server
    * Step-by-step tutorial of integration with WildFly

## UI

* Reusable UI widgets
    * Generic API-based dialogs (e.g. info, text input and options)
* A fully implemented UI framework
    * A generic activity hierarchy with generic base activity features such as enabling a view pager or activity drawer and committing fragment transactions
    * Account settings list
    * Full UI implementation of each account setting feature

This project is easy to set up as each application component is fully implemented. See [Set up](https://github.com/computingfoundation/enterprise-client-server-android-applications#set-up) for more information.

Each application component of this project is implemented with possibly the best implementation possible. **This project has been in development for three full years.**

This project is also the foundation of Android application [FencedIn](http://www.fencedinapp.com), an application that has been in development for four full years.

**Please note: This project is still unreleased.** See [Release](https://github.com/computingfoundation/enterprise-client-server-android-applications#release) for more information.

# Project setup

The following is an overview of the setup of this project.

## IDE

This project is set up as an IntelliJ project and each of the three components are modules. [Here](https://raw.githubusercontent.com/computingfoundation/enterprise-client-server-android-applications/images/intellij-modules-illustration.png) is an illustration of the setup. *Please note that core/dao will be removed and core/commons will be changed to a top-level module.*

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

This project is unreleased and needs to be finalized. In order for me to finalize it, I need funding. To read more and donate, please go to [www.fundly.com/cf-enterprise-client-server-android-applications](https://fundly.com/cf-enterprise-client-server-android-applications).

Some source code files from the backend and commons library components and the databases are provided. Please note that many of them have changed.

Thank you.

