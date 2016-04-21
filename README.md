# Eclipse Tools for Cloud Foundry
  
  Eclipse Tools for Cloud Foundry (CFT) is a framework that provides first-class support for the [Cloud Foundry
  PaaS] (http://docs.cloudfoundry.org). It allows users to directly deploy applications from their Eclipse
  workspace to a running Cloud Foundry server instance, as well as view and manage deployed applications and services.
  
  Cloud Foundry vendors can also contribute their own Cloud Foundry target definitions for users of the tools
  through various extension points provided by the framework.
  
  CFT is a project under [Eclipse Cloud Development](https://projects.eclipse.org/projects/ecd): 
  
  https://projects.eclipse.org/projects/ecd.cft
  
  CFT integrates into and requires Eclipse [Web Tools Platform (WTP)](http://eclipse.org/webtools).
  
  CFT is also part of the Eclipse Simultaneous Release, and we follow the Eclipse Simultaneous Release cycle.
  
  https://wiki.eclipse.org/Simultaneous_Release  
  
## IMPORTANT NOTE: Breaking Bundle and Extension Point Changes
  
  All bundle names and extension point IDs have been renamed, therefore new versions of CFT are NOT backward compatible with any older versions 
  of the plug-in, previously known as Cloud Foundry Eclipse, that used bundle names starting with:
  
  org.cloudfoundry.ide.eclipse.server
  
  Any older versions of the tools need to be manually uninstalled FIRST before installing CFT. This applies to Cloud Foundry Eclipse version 1.8.3 and older.
  
## CLA and Third-Party Pull Requests
  
  All third-party contributors are required to sign an Eclipse CLA before Pull Requests are merged into the repository, 
  pending review and acceptance of the submitted Pull Request.
  
  https://wiki.eclipse.org/CLA
  
  In addition to signing the Eclipse CLA, your Pull Request must not have merge conflicts with the master branch, so please be sure to
  synchronize with master branch before creating your Pull Request.
  
## Update Sites
  
  The nightly CFT driver can be installed using this URL:
    
  http://download.eclipse.org/cft/nightly/
  
  (place this URL into the "Install New Software" dialog of your Eclipse)
  
  It contains our latest changes and is not guaranteed to be fully stable.
  
  To install stable milestones or releases, please refer to the list of sites under "Update Sites":
  
  https://projects.eclipse.org/projects/ecd.cft/downloads
  
  You can also install from the Eclipse Marketplace. In Eclipse, go to Help -> Eclipse Marketplace 
  and search for "Cloud Foundry".
  
  The marketplace page is:
  http://marketplace.eclipse.org/content/cloud-foundry-integration-eclipse
  
## Raising Bugs, Feature Requests, Mailing List

  Bugs and feature requests should be raised via bugzilla under product "CFT":
  
  https://bugs.eclipse.org/bugs/enter_bug.cgi?product=CFT
  
  We also encourage you to subscribe to our cft-dev mailing list, as we frequently post updates on CFT there, including milestone 
  and release information.
  
  It's also a good place to ask questions to the devs.
  
  https://dev.eclipse.org/mailman/listinfo/cft-dev

## Installation

  Java 7 is a minimum execution environment required to install and run CFT.
  Please make sure that your Eclipse is using Java 7 or higher.
  
## Offline Installation

  Release and milestone versions of CFT can be installed offline using one of the release update 
  site zip files listed below. Once the zip file is available in an offline environment, CFT can be 
  installed following these steps in Eclipse or STS:
  
  Help -> Install New Software -> Add -> Archive
  
  Browse to the location of the zip file, and installation should complete in offline mode.
  
  Zips for the update sites can be found under "Downloads" here:
  
  [Update Sites Zips](https://projects.eclipse.org/projects/ecd.cft/downloads)
  
## Cloud Foundry Java Client Library
   
   CFT 1.0 uses a customized, unpublished version of the v1 Java Cloud Foundry client library. The library is Apache 2.0 license, 
   and we maintain that library outside of Eclipse here:
   
   https://github.com/nierajsingh/cf-java-client/tree/cft-1.0.0-java-client-1.1.4-patch
      
   We plan to move to v2 client sometime after Neon release (June 23, 2016). Please subscribe to our mailing lists to
   get updates on progress towards v2, as well as check bugzilla under "CFT" product.
   
   Once we move to v2, we will no longer support v1.
   
   More information on the v2 client is available here:
   
   https://github.com/cloudfoundry/cf-java-client
    

## Getting started

  The basic steps for using CFT are described here:

  http://docs.cloudfoundry.org/buildpacks/java/sts.html

  Note that this description is targeted at users of Spring Tool Suite, but similar steps apply for
  other Eclipse JEE users.
  
## Hudson Builds

  We build CFT nightly, milestones, and releases using Hudson:
  
  https://hudson.eclipse.org/cft/
  
## Building the project
  
  The CFT uses Maven Tycho to do continuous integration builds and
  to produce p2 repos and update sites. To build the tooling yourself, you can execute:

  mvn -Pe43 package
  
  Check the parent pom in master branch for additional profiles under "profiles". We build against the latest Eclipse, so you 
  should be able to find a profile for the latest Eclipse that you can pass to mvn:
  
  https://github.com/eclipse/cft/blob/master/pom.xml