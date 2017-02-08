
# CFT JUnit
  Below are the instructions to run the CFT JUnit against a specific Cloud Foundry target.
  Currently the JUnit requires a valid access to a Cloud Foundry account.

  **WARNING**: Running the CFT JUnit will **DELETE** all apps and services from the target Cloud Foundry space. Use **great care** when setting the CF target
  to use for the JUnit.

# Two Options to Specify CF Target
  To run the Cloud Foundry unit tests, there are two options to pass in the Cloud Foundry target information, including account information. Note that the JUnit tests also require at least one service instance definition, as services are created and deleted as part of the tests.

## 1. Environment Variables
  Set the following variables in the "Environment" tab in the JUnit Plug-in launch configuration:

  Required:
  
  ```
  CFT_TEST_SPACE
  CFT_TEST_ORG
  CFT_TEST_PASSWORD
  CFT_TEST_USER
  CFT_TEST_URL
  CFT_TEST_SERVICE_NAME
  CFT_TEST_SERVICE_TYPE
  CFT_TEST_SERVICE_PLAN
  ```

  Optional:
  ```
  CFT_TEST_SKIP_SSL - (true/false)
  CFT_TEST_BUILDPACK
  ```
  
## 2. Properties Text File
  An alternative to using environment variables is a local properties text file that can be created at top-level in the org.eclipse.cft.server.tests plug-in with the following name:
  CFcredentials.txt

  Contents:

  ```
  url: [target Cloud URL]
  selfsigned: [true/false. true if target Cloud uses self signed certificate. false or omit if not needed]
  username: [your username]
  password: [your password]
  org: [Cloud organisation]
  space: [Cloud space]
  servicename: [name of service instance to create]
  servicetype: [type of service instance to create]
  serviceplan: [plan of service instance to create]
  buildpack: [OPTIONAL - buildpack name or URL]
  selfsigned: [OPTIONAL - true/false - trust self signed certificate]
  ```

  If the file is created elsewhere, then the full path location of the file should also  be specified by the VM argument:

  ```
  -Dtest.credentials=[file location]
  ```


  Example:

  Create CFcredentials.txt in this location with the following entries:

  ```
  url: api.run.pivotal.io
  selfsigned: false
  username: myusername@pivotal.io
  password: mypassword
  org: PivotalOrg
  space: TestSpace
  servicename: testservice
  servicetype: cleardb
  serviceplan: spark
  ```


  The values of all entries above should be modified as per your account information and target.

  The file location is then passed as a VM argument:

  ```
  -Dtest.credentials=CFcredentials.txt
  ```

  The URL in the properties file can also include "http://" or "https://", instead of just the host:

  url: https://api.run.pivotal.io


  A "CF Base Tests.launch" configuration is provided with the VM args needed to run the Junits.

  However, if you do not wish to create CFcredentials.txt as suggested above, the test.credentials arg needs to be modified to point to your local credentials text file.