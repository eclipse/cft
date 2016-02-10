To run the Cloud Foundry unit tests, a local properties text file should be passed as a VM argument in the unit test
launch configuration. 
The default value of this property is "CFcredentials.txt" which implies you create it at the root of this plug-in, 
that is, the same location as this file.

The plain text properties file should contain:

url: [target Cloud URL]
selfsigned: [true/false. true if target Cloud uses self signed certificate. false or omit if not needed]
username: [your username]
password: [your password]
org: [Cloud organisation]
space: [Cloud space]


The full path location of the file should then be specified by the VM argument:

-Dtest.credentials=[full path file location]


Example:

Create CFcredentials.txt in this location with the following entries:

url: api.run.pivotal.io
selfsigned: false
username: myusername@pivotal.io
password: mypassword
org: PivotalOrg
space: TestSpace

The values of all entries above should be modified as per your account information and target.


The file location is then passed as a VM argument:

-Dtest.credentials=CFcredentials.txt

The URL in the properties file can also include "http://" or "https://", instead of just the host:

url: https://api.run.pivotal.io


A "CF Base Tests.launch" configuration is provided with the VM args needed to run the Junits:

-Xmx1024M -XX:PermSize=256M -XX:MaxPermSize=256M -Dtest.credentials=CFcredentials.txt

However, if you do not wish to create CFcredentials.txt as suggested above, the test.credentials arg needs to be modified to point to your local credentials text file.